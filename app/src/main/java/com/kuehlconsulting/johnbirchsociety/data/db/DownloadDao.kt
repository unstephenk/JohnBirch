package com.kuehlconsulting.johnbirchsociety.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE enclosureUrl = :url LIMIT 1")
    suspend fun get(url: String): Download?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(d: Download)

    @Query("DELETE FROM downloads WHERE enclosureUrl = :url")
    suspend fun delete(url: String)
}
