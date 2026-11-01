package com.example.monitoring

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Služba pro monitorování tenzorové konfidence výstupů AI
 * a správa historie autoregresivních dohledů (Tenzorový dohled).
 */
object TensorMonitorService {
    // Aktuální konfidenční skóre (0.0 - 1.0)
    val confidenceScore: MutableState<Float> = mutableStateOf(1.0f)

    // Kognitivní tenze (inverzní ke konfidenci)
    val cognitiveTension: Float get() = 1.0f - confidenceScore.value

    private val _oversightLogs = MutableStateFlow<List<TensorOversightLog>>(createInitialDemoLogs())
    val oversightLogs: StateFlow<List<TensorOversightLog>> = _oversightLogs.asStateFlow()

    /**
     * Vyhodnotí, zda je nutná samo-oprava (autoregresivní kontrolní cyklus).
     */
    fun shouldTriggerSelfHealing(score: Float): Boolean {
        confidenceScore.value = score
        val tension = 1.0f - score
        // Spustit autoregresivní cyklus, pokud tenze klesne pod 0.3 nebo je celková konfidence nízká (< 0.65)
        return tension < 0.3f || score < 0.65f 
    }

    /**
     * Zaznamená událost tenzorového dohledu do auditní historie.
     */
    fun recordOversight(
        query: String,
        originalContent: String,
        supplementedContent: String,
        confidenceBefore: Float,
        confidenceAfter: Float,
        correctionType: String = "Doplnění chybějících dat",
        triggerReason: String = "Kognitivní tenze klesla pod 0.3",
        latencyMs: Long = 420L
    ): TensorOversightLog {
        val entry = TensorOversightLog(
            query = query,
            originalContent = originalContent,
            supplementedContent = supplementedContent,
            confidenceBefore = confidenceBefore,
            confidenceAfter = confidenceAfter,
            tensionBefore = 1.0f - confidenceBefore,
            tensionAfter = 1.0f - confidenceAfter,
            correctionType = correctionType,
            triggerReason = triggerReason,
            executionLatencyMs = latencyMs
        )
        _oversightLogs.value = listOf(entry) + _oversightLogs.value
        confidenceScore.value = confidenceAfter
        return entry
    }

    /**
     * Vyčistí historii dohledů.
     */
    fun clearLogs() {
        _oversightLogs.value = emptyList()
    }

    private fun createInitialDemoLogs(): List<TensorOversightLog> {
        val now = System.currentTimeMillis()
        return listOf(
            TensorOversightLog(
                id = "ovs_demo_1",
                timestamp = now - 180000L,
                query = "Recyklace vzácných zemin a proces hydrometalurgie",
                originalContent = "Hydrometalurgie využívá kyselé loužení pro extrakci kovů z e-waste.",
                supplementedContent = "Hydrometalurgický cyklus provádí selektivní loužení pomocí 2M H2SO4 s přídavkem H2O2 při 65 °C. Následuje kapalinová extrakce organickými fosfáty (D2EHPA) pro selektivní separaci Nd, Dy a Pr s výtěžností 94.8 %.",
                confidenceBefore = 0.52f,
                confidenceAfter = 0.94f,
                tensionBefore = 0.48f,
                tensionAfter = 0.06f,
                correctionType = "Technické zpřesnění a chemická kinetika",
                triggerReason = "Kognitivní tenze < 0.3 (detekována povrchní generalizace)",
                executionLatencyMs = 620L
            ),
            TensorOversightLog(
                id = "ovs_demo_2",
                timestamp = now - 720000L,
                query = "Ověření statutární odpovědnosti dle NIS2",
                originalContent = "NIS2 vyžaduje zabezpečení sítě a hlášení incidentů.",
                supplementedContent = "Dle čl. 20 a 21 odst. 2 směrnice NIS2 odpovídá statutární orgán osobně za schválení a dohled nad řízením kybernetických rizik. Sankce dosahují až 10 mil. EUR nebo 2 % celosvětového obratu s možností dočasného zákazu výkonu řídící funkce.",
                confidenceBefore = 0.58f,
                confidenceAfter = 0.96f,
                tensionBefore = 0.42f,
                tensionAfter = 0.04f,
                correctionType = "Doplnění právních citací a paragrafů",
                triggerReason = "Kognitivní tenze < 0.3 (absence právních kotev)",
                executionLatencyMs = 540L
            )
        )
    }
}
