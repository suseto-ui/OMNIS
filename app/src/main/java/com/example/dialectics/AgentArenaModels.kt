package com.example.dialectics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AgentArchetype(
    val id: String,
    val name: String,
    val roleTitle: String,
    val description: String,
    val badgeIconName: String,
    val colorHex: Long,
    val primaryDomains: List<String>, // 8D domains: SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC
    val riskTolerance: Float, // 0.0 (Ultra-conservative) to 1.0 (Radical innovator)
    val epistemicRigor: Float // 0.0 to 1.0
)

@Immutable
data class ArenaDebateTurn(
    val turnNumber: Int,
    val roundName: String, // e.g., "Kolo 1: Úvodní Pozice", "Kolo 2: Antiteze & Křížové Námitky", "Kolo 3: Syntéza"
    val agentId: String,
    val agentName: String,
    val argumentText: String,
    val highlightedDomains: List<String>,
    val domainScores: Map<String, Float>, // Domain -> Score (-1.0 to +1.0)
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class ArenaDebateResult(
    val debateId: String = "arena-${System.currentTimeMillis()}",
    val problemStatement: String,
    val agentA: AgentArchetype,
    val agentB: AgentArchetype,
    val turns: List<ArenaDebateTurn>,
    val agentA8DScores: Map<String, Float>, // SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC (-1.0 to 1.0)
    val agentB8DScores: Map<String, Float>,
    val synthesized8DScores: Map<String, Float>,
    val divergenceIndex: Float, // 0.0 (identical) to 1.0 (polar opposite)
    val consensusIndex: Float, // 0.0 to 1.0
    val synthesisSummary: String,
    val actionableGuidelines: List<String>,
    val zkCommitmentHash: String
)

object AgentArenaLibrary {
    val presetArchetypes = listOf(
        AgentArchetype(
            id = "conservative_analyst",
            name = "Konzervativní Analytik",
            roleTitle = "Normativní Garance & Riziková Averze",
            description = "Upřednostňuje systémovou stabilitu, bezpečnostní standardy (ISO/IEC, NIS2) a striktní brány Refusal Ladderu.",
            badgeIconName = "Shield",
            colorHex = 0xFF3B82F6, // Blue
            primaryDomains = listOf("LAW", "SEC", "ECO"),
            riskTolerance = 0.15f,
            epistemicRigor = 0.95f
        ),
        AgentArchetype(
            id = "radical_innovator",
            name = "Radikální Inovátor",
            roleTitle = "Kognitivní Průlom & Tenzorový Výkon",
            description = "Maximální akcelerace výpočetního jádra, obcházení byrokratických prodlev a agresivní využití 8D matice.",
            badgeIconName = "Bolt",
            colorHex = 0xFFEC4899, // Pink
            primaryDomains = listOf("SYS", "ECON", "PHYS"),
            riskTolerance = 0.88f,
            epistemicRigor = 0.75f
        ),
        AgentArchetype(
            id = "skeptic_auditor",
            name = "Skeptik & Auditor",
            roleTitle = "Detekce Černých Labutí & Entropie",
            description = "Vyhledává skryté korelativní trhliny, zkoumá kaskádová selhání a zpochybňuje přehnaná optimistická tvrzení.",
            badgeIconName = "Search",
            colorHex = 0xFFF59E0B, // Amber/Orange
            primaryDomains = listOf("SEC", "PSYCH", "SYS"),
            riskTolerance = 0.30f,
            epistemicRigor = 0.98f
        ),
        AgentArchetype(
            id = "pragmatic_engineer",
            name = "Pragmatický Inženýr",
            roleTitle = "Exekuční Stablita & ZK-SNARKs",
            description = "Zaměřen na reálnou architekturu kódu v Kotlin 2.0, paměťovou efektivitu v CSR matici a garanci auditní stopy.",
            badgeIconName = "Build",
            colorHex = 0xFF10B981, // Green
            primaryDomains = listOf("PHYS", "SYS", "ECON"),
            riskTolerance = 0.45f,
            epistemicRigor = 0.90f
        ),
        AgentArchetype(
            id = "eco_socius",
            name = "Eko-Systémový Etik",
            roleTitle = "Společenská Integrita & Udržitelnost",
            description = "Hodnotí dopady na lidskou psychiku, ekologické zdroje a dlouhodobou stabilitu celospolečenského ekosystému.",
            badgeIconName = "Public",
            colorHex = 0xFF8B5CF6, // Purple
            primaryDomains = listOf("ECO", "PSYCH", "SOC"),
            riskTolerance = 0.25f,
            epistemicRigor = 0.92f
        )
    )

    val presetDebateTopics = listOf(
        "Kvantová migrace bankovní infrastruktury: Okamžitý přechod vs. Postupné testování",
        "Autonomní AI v řízení letového provozu: Bezvýhradný automat vs. Člověk v rozhodovací smyčce",
        "Implementace EU AI Act u generativních modelů: Striktní stop-brány vs. Inovační výjimky",
        "Energetická alokace pro kognitivní grafy: Dynamické přetěžování vs. Ekologický strop",
        "Dezentralizace kognitivního jádra O.M.N.I.S.: Peer-to-Peer CSR mesh vs. Centralizovaný klastr"
    )
}
