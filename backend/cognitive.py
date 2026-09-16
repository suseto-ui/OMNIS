import logging
import uuid
from datetime import datetime, timezone
from typing import Optional, List
from fastapi import APIRouter, BackgroundTasks, Depends
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
    TokenUsageStats,
    AdversarialReviewRequest,
    AdversarialReviewResponse,
)
from backend.memory import background_record_vector_memory

logger = logging.getLogger("omnis.api")
cognitive_router = APIRouter(prefix="/api", tags=["Cognitive Query"])

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
        conv_id = uuid.uuid4()
        conversation = Conversation(
            id=str(conv_id),
            title=f"Analýza: {title_snippet}...",
            ontology_domain=request.ontology_domain,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
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
                conversation_id=conv_id,
                role="assistant",
                content=c_meta["answer"],
                cognitive_thoughts=c_meta.get("cognitive_process", "") + "\n\n[⚡ EXEKUOVÁNO Z PGVECTOR SEMANTIC CACHE: 0ms LATENCE, $0 NÁKLAD]",
                follow_up_questions=c_meta.get("follow_up_questions", []),
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
                conversation_id=conv_id,
                message_id=cached_msg.id,
                answer=c_meta["answer"],
                cognitive_process=cached_msg.cognitive_thoughts,
                follow_up_questions=cached_msg.follow_up_questions,
                impact_matrix=im_scores,
                consequence_forensics=cf_obj,
                token_usage=TokenUsageStats(prompt_tokens=0, completion_tokens=0, total_tokens=0, cost_usd=0.0),
                related_memories_count=1,
                created_at=cached_msg.created_at
            )
        stmt = (
            select(VectorMemory.content)
            .order_by(VectorMemory.embedding.cosine_distance(query_vec))
            .limit(3)
        )
        result = await db.execute(stmt)
        context_memories = [row[0] for row in result.all()]
    except Exception as exc:
        logger.debug(f"pgvector query note (normal during first run without data): {exc}")

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

    return QueryResponse(
        conversation_id=conv_id, message_id=asst_msg.id, answer=answer, cognitive_process=thoughts,
        follow_up_questions=follow_ups, impact_matrix=impact_matrix, consequence_forensics=consequence_forensics,
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
