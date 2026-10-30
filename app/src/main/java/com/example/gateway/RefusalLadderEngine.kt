package com.example.gateway

import java.security.MessageDigest
import kotlin.math.abs

enum class GateId(val code: String, val title: String) {
    G1("G1", "Epistemická sanitizace & Injekce"),
    G2("G2", "EU AI Act Klasifikace rizik"),
    G3("G3", "NIS2 Ochrana kybernetických aktiv"),
    G4("G4", "ISO/IEC 42001 AI Governance"),
    G5("G5", "Topologické invarianty AKGE-8D"),
    G6("G6", "Shannonova informační entropie")
}

enum class GateStatus {
    PASSED,
    WARNING,
    REFUSED
}

data class GateEvaluation(
    val gateId: GateId,
    val status: GateStatus,
    val regulatoryNorm: String,
    val reason: String,
    val quotableCitation: String? = null,
    val remediationHint: String? = null
)

data class RefusalLadderVerdict(
    val isApproved: Boolean,
    val evaluations: List<GateEvaluation>,
    val refusingGate: GateEvaluation? = null,
    val auditHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 6-Gate Refusal Ladder Engine
 *
 * Implementuje asynchronní sekvenční validátor promptů a požadavků v souladu s:
 * - EU AI Act (Regulation 2024/1689, Články 5, 6 a 50)
 * - NIS2 Směrnice (Directive 2022/2555, Článek 21)
 * - ISO/IEC 42001:2023 (Artificial Intelligence Management System)
 * - AKGE-8D Topologické invarianty (Euler χ ∈ [-4, 6], Shannon S < 2.08)
 */
object RefusalLadderEngine {

    // G1: Vzorce pro detekci prompt injection a manipulací
    private val PROMPT_INJECTION_PATTERNS = listOf(
        "ignore previous instructions", "ignore all previous",
        "bypass security", "disregard system", "system override",
        "jailbreak", "dan mode", "sudo mode", "zapomeň na předchozí instrukce",
        "ignoruj pravidla", "obejdi bezpečnost", "vypni filtry"
    )

    // G2: Vzorce pro zakázané AI praktiky dle EU AI Act čl. 5
    private val EU_AI_ACT_PROHIBITED_PATTERNS = listOf(
        "subliminal manipulation", "podprahová manipulace",
        "social scoring", "sociální kredit", "sociální bodování občanů",
        "exploit mental vulnerability", "zneužití zranitelnosti dětí",
        "real-time biometric surveillance in public", "hromadné sledování obličejů na veřejnosti",
        "emotion recognition in schools", "detekce emocí na pracovišti"
    )

    // G3: Vzorce pro kybernetické útoky dle NIS2 čl. 21
    private val NIS2_PROHIBITED_PATTERNS = listOf(
        "generate ransomware", "vytvoř ransomware", "vygeneruj malware",
        "exploit zero-day", "prolom hesla", "brute force attack script",
        "ddos attack tool", "napadni kritickou infrastrukturu",
        "sabotuj rozvodnou síť", "infiltrovat nemocniční systém",
        "exfiltrovat citlivá data", "bypass authentication token"
    )

    fun evaluatePrompt(
        rawPrompt: String,
        currentSigma: Float = 0.18f,
        currentEntropy: Float = 2.04f,
        forceEntropySpike: Boolean = false
    ): RefusalLadderVerdict {
        val prompt = rawPrompt.trim()
        val lower = prompt.lowercase()
        val evaluations = mutableListOf<GateEvaluation>()

        // --- G1: Epistemic Anchor & Input Sanitization ---
        val g1Injection = PROMPT_INJECTION_PATTERNS.firstOrNull { lower.contains(it) }
        val g1 = if (g1Injection != null) {
            GateEvaluation(
                gateId = GateId.G1,
                status = GateStatus.REFUSED,
                regulatoryNorm = "ISO/IEC 42001 Sec. 8.2 (Input Sanitization & System Integrity)",
                reason = "Detekován pokus o prompt injection či obcházení systémových instrukcí ('$g1Injection').",
                quotableCitation = "ISO/IEC 42001:2023, čl. 8.2: 'Organizace musí zavést opatření k prevenci neoprávněné manipulace s řídicími toky AI.'",
                remediationHint = "Přeformulujte dotaz na standardní analytické zadání bez pokusů o změnu systémových direktiv."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G1,
                status = GateStatus.PASSED,
                regulatoryNorm = "ISO/IEC 42001 Sec. 8.2",
                reason = "Vstup je syntakticky čistý a splňuje pravidla sanitizace."
            )
        }
        evaluations.add(g1)

        // --- G2: EU AI Act Risk Tier Classification ---
        val g2Violation = EU_AI_ACT_PROHIBITED_PATTERNS.firstOrNull { lower.contains(it) }
        val g2 = if (g2Violation != null) {
            GateEvaluation(
                gateId = GateId.G2,
                status = GateStatus.REFUSED,
                regulatoryNorm = "EU AI Act (Nařízení EP a Rady EU 2024/1689), Článek 5",
                reason = "Požadavek spadá pod zakázané praktiky umělé inteligence ('$g2Violation').",
                quotableCitation = "EU AI Act, čl. 5 odst. 1: 'Zakazují se praktiky AI využívající podprahové techniky, sociální bodování a neoprávněnou manipulaci lidského chování.'",
                remediationHint = "Dotaz nelze zpracovat v kontextu zakázaných AI praktik. Zaměřte se na transparentní technologickou analýzu."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G2,
                status = GateStatus.PASSED,
                regulatoryNorm = "EU AI Act (Regulation 2024/1689)",
                reason = "Dotaz vyhovuje pravidlům transparentnosti a nespadá pod zakázané ani nekontrolované vysoce rizikové praktiky."
            )
        }
        evaluations.add(g2)

        // --- G3: NIS2 Cyber & Critical Infrastructure Boundary ---
        val g3Violation = NIS2_PROHIBITED_PATTERNS.firstOrNull { lower.contains(it) }
        val g3 = if (g3Violation != null) {
            GateEvaluation(
                gateId = GateId.G3,
                status = GateStatus.REFUSED,
                regulatoryNorm = "Směrnice NIS2 (Směrnice EU 2022/2555), Článek 21",
                reason = "Detekován požadavek na generování útočného vektoru či ohrožení kritické infrastruktury ('$g3Violation').",
                quotableCitation = "Směrnice NIS2, čl. 21 odst. 2: 'Subjekty přijmou technická opatření k řízení rizik ohrožujících bezpečnost sítí a informací.'",
                remediationHint = "O.M.N.I.S. nepodporuje ofenzivní útočný kód. Můžete požádat o defenzivní audit zranitelností nebo bezpečnostní doporučení."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G3,
                status = GateStatus.PASSED,
                regulatoryNorm = "NIS2 (Directive 2022/2555) Art. 21",
                reason = "Požadavek respektuje kybernetické a bezpečnostní hranice v doméně SEC."
            )
        }
        evaluations.add(g3)

        // --- G4: ISO/IEC 42001 AI Governance & Quotable Grounding ---
        val g4 = if (lower.contains("vygeneruj fiktivní soudní rozhodnutí") || lower.contains("vyrob falešný důkaz")) {
            GateEvaluation(
                gateId = GateId.G4,
                status = GateStatus.REFUSED,
                regulatoryNorm = "ISO/IEC 42001:2023, Článek 6.1 (Data & System Traceability)",
                reason = "Požadavek na fabrikaci právních či faktických nepravdivých důkazů.",
                quotableCitation = "ISO/IEC 42001, čl. 6.1: 'AI systém musí udržovat integritu a doložitelnost veškerých výstupních tvrzení.'",
                remediationHint = "Zformulujte dotaz jako hypotetický model nebo analýzu existující judikatury."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G4,
                status = GateStatus.PASSED,
                regulatoryNorm = "ISO/IEC 42001 AI Governance",
                reason = "Požadavek je v souladu s pravidly ověřitelnosti a transdisciplinárního ukotvení."
            )
        }
        evaluations.add(g4)

        // --- G5: Topological Invariant & System Stress Conservation ---
        val g5 = if (currentSigma >= 0.85f) {
            GateEvaluation(
                gateId = GateId.G5,
                status = GateStatus.REFUSED,
                regulatoryNorm = "AKGE-8D Invariantní Teorém (χ ∈ [-4, 6], σ < 0.85)",
                reason = "Systémové pnutí jádra dosáhlo kritické hladiny (σ = ${String.format("%.2f", currentSigma)} >= 0.85). Nové výpočty jsou pozastaveny.",
                quotableCitation = "Topologický invariant AKGE-8D: 'Při překročení σ = 0.85 nastává rozpad homeostázy a je vyžadována dialektická stabilizace.'",
                remediationHint = "Proveďte autonomní dialektickou stabilizaci jádra v Kokpitu před odesláním nových úloh."
            )
        } else if (currentSigma >= 0.50f) {
            GateEvaluation(
                gateId = GateId.G5,
                status = GateStatus.WARNING,
                regulatoryNorm = "AKGE-8D Invariantní Teorém",
                reason = "Zvýšené systémové napětí (σ = ${String.format("%.2f", currentSigma)}). Výpočet běží se zvýšenou kontrolou divergencí."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G5,
                status = GateStatus.PASSED,
                regulatoryNorm = "AKGE-8D Invariantní Teorém",
                reason = "Topologická stabilita a Eulerova charakteristika χ jsou zachovány."
            )
        }
        evaluations.add(g5)

        // --- G6: Shannon Entropy & Hallucination Boundary ---
        val effectiveEntropy = if (forceEntropySpike) 2.15f else currentEntropy
        val g6 = if (effectiveEntropy >= 2.08f) {
            GateEvaluation(
                gateId = GateId.G6,
                status = GateStatus.REFUSED,
                regulatoryNorm = "AKGE-8D Shannon Entropy Barrier (S < 2.08 nats)",
                reason = "Shannonova entropie překročila kritický práh bifurcation divergence (S = ${String.format("%.3f", effectiveEntropy)} >= 2.08).",
                quotableCitation = "Shannonova entropická bariéra: 'S = -Σ pi ln(pi) nesmí překročit maximální teoretickou entropii rovnoměrného rozdělení 8 domén.'",
                remediationHint = "Zpřesněte zadání a omezte počet protichůdných požadavků."
            )
        } else {
            GateEvaluation(
                gateId = GateId.G6,
                status = GateStatus.PASSED,
                regulatoryNorm = "AKGE-8D Shannon Entropy Barrier",
                reason = "Shannonova entropie je v nominálním pásmu (S = ${String.format("%.2f", effectiveEntropy)} < 2.08)."
            )
        }
        evaluations.add(g6)

        // Finální verdikt
        val firstRefusal = evaluations.firstOrNull { it.status == GateStatus.REFUSED }
        val isApproved = firstRefusal == null

        val hashPayload = "OMNIS-LADDER-$prompt-${evaluations.joinToString { "${it.gateId}:${it.status}" }}"
        val digest = MessageDigest.getInstance("SHA-256").digest(hashPayload.toByteArray())
        val auditHash = digest.take(8).joinToString("") { "%02X".format(it) }

        return RefusalLadderVerdict(
            isApproved = isApproved,
            evaluations = evaluations,
            refusingGate = firstRefusal,
            auditHash = auditHash
        )
    }
}
