package com.example.defense

import org.junit.Assert.*
import org.junit.Test

class OpponentAuditEngineTest {

    @Test
    fun verifyResponse_highConfidence() {
        val prompt = "Analyzuj dopad nového systému na stabilitu infrastruktury."
        val answer = "Systém vykazuje vysokou stabilitu s minimálním rozptylem latence a plným pokrytím všech osmi kognitivních domén. Byla provedena kompletní verifikace tlumicích mechanizmů."
        val domainScores = mapOf(
            "Sys" to 0.88f, "Econ" to 0.82f, "Psych" to 0.85f, "Eco" to 0.80f,
            "Law" to 0.84f, "Sec" to 0.90f, "Phys" to 0.86f, "Soc" to 0.83f
        )

        val result = OpponentAuditEngine.verifyResponse(
            prompt = prompt,
            synthesizedAnswer = answer,
            domainScores = domainScores,
            isEcoMode = false
        )

        assertEquals(ReliabilityStatus.HIGH, result.status)
        assertTrue(result.confidenceScore >= 0.82f)
        assertTrue(result.detectedDiscrepancies.isEmpty())
    }

    @Test
    fun verifyResponse_lowConfidenceDueToUncertaintyAndMissingFinancials() {
        val prompt = "Jaká je přesná cena a rozpočet nového CSR projektu?"
        val answer = "Možná bude projekt vyžadovat nákladné investice, ale chybí informace a nejsem si jist."
        val domainScores = mapOf(
            "Sys" to 0.90f, "Econ" to 0.20f, "Psych" to 0.80f, "Eco" to 0.15f,
            "Law" to 0.85f, "Sec" to 0.90f, "Phys" to 0.80f, "Soc" to 0.80f
        )

        val result = OpponentAuditEngine.verifyResponse(
            prompt = prompt,
            synthesizedAnswer = answer,
            domainScores = domainScores,
            isEcoMode = false
        )

        assertEquals(ReliabilityStatus.LOW_WARNING, result.status)
        assertTrue(result.confidenceScore < 0.68f)
        assertTrue(result.detectedDiscrepancies.isNotEmpty())
        assertTrue(result.suggestedClarifications.isNotEmpty())
    }
}
