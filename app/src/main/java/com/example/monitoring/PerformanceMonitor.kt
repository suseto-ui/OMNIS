package com.example.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Motor pro sledování metriky a efektivity systému
 */
object PerformanceMonitor {
    private val _totalRequests = AtomicInteger(0)
    private val _totalBatches = AtomicInteger(0)
    private val _totalTokensSaved = AtomicLong(0)
    private val _averageLatencyMs = AtomicLong(0)
    private val _lastLatencyMs = MutableStateFlow(0L)

    private val _metricsUpdateTrigger = MutableStateFlow(0)
    val metricsUpdateTrigger = _metricsUpdateTrigger.asStateFlow()

    fun recordRequest(latencyMs: Long, tokensSaved: Long = 0, isBatch: Boolean = false) {
        _totalRequests.incrementAndGet()
        if (isBatch) _totalBatches.incrementAndGet()
        _totalTokensSaved.addAndGet(tokensSaved)
        
        // Jednoduchý klouzavý průměr latence
        val currentAvg = _averageLatencyMs.get()
        if (currentAvg == 0L) {
            _averageLatencyMs.set(latencyMs)
        } else {
            _averageLatencyMs.set((currentAvg + latencyMs) / 2)
        }
        
        _lastLatencyMs.value = latencyMs
        _metricsUpdateTrigger.value += 1
    }

    fun getTotalRequests() = _totalRequests.get()
    fun getTotalBatches() = _totalBatches.get()
    fun getTotalTokensSaved() = _totalTokensSaved.get()
    fun getAverageLatency() = _averageLatencyMs.get()
    fun getLastLatency() = _lastLatencyMs.value

    fun getEfficiencyRatio(): Float {
        val req = _totalRequests.get().toFloat()
        if (req == 0f) return 0f
        return (_totalBatches.get().toFloat() / req) * 100f
    }
}
