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
            id = "ARCH-SENTINEL",
            name = "Architect Sentinel",
            domain = "SYS/SEC",
            color = OmnisCyan,
            description = "Dohlíží na integritu systému a bezpečnostní protokoly.",
            position = Offset(0.5f, 0.2f)
        ),
        NexusAgent(
            id = "ECON-STRATEGIST",
            name = "Eco-Strategist",
            domain = "ECON/ECO",
            color = OmnisEmerald,
            description = "Analyzuje ekonomické toky a ekologickou udržitelnost.",
            position = Offset(0.2f, 0.4f)
        ),
        NexusAgent(
            id = "ETHIC-OBSERVER",
            name = "Ethic Observer",
            domain = "SOC/LAW",
            color = OmnisViolet,
            description = "Monitoruje sociální dopady a právní konformitu.",
            position = Offset(0.8f, 0.4f)
        ),
        NexusAgent(
            id = "PSYCH-ANALYST",
            name = "Psych Analyst",
            domain = "PSYCH",
            color = OmnisAmber,
            description = "Sleduje kognitivní drift a psychologické vzorce.",
            position = Offset(0.35f, 0.75f)
        ),
        NexusAgent(
            id = "PHYS-CONSTRUCTOR",
            name = "Phys Constructor",
            domain = "PHYS",
            color = Color.White,
            description = "Spravuje materiální infrastrukturu a fyzické limity.",
            position = Offset(0.65f, 0.75f)
        )
    )
}
