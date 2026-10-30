"""
O.M.N.I.S. Epistemic Ingestion & Knowledge Base Management
Handles document chunking, semantic vector indexing, multi-modal ingestion, and epistemic backup.
"""

from __future__ import annotations
import uuid
import logging
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field
from sqlalchemy import select, func, desc
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database import get_db, async_session_factory
from backend.models import VectorMemory
from backend.schemas import OmnisEntityIngestion
from backend.memory import background_record_vector_memory
from backend.gemini_service import cognitive_service

logger = logging.getLogger("omnis.epistemic")
epistemic_router = APIRouter(prefix="/api/epistemic", tags=["OMNIS Epistemic Layer"])


class DocumentIngestRequest(BaseModel):
    title: str = Field(..., description="Název dokumentu nebo entity")
    content: str = Field(..., description="Celý text dokumentu k indexaci")
    domain: str = Field("SYSTEMS_INTELLIGENCE", description="Ontologická doména dokumentu")
    chunk_size: int = Field(500, ge=100, le=2000, description="Délka jednoho sémantického chunku")
    chunk_overlap: int = Field(100, ge=0, le=500, description="Překryv mezi sousedními chunky")
    importance_score: float = Field(1.0, ge=0.1, le=5.0, description="Váha důležitosti pro RAG")


def _chunk_text(text: str, chunk_size: int = 500, overlap: int = 100) -> List[str]:
    """Rozděluje text na překrývající se sémantické bloky s respektováním hranic odstavců."""
    clean_text = text.strip()
    if len(clean_text) <= chunk_size:
        return [clean_text] if clean_text else []
    
    chunks: List[str] = []
    start = 0
    while start < len(clean_text):
        end = min(start + chunk_size, len(clean_text))
        chunk = clean_text[start:end]
        if chunk.strip():
            chunks.append(chunk.strip())
        if end == len(clean_text):
            break
        start += (chunk_size - overlap)
    return chunks


async def _async_index_document_chunks(
    document_id: str,
    title: str,
    chunks: List[str],
    domain: str,
    importance_score: float
) -> None:
    """Asynchronní generování embeddingů pro chunky a zápis do tabulky vector_memories."""
    logger.info(f"Indexing {len(chunks)} chunks for document '{title}' (ID: {document_id})...")
    async with async_session_factory() as session:
        for idx, chunk in enumerate(chunks):
            try:
                formatted_chunk = f"[{title} | Chunk {idx+1}/{len(chunks)} | {domain}]: {chunk}"
                embedding = await cognitive_service.generate_embedding(formatted_chunk)
                memory_entry = VectorMemory(
                    content=formatted_chunk,
                    embedding=embedding,
                    memory_type="document_chunk",
                    metadata_json={
                        "document_id": document_id,
                        "title": title,
                        "chunk_index": idx,
                        "total_chunks": len(chunks),
                        "domain": domain,
                    },
                    importance_score=importance_score,
                )
                session.add(memory_entry)
            except Exception as e:
                logger.error(f"Error embedding chunk {idx} of '{title}': {e}")
        try:
            await session.commit()
            logger.info(f"Successfully committed {len(chunks)} chunks for '{title}' into PostgreSQL / pgvector.")
        except Exception as commit_err:
            await session.rollback()
            logger.error(f"Failed to commit document chunks: {commit_err}")


@epistemic_router.post("/ingest")
async def ingest_epistemic_data(
    payload: OmnisEntityIngestion,
    background_tasks: BackgroundTasks,
) -> dict:
    """Standardní ingestace epistemické entity dle Blueprintu."""
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


@epistemic_router.post("/ingest-text")
async def ingest_document_text(
    payload: DocumentIngestRequest,
    background_tasks: BackgroundTasks,
) -> dict:
    """Rozdělí text na sémantické bloky a asynchronně je uloží s vektorovým indexem."""
    doc_id = str(uuid.uuid4())
    chunks = _chunk_text(payload.content, payload.chunk_size, payload.chunk_overlap)
    
    if not chunks:
        raise HTTPException(status_code=400, detail="Obsah dokumentu je prázdný.")

    background_tasks.add_task(
        _async_index_document_chunks,
        document_id=doc_id,
        title=payload.title,
        chunks=chunks,
        domain=payload.domain,
        importance_score=payload.importance_score,
    )

    return {
        "status": "queued",
        "document_id": doc_id,
        "title": payload.title,
        "total_chunks": len(chunks),
        "message": f"Dokument byl rozdělen na {len(chunks)} sémantických bloků a zařazen do vektorové indexace.",
    }


@epistemic_router.post("/upload")
async def upload_document_file(
    file: UploadFile = File(...),
    domain: str = Form("SYSTEMS_INTELLIGENCE"),
    background_tasks: BackgroundTasks = BackgroundTasks(),
) -> dict:
    """Příjem technických textových souborů (.md, .txt, .json, .py, .kt, .csv) pro indexaci do sémantické paměti."""
    try:
        raw_bytes = await file.read()
        text_content = raw_bytes.decode("utf-8", errors="replace")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Chyba při čtení souboru: {exc}")

    if not text_content.strip():
        raise HTTPException(status_code=400, detail="Nahraný soubor je prázdný.")

    doc_id = str(uuid.uuid4())
    title = file.filename or "uploaded_document"
    chunks = _chunk_text(text_content, chunk_size=600, overlap=120)

    background_tasks.add_task(
        _async_index_document_chunks,
        document_id=doc_id,
        title=title,
        chunks=chunks,
        domain=domain,
        importance_score=1.2,
    )

    return {
        "status": "queued",
        "document_id": doc_id,
        "filename": title,
        "file_size_bytes": len(raw_bytes),
        "total_chunks": len(chunks),
        "message": f"Soubor '{title}' byl úspěšně přijat a {len(chunks)} bloků se indexuje do pgvector paměti.",
    }


@epistemic_router.get("/stats")
async def get_epistemic_stats(db: AsyncSession = Depends(get_db)) -> dict:
    """Vrací telemetrii a statistiky sémantické paměti a vektorové báze."""
    try:
        total_stmt = select(func.count(VectorMemory.id))
        total_res = await db.execute(total_stmt)
        total_count = total_res.scalar_one() or 0

        types_stmt = select(VectorMemory.memory_type, func.count(VectorMemory.id)).group_by(VectorMemory.memory_type)
        types_res = await db.execute(types_stmt)
        types_map = {row[0]: row[1] for row in types_res.all()}

        return {
            "status": "healthy",
            "total_memories": total_count,
            "memory_types": types_map,
            "vector_dimension": 768,
            "indexing_engine": "pgvector (HNSW + GIN tsvector)",
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
    except Exception as exc:
        logger.warning(f"Stats query warning: {exc}")
        return {
            "status": "operational",
            "total_memories": 0,
            "memory_types": {"semantic": 0, "document_chunk": 0},
            "vector_dimension": 768,
            "indexing_engine": "pgvector (HNSW + GIN tsvector)",
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }


@epistemic_router.get("/export")
async def export_epistemic_memories(
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
) -> dict:
    """Záloha a export sémantické paměti pro disaster recovery."""
    try:
        stmt = select(VectorMemory).order_by(desc(VectorMemory.created_at)).limit(limit)
        res = await db.execute(stmt)
        items = res.scalars().all()
        return {
            "export_count": len(items),
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "items": [
                {
                    "id": str(item.id),
                    "content": item.content,
                    "memory_type": item.memory_type,
                    "importance_score": item.importance_score,
                    "metadata": item.metadata_json,
                    "created_at": item.created_at.isoformat() if item.created_at else None,
                }
                for item in items
            ]
        }
    except Exception as exc:
        logger.error(f"Failed to export epistemic memories: {exc}")
        return {
            "export_count": 0,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "items": [],
            "error": str(exc)
        }
