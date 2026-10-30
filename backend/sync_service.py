import logging
from typing import List, Tuple
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from backend.models import OmnisSyncMessage, OmnisSyncMemory, OmnisSyncTelemetry
from backend.schemas import SyncMessageItem, SyncMemoryItem, SyncTelemetryItem

logger = logging.getLogger("omnis.sync_service")

class SyncService:

    @staticmethod
    async def upsert_messages(
        session: AsyncSession,
        items: List[SyncMessageItem]
    ) -> Tuple[int, str]:
        """
        Dávkově vloží nebo aktualizuje synchronizované zprávy z mobilního klienta.
        """
        if not items:
            return 0, "Prázdný seznam zpráv."

        count = 0
        try:
            for item in items:
                # Použití ORM modelu
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
                session.add(msg)
                count += 1

            await session.commit()
            return count, f"Úspěšně synchronizováno {count} zpráv do PostgreSQL."
        except Exception as e:
            logger.error(f"Chyba při synchronizaci zpráv: {e}", exc_info=True)
            await session.rollback()
            raise e

    @staticmethod
    async def upsert_memory_fragments(
        session: AsyncSession,
        items: List[SyncMemoryItem]
    ) -> Tuple[int, str]:
        """
        Dávkově vloží nebo aktualizuje paměťové fragmenty ze Znalostního Nexusu.
        """
        if not items:
            return 0, "Prázdný seznam paměťových fragmentů."

        count = 0
        try:
            for item in items:
                mem = OmnisSyncMemory(
                    id=item.id,
                    content=item.content,
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
                session.add(mem)
                count += 1

            await session.commit()
            return count, f"Úspěšně synchronizováno {count} paměťových fragmentů."
        except Exception as e:
            logger.error(f"Chyba při synchronizaci paměti: {e}", exc_info=True)
            await session.rollback()
            raise e

    @staticmethod
    async def insert_telemetry_logs(
        session: AsyncSession,
        items: List[SyncTelemetryItem]
    ) -> Tuple[int, str]:
        """
        Dávkově uloží logy telemetrie a 8D diagnostiky.
        """
        if not items:
            return 0, "Prázdný seznam telemetrie."

        count = 0
        try:
            for item in items:
                tel = OmnisSyncTelemetry(
                    level=item.level,
                    tag=item.tag,
                    message=item.message,
                    metadata_json=item.metadata_json or "",
                    timestamp=item.timestamp
                )
                session.add(tel)
                count += 1

            await session.commit()
            return count, f"Úspěšně uloženo {count} záznamů telemetrie."
        except Exception as e:
            logger.error(f"Chyba při ukládání telemetrie: {e}", exc_info=True)
            await session.rollback()
            raise e
