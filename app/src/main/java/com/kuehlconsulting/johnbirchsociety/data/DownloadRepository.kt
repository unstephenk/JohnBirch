package com.kuehlconsulting.johnbirchsociety.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.kuehlconsulting.johnbirchsociety.data.db.AppDatabase
import com.kuehlconsulting.johnbirchsociety.data.db.Download
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).downloadDao()

    fun observeAll(): Flow<List<Download>> = dao.observeAll()

    suspend fun upsertCompleted(enclosureUrl: String, localRef: String, bytesExpected: Long?) {
        dao.upsert(
            Download(
                enclosureUrl = enclosureUrl,
                localRef = localRef,
                bytesExpected = bytesExpected,
                bytesCompleted = bytesExpected,
                isDownloaded = true
            )
        )
    }

    /**
     * Scan Android/media/<pkg>/JohnBirch and reconcile DB entries.
     * Call on app start or when user pulls to refresh.
     */
    suspend fun reconcileJohnBirchFolder() {
        val relPath = "Android/media/${context.packageName}/JohnBirch/"
        val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val sel = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val args = arrayOf(relPath)

        val foundByName = mutableMapOf<String, String>() // filename -> content:// uri string
        context.contentResolver.query(uri, projection, sel, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) // e.g., 20250730_Wed_Alex.mp3
                val itemUri = ContentUris.withAppendedId(uri, id).toString()
                foundByName[name] = itemUri
            }
        }

        // Optional: If your enclosureUrl always ends with the file name:
        // match DB items by lastPathSegment; add missing ones as completed.
        // (If not, skip this, DB remains the source of truth after first download.)
    }
}
