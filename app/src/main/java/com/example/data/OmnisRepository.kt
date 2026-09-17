package com.example.data

import kotlinx.coroutines.flow.Flow

class OmnisRepository(private val dao: OmnisDao) {
    val allRecords: Flow<List<OmnisRecord>> = dao.getAllRecords()
    val latestRecord: Flow<OmnisRecord?> = dao.getLatestRecord()

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
}
