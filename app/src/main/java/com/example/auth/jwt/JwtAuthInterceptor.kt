package com.example.auth.jwt

import android.util.Log
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp Middleware pro ochranu API tras a automatické vkládání
 * autorizačního JWT tokenu (RFC 7519) do hlavičky 'Authorization: Bearer <jwt>'.
 *
 * Zajišťuje striktní kontrolu přístupu k chráněným API endpointům:
 * - Pokud token chybí, vypršel nebo je neplatný, automaticky zablokuje požadavek,
 *   vrátí lokální HTTP 401 Unauthorized / HTTP 403 Forbidden a vyvolá událost
 *   na [AuthEventBus], která odhlásí uživatele a přesměruje aplikaci do přihlašovací obrazovky.
 */
class JwtAuthInterceptor(
    private val tokenProvider: () -> String? = { OmnisAuthService.currentSession.value?.token },
    private val enforceStrictRouteProtection: Boolean = true
) : Interceptor {

    companion object {
        private const val TAG = "JwtAuthInterceptor"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "

        // Seznam chráněných koncových tras (API routes requiring authentication)
        val PROTECTED_PATHS = listOf(
            "/api/v1/omnis",
            "/api/protected",
            "/api/admin",
            "/v1beta/models",
            "/api/stabilize",
            "/api/synthesize"
        )

        val ADMIN_ONLY_PATHS = listOf(
            "/api/admin",
            "/api/system/override"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val token = tokenProvider()

        val isProtected = isRouteProtected(path)
        val isAdminRoute = isAdminOnlyRoute(path)

        // Pokud je trasa chráněna
        if (isProtected) {
            if (token.isNullOrBlank()) {
                Log.w(TAG, "Neautorizovaný přístup k chráněné trase $path - chybí JWT token.")
                if (enforceStrictRouteProtection) {
                    AuthEventBus.emit(
                        AuthEvent.Unauthorized(
                            path = path,
                            reason = "Chybí platný JWT token pro přístup k chráněnému endpointu $path",
                            statusCode = 401
                        )
                    )
                    return createUnauthorizedResponse(chain, 401, "Neautorizovaný přístup: Chybí platný JWT token.")
                }
            } else {
                // Validace integrity tokenu
                when (val validation = JwtTokenManager.validateToken(token)) {
                    is JwtTokenManager.JwtValidationResult.Valid -> {
                        // Kontrola admin oprávnění pokud je vyžadováno
                        if (isAdminRoute && validation.claims.role != UserRole.ADMIN_OPERATOR.name) {
                            Log.w(TAG, "Odepřen přístup k admin trase $path pro roli ${validation.claims.role}.")
                            if (enforceStrictRouteProtection) {
                                AuthEventBus.emit(
                                    AuthEvent.AccessDenied(
                                        reason = "Přístup odepřen: Endpoint $path vyžaduje roli ADMIN_OPERATOR.",
                                        statusCode = 403
                                    )
                                )
                                return createUnauthorizedResponse(chain, 403, "Přístup odepřen: Vyžadována role ADMIN_OPERATOR.")
                            }
                        }
                    }
                    is JwtTokenManager.JwtValidationResult.Expired -> {
                        Log.w(TAG, "Expirovaný JWT token pro trasu $path.")
                        if (enforceStrictRouteProtection) {
                            AuthEventBus.emit(
                                AuthEvent.SessionExpired(
                                    reason = "Platnost JWT tokenu vypršela. Prosím přihlaste se znovu.",
                                    statusCode = 401
                                )
                            )
                            return createUnauthorizedResponse(chain, 401, "Token expiroval. Prosím přihlaste se znovu.")
                        }
                    }
                    is JwtTokenManager.JwtValidationResult.InvalidSignature,
                    is JwtTokenManager.JwtValidationResult.Malformed -> {
                        Log.w(TAG, "Neplatný podpis nebo poškozený JWT token pro trasu $path.")
                        if (enforceStrictRouteProtection) {
                            AuthEventBus.emit(
                                AuthEvent.SessionExpired(
                                    reason = "Neplatný kryptografický podpis nebo poškozený token.",
                                    statusCode = 401
                                )
                            )
                            return createUnauthorizedResponse(chain, 401, "Neplatný autentizační token.")
                        }
                    }
                }
            }
        }

        // Připojení hlavičky Authorization: Bearer <jwt> pokud máme token
        val builder = request.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
        }

        return chain.proceed(builder.build())
    }

    fun isRouteProtected(path: String): Boolean {
        return PROTECTED_PATHS.any { path.contains(it, ignoreCase = true) }
    }

    fun isAdminOnlyRoute(path: String): Boolean {
        return ADMIN_ONLY_PATHS.any { path.contains(it, ignoreCase = true) }
    }

    private fun createUnauthorizedResponse(
        chain: Interceptor.Chain,
        statusCode: Int,
        message: String
    ): Response {
        val json = "{\"error\": \"$message\", \"status\": $statusCode}"
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(if (statusCode == 401) "Unauthorized" else "Forbidden")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
