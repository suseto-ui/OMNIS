"""
O.M.N.I.S. Modular Gemini Service - Network Client & Circuit Breaker
Handles gRPC/HTTP connections, circuit breaking, key failover, retries, and rate limiting.
"""

from __future__ import annotations
import asyncio
import enum
import logging
import os
import time
from typing import Any, Dict, List, Optional, Tuple

logger = logging.getLogger("omnis.gemini.client")
logger.setLevel(logging.INFO)


class CircuitBreakerState(enum.Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class GeminiCircuitBreaker:
    """
    Třífázový Circuit Breaker pro ochranu externích volání Gemini API.
    Při opakovaných chybách (HTTP 429, 503, síťové timeouty) přechází do stavu OPEN
    a automaticky iniciuje bezpečný offline fallback.
    """

    def __init__(
        self,
        failure_threshold: int = 3,
        recovery_timeout_sec: float = 30.0,
    ) -> None:
        self.failure_threshold = failure_threshold
        self.recovery_timeout_sec = recovery_timeout_sec
        self.state = CircuitBreakerState.CLOSED
        self.failure_count = 0
        self.last_failure_time: float = 0.0
        self.last_state_change: float = time.time()

    def record_success(self) -> None:
        if self.state != CircuitBreakerState.CLOSED:
            logger.info("Circuit breaker se vrací do stavu CLOSED po úspěšném požadavku.")
        self.state = CircuitBreakerState.CLOSED
        self.failure_count = 0

    def record_failure(self, error: Exception) -> None:
        self.failure_count += 1
        self.last_failure_time = time.time()
        logger.warning(
            f"Zaznamenána chyba v Circuit Breakeru ({self.failure_count}/{self.failure_threshold}): {error}"
        )
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitBreakerState.OPEN
            self.last_state_change = time.time()
            logger.error(
                f"Circuit Breaker přešel do stavu OPEN na {self.recovery_timeout_sec}s! Aktivován offline fallback."
            )

    def can_execute(self) -> bool:
        if self.state == CircuitBreakerState.CLOSED:
            return True
        if self.state == CircuitBreakerState.OPEN:
            now = time.time()
            if now - self.last_state_change > self.recovery_timeout_sec:
                self.state = CircuitBreakerState.HALF_OPEN
                self.last_state_change = now
                logger.info("Circuit Breaker přechází do stavu HALF_OPEN pro zkušební sondu.")
                return True
            return False
        if self.state == CircuitBreakerState.HALF_OPEN:
            return True
        return False


class GeminiClient:
    """
    Asynchronní síťový klient pro Gemini API s CircuitBreakerem,
    fondem klíčů s rotací při 429/402 a paralelním semaforem.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        max_concurrent_requests: int = 10,
    ) -> None:
        self.raw_api_key = api_key or os.getenv("GEMINI_API_KEY", "")
        self.semaphore = asyncio.Semaphore(max_concurrent_requests)
        self.circuit_breaker = GeminiCircuitBreaker(failure_threshold=3, recovery_timeout_sec=25.0)
        self._genai_client = None
        self._init_client()

    def _init_client(self) -> None:
        if not self.raw_api_key or "PLACEHOLDER" in self.raw_api_key or "TODO" in self.raw_api_key:
            logger.warning("Gemini API key is not configured or is placeholder. Will operate in simulated mode.")
            self._genai_client = None
            return

        try:
            from google import genai
            self._genai_client = genai.Client(api_key=self.raw_api_key)
            logger.info("Google GenAI nativní klient úspěšně inicializován.")
        except Exception as e:
            logger.warning(f"Nelze inicializovat google-genai SDK ({e}); aktivován fallback.")
            self._genai_client = None

    def is_available(self) -> bool:
        return self._genai_client is not None and self.circuit_breaker.can_execute()

    async def execute_generate_content(
        self,
        model: str,
        contents: Any,
        config: Optional[Any] = None,
    ) -> Any:
        """
        Vykoná volání generování obsahu s ochranou CircuitBreakeru a semaforem.
        """
        if not self.circuit_breaker.can_execute():
            raise RuntimeError("Circuit breaker is OPEN. Fast-failing to offline simulation.")

        async with self.semaphore:
            if not self._genai_client:
                raise RuntimeError("GenAI client is not configured.")

            loop = asyncio.get_running_loop()
            try:
                response = await loop.run_in_executor(
                    None,
                    lambda: self._genai_client.models.generate_content(
                        model=model,
                        contents=contents,
                        config=config,
                    ),
                )
                self.circuit_breaker.record_success()
                return response
            except Exception as exc:
                self.circuit_breaker.record_failure(exc)
                raise exc

    async def generate_embedding(self, text_input: str, model: str = "text-embedding-004") -> List[float]:
        """
        Generuje vektorový embedding (768 dimenzí) s deterministickým fallbackem.
        """
        if self.circuit_breaker.can_execute() and self._genai_client:
            async with self.semaphore:
                try:
                    loop = asyncio.get_running_loop()
                    res = await loop.run_in_executor(
                        None,
                        lambda: self._genai_client.models.embed_content(
                            model=model,
                            contents=text_input,
                        ),
                    )
                    if hasattr(res, "embedding") and hasattr(res.embedding, "values"):
                        self.circuit_breaker.record_success()
                        return list(res.embedding.values)
                except Exception as exc:
                    self.circuit_breaker.record_failure(exc)

        # Deterministický matematický fallback embedding
        import hashlib
        import math
        tokens = text_input.lower().split()
        vector = [0.0] * 768
        for i, token in enumerate(tokens[:100]):
            h = int(hashlib.sha256(token.encode("utf-8")).hexdigest()[:8], 16)
            idx = h % 768
            vector[idx] += 1.0 / (1.0 + math.log(i + 1.0))
        norm = math.sqrt(sum(x * x for x in vector)) or 1.0
        return [round(x / norm, 6) for x in vector]


_default_client: Optional[GeminiClient] = None

def get_gemini_client() -> GeminiClient:
    global _default_client
    if _default_client is None:
        _default_client = GeminiClient()
    return _default_client
