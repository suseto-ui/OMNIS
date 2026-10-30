package com.example.auth.jwt

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Události související s autentizací a ochranou API tras.
 */
sealed class AuthEvent {
    data class SessionExpired(val reason: String, val statusCode: Int = 401) : AuthEvent()
    data class AccessDenied(val reason: String, val statusCode: Int = 403) : AuthEvent()
    data class Unauthorized(val path: String, val reason: String, val statusCode: Int = 401) : AuthEvent()
}

/**
 * Událostní sběrnice (Event Bus) pro asynchronní notifikace o neautorizovaném přístupu
 * nebo vypršení platnosti JWT tokenu zachyceném v síťovém AuthInterceptor middleware.
 *
 * Umožňuje síťové vrstvě (OkHttp) bezpečně vyvolat reset session a přesměrování
 * uživatele do přihlašovací obrazovky bez přímé vazby na konkrétní UI komponenty.
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun emit(event: AuthEvent) {
        _events.tryEmit(event)
    }

    suspend fun emitSuspend(event: AuthEvent) {
        _events.emit(event)
    }
}
