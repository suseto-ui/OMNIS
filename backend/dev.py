from fastapi import APIRouter
from backend.token_service import token_telemetry_service

dev_router = APIRouter(prefix="/api/dev", tags=["Development & Diagnostics"])

@dev_router.get("/token-telemetry")
async def get_token_telemetry():
    """Vrací kumulativní statistiky spotřeby tokenů pro aktuální instanci."""
    return token_telemetry_service.get_telemetry()

@dev_router.post("/estimate-tokens")
async def estimate_query_tokens(payload: dict):
    """Vypočítá předpokládanou spotřebu tokenů pro zadaný dotaz."""
    query = payload.get("query", "")
    include_sys = payload.get("include_system_prompt", True)
    include_mem = payload.get("include_memory_context", True)
    return token_telemetry_service.estimate_query_tokens(
        query=query,
        include_system_prompt=include_sys,
        include_memory_context=include_mem,
    )

@dev_router.post("/reset-tokens")
async def reset_token_telemetry():
    """Resetuje relaci počítadla tokenů."""
    token_telemetry_service.reset_telemetry()
    return {"status": "reset_successful", "message": "Počítadlo tokenů bylo vynulováno."}