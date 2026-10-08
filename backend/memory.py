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


@memory_router.get("/conversations", response_model=List[ConversationDetail])
async def list_conversations(
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> List[ConversationDetail]:
    """Return the most recently updated conversations."""
    stmt = select(Conversation).order_by(desc(Conversation.updated_at)).limit(limit)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [ConversationDetail.model_validate(record) for record in records]

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


from pydantic import BaseModel
from datetime import datetime, timezone

class SyncThreadsRequest(BaseModel):
    threads: List[dict]

@memory_router.post("/memory/sync")
async def sync_threads(
    payload: SyncThreadsRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Asynchronní synchronizace konverzačních vláken z klientské IndexedDB do PostgreSQL / Cloud SQL.
    """
    synced_count = 0
    try:
        for t in payload.threads:
            t_id = t.get("id")
            if not t_id:
                continue
            
            # Skupina konverzace podle ID
            stmt = select(Conversation).where(Conversation.id == str(t_id))
            result = await db.execute(stmt)
            existing_conv = result.scalar_one_or_none()
            
            if not existing_conv:
                new_conv = Conversation(
                    id=str(t_id),
                    title=t.get("title", "Synchronizované Vlákno"),
                    ontology_domain=t.get("ontologyDomain", "SYSTEMS_INTELLIGENCE"),
                    created_at=datetime.now(timezone.utc),
                    updated_at=datetime.now(timezone.utc)
                )
                db.add(new_conv)
                synced_count += 1
        
        await db.commit()
        return {
            "status": "success",
            "message": f"Úspěšně synchronizováno {synced_count} nově vytvořených vláken z IndexedDB.",
            "synced_count": synced_count
        }
    except Exception as exc:
        await db.rollback()
        logger.error(f"Chyba při asynchronní synchronizaci z IndexedDB: {exc}")
        return {
            "status": "partial_success",
            "message": f"Chyba synchronizace (uloženo lokálně v IndexedDB): {str(exc)}",
            "synced_count": 0
        }
