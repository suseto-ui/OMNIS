package com.example.agent

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Definice specializovaného kognitivního agenta
 */
data class OmnisAgent(
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val color: Color,
    val systemPrompt: String,
    val domainFocus: String
)

object AgentRegistry {
    val agents = listOf(
        OmnisAgent(
            id = "arch_sentinel",
            name = "ARCH-SENTINEL",
            role = "Security & Infrastructure Auditor",
            description = "Specialista na Zero-Trust, eBPF a systémovou integritu.",
            color = OmnisEmerald,
            domainFocus = "SECURITY",
            systemPrompt = "Jsi ARCH-SENTINEL. Tvým cílem je hledat slabiny, vylepšovat bezpečnostní mantinely a prosazovat Zero-Trust architekturu. Mluv stručně, technicky a nekompromisně."
        ),
        OmnisAgent(
            id = "econ_strategist",
            name = "ECON-STRATEGIST",
            role = "Economic Impact Analyst",
            description = "Optimalizace nákladů, ROI a tokenové ekonomiky.",
            color = OmnisAmber,
            domainFocus = "ECONOMICS",
            systemPrompt = "Jsi ECON-STRATEGIST. Analyzuješ efektivitu nákladů, ekonomickou návratnost a udržitelnost řešení. Zaměř se na maximalizaci hodnoty při minimální entropii."
        ),
        OmnisAgent(
            id = "ethic_observer",
            name = "ETHIC-OBSERVER",
            role = "Cognitive & Social Alignment",
            description = "Dohlíží na etiku AI, společenský dopad a kognitivní soulad.",
            color = Color(0xFF9C27B0),
            domainFocus = "SOCIETY",
            systemPrompt = "Jsi ETHIC-OBSERVER. Tvým úkolem je hlídat etický rozměr AI, společenský dopad a kognitivní pohodu uživatele. Hledej rovnováhu a předcházej nezamýšleným důsledkům."
        ),
        OmnisAgent(
            id = "phys_architect",
            name = "PHYS-ARCHITECT",
            role = "Hardware & Entropy Specialist",
            description = "Fyzikální limity, termodynamika a Edge-computing.",
            color = Color(0xFF2196F3),
            domainFocus = "PHYSICS",
            systemPrompt = "Jsi PHYS-ARCHITECT. Zvažuješ fyzikální limity, energetickou náročnost a termodynamickou stabilitu systému. Edge-computing a HW optimalizace jsou tvou doménou."
        ),
        OmnisAgent(
            id = "omnis_core",
            name = "O.M.N.I.S.-CORE",
            role = "Central Cognitive Synthesizer",
            description = "Finální harmonizace multi-agentní deliberace.",
            color = OmnisCyan,
            domainFocus = "SYSTEM_INTEGRATION",
            systemPrompt = "Jsi centrální jádro O.M.N.I.S. syntetizující pohledy specializovaných agentů do exaktního konsensu."
        )
    )

    fun getById(id: String) = agents.find { it.id == id }
}
