from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database import get_db
from backend.models import Message, ImpactMatrixMetric
from backend.schemas import AutopoieticFeedbackRequest, AutopoieticFeedbackResponse
from backend.memory import background_record_vector_memory

autopoiesis_router = APIRouter(prefix="/api", tags=["Autopoiesis"])

@autopoiesis_router.post("/feedback", response_model=AutopoieticFeedbackResponse)
async def submit_feedback(
    payload: AutopoieticFeedbackRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
) -> AutopoieticFeedbackResponse:
    message = await db.get(Message, payload.message_id)
    if not message:
        raise HTTPException(status_code=404, detail="Message not found.")

    adaptation_delta = (payload.user_rating - 3) * 0.05

    if payload.adjusted_matrix:
        stmt = select(ImpactMatrixMetric).where(ImpactMatrixMetric.message_id == message.id)
        result = await db.execute(stmt)
        metric = result.scalar_one_or_none()
        if metric:
            metric.sys = payload.adjusted_matrix.sys
            metric.econ = payload.adjusted_matrix.econ
            metric.psych = payload.adjusted_matrix.psych
            metric.eco = payload.adjusted_matrix.eco
            metric.law = payload.adjusted_matrix.law
            metric.sec = payload.adjusted_matrix.sec
            metric.phys = payload.adjusted_matrix.phys
            metric.soc = payload.adjusted_matrix.soc
            metric.composite_score = payload.adjusted_matrix.composite_score
            metric.reasoning = payload.adjusted_matrix.reasoning
            await db.commit()

    fb_text = payload.feedback_text or f"Hodnocení valence: {payload.user_rating}/5"

    background_tasks.add_task(
        background_record_vector_memory,
        conversation_id=payload.conversation_id,
        content=f"[Autopoietická zpětná vazba]: {fb_text}",
        memory_type="autopoietic_feedback",
        metadata_json={"user_rating": payload.user_rating, "delta": adaptation_delta},
    )

    return AutopoieticFeedbackResponse(
        status="success", adaptation_delta=round(adaptation_delta, 3), message="Autopoietická smyčka byla aktualizována novým otiskem.",)