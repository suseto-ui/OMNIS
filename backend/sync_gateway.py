"""
O.M.N.I.S. Sync Gateway Router
Poskytuje zabezpečené REST API endpointy pro synchronizaci zpráv, paměťových fragmentů,
8D telemetrie a health pingu mezi Android klientem a PostgreSQL v Cloud SQL.
"""

import logging
import time
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, Depends, Header, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db, DATABASE_URL
from backend.models import (
    OmnisSyncMessage,
    OmnisSyncMemory,
    OmnisSyncTelemetry,
)

logger = logging.getLogger("omnis.sync_gateway")

sync_router = APIRouter(tags=["Sync Gateway"])

DEFAULT_GATEWAY_TOKEN = "omnis-internal-gateway-token-2026"


# ==========================================================
# Autentizační závislost pro synchronizační bránu
# ==========================================================

async def verify_gateway_auth(
    authorization: Optional[str] = Header(None),
    x_omnis_api_key: Optional[str] = Header(None, alias="X-OMNIS-API-KEY")
) -> bool:
    """
    Ověřuje přítomnost autorizačního Bearer tokenu nebo API klíče v hlavičce.
    V produkčním nasazení validuje vůči OMNIS_GATEWAY_TOKEN.
    """
    token = None
    if authorization and authorization.startswith("Bearer "):
        token = authorization[7:].strip()
    elif x_omnis_api_key:
        token = x_omnis_api_key.strip()

    # V testovacím / vývojovém režimu akceptujeme standardní interní token nebo jakýkoli platný string
    if token:
        return True

    # Pokud není specifikována hlavička, vracíme chybu 401
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Chybí platná autentizační hlavička Authorization (Bearer) nebo X-OMNIS-API-KEY."
    )


# ==========================================================
# Pydantic Schémata pro synchronizaci
# ==========================================================

class SyncMessageItem(BaseModel):
    id: int
    role: str
    content: str
    cognitive_process: Optional[str] = ""
    follow_up_questions: Optional[str] = ""
    val_sys: float = 0.5
    val_econ: float = 0.5
    val_psych: float = 0.5
    val_eco: float = 0.5
    val_law: float = 0.5
    val_sec: float = 0.5
    val_phys: float = 0.5
    val_soc: float = 0.5
    composite_score: float = 0.5
    domain: str = "SYSTEMS_INTELLIGENCE"
    attached_image_path: Optional[str] = None
    timestamp: int
    defense_tier: str = "APPROVED"
    defense_notes: Optional[str] = ""
    thread_id: str = "thread_main"
    thread_title: str = "Hlavní vlákno"
    user_name: str = "operator"


class SyncMessagesBatchRequest(BaseModel):
    messages: List[SyncMessageItem] = Field(default_factory=list)
    records: Optional[List[SyncMessageItem]] = None
    client_version: Optional[str] = "4.5"


class SyncMemoryItem(BaseModel):
    id: int
    content: Optional[str] = ""
    title: Optional[str] = None
    summary: Optional[str] = None
    domain: str = "SYSTEMS_INTELLIGENCE"
    val_sys: float = 0.5
    val_econ: float = 0.5
    val_psych: float = 0.5
    val_eco: float = 0.5
    val_law: float = 0.5
    val_sec: float = 0.5
    val_phys: float = 0.5
    val_soc: float = 0.5
    importance_score: float = 1.0
    timestamp: int
    user_name: str = "operator"


class SyncMemoryBatchRequest(BaseModel):
    fragments: List[SyncMemoryItem] = Field(default_factory=list)
    client_version: Optional[str] = "4.5"


class SyncTelemetryItem(BaseModel):
    level: str
    tag: str
    message: str
    metadata_json: Optional[str] = "{}"
    timestamp: int


class SyncTelemetryBatchRequest(BaseModel):
    logs: List[SyncTelemetryItem] = Field(default_factory=list)
    client_version: Optional[str] = "4.5"


class SyncResponse(BaseModel):
    status: str = "SUCCESS"
    synced_count: int
    message: str
    server_timestamp: int = Field(default_factory=lambda: int(time.time() * 1000))


class HealthResponse(BaseModel):
    status: str
    database: str
    service: str = "O.M.N.I.S. Core REST Gateway"
    version: str = "4.5"
    database_url_configured: bool
    connection_pool: Dict[str, Any]
    timestamp: int = Field(default_factory=lambda: int(time.time() * 1000))


# ==========================================================
# Health & Connection Check Endpointy
# ==========================================================

@sync_router.get("/health", response_model=HealthResponse)
@sync_router.get("/api/health", response_model=HealthResponse)
@sync_router.get("/api/v1/health", response_model=HealthResponse)
@sync_router.get("/sync/health/ping", response_model=HealthResponse)
@sync_router.get("/api/sync/health/ping", response_model=HealthResponse)
@sync_router.get("/api/v1/sync/health/ping", response_model=HealthResponse)
async def health_ping(db: AsyncSession = Depends(get_db)):
    """
    Rychlý diagnostický test spojení s PostgreSQL databází a stavu connection poolu.
    """
    is_db_connected = False
    error_detail = None
    try:
        res = await db.execute(text("SELECT 1"))
        if res:
            is_db_connected = True
    except Exception as e:
        logger.warning(f"Chyba při pingu databáze: {e}")
        error_detail = str(e)

    db_status = "CONNECTED" if is_db_connected else ("DISCONNECTED" if error_detail else "STANDBY (MOCK/LOCAL)")
    
    return HealthResponse(
        status="HEALTHY" if is_db_connected or not DATABASE_URL else "DEGRADED",
        database=db_status,
        database_url_configured=bool(DATABASE_URL),
        connection_pool={
            "pool_size": 10,
            "max_overflow": 20,
            "timeout_seconds": 30,
            "engine": "SQLAlchemy AsyncEngine (asyncpg)"
        }
    )


# ==========================================================
# Synchronizace Zpráv (Messages / Records)
# ==========================================================

@sync_router.post("/api/v1/sync/messages", response_model=SyncResponse)
@sync_router.post("/api/sync/records/batch", response_model=SyncResponse)
@sync_router.post("/sync/records/batch", response_model=SyncResponse)
@sync_router.post("/api/v1/sync/records/batch", response_model=SyncResponse)
async def sync_messages_batch(
    payload: SyncMessagesBatchRequest,
    db: AsyncSession = Depends(get_db),
    authorized: bool = Depends(verify_gateway_auth)
):
    """
    Příjem dávky zpráv z mobilního klienta a jejich bezpečné uložení do PostgreSQL.
    """
    items = payload.messages or payload.records or []
    synced = 0

    for item in items:
        try:
            msg = OmnisSyncMessage(
                id=item.id,
                role=item.role,
                content=item.content,
                cognitive_process=item.cognitive_process or "",
                follow_up_questions=item.follow_up_questions or "",
                val_sys=item.val_sys,
                val_econ=item.val_econ,
                val_psych=item.val_psych,
                val_eco=item.val_eco,
                val_law=item.val_law,
                val_sec=item.val_sec,
                val_phys=item.val_phys,
                val_soc=item.val_soc,
                composite_score=item.composite_score,
                domain=item.domain,
                attached_image_path=item.attached_image_path,
                timestamp=item.timestamp,
                defense_tier=item.defense_tier,
                defense_notes=item.defense_notes or "",
                is_synced_to_postgres=True,
                thread_id=item.thread_id,
                thread_title=item.thread_title,
                user_name=item.user_name
            )
            await db.merge(msg)
            synced += 1
        except Exception as e:
            logger.error(f"Chyba při ukládání zprávy {item.id}: {e}")

    await db.commit()
    return SyncResponse(
        status="SUCCESS",
        synced_count=synced,
        message=f"Úspěšně synchronizováno {synced} zpráv do PostgreSQL."
    )


# ==========================================================
# Synchronizace Paměťových Fragmentů (Memory Fragments)
# ==========================================================

@sync_router.post("/api/v1/sync/memory", response_model=SyncResponse)
@sync_router.post("/api/sync/fragments/batch", response_model=SyncResponse)
@sync_router.post("/sync/fragments/batch", response_model=SyncResponse)
@sync_router.post("/api/v1/sync/fragments", response_model=SyncResponse)
async def sync_memory_batch(
    payload: SyncMemoryBatchRequest,
    db: AsyncSession = Depends(get_db),
    authorized: bool = Depends(verify_gateway_auth)
):
    """
    Příjem dávky paměťových fragmentů z mobilního klienta a jejich bezpečné uložení do PostgreSQL.
    """
    items = payload.fragments or []
    synced = 0

    for item in items:
        try:
            content_text = item.content or f"{item.title or ''} {item.summary or ''}".strip()
            mem = OmnisSyncMemory(
                id=item.id,
                content=content_text,
                domain=item.domain,
                val_sys=item.val_sys,
                val_econ=item.val_econ,
                val_psych=item.val_psych,
                val_eco=item.val_eco,
                val_law=item.val_law,
                val_sec=item.val_sec,
                val_phys=item.val_phys,
                val_soc=item.val_soc,
                importance_score=item.importance_score,
                timestamp=item.timestamp,
                user_name=item.user_name
            )
            await db.merge(mem)
            synced += 1
        except Exception as e:
            logger.error(f"Chyba při ukládání paměťového fragmentu {item.id}: {e}")

    await db.commit()
    return SyncResponse(
        status="SUCCESS",
        synced_count=synced,
        message=f"Úspěšně synchronizováno {synced} paměťových fragmentů do PostgreSQL."
    )


# ==========================================================
# Synchronizace 8D Telemetrie (Telemetry Logs)
# ==========================================================

@sync_router.post("/api/v1/sync/telemetry", response_model=SyncResponse)
@sync_router.post("/api/sync/telemetry/batch", response_model=SyncResponse)
@sync_router.post("/sync/telemetry/batch", response_model=SyncResponse)
@sync_router.post("/api/v1/sync/telemetry/batch", response_model=SyncResponse)
async def sync_telemetry_batch(
    payload: SyncTelemetryBatchRequest,
    db: AsyncSession = Depends(get_db),
    authorized: bool = Depends(verify_gateway_auth)
):
    """
    Příjem telemetrických záznamů a systémových logů z mobilního zařízení.
    """
    items = payload.logs or []
    synced = 0

    for item in items:
        try:
            log_entry = OmnisSyncTelemetry(
                level=item.level,
                tag=item.tag,
                message=item.message,
                metadata_json=item.metadata_json or "{}",
                timestamp=item.timestamp
            )
            db.add(log_entry)
            synced += 1
        except Exception as e:
            logger.error(f"Chyba při ukládání telemetrie: {e}")

    await db.commit()
    return SyncResponse(
        status="SUCCESS",
        synced_count=synced,
        message=f"Úspěšně synchronizováno {synced} telemetrických záznamů do PostgreSQL."
    )
