package com.example.monitoring

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TensorOversightTest {

    @Before
    fun setUp() {
        TensorMonitorService.clearLogs()
    }

    @Test
    fun testTensionThresholdCalculationAndSelfHealingTrigger() {
        // High confidence (0.95) -> Tension = 0.05 (< 0.3) -> should trigger autoregressive deep verification
        val shouldTriggerOnLowTension = TensorMonitorService.shouldTriggerSelfHealing(0.95f)
        assertTrue(shouldTriggerOnLowTension)
        assertTrue(TensorMonitorService.cognitiveTension < 0.3f)

        // Low confidence (0.50) -> Tension = 0.50, but score < 0.65 -> should also trigger self-healing
        val shouldTriggerOnLowConfidence = TensorMonitorService.shouldTriggerSelfHealing(0.50f)
        assertTrue(shouldTriggerOnLowConfidence)
        assertEquals(0.50f, TensorMonitorService.cognitiveTension, 0.01f)

        // Balanced confidence (0.70) -> Tension = 0.30 -> should not trigger self-healing
        val shouldNotTrigger = TensorMonitorService.shouldTriggerSelfHealing(0.70f)
        assertFalse(shouldNotTrigger)
    }

    @Test
    fun testRecordingOversightLogPreservesBeforeAndAfterMetrics() = runBlocking {
        val entry = TensorMonitorService.recordOversight(
            query = "Kinetika loužení mědi",
            originalContent = "Měď se louží kyselinou.",
            supplementedContent = "Měď se louží ve zředěné H2SO4 za přítomnosti Fe3+ oxidačních iontů při 40 °C.",
            confidenceBefore = 0.52f,
            confidenceAfter = 0.95f,
            correctionType = "Doplnění chemické kinetiky",
            triggerReason = "Kognitivní tenze < 0.3",
            latencyMs = 450L
        )

        assertNotNull(entry.id)
        assertTrue(entry.timestamp > 0)
        assertEquals("Kinetika loužení mědi", entry.query)
        assertEquals("Měď se louží kyselinou.", entry.originalContent)
        assertEquals(0.52f, entry.confidenceBefore, 0.01f)
        assertEquals(0.95f, entry.confidenceAfter, 0.01f)
        assertEquals(0.48f, entry.tensionBefore, 0.01f)
        assertEquals(0.05f, entry.tensionAfter, 0.01f)
        assertEquals(450L, entry.executionLatencyMs)

        val logs = TensorMonitorService.oversightLogs.first()
        assertTrue(logs.isNotEmpty())
        assertEquals(entry.id, logs.first().id)
    }

    @Test
    fun testClearLogsEmptiesOversightHistory() = runBlocking {
        TensorMonitorService.recordOversight(
            query = "Testovací dotaz",
            originalContent = "Původní text",
            supplementedContent = "Doplněný text",
            confidenceBefore = 0.50f,
            confidenceAfter = 0.90f
        )
        val logsBefore = TensorMonitorService.oversightLogs.first()
        assertTrue(logsBefore.isNotEmpty())

        TensorMonitorService.clearLogs()
        val logsAfter = TensorMonitorService.oversightLogs.first()
        assertTrue(logsAfter.isEmpty())
    }
}
