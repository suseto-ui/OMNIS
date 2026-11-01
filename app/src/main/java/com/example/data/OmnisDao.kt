package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OmnisDao {
    @Query("SELECT * FROM omnis_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: String): Flow<List<OmnisRecord>>

    @Query("SELECT * FROM omnis_messages ORDER BY timestamp ASC")
    fun getAllRecords(): Flow<List<OmnisRecord>>

    @Query("SELECT DISTINCT threadId FROM omnis_messages")
    fun getAllThreads(): Flow<List<String>>

    @Query("SELECT * FROM omnis_memory_fragments ORDER BY timestamp DESC")
    fun getAllFragments(): Flow<List<MemoryFragment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: OmnisRecord): Long

    suspend fun insertRecord(record: OmnisRecord): Long = insert(record)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<OmnisRecord>)

    @Query("DELETE FROM omnis_messages WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragment(fragment: MemoryFragment): Long

    @Query("DELETE FROM omnis_memory_fragments WHERE id = :id")
    suspend fun deleteFragment(id: Long)

    @Query("SELECT COUNT(*) FROM omnis_messages")
    suspend fun getRecordCount(): Int

    @Query("SELECT COUNT(*) FROM omnis_messages WHERE threadId = :threadId")
    suspend fun getThreadMessageCount(threadId: String): Int

    @Query("SELECT COUNT(*) FROM omnis_messages WHERE threadId LIKE 'benchmark_%' OR threadId = 'thread_benchmark'")
    suspend fun getBenchmarkRecordCount(): Int

    @Query("DELETE FROM omnis_messages WHERE threadId LIKE 'benchmark_%' OR threadId = 'thread_benchmark'")
    suspend fun deleteBenchmarkRecords(): Int

    @Query("SELECT * FROM omnis_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecordsSync(limit: Int): List<OmnisRecord>

    @Query("SELECT * FROM omnis_messages WHERE threadId = :threadId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getPagedRecordsByThread(threadId: String, limit: Int = 50, offset: Int = 0): List<OmnisRecord>

    @Query("SELECT * FROM omnis_messages WHERE isSyncedToPostgres = 0 LIMIT :limit")
    suspend fun getUnsyncedRecords(limit: Int = 100): List<OmnisRecord>

    @Query("SELECT COUNT(*) FROM omnis_messages WHERE isSyncedToPostgres = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("UPDATE omnis_messages SET isSyncedToPostgres = 1 WHERE id IN (:ids)")
    suspend fun markRecordsAsSynced(ids: List<Long>)

    @Query("UPDATE omnis_messages SET isSyncedToPostgres = 1")
    suspend fun markAllAsSynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: OmnisGoal): Long
}
