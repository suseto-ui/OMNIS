package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OmnisDao {
    @Query("SELECT * FROM omnis_messages ORDER BY timestamp ASC")
    fun getAllRecords(): Flow<List<OmnisRecord>>

    @Query("SELECT * FROM omnis_messages ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<OmnisRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: OmnisRecord): Long

    @Query("DELETE FROM omnis_messages")
    suspend fun clearAll()
}
