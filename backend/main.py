"""
O.M.N.I.S. Core FastAPI Backend Application
Handles queries, cognitive processes, Impact Matrix evaluations, and autopoietic memory loops.
"""

from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.routing import _IncludedRouter
from fastapi.staticfiles import StaticFiles
from sqlalchemy.exc import SQLAlchemyError

from .database import get_db, init_db
from backend import (
    autopoiesis_router,
    cognitive_router,
    dev_router,
    engine_router,
    epistemic_router,
    memory_router,
    phase4_router,
    system_router,
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
app.include_router(phase4_router)
app.include_router(dev_router)

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
