import logging
import os
from typing import AsyncGenerator, Iterable, Optional
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from backend.models import Base
from sqlalchemy import text

logger = logging.getLogger("omnis.db")

DATABASE_URL = os.getenv("DATABASE_URL")

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
    logger.info("Database URL detected. Preparing Google Cloud SQL connection...")
    try:
        url = DATABASE_URL
        if url.startswith("postgresql://"):
            url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
        
        engine = create_async_engine(
            url,
            echo=False,
            pool_pre_ping=True,
            pool_recycle=1800,
        )
        async_session_factory = async_sessionmaker(
            bind=engine,
            class_=AsyncSession,
            expire_on_commit=False,
        )
        logger.info("Real database engine configured successfully.")
    except Exception as e:
        logger.error(f"Failed to create async database engine: {e}. Falling back to mock session.")
        async_session_factory = _NullSessionFactory()
else:
    logger.info("No DATABASE_URL environment variable found. Falling back to in-memory mock session.")
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
                    logger.warning(f"Could not create vector extension (might be already installed or insufficient privileges): {ext_err}")
                
                await conn.run_sync(Base.metadata.create_all)
            logger.info("Database schema initialized successfully on Google Cloud SQL.")
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
