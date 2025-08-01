package com.kuehlconsulting.johnbirchsociety.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey val enclosureUrl: String,   // stable key from RSS
    val localRef: String,                   // content://… (Q+) or file path (< Q)
    val bytesExpected: Long?,               // from HTTP header or RSS length attr
    val bytesCompleted: Long?,              // optional (for partials/cancel)
    val isDownloaded: Boolean,              // true when fully written
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null
)
