package com.example.auth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _authNotice = MutableStateFlow<String?>(null)
    val authNotice: StateFlow<String?> = _authNotice.asStateFlow()

    private var isEventBusListening = false

    fun clearAuthNotice() {
        _authNotice.value = null
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        val session = authManager.restoreSession(appContext)
        if (session != null) {
            _currentSession.value = session
            _currentUserRole.value = session.role
            _isAuthenticated.value = true
        }

        if (!isEventBusListening) {
            isEventBusListening = true
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                com.example.auth.jwt.AuthEventBus.events.collect { event ->
                    when (event) {
                        is com.example.auth.jwt.AuthEvent.SessionExpired -> {
                            logout(appContext)
                            _authNotice.value = "Relace ukončena (401): ${event.reason}"
                        }
                        is com.example.auth.jwt.AuthEvent.Unauthorized -> {
                            logout(appContext)
                            _authNotice.value = "Neautorizovaný přístup (401) k ${event.path}: ${event.reason}"
                        }
                        is com.example.auth.jwt.AuthEvent.AccessDenied -> {
                            logout(appContext)
                            _authNotice.value = "Přístup zamítnut (403): ${event.reason}"
                        }
                    }
                }
            }
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

    /**
     * Bezpečné přihlášení s ověřením PBKDF2 soleného hesla z Room databáze a vystavením JWT.
     */
    suspend fun loginWithDb(
        context: Context,
        username: String,
        secret: String,
        roleRequested: UserRole? = null
    ): AuthResult {
        val result = UserAuthService.instance.login(context, username, secret, roleRequested)
        if (result is AuthResult.Success) {
            _currentSession.value = result.session
            _currentUserRole.value = result.session.role
            _isAuthenticated.value = true
        }
        return result
    }

    /**
     * Registrace nového uživatele s unikátním jménem, PBKDF2 hashovaným heslem a vystavením JWT.
     */
    suspend fun register(
        context: Context,
        username: String,
        secret: String,
        roleRequested: UserRole = UserRole.STANDARD_USER
    ): RegistrationResult {
        val result = UserAuthService.instance.register(context, username, secret, roleRequested)
        if (result is RegistrationResult.Success) {
            val session = AuthSession(
                token = result.token,
                username = result.user.username,
                role = roleRequested
            )
            _currentSession.value = session
            _currentUserRole.value = session.role
            _isAuthenticated.value = true
        }
        return result
    }

    fun logout(context: Context) {
        UserAuthService.instance.logout(context)
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
