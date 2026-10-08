package com.example.domain.security

import android.util.Log
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole

/**
 * Typy administrátorských a diagnostických funkcí v systému O.M.N.I.S.
 */
enum class AdminFeature(val title: String, val requiresStrictDebug: Boolean = false) {
    DEV_PROMPT_LAB("DevPromptLab & Šablony", requiresStrictDebug = false),
    DATABASE_PRUNING("Databázové prořezávání", requiresStrictDebug = false),
    STRESS_BENCHMARK("Zátěžové benchmarky", requiresStrictDebug = false),
    PRODUCTION_AUDIT("Produkční bezpečnostní audit", requiresStrictDebug = false),
    SYSTEM_TELEMETRY_LOGS("Surové telemetrické logy", requiresStrictDebug = false),
    DIRECT_SQL_CONSOLE("Přímá SQL konzole", requiresStrictDebug = true)
}

/**
 * Výsledek bezpečnostní autorizace.
 */
sealed class SecurityAccessResult {
    data object Granted : SecurityAccessResult()
    data class Denied(val reason: String) : SecurityAccessResult()
}

/**
 * Bezpečnostní strážce pro administrátorský a vývojářský režim.
 * Zajišťuje striktní izolaci citlivých operací v souladu s OWASP MASVS a Google Play zásadami.
 */
object AdminSecurityGuard {
    private const val TAG = "AdminSecurityGuard"

    /**
     * Ověřuje, zda má aktuální uživatel oprávnění přistupovat k dané administrátorské funkci.
     */
    fun checkAccess(feature: AdminFeature, role: UserRole = OmnisAuthService.currentUserRole.value): SecurityAccessResult {
        // 1. Ověření role
        if (role != UserRole.ADMIN_OPERATOR) {
            Log.w(TAG, "Přístup k ${feature.name} odmítnut: Uživatel nemá roli ADMIN_OPERATOR")
            return SecurityAccessResult.Denied("Pro přístup k této funkci je vyžadována autorizovaná role Admin / Operátor.")
        }

        // 2. Ověření aktivní session
        val session = OmnisAuthService.currentSession.value
        if (session == null || !OmnisAuthService.isAuthenticated.value) {
            Log.w(TAG, "Přístup k ${feature.name} odmítnut: Chybí platná session")
            return SecurityAccessResult.Denied("Přístup vyžaduje aktivní ověřenou relaci.")
        }

        return SecurityAccessResult.Granted
    }

    /**
     * Rychlá kontrola pro zobrazení admin komponent v UI.
     */
    fun isFeatureVisible(feature: AdminFeature, role: UserRole = OmnisAuthService.currentUserRole.value): Boolean {
        return checkAccess(feature, role) is SecurityAccessResult.Granted
    }
}
