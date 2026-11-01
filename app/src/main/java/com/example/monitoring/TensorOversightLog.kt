package com.example.monitoring

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Záznam o aktivaci autoregresivního kontrolního cyklu (Tenzorový dohled).
 * Zachycuje stav kognitivního toku před a po samo-opravném zásahu.
 */
@Immutable
@Serializable
data class TensorOversightLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val originalContent: String,
    val supplementedContent: String,
    val confidenceBefore: Float,
    val confidenceAfter: Float,
    val tensionBefore: Float,
    val tensionAfter: Float,
    val correctionType: String, // e.g. "Doplnění chybějících dat", "Technické zpřesnění", "Strukturální korekce"
    val triggerReason: String,  // e.g. "Kognitivní tenze klesla pod 0.3", "Nízká konfidence < 0.65"
    val executionLatencyMs: Long = 0L
)
