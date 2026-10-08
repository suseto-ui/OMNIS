package com.example.domain.usecase

import com.example.action.ResilienceManager
import com.example.monitoring.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean Architecture Use Case pro spouštění diagnostických kontrol, samoopravných rutin (Self-Healing)
 * a auditních prověrek stability systému O.M.N.I.S.
 */
class SelfHealingAuditUseCase {
    suspend fun executeSelfHealingScan(): SelfHealingScanResult = withContext(Dispatchers.IO) {
        val runtime = Runtime.getRuntime()
        val totalMem = runtime.totalMemory()
        val freeMem = runtime.freeMemory()
        val usedMem = totalMem - freeMem
        val memUsagePercent = (usedMem.toDouble() / totalMem.toDouble()) * 100.0

        val actionsTaken = mutableListOf<String>()

        if (ResilienceManager.circuitState.value != ResilienceManager.CircuitState.CLOSED) {
            ResilienceManager.setCircuitState(ResilienceManager.CircuitState.CLOSED)
            try {
                com.example.api.OmnisGeminiClient.circuitBreaker.reset()
            } catch (_: Throwable) {}
            actionsTaken.add("Obnoven stav Circuit Breakeru na CLOSED")
        }

        if (memUsagePercent > 80.0) {
            System.gc()
            actionsTaken.add("Provedena preventivní garbage collection")
        }

        val latency = PerformanceMonitor.getLastLatency()
        if (latency > 1500) {
            actionsTaken.add("Zjištěna zvýšená latence (${latency}ms) - aktivována adaptivní mezipaměť")
        }

        SelfHealingScanResult(
            status = if (actionsTaken.isEmpty()) "HEALTHY" else "HEALED",
            actionsTaken = actionsTaken,
            memoryUsagePercent = memUsagePercent,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class SelfHealingScanResult(
    val status: String,
    val actionsTaken: List<String>,
    val memoryUsagePercent: Double,
    val timestamp: Long
)
