package com.example.nexus

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Model agenta v rámci sítě Nexus.
 */
data class NexusAgent(
    val id: String,
    val name: String,
    val domain: String,
    val color: Color,
    val description: String,
    var position: Offset = Offset.Zero, // Relativní pozice 0.0 - 1.0
    var isActive: Boolean = false,
    var activityLevel: Float = 0f // 0.0 - 1.0 pro pulzování
)

object NexusTopology {
    val agents = listOf(
        NexusAgent(
            id = "sys_sentinel",
            name = "SYS-SENTINEL",
            domain = "SYSTEMS",
            color = OmnisCyan,
            description = "Dohlíží na systémovou architekturu, integrace a komplexitu.",
            position = Offset(0.5f, 0.15f)
        ),
        NexusAgent(
            id = "econ_strategist",
            name = "ECON-STRATEGIST",
            domain = "ECONOMICS",
            color = OmnisAmber,
            description = "Analyzuje efektivitu nákladů, ekonomickou návratnost a udržitelnost.",
            position = Offset(0.75f, 0.25f)
        ),
        NexusAgent(
            id = "psych_aligner",
            name = "PSYCH-ALIGNER",
            domain = "PSYCHOLOGY",
            color = Color(0xFFFF5722),
            description = "Zaměřuje se na kognitivní ergonomii a uživatelský komfort.",
            position = Offset(0.85f, 0.5f)
        ),
        NexusAgent(
            id = "eco_restorer",
            name = "ECO-RESTORER",
            domain = "ECOLOGY",
            color = OmnisEmerald,
            description = "Sleduje ekologický otisk, spotřebu energie a udržitelnost.",
            position = Offset(0.75f, 0.75f)
        ),
        NexusAgent(
            id = "law_compliance",
            name = "LAW-COMPLIANCE",
            domain = "LAW",
            color = Color(0xFF9C27B0),
            description = "Tvojí doménou je právní soulad a legislativní rámce.",
            position = Offset(0.5f, 0.85f)
        ),
        NexusAgent(
            id = "arch_sentinel",
            name = "ARCH-SENTINEL",
            domain = "SECURITY",
            color = Color(0xFFE91E63),
            description = "Hledá slabiny, vylepšuje bezpečnostní mantinely a Zero-Trust.",
            position = Offset(0.25f, 0.75f)
        ),
        NexusAgent(
            id = "phys_architect",
            name = "PHYS-ARCHITECT",
            domain = "PHYSICS",
            color = Color(0xFF2196F3),
            description = "Zvažuje fyzikální limity, energetickou náročnost a hardware.",
            position = Offset(0.15f, 0.5f)
        ),
        NexusAgent(
            id = "soc_analyst",
            name = "SOC-ANALYST",
            domain = "SOCIETY",
            color = Color(0xFF3F51B5),
            description = "Sleduje širší společenský dopad navrhovaných technologií.",
            position = Offset(0.25f, 0.25f)
        )
    )
}
