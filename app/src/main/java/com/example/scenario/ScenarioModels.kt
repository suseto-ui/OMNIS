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
        ),
        ScenarioEvent(
            "regulatory_compliance", "Regulace & AI Governance", 
            "Zpřísnění etických norem a auditních požadavků (EU AI Act).",
            -0.1f, -0.2f, 0.1f, 0.0f, 0.5f, 0.4f, 0.0f, 0.3f,
            OmnisCyan
        ),
        ScenarioEvent(
            "green_transition", "Energetická & ESG Tranzice", 
            "Přechod na zero-emission výpočetní clustery a ESG standardy.",
            0.1f, -0.2f, 0.2f, 0.6f, 0.2f, 0.1f, 0.1f, 0.3f,
            OmnisEmerald
        ),
        ScenarioEvent(
            "quantum_supremacy", "Kvantový Kryptoanalytický Skok", 
            "Prolomení asymetrických šifer a nutnost post-kvantové migrace.",
            0.3f, -0.1f, -0.2f, 0.0f, 0.1f, -0.5f, 0.3f, -0.1f,
            OmnisAmber
        ),
        ScenarioEvent(
            "post_quantum_shock", "Post-Kvantový Bezpečnostní Šok",
            "Okamžitý požadavek na PQC migrace napříč všemi 8D vrstvami.",
            0.2f, -0.3f, -0.2f, 0.0f, 0.2f, -0.6f, 0.1f, -0.2f,
            OmnisAmber
        ),
        ScenarioEvent(
            "supply_chain_collapse", "Kolaps Čipových & Logistických Řetězců",
            "Kritický výpadek dodávek akcelerátorů a fyzické infrastruktury.",
            -0.4f, -0.6f, -0.3f, 0.1f, -0.1f, -0.3f, -0.5f, -0.3f,
            Color.Red
        ),
        ScenarioEvent(
            "blackout_grid_failure", "Masivní Výpadek Energetické Sítě",
            "Kaskádový blackout a přechod na autonomní ultra-low-power uzly.",
            -0.5f, -0.4f, -0.4f, 0.2f, 0.0f, -0.4f, -0.6f, -0.2f,
            OmnisViolet
        ),
        ScenarioEvent(
            "deepfake_disinfo_flood", "Syntetický Informační Útok",
            "Koordinovaný informační útok zpochybňující integritu datových zdrojů.",
            -0.2f, -0.1f, -0.6f, 0.0f, -0.3f, -0.4f, 0.0f, -0.6f,
            OmnisCyan
        )
    )
}
