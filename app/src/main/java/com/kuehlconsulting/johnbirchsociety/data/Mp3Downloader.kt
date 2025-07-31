package com.kuehlconsulting.johnbirchsociety.data

import android.content.Context
import com.kuehlconsulting.johnbirchsociety.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Mp3Downloader(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun downloadMp3(
        rssItem: RssItem,
        onProgress: (Float) -> Unit,
        onCompletion: (String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (rssItem.enclosureUrl == null) {
            onCompletion(null)
            return@withContext
        }

        val fileName = rssItem.enclosureUrl?.substringAfterLast('/') ?: "audio_${System.currentTimeMillis()}.mp3"
        val file = File(context.filesDir, fileName)

        try {
            val request = Request.Builder().url(rssItem.enclosureUrl!!).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) throw IOException("Failed to download file: ${response}")

            val body = response.body ?: throw IOException("Response body is null")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                    }
                }
            }
            onCompletion(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onCompletion(null)
        }
    }
}
