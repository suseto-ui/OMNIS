"""
O.M.N.I.S. Core FastAPI Backend Application
Handles queries, cognitive processes, Impact Matrix evaluations, and autopoietic memory loops.
"""

from __future__ import annotations
import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.exc import SQLAlchemyError

from .database import init_db
from backend import (
    system_router,
    cognitive_router,
    engine_router,
    autopoiesis_router,
    memory_router,
    epistemic_router,
    phase4_router,
    dev_router,
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
import os
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
    logger.error(f"CRITICAL: Database exception intercepted across router boundaries: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "status": "error",
            "type": "DatabaseOperationalFailure",
            "message": "A critical storage plane or relational integrity constraint error occurred within O.M.N.I.S.",
            "detail": str(exc)
        }
    )

app.include_router(system_router)
app.include_router(cognitive_router)
app.include_router(engine_router)
app.include_router(autopoiesis_router)
app.include_router(memory_router)
app.include_router(epistemic_router)
app.include_router(phase4_router)
app.include_router(dev_router)

# ==========================================================
# Static Frontend Serving (Cloud Run & Web Production)
# ==========================================================

import os
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

frontend_dist = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "dist"))

# [REFAKTORIZACE - SLABÉ MÍSTO]: Pokud frontend nebyl vygenerován v momentě startu backendu
# (což se stává v Docker CI/CD pipelines), tento if-block způsobil, že backend nikdy 
# nezačal servírovat frontend, ani když se složka dist objevila později.
# Odstraněn load-time check z hlavní route - nyní zachytáváme vše a ověřujeme existenci 
# souboru až v době requestu.

assets_dir = os.path.join(frontend_dist, "assets")
try:
    if os.path.exists(assets_dir):
        app.mount("/assets", StaticFiles(directory=assets_dir), name="assets")
except Exception:
    pass

if os.path.exists(frontend_dist):
    @app.get("/{full_path:path}", include_in_schema=False)
    async def serve_spa(full_path: str):
        # API routes are already handled by priority, only fall back for non-existing files
        file_path = os.path.join(frontend_dist, full_path)
        if os.path.isfile(file_path):
            return FileResponse(file_path)
        return FileResponse(os.path.join(frontend_dist, "index.html"))
else:
    logger.warning("Frontend distribution directory not found. SPA serving disabled.")
