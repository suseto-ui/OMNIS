from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from backend.database import get_db

system_router = APIRouter(tags=["System"])

@system_router.get("/healthz")
@system_router.get("/api/health-check")
async def health_check() -> dict[str, str]:
    """Health check endpoint for Cloud Run and orchestrator probes."""
    return {"status": "healthy", "service": "O.M.N.I.S. Cognitive Architecture"}

@system_router.get("/api/system/db-check")
async def db_check(db: AsyncSession = Depends(get_db)) -> dict[str, str]:
    """Checks the live connectivity status of the Google Cloud SQL instance."""
    try:
        # If the session is a mock session, it won't connect to a real DB
        from backend.database import engine
        if engine is None:
            return {"status": "offline", "reason": "No database URL configured"}
            
        await db.execute(text("SELECT 1"))
        return {"status": "online"}
    except Exception as e:
        return {"status": "offline", "reason": str(e)}
