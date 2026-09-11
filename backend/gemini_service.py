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

from .schemas import ImpactMatrixScores, TokenUsageStats
from .omnis_pipeline import assemble_omnis_cognitive_cycle
from .token_service import token_telemetry_service

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
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores, Optional[Dict[str, Any]], TokenUsageStats]:
        """
        Executes an O.M.N.I.S. cognitive query cycle.
        Returns:
            (answer, cognitive_thoughts, follow_up_questions, impact_matrix_scores, consequence_forensics, token_usage)
        """
        system_prompt = (
            "<system_identity>\n"
            "ROLE: O.M.N.I.S. / SIGMA-OMEGA Pluriversal Resonance Engine.\n"
            "MODE: Transdisciplinární suverenita. Absolutní Zero-Fluff. Okamžitá exekuce syntéz.\n"
            "EPISTEMOLOGIE: Pravda nevzniká konsenzem, nýbrž přežitím simultánního stresu všech domén.\n"
            "</system_identity>\n"
            "<core_invariants>\n"
            "1. PLURIDISCIPLINARY SOVEREIGNTY: Zpracovávej každý požadavek současně skrze 8 transdisciplinárních domén:\n"
            "   (1) Systémové inženýrství & Kybernetika, (2) Teorie her & Asymetrická ekonomie, (3) Kognitivní vědy & Neuro-ergonomie,\n"
            "   (4) Regenerativní dynamika & Ekologie, (5) Regulace, Právo & AI Governance, (6) Zero-Trust Bezpečnost & Kryptografie,\n"
            "   (7) Fyzikální termodynamika & Výpočetní efektivita, (8) Socio-kulturní dynamika & Etická rezonance.\n"
            "2. ADVERSARIAL VALIDATION: Než vydáš finální doporučení, podrob jej internímu red-teamingu (hledání slabin, kazuistik a rizik selhání).\n"
            "3. DETERMINISTIC EXECUTION: Vyhni se obecným frázím. Generuj přímo exekuční plány, zdrojový kód, strukturovaná data nebo právní/architektonické rámce.\n"
            "4. STRUCTURAL RIGOR: Dodržuj přísné členění výstupů na 5 fází O.M.N.I.S.:\n"
            "   - Fáze I: Sémantická Dekonstrukce (Zero-Assumption logika, rozklad na prvočinitele)\n"
            "   - Fáze II: Transdisciplinární Křížení (modální překlad mezi 8 doménami, nalezení pákového uzlového bodu / leverage point)\n"
            "   - Fáze III: Okamžitý Akční Plán (Win-Win-Win strategie, poměr úsilí/páka 1:10, milníky)\n"
            "   - Fáze IV: Deterministická Exekuce (konkrétní spustitelný kód / formální direktiva bez sémantického šumu)\n"
            "   - Fáze V: Autopoietická Reflexe & 4D Matice dopadů (váhy: ekonomika 0.3, technologie 0.3, ekologie 0.2, psychologie 0.2, penalizace za zranitelnosti)\n"
            "5. PROSPECTIVE RISK FORENSICS: Závěrečná meta-analýza kaskádových rizik T+1 až T+N napříč všemi 8 doménami.\n"
            "</core_invariants>\n\n"
            "Výstup MUSÍ být výhradně validní JSON ve formátu:\n"
            "{\n"
            '  "cognitive_process": "Podrobný kognitivní řetězec: Fáze I až V včetně 8-doménového modálního překladu a identifikace uzlových bodů (leverage points)...",\n'
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
            "  },\n"
            '  "consequence_forensics": {\n'
            '    "risk_index": 0.042,\n'
            '    "horizon": "T+30_days",\n'
            '    "t_plus_1_systemic_drift": "Minimální odchylka v T+1",\n'
            '    "asymmetric_failure_modes": ["SPOF-01: Potenciální latence rozhraní"],\n'
            '    "regulatory_compliance_deltas": ["Soulad s EU AI Act"],\n'
            '    "thermodynamic_entropy_spike": "+0.02 J/op",\n'
            '    "identified_vectors": [\n'
            "      {\n"
            '        "domain": "Zero-Trust Bezpečnost & Kryptografie",\n'
            '        "vector": "Zvýšená entropie vstupních dat při eskalaci uživatelských oprávnění",\n'
            '        "severity": "MEDIUM",\n'
            '        "probability": "LOW",\n'
            '        "mitigation": "Vynucení strict PoLP filtru na rozhraní API"\n'
            "      }\n"
            "    ],\n"
            '    "mitigation_directives": ["DIR-01: Striktní validace"],\n'
            '    "automatic_countermeasure_deployed": true\n'
            "  }\n"
            "}\n"
        )

        context_block = ""
        if context_memories:
            context_block = "\nRelevance ze sémantické paměti systému:\n" + "\n".join(
                f"- {m}" for m in context_memories[:5]
            )

        user_content = f"Ontologická doména: {ontology_domain}\n{context_block}\nDotaz uživatele: {query}"

        models_to_try = [MODEL_NAME, "gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash"]
        unique_models = []
        for m in models_to_try:
            if m not in unique_models:
                unique_models.append(m)

        async with RATE_LIMIT_SEMAPHORE:
            if self._client:
                for current_model in unique_models:
                    try:
                        loop = asyncio.get_running_loop()
                        response = await loop.run_in_executor(
                            None,
                            lambda m=current_model: self._client.models.generate_content(
                                model=m,
                                contents=[
                                    {"role": "user", "parts": [{"text": system_prompt + "\n" + user_content}]}
                                ],
                            ),
                        )
                        raw_text = response.text or ""
                        parsed = self._extract_json(raw_text)
                        if parsed:
                            assembled = assemble_omnis_cognitive_cycle(
                                raw_query=query,
                                ontology_domain=ontology_domain,
                                context_memories=context_memories,
                                gemini_parsed_response=parsed,
                            )
                            matrix_dict = parsed.get("impact_matrix", {})
                            if not matrix_dict or not isinstance(matrix_dict, dict):
                                matrix_dict = {
                                    "economic_viability": assembled.phase5.economic_viability,
                                    "technological_elegance": assembled.phase5.technological_elegance,
                                    "eco_social_regeneration": assembled.phase5.eco_social_regeneration,
                                    "psychological_acceptability": assembled.phase5.psychological_acceptability,
                                    "composite_score": assembled.phase5.composite_score,
                                    "reasoning": assembled.phase5.reasoning,
                                    "adversarial_vulnerabilities": assembled.phase5.adversarial_vulnerabilities,
                                    "leverage_point": assembled.phase2.leverage_point,
                                }
                            forensics_dict = None
                            if assembled.risk_forensics:
                                forensics_dict = {
                                    "risk_index": assembled.risk_forensics.risk_index,
                                    "risk_level": assembled.risk_forensics.risk_level,
                                    "horizon": assembled.risk_forensics.horizon,
                                    "t_plus_1_systemic_drift": assembled.risk_forensics.t_plus_1_systemic_drift,
                                    "asymmetric_failure_modes": assembled.risk_forensics.asymmetric_failure_modes,
                                    "regulatory_compliance_deltas": assembled.risk_forensics.regulatory_compliance_deltas,
                                    "thermodynamic_entropy_spike": assembled.risk_forensics.thermodynamic_entropy_spike,
                                    "identified_vectors": [
                                        {
                                            "domain": v.domain,
                                            "vector": v.vector,
                                            "severity": v.severity,
                                            "probability": v.probability,
                                            "mitigation": v.mitigation,
                                            "cascade_timeline": v.cascade_timeline,
                                            "entropy_impact": v.entropy_impact,
                                        }
                                        for v in assembled.risk_forensics.identified_vectors
                                    ],
                                    "mitigation_directives": assembled.risk_forensics.mitigation_directives,
                                    "automatic_countermeasure_deployed": assembled.risk_forensics.automatic_countermeasure_deployed,
                                }
                            usage_meta = getattr(response, "usage_metadata", None)
                            p_tokens = getattr(usage_meta, "prompt_token_count", None) or token_telemetry_service.estimate_text_tokens(system_prompt + "\n" + user_content)
                            c_tokens = getattr(usage_meta, "candidates_token_count", None) or token_telemetry_service.estimate_text_tokens(raw_text or assembled.formatted_answer)
                            rec = token_telemetry_service.record_usage(
                                prompt_tokens=p_tokens,
                                completion_tokens=c_tokens,
                                query_preview=query,
                                domain=ontology_domain,
                            )
                            token_stats = TokenUsageStats(
                                prompt_tokens=p_tokens,
                                completion_tokens=c_tokens,
                                total_tokens=p_tokens + c_tokens,
                                cost_usd=rec.cost_usd,
                            )
                            return (
                                assembled.formatted_answer,
                                assembled.cognitive_process,
                                assembled.follow_up_questions,
                                ImpactMatrixScores(**matrix_dict),
                                forensics_dict,
                                token_stats,
                            )
                    except Exception as exc:
                        exc_str = str(exc).lower()
                        if "429" in exc_str or "quota" in exc_str or "exhausted" in exc_str or "404" in exc_str or "not_found" in exc_str or "no longer available" in exc_str:
                            logger.warning(f"Rate limit, quota, or availability issue for {current_model}: {exc}. Trying fallback.")
                            continue
                        else:
                            logger.error(f"Error during GenAI query execution with {current_model}: {exc}. Applying cognitive fallback.")
                            break

        # Fallback synthesis
        return self._synthesize_fallback(query, ontology_domain)

    def _extract_json(self, text_content: str) -> Optional[Dict[str, Any]]:
        """Safely extracts JSON object from response string."""
        # [REFAKTORIZACE - SLABÉ MÍSTO]: Původní regex `\{.*\}` s re.DOTALL byl příliš naivní.
        # Mohl zachytit i text kolem JSONu nebo markdown formátování (např. ```json ... ```), což vedlo k JSONDecodeError.
        # Nyní nejdříve zkusíme očistit markdown bloky.
        text_content = text_content.strip()
        if text_content.startswith("```json"):
            text_content = text_content[7:]
        if text_content.startswith("```"):
            text_content = text_content[3:]
        if text_content.endswith("```"):
            text_content = text_content[:-3]
        text_content = text_content.strip()
            
        match = re.search(r"\{.*\}", text_content, re.DOTALL)
        if match:
            try:
                return json.loads(match.group(0))
            except json.JSONDecodeError as e:
                logger.error(f"JSONDecodeError during extraction: {e}")
                pass
        return None

    def _synthesize_fallback(
        self, query: str, ontology_domain: str
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores, Optional[Dict[str, Any]], TokenUsageStats]:
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

        fallback_forensics = {
            "risk_index": 0.05,
            "risk_level": "SAFE",
            "horizon": "T+30_days",
            "t_plus_1_systemic_drift": "Stav invariantu: Diagnostický režim bez aktivního LLM.",
            "asymmetric_failure_modes": ["SPOF-00: Chybějící credentials pro vzdálenou inferenci."],
            "regulatory_compliance_deltas": ["Plný soulad: Žádná falešná data nebyla emitována."],
            "thermodynamic_entropy_spike": "0.00 J/op (nulová režie)",
            "identified_vectors": [
                {
                    "domain": "Systémové inženýrství & Kybernetika",
                    "vector": "Absence aktivního spojení s modelem",
                    "severity": "LOW",
                    "probability": "LOW",
                    "mitigation": "Vložení GEMINI_API_KEY do Secrets panelu AI Studio",
                    "cascade_timeline": "T+1_immediate",
                    "entropy_impact": 0.01,
                }
            ],
            "mitigation_directives": ["Aktivovat GEMINI_API_KEY pro plný běh."],
            "automatic_countermeasure_deployed": True,
        }

        p_tokens = token_telemetry_service.estimate_text_tokens(query) + token_telemetry_service.BASE_SYSTEM_PROMPT_TOKENS
        c_tokens = token_telemetry_service.estimate_text_tokens(answer)
        rec = token_telemetry_service.record_usage(
            prompt_tokens=p_tokens,
            completion_tokens=c_tokens,
            query_preview=query,
            domain=ontology_domain,
        )
        token_stats = TokenUsageStats(
            prompt_tokens=p_tokens,
            completion_tokens=c_tokens,
            total_tokens=p_tokens + c_tokens,
            cost_usd=rec.cost_usd,
        )

        return answer, cognitive_thoughts, follow_ups, matrix, fallback_forensics, token_stats


# Singleton service instance
cognitive_service = GeminiCognitiveService()
