package com.example.ai.transparency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiActTransparencyTest {

    @Test
    fun testSteganographyWatermarkRoundtrip() {
        val originalText = "Architektonická dekompozice modulů OMNIS v souladu s požadavky EU AI Act."
        val watermarked = AiSteganographyEngine.embedWatermark(
            plainText = originalText,
            modelName = "gemini-3.1-pro-preview",
            timestamp = 1774300000000L
        )

        // Text should be visibly readable and not corrupted
        assertTrue(watermarked.contains("Architektonická"))
        assertTrue(watermarked.contains("OMNIS"))

        // Extraction test
        val extracted = AiSteganographyEngine.extractWatermark(watermarked)
        assertTrue(extracted.hasValidWatermark)
        assertEquals("gemini-3.1-pro-preview", extracted.modelName)
        assertEquals(1774300000000L, extracted.timestamp)
    }

    @Test
    fun testAiOriginDetector_identifiesVerifiedVsSuspiciousVsHuman() {
        // 1. Verified synthetic
        val watermarked = AiSteganographyEngine.embedWatermark("Ověřená syntetická odpověď systému.")
        val report1 = AiOriginDetector.analyzeContent(watermarked)
        assertEquals(ContentOriginType.VERIFIED_OMNIS_SYNTHETIC, report1.originType)
        assertTrue(report1.complianceStatus.contains("PLNĚ SHODNÉ"))

        // 2. Natural human
        val humanText = "Krátká lidská poznámka o stavu serveru."
        val report2 = AiOriginDetector.analyzeContent(humanText)
        assertEquals(ContentOriginType.NATURAL_HUMAN, report2.originType)

        // 3. Unverified suspicious AI
        val suspiciousText = "Jako jazykový model umělé inteligence nemám osobní pocity, ale mohu vám pomoci analyzovat následující text a strukturovat odpověď podle zadaných parametrů v promptu bez jakéhokoliv dalšího kontextu."
        val report3 = AiOriginDetector.analyzeContent(suspiciousText)
        assertEquals(ContentOriginType.UNVERIFIED_SUSPICIOUS_AI, report3.originType)
        assertTrue(report3.complianceStatus.contains("NON-COMPLIANT"))
    }

    @Test
    fun testC2paManifestGeneration() {
        val manifest = AiSteganographyEngine.generateC2paManifest("asset_001", "gemini-3.1-pro-preview")
        assertNotNull(manifest)
        assertEquals("1.3", manifest["c2pa_version"])
        assertEquals("OMNIS Platform / AI Act Art 50(2)", manifest["claim_generator"])
    }

    @Test
    fun testAiDisclosureFlow() {
        AiDisclosureManager.resetForTesting()
        assertFalse(AiDisclosureManager.isDisclosureConfirmed.value)

        // First check prompts disclosure and blocks direct sending
        val canProceed = AiDisclosureManager.checkOrPromptDisclosure()
        assertFalse(canProceed)
        assertTrue(AiDisclosureManager.showDisclosureDialog.value)
    }
}
