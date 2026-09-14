import logging
from typing import AsyncGenerator, Iterable, Optional

logger = logging.getLogger("omnis.db")


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


async_session_factory = _NullSessionFactory()


async def init_db():
    logger.info("Local environment detected: Using in-memory mock storage (No SQL connection).")


async def get_db() -> AsyncGenerator[_MockSession, None]:
    """Provide a lightweight mock DB dependency for local and test environments."""
    yield _MockSession()