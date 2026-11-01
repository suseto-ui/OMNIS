package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pro tabulku auditních záznamů uživatelských aktivit.
 */
@Dao
interface UserActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UserActivityLogEntity)

    @Query("SELECT * FROM user_activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UserActivityLogEntity>>

    @Query("SELECT * FROM user_activity_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getLogsByUserId(userId: String): Flow<List<UserActivityLogEntity>>

    @Query("SELECT * FROM user_activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<UserActivityLogEntity>>

    @Query("DELETE FROM user_activity_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM user_activity_logs")
    suspend fun getLogCount(): Int
}
