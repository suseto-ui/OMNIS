package com.example.auth

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/**
 * Výsledek pokusu o autentizaci
 */
sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}

/**
 * AuthenticationManager:
 * Centrální komponenta pro správu autentizace a autorizace.
 * Implementuje zásadu jediné odpovědnosti (SRP) a techniku 'Extract Method'
 * pro udržení cyklomatické složitosti každé metody pod hodnotou 3.
 */
class AuthenticationManager(
    private val strategyRegistry: UserAuthorizationStrategyRegistry = UserAuthorizationStrategyRegistry
) {
    companion object {
        const val PREFS_NAME = "omnis_auth_prefs"
        const val KEY_TOKEN = "session_token"
        const val KEY_USERNAME = "session_username"
        const val KEY_ROLE = "session_role"

        val instance: AuthenticationManager by lazy { AuthenticationManager() }
    }

    /**
     * Hlavní vstupní bod pro přihlášení uživatele.
     * Cyklomatická složitost: 2
     */
    fun login(
        context: Context,
        username: String,
        secret: String,
        roleRequested: UserRole
    ): AuthResult {
        val sanitizedUser = sanitizeInput(username) ?: return AuthResult.Failure("Uživatelské jméno nesmí být prázdné.")
        val strategy = strategyRegistry.getStrategy(roleRequested)

        if (!validateCredentialsWithStrategy(strategy, secret)) {
            return AuthResult.Failure("Neplatné přístupové oprávnění pro roli ${roleRequested.roleName}.")
        }

        val session = createSession(sanitizedUser, roleRequested)
        persistSession(context, session)
        return AuthResult.Success(session)
    }

    /**
     * Odhlášení a vymazání perzistentní relace.
     * Cyklomatická složitost: 1
     */
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Načtení existující relace z perzistentního úložiště.
     * Cyklomatická složitost: 2
     */
    fun restoreSession(context: Context): AuthSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val roleStr = prefs.getString(KEY_ROLE, null) ?: return null

        val role = parseRoleSafely(roleStr)
        return AuthSession(token = token, username = username, role = role)
    }

    /**
     * Ověření oprávnění k operaci skrze polymorfní strategii.
     * Cyklomatická složitost: 1
     */
    fun checkPermission(role: UserRole, permission: SystemPermission): Boolean {
        val strategy = strategyRegistry.getStrategy(role)
        return strategy.hasPermission(permission)
    }

    /**
     * Ověření možnosti spuštění konkrétní transakce.
     * Cyklomatická složitost: 1
     */
    fun canExecuteTransaction(role: UserRole, transactionType: String): Boolean {
        val strategy = strategyRegistry.getStrategy(role)
        return strategy.canExecuteTransaction(transactionType)
    }

    // =========================================================================
    // EXTRACT METHOD: Vysoce soudržné, malé privátní metody s CC = 1
    // =========================================================================

    private fun sanitizeInput(input: String): String? {
        val trimmed = input.trim()
        return if (trimmed.isBlank()) null else trimmed
    }

    private fun validateCredentialsWithStrategy(
        strategy: UserAuthorizationStrategy,
        secret: String
    ): Boolean {
        return strategy.validateCredentials(secret, ::hashSecret)
    }

    private fun createSession(username: String, role: UserRole): AuthSession {
        val token = generateToken()
        return AuthSession(
            token = token,
            username = username,
            role = role
        )
    }

    private fun generateToken(): String {
        return "omnis_jwt_${UUID.randomUUID()}_${System.currentTimeMillis()}"
    }

    private fun persistSession(context: Context, session: AuthSession) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_ROLE, session.role.name)
            .apply()
    }

    private fun parseRoleSafely(roleName: String): UserRole {
        return try {
            UserRole.valueOf(roleName)
        } catch (e: Exception) {
            UserRole.STANDARD_USER
        }
    }

    fun hashSecret(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
