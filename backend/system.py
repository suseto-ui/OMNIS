from fastapi import APIRouter

system_router = APIRouter(tags=["System"])

@system_router.get("/healthz")
@system_router.get("/api/health-check")
async def health_check() -> dict[str, str]:
    """Health check endpoint for Cloud Run and orchestrator probes."""
    return {"status": "healthy", "service": "O.M.N.I.S. Cognitive Architecture"}