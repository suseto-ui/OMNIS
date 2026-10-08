package com.example.api

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiApiKeyDiagnosticsTest {

    @Test
    fun testKeyMasking_validKey() {
        val key = "AIzaSyAbcdef1234567890xyzQWERT"
        val masked = GeminiDiagnosticService.maskApiKey(key)
        assertEquals("AIzaSy...WERT", masked)
    }

    @Test
    fun testKeyMasking_emptyOrShort() {
        assertEquals("[NENÍ NASTAVEN]", GeminiDiagnosticService.maskApiKey(""))
        assertEquals("AB****", GeminiDiagnosticService.maskApiKey("ABCDEF"))
    }

    @Test
    fun testParseProbeResponse_200OkWithModels() {
        val json = """
            {
              "models": [
                { "name": "models/gemini-2.5-flash", "displayName": "Gemini 2.5 Flash" },
                { "name": "models/gemini-3.5-flash", "displayName": "Gemini 3.5 Flash" }
              ]
            }
        """.trimIndent()

        val report = GeminiDiagnosticService.parseProbeResponse(
            code = 200,
            bodyStr = json,
            maskedKey = "AIzaSy...WERT",
            keyLength = 39,
            latencyMs = 120L
        )

        assertEquals(GeminiKeyStatus.VALID_ACTIVE, report.status)
        assertEquals(200, report.httpCode)
        assertEquals(2, report.discoveredModels.size)
        assertTrue(report.discoveredModels.contains("gemini-2.5-flash"))
        assertTrue(report.discoveredModels.contains("gemini-3.5-flash"))
        assertEquals("Aktivní (Standardní / Pay-as-you-go kvóta)", report.quotaState)
    }

    @Test
    fun testParseProbeResponse_402PrepaymentDepleted() {
        val json = """
            {
              "error": {
                "code": 402,
                "message": "Your prepayment credits are depleted. Please go to AI Studio to manage your project.",
                "status": "PAYMENT_REQUIRED"
              }
            }
        """.trimIndent()

        val report = GeminiDiagnosticService.parseProbeResponse(
            code = 402,
            bodyStr = json,
            maskedKey = "AIzaSy...WERT",
            keyLength = 39,
            latencyMs = 85L
        )

        assertEquals(GeminiKeyStatus.CREDITS_DEPLETED_402, report.status)
        assertEquals(402, report.httpCode)
        assertTrue(report.statusTitle.contains("402"))
        assertTrue(report.statusDescription.contains("vyčerpal předplacené kredity"))
    }

    @Test
    fun testParseProbeResponse_429RateLimited() {
        val json = """
            {
              "error": {
                "code": 429,
                "message": "Resource has been exhausted (e.g. check quota)."
              }
            }
        """.trimIndent()

        val report = GeminiDiagnosticService.parseProbeResponse(
            code = 429,
            bodyStr = json,
            maskedKey = "AIzaSy...WERT",
            keyLength = 39,
            latencyMs = 50L
        )

        assertEquals(GeminiKeyStatus.RATE_LIMITED_429, report.status)
        assertEquals(429, report.httpCode)
    }

    @Test
    fun testParseProbeResponse_400InvalidKey() {
        val json = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key."
              }
            }
        """.trimIndent()

        val report = GeminiDiagnosticService.parseProbeResponse(
            code = 400,
            bodyStr = json,
            maskedKey = "AIzaSy...WERT",
            keyLength = 39,
            latencyMs = 45L
        )

        assertEquals(GeminiKeyStatus.INVALID_KEY_400_403, report.status)
        assertEquals(400, report.httpCode)
    }

    @Test
    fun testPingLogsManagement() {
        GeminiDiagnosticService.clearLogs()
        assertEquals(0, GeminiDiagnosticService.pingLogs.value.size)
    }

    @Test
    fun testValidateBuildConfigKeyFormat_placeholderGivesCritical() {
        val res = GeminiDiagnosticService.validateBuildConfigKeyFormat("MY_GEMINI_API_KEY")
        assertEquals(WarningLevel.CRITICAL, res.warningLevel)
        assertTrue(res.isPlaceholder)
        assertFalse(res.isValidFormat)
    }

    @Test
    fun testValidateBuildConfigKeyFormat_emptyGivesCritical() {
        val res = GeminiDiagnosticService.validateBuildConfigKeyFormat("")
        assertEquals(WarningLevel.CRITICAL, res.warningLevel)
        assertFalse(res.isPresent)
    }

    @Test
    fun testValidateBuildConfigKeyFormat_validStandardKeyGivesOk() {
        // Standard Google key is 39 chars starting with AIzaSy
        val standardKey = "AIzaSy" + "A".repeat(33)
        val res = GeminiDiagnosticService.validateBuildConfigKeyFormat(standardKey)
        assertEquals(WarningLevel.OK, res.warningLevel)
        assertTrue(res.isValidFormat)
        assertTrue(res.hasValidPrefix)
        assertTrue(res.hasStandardLength)
    }

    @Test
    fun testValidateBuildConfigKeyFormat_nonStandardPrefixOrLengthGivesWarning() {
        val nonStandardKey = "custom_key_12345"
        val res = GeminiDiagnosticService.validateBuildConfigKeyFormat(nonStandardKey)
        assertEquals(WarningLevel.WARNING, res.warningLevel)
        assertFalse(res.hasValidPrefix)
    }
}
