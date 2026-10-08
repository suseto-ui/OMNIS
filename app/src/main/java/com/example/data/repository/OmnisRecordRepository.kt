package com.example.data.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture Repository rozhraní pro správu záznamů, vláken a paměti v systému O.M.N.I.S.
 */
interface OmnisRecordRepository {
    val allRecords: Flow<List<OmnisRecord>>
    val latestRecord: Flow<OmnisRecord?>
    val allFragments: Flow<List<MemoryFragment>>
    val allThreads: Flow<List<ThreadSummary>>
    val allUserNames: Flow<List<String>>

    fun getRecordsByThread(threadId: String): Flow<List<OmnisRecord>>
    fun getRecordsByUser(userName: String): Flow<List<OmnisRecord>>
    fun getThreadsByUser(userName: String): Flow<List<ThreadSummary>>
    fun searchRecords(query: String, threadId: String? = null, limit: Int = 100): Flow<List<OmnisRecord>>

    suspend fun insert(record: OmnisRecord): Long
    suspend fun insertRecordsBatched(records: List<OmnisRecord>, chunkSize: Int = 250)
    suspend fun deleteThread(threadId: String)
    suspend fun updateThreadTitle(threadId: String, newTitle: String)
    suspend fun insertFragment(fragment: MemoryFragment): Long
    suspend fun deleteFragment(id: Long)
    suspend fun pruneOldSyncedRecords(daysOld: Int = 14): Int
    suspend fun clear()
}

/**
 * Výchozí implementace OmnisRecordRepository delegující na Room OmnisDao a OmnisRepository.
 */
class OmnisRecordRepositoryImpl(
    private val repository: OmnisRepository
) : OmnisRecordRepository {
    override val allRecords: Flow<List<OmnisRecord>> = repository.allRecords
    override val latestRecord: Flow<OmnisRecord?> = repository.latestRecord
    override val allFragments: Flow<List<MemoryFragment>> = repository.allFragments
    override val allThreads: Flow<List<ThreadSummary>> = repository.allThreads
    override val allUserNames: Flow<List<String>> = repository.allUserNames

    override fun getRecordsByThread(threadId: String): Flow<List<OmnisRecord>> = repository.getRecordsByThread(threadId)
    override fun getRecordsByUser(userName: String): Flow<List<OmnisRecord>> = repository.getRecordsByUser(userName)
    override fun getThreadsByUser(userName: String): Flow<List<ThreadSummary>> = repository.getThreadsByUser(userName)
    override fun searchRecords(query: String, threadId: String?, limit: Int): Flow<List<OmnisRecord>> =
        repository.searchRecords(query, threadId, limit)

    override suspend fun insert(record: OmnisRecord): Long = repository.insert(record)
    override suspend fun insertRecordsBatched(records: List<OmnisRecord>, chunkSize: Int) =
        repository.insertRecordsBatched(records, chunkSize)
    override suspend fun deleteThread(threadId: String) = repository.deleteThread(threadId)
    override suspend fun updateThreadTitle(threadId: String, newTitle: String) = repository.updateThreadTitle(threadId, newTitle)
    override suspend fun insertFragment(fragment: MemoryFragment): Long = repository.insertFragment(fragment)
    override suspend fun deleteFragment(id: Long) = repository.deleteFragment(id)
    override suspend fun pruneOldSyncedRecords(daysOld: Int): Int = repository.pruneOldSyncedRecords(daysOld)
    override suspend fun clear() = repository.clear()
}
