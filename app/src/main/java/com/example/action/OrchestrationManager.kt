package com.example.action

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Správa orchestrace a optimalizace tokenů (Smart Batching)
 */
object OrchestrationManager {
    private val _isSmartBatchingEnabled = MutableStateFlow(false)
    val isSmartBatchingEnabled = _isSmartBatchingEnabled.asStateFlow()

    private val _batchingWindowMs = MutableStateFlow(1500L) // 1.5 sekundy okno
    val batchingWindowMs = _batchingWindowMs.asStateFlow()

    fun toggleSmartBatching(enabled: Boolean) {
        _isSmartBatchingEnabled.value = enabled
    }

    fun setBatchingWindow(windowMs: Long) {
        _batchingWindowMs.value = windowMs
    }

    fun getStatusSummary(): String {
        return "Batching: ${if (isSmartBatchingEnabled.value) "ACTIVE (${batchingWindowMs.value}ms)" else "DISABLED"}"
    }
}
