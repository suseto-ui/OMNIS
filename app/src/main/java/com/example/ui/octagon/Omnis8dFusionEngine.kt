package com.example.ui.octagon

import androidx.compose.ui.graphics.Color
import com.example.data.OmnisRecord
import com.example.telemetry.OmnisPhysicalTelemetryBridge
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 8D Vektor po hybridní fúzi sémantických a telemetrických dat.
 */
data class Omnis8dVector(
    val sys: Float,
    val econ: Float,
    val psych: Float,
    val eco: Float,
    val law: Float,
    val sec: Float,
    val phys: Float,
    val soc: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isFused: Boolean = true,
    val recordId: Long = 0L,
    val label: String = ""
) {
    val composite: Float
        get() = (sys + econ + psych + eco + law + sec + phys + soc) / 8f

    fun getValue(key: String): Float = when (key.lowercase()) {
        "sys" -> sys
        "econ" -> econ
        "psych" -> psych
        "eco" -> eco
        "law" -> law
        "sec" -> sec
        "phys" -> phys
        "soc" -> soc
        else -> composite
    }
}

enum class AnomalySeverity {
    LOW,
    MEDIUM,
    CRITICAL
}

data class AnomalyAlert(
    val dimensionKey: String,
    val dimensionName: String,
    val currentValue: Float,
    val meanValue: Float,
    val sigmaDiff: Float,
    val severity: AnomalySeverity,
    val recommendation: String,
    val color: Color
)

data class DomainFrictionPair(
    val dim1Key: String,
    val dim1Name: String,
    val dim2Key: String,
    val dim2Name: String,
    val val1: Float,
    val val2: Float,
    val frictionScore: Float, // 0.0f (perfektní synergie) - 1.0f (kritické tření)
    val status: String,
    val description: String,
    val recommendation: String
)

/**
 * Omnis8dFusionEngine:
 * Jádro pro hybridní fúzi LLM a hardware telemetrie, exponenciální klouzavý průměr (EMA),
 * detekci anomálií a křížové tření mezi doménami.
 */
object Omnis8dFusionEngine {

    const val DEFAULT_EMA_ALPHA = 0.35f
    const val ANOMALY_SIGMA_THRESHOLD = 2.0f

    /**
     * Provede hybridní fúzi sémantických odhadů (LLM) a reálné hardware telemetrie zařízení.
     */
    fun fuseRecord(
        record: OmnisRecord,
        telemetry: OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot
    ): Omnis8dVector {
        // 1. Sys (Systémové inž.): 60% sémantika LLM + 40% stav paměti RAM a systémového běhu
        val memoryStability = (1.0f - telemetry.memoryPressure).coerceIn(0.1f, 1.0f)
        val fusedSys = (record.valSys * 0.60f + memoryStability * 0.40f).coerceIn(0.05f, 1.0f)

        // 2. Econ (Ekonomie & Efektivita): 70% sémantika LLM + 30% energetická/datová úspora
        val energyEfficiency = if (telemetry.isCharging) 0.90f else (telemetry.batteryLevel * 0.85f).coerceIn(0.2f, 0.95f)
        val fusedEcon = (record.valEcon * 0.70f + energyEfficiency * 0.30f).coerceIn(0.05f, 1.0f)

        // 3. Psych (Kognice & Etika): 90% sémantika LLM + 10% stabilita operátora
        val fusedPsych = record.valPsych.coerceIn(0.05f, 1.0f)

        // 4. Eco (Ekologie): 50% sémantika LLM + 50% kalkulovaný ekologický vektor z telemetrie
        val fusedEco = (record.valEco * 0.50f + telemetry.calculatedEcoVector * 0.50f).coerceIn(0.05f, 1.0f)

        // 5. Law (Právo & Soulad): 90% sémantika LLM + 10% systémová compliance
        val fusedLaw = record.valLaw.coerceIn(0.05f, 1.0f)

        // 6. Sec (Zero-Trust): 70% sémantika LLM + 30% síťová bezpečnost (latence + zabezpečení)
        val networkSecurityScore = if (telemetry.isWifiConnected) 0.85f else 0.70f
        val fusedSec = (record.valSec * 0.70f + networkSecurityScore * 0.30f).coerceIn(0.05f, 1.0f)

        // 7. Phys (Termodynamika & Zátěž): 40% sémantika LLM + 60% reálná fyzikální zátěž hardwaru
        val fusedPhys = (record.valPhys * 0.40f + telemetry.calculatedPhysVector * 0.60f).coerceIn(0.05f, 1.0f)

        // 8. Soc (Sociální dopad): 90% sémantika LLM
        val fusedSoc = record.valSoc.coerceIn(0.05f, 1.0f)

        return Omnis8dVector(
            sys = fusedSys,
            econ = fusedEcon,
            psych = fusedPsych,
            eco = fusedEco,
            law = fusedLaw,
            sec = fusedSec,
            phys = fusedPhys,
            soc = fusedSoc,
            timestamp = record.timestamp,
            isFused = true,
            recordId = record.id,
            label = "ID #${record.id}"
        )
    }

    /**
     * Převede seznam záznamů na řadu hybridních fúzovaných 8D vektorů.
     */
    fun createFusionSeries(
        records: List<OmnisRecord>,
        telemetry: OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot
    ): List<Omnis8dVector> {
        val assistantRecords = records.filter { it.role == "assistant" }
        return assistantRecords.map { fuseRecord(it, telemetry) }
    }

    /**
     * Vypočítá exponenciální klouzavý průměr (EMA) pro vyhlazení časové řady.
     * EMA_t = alpha * X_t + (1 - alpha) * EMA_{t-1}
     */
    fun computeEmaSeries(
        series: List<Omnis8dVector>,
        alpha: Float = DEFAULT_EMA_ALPHA
    ): List<Omnis8dVector> {
        if (series.isEmpty()) return emptyList()

        val emaList = mutableListOf<Omnis8dVector>()
        var prev = series.first()
        emaList.add(prev)

        for (i in 1 until series.size) {
            val curr = series[i]
            val emaVector = Omnis8dVector(
                sys = alpha * curr.sys + (1f - alpha) * prev.sys,
                econ = alpha * curr.econ + (1f - alpha) * prev.econ,
                psych = alpha * curr.psych + (1f - alpha) * prev.psych,
                eco = alpha * curr.eco + (1f - alpha) * prev.eco,
                law = alpha * curr.law + (1f - alpha) * prev.law,
                sec = alpha * curr.sec + (1f - alpha) * prev.sec,
                phys = alpha * curr.phys + (1f - alpha) * prev.phys,
                soc = alpha * curr.soc + (1f - alpha) * prev.soc,
                timestamp = curr.timestamp,
                isFused = true,
                recordId = curr.recordId,
                label = "EMA #${curr.recordId}"
            )
            emaList.add(emaVector)
            prev = emaVector
        }

        return emaList
    }

    /**
     * Detekuje statistické anomálie (výkyvy > threshold sigma nebo kritické propady).
     */
    fun detectAnomalies(
        series: List<Omnis8dVector>,
        current: Omnis8dVector?
    ): List<AnomalyAlert> {
        if (current == null || series.size < 3) return emptyList()

        val alerts = mutableListOf<AnomalyAlert>()
        val dimKeys = listOf(
            Triple("Sys", "Systémové inž.", Color(0xFF60A5FA)),
            Triple("Econ", "Ekonomie", Color(0xFFFBBF24)),
            Triple("Psych", "Kognice & Etika", Color(0xFFC084FC)),
            Triple("Eco", "Ekologie", Color(0xFF34D399)),
            Triple("Law", "Právo & Soulad", Color(0xFFFB7185)),
            Triple("Sec", "Zero-Trust", Color(0xFFEF4444)),
            Triple("Phys", "Termodynamika", Color(0xFFFB923C)),
            Triple("Soc", "Sociální dopad", Color(0xFFF472B6))
        )

        for ((key, name, color) in dimKeys) {
            val values = series.map { it.getValue(key) }
            val mean = values.average().toFloat()
            val variance = values.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance).toFloat().coerceAtLeast(0.03f)

            val curVal = current.getValue(key)
            val diff = abs(curVal - mean)
            val sigma = diff / stdDev

            // Anomálie: signifikantní statistický výkyv NEBO kritický propad pod 0.25
            if (sigma >= ANOMALY_SIGMA_THRESHOLD || curVal < 0.25f) {
                val severity = when {
                    curVal < 0.20f || sigma >= 3.0f -> AnomalySeverity.CRITICAL
                    sigma >= 2.5f -> AnomalySeverity.MEDIUM
                    else -> AnomalySeverity.LOW
                }

                val rec = when (key) {
                    "Sys" -> "Vysoké vytížení systémových vláken / paměti. Doporučena garbage collection a snížení paralelních požadavků."
                    "Econ" -> "Neúměrná spotřeba tokenů nebo výpočetní energie. Zvažte zapnutí sémantické mezipaměti."
                    "Psych" -> "Pokles sémantické koherence odpovědi. Doporučeno zkontrolovat prompt v DevPromptLab."
                    "Eco" -> "Zvýšená energetická stopa. Zařízení není připojeno ke stabilnímu napájení s efektivní sítí."
                    "Law" -> "Možný rozpor s normami nebo bezpečnostními směrnicemi (NIS2/AI Act)."
                    "Sec" -> "Detekováno narušení bezpečnostního perimetru nebo nestabilní síťové spojení."
                    "Phys" -> "Vysoká teplota zařízení nebo nadměrná latence I/O. Nutná termodynamická stabilizace."
                    "Soc" -> "Potenciální polarizační drift výstupu. Proveďte dialektickou harmonizaci."
                    else -> "Doporučeno provést rekalibraci 8D kognitivního tenzoru."
                }

                alerts.add(
                    AnomalyAlert(
                        dimensionKey = key,
                        dimensionName = name,
                        currentValue = curVal,
                        meanValue = mean,
                        sigmaDiff = sigma,
                        severity = severity,
                        recommendation = rec,
                        color = color
                    )
                )
            }
        }

        return alerts
    }

    /**
     * Vyhodnotí tření (cross-domain friction) a synergie mezi klíčovými páry dimenzí.
     */
    fun evaluateFriction(vector: Omnis8dVector): List<DomainFrictionPair> {
        val pairs = listOf(
            // 1. Bezpečnost (Sec) vs. Psychologie / Uživatelská přívětivost (Psych)
            Pair("Sec" to "Zero-Trust", "Psych" to "Kognice & Důvěra") to { v: Omnis8dVector ->
                val diff = abs(v.sec - v.psych)
                val isHighSecLowPsych = v.sec > 0.8f && v.psych < 0.5f
                val friction = if (isHighSecLowPsych) diff * 1.2f else diff * 0.7f
                Triple(
                    friction.coerceIn(0f, 1f),
                    if (friction > 0.4f) "TŘENÍ (PŘÍSNÝ PROTOKOL)" else "SYNERGIE",
                    "Extrémní bezpečnostní restrikce mohou vést k frustraci operátora nebo degradaci UX."
                )
            },
            // 2. Termodynamika/Fyzická zátěž (Phys) vs. Ekologie (Eco)
            Pair("Phys" to "Termodynamika", "Eco" to "Ekologie") to { v: Omnis8dVector ->
                val diff = abs(v.phys - v.eco)
                val friction = diff.coerceIn(0f, 1f)
                Triple(
                    friction,
                    if (friction > 0.35f) "ENERGETICKÁ DIVERGENCE" else "HARMONIE",
                    "Vysoká zátěž procesoru a baterie zhoršuje celkovou energetickou efektivitu modelu."
                )
            },
            // 3. Systémová modularita (Sys) vs. Ekonomická nákladovost (Econ)
            Pair("Sys" to "Systémové inž.", "Econ" to "Ekonomie") to { v: Omnis8dVector ->
                val diff = abs(v.sys - v.econ)
                val friction = (diff * 0.8f).coerceIn(0f, 1f)
                Triple(
                    friction,
                    if (friction > 0.4f) "STRUKTURÁLNÍ NÁKLADY" else "OPTIMÁLNÍ VÝKON",
                    "Komplexní modulární pipeline zvyšuje spotřebu zdrojů a režii dotazování."
                )
            },
            // 4. Legislativa (Law) vs. Sociální dynamika (Soc)
            Pair("Law" to "Právo & Soulad", "Soc" to "Sociální dopad") to { v: Omnis8dVector ->
                val diff = abs(v.law - v.soc)
                val friction = diff.coerceIn(0f, 1f)
                Triple(
                    friction,
                    if (friction > 0.45f) "REGULAČNÍ ROZPOR" else "SPOLEČENSKÝ KONSENZUS",
                    "Právní omezení mohou kolidovat s požadavky na dynamickou adaptaci v sociálním kontextu."
                )
            }
        )

        return pairs.map { (pairDefs, evaluator) ->
            val (dim1, dim2) = pairDefs
            val (fScore, status, desc) = evaluator(vector)
            val rec = if (fScore > 0.4f) {
                "Doporučena syntéza kompromisního profilu a aplikace multi-doménové harmonizace."
            } else {
                "Dimenze pracují ve vzájemné synergii bez kritického napětí."
            }

            DomainFrictionPair(
                dim1Key = dim1.first,
                dim1Name = dim1.second,
                dim2Key = dim2.first,
                dim2Name = dim2.second,
                val1 = vector.getValue(dim1.first),
                val2 = vector.getValue(dim2.first),
                frictionScore = fScore,
                status = status,
                description = desc,
                recommendation = rec
            )
        }
    }
}
