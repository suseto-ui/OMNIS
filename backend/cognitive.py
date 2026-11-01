import json
import asyncio
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional, List
from fastapi import APIRouter, BackgroundTasks, Depends
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database import get_db
from backend.gemini_service import cognitive_service
from backend.models import Conversation, Message, ImpactMatrixMetric, VectorMemory
from backend.schemas import (
    QueryRequest,
    QueryResponse,
    ImpactMatrixScores,
    ConsequenceForensicsSchema,
    CognitiveNodeMetricsSchema,
    TokenUsageStats,
    AdversarialReviewRequest,
    AdversarialReviewResponse,
)
from backend.memory import background_record_vector_memory
from sqlalchemy import text

logger = logging.getLogger("omnis.api")
cognitive_router = APIRouter(prefix="/api", tags=["Cognitive Query"])

async def hybrid_retrieve_memories(
    db: AsyncSession,
    query_text: str,
    query_vec: List[float],
    top_k: int = 3,
    rrf_k: int = 60
) -> List[str]:
    """
    Hybrid Epistemic Retrieval combining Dense Semantic Search (HNSW pgvector)
    and Sparse Keyword Search (PostgreSQL GIN tsvector) using Reciprocal Rank Fusion (RRF).
    """
    scores: dict[str, float] = {}
    
    # 1. Dense Semantic Search (pgvector)
    try:
        dense_stmt = (
            select(VectorMemory.content)
            .filter(VectorMemory.memory_type != "semantic_cache")
            .order_by(VectorMemory.embedding.cosine_distance(query_vec))
            .limit(5)
        )
        dense_res = await db.execute(dense_stmt)
        dense_rows = [r[0] for r in dense_res.all()]
        for rank, content in enumerate(dense_rows):
            scores[content] = scores.get(content, 0.0) + (1.0 / (rrf_k + rank))
    except Exception as d_err:
        logger.debug(f"Dense vector retrieval notice: {d_err}")

    # 2. Sparse Keyword Search (tsvector full-text)
    try:
        sparse_stmt = (
            select(VectorMemory.content)
            .filter(VectorMemory.memory_type != "semantic_cache")
            .filter(text("to_tsvector('simple', content) @@ plainto_tsquery('simple', :q)"))
            .params(q=query_text)
            .limit(5)
        )
        sparse_res = await db.execute(sparse_stmt)
        sparse_rows = [r[0] for r in sparse_res.all()]
        for rank, content in enumerate(sparse_rows):
            scores[content] = scores.get(content, 0.0) + (1.0 / (rrf_k + rank))
    except Exception as s_err:
        logger.debug(f"Sparse keyword retrieval notice: {s_err}")

    # Sort candidates by combined RRF score
    sorted_candidates = sorted(scores.keys(), key=lambda c: scores[c], reverse=True)
    return sorted_candidates[:top_k]

def calculate_cognitive_node_metrics(text: str) -> CognitiveNodeMetricsSchema:
    if not text:
        return CognitiveNodeMetricsSchema()
    words = text.split()
    unique_words = set(w.lower() for w in words)
    lexical_diversity = len(unique_words) / len(words) if words else 0.5
    raw_entropy = round(0.05 + (1.0 - lexical_diversity) * 0.35, 2)
    homeostasis = round(max(90.0, min(99.9, 99.8 - raw_entropy * 12)), 1)
    tech_count = sum(1 for w in words if len(w) > 5 or any(c.isupper() for c in w))
    density = round(max(72.0, min(98.5, (tech_count / max(1, len(words))) * 100 + 42)), 1)
    branches = min(5, max(1, text.count("?") + text.count(":") + text.count("-")))
    return CognitiveNodeMetricsSchema(
        entity_density_pct=density,
        entropy_val=raw_entropy,
        homeostasis_pct=homeostasis,
        stochastic_branches=branches
    )

@cognitive_router.post("/query", response_model=QueryResponse)
async def process_user_query(
    request: QueryRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
) -> QueryResponse:
    conv_id = request.conversation_id
    conversation: Optional[Conversation] = None

    if conv_id:
        conversation = await db.get(Conversation, conv_id)

    if not conversation:
        title_snippet = request.query[:45].strip()
        conv_id = str(conv_id) if conv_id else str(uuid.uuid4())
        conversation = Conversation(
            id=conv_id,
            title=f"Analýza: {title_snippet}...",
            ontology_domain=request.ontology_domain,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        db.add(conversation)
        await db.commit()
    else:
        conv_id = str(conversation.id)
        conversation.updated_at = datetime.now(timezone.utc)
        db.add(conversation)
        await db.commit()

    context_memories: List[str] = []
    try:
        query_vec = await cognitive_service.generate_embedding(request.query)

        cache_stmt = (
            select(VectorMemory)
            .filter(VectorMemory.memory_type == "semantic_cache")
            .filter(VectorMemory.embedding.cosine_distance(query_vec) < 0.02)
            .order_by(VectorMemory.embedding.cosine_distance(query_vec))
            .limit(1)
        )
        cache_result = await db.execute(cache_stmt)
        cached_record = cache_result.scalar_one_or_none()

        if cached_record and cached_record.metadata_json:
            logging.info(f"⚡ O.M.N.I.S. SEMANTIC CACHE HIT (Bypass Gemini LLM): {request.query}")
            c_meta = cached_record.metadata_json
            
            cached_msg = Message(
                id=str(uuid.uuid4()),
                conversation_id=conv_id,
                role="assistant",
                content=c_meta["answer"],
                cognitive_thoughts=c_meta.get("cognitive_process", "") + "\n\n[⚡ EXEKUOVÁNO Z PGVECTOR SEMANTIC CACHE: 0ms LATENCE, $0 NÁKLAD]",
                follow_up_questions=c_meta.get("follow_up_questions", []),
                created_at=datetime.now(timezone.utc),
            )
            db.add(cached_msg)
            await db.commit()
            
            im = c_meta.get("impact_matrix", {})
            im_scores = ImpactMatrixScores(
                sys=im.get("sys", 0.5), econ=im.get("econ", 0.5), psych=im.get("psych", 0.5),
                eco=im.get("eco", 0.5), law=im.get("law", 0.5), sec=im.get("sec", 0.5),
                phys=im.get("phys", 0.5), soc=im.get("soc", 0.5), composite_score=im.get("composite_score", 0.5),
                reasoning=im.get("reasoning", "")
            )
            
            cf_dict = c_meta.get("consequence_forensics", {})
            cf_obj = None
            if cf_dict:
                cf_obj = ConsequenceForensicsSchema(
                    horizon=cf_dict.get("horizon", "T+1"),
                    risk_index=cf_dict.get("risk_index", 0.1),
                    risk_level=cf_dict.get("risk_level", "SAFE"),
                    identified_vectors=cf_dict.get("identified_vectors", []),
                    t_plus_1_systemic_drift=cf_dict.get("t_plus_1_systemic_drift", ""),
                    thermodynamic_entropy_spike=cf_dict.get("thermodynamic_entropy_spike", "")
                )
                
            return QueryResponse(
                conversation_id=str(conv_id),
                message_id=str(cached_msg.id),
                answer=c_meta["answer"],
                cognitive_process=cached_msg.cognitive_thoughts,
                follow_up_questions=cached_msg.follow_up_questions,
                impact_matrix=im_scores,
                consequence_forensics=cf_obj,
                cognitive_node_metrics=calculate_cognitive_node_metrics(c_meta["answer"]),
                token_usage=TokenUsageStats(prompt_tokens=0, completion_tokens=0, total_tokens=0, cost_usd=0.0),
                related_memories_count=1,
                created_at=cached_msg.created_at
            )

        # Use Hybrid RAG Retrieval (Dense HNSW + Sparse tsvector) with RRF
        context_memories = await hybrid_retrieve_memories(
            db=db,
            query_text=request.query,
            query_vec=query_vec,
            top_k=3
        )
    except Exception as exc:
        logger.debug(f"pgvector hybrid query note (normal during first run without data): {exc}")

    answer, thoughts, follow_ups, impact_matrix, consequence_forensics, token_stats, adv_score, flagged_issues = await cognitive_service.process_query(
        query=request.query,
        ontology_domain=request.ontology_domain,
        context_memories=context_memories,
        enable_thinking=request.enable_thinking,
        history=request.history,
        image_data=request.image_data,
        image_mime=request.image_mime,
    )

    user_msg = Message(
        id=str(uuid.uuid4()),
        conversation_id=str(conv_id),
        role="user",
        content=request.query,
        created_at=datetime.now(timezone.utc),
    )
    db.add(user_msg)

    asst_msg = Message(
        id=str(uuid.uuid4()),
        conversation_id=str(conv_id),
        role="assistant",
        content=answer,
        cognitive_thoughts=thoughts,
        follow_up_questions=follow_ups,
        created_at=datetime.now(timezone.utc),
    )
    db.add(asst_msg)
    await db.flush()

    metric_record = ImpactMatrixMetric(
        id=str(uuid.uuid4()),
        message_id=str(asst_msg.id),
        conversation_id=str(conv_id),
        sys=impact_matrix.sys, econ=impact_matrix.econ, psych=impact_matrix.psych,
        eco=impact_matrix.eco, law=impact_matrix.law, sec=impact_matrix.sec,
        phys=impact_matrix.phys, soc=impact_matrix.soc,
        composite_score=impact_matrix.composite_score, reasoning=impact_matrix.reasoning,
    )
    db.add(metric_record)
    
    try:
        await db.commit()
    except Exception as exc:
        await db.rollback()
        logger.error(f"Chyba při zápisu konverzace do DB: {exc}. Odpověď přesto vracíme.")

    background_content = f"Dotaz: {request.query}\nOdpověď: {answer[:300]}"
    consequence_forensics_dump = (
        consequence_forensics.model_dump()
        if hasattr(consequence_forensics, "model_dump")
        else (consequence_forensics or {})
    )
    cache_meta = {
        "answer": answer, "cognitive_process": thoughts, "follow_up_questions": follow_ups,
        "impact_matrix": impact_matrix.model_dump() if impact_matrix else {},
        "consequence_forensics": consequence_forensics_dump,
    }
    background_tasks.add_task(background_record_vector_memory, conversation_id=conv_id, content=request.query, memory_type="semantic_cache", metadata_json=cache_meta)
    background_tasks.add_task(background_record_vector_memory, conversation_id=conv_id, content=background_content, memory_type="semantic", metadata_json={"domain": request.ontology_domain, "composite_score": impact_matrix.composite_score})

    cog_metrics = calculate_cognitive_node_metrics(answer)

    return QueryResponse(
        conversation_id=str(conv_id), message_id=str(asst_msg.id), answer=answer, cognitive_process=thoughts,
        follow_up_questions=follow_ups, impact_matrix=impact_matrix, consequence_forensics=consequence_forensics,
        cognitive_node_metrics=cog_metrics,
        token_usage=token_stats, related_memories_count=len(context_memories), created_at=asst_msg.created_at,
        adversarial_score=adv_score, flagged_issues=flagged_issues
    )


@cognitive_router.post("/adversarial-review", response_model=AdversarialReviewResponse)
async def perform_adversarial_review(request: AdversarialReviewRequest) -> AdversarialReviewResponse:
    """
    Triggers an independent adversarial critique pass by a secondary model
    tasked with acting as a 'Skeptical Opponent' to evaluate primary reasoning.
    """
    vulnerabilities, critique_summary, adv_score, flagged_issues = await cognitive_service.run_adversarial_red_team(
        query=request.query,
        answer=request.answer,
        ontology_domain=request.ontology_domain,
        current_matrix={}
    )
    return AdversarialReviewResponse(
        vulnerabilities=vulnerabilities,
        critique_summary=critique_summary,
        adversarial_score=adv_score,
        flagged_issues=flagged_issues
    )


@cognitive_router.post("/query/stream")
async def process_user_query_stream(
    request: QueryRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
):
    """
    Streams the 5-phase cognitive synthesis in real-time via Server-Sent Events (SSE).
    Emits events: init, phase_start, token_chunk, matrix_update, consequence_forensics, complete.
    """
    conv_id = request.conversation_id
    conversation: Optional[Conversation] = None

    if conv_id:
        conversation = await db.get(Conversation, conv_id)

    if not conversation:
        title_snippet = request.query[:45].strip()
        conv_id = str(conv_id) if conv_id else str(uuid.uuid4())
        conversation = Conversation(
            id=conv_id,
            title=f"Analýza: {title_snippet}...",
            ontology_domain=request.ontology_domain,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        db.add(conversation)
        await db.commit()
    else:
        conv_id = str(conversation.id)
        conversation.updated_at = datetime.now(timezone.utc)
        db.add(conversation)
        await db.commit()

    async def event_generator():
        yield f"event: init\ndata: {json.dumps({'conversation_id': str(conv_id), 'status': 'connected'})}\n\n"
        await asyncio.sleep(0.01)

        # 1. Epistemic retrieval & Cache check
        context_memories: List[str] = []
        try:
            query_vec = await cognitive_service.generate_embedding(request.query)
            cache_stmt = (
                select(VectorMemory)
                .filter(VectorMemory.memory_type == "semantic_cache")
                .filter(VectorMemory.embedding.cosine_distance(query_vec) < 0.02)
                .order_by(VectorMemory.embedding.cosine_distance(query_vec))
                .limit(1)
            )
            cache_result = await db.execute(cache_stmt)
            cached_record = cache_result.scalar_one_or_none()

            if cached_record and cached_record.metadata_json:
                c_meta = cached_record.metadata_json
                yield f"event: cache_hit\ndata: {json.dumps({'message': '⚡ EXEKUOVÁNO Z PGVECTOR SEMANTIC CACHE'})}\n\n"
                
                # Stream cached thoughts and answer smoothly
                yield f"event: phase_start\ndata: {json.dumps({'phase': 1, 'title': 'Sémantická Mezipaměť (Cache Hit)'})}\n\n"
                answer_text = c_meta.get("answer", "")
                words = answer_text.split()
                chunk_size = 6
                for i in range(0, len(words), chunk_size):
                    chunk = " ".join(words[i:i+chunk_size]) + " "
                    yield f"event: token_chunk\ndata: {json.dumps({'chunk': chunk, 'phase': 5})}\n\n"
                    await asyncio.sleep(0.01)

                im = c_meta.get("impact_matrix", {})
                yield f"event: matrix_update\ndata: {json.dumps(im)}\n\n"
                
                complete_payload = {
                    "conversation_id": str(conv_id),
                    "message_id": str(uuid.uuid4()),
                    "answer": c_meta.get("answer", ""),
                    "cognitive_process": c_meta.get("cognitive_process", "") + "\n\n[⚡ EXEKUOVÁNO Z PGVECTOR SEMANTIC CACHE: 0ms LATENCE]",
                    "follow_up_questions": c_meta.get("follow_up_questions", []),
                    "impact_matrix": im,
                    "consequence_forensics": c_meta.get("consequence_forensics", None),
                    "cognitive_node_metrics": calculate_cognitive_node_metrics(c_meta.get("answer", "")).model_dump(),
                    "token_usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0, "cost_usd": 0.0},
                    "related_memories_count": 1,
                    "created_at": datetime.now(timezone.utc).isoformat()
                }
                yield f"event: complete\ndata: {json.dumps(complete_payload)}\n\n"
                return

            context_memories = await hybrid_retrieve_memories(
                db=db,
                query_text=request.query,
                query_vec=query_vec,
                top_k=3
            )
        except Exception as exc:
            logger.debug(f"Streaming hybrid cache lookup note: {exc}")

        # Stream phase 1 start
        yield f"event: phase_start\ndata: {json.dumps({'phase': 1, 'title': 'Fáze I: Dekonstrukce & Invarianty', 'domain': request.ontology_domain})}\n\n"
        await asyncio.sleep(0.02)

        yield f"event: phase_start\ndata: {json.dumps({'phase': 2, 'title': 'Fáze II: Transdisciplinární Izomorfismus & Pákový bod'})}\n\n"
        await asyncio.sleep(0.02)

        # Execute cognitive query
        answer, thoughts, follow_ups, impact_matrix, consequence_forensics, token_stats, adv_score, flagged_issues = await cognitive_service.process_query(
            query=request.query,
            ontology_domain=request.ontology_domain,
            context_memories=context_memories,
            enable_thinking=request.enable_thinking,
            history=request.history,
            image_data=request.image_data,
            image_mime=request.image_mime,
        )

        yield f"event: phase_start\ndata: {json.dumps({'phase': 3, 'title': 'Fáze III: Kvantitativní Formule & Poměr úsilí ku páce'})}\n\n"
        await asyncio.sleep(0.01)

        yield f"event: phase_start\ndata: {json.dumps({'phase': 4, 'title': 'Fáze IV: Deterministický Výstup & Produkční Kód'})}\n\n"

        # Stream answer chunks for typewriter effect
        words = answer.split()
        chunk_size = 5
        for i in range(0, len(words), chunk_size):
            chunk = " ".join(words[i:i+chunk_size]) + " "
            yield f"event: token_chunk\ndata: {json.dumps({'chunk': chunk, 'phase': 4})}\n\n"
            await asyncio.sleep(0.008)

        # Stream impact matrix and forensics
        im_dump = impact_matrix.model_dump() if hasattr(impact_matrix, "model_dump") else (impact_matrix or {})
        yield f"event: matrix_update\ndata: {json.dumps(im_dump)}\n\n"

        cf_dump = consequence_forensics.model_dump() if hasattr(consequence_forensics, "model_dump") else (consequence_forensics or {})
        if cf_dump:
            yield f"event: consequence_forensics\ndata: {json.dumps(cf_dump)}\n\n"

        # Record messages to DB
        asst_msg_id = str(uuid.uuid4())
        user_msg = Message(
            id=str(uuid.uuid4()),
            conversation_id=str(conv_id),
            role="user",
            content=request.query,
            created_at=datetime.now(timezone.utc),
        )
        db.add(user_msg)

        asst_msg = Message(
            id=asst_msg_id,
            conversation_id=str(conv_id),
            role="assistant",
            content=answer,
            cognitive_thoughts=thoughts,
            follow_up_questions=follow_ups,
            created_at=datetime.now(timezone.utc),
        )
        db.add(asst_msg)

        metric_record = ImpactMatrixMetric(
            id=str(uuid.uuid4()),
            message_id=asst_msg_id,
            conversation_id=str(conv_id),
            sys=impact_matrix.sys, econ=impact_matrix.econ, psych=impact_matrix.psych,
            eco=impact_matrix.eco, law=impact_matrix.law, sec=impact_matrix.sec,
            phys=impact_matrix.phys, soc=impact_matrix.soc,
            composite_score=impact_matrix.composite_score, reasoning=impact_matrix.reasoning,
        )
        db.add(metric_record)

        try:
            await db.commit()
        except Exception as exc:
            await db.rollback()
            logger.error(f"Chyba při zápisu streamované konverzace: {exc}")

        # Record cache in background
        background_content = f"Dotaz: {request.query}\nOdpověď: {answer[:300]}"
        cache_meta = {
            "answer": answer, "cognitive_process": thoughts, "follow_up_questions": follow_ups,
            "impact_matrix": im_dump,
            "consequence_forensics": cf_dump,
        }
        background_tasks.add_task(background_record_vector_memory, conversation_id=conv_id, content=request.query, memory_type="semantic_cache", metadata_json=cache_meta)
        background_tasks.add_task(background_record_vector_memory, conversation_id=conv_id, content=background_content, memory_type="semantic", metadata_json={"domain": request.ontology_domain, "composite_score": impact_matrix.composite_score})

        cog_metrics = calculate_cognitive_node_metrics(answer).model_dump()
        token_stats_dump = token_stats.model_dump() if hasattr(token_stats, "model_dump") else token_stats

        final_response_obj = {
            "conversation_id": str(conv_id),
            "message_id": asst_msg_id,
            "answer": answer,
            "cognitive_process": thoughts,
            "follow_up_questions": follow_ups,
            "impact_matrix": im_dump,
            "consequence_forensics": cf_dump,
            "cognitive_node_metrics": cog_metrics,
            "token_usage": token_stats_dump,
            "related_memories_count": len(context_memories),
            "created_at": asst_msg.created_at.isoformat(),
            "adversarial_score": adv_score,
            "flagged_issues": flagged_issues
        }
        yield f"event: complete\ndata: {json.dumps(final_response_obj)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )

