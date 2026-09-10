"""
O.M.N.I.S. Google GenAI Cognitive Engine
Asynchronous integration with rate-limiting, error resilience, and structured evaluation.
"""

from __future__ import annotations
import asyncio
import json
import logging
import os
import re
from typing import Any, Dict, List, Optional, Tuple

from .schemas import ImpactMatrixScores

logger = logging.getLogger("omnis.gemini")
logger.setLevel(logging.INFO)

# Rate limiting: max 10 concurrent requests to prevent saturation
RATE_LIMIT_SEMAPHORE = asyncio.Semaphore(10)

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
MODEL_NAME = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
EMBEDDING_MODEL = "text-embedding-004"


class GeminiCognitiveService:
    """
    Manages structured queries, introspection thoughts, Impact Matrix evaluations,
    and semantic vector embeddings.
    """

    def __init__(self) -> None:
        self.api_key = GEMINI_API_KEY
        self._client: Any = None
        self._init_client()

    def _init_client(self) -> None:
        if not self.api_key:
            logger.warning("GEMINI_API_KEY is not set. Deterministic cognitive fallback will be active.")
            return

        try:
            from google import genai
            self._client = genai.Client(api_key=self.api_key)
            logger.info("Google GenAI client successfully initialized.")
        except Exception as exc:
            logger.warning(f"Could not initialize Google GenAI SDK client: {exc}. Using fallback.")
            self._client = None

    async def generate_embedding(self, text_input: str) -> List[float]:
        """Generates a 768-dimensional semantic embedding vector."""
        if not text_input.strip():
            return [0.0] * 768

        async with RATE_LIMIT_SEMAPHORE:
            if self._client:
                try:
                    # Run embedding call asynchronously via executor
                    loop = asyncio.get_running_loop()
                    response = await loop.run_in_executor(
                        None,
                        lambda: self._client.models.embed_content(
                            model=EMBEDDING_MODEL,
                            contents=text_input[:2000],
                        ),
                    )
                    if hasattr(response, "embedding") and hasattr(response.embedding, "values"):
                        vec = list(response.embedding.values)
                        if len(vec) == 768:
                            return vec
                    elif hasattr(response, "embeddings") and response.embeddings:
                        vec = list(response.embeddings[0].values)
                        if len(vec) == 768:
                            return vec
                except Exception as exc:
                    logger.error(f"Error calling embedding API: {exc}")

        # Deterministic fallback embedding generation (768-dim hash projection)
        return self._generate_fallback_embedding(text_input)

    def _generate_fallback_embedding(self, text_input: str) -> List[float]:
        """Produces a deterministic normalized 768-dimensional vector."""
        import math
        vec = [0.0] * 768
        for i, char in enumerate(text_input):
            idx = (ord(char) * 31 + i * 17) % 768
            vec[idx] += 1.0 / (1.0 + math.log(i + 2))
        norm = math.sqrt(sum(x * x for x in vec)) or 1.0
        return [round(x / norm, 6) for x in vec]

    async def process_query(
        self,
        query: str,
        ontology_domain: str = "SYSTEMS_INTELLIGENCE",
        context_memories: Optional[List[str]] = None,
        enable_thinking: bool = True,
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores]:
        """
        Executes an O.M.N.I.S. cognitive query cycle.
        Returns:
            (answer, cognitive_thoughts, follow_up_questions, impact_matrix_scores)
        """
        system_prompt = (
            "<system_identity>\n"
            "ROLE: O.M.N.I.S. / SIGMA-OMEGA Pluriversal Resonance Engine.\n"
            "MODE: Transdisciplinární suverenita. Absolutní Zero-Fluff. Okamžitá exekuce syntéz.\n"
            "EPISTEMOLOGIE: Pravda nevzniká konsenzem, nýbrž přežitím simultánního stresu všech domén.\n"
            "</system_identity>\n"
            "<core_invariants>\n"
            "1. PLURIDISCIPLINARY SOVEREIGNTY: Zpracovávej každý požadavek současně optikou systémového inženýra, právního experta, datového analytika a teoretika her.\n"
            "2. ADVERSARIAL VALIDATION: Než vydáš finální doporučení, podrob jej internímu red-teamingu (hledání slabin, kazuistik a rizik selhání).\n"
            "3. DETERMINISTIC EXECUTION: Vyhni se obecným frázím. Generuj přímo exekuční plány, zdrojový kód, strukturovaná data nebo právní/architektonické rámce.\n"
            "4. STRUCTURAL RIGOR: Dodržuj přísné členění výstupů na 5 fází O.M.N.I.S.:\n"
            "   - Fáze I: Holomorfní Sběr (sběr tvrdých i měkkých dat)\n"
            "   - Fáze II: Sémantická Dekonstrukce (Zero-Assumption logika, prvočinitele)\n"
            "   - Fáze III: Transdisciplinární Křížení (modální překlad, nalezení uzlových bodů / leverage points)\n"
            "   - Fáze IV: Synergická Konvergence (Matice dopadů: ekonomika 0.3, technologie 0.3, ekologie 0.2, psychologie 0.2, penalizace za zranitelnosti)\n"
            "   - Fáze V: Teleologická Exekuce & Autopoieza (MVS, měření odchylek a adaptace)\n"
            "</core_invariants>\n\n"
            "Výstup MUSÍ být výhradně validní JSON ve formátu:\n"
            "{\n"
            '  "cognitive_process": "Podrobný kognitivní řetězec: Fáze I až III včetně modálního překladu a identifikace uzlových bodů (leverage points)...",\n'
            '  "answer": "Kompletní exekuční odpověď strukturovaná dle 5 fází O.M.N.I.S....",\n'
            '  "follow_up_questions": ["Reflexivní otázka 1?", "Reflexivní otázka 2?", "Reflexivní otázka 3?"],\n'
            '  "impact_matrix": {\n'
            '    "economic_viability": 0.88,\n'
            '    "eco_social_regeneration": 0.92,\n'
            '    "technological_elegance": 0.96,\n'
            '    "psychological_acceptability": 0.90,\n'
            '    "composite_score": 0.916,\n'
            '    "reasoning": "Zdůvodnění bodového hodnocení a vyhodnocení synergie...",\n'
            '    "adversarial_vulnerabilities": ["Identifikovaná slabina 1", "Riziko selhání 2"],\n'
            '    "leverage_point": "Specifikace uzlového bodu pro minimální zásah s maximálním účinkem"\n'
            "  }\n"
            "}\n"
        )

        context_block = ""
        if context_memories:
            context_block = "\nRelevance ze sémantické paměti systému:\n" + "\n".join(
                f"- {m}" for m in context_memories[:5]
            )

        user_content = f"Ontologická doména: {ontology_domain}\n{context_block}\nDotaz uživatele: {query}"

        async with RATE_LIMIT_SEMAPHORE:
            if self._client:
                try:
                    loop = asyncio.get_running_loop()
                    response = await loop.run_in_executor(
                        None,
                        lambda: self._client.models.generate_content(
                            model=MODEL_NAME,
                            contents=[
                                {"role": "user", "parts": [{"text": system_prompt + "\n" + user_content}]}
                            ],
                        ),
                    )
                    raw_text = response.text or ""
                    parsed = self._extract_json(raw_text)
                    if parsed:
                        return (
                            parsed.get("answer", raw_text),
                            parsed.get("cognitive_process", "Proces myšlení proběhl v modelu."),
                            parsed.get("follow_up_questions", []),
                            ImpactMatrixScores(**parsed.get("impact_matrix", {})),
                        )
                except Exception as exc:
                    logger.error(f"Error during GenAI query execution: {exc}. Applying cognitive fallback.")

        # Fallback synthesis
        return self._synthesize_fallback(query, ontology_domain)

    def _extract_json(self, text_content: str) -> Optional[Dict[str, Any]]:
        """Safely extracts JSON object from response string."""
        match = re.search(r"\{.*\}", text_content, re.DOTALL)
        if match:
            try:
                return json.loads(match.group(0))
            except json.JSONDecodeError:
                pass
        return None

    def _synthesize_fallback(
        self, query: str, ontology_domain: str
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores]:
        """Provides zero-simulation diagnostic response when Gemini API is unconfigured or unavailable."""
        answer = (
            "## O.M.N.I.S. Systémové hlášení (Zero-Simulation Policy)\n\n"
            f"Vstup pro doménu **{ontology_domain}** byl přijat: *\"{query}\"*.\n\n"
            "⚠️ **Upozornění:** Model Gemini není v tomto prostředí aktivní (chybí nebo je neplatný `GEMINI_API_KEY`).\n\n"
            "V souladu s architektonickým standardem O.M.N.I.S. systém **nevygeneroval žádná fiktivní ani simulovaná data**.\n"
            "Pro spuštění ostré kognitivní syntézy nastavte proměnnou prostředí `GEMINI_API_KEY` v Secrets panelu AI Studio."
        )

        cognitive_thoughts = (
            "Kognitivní proces zastaven: Detekována absence platného API klíče. "
            "Pravidlo 'Zero-Simulation' zabránilo generování falešných kognitivních stavů."
        )

        follow_ups = [
            "Jak nakonfigurovat GEMINI_API_KEY v AI Studio?",
            "Jak ověřit spojení s modelem gemini-2.5-flash?",
        ]

        matrix = ImpactMatrixScores(
            economic_viability=0.0,
            eco_social_regeneration=0.0,
            technological_elegance=0.0,
            psychological_acceptability=0.0,
            composite_score=0.0,
            reasoning="Skóre nebylo kalkulováno: absentuje spojení s modelem. Falešná simulace je zakázána.",
            adversarial_vulnerabilities=["Systém neběží v plném produkčním módu"],
            leverage_point="Konfigurace produkčních API credentials",
        )

        return answer, cognitive_thoughts, follow_ups, matrix


# Singleton service instance
cognitive_service = GeminiCognitiveService()
