package com.example.data

import kotlinx.coroutines.flow.Flow

class OmnisRepository(private val dao: OmnisDao) {
    val allRecords: Flow<List<OmnisRecord>> = dao.getAllRecords()
    val latestRecord: Flow<OmnisRecord?> = dao.getLatestRecord()

    suspend fun insert(record: OmnisRecord): Long {
        val rowId = dao.insertRecord(record)
        val recordWithId = record.copy(id = rowId)
        CloudSqlSyncManager.syncRecordAsync(recordWithId)
        return rowId
    }

    suspend fun clear() {
        dao.clearAll()
    }
}
