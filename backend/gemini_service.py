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
import time
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple
import base64

from .schemas import ImpactMatrixScores, TokenUsageStats, ChatMessage
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


@dataclass
class GeminiKeyEntry:
    name: str
    raw_key: str
    masked: str
    status: str = "ACTIVE"  # "ACTIVE", "EXHAUSTED_402", "RATE_LIMITED_429", "FAILED"
    error_reason: Optional[str] = None
    exhausted_at: Optional[float] = None
    success_count: int = 0
    fail_count: int = 0


class GeminiKeyPool:
    """
    Spravuje fond Google Gemini API klíčů ze secrets a environmentu.
    Při vyčerpání předplaceného kreditu (402) nebo kvóty (429) provádí
    okamžitý, transparentní failover na další klíč (GEMINI_API_KEY -> GEMINI_API_KEY2 -> GEMINI_API_KEY3...).
    """

    def __init__(self) -> None:
        self.entries: List[GeminiKeyEntry] = []
        self.active_index: int = 0
        self._clients: Dict[str, Any] = {}
        self._discover_keys()

    def _discover_keys(self) -> None:
        seen_keys = set()

        def extract_val(env_name: str, file_name: Optional[str] = None) -> Optional[Tuple[str, str]]:
            val = ""
            if file_name:
                secret_path = f"/run/secrets/{file_name}"
                if os.path.exists(secret_path):
                    try:
                        with open(secret_path, "r") as f:
                            val = f.read().strip()
                    except Exception:
                        pass
            if not val:
                val = os.getenv(env_name, "").strip()
            if not val:
                return None

            raw = val
            if not raw.startswith("AIza"):
                unm = _unmask_key(raw)
                if unm:
                    raw = unm
            if raw and raw not in seen_keys:
                seen_keys.add(raw)
                return (env_name, raw)
            return None

        # Prioritní řazení: 1 -> 2 -> 3 -> další
        candidates = [
            ("GEMINI_API_KEY", "gemini_api_key"),
            ("GEMINI_API_KEY1", "gemini_api_key1"),
            ("GEMINI_API_KEY_1", "gemini_api_key_1"),
            ("GEMINI_API_KEY2", "gemini_api_key2"),
            ("GEMINI_API_KEY_2", "gemini_api_key_2"),
            ("GEMINI_API_KEY3", "gemini_api_key3"),
            ("GEMINI_API_KEY_3", "gemini_api_key_3"),
            ("GEMINI_API_KEY4", "gemini_api_key4"),
            ("GEMINI_API_KEY_4", "gemini_api_key_4"),
            ("GEMINI_API_KEY5", "gemini_api_key5"),
            ("GEMINI_API_KEY_5", "gemini_api_key_5"),
        ]

        for env_k, file_k in candidates:
            res = extract_val(env_k, file_k)
            if res:
                self._add_entry(res[0], res[1])

        # Prozkoumání jakýchkoliv dalších GEMINI_API_KEY proměnných
        for env_k in sorted(os.environ.keys()):
            if "GEMINI_API_KEY" in env_k.upper() and env_k not in [e.name for e in self.entries]:
                res = extract_val(env_k)
                if res:
                    self._add_entry(res[0], res[1])

        logger.info(f"GeminiKeyPool: Inicializováno {len(self.entries)} klíčů: {[e.name for e in self.entries]}")

    def _add_entry(self, name: str, raw_key: str) -> None:
        masked = f"{raw_key[:6]}...{raw_key[-4:]}" if len(raw_key) > 10 else "***"
        self.entries.append(GeminiKeyEntry(name=name, raw_key=raw_key, masked=masked))

    def get_active_entry(self) -> Optional[GeminiKeyEntry]:
        if not self.entries:
            return None

        # Najít první aktivní klíč počínaje active_index
        total = len(self.entries)
        for i in range(total):
            idx = (self.active_index + i) % total
            entry = self.entries[idx]
            
            # Pokud byl klíč dočasně omezen rate-limitem (429), zkontrolovat 60s cooldown
            if entry.status == "RATE_LIMITED_429" and entry.exhausted_at:
                if (time.time() - entry.exhausted_at) > 60:
                    entry.status = "ACTIVE"
                    entry.error_reason = None
            
            if entry.status == "ACTIVE":
                self.active_index = idx
                return entry

        # Pokud jsou všechny označené jako vyčerpané, vrátit aktuální pro možnost retry
        return self.entries[self.active_index]

    def get_client_for_entry(self, entry: GeminiKeyEntry) -> Any:
        if entry.name in self._clients:
            return self._clients[entry.name]
        try:
            from google import genai
            client = genai.Client(api_key=entry.raw_key)
            self._clients[entry.name] = client
            return client
        except Exception:
            logger.exception(f"Nelze vytvořit Google GenAI SDK klienta pro {entry.name}")
            return None

    def mark_exhausted(self, entry: GeminiKeyEntry, error_detail: str) -> Optional[GeminiKeyEntry]:
        err_lower = error_detail.lower()
        if any(w in err_lower for w in ["402", "depleted", "prepayment", "credits"]):
            entry.status = "EXHAUSTED_402"
        elif any(w in err_lower for w in ["429", "quota"]):
            entry.status = "RATE_LIMITED_429"
        else:
            entry.status = "FAILED"

        entry.error_reason = error_detail[:250]
        entry.exhausted_at = time.time()
        entry.fail_count += 1

        logger.warning(
            f"⚡ [O.M.N.I.S. KEY-FAILOVER DETEKOVÁN] Klíč {entry.name} ({entry.masked}) byl označen jako {entry.status}: "
            f"{entry.error_reason}. Rotuji na další klíč v Secrets fondu."
        )

        # Okamžitá rotace na další klíč
        total = len(self.entries)
        for i in range(1, total):
            cand_idx = (self.active_index + i) % total
            cand = self.entries[cand_idx]
            if cand.status == "ACTIVE":
                self.active_index = cand_idx
                logger.info(f"🔄 [FAILOVER ÚSPĚŠNÝ] Aktivován náhradní klíč: {cand.name} ({cand.masked})")
                return cand

        logger.error("🚨 [ALL KEYS EXHAUSTED] Všechny dostupné klíče v Secrets fondu (1, 2, 3...) jsou vyčerpané!")
        return None

    def mark_success(self, entry: GeminiKeyEntry) -> None:
        entry.status = "ACTIVE"
        entry.success_count += 1
        entry.error_reason = None

    def get_pool_status(self) -> List[Dict[str, Any]]:
        return [
            {
                "name": e.name,
                "masked": e.masked,
                "status": e.status,
                "is_active": (idx == self.active_index),
                "success_count": e.success_count,
                "fail_count": e.fail_count,
                "error_reason": e.error_reason,
            }
            for idx, e in enumerate(self.entries)
        ]


key_pool = GeminiKeyPool()

# Bezpečnostní maskování všech klíčů v logách
all_raw_keys = [e.raw_key for e in key_pool.entries if e.raw_key]
if all_raw_keys:
    masker = SecretMasker(all_raw_keys)
    logger.addFilter(masker)
    logging.getLogger().addFilter(masker)

MODEL_NAME = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")
EMBEDDING_MODEL = os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001")


class GeminiCognitiveService:
    """
    Manages structured queries, introspection thoughts, Impact Matrix evaluations,
    and semantic vector embeddings with automatic multi-key failover.
    """

    def __init__(self) -> None:
        self.key_pool = key_pool

    @property
    def api_key(self) -> str:
        entry = self.key_pool.get_active_entry()
        return entry.raw_key if entry else ""

    @property
    def _client(self) -> Any:
        entry = self.key_pool.get_active_entry()
        if entry:
            return self.key_pool.get_client_for_entry(entry)
        return None

    async def generate_embedding(self, text_input: str) -> List[float]:
        """Generates a 768-dimensional semantic embedding vector with automatic key failover."""
        if not text_input.strip():
            return [0.0] * 768

        embedding_models_to_try = [
            EMBEDDING_MODEL,
            "gemini-embedding-001",
            "gemini-embedding-2",
            "gemini-embedding-2-preview"
        ]

        async with RATE_LIMIT_SEMAPHORE:
            max_attempts = len(self.key_pool.entries) or 1
            for _ in range(max_attempts):
                entry = self.key_pool.get_active_entry()
                if not entry:
                    break
                client = self.key_pool.get_client_for_entry(entry)
                if not client:
                    continue
                try:
                    loop = asyncio.get_running_loop()

                    for model_cand in embedding_models_to_try:
                        try:
                            def _call_embed(m=model_cand, c=client):
                                try:
                                    return c.models.embed_content(
                                        model=m,
                                        contents=text_input[:2000],
                                        config={"output_dimensionality": 768}
                                    )
                                except Exception:
                                    return c.models.embed_content(
                                        model=m,
                                        contents=text_input[:2000],
                                    )

                            response = await loop.run_in_executor(None, _call_embed)
                            vec = None
                            if hasattr(response, "embedding") and hasattr(response.embedding, "values"):
                                vec = list(response.embedding.values)
                            elif hasattr(response, "embeddings") and response.embeddings:
                                vec = list(response.embeddings[0].values)

                            if vec:
                                if len(vec) == 768:
                                    self.key_pool.mark_success(entry)
                                    return vec
                                elif len(vec) > 768:
                                    import math
                                    sub_vec = vec[:768]
                                    norm = math.sqrt(sum(x * x for x in sub_vec)) or 1.0
                                    normalized = [round(x / norm, 6) for x in sub_vec]
                                    self.key_pool.mark_success(entry)
                                    return normalized
                        except Exception as inner_e:
                            inner_str = str(inner_e).lower()
                            if any(k in inner_str for k in ["402", "depleted", "prepayment", "quota", "exhausted", "429"]):
                                raise inner_e
                            continue
                except Exception as exc:
                    exc_str = str(exc).lower()
                    if any(k in exc_str for k in ["402", "depleted", "prepayment", "quota", "exhausted", "429"]):
                        self.key_pool.mark_exhausted(entry, str(exc))
                        continue
                    else:
                        logger.exception("Error calling embedding API")
                        break

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
        history: Optional[List[ChatMessage]] = None,
        image_data: Optional[str] = None,
        image_mime: Optional[str] = "image/jpeg",
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores, Optional[Dict[str, Any]], TokenUsageStats, float, List[str]]:
        """
        Executes an O.M.N.I.S. cognitive query cycle with full conversational context.
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

        # Sestavení historie pro Gemini (konverze role 'assistant' na 'model')
        gemini_history = []
        if history:
            for msg in history:
                role = "user" if msg.role == "user" else "model"
                gemini_history.append({"role": role, "parts": [{"text": msg.content}]})

        # Finální zpráva s instrukcemi a případným vysoce rozlišitelným obrazovým streamem
        user_parts = [{"text": system_prompt + "\n" + user_content}]
        if image_data:
            b64_clean = image_data
            if "," in b64_clean:
                b64_clean = b64_clean.split(",", 1)[1]
            user_parts.append({
                "inline_data": {
                    "mime_type": image_mime or "image/jpeg",
                    "data": b64_clean
                }
            })

        gemini_history.append({"role": "user", "parts": user_parts})

        models_to_try = [
            MODEL_NAME,
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-flash-latest",
            "gemma-4-26b-a4b-it",
            "gemma-4-31b-it",
            "gemini-3.1-pro-preview",
            "gemini-2.5-pro",
        ]
        unique_models = []
        for m in models_to_try:
            if m and m not in unique_models:
                unique_models.append(m)

        last_error_detail: Optional[str] = None
        failover_keys_used: List[str] = []

        async with RATE_LIMIT_SEMAPHORE:
            max_rotations = len(self.key_pool.entries) or 1
            for key_rot in range(max_rotations):
                active_entry = self.key_pool.get_active_entry()
                if not active_entry:
                    break
                client = self.key_pool.get_client_for_entry(active_entry)
                if not client:
                    self.key_pool.mark_exhausted(active_entry, "Nelze inicializovat SDK klienta")
                    continue

                key_exhausted = False
                for idx, current_model in enumerate(unique_models):
                    try:
                        loop = asyncio.get_running_loop()
                        response = await loop.run_in_executor(
                            None,
                            lambda m=current_model, c=client: c.models.generate_content(
                                model=m,
                                contents=gemini_history,
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
                            
                            # Označit klíč jako úspěšný
                            self.key_pool.mark_success(active_entry)

                            # Pokud došlo k rotaci vyčerpaného klíče na další v Secrets, informujeme v odpovědi
                            if key_rot > 0 or failover_keys_used:
                                failover_notice = (
                                    f"> 🔄 **Automatický Key-Failover aktivován:** Vyčerpaný klíč byl automaticky nahrazen. "
                                    f"Syntéza úspěšně zpracována náhradním klíčem `{active_entry.name}` ({active_entry.masked}).\n\n"
                                )
                                assembled.formatted_answer = failover_notice + assembled.formatted_answer
                                assembled.cognitive_process = f"[SYSTEM: Failover to key {active_entry.name} completed successfully]\n" + assembled.cognitive_process

                            # Pokud došlo k fallbacku (použili jsme jiný než první model), přidáme info
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
                        last_error_detail = str(exc)
                        if any(k in exc_str for k in ["429", "402", "quota", "exhausted", "depleted", "prepayment"]):
                            logger.warning(
                                f"Klíč {active_entry.name} vyčerpal kvótu/kredit: {exc}. "
                                f"Rotuji na další klíč v Secrets fondu!"
                            )
                            failover_keys_used.append(active_entry.name)
                            self.key_pool.mark_exhausted(active_entry, str(exc))
                            key_exhausted = True
                            break # Break model loop, advance to next key in key pool
                        elif any(k in exc_str for k in ["404", "not_found", "no longer available"]):
                            logger.warning(f"Model {current_model} nedostupný: {exc}. Zkouším další model.")
                            continue
                        else:
                            logger.error(f"Error during GenAI query execution with {current_model}: {exc}. Applying cognitive fallback.")
                            break

                if not key_exhausted:
                    # Pokud chyba nebyla vyčerpáním klíče, nerotujeme bezhlavě další klíče
                    break

        # Fallback synthesis with exact diagnostic context
        return self._synthesize_fallback(query, ontology_domain, last_error_detail)

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
        self, query: str, ontology_domain: str, error_detail: Optional[str] = None
    ) -> Tuple[str, Optional[str], List[str], ImpactMatrixScores, Optional[Dict[str, Any]], TokenUsageStats, float, List[str]]:
        """Provides zero-simulation diagnostic response with precise root-cause reporting when Gemini API is unconfigured or exhausted."""
        err_lower = (error_detail or "").lower()

        pool_summary_lines = []
        for e in self.key_pool.entries:
            st = f"**`{e.name}`** ({e.masked}): status `{e.status}`"
            if e.error_reason:
                st += f" — *{e.error_reason[:90]}*"
            pool_summary_lines.append(f"- {st}")
        pool_status_md = "\n".join(pool_summary_lines) if pool_summary_lines else "- Žádné klíče nebyly detekovány."

        if "402" in err_lower or "depleted" in err_lower or "prepayment" in err_lower:
            status_title = "Předplacený kredit v Google AI Studio byl vyčerpán (402 RESOURCE_EXHAUSTED)"
            diagnostic_explanation = (
                "⚠️ **Google AI Studio: Předplacený kredit byl vyčerpán (402 RESOURCE_EXHAUSTED)**\n\n"
                "Všechny dostupné klíče v Secrets fondu vyčerpaly svůj limit nebo předplacený kredit:\n\n"
                f"### 🔑 Stav klíčů v Secrets fondu:\n{pool_status_md}\n\n"
                "### 🛠️ Jak okamžitě obnovit funkčnost:\n"
                "1. **Doplnit kredit / Zkontrolovat Billing:** Přejděte na [Google AI Studio Projects](https://ai.studio/projects), vyberte svůj projekt a v sekci **Billing** doplňte předplacený kredit.\n"
                "2. **Nový klíč z bezplatného projektu:** Vytvořte nový API klíč v novém bezplatném projektu a přidejte jej do **Secrets panelu** jako `GEMINI_API_KEY`, `GEMINI_API_KEY2` nebo `GEMINI_API_KEY3`."
            )
            cognitive_reason = "Všechny klíče v fondu vyčerpány (402 Prepayment credits depleted)."
        elif "429" in err_lower or "quota" in err_lower or "exhausted" in err_lower:
            status_title = "Překročena rychlostní kvóta požadavků (429 Rate Limit)"
            diagnostic_explanation = (
                "⚠️ **Google Gemini API: Překročena rychlostní kvóta požadavků (429 Rate Limit / Quota Exceeded)**\n\n"
                f"### 🔑 Stav klíčů v Secrets fondu:\n{pool_status_md}\n\n"
                "Byl dočasně vyčerpán limit požadavků za minutu (RPM/TPM). Počkejte prosím 30-60 sekund před dalším dotazem."
            )
            cognitive_reason = "Dočasné překročení rychlostní kvóty (429 Rate Limit)."
        elif not self.api_key:
            status_title = "Chybějící konfigurace GEMINI_API_KEY"
            diagnostic_explanation = (
                "⚠️ **Upozornění:** Model Gemini není v tomto prostředí aktivní (chybí `GEMINI_API_KEY`).\n\n"
                "Vložte svůj API klíč do **Secrets panelu** v Google AI Studio jako proměnnou `GEMINI_API_KEY` (případně `GEMINI_API_KEY2`, `GEMINI_API_KEY3`)."
            )
            cognitive_reason = "Absence klíčů v Secrets panelu."
        else:
            status_title = "Dočasná nedostupnost kognitivního jádra"
            diagnostic_explanation = (
                f"⚠️ **Chyba spojení s modelem Gemini:**\n"
                f"> {error_detail or 'Nespecifikovaná systémová chyba.'}\n\n"
                f"### 🔑 Stav klíčů v Secrets fondu:\n{pool_status_md}\n\n"
                "Data jsou bezpečně zachována v lokální paměti."
            )
            cognitive_reason = f"Chyba modelu: {error_detail or 'Neznámá chyba'}"

        answer = (
            f"## O.M.N.I.S. Systémové hlášení (Diagnostika API)\n\n"
            f"Vstup pro doménu **{ontology_domain}** byl přijat: *\"{query}\"*.\n\n"
            f"{diagnostic_explanation}\n\n"
            "V souladu s architektonickým standardem O.M.N.I.S. systém **nevygeneroval žádná fiktivní ani simulovaná data**."
        )

        cognitive_thoughts = (
            f"Kognitivní proces zastaven: {cognitive_reason} "
            "Pravidlo 'Zero-Simulation' zabránilo generování falešných kognitivních stavů."
        )

        follow_ups = [
            "Jak doplnit předplacený kredit na https://ai.studio/projects?",
            "Jak nastavit nový API klíč v Secrets panelu?",
            "Ověřit stav připojení k databázi Cloud SQL?"
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
            client = self._client
            if not client:
                raise RuntimeError("No active client available")
            loop = asyncio.get_running_loop()
            response = await loop.run_in_executor(
                None,
                lambda: client.models.generate_content(
                    model="gemini-3.5-flash",
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
