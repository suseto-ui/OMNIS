package com.example.scenario

import com.example.data.OmnisRecord
import kotlin.math.max
import kotlin.math.min

/**
 * Engine pro výpočet časových projekcí 8D matice
 */
object ForecastingEngine {

    /**
     * Vygeneruje časovou řadu (forecast) na základě aktuálního stavu a sady událostí
     */
    fun project(
        initialState: OmnisRecord,
        events: List<ScenarioEvent>,
        steps: Int = 10
    ): List<ForecastPoint> {
        val forecast = mutableListOf<ForecastPoint>()
        
        // Startovní bod
        var currentSys = initialState.valSys
        var currentEcon = initialState.valEcon
        var currentPsych = initialState.valPsych
        var currentEco = initialState.valEco
        var currentLaw = initialState.valLaw
        var currentSec = initialState.valSec
        var currentPhys = initialState.valPhys
        var currentSoc = initialState.valSoc

        forecast.add(ForecastPoint(0, currentSys, currentEcon, currentPsych, currentEco, currentLaw, currentSec, currentPhys, currentSoc))

        // Výpočet kroků
        for (i in 1..steps) {
            // Každá událost má kumulativní vliv rozložený v čase
            events.forEach { event ->
                val influenceFactor = 1.0f / steps
                currentSys = clamp(currentSys + event.impactSys * influenceFactor)
                currentEcon = clamp(currentEcon + event.impactEcon * influenceFactor)
                currentPsych = clamp(currentPsych + event.impactPsych * influenceFactor)
                currentEco = clamp(currentEco + event.impactEco * influenceFactor)
                currentLaw = clamp(currentLaw + event.impactLaw * influenceFactor)
                currentSec = clamp(currentSec + event.impactSec * influenceFactor)
                currentPhys = clamp(currentPhys + event.impactPhys * influenceFactor)
                currentSoc = clamp(currentSoc + event.impactSoc * influenceFactor)
            }
            
            // Přidání mírného náhodného driftu (entropie)
            currentSys = clamp(currentSys + (Math.random().toFloat() - 0.5f) * 0.01f)
            
            forecast.add(ForecastPoint(i, currentSys, currentEcon, currentPsych, currentEco, currentLaw, currentSec, currentPhys, currentSoc))
        }

        return forecast
    }

    private fun clamp(value: Float): Float = max(0.0f, min(1.0f, value))
}
