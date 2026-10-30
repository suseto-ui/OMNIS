package com.example.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FÁZE XI (11.1): IoT & Physical Vector Telemetry Bridge.
 * Propojuje fyzický stav hardwaru (baterie, síťová latence, systémová zátěž)
 * s 8D kognitivní maticí O.M.N.I.S., dynamicky ovlivňuje dimenze val_phys a val_eco.
 */
object OmnisPhysicalTelemetryBridge {

    private const val TAG = "OmnisPhysicalBridge"

    data class PhysicalTelemetrySnapshot(
        val batteryLevel: Float, // 0.0f - 1.0f
        val isCharging: Boolean,
        val isWifiConnected: Boolean,
        val estimatedLatencyMs: Long,
        val memoryPressure: Float, // 0.0f - 1.0f (poměr využité RAM)
        val calculatedPhysVector: Float, // 0.0f - 1.0f
        val calculatedEcoVector: Float, // 0.0f - 1.0f
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _telemetryState = MutableStateFlow(
        PhysicalTelemetrySnapshot(
            batteryLevel = 1.0f,
            isCharging = true,
            isWifiConnected = true,
            estimatedLatencyMs = 25L,
            memoryPressure = 0.20f,
            calculatedPhysVector = 0.95f,
            calculatedEcoVector = 0.90f
        )
    )
    val telemetryState: StateFlow<PhysicalTelemetrySnapshot> = _telemetryState.asStateFlow()

    /**
     * Obnoví fyzickou telemetrii zařízení a přepočítá vliv na 8D matici.
     */
    fun refreshTelemetry(context: Context) {
        try {
            // 1. Stav baterie a nabíjení
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) level / scale.toFloat() else 0.85f

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            // 2. Stav konektivity
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            val latency = if (isWifi) 20L else 75L

            // 3. Stav RAM paměti
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryPressure = (usedMemory.toDouble() / maxMemory.toDouble()).toFloat().coerceIn(0f, 1f)

            // 4. Výpočet dopadu na 8D fyzický (val_phys) a ekologický (val_eco) vektor
            // Fyzický vektor: klesá při vysoké latenci, nedostatku RAM nebo nízké baterii
            val physScore = (
                (if (isWifi) 0.40f else 0.25f) +
                (1.0f - memoryPressure) * 0.35f +
                (batteryPct * 0.25f)
            ).coerceIn(0.20f, 0.98f)

            // Ekologický vektor: stoupá při nabíjení z efektivní sítě a nízké spotřebě RAM
            val ecoScore = (
                (if (isCharging) 0.50f else 0.35f) +
                (1.0f - memoryPressure) * 0.30f +
                (if (isWifi) 0.20f else 0.10f)
            ).coerceIn(0.20f, 0.98f)

            val snapshot = PhysicalTelemetrySnapshot(
                batteryLevel = batteryPct,
                isCharging = isCharging,
                isWifiConnected = isWifi,
                estimatedLatencyMs = latency,
                memoryPressure = memoryPressure,
                calculatedPhysVector = physScore,
                calculatedEcoVector = ecoScore
            )

            _telemetryState.value = snapshot

            TelemetryEngine.log(
                type = "PHYSICAL_TELEMETRY",
                component = "PhysicalBridge",
                message = "Fyzická telemetrie aktualizována: Phys=${(physScore * 100).toInt()}%, Eco=${(ecoScore * 100).toInt()}%, Baterie=${(batteryPct * 100).toInt()}%",
                metadata = "{\"phys\":$physScore, \"eco\":$ecoScore, \"battery\":$batteryPct, \"isCharging\":$isCharging, \"isWifi\":$isWifi}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Chyba při čtení fyzické telemetrie: ${e.message}")
        }
    }
}
