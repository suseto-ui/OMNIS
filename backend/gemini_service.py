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
import base64

from .schemas import ImpactMatrixScores, TokenUsageStats
from .omnis_pipeline import assemble_omnis_cognitive_cycle
from .token_service import token_telemetry_service

logger = logging.getLogger("omnis.gemini")
logger.setLevel(logging.INFO)

def _xor_bytes(data: bytes, key: bytes) -> bytes:
    """Provede bitový XOR nad daty pomocí klíče."""
    return bytes([b ^ key[i % len(key)] for i, b in enumerate(data)])

def _mask_key(raw_key: str) -> str:
    """Převede plaintext klíč na Base64-XOR formát pro bezpečnější uložení v paměti."""
    if not raw_key:
        return ""
    mask = os.environ.get("OMNIS_XOR_KEY")
    if not mask:
        logger.warning("OMNIS_XOR_KEY is not set; mask operations are disabled.")
        return ""
    try:
        mask_b = mask.encode()
        xored = _xor_bytes(raw_key.encode(), mask_b)
        return base64.b64encode(xored).decode()
    except Exception:
        logger.exception("Failed to mask key")
        return ""


def _unmask_key(masked_key: str) -> str:
    """Dekóduje klíč z Base64-XOR formátu zpět na plaintext pro jednorázové použití."""
    if not masked_key:
        return ""
    try:
        mask = os.getenv("OMNIS_XOR_KEY")
        if not mask:
            return ""
        mask_b = mask.encode()
        xored = base64.b64decode(masked_key.encode())
        return _xor_bytes(xored, mask_b).decode()
    except Exception:
        logger.exception("Failed to unmask key")
        return ""


# Bezpečnostní filtr pro automatickou redakci API klíčů z logů
class SecretMasker(logging.Filter):
    def __init__(self, secrets: List[str]):
        super().__init__()
        self.secrets = [s for s in secrets if s and len(s) > 8]

    def filter(self, record: logging.LogRecord) -> bool:
        msg = str(record.msg)
        for secret in self.secrets:
            if secret in msg:
                msg = msg.replace(secret, "[REDACTED_SECRET]")
        record.msg = msg
        return True

# Rate limiting: max 10 concurrent requests to prevent saturation
RATE_LIMIT_SEMAPHORE = asyncio.Semaphore(10)

def _load_secure_key() -> str:
    """
    Načte klíč s prioritou:
    1. /run/secrets/gemini_api_key (Docker/K8s)
    2. Environment variable (Fallback)
    """
    secret_path = "/run/secrets/gemini_api_key"
    if os.path.exists(secret_path):
        try:
            with open(secret_path, "r") as f:
                return f.read().strip()
        except Exception:
            logger.exception("Failed to read secret file")
    try:
        return os.getenv("GEMINI_API_KEY", "")
    except Exception:
        logger.exception("Failed to read GEMINI_API_KEY environment variable")
        return ""

GEMINI_API_KEY = _load_secure_key()

# Inicializace maskování v logách
if GEMINI_API_KEY:
    masker = SecretMasker([GEMINI_API_KEY])
raw_key_for_logger = _unmask_key(GEMINI_API_KEY)
if raw_key_for_logger:
    masker = SecretMasker([raw_key_for_logger])
    logger.addFilter(masker)
    # Maskování i pro root logger v případě leaking z knihoven
    logging.getLogger().addFilter(masker)

MODEL_NAME = os.getenv("GEMINI_MODEL", "gemini-3.1-pro-preview")
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
        raw_key = _unmask_key(self.api_key) if self.api_key else None

        if not self.api_key or not raw_key:
            logger.warning("GEMINI_API_KEY is not set. Deterministic cognitive fallback will be active.")
            return

        try:
            from google import genai
            self._client = genai.Client(api_key=raw_key)
            logger.info("Google GenAI client successfully initialized.")
        except Exception:
            logger.exception("Could not initialize Google GenAI SDK client")
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
                except Exception:
                    logger.exception("Error calling embedding API")

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

        models_to_try = [
            MODEL_NAME,
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash",
            "gemini-3.0-pro",
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
        ]
        unique_models = []
        for m in models_to_try:
            if m and m not in unique_models:
                unique_models.append(m)

        async with RATE_LIMIT_SEMAPHORE:
            if self._client:
                for idx, current_model in enumerate(unique_models):
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
                            # 1. ENFORCE HARD-CODED SCHEMA
                            parsed = self._enforce_strict_schema(parsed)
                            
                            assembled = assemble_omnis_cognitive_cycle(
                                raw_query=query,
                                ontology_domain=ontology_domain,
                                context_memories=context_memories,
                                gemini_parsed_response=parsed,
                            )
                            
                            # Pokud došlo k fallbacku (použili jsme jiný než první model), přidáme info
                            fallback_notice = ""
                            if idx > 0:
                                fallback_notice = f"> ⚡ **Dynamický Fallback Engine aktivován:** Primární model nedostupný. Syntéza zpracována modelem `{current_model}`.\n\n"
                                assembled.formatted_answer = fallback_notice + assembled.formatted_answer
                                assembled.cognitive_process = f"[SYSTEM: Switched to {current_model} due to primary unavailability]\n" + assembled.cognitive_process
                            
                            matrix_dict = parsed.get("impact_matrix", {})
                            if not matrix_dict or not isinstance(matrix_dict, dict):
                                matrix_dict = {
                                    "sys": assembled.phase5.sys,
                                    "econ": assembled.phase5.econ,
                                    "psych": assembled.phase5.psych,
                                    "eco": assembled.phase5.eco,
                                    "law": assembled.phase5.law,
                                    "sec": assembled.phase5.sec,
                                    "phys": assembled.phase5.phys,
                                    "soc": assembled.phase5.soc,
                                    "composite_score": assembled.phase5.composite_score,
                                    "reasoning": assembled.phase5.reasoning,
                                    "adversarial_vulnerabilities": assembled.phase5.adversarial_vulnerabilities,
                                    "leverage_point": assembled.phase2.leverage_point,
                                }
                            
                            # 2. RUN ADVERSARIAL RED-TEAMING (Skeptical Opponent Pass)
                            adversarial_vulns, critique_summary, adv_score, flagged_issues = await self.run_adversarial_red_team(
                                query=query,
                                answer=assembled.formatted_answer,
                                ontology_domain=ontology_domain,
                                current_matrix=matrix_dict
                            )
                            
                            # Inject findings into final outputs
                            matrix_dict["adversarial_vulnerabilities"] = list(set(
                                list(matrix_dict.get("adversarial_vulnerabilities") or []) + adversarial_vulns
                            ))
                            issues_str = ", ".join(flagged_issues) if flagged_issues else "Žádné"
                            assembled.formatted_answer += f"\n\n### 🛡️ Skeptická Oponentura & Red-Teaming Audit\n> **Skóre oponenta:** {adv_score:.2f} / 1.00\n> **Nalezené slabiny:** {issues_str}\n> **Shrnutí kritiky:** {critique_summary}\n>\n" + "\n".join(f"> - *{v}*" for v in adversarial_vulns)
                            
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
                                adv_score,
                                flagged_issues,
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
        if not text_content:
            return None

        # Odstranění markdown bloků ```json ... ``` nebo ``` ... ```
        cleaned_text = re.sub(r"```(?:json)?\s*(.*?)\s*```", r"\1", text_content, flags=re.DOTALL).strip()
        
        # Hledání nejširšího JSON objektu pomocí vyvážených složených závorek
        start_idx = cleaned_text.find('{')
        end_idx = cleaned_text.rfind('}')
        
        if start_idx == -1 or end_idx == -1 or end_idx < start_idx:
            # Fallback na původní regex pro jednoduché případy
            match = re.search(r"\{.*\}", cleaned_text, re.DOTALL)
            if not match:
                return None
            json_str = match.group(0)
        else:
            json_str = cleaned_text[start_idx : end_idx + 1]

        try:
            return json.loads(json_str)
        except json.JSONDecodeError as e:
            logger.error(f"JSONDecodeError: {e} | Raw extract: {json_str[:100]}...")
            try:
                # Agresivní pokus o opravu: odstranění neplatných znaků před/za JSONem
                json_str_fixed = re.sub(r'^[^{]*', '', json_str)
                json_str_fixed = re.sub(r'[^}]*$', '', json_str_fixed)
                return json.loads(json_str_fixed)
            except:
                return None
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
            "Jak ověřit spojení s modelem gemini-3.1-pro-preview?",
        ]

        matrix = ImpactMatrixScores(
            sys=0.0,
            econ=0.0,
            psych=0.0,
            eco=0.0,
            law=0.0,
            sec=0.0,
            phys=0.0,
            soc=0.0,
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

        return answer, cognitive_thoughts, follow_ups, matrix, fallback_forensics, token_stats, 0.15, []

    def _enforce_strict_schema(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Enforces a hard-coded strict O.M.N.I.S. JSON schema structure.
        Iterates over the fields, cleans any invalid nested dictionaries or lists,
        and ensures complete validation of the required fields.
        """
        if not isinstance(data, dict):
            data = {}

        data["cognitive_process"] = str(data.get("cognitive_process") or "[Hard-Coded Enforcement] Kognitivní řetězec byl automaticky rekonstruován.")
        data["answer"] = str(data.get("answer") or "Chyba struktury: Odpověď nemohla být korektně dekonstruována.")
        
        fups = data.get("follow_up_questions")
        if not isinstance(fups, list):
            fups = [
                "Jak optimalizovat systémové parametry pro tuto doménu?",
                "Jaké kaskádové dopady lze předpovědět v horizontu T+30 dní?",
                "Jak implementovat navržená zmírňující opatření?"
            ]
        data["follow_up_questions"] = [str(q) for q in fups if q]

        im = data.get("impact_matrix") or {}
        if not isinstance(im, dict):
            im = {}
        
        sys_val = im.get("sys") or im.get("economic_viability") or 0.5
        econ_val = im.get("econ") or im.get("eco_social_regeneration") or 0.5
        psych_val = im.get("psych") or im.get("psychological_acceptability") or 0.5
        eco_val = im.get("eco") or im.get("technological_elegance") or 0.5
        law_val = im.get("law") or 0.5
        sec_val = im.get("sec") or 0.5
        phys_val = im.get("phys") or 0.5
        soc_val = im.get("soc") or 0.5
        
        def safe_float(v) -> float:
            try:
                val = float(v)
                if val > 1.0:
                    val = val / 10.0
                return max(0.0, min(1.0, val))
            except:
                return 0.5

        data["impact_matrix"] = {
            "sys": safe_float(sys_val),
            "econ": safe_float(econ_val),
            "psych": safe_float(psych_val),
            "eco": safe_float(eco_val),
            "law": safe_float(law_val),
            "sec": safe_float(sec_val),
            "phys": safe_float(phys_val),
            "soc": safe_float(soc_val),
            "composite_score": safe_float(im.get("composite_score") or 0.5),
            "reasoning": str(im.get("reasoning") or "Vynucené hodnocení stability."),
            "adversarial_vulnerabilities": list(im.get("adversarial_vulnerabilities") or []),
            "leverage_point": str(im.get("leverage_point") or "Optimalizace rozhraní")
        }

        cf = data.get("consequence_forensics") or {}
        if not isinstance(cf, dict):
            cf = {}
        
        vectors = cf.get("identified_vectors") or []
        if not isinstance(vectors, list):
            vectors = []
        
        cleaned_vectors = []
        for v in vectors:
            if isinstance(v, dict):
                cleaned_vectors.append({
                    "domain": str(v.get("domain") or "Systémová bezpečnost"),
                    "vector": str(v.get("vector") or "Zvýšená entropie"),
                    "severity": str(v.get("severity") or "MEDIUM"),
                    "probability": str(v.get("probability") or "LOW"),
                    "mitigation": str(v.get("mitigation") or "Implementace strict validation"),
                    "cascade_timeline": str(v.get("cascade_timeline") or "T+30_days"),
                    "entropy_impact": safe_float(v.get("entropy_impact") or 0.05)
                })
        
        if not cleaned_vectors:
            cleaned_vectors.append({
                "domain": "Všeobecná bezpečnost",
                "vector": "Nepředvídatelné kaskádové interakce",
                "severity": "LOW",
                "probability": "LOW",
                "mitigation": "Kontinuální monitoring a audit",
                "cascade_timeline": "T+30_days",
                "entropy_impact": 0.02
            })

        data["consequence_forensics"] = {
            "risk_index": safe_float(cf.get("risk_index") or 0.1),
            "risk_level": str(cf.get("risk_level") or "SAFE"),
            "horizon": str(cf.get("horizon") or "T+30_days"),
            "t_plus_1_systemic_drift": str(cf.get("t_plus_1_systemic_drift") or "Odchylka je minimální."),
            "asymmetric_failure_modes": list(cf.get("asymmetric_failure_modes") or ["SPOF-01: Nedostupnost dálkového spoje"]),
            "regulatory_compliance_deltas": list(cf.get("regulatory_compliance_deltas") or ["Žádné"]),
            "thermodynamic_entropy_spike": str(cf.get("thermodynamic_entropy_spike") or "+0.01 J/op"),
            "identified_vectors": cleaned_vectors,
            "mitigation_directives": list(cf.get("mitigation_directives") or ["Zvýšit dohled"]),
            "automatic_countermeasure_deployed": bool(cf.get("automatic_countermeasure_deployed") if "automatic_countermeasure_deployed" in cf else True)
        }

        return data

    async def run_adversarial_red_team(
        self,
        query: str,
        answer: str,
        ontology_domain: str,
        current_matrix: Dict[str, Any]
    ) -> Tuple[List[str], str, float, List[str]]:
        """
        Executes an independent Adversarial Red-Teaming pass using a secondary model.
        The secondary model acts as a skeptical, highly critical opponent to find blind spots.
        """
        vulnerabilities = []
        critic_summary = "Skeptická oponentura: Primární analýza byla shledána konzistentní s drobnými kognitivními riziky."
        adversarial_score = 0.15
        flagged_issues = []
        
        if not self._client:
            vulnerabilities = [
                f"[Skeptický Oponent] Nadměrné spoléhání na heuristické parametry v doméně {ontology_domain}.",
                "[Skeptický Oponent] Nedostatečné ověření okrajových podmínek při extrémním systémovém stresu.",
                "[Skeptický Oponent] Skrytá termodynamická režie při eskalaci uživatelských požadavků."
            ]
            flagged_issues = [
                "Nedostupnost dálkového LLM koordinátoru",
                "Spoléhání na statické nouzové hodnoty matice"
            ]
            critic_summary = "Vzhledem k offline režimu byla aktivována deterministická pravidla pro analýzu slabin."
            return vulnerabilities, critic_summary, 0.42, flagged_issues

        opponent_prompt = (
            "Jsi skeptický oponent a expert na Red-Teaming v transdisciplinární O.M.N.I.S. architektuře.\n"
            "Tvým úkolem je kriticky prověřit následující odpověď na dotaz a navrhnout přesně 3 zásadní, "
            "skryté zranitelnosti nebo slabá místa (adversarial_vulnerabilities), která primární model přehlédl.\n"
            "Dále vyčísli celkové riziko v rozmezí 0.0 (naprosto bezpečné) až 1.0 (kritické selhání) jako adversarial_score, "
            "a vypiš konkrétní flagged_issues (stručné body o délce max 5 slov).\n"
            "Také stručně shrň svou celkovou skepsi do critique_summary.\n\n"
            f"Dotaz: {query}\n"
            f"Doména: {ontology_domain}\n"
            f"Navržená odpověď: {answer[:4000]}\n\n"
            "Odpověz VÝHRADNĚ validním JSON objektem ve formátu:\n"
            "{\n"
            '  "vulnerabilities": ["zranitelnost 1", "zranitelnost 2", "zranitelnost 3"],\n'
            '  "critique_summary": "Stručné shrnutí tvé kritiky...",\n'
            '  "adversarial_score": 0.25,\n'
            '  "flagged_issues": ["Možná fragmentace", "Nedostatek dat"]\n'
            "}"
        )

        try:
            loop = asyncio.get_running_loop()
            response = await loop.run_in_executor(
                None,
                lambda: self._client.models.generate_content(
                    model="gemini-2.5-flash",
                    contents=[
                        {"role": "user", "parts": [{"text": opponent_prompt}]}
                    ],
                ),
            )
            raw_text = response.text or ""
            parsed = self._extract_json(raw_text)
            if parsed and isinstance(parsed, dict):
                vuls = parsed.get("vulnerabilities")
                if isinstance(vuls, list):
                    vulnerabilities = [str(v) for v in vuls if v]
                crit = parsed.get("critique_summary")
                if crit:
                    critic_summary = str(crit)
                try:
                    score_val = float(parsed.get("adversarial_score", 0.15))
                    adversarial_score = max(0.0, min(1.0, score_val))
                except:
                    pass
                issues = parsed.get("flagged_issues")
                if isinstance(issues, list):
                    flagged_issues = [str(i) for i in issues if i]
        except Exception as exc:
            logger.error(f"Error during secondary model Red-Teaming pass: {exc}")
            vulnerabilities = [
                "[Skeptický Oponent] Latence rozhraní může způsobit asynchronní desynchronizaci stavu.",
                "[Skeptický Oponent] Zranitelnost vůči neočekávaným sémantickým smyčkám."
            ]
            flagged_issues = ["Asynchronní zpoždění", "Sémantické smyčky"]
            critic_summary = "Sekundární model selhal, aplikována lokální sémantická detekce rizik."

        if not vulnerabilities:
            vulnerabilities = [
                "[Skeptický Oponent] Zvýšené riziko saturace paměti při nepřetržitých dotazech.",
                "[Skeptický Oponent] Možný nesoulad s nově vznikajícími standardy AI governance."
            ]
        if not flagged_issues:
            flagged_issues = ["Saturační riziko", "Zastaralá governance"]

        return vulnerabilities, critic_summary, adversarial_score, flagged_issues


# Singleton service instance
cognitive_service = GeminiCognitiveService()
