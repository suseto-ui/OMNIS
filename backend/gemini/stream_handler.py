"""
O.M.N.I.S. Modular Gemini Service - Stream Handler
Manages Server-Sent Events (SSE), token buffering, latency tracking (TTFT), and stream multiplexing.
"""

from __future__ import annotations
import asyncio
import json
import logging
import time
from typing import Any, AsyncGenerator, Dict, List, Optional

logger = logging.getLogger("omnis.gemini.stream_handler")


class TokenBuffer:
    """
    Vyrovnávací paměť pro shromažďování tokenů s měřením latence prvního tokenu (TTFT).
    """

    def __init__(self) -> None:
        self.start_time = time.time()
        self.first_token_time: Optional[float] = None
        self.tokens: List[str] = []
        self.chunk_count: int = 0

    def append(self, token: str) -> None:
        if self.first_token_time is None:
            self.first_token_time = time.time()
        self.tokens.append(token)
        self.chunk_count += 1

    @property
    def ttft_ms(self) -> float:
        if self.first_token_time is None:
            return 0.0
        return round((self.first_token_time - self.start_time) * 1000, 2)

    @property
    def full_text(self) -> str:
        return "".join(self.tokens)


async def sse_chunk_multiplexer(
    text_stream: AsyncGenerator[str, None],
    event_type: str = "token",
) -> AsyncGenerator[str, None]:
    """
    Multiplexuje asynchronní proud textových tokenů do formátu SSE (Server-Sent Events).
    Každý blok odpovídá standardu: `event: <typ>\ndata: <json>\n\n`.
    """
    try:
        async for chunk in text_stream:
            payload = json.dumps({"type": event_type, "chunk": chunk, "ts": time.time()})
            yield f"event: {event_type}\ndata: {payload}\n\n"
    except Exception as exc:
        err_payload = json.dumps({"type": "error", "error": str(exc)})
        yield f"event: error\ndata: {err_payload}\n\n"


class StreamHandler:
    """
    Obsluha streamovacích toků pro Gemini a offline simulaci.
    """

    @classmethod
    async def simulate_streaming(
        cls,
        full_text: str,
        chunk_size: int = 4,
        delay_sec: float = 0.015,
    ) -> AsyncGenerator[str, None]:
        """
        Simuluje plynulý tokenový stream pro offline režim nebo záložní běh.
        """
        words = full_text.split(" ")
        for i in range(0, len(words), chunk_size):
            chunk = " ".join(words[i : i + chunk_size]) + " "
            yield chunk
            await asyncio.sleep(delay_sec)
