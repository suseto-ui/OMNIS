package com.example.telemetry

import android.util.Log

object TelemetryEngine {
    fun log(type: String, component: String, message: String, metadata: String = "") {
        Log.i("Telemetry", "[$type] [$component] $message ($metadata)")
    }
}
