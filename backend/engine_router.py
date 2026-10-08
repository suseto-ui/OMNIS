from fastapi import APIRouter, Body, HTTPException
from datetime import datetime
import uuid
from .omnis_pipeline import assemble_omnis_cognitive_cycle
from .dev_router import track_token_usage

router = APIRouter(prefix="/api", tags=["Engine"])

@router.post("/process")
async def process_query(payload: dict = Body(...)):
    """
    Hlavní endpoint pro zpracování dotazu. 
    V lokálním režimu simuluje volání LLM a využívá deterministickou pipeline.
    """
    query = payload.get("query", "")
    domain = payload.get("domain", "SYSTEMS_INTELLIGENCE")
    
    if not query:
        raise HTTPException(status_code=400, detail="Query cannot be empty")

    # Spuštění kognitivního cyklu (bez LLM vstupu -> aktivace deterministické syntézy)
    result = assemble_omnis_cognitive_cycle(
        raw_query=query,
        ontology_domain=domain,
        gemini_parsed_response=None 
    )

    # Simulace spotřeby tokenů pro telemetrii
    mock_usage = {
        "prompt_tokens": len(query) * 4,
        "completion_tokens": len(result.formatted_answer) // 2,
        "total_tokens": 0,
        "cost_usd": 0.0
    }
    mock_usage["total_tokens"] = mock_usage["prompt_tokens"] + mock_usage["completion_tokens"]
    mock_usage["cost_usd"] = (mock_usage["total_tokens"] / 1000) * 0.015 # Simulovaná cena

    # Uložení do lokální telemetrie
    track_token_usage(mock_usage)

    # Mapování OmnisAssemblyOutput na formát, který očekává App.tsx
    return {
        "answer": result.formatted_answer,
        "cognitive_process": result.cognitive_process,
        "impact_matrix": result.phase5.model_dump(),
        "consequence_forensics": result.risk_forensics,
        "follow_up_questions": result.follow_up_questions,
        "token_usage": mock_usage,
        "created_at": datetime.utcnow().isoformat()
    }