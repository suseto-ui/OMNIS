package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OmnisRepository(private val dao: OmnisDao) {
    val omnisDao: OmnisDao get() = dao
    val allRecords: Flow<List<OmnisRecord>> = dao.getAllRecords()

    val latestRecord: Flow<OmnisRecord?> = dao.getAllRecords().map { it.lastOrNull() }

    val allFragments: Flow<List<MemoryFragment>> = dao.getAllFragments()

    val allThreads: Flow<List<ThreadSummary>> = dao.getAllRecords().map { records ->
        records.groupBy { it.threadId }.map { (threadId, list) ->
            val last = list.maxByOrNull { it.timestamp }
            ThreadSummary(
                threadId = threadId,
                threadTitle = last?.threadTitle ?: "Thread",
                userName = last?.userName ?: "operator",
                lastTimestamp = last?.timestamp ?: System.currentTimeMillis(),
                messageCount = list.size,
                lastContent = last?.content
            )
        }
    }

    val allUserNames: Flow<List<String>> = dao.getAllRecords().map { records ->
        records.map { it.userName }.distinct()
    }

    fun getMessagesForThread(threadId: String): Flow<List<OmnisRecord>> = dao.getMessagesForThread(threadId)

    fun getRecordsByThread(threadId: String): Flow<List<OmnisRecord>> = dao.getMessagesForThread(threadId)

    fun getRecordsByUser(userName: String): Flow<List<OmnisRecord>> = dao.getAllRecords().map { list ->
        list.filter { it.userName == userName }
    }

    fun getThreadsByUser(userName: String): Flow<List<ThreadSummary>> = allThreads.map { threads ->
        threads.filter { it.userName == userName }
    }

    fun searchRecords(query: String, threadId: String?, limit: Int): Flow<List<OmnisRecord>> = dao.getAllRecords().map { list ->
        list.filter { rec ->
            (threadId == null || rec.threadId == threadId) && rec.content.contains(query, ignoreCase = true)
        }.take(limit)
    }

    suspend fun insert(record: OmnisRecord): Long = dao.insert(record)

    suspend fun insertRecordsBatched(records: List<OmnisRecord>, chunkSize: Int = 50) {
        records.chunked(chunkSize).forEach { chunk ->
            dao.insertRecords(chunk)
        }
    }

    suspend fun deleteThread(threadId: String) = dao.deleteThread(threadId)

    suspend fun updateThreadTitle(threadId: String, newTitle: String) {}

    suspend fun insertFragment(fragment: MemoryFragment): Long = dao.insertFragment(fragment)

    suspend fun deleteFragment(id: Long) = dao.deleteFragment(id)

    suspend fun pruneOldSyncedRecords(daysOld: Int): Int = 0

    suspend fun clear() {}
}
