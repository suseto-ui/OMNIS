import uuid
import logging
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database import get_db, async_session_factory
from backend.gemini_service import cognitive_service
from backend.models import Conversation, VectorMemory
from backend.schemas import ConversationDetail, MemoryItem

logger = logging.getLogger("omnis.api")
memory_router = APIRouter(prefix="/api", tags=["Memory & Conversations"])

async def background_record_vector_memory(
    conversation_id: Optional[uuid.UUID],
    content: str,
    memory_type: str = "semantic",
    metadata_json: Optional[dict] = None,
) -> None:
    """Background task to asynchronously generate embedding and save vector imprint."""
    try:
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
        # Log full exception with stacktrace for debugging and telemetry
        logger.exception("Failed to record background vector memory: %s", exc)


@memory_router.get("/memory", response_model=List[MemoryItem])
async def list_memories(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> List[MemoryItem]:
    stmt = select(VectorMemory).order_by(desc(VectorMemory.created_at)).limit(limit)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [MemoryItem.model_validate(r) for r in records]


@memory_router.get("/conversations", response_model=List[ConversationDetail])
async def list_conversations(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> List[ConversationDetail]:
    stmt = select(Conversation).order_by(desc(Conversation.updated_at)).limit(limit)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [ConversationDetail.model_validate(r) for r in records]
