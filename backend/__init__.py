from fastapi import APIRouter

# Import reálných routerů, které definujeme níže
from .engine_router import router as engine_router
from .dev_router import router as dev_router

# Staby pro ostatní routery zmíněné v main.py, aby systém šel spustit
system_router = APIRouter(prefix="/api/system", tags=["System"])
cognitive_router = APIRouter(prefix="/api/cognitive", tags=["Cognitive"])
autopoiesis_router = APIRouter(prefix="/api/autopoiesis", tags=["Autopoiesis"])
memory_router = APIRouter(prefix="/api/memory", tags=["Memory"])
epistemic_router = APIRouter(prefix="/api/epistemic", tags=["Epistemic"])
phase4_router = APIRouter(prefix="/api/phase4", tags=["Phase 4"])

@system_router.get("/health")
async def health_check():
    return {"status": "online", "engine": "O.M.N.I.S. Local Mock"}