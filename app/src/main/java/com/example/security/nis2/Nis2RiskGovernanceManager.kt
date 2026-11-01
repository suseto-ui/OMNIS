package com.example.security.nis2

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Model manažerského hodnocení rizik a shody pro statutární orgány
 */
data class ManagementRiskSummary(
    val governanceStatus: String, // COMPLIANT, ACTION_REQUIRED, NON_COMPLIANT
    val statutoryOfficerWarning: String,
    val penaltyRiskScore: Float, // 0.0 (Zero risk) to 1.0 (Critical risk of sanctions/disqualification)
    val totalSecurityIncidents30d: Int,
    val mfaEnforcementCoveragePct: Float,
    val oidcMigrationProgressPct: Float,
    val lastExecutiveAuditTimestamp: String
)

/**
 * Nis2RiskGovernanceManager:
 * Připravuje vrcholný management organizace na jejich novou roli bezpečnostních garantů.
 * Poskytuje přehled architektury rizik pro prevenci finančních sankcí a zákazů činnosti statutárních orgánů.
 */
object Nis2RiskGovernanceManager {

    private val _riskSummary = MutableStateFlow(
        ManagementRiskSummary(
            governanceStatus = "COMPLIANT",
            statutoryOfficerWarning = "Všechna statutární opatření NIS2 a OIDC/MFA jsou aktivní. Nulové riziko personální diskvalifikace.",
            penaltyRiskScore = 0.05f,
            totalSecurityIncidents30d = 0,
            mfaEnforcementCoveragePct = 100.0f,
            oidcMigrationProgressPct = 100.0f,
            lastExecutiveAuditTimestamp = "2026-09-22 14:00:00"
        )
    )
    val riskSummary: StateFlow<ManagementRiskSummary> = _riskSummary.asStateFlow()

    fun updateRiskEvaluation(
        unresolvedVulnerabilities: Int,
        mfaEnabled: Boolean,
        oidcActive: Boolean
    ) {
        val score = if (!mfaEnabled || !oidcActive) 0.85f else if (unresolvedVulnerabilities > 0) 0.35f else 0.05f
        val status = if (score > 0.5f) "NON_COMPLIANT" else if (score > 0.2f) "ACTION_REQUIRED" else "COMPLIANT"
        val warning = if (score > 0.5f) {
            "KRITICKÉ VAROVÁNÍ: Absence MFA/OIDC vystavuje statutární orgány hrozbě zákazu činnosti dle zákona o kybernetické bezpečnosti."
        } else {
            "Všechna statutární opatření NIS2 jsou aktivní. Shoda s požadavky NÚKIB garantována."
        }

        _riskSummary.value = _riskSummary.value.copy(
            governanceStatus = status,
            statutoryOfficerWarning = warning,
            penaltyRiskScore = score,
            mfaEnforcementCoveragePct = if (mfaEnabled) 100f else 0f,
            oidcMigrationProgressPct = if (oidcActive) 100f else 0f
        )
    }
}
