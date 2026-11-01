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