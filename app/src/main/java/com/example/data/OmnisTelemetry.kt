package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entita pro ukládání systémové telemetrie, logů a metrik výkonu.
 */
@Entity(tableName = "omnis_telemetry")
data class OmnisTelemetry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // INFO, WARNING, ERROR, COGNITIVE_DRIFT, PERFORMANCE
    val component: String, // Nexus, GeminiCore, ForecastingEngine, AGTO
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "{}" // JSON metadata (latency, token usage, etc.)
)
