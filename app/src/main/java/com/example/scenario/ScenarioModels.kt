package com.example.scenario

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Reprezentace hypotetické události pro simulaci
 */
data class ScenarioEvent(
    val id: String,
    val name: String,
    val description: String,
    val impactSys: Float,
    val impactEcon: Float,
    val impactPsych: Float,
    val impactEco: Float,
    val impactLaw: Float,
    val impactSec: Float,
    val impactPhys: Float,
    val impactSoc: Float,
    val color: Color = OmnisCyan
)

/**
 * Výsledek predikce v čase
 */
data class ForecastPoint(
    val step: Int, // Časový krok (např. dny/týdny)
    val valSys: Float,
    val valEcon: Float,
    val valPsych: Float,
    val valEco: Float,
    val valLaw: Float,
    val valSec: Float,
    val valPhys: Float,
    val valSoc: Float
)

object ScenarioLibrary {
    val presetEvents = listOf(
        ScenarioEvent(
            "ai_singularity", "Technologický Průlom", 
            "Autonomní optimalizace kognitivního jádra.",
            0.4f, 0.2f, -0.1f, 0.1f, -0.2f, 0.3f, 0.2f, 0.1f,
            OmnisEmerald
        ),
        ScenarioEvent(
            "market_crash", "Ekonomická Recese", 
            "Globální pokles likvidity a zdrojů.",
            -0.1f, -0.5f, -0.3f, 0.0f, 0.1f, -0.2f, -0.1f, -0.4f,
            Color.Red
        ),
        ScenarioEvent(
            "cyber_warfare", "Kybernetický Konflikt", 
            "Masivní útoky na distribuované uzly.",
            -0.3f, -0.1f, -0.2f, 0.0f, -0.1f, -0.6f, -0.2f, -0.1f,
            OmnisAmber
        ),
        ScenarioEvent(
            "social_uprising", "Sociální Transformace", 
            "Změna paradigmatu v lidsko-AI interakci.",
            0.1f, 0.1f, 0.4f, 0.1f, 0.3f, 0.1f, 0.0f, 0.5f,
            OmnisViolet
        )
    )
}
