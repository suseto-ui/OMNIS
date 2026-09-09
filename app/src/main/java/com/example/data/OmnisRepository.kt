package com.example.data

import kotlinx.coroutines.flow.Flow

class OmnisRepository(private val dao: OmnisDao) {
    val allRecords: Flow<List<OmnisRecord>> = dao.getAllRecords()
    val latestRecord: Flow<OmnisRecord?> = dao.getLatestRecord()

    suspend fun insert(record: OmnisRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun clear() {
        dao.clearAll()
    }
}
