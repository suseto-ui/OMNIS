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

    @Query("SELECT * FROM omnis_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getRecordsByThread(threadId: String): Flow<List<OmnisRecord>>

    @Query("SELECT * FROM omnis_messages WHERE userName = :userName ORDER BY timestamp ASC")
    fun getRecordsByUser(userName: String): Flow<List<OmnisRecord>>

    @Query("SELECT DISTINCT userName FROM omnis_messages WHERE userName IS NOT NULL AND userName != '' ORDER BY userName ASC")
    fun getAllUserNames(): Flow<List<String>>

    @Query("SELECT threadId, threadTitle, userName, MAX(timestamp) as lastTimestamp, COUNT(id) as messageCount, (SELECT content FROM omnis_messages m2 WHERE m2.threadId = m1.threadId ORDER BY timestamp DESC LIMIT 1) as lastContent FROM omnis_messages m1 WHERE userName = :userName GROUP BY threadId ORDER BY lastTimestamp DESC")
    fun getThreadsByUser(userName: String): Flow<List<ThreadSummary>>

    @Query("SELECT threadId, threadTitle, userName, MAX(timestamp) as lastTimestamp, COUNT(id) as messageCount, (SELECT content FROM omnis_messages m2 WHERE m2.threadId = m1.threadId ORDER BY timestamp DESC LIMIT 1) as lastContent FROM omnis_messages m1 GROUP BY threadId ORDER BY lastTimestamp DESC")
    fun getAllThreads(): Flow<List<ThreadSummary>>

    @Query("DELETE FROM omnis_messages WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("UPDATE omnis_messages SET threadTitle = :newTitle WHERE threadId = :threadId")
    suspend fun updateThreadTitle(threadId: String, newTitle: String)

    @Query("SELECT * FROM omnis_messages ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<OmnisRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: OmnisRecord): Long

    @Query("DELETE FROM omnis_messages WHERE id <= :maxSyncedId")
    suspend fun deleteSyncedRecords(maxSyncedId: Long)

    @Query("DELETE FROM omnis_messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM omnis_messages WHERE isSyncedToPostgres = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("UPDATE omnis_messages SET isSyncedToPostgres = 1 WHERE isSyncedToPostgres = 0")
    suspend fun markAllAsSynced()

    @Query("SELECT COUNT(*) FROM omnis_messages")
    suspend fun getRecordCount(): Int

    // Memory Fragments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragment(fragment: MemoryFragment): Long

    @Query("SELECT * FROM omnis_memory_fragments ORDER BY timestamp DESC")
    fun getAllFragments(): Flow<List<MemoryFragment>>

    @Query("DELETE FROM omnis_memory_fragments WHERE id = :id")
    suspend fun deleteFragment(id: Long)

    // Artifacts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: OmnisArtifact): Long

    @Query("SELECT * FROM omnis_artifacts ORDER BY timestamp DESC")
    fun getAllArtifacts(): Flow<List<OmnisArtifact>>

    @Query("DELETE FROM omnis_artifacts WHERE id = :id")
    suspend fun deleteArtifact(id: Long)

    // Goals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: OmnisGoal): Long

    @Query("SELECT * FROM omnis_goals ORDER BY priority DESC, timestamp DESC")
    fun getAllGoals(): Flow<List<OmnisGoal>>

    @Query("DELETE FROM omnis_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE omnis_goals SET status = :status, progress = :progress, tasksJson = :tasksJson WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, status: String, progress: Float, tasksJson: String)

    // Telemetry
    @Insert
    suspend fun insertTelemetry(telemetry: OmnisTelemetry)

    @Query("SELECT * FROM omnis_telemetry ORDER BY timestamp DESC LIMIT 200")
    fun getRecentTelemetry(): kotlinx.coroutines.flow.Flow<List<OmnisTelemetry>>

    @Query("DELETE FROM omnis_telemetry")
    suspend fun clearTelemetry()
}
