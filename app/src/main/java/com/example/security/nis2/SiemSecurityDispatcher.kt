package com.example.security.nis2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Model bezpečnostní události pro SIEM (Security Information and Event Management)
 */
data class SecurityEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val severity: String, // INFO, LOW, MEDIUM, HIGH, CRITICAL
    val component: String,
    val details: String,
    val clientIpOrOrigin: String = "127.0.0.1",
    val userSubject: String = "system"
) {
    /**
     * Převede událost do formátu CEF (Common Event Format) dle ArcSight/SIEM standardu.
     */
    fun toCefString(): String {
        val dateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date(timestamp))
        val cefSeverity = when (severity) {
            "CRITICAL" -> 10
            "HIGH" -> 8
            "MEDIUM" -> 5
            "LOW" -> 3
            else -> 1
        }
        return "CEF:0|OMNIS|CognitivePlatform|2026.1|$eventType|$eventType|$cefSeverity|rt=$dateIso src=$clientIpOrOrigin suser=$userSubject msg=$details"
    }

    /**
     * Převede událost do formátu Syslog dle RFC 5424.
     */
    fun toSyslogString(): String {
        val dateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date(timestamp))
        return "<134>1 $dateIso omnis-node $component $eventId - - [nis2@omnis eventType=\"$eventType\" severity=\"$severity\"] $details"
    }
}

/**
 * SiemSecurityDispatcher:
 * Asynchronní modul pro sběr klientských i serverových odchylek s přenosem do SIEM pro forenzní dokazování.
 */
object SiemSecurityDispatcher {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _events = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val events: StateFlow<List<SecurityEvent>> = _events.asStateFlow()

    private val _siemTransmittedCount = MutableStateFlow(0)
    val siemTransmittedCount: StateFlow<Int> = _siemTransmittedCount.asStateFlow()

    /**
     * Zaznamená bezpečnostní událost a asynchronně ji odešle do SIEM streamu.
     */
    fun logSecurityEvent(
        eventType: String,
        severity: String,
        details: String,
        component: String = "OMNIS-CORE",
        userSubject: String = "operator"
    ) {
        val event = SecurityEvent(
            eventType = eventType,
            severity = severity,
            component = component,
            details = details,
            userSubject = userSubject
        )

        val updated = _events.value.toMutableList()
        updated.add(0, event)
        if (updated.size > 200) {
            _events.value = updated.take(200)
        } else {
            _events.value = updated
        }

        scope.launch {
            dispatchToSiem(event)
        }
    }

    private suspend fun dispatchToSiem(event: SecurityEvent) {
        // Simulace asynchronního transportu přes TLS UDP/TCP Syslog / HTTPS CEF do SIEM clusteru
        kotlinx.coroutines.delay(50)
        _siemTransmittedCount.value += 1
    }

    fun clearEvents() {
        _events.value = emptyList()
        _siemTransmittedCount.value = 0
    }
}
