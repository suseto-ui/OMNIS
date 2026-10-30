package com.example.domain.usecase

import com.example.data.OmnisRecord
import com.example.telemetry.OmnisPhysicalTelemetryBridge
import com.example.ui.octagon.Omnis8dFusionEngine
import com.example.ui.octagon.Omnis8dVector

/**
 * Use Case pro deterministický výpočet a hybridní fúzi 8D kognitivní matice
 * s reálnou telemetrií mobilního zařízení (RAM, teplota, baterie, síť).
 */
class Compute8DMatrixUseCase {

    /**
     * Vypočítá hybridní 8D vektor pro daný záznam a telemetrický stav.
     */
    fun execute(
        record: OmnisRecord,
        telemetry: OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot
    ): Omnis8dVector {
        return Omnis8dFusionEngine.fuseRecord(record, telemetry)
    }

    /**
     * Spočítá vážený harmonický průměr pro eliminaci slabých míst (Leontief Weakest Link).
     */
    fun computeHarmonicMean(vector: Omnis8dVector, weights: Map<String, Float> = emptyMap()): Float {
        val defaultWeight = 1.0f
        val dims = listOf(
            "sys" to vector.sys,
            "econ" to vector.econ,
            "psych" to vector.psych,
            "eco" to vector.eco,
            "law" to vector.law,
            "sec" to vector.sec,
            "phys" to vector.phys,
            "soc" to vector.soc
        )

        var totalWeight = 0f
        var weightedReciprocalSum = 0f

        for ((key, value) in dims) {
            val w = weights[key] ?: defaultWeight
            val v = value.coerceIn(0.01f, 1.0f)
            totalWeight += w
            weightedReciprocalSum += (w / v)
        }

        return if (weightedReciprocalSum > 0f) totalWeight / weightedReciprocalSum else 0f
    }

    /**
     * Vypočítá exponenciální klouzavý průměr (EMA) pro vyhlazení časových řad.
     */
    fun computeEma(history: List<Omnis8dVector>, alpha: Float = Omnis8dFusionEngine.DEFAULT_EMA_ALPHA): Omnis8dVector? {
        return Omnis8dFusionEngine.computeEmaSeries(history, alpha).lastOrNull()
    }
}
