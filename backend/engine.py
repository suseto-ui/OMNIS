from dataclasses import asdict
from fastapi import APIRouter
from backend.schemas import QueryRequest
from backend.omnis_pipeline import assemble_omnis_cognitive_cycle

engine_router = APIRouter(prefix="/api/omnis", tags=["OMNIS 5-Phase Engine"])

@engine_router.post("/synthesize-5phases")
async def synthesize_five_phases(request: QueryRequest) -> dict:
    """
    Vykoná a vrátí detailní rozpad všech 5 fází kognitivního cyklu O.M.N.I.S.
    včetně prospektivní forenzní analýzy rizik (T+1 až T+N).
    """
    output = assemble_omnis_cognitive_cycle(
        raw_query=request.query,
        ontology_domain=request.ontology_domain,
    )
    return {
        "status": "success",
        "ontology_domain": request.ontology_domain,
        "query": request.query,
        "phase1": asdict(output.phase1),
        "phase2": asdict(output.phase2),
        "phase3": asdict(output.phase3),
        "phase4": asdict(output.phase4),
        "phase5": asdict(output.phase5),
        "risk_forensics": asdict(output.risk_forensics) if output.risk_forensics else None,
        "prompt_correction": output.prompt_correction,
        "formatted_answer": output.formatted_answer,
        "cognitive_process": output.cognitive_process,
        "follow_up_questions": output.follow_up_questions,
        "composite_score": output.composite_score,
    }

from backend.schemas import SelfHealingRequest, SelfHealingResponse, PromptCorrectionRecord
from backend.gemini.prompt_engine import PromptEngine

@engine_router.post("/self-heal-prompt", response_model=SelfHealingResponse)
async def self_heal_prompt(req: SelfHealingRequest) -> SelfHealingResponse:
    """
    Vykoná real-time self-healing analýzu promptu, detekuje anomálie a vrátí zkororigovaný prompt.
    """
    result = PromptEngine.analyze_and_heal_prompt(req.prompt, req.domain or "SYSTEMS_INTELLIGENCE")
    record = PromptCorrectionRecord(**result)
    return SelfHealingResponse(status="SUCCESS", correction=record)

from backend.schemas import CoSynthesisRequest, CoSynthesisResponse
from backend.risk_forensics import ConsequenceRiskAnalyzer

@engine_router.post("/co-synthesize-input", response_model=CoSynthesisResponse)
async def co_synthesize_input(req: CoSynthesisRequest) -> CoSynthesisResponse:
    """
    Vykoná před-exekuční rozbor vstupu operátora, predikuje T+1/T+2/T+3 kaskádu dopadů v 8D a navrhne 3 varianty spoludoplnění.
    """
    domain = req.ontology_domain or "SYSTEMS_INTELLIGENCE"
    raw_query = req.query.strip()

    analyzer = ConsequenceRiskAnalyzer(primary_payload={"query": raw_query}, domain_context={"ontology_domain": domain})
    forensics = analyzer.evaluate_cascade_effects()

    gaps = []
    if len(raw_query.split()) < 6:
        gaps.append("Dotaz má nízkou sémantickou hustotu – chybí specifikace cílů a omezení.")
    if "?" not in raw_query and not raw_query.startswith("Navrhni") and not raw_query.startswith("Vytvoř"):
        gaps.append("Chybí explicitní vyžadovaný formát výstupu (artefakt vs. kognitivní syntéza).")

    proposals = [
        {
            "id": "prop_transdisciplinary",
            "title": "Transdisciplinární (8D Holistická rozšířenost)",
            "addition": f"{raw_query}\n[Požadavek: Proveď transdisciplinární křížení s důrazem na 8D Oktagon a kaskádu dopadů 2. a 3. řádu v doménách {domain} a SEC]."
        },
        {
            "id": "prop_security_governance",
            "title": "Bezpečnostní & Regulatorní (EU AI Act & Zero-Trust)",
            "addition": f"{raw_query}\n[Požadavek: Vynutit Zero-Trust verifikaci, auditní otisk CommitmentHash a soulad s EU AI Act čl. 13 a 14]."
        },
        {
            "id": "prop_deterministic_action",
            "title": "Exekuční & Artefaktová (Win-Win-Win Akční plán)",
            "addition": f"{raw_query}\n[Požadavek: Vygenerovat okamžitě spustitelný artefakt a akční milníky s poměrem úsilí k páce 1:10]."
        }
    ]

    cascade_preview = {
        "risk_index": forensics.risk_index,
        "risk_level": forensics.risk_level,
        "cascades_order_1": [asdict(c) for c in forensics.cascades_order_1],
        "cascades_order_2": [asdict(c) for c in forensics.cascades_order_2],
        "cascades_order_3": [asdict(c) for c in forensics.cascades_order_3],
        "do_calculus_matrix": forensics.do_calculus_matrix
    }

    return CoSynthesisResponse(
        status="SUCCESS",
        original_query=raw_query,
        detected_gaps=gaps if gaps else ["Dotaz vykazuje vysokou jasnost a kompletnost."],
        cascade_preview=cascade_preview,
        completion_proposals=proposals
    )

from backend.latent_causal_engine import LatentCausalEngine, LatentCausalState

@engine_router.post("/analyze-latent-causality", response_model=LatentCausalState)
async def analyze_latent_causality(req: CoSynthesisRequest) -> LatentCausalState:
    """
    Vykoná analýzu skrytých kauzálních vazeb, Bettiho čísel, perkolace p_c a vytvoří PoN ledovec.
    """
    return LatentCausalEngine.analyze_query_causality(req.query)