package com.example.telemetry

import android.util.Log
import com.example.data.OmnisDao
import com.example.data.OmnisTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Engine pro sběr a ukládání systémové telemetrie.
 */
object TelemetryEngine {
    private var dao: OmnisDao? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(omnisDao: OmnisDao) {
        dao = omnisDao
        log("INFO", "TelemetryEngine", "Inicializován Telemetry Engine.")
    }

    fun log(type: String, component: String, message: String, metadata: String = "{}") {
        Log.d("OMNIS_TELEMETRY", "[$type] $component: $message")
        scope.launch {
            dao?.insertTelemetry(
                OmnisTelemetry(
                    type = type,
                    component = component,
                    message = message,
                    metadata = metadata
                )
            )
        }
    }

    fun logError(component: String, message: String, e: Throwable? = null) {
        val fullMessage = if (e != null) "$message | Exception: ${e.message}" else message
        log("ERROR", component, fullMessage)
    }
}
