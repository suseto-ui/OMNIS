package com.example.engine

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TopologicalInferenceEngineTest {

    @Test
    fun `evaluateTopologicalMetrics calculates Euler characteristic and Betti numbers correctly`() {
        val domainScores = mapOf(
            "SYS" to 0.8f, "ECON" to 0.7f, "PSYCH" to 0.6f, "ECO" to 0.5f,
            "LAW" to 0.9f, "SEC" to 0.85f, "PHYS" to 0.75f, "SOC" to 0.65f
        )
        // With 8 vertices, 12 edges, 5 faces:
        // chi = 8 - 12 + 5 = 1
        // betti0 = 1
        // betti1 = max(0, 12 - 8 + 1) = 5
        val metrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(
            domainScores = domainScores,
            customNodesCount = 8,
            customEdgesCount = 12,
            customFacesCount = 5
        )

        assertEquals(8, metrics.vertices)
        assertEquals(12, metrics.edges)
        assertEquals(5, metrics.faces)
        assertEquals(1, metrics.eulerCharacteristic)
        assertEquals(1, metrics.betti0)
        assertEquals(5, metrics.betti1)
        assertTrue(metrics.shannonEntropy > 0.0f)
        assertTrue(metrics.isValid)
        assertNull(metrics.failureReason)
    }

    @Test
    fun `evaluateTopologicalMetrics detects singular topology when Euler characteristic is out of bounds`() {
        val domainScores = mapOf("SYS" to 0.5f)
        // With 2 vertices, 15 edges, 0 faces: chi = 2 - 15 + 0 = -13 (!in -4..6)
        val metrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(
            domainScores = domainScores,
            customNodesCount = 2,
            customEdgesCount = 15,
            customFacesCount = 0
        )
        assertFalse(metrics.isValid)
        assertNotNull(metrics.failureReason)
        assertTrue(metrics.failureReason!!.contains("Eulerova charakteristika"))
    }

    @Test
    fun `evalCascadeRisk correctly detects percolation threshold crossing`() {
        // Below critical percolation threshold (38%)
        val safeDomains = mapOf(
            "SYS" to "STABLE",
            "ECON" to "STABLE",
            "PSYCH" to "STABLE",
            "ECO" to "STABLE",
            "LAW" to "STABLE",
            "SEC" to "FAILED", // 1 out of 6 ~ 16.6% < 38%
            "PHYS" to "STABLE",
            "SOC" to "STABLE"
        )
        val safePercolation = TopologicalInferenceEngine.evalCascadeRisk(safeDomains)
        assertFalse(safePercolation.isCascadeRiskActive)
        assertEquals("SYSTEM_STABLE", safePercolation.statusMessage)

        // Above critical percolation threshold (4 out of 8 = 50% >= 38%)
        val failingDomains = mapOf(
            "SYS" to "FAILED",
            "ECON" to "STRESSED",
            "PSYCH" to "STRESSED",
            "ECO" to "FAILED",
            "LAW" to "STABLE",
            "SEC" to "STABLE",
            "PHYS" to "STABLE",
            "SOC" to "STABLE"
        )
        val criticalPercolation = TopologicalInferenceEngine.evalCascadeRisk(failingDomains)
        assertTrue(criticalPercolation.isCascadeRiskActive)
        assertEquals("CRITICAL_CASCADE_RISK_ACTIVATED", criticalPercolation.statusMessage)
        assertEquals(4, criticalPercolation.failingDomains)
        assertEquals(0.5f, criticalPercolation.failingRatio, 0.01f)
    }

    @Test
    fun `calculateTokenBudget dynamically adjusts compression rate based on system stress`() {
        val lowStress = TopologicalInferenceEngine.calculateTokenBudget(0.1f)
        assertEquals(0.70f, lowStress.compressionRate, 0.001f)
        assertEquals("MINIMAL", lowStress.forensicBudget)
        assertEquals("STANDARD", lowStress.synthesisDepth)

        val mediumStress = TopologicalInferenceEngine.calculateTokenBudget(0.45f)
        assertEquals(0.30f, mediumStress.compressionRate, 0.001f)
        assertEquals("MODERATE", mediumStress.forensicBudget)
        assertEquals("BALANCED", mediumStress.synthesisDepth)

        val highStress = TopologicalInferenceEngine.calculateTokenBudget(0.85f)
        assertEquals(0.0f, highStress.compressionRate, 0.001f)
        assertEquals("EXTENDED_FORENSIC", highStress.forensicBudget)
        assertEquals("DEEP_ANALYTICS", highStress.synthesisDepth)
    }

    @Test
    fun `verifyInferenceOutput enforces executive override with audit proof of necessity`() {
        val domainScores = mapOf("SYS" to 0.8f, "SEC" to 0.9f)
        val failingDomains = mapOf(
            "SYS" to "FAILED", "ECON" to "FAILED", "LAW" to "FAILED", "SEC" to "FAILED"
        )

        // Without override -> Glitch triggered
        val unverifiedResult = TopologicalInferenceEngine.verifyInferenceOutput(
            domainScores = domainScores,
            domainsStatus = failingDomains,
            systemStressSigma = 0.9f,
            hasExplicitOverride = false
        )
        assertTrue(unverifiedResult.isGlitchTriggered)
        assertFalse(unverifiedResult.isPhase2Verified)

        // With override -> Glitch suppressed, emergency audit justification attached
        val overrideResult = TopologicalInferenceEngine.verifyInferenceOutput(
            domainScores = domainScores,
            domainsStatus = failingDomains,
            systemStressSigma = 0.9f,
            hasExplicitOverride = true,
            overrideJustification = "Critical NIS2 incident response"
        )
        assertFalse(overrideResult.isGlitchTriggered)
        assertTrue(overrideResult.isPhase2Verified)
        assertTrue(overrideResult.proofOfNecessity!!.contains("OVERRIDE_ACTIVE"))
        assertTrue(overrideResult.proofOfNecessity!!.contains("Critical NIS2 incident response"))
    }
}
