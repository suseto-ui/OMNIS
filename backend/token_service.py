"""
O.M.N.I.S. Token Telemetry & Estimation Service
Precise token calculations, session usage tracking, and cost estimations for Gemini models.
"""

from __future__ import annotations
import math
import threading
import time
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Optional


@dataclass
class QueryTokenRecord:
    query_preview: str
    domain: str
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    cost_usd: float
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict:
        return asdict(self)


@dataclass
class TokenTelemetryStats:
    cumulative_prompt_tokens: int = 0
    cumulative_completion_tokens: int = 0
    cumulative_total_tokens: int = 0
    total_queries_executed: int = 0
    estimated_total_cost_usd: float = 0.0
    estimated_total_cost_czk: float = 0.0
    recent_records: List[Dict] = field(default_factory=list)

    def to_dict(self) -> Dict:
        return asdict(self)


class TokenService:
    """
    Thread-safe tracker and estimator for LLM tokens (Gemini 2.5 Flash / Pro).
    Pricing reference (Gemini 2.5 Flash):
      - Input:  $0.075 per 1,000,000 tokens
      - Output: $0.300 per 1,000,000 tokens
      - USD/CZK rate approx ~23.50 CZK/USD
    """

    INPUT_COST_PER_MILLION: float = 0.075
    OUTPUT_COST_PER_MILLION: float = 0.300
    USD_TO_CZK: float = 23.50

    # System prompt approximate tokens (O.M.N.I.S. 8-domain, 5-phase strict instructions)
    BASE_SYSTEM_PROMPT_TOKENS: int = 1180

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._prompt_tokens: int = 0
        self._completion_tokens: int = 0
        self._query_count: int = 0
        self._history: List[QueryTokenRecord] = []

    def estimate_text_tokens(self, text: str) -> int:
        """
        Calculates token count based on Gemini multilingual tokenization patterns.
        Czech/multilingual text + punctuation averages ~3.5 to 3.8 characters per token.
        """
        if not text:
            return 0
        char_count = len(text)
        word_count = len(text.split())
        # Blended estimator: word weight + character density
        est_from_chars = char_count / 3.65
        est_from_words = word_count * 1.32
        estimated = int(math.ceil((est_from_chars * 0.6) + (est_from_words * 0.4)))
        return max(1, estimated)

    def estimate_query_tokens(
        self,
        query: str,
        include_system_prompt: bool = True,
        include_memory_context: bool = True,
    ) -> Dict[str, int | float]:
        """Estimates input and anticipated output tokens for a single query."""
        user_query_tokens = self.estimate_text_tokens(query)
        sys_tokens = self.BASE_SYSTEM_PROMPT_TOKENS if include_system_prompt else 0
        mem_tokens = 150 if include_memory_context else 0

        prompt_tokens = sys_tokens + mem_tokens + user_query_tokens

        # Anticipated completion for O.M.N.I.S. (5-phase structured JSON output)
        # Typically between 1200 and 1650 tokens
        base_completion_tokens = 1380
        # Longer queries may induce longer responses
        completion_tokens = base_completion_tokens + int(user_query_tokens * 0.5)

        total_tokens = prompt_tokens + completion_tokens
        cost_usd = (
            (prompt_tokens * self.INPUT_COST_PER_MILLION / 1_000_000.0)
            + (completion_tokens * self.OUTPUT_COST_PER_MILLION / 1_000_000.0)
        )
        cost_czk = cost_usd * self.USD_TO_CZK

        return {
            "user_query_tokens": user_query_tokens,
            "system_prompt_tokens": sys_tokens,
            "context_memory_tokens": mem_tokens,
            "estimated_prompt_tokens": prompt_tokens,
            "estimated_completion_tokens": completion_tokens,
            "estimated_total_tokens": total_tokens,
            "estimated_cost_usd": round(cost_usd, 6),
            "estimated_cost_czk": round(cost_czk, 4),
        }

    def record_usage(
        self,
        prompt_tokens: int,
        completion_tokens: int,
        query_preview: str = "",
        domain: str = "SYSTEMS_INTELLIGENCE",
    ) -> QueryTokenRecord:
        """Records executed tokens into cumulative telemetry."""
        total = prompt_tokens + completion_tokens
        cost = (
            (prompt_tokens * self.INPUT_COST_PER_MILLION / 1_000_000.0)
            + (completion_tokens * self.OUTPUT_COST_PER_MILLION / 1_000_000.0)
        )

        record = QueryTokenRecord(
            query_preview=query_preview[:60] if query_preview else "O.M.N.I.S. Query",
            domain=domain,
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            total_tokens=total,
            cost_usd=round(cost, 6),
        )

        with self._lock:
            self._prompt_tokens += prompt_tokens
            self._completion_tokens += completion_tokens
            self._query_count += 1
            self._history.append(record)
            if len(self._history) > 100:
                self._history.pop(0)

        return record

    def get_telemetry(self) -> TokenTelemetryStats:
        """Retrieves global session telemetry snapshot."""
        with self._lock:
            total_cost_usd = (
                (self._prompt_tokens * self.INPUT_COST_PER_MILLION / 1_000_000.0)
                + (self._completion_tokens * self.OUTPUT_COST_PER_MILLION / 1_000_000.0)
            )
            return TokenTelemetryStats(
                cumulative_prompt_tokens=self._prompt_tokens,
                cumulative_completion_tokens=self._completion_tokens,
                cumulative_total_tokens=self._prompt_tokens + self._completion_tokens,
                total_queries_executed=self._query_count,
                estimated_total_cost_usd=round(total_cost_usd, 6),
                estimated_total_cost_czk=round(total_cost_usd * self.USD_TO_CZK, 4),
                recent_records=list(reversed(self._history[-10:])),
            )

    def reset_telemetry(self) -> None:
        """Resets session telemetry to zero."""
        with self._lock:
            self._prompt_tokens = 0
            self._completion_tokens = 0
            self._query_count = 0
            self._history.clear()


# Global singleton instance
token_telemetry_service = TokenService()
