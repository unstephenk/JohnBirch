package com.kuehlconsulting.johnbirchsociety.data

import android.content.ContentValues
import android.content.Context
import com.kuehlconsulting.johnbirchsociety.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.os.Environment
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import java.io.OutputStream

class Mp3Downloader(private val context: Context) {

    private val client = OkHttpClient()

    @OptIn(UnstableApi::class)
    suspend fun downloadMp3(
        rssItem: RssItem,
        onProgress: (Float) -> Unit,
        onCompletion: (String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = rssItem.enclosureUrl ?: return@withContext onCompletion(null)

        // Build a request that most servers will accept
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) OkHttp JohnBirch/1.0")
            .header("Accept", "*/*")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Mp3Downloader", "HTTP ${response.code} ${response.message} for $url")
                onCompletion(null)
                return@withContext
            }

            val body = response.body

            val reportedLen = body.contentLength() // may be -1 but your feed usually has length
            Log.d("Mp3Downloader", "Start download: len=$reportedLen url=$url")

            val fileName = url.substringAfterLast('/').ifBlank { "audio_${System.currentTimeMillis()}.mp3" }

            // Choose output: MediaStore on API 29+, File on older
            var localRef: String? = null
            var out: OutputStream? = null
            var mediaUri: android.net.Uri? = null
            var legacyFile: File? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Save into Android/media/<package>/JohnBirch
                    val relativePath = "Android/media/${context.packageName}/JohnBirch"
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    }
                    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    mediaUri = context.contentResolver.insert(collection, values)
                    if (mediaUri == null) throw IOException("Failed to create media row")
                    out = context.contentResolver.openOutputStream(mediaUri, "w")
                    if (out == null) throw IOException("Failed to open output stream")
                    localRef = mediaUri.toString() // return a URI string on Q+
                } else {
                    // API 28 and lower: write directly to public external storage
                    // (WRITE_EXTERNAL_STORAGE is already in your manifest with maxSdkVersion)
                    val base = Environment.getExternalStorageDirectory()
                    val targetDir = File(base, "Android/media/${context.packageName}/JohnBirch").apply { mkdirs() }
                    legacyFile = File(targetDir, fileName)
                    out = FileOutputStream(legacyFile)
                    localRef = legacyFile.absolutePath
                }

                // Stream copy with progress
                body.byteStream().use { input ->
                    out.use { dst ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            dst!!.write(buffer, 0, read)
                            downloaded += read
                            if (reportedLen > 0) {
                                onProgress(downloaded.toFloat() / reportedLen.toFloat())
                            }
                        }
                        dst!!.flush()
                        // For file streams, ensure it’s flushed to disk
                        if (dst is FileOutputStream) dst.fd.sync()
                    }
                }

                // Make visible to media apps (mainly for legacy path)
                legacyFile?.let { f ->
                    MediaScannerConnection.scanFile(
                        context, arrayOf(f.absolutePath), arrayOf("audio/mpeg"), null
                    )
                }

                Log.d("Mp3Downloader", "Done: $localRef")
                onProgress(1f)
                onCompletion(localRef)
            } catch (e: Exception) {
                // Clean up partially written MediaStore item
                mediaUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                legacyFile?.let { runCatching { it.delete() } }
                Log.e("Mp3Downloader", "Download failed for $url", e)
                onCompletion(null)
            }
        } catch (e: Exception) {
            Log.e("Mp3Downloader", "Network error for $url", e)
            onCompletion(null)
        }
    }
}
