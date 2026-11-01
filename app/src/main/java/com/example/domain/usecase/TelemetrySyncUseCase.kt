package com.example.domain.usecase

import com.example.telemetry.OmnisPhysicalTelemetryBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Use Case pro správu systémové telemetrie, plovoucího paměťového okna (max 60 vzorků)
 * a kritérií pro údržbu databáze.
 */
class TelemetrySyncUseCase {

    companion object {
        const val MAX_TELEMETRY_SAMPLES = 60
        const val PRUNE_THRESHOLD_DAYS_MS = 14L * 24L * 60L * 60L * 1000L // 14 dní
    }

    private val _telemetryHistory = MutableStateFlow<List<OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot>>(emptyList())
    val telemetryHistory: StateFlow<List<OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot>> = _telemetryHistory.asStateFlow()

    /**
     * Přidá nový vzorek telemetrie a udržuje plovoucí okno maximálně 60 vzorků.
     */
    fun recordSample(sample: OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot) {
        val currentList = _telemetryHistory.value.toMutableList()
        currentList.add(sample)
        if (currentList.size > MAX_TELEMETRY_SAMPLES) {
            currentList.removeAt(0)
        }
        _telemetryHistory.value = currentList
    }

    /**
     * Určí časové razítko pro prořezání starých synchronizovaných záznamů (starších 14 dnů).
     */
    fun getPruneCutoffTimestamp(): Long {
        return System.currentTimeMillis() - PRUNE_THRESHOLD_DAYS_MS
    }
}
