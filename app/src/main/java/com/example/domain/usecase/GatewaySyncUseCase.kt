package com.example.domain.usecase

import com.example.data.OmnisDao
import com.example.data.OmnisRecord
import com.example.data.sync.OmnisApiGatewayClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean Architecture Use Case pro asynchronní synchronizaci neodbavených dat z mobilního klienta
 * do PostgreSQL přes zabezpečenou REST API Gateway.
 */
class GatewaySyncUseCase(
    private val apiGatewayClient: OmnisApiGatewayClient = OmnisApiGatewayClient
) {
    suspend fun syncPendingRecords(records: List<OmnisRecord>): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            apiGatewayClient.syncBatchToGateway(records)
        } catch (e: Exception) {
            Pair(0, "Chyba synchronizace: ${e.message}")
        }
    }

    suspend fun pingGateway(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            apiGatewayClient.pingGateway()
        } catch (e: Exception) {
            Pair(false, "Chyba připojení: ${e.message}")
        }
    }
}
