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
        val rowId = dao.insertRecord(record)
        val recordWithId = record.copy(id = rowId)
        if (!DatabaseConfig.isTesting) {
            CloudSqlSyncManager.syncRecordAsync(recordWithId)
        }
        return rowId
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
}
