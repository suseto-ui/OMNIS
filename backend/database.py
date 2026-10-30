import logging
import os
import urllib.parse
from typing import AsyncGenerator, Iterable, Optional, Dict, Any
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from backend.models import Base
from sqlalchemy import text

logger = logging.getLogger("omnis.db")

raw_db_url = os.getenv("DATABASE_URL", "").strip()
DATABASE_URL = None

if raw_db_url and (
    raw_db_url.startswith("postgresql://")
    or raw_db_url.startswith("postgresql+asyncpg://")
    or raw_db_url.startswith("postgres://")
    or raw_db_url.startswith("sqlite")
):
    DATABASE_URL = raw_db_url
else:
    if raw_db_url:
        logger.warning(
            "Zadaná proměnná DATABASE_URL neobsahuje platné PostgreSQL/SQLite schéma. Zkouším CLOUDSQL_* proměnné."
        )
    cloudsql_host = os.getenv("CLOUDSQL_HOST")
    cloudsql_user = os.getenv("CLOUDSQL_USER")
    cloudsql_pwd = os.getenv("CLOUDSQL_PASSWORD")
    cloudsql_db = os.getenv("CLOUDSQL_DB")

    if cloudsql_host and cloudsql_user and cloudsql_pwd and cloudsql_db:
        encoded_user = urllib.parse.quote_plus(cloudsql_user)
        encoded_pwd = urllib.parse.quote_plus(cloudsql_pwd)
        DATABASE_URL = f"postgresql+asyncpg://{encoded_user}:{encoded_pwd}@{cloudsql_host}:5432/{cloudsql_db}"
        logger.info(
            f"DATABASE_URL automaticky sestavena z Cloud SQL proměnných ({cloudsql_host}/{cloudsql_db})"
        )


class _QueryResult:
    def __init__(self, rows: Optional[Iterable] = None):
        self._rows = list(rows or [])

    def scalar_one_or_none(self):
        return self._rows[0] if self._rows else None

    def all(self):
        return list(self._rows)

    def scalars(self):
        return self

    def __iter__(self):
        return iter(self._rows)


class _MockSession:
    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb):
        return False

    def add(self, obj):
        return None

    def add_all(self, objs):
        return None

    async def commit(self):
        return None

    async def rollback(self):
        return None

    async def flush(self):
        return None

    async def get(self, model, ident):
        return None

    async def execute(self, *args, **kwargs):
        return _QueryResult()

    def __getattr__(self, name):
        return None


class _NullSessionFactory:
    def __call__(self):
        return _MockSession()


engine = None
async_session_factory = None

if DATABASE_URL:
    logger.info("Database URL detected. Preparing Google Cloud SQL connection pool...")
    try:
        url = DATABASE_URL
        if url.startswith("postgresql://"):
            url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
        elif url.startswith("postgres://"):
            url = url.replace("postgres://", "postgresql+asyncpg://", 1)

        engine_kwargs = {
            "echo": False,
            "pool_pre_ping": True,
            "pool_recycle": 300,  # 5 min recycle for Cloud SQL TCP stability
        }

        if "sqlite" not in url:
            engine_kwargs["pool_size"] = 20
            engine_kwargs["max_overflow"] = 10
            engine_kwargs["pool_timeout"] = 30

        engine = create_async_engine(url, **engine_kwargs)

        async_session_factory = async_sessionmaker(
            bind=engine,
            class_=AsyncSession,
            expire_on_commit=False,
        )
        logger.info("PostgreSQL AsyncEngine configured with asyncpg connection pool.")
    except Exception as e:
        logger.error(
            f"Failed to create async database engine: {e}. Falling back to mock session."
        )
        async_session_factory = _NullSessionFactory()
else:
    logger.info(
        "No DATABASE_URL environment variable found. Falling back to in-memory mock session."
    )
    async_session_factory = _NullSessionFactory()


async def init_db():
    if engine is not None:
        logger.info("Initializing Google Cloud SQL database schema...")
        try:
            async with engine.begin() as conn:
                try:
                    await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector;"))
                    logger.info("pgvector extension installed/verified.")
                except Exception as ext_err:
                    logger.warning(
                        f"Could not create vector extension (might be already installed or insufficient privileges): {ext_err}"
                    )

                await conn.run_sync(Base.metadata.create_all)
                logger.info("Database schema tables created/verified.")
        except Exception as err:
            logger.error(f"Database schema initialization failed: {err}")
            raise err
    else:
        logger.info("Mock database environment: init_db is a no-op.")


async def get_db() -> AsyncGenerator:
    """Provide either a real SQLAlchemy AsyncSession or a mock fallback session."""
    if engine is not None and async_session_factory is not None:
        async with async_session_factory() as session:
            try:
                yield session
            except Exception as e:
                logger.error(f"Error during database session operation: {e}")
                await session.rollback()
                raise
            finally:
                await session.close()
    else:
        yield _MockSession()


def get_pool_status() -> Dict[str, Any]:
    """Vrací diagnostiku stavu connection poolu pro /api/v1/health."""
    if engine is None:
        return {
            "driver": "mock",
            "status": "in_memory_emulation",
            "pool_size": 0,
            "checked_in": 0,
            "checked_out": 0,
            "overflow": 0,
        }

    try:
        pool = engine.pool
        return {
            "driver": "postgresql+asyncpg",
            "status": "connected",
            "pool_size": getattr(pool, "size", lambda: 20)(),
            "checked_in": getattr(pool, "checkedin", lambda: 0)(),
            "checked_out": getattr(pool, "checkedout", lambda: 0)(),
            "overflow": getattr(pool, "overflow", lambda: 0)(),
        }
    except Exception as e:
        return {
            "driver": "postgresql+asyncpg",
            "status": "error",
            "error": str(e),
        }
