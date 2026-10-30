package com.example.action

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centrální správa stability a jističů (Circuit Breaker)
 */
object ResilienceManager {
    enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

    private val _circuitState = MutableStateFlow(CircuitState.CLOSED)
    val circuitState = _circuitState.asStateFlow()

    private val _isChaosModeEnabled = MutableStateFlow(false)
    val isChaosModeEnabled = _isChaosModeEnabled.asStateFlow()

    fun setCircuitState(state: CircuitState) {
        _circuitState.value = state
    }

    fun toggleChaosMode(enabled: Boolean) {
        _isChaosModeEnabled.value = enabled
    }

    fun getStatusSummary(): String {
        return "Circuit: ${circuitState.value}, Chaos: ${if (isChaosModeEnabled.value) "ACTIVE" else "INACTIVE"}"
    }
}
