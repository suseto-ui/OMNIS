"""
O.M.N.I.S. Core FastAPI Backend Application
Handles queries, cognitive processes, Impact Matrix evaluations, and autopoietic memory loops.
"""

from __future__ import annotations

import logging
import os
import uuid
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import AsyncGenerator, List

from fastapi import FastAPI, APIRouter, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.routing import _IncludedRouter
from fastapi.staticfiles import StaticFiles
from sqlalchemy import select, delete as sql_delete
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from .database import get_db, init_db
from backend.models import Conversation, Message
from backend.schemas import (
    ThreadCreateRequest,
    ThreadUpdateRequest,
    ThreadDetailResponse,
    MessageSchema,
)
from backend import (
    autopoiesis_router,
    cognitive_router,
    dev_router,
    engine_router,
    epistemic_router,
    memory_router,
    multi_agent_router,
    phase4_router,
    system_router,
    sync_router,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("omnis.api")

# ==========================================================
# Thread Persistence Router (Database Persistence Layer)
# ==========================================================
thread_router = APIRouter(prefix="/api/threads", tags=["Thread Persistence"])


@thread_router.get("", response_model=List[ThreadDetailResponse])
@thread_router.get("/", response_model=List[ThreadDetailResponse])
async def list_threads(db: AsyncSession = Depends(get_db)):
    """Seznam všech konverzačních vláken pro persistenci a databázovou migraci."""
    stmt = (
        select(Conversation)
        .options(selectinload(Conversation.messages))
        .order_by(Conversation.updated_at.desc())
    )
    result = await db.execute(stmt)
    conversations = result.scalars().all()

    response = []
    for conv in conversations:
        response.append(
            ThreadDetailResponse(
                id=str(conv.id),
                title=conv.title or "Bez názvu",
                ontology_domain=conv.ontology_domain or "SYSTEMS_INTELLIGENCE",
                messages=[
                    MessageSchema(
                        id=str(m.id),
                        conversation_id=str(m.conversation_id),
                        role=m.role,
                        content=m.content,
                        cognitive_thoughts=m.cognitive_thoughts,
                        follow_up_questions=m.follow_up_questions,
                        created_at=m.created_at
                    ) for m in (conv.messages or [])
                ],
                created_at=conv.created_at,
                updated_at=conv.updated_at
            )
        )
    return response


@thread_router.post("", response_model=ThreadDetailResponse, status_code=status.HTTP_201_CREATED)
@thread_router.post("/", response_model=ThreadDetailResponse, status_code=status.HTTP_201_CREATED)
async def create_thread(req: ThreadCreateRequest, db: AsyncSession = Depends(get_db)):
    """Vytvořit nové konverzační vlákno."""
    conv_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc)
    conv = Conversation(
        id=conv_id,
        title=req.title,
        ontology_domain=req.ontology_domain,
        created_at=now,
        updated_at=now
    )
    db.add(conv)
    await db.commit()
    return ThreadDetailResponse(
        id=conv_id,
        title=req.title,
        ontology_domain=req.ontology_domain,
        messages=[],
        created_at=now,
        updated_at=now
    )


@thread_router.get("/{thread_id}", response_model=ThreadDetailResponse)
async def get_thread(thread_id: str, db: AsyncSession = Depends(get_db)):
    """Detail konkrétního vlákna včetně historie zpráv."""
    stmt = (
        select(Conversation)
        .options(selectinload(Conversation.messages))
        .where(Conversation.id == thread_id)
    )
    result = await db.execute(stmt)
    conv = result.scalar_one_or_none()
    if not conv:
        raise HTTPException(status_code=404, detail=f"Vlákno {thread_id} nebylo nalezeno.")

    return ThreadDetailResponse(
        id=str(conv.id),
        title=conv.title or "Bez názvu",
        ontology_domain=conv.ontology_domain or "SYSTEMS_INTELLIGENCE",
        messages=[
            MessageSchema(
                id=str(m.id),
                conversation_id=str(m.conversation_id),
                role=m.role,
                content=m.content,
                cognitive_thoughts=m.cognitive_thoughts,
                follow_up_questions=m.follow_up_questions,
                created_at=m.created_at
            ) for m in (conv.messages or [])
        ],
        created_at=conv.created_at,
        updated_at=conv.updated_at
    )


@thread_router.put("/{thread_id}", response_model=ThreadDetailResponse)
async def update_thread(thread_id: str, req: ThreadUpdateRequest, db: AsyncSession = Depends(get_db)):
    """Aktualizace námitky/názvu nebo domény vlákna."""
    stmt = (
        select(Conversation)
        .options(selectinload(Conversation.messages))
        .where(Conversation.id == thread_id)
    )
    result = await db.execute(stmt)
    conv = result.scalar_one_or_none()
    if not conv:
        raise HTTPException(status_code=404, detail=f"Vlákno {thread_id} nebylo nalezeno.")

    if req.title is not None:
        conv.title = req.title
    if req.ontology_domain is not None:
        conv.ontology_domain = req.ontology_domain
    conv.updated_at = datetime.now(timezone.utc)

    db.add(conv)
    await db.commit()

    return ThreadDetailResponse(
        id=str(conv.id),
        title=conv.title or "Bez názvu",
        ontology_domain=conv.ontology_domain or "SYSTEMS_INTELLIGENCE",
        messages=[
            MessageSchema(
                id=str(m.id),
                conversation_id=str(m.conversation_id),
                role=m.role,
                content=m.content,
                cognitive_thoughts=m.cognitive_thoughts,
                follow_up_questions=m.follow_up_questions,
                created_at=m.created_at
            ) for m in (conv.messages or [])
        ],
        created_at=conv.created_at,
        updated_at=conv.updated_at
    )


@thread_router.delete("/{thread_id}")
async def delete_thread(thread_id: str, db: AsyncSession = Depends(get_db)):
    """Smazání vlákna a všech přidružených zpráv."""
    stmt = select(Conversation).where(Conversation.id == thread_id)
    result = await db.execute(stmt)
    conv = result.scalar_one_or_none()
    if not conv:
        raise HTTPException(status_code=404, detail=f"Vlákno {thread_id} nebylo nalezeno.")

    await db.execute(sql_delete(Message).where(Message.conversation_id == thread_id))
    await db.delete(conv)
    await db.commit()

    return {"status": "success", "message": f"Vlákno {thread_id} bylo úspěšně smazáno.", "thread_id": thread_id}


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Startup and shutdown lifecycle handler."""
    logger.info("Initializing O.M.N.I.S. database and pgvector extensions...")
    try:
        await init_db()
        logger.info("Database schema initialized successfully.")
    except Exception:
        logger.exception("Database initialization warning (will retry on queries)")
    yield
    logger.info("Shutting down O.M.N.I.S. backend.")


app = FastAPI(
    title="O.M.N.I.S. Cognitive Architecture API",
    description="Omni-Modal Network for Integrated Synthesis - Backend & Impact Matrix Engine",
    version="2.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:5173,http://localhost:8000").split(","),
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)


@app.exception_handler(SQLAlchemyError)
async def sqlalchemy_exception_handler(request, exc: SQLAlchemyError):
    """Intercepts and gracefully sanitizes database-level exceptions globally."""
    logger.exception("CRITICAL: Database exception intercepted across router boundaries: %s", exc)
    return JSONResponse(
        status_code=500,
        content={
            "status": "error",
            "type": "DatabaseOperationalFailure",
            "message": "A critical storage plane or relational integrity constraint error occurred within O.M.N.I.S.",
            "detail": str(exc),
        },
    )


app.include_router(system_router)
app.include_router(cognitive_router)
app.include_router(engine_router)
app.include_router(autopoiesis_router)
app.include_router(memory_router)
app.include_router(epistemic_router)
app.include_router(multi_agent_router)
app.include_router(phase4_router)
app.include_router(dev_router)
app.include_router(thread_router)
app.include_router(sync_router)

# Expand included routers into concrete routes so route inspection and URL matching work reliably.
flattened_routes = []
for route in app.router.routes:
    if isinstance(route, _IncludedRouter):
        flattened_routes.extend(getattr(route.original_router, "routes", []))
    else:
        flattened_routes.append(route)
app.router.routes[:] = flattened_routes

# ==========================================================
# Static Frontend Serving (Cloud Run & Web Production)
# Always register SPA fallback route; check filesystem at request time.
# ==========================================================

frontend_dist = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "dist"))
assets_dir = os.path.join(frontend_dist, "assets")

if os.path.exists(assets_dir):
    try:
        app.mount("/assets", StaticFiles(directory=assets_dir), name="assets")
    except Exception:
        logger.exception("Failed to mount /assets directory")


@app.get("/{full_path:path}", include_in_schema=False)
async def serve_spa(full_path: str):
    file_path = os.path.join(frontend_dist, full_path)
    index_path = os.path.join(frontend_dist, "index.html")
    try:
        if os.path.isfile(file_path):
            return FileResponse(file_path)
        if os.path.isdir(frontend_dist) and os.path.isfile(index_path):
            return FileResponse(index_path)
    except Exception:
        logger.exception("Error while serving SPA file: %s", full_path)
    return JSONResponse(status_code=404, content={"error": "SPA not built or file not found."})
