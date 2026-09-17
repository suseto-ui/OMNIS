package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

/**
 * RBAC Role Schéma:
 * - ADMIN_OPERATOR: Plný přístup ke všem systémovým stavům, spouštění systémových akcí, diagnostice,
 *   správě paměti, nastavení tenzorů a vývojářským panelům.
 * - STANDARD_USER: Omezená práva pro standardní uživatelskou kognitivní interakci bez přístupu
 *   k systémovým stavům, nebezpečným příkazům a konfiguračním endpointům.
 */
enum class UserRole(val roleName: String, val level: Int) {
    ADMIN_OPERATOR("Admin / Operátor", 2),
    STANDARD_USER("Běžný Uživatel", 1);

    fun canAccessSystemActions(): Boolean = this == ADMIN_OPERATOR
    fun canAccessDevDiagnostic(): Boolean = this == ADMIN_OPERATOR
    fun canModifySystemState(): Boolean = this == ADMIN_OPERATOR
}

data class AuthSession(
    val token: String,
    val username: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)

object OmnisAuthService {
    private const val PREFS_NAME = "omnis_auth_prefs"
    private const val KEY_TOKEN = "session_token"
    private const val KEY_USERNAME = "session_username"
    private const val KEY_ROLE = "session_role"

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val _currentUserRole = MutableStateFlow(UserRole.STANDARD_USER)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val username = prefs.getString(KEY_USERNAME, null)
        val roleStr = prefs.getString(KEY_ROLE, null)

        if (token != null && username != null && roleStr != null) {
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                UserRole.STANDARD_USER
            }
            val session = AuthSession(token = token, username = username, role = role)
            _currentSession.value = session
            _currentUserRole.value = role
            _isAuthenticated.value = true
        }
    }

    /**
     * Autentizace uživatele s tokenovou generací a RBAC autorizací.
     * Operátorské heslo je kryptograficky ověřeno (SHA-256 hash).
     */
    fun login(context: Context, username: String, secret: String, roleRequested: UserRole): Boolean {
        val trimmedUser = username.trim()
        val trimmedSecret = secret.trim()

        if (trimmedUser.isBlank()) return false

        val assignedRole = if (roleRequested == UserRole.ADMIN_OPERATOR) {
            // Ověření operátorského přístupového klíče
            val hash = hashSecret(trimmedSecret)
            // Default klíč operátora: omnis2026 nebo root_operator
            val validHashes = setOf(
                hashSecret("omnis2026"),
                hashSecret("root_operator"),
                hashSecret("admin")
            )
            if (validHashes.contains(hash)) {
                UserRole.ADMIN_OPERATOR
            } else {
                return false
            }
        } else {
            UserRole.STANDARD_USER
        }

        val sessionToken = "omnis_jwt_${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val session = AuthSession(
            token = sessionToken,
            username = trimmedUser,
            role = assignedRole
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_ROLE, session.role.name)
            .apply()

        _currentSession.value = session
        _currentUserRole.value = assignedRole
        _isAuthenticated.value = true
        return true
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _currentSession.value = null
        _currentUserRole.value = UserRole.STANDARD_USER
        _isAuthenticated.value = false
    }

    private fun hashSecret(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
