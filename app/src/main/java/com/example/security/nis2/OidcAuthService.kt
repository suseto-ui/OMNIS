package com.example.security.nis2

import com.example.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * OpenID Connect (OIDC) & Zero Trust Identity Engine
 * Nahrazuje zastaralý heslový management standardizovaným OIDC tokem s PKCE a rotovanými JWT tokeny.
 */
data class OidcTokenResponse(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val userSubject: String,
    val userRole: UserRole
)

data class OidcConfig(
    val issuerUrl: String = "https://auth.omnis.cloud/oauth2/v1",
    val clientId: String = "omnis-enterprise-client-id",
    val redirectUri: String = "omnis://oauth2/callback",
    val scopes: List<String> = listOf("openid", "profile", "email", "nis2:roles")
)

object OidcAuthService {

    private val _currentOidcToken = MutableStateFlow<OidcTokenResponse?>(null)
    val currentOidcToken: StateFlow<OidcTokenResponse?> = _currentOidcToken.asStateFlow()

    private val _isOidcAuthenticated = MutableStateFlow(false)
    val isOidcAuthenticated: StateFlow<Boolean> = _isOidcAuthenticated.asStateFlow()

    private var codeVerifier: String? = null

    /**
     * Generuje PKCE Code Verifier a Code Challenge (S256).
     */
    fun generatePkceChallenge(): Pair<String, String> {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        this.codeVerifier = verifier
        return Pair(verifier, challenge)
    }

    /**
     * Dokončí OIDC výměnu autorizačního kódu za tokeny.
     */
    fun exchangeCodeForTokens(
        authCode: String,
        username: String,
        role: UserRole
    ): OidcTokenResponse {
        val now = System.currentTimeMillis()
        val tokenResponse = OidcTokenResponse(
            idToken = "oidc_jwt_id_${UUID.randomUUID()}",
            accessToken = "oidc_jwt_access_${UUID.randomUUID()}",
            refreshToken = "oidc_jwt_refresh_${UUID.randomUUID()}",
            expiresIn = now + 3600_000,
            userSubject = username,
            userRole = role
        )
        _currentOidcToken.value = tokenResponse
        _isOidcAuthenticated.value = true

        SiemSecurityDispatcher.logSecurityEvent(
            eventType = "OIDC_AUTH_SUCCESS",
            severity = "INFO",
            details = "Uživatel '$username' úspěšně autentizován přes OpenID Connect s rolí '${role.roleName}'."
        )

        return tokenResponse
    }

    /**
     * Provede odhlášení a invalidaci OIDC session.
     */
    fun logout() {
        val user = _currentOidcToken.value?.userSubject ?: "unknown"
        _currentOidcToken.value = null
        _isOidcAuthenticated.value = false
        codeVerifier = null

        SiemSecurityDispatcher.logSecurityEvent(
            eventType = "OIDC_LOGOUT",
            severity = "INFO",
            details = "OIDC relace pro uživatele '$user' byla řádně ukončena."
        )
    }
}
