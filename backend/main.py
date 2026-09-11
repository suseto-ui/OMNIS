"""
O.M.N.I.S. Core FastAPI Backend Application
Handles queries, cognitive processes, Impact Matrix evaluations, and autopoietic memory loops.
"""

from __future__ import annotations
import logging
import uuid
from contextlib import asynccontextmanager
from typing import AsyncGenerator, List, Optional
from fastapi import APIRouter, BackgroundTasks, Depends, FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from .database import get_db, init_db
from .gemini_service import cognitive_service
from .omnis_pipeline import assemble_omnis_cognitive_cycle
from .token_service import token_telemetry_service
from .models import Conversation, ImpactMatrixMetric, Message, VectorMemory
from .schemas import (
    AutopoieticFeedbackRequest,
    AutopoieticFeedbackResponse,
    ConversationDetail,
    ConvergenceEvaluationRequest,
    ConvergenceEvaluationResponse,
    ImpactMatrixScores,
    ImpactScore,
    MemoryItem,
    OmnisEntityIngestion,
    QueryRequest,
    QueryResponse,
    SolutionCandidate,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("omnis.api")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Startup and shutdown lifecycle handler."""
    logger.info("Initializing O.M.N.I.S. database and pgvector extensions...")
    try:
        await init_db()
        logger.info("Database schema initialized successfully.")
    except Exception as exc:
        logger.error(f"Database initialization warning (will retry on queries): {exc}")
    yield
    logger.info("Shutting down O.M.N.I.S. backend.")


app = FastAPI(
    title="O.M.N.I.S. Cognitive Architecture API",
    description="Omni-Modal Network for Integrated Synthesis - Backend & Impact Matrix Engine",
    version="2.0.0",
    lifespan=lifespan,
)

# CORS configuration for local development and Cloud Run deployment
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


async def background_record_vector_memory(
    conversation_id: Optional[uuid.UUID],
    content: str,
    memory_type: str = "semantic",
    metadata_json: Optional[dict] = None,
) -> None:
    """Background task to asynchronously generate embedding and save vector imprint."""
    try:
        from .database import async_session_factory

        embedding = await cognitive_service.generate_embedding(content)
        async with async_session_factory() as session:
            memory_entry = VectorMemory(
                conversation_id=conversation_id,
                content=content,
                embedding=embedding,
                memory_type=memory_type,
                metadata_json=metadata_json or {},
                importance_score=1.0,
            )
            session.add(memory_entry)
            await session.commit()
            logger.info(f"Background vector memory stored for conversation {conversation_id}")
    except Exception as exc:
        logger.error(f"Failed to record background vector memory: {exc}")


@app.get("/healthz", tags=["System"])
@app.get("/api/health-check", tags=["System"])
async def health_check() -> dict[str, str]:
    """Health check endpoint for Cloud Run and orchestrator probes."""
    return {"status": "healthy", "service": "O.M.N.I.S. Cognitive Architecture"}


@app.post("/api/query", response_model=QueryResponse, tags=["Cognitive Query"])
async def process_user_query(
    request: QueryRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
) -> QueryResponse:
    """
    Main O.M.N.I.S. reasoning endpoint.
    Performs memory retrieval, invokes Google GenAI cognitive loop, computes
    Impact Matrix scores, records the interaction, and enqueues background vector indexing.
    """
    conv_id = request.conversation_id
    conversation: Optional[Conversation] = None

    # Retrieve or create conversation
    if conv_id:
        conversation = await db.get(Conversation, conv_id)

    if not conversation:
        title_snippet = request.query[:45].strip()
        conversation = Conversation(
            title=f"Analýza: {title_snippet}...",
            ontology_domain=request.ontology_domain,
        )
        db.add(conversation)
        await db.flush()
        conv_id = conversation.id

    # Retrieve relevant vector memories using pgvector cosine distance if possible
    context_memories: List[str] = []
    try:
        query_vec = await cognitive_service.generate_embedding(request.query)
        # pgvector cosine distance operator: <=>
        stmt = (
            select(VectorMemory.content)
            .order_by(VectorMemory.embedding.cosine_distance(query_vec))
            .limit(3)
        )
        result = await db.execute(stmt)
        context_memories = [row[0] for row in result.all()]
    except Exception as exc:
        logger.debug(f"pgvector query note (normal during first run without data): {exc}")

    # Process query via Gemini Cognitive Engine
    answer, thoughts, follow_ups, impact_matrix, consequence_forensics, token_stats = await cognitive_service.process_query(
        query=request.query,
        ontology_domain=request.ontology_domain,
        context_memories=context_memories,
        enable_thinking=request.enable_thinking,
    )

    # Save user message
    user_msg = Message(
        conversation_id=conv_id,
        role="user",
        content=request.query,
    )
    db.add(user_msg)

    # Save assistant message
    asst_msg = Message(
        conversation_id=conv_id,
        role="assistant",
        content=answer,
        cognitive_thoughts=thoughts,
        follow_up_questions=follow_ups,
    )
    db.add(asst_msg)
    await db.flush()

    # Save Impact Matrix Metric record
    metric_record = ImpactMatrixMetric(
        message_id=asst_msg.id,
        conversation_id=conv_id,
        economic_viability=impact_matrix.economic_viability,
        eco_social_regeneration=impact_matrix.eco_social_regeneration,
        technological_elegance=impact_matrix.technological_elegance,
        psychological_acceptability=impact_matrix.psychological_acceptability,
        composite_score=impact_matrix.composite_score,
        reasoning=impact_matrix.reasoning,
    )
    db.add(metric_record)
    await db.commit()

    # Background task: embed conversation chunk for autopoietic learning
    background_content = f"Dotaz: {request.query}\nOdpověď: {answer[:300]}"
    background_tasks.add_task(
        background_record_vector_memory,
        conversation_id=conv_id,
        content=background_content,
        memory_type="semantic",
        metadata_json={
            "domain": request.ontology_domain,
            "composite_score": impact_matrix.composite_score,
        },
    )

    return QueryResponse(
        conversation_id=conv_id,
        message_id=asst_msg.id,
        answer=answer,
        cognitive_process=thoughts,
        follow_up_questions=follow_ups,
        impact_matrix=impact_matrix,
        consequence_forensics=consequence_forensics,
        token_usage=token_stats,
        related_memories_count=len(context_memories),
        created_at=asst_msg.created_at,
    )


@app.post("/api/omnis/synthesize-5phases", tags=["OMNIS 5-Phase Engine"])
async def synthesize_five_phases(request: QueryRequest) -> dict:
    """
    Vykoná a vrátí detailní rozpad všech 5 fází kognitivního cyklu O.M.N.I.S.
    včetně prospektivní forenzní analýzy rizik (T+1 až T+N):
      1. Sémantická dekonstrukce
      2. Transdisciplinární křížení (Oktagon 8 domén)
      3. Okamžitý akční plán (Win-Win-Win)
      4. Deterministická exekuce
      5. Autopoietická reflexe a 4D Matice dopadů
      + Prospektivní forenzní analýza následků
    """
    output = assemble_omnis_cognitive_cycle(
        raw_query=request.query,
        ontology_domain=request.ontology_domain,
    )
    from dataclasses import asdict
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
        "formatted_answer": output.formatted_answer,
        "cognitive_process": output.cognitive_process,
        "follow_up_questions": output.follow_up_questions,
        "composite_score": output.composite_score,
    }


@app.post("/api/feedback", response_model=AutopoieticFeedbackResponse, tags=["Autopoiesis"])
async def submit_feedback(
    payload: AutopoieticFeedbackRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
) -> AutopoieticFeedbackResponse:
    """
    Submits user validation/feedback into the self-referential autopoietic loop.
    Adapts network weights and records the delta in vector memory.
    """
    message = await db.get(Message, payload.message_id)
    if not message:
        raise HTTPException(status_code=404, detail="Message not found.")

    # Calculate adaptation delta (-0.1 to +0.1 based on rating 1..5)
    adaptation_delta = (payload.user_rating - 3) * 0.05

    # If adjusted matrix provided, update the stored metric
    if payload.adjusted_matrix:
        stmt = select(ImpactMatrixMetric).where(ImpactMatrixMetric.message_id == message.id)
        result = await db.execute(stmt)
        metric = result.scalar_one_or_none()
        if metric:
            metric.economic_viability = payload.adjusted_matrix.economic_viability
            metric.eco_social_regeneration = payload.adjusted_matrix.eco_social_regeneration
            metric.technological_elegance = payload.adjusted_matrix.technological_elegance
            metric.psychological_acceptability = payload.adjusted_matrix.psychological_acceptability
            metric.composite_score = payload.adjusted_matrix.composite_score
            metric.reasoning = payload.adjusted_matrix.reasoning
            await db.commit()

    # Log autopoietic feedback memory
    fb_text = payload.feedback_text or f"Hodnocení valence: {payload.user_rating}/5"
    background_tasks.add_task(
        background_record_vector_memory,
        conversation_id=payload.conversation_id,
        content=f"[Autopoietická zpětná vazba]: {fb_text}",
        memory_type="autopoietic_feedback",
        metadata_json={"user_rating": payload.user_rating, "delta": adaptation_delta},
    )

    return AutopoieticFeedbackResponse(
        status="success",
        adaptation_delta=round(adaptation_delta, 3),
        message="Autopoietická smyčka byla aktualizována novým otiskem.",
    )


@app.get("/api/memory", response_model=List[MemoryItem], tags=["Memory"])
async def list_memories(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> List[MemoryItem]:
    """Lists recent vector memory imprints."""
    stmt = select(VectorMemory).order_by(desc(VectorMemory.created_at)).limit(limit)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [MemoryItem.model_validate(r) for r in records]


@app.get("/api/conversations", response_model=List[ConversationDetail], tags=["Conversations"])
async def list_conversations(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> List[ConversationDetail]:
    """Returns conversation history."""
    stmt = select(Conversation).order_by(desc(Conversation.updated_at)).limit(limit)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [ConversationDetail.model_validate(r) for r in records]


# ==========================================================
# Epistemic Layer Ingestion (Blueprint Page 3-4)
# ==========================================================

@app.post("/api/epistemic/ingest", tags=["OMNIS Epistemic Layer"])
async def ingest_epistemic_data(
    payload: OmnisEntityIngestion,
    background_tasks: BackgroundTasks,
) -> dict:
    """
    Epistemická (Poznávací) vrstva:
    Sběr a ontologické mapování heterogenních dat (tvrdá, měkká, heuristická).
    Eliminuje sémantický šum před vstupem do syntetické roviny.
    """
    summary = (
        f"Entity: {payload.omnis_entity_id} | Purpose: {payload.fundamental_purpose} | "
        f"Hard: {list(payload.epistemic_data_layer.hard_data.parameters.keys())} | "
        f"Soft: {list(payload.epistemic_data_layer.soft_data.parameters.keys())}"
    )
    background_tasks.add_task(
        background_record_vector_memory,
        conversation_id=None,
        content=f"[Epistemic Ingest]: {summary}",
        memory_type="epistemic_entity",
        metadata_json=payload.model_dump(),
    )
    return {
        "status": "assimilated",
        "omnis_entity_id": payload.omnis_entity_id,
        "message": "Entita byla úspěšně asimilována do epistemické roviny bez sémantického šumu.",
    }


# ==========================================================
# Fáze IV: Synergická Konvergence - Guardrail (Blueprint Page 9-11)
# ==========================================================

phase4_router = APIRouter(prefix="/omnis/phase-4", tags=["OMNIS Phase IV - Convergence"])


@phase4_router.post("/evaluate-matrix", response_model=ConvergenceEvaluationResponse)
async def evaluate_impact_matrix(payload: ConvergenceEvaluationRequest):
    """
    Fáze IV (Synergická Konvergence) dle blueprintu (str. 10-11):
    Deterministický výstupní validátor (Guardrail).
    Váhy:
      - Ekonomická životaschopnost: 0.3
      - Technologická elegance: 0.3
      - Ekologicko-sociální dopad: 0.2
      - Psychologická přijatelnost: 0.2
      - Penalizace za každou adversarial zranitelnost: -0.5
    """
    if not payload.candidates:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Seznam kandidátů pro vyhodnocení je prázdný.",
        )

    rankings = []
    for cand in payload.candidates:
        s = cand.scores
        avg_score = (
            s.economic_viability * 0.3
            + s.tech_elegance * 0.3
            + s.social_ecological_impact * 0.2
            + s.psychological_acceptance * 0.2
        )
        penalty = len(cand.adversarial_vulnerabilities) * 0.5
        final_score = max(0.0, avg_score - penalty)

        rankings.append(
            {
                "candidate_id": cand.candidate_id,
                "title": cand.title,
                "final_score": round(final_score, 2),
                "avg_before_penalty": round(avg_score, 2),
                "vulnerabilities_count": len(cand.adversarial_vulnerabilities),
                "passed": final_score >= payload.minimum_threshold,
                "candidate_object": cand,
            }
        )

    rankings.sort(key=lambda x: x["final_score"], reverse=True)
    passed_candidates = [r for r in rankings if r["passed"]]
    optimal = passed_candidates[0]["candidate_object"] if passed_candidates else None

    return ConvergenceEvaluationResponse(
        selected_optimal_candidate=optimal,
        weighted_rankings=[
            {k: v for k, v in r.items() if k != "candidate_object"} for r in rankings
        ],
        status=(
            "SUCCESS"
            if optimal
            else "WARNING: Žádný kandidát nepřekročil prahovou hodnotu."
        ),
    )


app.include_router(phase4_router)


# ==========================================================
# Dev & Diagnostic Laboratoř (Token Telemetrie & Test Prompty)
# Interní vývojový modul - není určen pro produkci
# ==========================================================

dev_router = APIRouter(prefix="/api/dev", tags=["Development & Diagnostics"])


@dev_router.get("/token-telemetry")
async def get_token_telemetry():
    """Vrací kumulativní statistiky spotřeby tokenů pro aktuální instanci."""
    return token_telemetry_service.get_telemetry()


@dev_router.post("/estimate-tokens")
async def estimate_query_tokens(payload: dict):
    """Vypočítá předpokládanou spotřebu tokenů pro zadaný dotaz."""
    query = payload.get("query", "")
    include_sys = payload.get("include_system_prompt", True)
    include_mem = payload.get("include_memory_context", True)
    return token_telemetry_service.estimate_query_tokens(
        query=query,
        include_system_prompt=include_sys,
        include_memory_context=include_mem,
    )


@dev_router.post("/reset-tokens")
async def reset_token_telemetry():
    """Resetuje relaci počítadla tokenů."""
    token_telemetry_service.reset_telemetry()
    return {"status": "reset_successful", "message": "Počítadlo tokenů bylo vynulováno."}


app.include_router(dev_router)

# ==========================================================
# Static Frontend Serving (Cloud Run & Web Production)
# ==========================================================

import os
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

frontend_dist = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "dist"))
if os.path.exists(frontend_dist):
    assets_dir = os.path.join(frontend_dist, "assets")
    if os.path.exists(assets_dir):
        app.mount("/assets", StaticFiles(directory=assets_dir), name="assets")

    @app.get("/{full_path:path}")
    async def serve_spa(full_path: str):
        if full_path.startswith("api") or full_path.startswith("omnis") or full_path in ("docs", "redoc", "openapi.json"):
            raise HTTPException(status_code=404, detail="Not Found")
        file_path = os.path.join(frontend_dist, full_path)
        if os.path.isfile(file_path):
            return FileResponse(file_path)
        index_file = os.path.join(frontend_dist, "index.html")
        if os.path.exists(index_file):
            return FileResponse(index_file)
        raise HTTPException(status_code=404, detail="Frontend dist not found")

