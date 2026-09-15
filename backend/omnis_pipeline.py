"""
O.M.N.I.S. 5-Phase Cognitive Assembly Pipeline
Deterministic, modular implementation of the 5 core functions:
  1. Fáze I: Sémantická dekonstrukce (Semantic Deconstruction & First Principles)
  2. Fáze II: Transdisciplinární křížení (Transdisciplinary Synthesis & Leverage Points)
  3. Fáze III: Okamžitý akční plán (Immediate Win-Win-Win Action Plan)
  4. Fáze IV: Deterministická exekuce (Deterministic Execution & Concrete Artifacts)
  5. Fáze V: Autopoietická reflexe & 4D Matice dopadů (Feedback Loop & Impact Matrix)
"""

from __future__ import annotations
from typing import Any, Dict, List, Optional, Tuple, Annotated
from pydantic import BaseModel, Field, ConfigDict, field_validator

from .risk_forensics import ConsequenceRiskAnalyzer, ConsequenceForensicsResult


class Phase1Result(BaseModel):
    """Fáze I: Očištění od předpokladů a rozpad na prvočinitele."""
    cleaned_query: str
    ontology_domain: str
    core_essence: str
    identified_assumptions: List[str]
    hard_data_inferred: Dict[str, Any] = Field(default_factory=dict)
    soft_data_inferred: Dict[str, Any] = Field(default_factory=dict)


class Phase2Result(BaseModel):
    """Fáze II: Transdisciplinární křížení a nalezení pákového bodu."""
    domain_mappings: Dict[str, str]
    leverage_point: str
    nonlinear_synergies: List[str]


class Phase3Result(BaseModel):
    """Fáze III: Win-Win-Win akční plán s minimálním úsilím."""
    win_win_win_rationale: str
    strategic_milestones: List[str]
    effort_to_leverage_ratio: str


class Phase4Result(BaseModel):
    """Fáze IV: Deterministický výstup (konkrétní kód, direktiva, architektura)."""
    artifact_type: str
    execution_steps: List[str]
    concrete_output: str

NormalizedScore = Annotated[float, Field(ge=0.0, le=1.0)]

class RedTeamFinding(BaseModel):
    """Detailní nález z interního Red-Teaming procesu."""
    category: str = Field(..., description="Kategorie rizika (např. 'Technické', 'Právní', 'Kognitivní')")
    description: str = Field(..., description="Detailní popis zranitelnosti nebo rizika")
    severity: str = Field(..., description="Závažnost: LOW, MEDIUM, HIGH, CRITICAL")
    mitigation_strategy: str = Field(..., description="Navržená strategie pro eliminaci rizika")

class RedTeamAnalysis(BaseModel):
    """Strukturovaná analýza rizik a zranitelností v rámci Red-Teaming."""
    findings: List[RedTeamFinding] = Field(default_factory=list)
    risk_landscape_summary: str = Field(
        "Výchozí analýza: Detekce anomálií a slepých skvrn v navrženém exekučním plánu.",
        description="Celkové shrnutí rizikového prostředí"
    )
    attack_surface_delta: float = Field(0.0, description="Kvantifikovaná změna útočné plochy")

class Phase5Result(BaseModel):
    """Fáze V: 8D Matice dopadů, Red-Teaming a reflexe."""
    model_config = ConfigDict(extra="allow")
    
    sys: NormalizedScore
    econ: NormalizedScore
    psych: NormalizedScore
    eco: NormalizedScore
    law: NormalizedScore
    sec: NormalizedScore
    phys: NormalizedScore
    soc: NormalizedScore
    composite_score: NormalizedScore

    red_team_analysis: RedTeamAnalysis
    adversarial_vulnerabilities: List[str]
    reasoning: str
    reflexive_questions: List[str]


class OmnisAssemblyOutput(BaseModel):
    """Složený výstup všech 5 fází O.M.N.I.S. včetně prospektivní forenzní analýzy rizik."""
    phase1: Phase1Result
    phase2: Phase2Result
    phase3: Phase3Result
    phase4: Phase4Result
    phase5: Phase5Result
    formatted_answer: str
    cognitive_process: str
    follow_up_questions: List[str]
    composite_score: float
    risk_forensics: Optional[ConsequenceForensicsResult] = None


# ==========================================================
# 5 DEDICATED FUNCTIONS COMPOSING THE O.M.N.I.S. CYCLE
# ==========================================================

def phase_1_semantic_deconstruction(
    raw_query: str,
    ontology_domain: str,
    context_memories: Optional[List[str]] = None,
    parsed_input: Optional[Dict[str, Any]] = None,
) -> Phase1Result:
    """
    FUNKCE 1: Sémantická dekonstrukce
    - Očistí dotaz od sémantického šumu a implicitních předpokladů.
    - Definuje fundamentální jádro problému (First Principles).
    - Rozdělí data na tvrdá (kvantifikovatelná) a měkká (sociální/emoční kontext).
    """
    cleaned = raw_query.strip()
    if not cleaned:
        cleaned = "Nedefinovaný vstupní parametr"

    core_essence = f"Fundamentální optimalizace a řešení v doméně {ontology_domain}: '{cleaned[:80]}...'"
    assumptions = [
        "Předpoklad lineární závislosti nákladů na komplexitě systému",
        "Předpoklad nutnosti kompromisu mezi rychlostí a architektonickou čistotou",
    ]
    hard_data: Dict[str, Any] = {
        "input_length": len(cleaned),
        "domain": ontology_domain,
        "memory_relevance_count": len(context_memories) if context_memories else 0,
    }
    soft_data: Dict[str, Any] = {
        "urgency_detected": "vysoká" if any(w in cleaned.lower() for w in ["rychle", "hned", "urgent"]) else "standardní",
        "intent_vector": "konstruktivní_syntéza",
    }

    if parsed_input and "phase_1" in parsed_input:
        p1 = parsed_input["phase_1"]
        if isinstance(p1, dict):
            core_essence = p1.get("core_essence", core_essence)
            assumptions = p1.get("identified_assumptions", assumptions)

    return Phase1Result(
        cleaned_query=cleaned,
        ontology_domain=ontology_domain,
        core_essence=core_essence,
        identified_assumptions=assumptions,
        hard_data_inferred=hard_data,
        soft_data_inferred=soft_data,
    )


def phase_2_transdisciplinary_crossing(
    phase1: Phase1Result,
    parsed_input: Optional[Dict[str, Any]] = None,
) -> Phase2Result:
    """
    FUNKCE 2: Transdisciplinární křížení (Rozšířený oktagon)
    - Propojí problém optikou 8 domén:
        1. Systémové inženýrství a kybernetika
        2. Teorie her a asymetrická ekonomie
        3. Kognitivní vědy a neuro-ergonomie
        4. Regenerativní dynamika a ekologie
        5. Regulace, právo a AI governance
        6. Zero-Trust bezpečnost a kryptografie
        7. Fyzikální termodynamika a výpočetní efektivita
        8. Socio-kulturní dynamika a etická rezonance
    - Nalezne pákový uzlový bod (Leverage Point), kde minimální zásah vyvolá maximální systémový účinek.
    """
    domain_mappings = {
        "Systémové inženýrství & Kybernetika": (
            f"Modularizace vstupního toku '{phase1.core_essence[:50]}' na deterministické stavové automaty a dekompozice závislostí."
        ),
        "Teorie her & Asymetrická ekonomie": (
            "Formulace kooperativní Nashovy rovnováhy s asymetrickým výnosem: minimalizace transakčních nákladů a maximalizace návratnosti vstupu."
        ),
        "Kognitivní vědy & Neuro-ergonomie": (
            "Eliminace kognitivní zátěže (Zero-Friction UX), explicitní sémantická vodítka a posílení důvěry skrze deterministické výstupy."
        ),
        "Regenerativní dynamika & Ekologie": (
            "Autopoietická architektura minimalizující sémantický a datový odpad, která se sama opravuje a adaptuje na základě telemetrie."
        ),
        "Regulace, Právo & AI Governance": (
            "Soulad s regulatorními rámci (GDPR, EU AI Act), transparentnost rozhodovacích procesů a garance licenční čistoty."
        ),
        "Zero-Trust Bezpečnost & Kryptografie": (
            "Princip nejnižších privilegií (PoLP), neměnnost auditních stop, sanitizace vstupních toků a odolnost vůči prompt injection / CVE."
        ),
        "Fyzikální termodynamika & Výpočetní efektivita": (
            "Optimalizace energetické stopy a latence: redukce redundance tokenů a alokace minimálních výpočetních cyklů pro maximální efekt."
        ),
        "Socio-kulturní dynamika & Etická rezonance": (
            "Harmonizace se zájmy všech zúčastněných stran (Win-Win-Win), prevence sycophancy a podpora dlouhodobé stability ekosystému."
        ),
    }

    leverage_point = (
        f"Uzlový bod (Leverage Point): Zavedení deterministické validační a překladové vrstvy v doméně {phase1.ontology_domain}. "
        "Tento jediný uzel současně garantuje typovou integritu, eliminuje 80 % bezpečnostních i kognitivních chyb a sjednocuje 8 transdisciplinárních perspektiv."
    )

    synergies = [
        "Synergie architektury a rozhraní: Jediný zdroj pravdy (SSOT) pro backend i frontend eliminuje desynchronizaci",
        "Synergie bezpečnosti a kognice: Striktní typování přímo zamezuje injection zranitelnostem a současně odstraňuje mentální nejistotu operátora",
        "Synergie ekonomie a termodynamiky: Asynchronní exekuce a cachování snižují jak finanční náklady, tak energetickou stopu na dotaz",
        "Synergie práva a etiky: Transparentní auditní stopa a zero-hallucination politika automaticky plní nároky přísných regulatorních norem",
    ]

    if parsed_input and "phase_2" in parsed_input:
        p2 = parsed_input["phase_2"]
        if isinstance(p2, dict):
            leverage_point = p2.get("leverage_point", leverage_point)
            if "domain_mappings" in p2 and isinstance(p2["domain_mappings"], dict):
                domain_mappings.update(p2["domain_mappings"])
            if "nonlinear_synergies" in p2 and isinstance(p2["nonlinear_synergies"], list):
                synergies = p2["nonlinear_synergies"]

    return Phase2Result(
        domain_mappings=domain_mappings,
        leverage_point=leverage_point,
        nonlinear_synergies=synergies,
    )


def phase_3_immediate_action_plan(
    phase1: Phase1Result,
    phase2: Phase2Result,
    parsed_input: Optional[Dict[str, Any]] = None,
) -> Phase3Result:
    """
    FUNKCE 3: Okamžitý akční plán (Win-Win-Win)
    - Formuluje strategii maximální návratnosti s minimálním úsilím ze strany uživatele.
    - Sestavuje milníky pro Minimální Životaschopnou Syntézu (MVS - Minimum Viable Synthesis).
    """
    rationale = (
        "Win-Win-Win: Technologicky elegantní (čistý typovaný kód), "
        "ekonomicky výhodné (nulové plýtvání tokeny a výpočetními zdroji), "
        "uživatelsky nenáročné (okamžitá exekuce jedním klikem či příkazem)."
    )
    milestones = [
        f"Krok 1 (Okamžitá aplikace): Aktivace uzlového bodu ({phase2.leverage_point[:60]}...).",
        "Krok 2 (Ověření invariantů): Spuštění deterministického testu a ověření nulové chybovosti.",
        "Krok 3 (Autopoietické uzavření): Zpětná vazba a trvalé uložení do paměťové vrstvy.",
    ]
    ratio = "Maximální páka: 1 jednotka uživatelského vstupu = 10 jednotek systémového výstupu."

    if parsed_input and "phase_3" in parsed_input:
        p3 = parsed_input["phase_3"]
        if isinstance(p3, dict):
            rationale = p3.get("rationale", rationale)
            milestones = p3.get("milestones", milestones)

    return Phase3Result(
        win_win_win_rationale=rationale,
        strategic_milestones=milestones,
        effort_to_leverage_ratio=ratio,
    )


def phase_4_deterministic_execution(
    phase1: Phase1Result,
    phase2: Phase2Result,
    phase3: Phase3Result,
    parsed_input: Optional[Dict[str, Any]] = None,
) -> Phase4Result:
    """
    FUNKCE 4: Deterministický výstup / Exekuce
    - Generuje konkrétní kód, direktivu nebo rozhodnutí.
    - Absolutní Zero-Fluff: Žádné obecné formulace, pouze přímo použitelný artefakt.
    """
    artifact_type = "Produkční exekuční kód a systémová direktiva"
    execution_steps = [
        "Nasazení verifikovaného modulu do běhového prostředí",
        "Aktivace živé telemetrie a napojení na /api/health-check",
        "Nastavení automatického sběru zpětné vazby do autopoietické paměti",
    ]
    concrete_output = (
        f"// O.M.N.I.S. Deterministická direktiva pro doménu {phase1.ontology_domain}\n"
        f"// Jádro řešení: {phase1.core_essence}\n"
        f"// Pákový bod: {phase2.leverage_point}\n"
        "export const EXECUTION_INVARIANT = {\n"
        "  status: 'ACTIVE_ZERO_DEFECT',\n"
        f"  domain: '{phase1.ontology_domain}',\n"
        "  leverageApplied: true,\n"
        "};"
    )

    if parsed_input and "phase_4" in parsed_input:
        p4 = parsed_input["phase_4"]
        if isinstance(p4, dict):
            concrete_output = p4.get("concrete_output", concrete_output)
            artifact_type = p4.get("artifact_type", artifact_type)

    return Phase4Result(
        artifact_type=artifact_type,
        execution_steps=execution_steps,
        concrete_output=concrete_output,
    )


def phase_5_impact_matrix_and_reflection(
    phase1: Phase1Result,
    phase2: Phase2Result,
    phase3: Phase3Result,
    phase4: Phase4Result,
    raw_scores: Optional[Dict[str, float]] = None,
    parsed_red_team: Optional[Dict[str, Any]] = None,
    parsed_vulnerabilities_legacy: Optional[List[str]] = None,
    parsed_vulnerabilities: Optional[List[str]] = None,
) -> Phase5Result:
    """
    FUNKCE 5: Autopoietická reflexe & 8D Matice dopadů
    - Výpočet kompozitního skóre s rovnoměrnými vahami (12.5 % na doménu) přes 8 domén.
    - Red-Teaming (Hloubková analýza a penalizace za kritické zranitelnosti).
    - Vygenerování reflexivních otázek pro sebereferenční učení.
    """
    
    # [REFAKTORIZACE - SLABÉ MÍSTO]: Původní kód selhával, pokud LLM vrátil nečíselnou hodnotu (např. string "N/A").
    # Přidán bezpečný parser. Také odstraněny falešně vysoké defaultní hodnoty (0.91), které lhaly o úspěšnosti, pokud API selhalo.
    # Nyní systém padá na neutrální střed (0.5), což signalizuje nedostatek dat.
    def safe_float(val: Any, default: float = 0.5) -> float:
        try:
            return float(val)
        except (ValueError, TypeError):
            return default

    if raw_scores:
        sys = safe_float(raw_scores.get("sys"), 0.5)
        econ = safe_float(raw_scores.get("econ"), 0.5)
        psych = safe_float(raw_scores.get("psych"), 0.5)
        eco = safe_float(raw_scores.get("eco"), 0.5)
        law = safe_float(raw_scores.get("law"), 0.5)
        sec = safe_float(raw_scores.get("sec"), 0.5)
        phys = safe_float(raw_scores.get("phys"), 0.5)
        soc = safe_float(raw_scores.get("soc"), 0.5)
    else:
        # Neutrální baseline pro případ selhání parsování - zabráníme halucinaci vysokého skóre
        sys, econ, psych, eco, law, sec, phys, soc = [0.5] * 8

    # Zpracování detailního Red-Teamingu
    findings = []
    summary = "Analýza rizik nebyla detailně specifikována."
    surface_delta = 0.0

    if parsed_red_team:
        summary = parsed_red_team.get("risk_landscape_summary", summary)
        surface_delta = safe_float(parsed_red_team.get("attack_surface_delta"), 0.0)
        raw_findings = parsed_red_team.get("findings", [])
        if isinstance(raw_findings, list):
            for rf in raw_findings:
                if isinstance(rf, dict):
                    findings.append(RedTeamFinding(**rf))

    # Fallback na starý formát, pokud structured red-team chybí
    vulnerabilities_list = parsed_vulnerabilities_legacy or parsed_vulnerabilities or []
    if not findings and vulnerabilities_list:
        for v in vulnerabilities_list:
            findings.append(RedTeamFinding(
                category="Uncategorized",
                description=v,
                severity="MEDIUM",
                mitigation_strategy="Bude specifikováno v další iteraci."
            ))

    if not findings:
        vulnerabilities = ["Neznámá zranitelnost: LLM nedodalo data pro Red-Teaming."]
    else:
        vulnerabilities = [f"[{f.severity}] {f.category}: {f.description}" for f in findings]

    rt_analysis = RedTeamAnalysis(
        findings=findings,
        risk_landscape_summary=summary,
        attack_surface_delta=surface_delta
    )

    # Weighted calculation (equal weights 1/8)
    sum_scores = sys + econ + psych + eco + law + sec + phys + soc
    weighted_sum = sum_scores / 8.0
    # Scaled penalty (e.g. 0.02 per identified vulnerability in 0..1 scale)
    penalty = len(vulnerabilities) * 0.02
    composite = max(0.0, min(1.0, round(weighted_sum - penalty, 3)))

    reasoning = (
        f"Matice dopadů: 8 domén Oktagonu (průměr {round(weighted_sum * 100, 1)}%) "
        f"- Penalizace ({round(penalty, 3)}) = Kompozitní index {composite}."
    )

    reflexive_questions = [
        f"Jak lze dále škálovat identifikovaný pákový bod v doméně {phase1.ontology_domain}?",
        "Jaké konkrétní metriky potvrdí dosažení nulové chybovosti během prvních 24 hodin provozu?",
        "Existuje v systému skrytá závislost, kterou lze převést na autonomní asynchronní proces?",
    ]

    return Phase5Result(
        sys=round(sys, 3),
        econ=round(econ, 3),
        psych=round(psych, 3),
        eco=round(eco, 3),
        law=round(law, 3),
        sec=round(sec, 3),
        phys=round(phys, 3),
        soc=round(soc, 3),
        composite_score=composite,
        red_team_analysis=rt_analysis,
        adversarial_vulnerabilities=vulnerabilities,
        reasoning=reasoning,
        reflexive_questions=reflexive_questions,
    )


# ==========================================================
# MASTER ORCHESTRATOR FUNCTION
# ==========================================================

def assemble_omnis_cognitive_cycle(
    raw_query: str,
    ontology_domain: str = "SYSTEMS_INTELLIGENCE",
    context_memories: Optional[List[str]] = None,
    gemini_parsed_response: Optional[Dict[str, Any]] = None,
) -> OmnisAssemblyOutput:
    """
    Hlavní orchestrátor:
    Vykoná sekvenční složení všech 5 fází s garantovanými kontrakty mezi vstupy a výstupy.
    """
    # 1. Spuštění Funkce 1
    p1 = phase_1_semantic_deconstruction(
        raw_query=raw_query,
        ontology_domain=ontology_domain,
        context_memories=context_memories,
        parsed_input=gemini_parsed_response,
    )

    # 2. Spuštění Funkce 2
    p2 = phase_2_transdisciplinary_crossing(
        phase1=p1,
        parsed_input=gemini_parsed_response,
    )

    # 3. Spuštění Funkce 3
    p3 = phase_3_immediate_action_plan(
        phase1=p1,
        phase2=p2,
        parsed_input=gemini_parsed_response,
    )

    # 4. Spuštění Funkce 4
    p4 = phase_4_deterministic_execution(
        phase1=p1,
        phase2=p2,
        phase3=p3,
        parsed_input=gemini_parsed_response,
    )

    # 5. Spuštění Funkce 5
    raw_matrix = None
    red_team_data = None
    legacy_vulnerabilities = None
    if gemini_parsed_response and "impact_matrix" in gemini_parsed_response:
        raw_matrix = gemini_parsed_response["impact_matrix"]
        if isinstance(raw_matrix, dict):
            legacy_vulnerabilities = raw_matrix.get("adversarial_vulnerabilities", None)
            red_team_data = raw_matrix.get("red_team_analysis", None)

    p5 = phase_5_impact_matrix_and_reflection(
        phase1=p1,
        phase2=p2,
        phase3=p3,
        phase4=p4,
        raw_scores=raw_matrix if isinstance(raw_matrix, dict) else None,
        parsed_red_team=red_team_data if isinstance(red_team_data, dict) else None,
        parsed_vulnerabilities_legacy=legacy_vulnerabilities,
    )

    # 6. Spuštění Meta-vrstvy: Prospektivní forenzní analýza rizik (T+1 až T+N)
    parsed_forensics = None
    if gemini_parsed_response and "consequence_forensics" in gemini_parsed_response:
        parsed_forensics = gemini_parsed_response["consequence_forensics"]

    risk_analyzer = ConsequenceRiskAnalyzer(
        primary_payload={
            "query": raw_query,
            "essence": p1.core_essence,
            "leverage_point": p2.leverage_point,
            "artifact": p4.concrete_output,
            "composite_score": p5.composite_score,
        },
        domain_context={
            "ontology_domain": ontology_domain,
            "domain_mappings": p2.domain_mappings,
        },
        parsed_forensics=parsed_forensics if isinstance(parsed_forensics, dict) else None,
    )
    risk_results = risk_analyzer.evaluate_cascade_effects()

    # Sestavení finální strukturované odpovědi
    if gemini_parsed_response and "answer" in gemini_parsed_response:
        formatted_answer = gemini_parsed_response["answer"]
    else:
        formatted_answer = (
            f"# O.M.N.I.S. Transdisciplinární Syntéza: {p1.cleaned_query[:60]}\n\n"
            f"### 1. Sémantická dekonstrukce\n"
            f"- **Jádro problému:** {p1.core_essence}\n"
            f"- **Odstraněné předpoklady:** {', '.join(p1.identified_assumptions)}\n\n"
            f"### 2. Transdisciplinární křížení\n"
            f"- **Pákový uzlový bod:** {p2.leverage_point}\n"
            + "\n".join(f"- **{k}:** {v}" for k, v in p2.domain_mappings.items())
            + f"\n\n### 3. Okamžitý akční plán (Win-Win-Win)\n"
            f"{p3.win_win_win_rationale}\n"
            + "\n".join(f"{i+1}. {step}" for i, step in enumerate(p3.strategic_milestones))
            + f"\n\n### 4. Výstup & Exekuce\n"
            f"```{p4.artifact_type}\n{p4.concrete_output}\n```\n\n"
            f"### 5. Autopoietická reflexe & 8D Matice dopadů\n"
            f"- **Kompozitní index harmonie:** {p5.composite_score * 100:.1f} %\n"
            f"- **Zdůvodnění:** {p5.reasoning}\n"
            f"#### Detailní Red-Teaming Analýza:\n"
            f"> {p5.red_team_analysis.risk_landscape_summary}\n\n"
            + "\n".join(
                f"- **[{f.severity}] {f.category}**: {f.description}\n"
                f"  - *Mitigační strategie*: {f.mitigation_strategy}" 
                for f in p5.red_team_analysis.findings
            )
            + f"\n\n- **Delta útočné plochy:** {p5.red_team_analysis.attack_surface_delta:+.2f}\n\n"
            f"### 6. Prospektivní forenzní analýza rizik ({risk_results.horizon})\n"
            f"- **Rizikový index:** {risk_results.risk_index} ({risk_results.risk_level})\n"
            f"- **T+1 systémový drift:** {risk_results.t_plus_1_systemic_drift}\n"
            f"- **Detekované vektory:** {len(risk_results.identified_vectors)} vektorů analyzováno.\n"
        )

    cognitive_process = (
        f"Kognitivní proces O.M.N.I.S. [5 Fází + Forenzní analýza rizik]:\n"
        f"[Fáze I]: Dekonstrukce vstupu na prvočinitele pro doménu {p1.ontology_domain}.\n"
        f"[Fáze II]: Nalezen pákový bod -> {p2.leverage_point}\n"
        f"[Fáze III]: Formulován Win-Win-Win plán ({p3.effort_to_leverage_ratio}).\n"
        f"[Fáze IV]: Vygenerován deterministický artefakt typu '{p4.artifact_type}'.\n"
        f"[Fáze V]: Spočtena 8D Matice s kompozitním skóre {p5.composite_score}.\n"
        f"[Forenzní analýza]: Vyhodnoceny T+1..T+N kaskády (Rizikový index: {risk_results.risk_index}, Úroveň: {risk_results.risk_level})."
    )

    return OmnisAssemblyOutput(
        phase1=p1,
        phase2=p2,
        phase3=p3,
        phase4=p4,
        phase5=p5,
        formatted_answer=formatted_answer,
        cognitive_process=cognitive_process,
        follow_up_questions=p5.reflexive_questions,
        composite_score=p5.composite_score,
        risk_forensics=risk_results,
    )
