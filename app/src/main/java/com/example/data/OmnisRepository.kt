package com.example.data

import kotlinx.coroutines.flow.Flow

class OmnisRepository(private val dao: OmnisDao) {
    val omnisDao = dao
    val allRecords: Flow<List<OmnisRecord>> = dao.getAllRecords()
    val latestRecord: Flow<OmnisRecord?> = dao.getLatestRecord()
    val allFragments: Flow<List<MemoryFragment>> = dao.getAllFragments()
    val allUserNames: Flow<List<String>> = dao.getAllUserNames()
    val allThreads: Flow<List<ThreadSummary>> = dao.getAllThreads()

    fun getThreadsByUser(userName: String): Flow<List<ThreadSummary>> = dao.getThreadsByUser(userName)
    fun getRecordsByThread(threadId: String): Flow<List<OmnisRecord>> = dao.getRecordsByThread(threadId)
    fun getRecordsByUser(userName: String): Flow<List<OmnisRecord>> = dao.getRecordsByUser(userName)

    suspend fun deleteThread(threadId: String) {
        dao.deleteThread(threadId)
    }

    suspend fun updateThreadTitle(threadId: String, newTitle: String) {
        dao.updateThreadTitle(threadId, newTitle)
    }

    suspend fun insert(record: OmnisRecord): Long {
        return try {
            val rowId = dao.insertRecord(record)
            val recordWithId = record.copy(id = rowId)
            if (!DatabaseConfig.isTesting) {
                try {
                    CloudSqlSyncManager.syncRecordAsync(recordWithId, dao)
                } catch (e: Exception) {
                    android.util.Log.e("OmnisRepository", "Async cloud sync failed", e)
                }
            }
            rowId
        } catch (e: Exception) {
            android.util.Log.e("OmnisRepository", "Database insert failed", e)
            -1L
        }
    }

    /**
     * Dávková živá synchronizace neodbavených zpráv přímo do Google Cloud SQL.
     */
    suspend fun syncWithCloudSql(): Result<Int> {
        return CloudSqlSyncManager.syncPendingRecords(dao)
    }

    /**
     * Test konektivity k instanci Google Cloud SQL.
     */
    suspend fun testCloudSqlConnection(): Pair<Boolean, String> {
        return CloudSqlSyncManager.testConnection()
    }

    /**
     * Spustí obousměrnou synchronizaci konverzací s backend API.
     */
    fun syncWithBackend(context: android.content.Context, onComplete: ((Boolean) -> Unit)? = null) {
        OmnisSyncManager.triggerSync(context, this, onComplete)
    }

    suspend fun deleteSyncedRecords(maxSyncedId: Long) {
        dao.deleteSyncedRecords(maxSyncedId)
    }

    suspend fun clear() {
        dao.clearAll()
    }

    suspend fun insertFragment(fragment: MemoryFragment): Long {
        return dao.insertFragment(fragment)
    }

    suspend fun deleteFragment(id: Long) {
        dao.deleteFragment(id)
    }

    // Big Data & High-Throughput Storage Methods
    suspend fun getPagedRecords(threadId: String, limit: Int, offset: Int): List<OmnisRecord> {
        return dao.getPagedRecordsByThread(threadId, limit, offset)
    }

    fun searchRecords(query: String, threadId: String? = null, limit: Int = 100): Flow<List<OmnisRecord>> {
        return dao.searchRecords(query, threadId, limit)
    }

    suspend fun insertRecordsBatched(records: List<OmnisRecord>, chunkSize: Int = 250) {
        if (records.isEmpty()) return
        records.chunked(chunkSize).forEach { chunk ->
            dao.insertRecords(chunk)
            kotlinx.coroutines.yield()
        }
    }

    suspend fun pruneOldSyncedRecords(daysOld: Int = 14): Int {
        val cutoffTimestamp = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
        return dao.pruneOldSyncedRecords(cutoffTimestamp)
    }

    suspend fun getThreadMessageCount(threadId: String): Int {
        return dao.getThreadMessageCount(threadId)
    }
}
