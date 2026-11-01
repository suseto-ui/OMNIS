"""
O.M.N.I.S. Modular Gemini Service Package
Splits Gemini cognitive integration into client, prompt_engine, and stream_handler.
"""

from .client import GeminiClient, GeminiCircuitBreaker, CircuitBreakerState, get_gemini_client
from .prompt_engine import PromptEngine, build_4block_system_prompt
from .stream_handler import StreamHandler, TokenBuffer, sse_chunk_multiplexer

__all__ = [
    "GeminiClient",
    "GeminiCircuitBreaker",
    "CircuitBreakerState",
    "get_gemini_client",
    "PromptEngine",
    "build_4block_system_prompt",
    "StreamHandler",
    "TokenBuffer",
    "sse_chunk_multiplexer",
]
