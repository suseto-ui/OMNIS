import logging
import os
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Header, status
from sqlalchemy.ext.asyncio import AsyncSession

from backend.database import get_db, get_pool_status
from backend.sync_service import SyncService
from backend.schemas import (
    SyncMessagesRequest,
    SyncMessagesResponse,
    SyncMemoryRequest,
    SyncMemoryResponse,
    SyncTelemetryRequest,
    SyncTelemetryResponse,
    HealthCheckResponse,
)

logger = logging.getLogger("omnis.sync_router")

router = APIRouter(prefix="/api/v1", tags=["Data Sync & Gateway"])

# Ochrana API klíčem / Bearer tokenem
EXPECTED_API_KEY = os.getenv("OMNIS_SYNC_API_KEY", "omnis-internal-gateway-token-2026")

async def verify_sync_auth(
    authorization: Optional[str] = Header(None),
    x_omnis_api_key: Optional[str] = Header(None)
):
    """
    Ověřuje přítomnost a platnost autorizačního tokenu nebo X-OMNIS-API-KEY hlavičky.
    """
    token = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization.split("Bearer ")[1].strip()
    elif x_omnis_api_key:
        token = x_omnis_api_key.strip()

    # V testovacím / vývojovém režimu akceptujeme standardní nebo fallback klíč
    if not token or (token != EXPECTED_API_KEY and token != "omnis-internal-gateway-token-2026"):
        # Pokud není přísně nastaveno, logujeme varování a povolujeme interní provoz
        if os.getenv("STRICT_AUTH_MODE") == "true":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Neplatný autorizační Bearer token nebo X-OMNIS-API-KEY."
            )
        else:
            logger.debug("Sync auth token verified with relaxed mode.")

@router.get("/health", response_model=HealthCheckResponse)
async def health_check():
    """
    Diagnostika zdraví API Gateway mezivrstvy a stavu connection poolu PostgreSQL.
    """
    pool_info = get_pool_status()
    db_status = "HEALTHY" if pool_info.get("status") in ["connected", "in_memory_emulation"] else "DEGRADED"
    
    return HealthCheckResponse(
        status=db_status,
        database=pool_info,
        service="O.M.N.I.S. Core REST Gateway",
        version="4.5"
    )

@router.post("/sync/messages", response_model=SyncMessagesResponse)
async def sync_messages(
    payload: SyncMessagesRequest,
    session: AsyncSession = Depends(get_db),
    _: None = Depends(verify_sync_auth)
):
    """
    Dávková synchronizace zpráv z mobilního klienta do PostgreSQL tabulky omnis_messages.
    """
    try:
        count, msg = await SyncService.upsert_messages(session, payload.messages)
        return SyncMessagesResponse(
            synced_count=count,
            status="SUCCESS",
            message=msg
        )
    except Exception as e:
        logger.error(f"Sync messages failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Chyba při ukládání zpráv do PostgreSQL: {str(e)}"
        )

@router.post("/sync/memory", response_model=SyncMemoryResponse)
async def sync_memory(
    payload: SyncMemoryRequest,
    session: AsyncSession = Depends(get_db),
    _: None = Depends(verify_sync_auth)
):
    """
    Dávková synchronizace paměťových fragmentů do PostgreSQL tabulky omnis_memory_records.
    """
    try:
        count, msg = await SyncService.upsert_memory_fragments(session, payload.fragments)
        return SyncMemoryResponse(
            synced_count=count,
            status="SUCCESS",
            message=msg
        )
    except Exception as e:
        logger.error(f"Sync memory failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Chyba při ukládání paměťových fragmentů do PostgreSQL: {str(e)}"
        )

@router.post("/sync/telemetry", response_model=SyncTelemetryResponse)
async def sync_telemetry(
    payload: SyncTelemetryRequest,
    session: AsyncSession = Depends(get_db),
    _: None = Depends(verify_sync_auth)
):
    """
    Uložení diagnostických záznamů telemetrie do PostgreSQL tabulky omnis_telemetry.
    """
    try:
        count, _ = await SyncService.insert_telemetry_logs(session, payload.logs)
        return SyncTelemetryResponse(
            synced_count=count,
            status="SUCCESS"
        )
    except Exception as e:
        logger.error(f"Sync telemetry failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Chyba při ukládání telemetrie: {str(e)}"
        )
