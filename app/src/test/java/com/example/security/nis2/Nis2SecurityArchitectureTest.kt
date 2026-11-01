package com.example.security.nis2

import com.example.auth.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Nis2SecurityArchitectureTest {

    @Test
    fun testOidcPkceAndTokenExchange() {
        // 1. PKCE challenge generation
        val (verifier, challenge) = OidcAuthService.generatePkceChallenge()
        assertNotNull(verifier)
        assertNotNull(challenge)
        assertTrue(verifier.isNotBlank())
        assertTrue(challenge.isNotBlank())

        // 2. Token exchange
        val token = OidcAuthService.exchangeCodeForTokens(
            authCode = "auth_code_xyz",
            username = "chief_security_officer",
            role = UserRole.ADMIN_OPERATOR
        )

        assertNotNull(token)
        assertTrue(token.accessToken.startsWith("oidc_jwt_access_"))
        assertEquals("chief_security_officer", token.userSubject)
        assertEquals(UserRole.ADMIN_OPERATOR, token.userRole)
        assertTrue(OidcAuthService.isOidcAuthenticated.value)

        // 3. Logout
        OidcAuthService.logout()
        assertFalse(OidcAuthService.isOidcAuthenticated.value)
    }

    @Test
    fun testMfaTotpGenerationAndClockDriftVerification() {
        val secret = MfaTotpEngine.generateSecretKey()
        assertNotNull(secret)
        assertEquals(40, secret.length) // 20 bytes in hex = 40 chars

        val now = System.currentTimeMillis()
        val code = MfaTotpEngine.generateTotpCode(secret, now)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })

        // Valid code at current timestamp
        assertTrue(MfaTotpEngine.verifyTotpCode(secret, code, now))

        // Valid within +/- 30s clock drift
        assertTrue(MfaTotpEngine.verifyTotpCode(secret, code, now + 20_000L))
        assertTrue(MfaTotpEngine.verifyTotpCode(secret, code, now - 20_000L))

        // Invalid code
        assertFalse(MfaTotpEngine.verifyTotpCode(secret, "000000", now))
        assertFalse(MfaTotpEngine.verifyTotpCode(secret, "123", now))
    }

    @Test
    fun testSiemEventCefAndSyslogFormatting() {
        val event = SecurityEvent(
            eventType = "UNAUTHORIZED_ADMIN_ACTION_ATTEMPT",
            severity = "HIGH",
            component = "OmnisActionDispatcher",
            details = "Uživatel se pokusil o nepovolený zápis do systémových tenzorů.",
            clientIpOrOrigin = "192.168.1.50",
            userSubject = "unauthorized_user"
        )

        val cef = event.toCefString()
        assertTrue(cef.startsWith("CEF:0|OMNIS|CognitivePlatform|2026.1|UNAUTHORIZED_ADMIN_ACTION_ATTEMPT"))
        assertTrue(cef.contains("src=192.168.1.50"))
        assertTrue(cef.contains("suser=unauthorized_user"))

        val syslog = event.toSyslogString()
        assertTrue(syslog.contains("<134>1"))
        assertTrue(syslog.contains("omnis-node OmnisActionDispatcher"))
        assertTrue(syslog.contains("eventType=\"UNAUTHORIZED_ADMIN_ACTION_ATTEMPT\""))
    }

    @Test
    fun testAssetInventoryAndNukibReport() {
        val assets = AssetInventoryManager.protectedAssets
        assertTrue(assets.isNotEmpty())
        assertTrue(assets.any { it.criticality == "CRITICAL" })

        val report = AssetInventoryManager.generateNukibRegistrationReport()
        assertTrue(report.contains("NÚKIB & NIS2 ASSET AUDIT REPORT"))
        assertTrue(report.contains("PLNĚ SHODNÉ (COMPLIANT)"))
    }

    @Test
    fun testNis2GovernanceRiskWarning() {
        // Without MFA -> should show warning and high penalty risk
        Nis2RiskGovernanceManager.updateRiskEvaluation(
            unresolvedVulnerabilities = 0,
            mfaEnabled = false,
            oidcActive = false
        )

        val risk = Nis2RiskGovernanceManager.riskSummary.value
        assertEquals("NON_COMPLIANT", risk.governanceStatus)
        assertTrue(risk.penaltyRiskScore > 0.5f)
        assertTrue(risk.statutoryOfficerWarning.contains("KRITICKÉ VAROVÁNÍ"))

        // With MFA and OIDC -> compliant
        Nis2RiskGovernanceManager.updateRiskEvaluation(
            unresolvedVulnerabilities = 0,
            mfaEnabled = true,
            oidcActive = true
        )

        val compliantRisk = Nis2RiskGovernanceManager.riskSummary.value
        assertEquals("COMPLIANT", compliantRisk.governanceStatus)
        assertTrue(compliantRisk.penaltyRiskScore <= 0.1f)
    }
}
