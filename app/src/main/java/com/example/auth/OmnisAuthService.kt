package com.example.auth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RBAC Role Schéma:
 * Delegováno na polymorfní UserAuthorizationStrategy.
 */
enum class UserRole(val roleName: String, val level: Int) {
    ADMIN_OPERATOR("Admin / Operátor", 2),
    STANDARD_USER("Běžný Uživatel", 1);

    fun canAccessSystemActions(): Boolean =
        UserAuthorizationStrategyRegistry.getStrategy(this).canAccessSystemActions()

    fun canAccessDevDiagnostic(): Boolean =
        UserAuthorizationStrategyRegistry.getStrategy(this).canAccessDevDiagnostic()

    fun canModifySystemState(): Boolean =
        UserAuthorizationStrategyRegistry.getStrategy(this).canModifySystemState()
}

data class AuthSession(
    val token: String,
    val username: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)

object OmnisAuthService {
    private val authManager = AuthenticationManager.instance

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val _currentUserRole = MutableStateFlow(UserRole.STANDARD_USER)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun init(context: Context) {
        val session = authManager.restoreSession(context)
        if (session != null) {
            _currentSession.value = session
            _currentUserRole.value = session.role
            _isAuthenticated.value = true
        }
    }

    /**
     * Autentizace uživatele s tokenovou generací a RBAC autorizací skrze Strategy Pattern.
     */
    fun login(context: Context, username: String, secret: String, roleRequested: UserRole): Boolean {
        return when (val result = authManager.login(context, username, secret, roleRequested)) {
            is AuthResult.Success -> {
                _currentSession.value = result.session
                _currentUserRole.value = result.session.role
                _isAuthenticated.value = true
                true
            }
            is AuthResult.Failure -> false
        }
    }

    fun logout(context: Context) {
        authManager.logout(context)
        _currentSession.value = null
        _currentUserRole.value = UserRole.STANDARD_USER
        _isAuthenticated.value = false
    }

    fun setRoleForTesting(role: UserRole) {
        _currentUserRole.value = role
        _isAuthenticated.value = true
    }
}
