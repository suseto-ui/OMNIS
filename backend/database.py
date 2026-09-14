import logging

logger = logging.getLogger("omnis.db")

async def init_db():
    logger.info("Local environment detected: Using in-memory mock storage (No SQL connection).")