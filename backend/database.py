"""
O.M.N.I.S. Database Layer
SQLAlchemy 2.0 Async engine and session configuration with pgvector support.
"""

import os
from collections.abc import AsyncGenerator
from sqlalchemy import text
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql+asyncpg://omnis_user:omnis_secure_pass@localhost:5432/omnis_db",
)

# Async engine with connection pooling and pre-ping
engine = create_async_engine(
    DATABASE_URL,
    echo=False,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    pool_recycle=3600,
)

async_session_factory = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


class Base(DeclarativeBase):
    """Base declarative class for all O.M.N.I.S. models."""
    pass


async def init_db() -> None:
    """
    Initialize database extension and metadata schema.
    Installs the 'vector' extension for semantic vector embeddings
    and creates all declared tables.
    """
    async with engine.begin() as conn:
        # Enable pgvector extension
        await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        # Create all tables declared in models
        await conn.run_sync(Base.metadata.create_all)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency for yielding database session with automatic lifecycle."""
    async with async_session_factory() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
