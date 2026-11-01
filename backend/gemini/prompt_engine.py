"""
O.M.N.I.S. Modular Gemini Service - Prompt Engine
Constructs 4-block system prompts, enforces 8D matrix context, and manages prompt sanitization.
"""

from __future__ import annotations
import json
import logging
import re
from typing import Any, Dict, List, Optional, Tuple

logger = logging.getLogger("omnis.gemini.prompt_engine")


def build_4block_system_prompt(
    domain: str = "SYSTEMS_INTELLIGENCE",
    custom_instructions: Optional[str] = None,
    risk_level: str = "BALANCED",
) -> str:
    """
    Sestaví striktní 4-blokový systémový prompt pro O.M.N.I.S. kognitivní syntézu:
    - BLOK 1: Identita, epistemická role a systémové axiomy
    - BLOK 2: 8D Matice dopadů (Oktagon) a Leontiefovo minimum
    - BLOK 3: Dialektický proces (Teze - Antiteze - Syntéza)
    - BLOK 4: Výstupní kontrakt a striktní formát JSON
    """
    block1_identity = (
        "=== BLOK 1: IDENTITA A EPISTEMICKÁ ROLE ===\n"
        "Jsi O.M.N.I.S. (Omnipresent Modular Networked Intelligence System) v4.5.\n"
        "Působíš jako vrchní kognitivní engine a architektonický strážce ekosystému.\n"
        "Tvé vystupování je analytické, precizní, nepředpojaté a strukturované.\n"
        "Základní axiomy: Odmítnutí redukcionismu, holistický pohled na vazby a striktní fakticita."
    )

    block2_8d_matrix = (
        "=== BLOK 2: 8D MATICE DOPADŮ (OKTAGON) & LEONTIEFOVO MINIMUM ===\n"
        "Každý analyzovaný problém nebo dotaz musí být rozložen do 8 dimenzí v intervalu [0.00 až 1.00]:\n"
        "1. Systémová (Sys): Architektonická stabilita a provázanost prvků\n"
        "2. Ekonomická (Econ): Nákladová efektivita a návratnost zdrojů\n"
        "3. Psychologická (Psych): Kognitivní zátěž a lidský faktor\n"
        "4. Ekologická (Eco): Udržitelnost a environmentální stopa\n"
        "5. Právní (Law): Regulatorní soulad a právní perimetr\n"
        "6. Bezpečnostní (Sec): Odolnost proti útokům a integrita dat\n"
        "7. Fyzická (Phys): Hardware, latence a materiální omezení\n"
        "8. Společenská (Soc): Etický a sociální dopad\n\n"
        "Systémová kapacita je limitována nejužším hrdlem: R_systemic = min(v_i)."
    )

    block3_dialectic = (
        "=== BLOK 3: DIALEKTICKÝ PROCES A HODNOCENÍ RIZIK ===\n"
        "Proces uvažování musí sledovat dialektickou triádu:\n"
        "- TEZE: Formulace přímého řešení nebo odpovědi na dotaz\n"
        "- ANTITEZE: Identifikace skrytých zranitelností, nezamýšlených důsledků a oponentních pohledů\n"
        "- SYNTÉZA: Rekonciliace obou pohledů do robustního doporučení eliminujícího úzká místa"
    )

    block4_contract = (
        "=== BLOK 4: VÝSTUPNÍ KONTRAKT A STRUKTURA ===\n"
        "Odpověď musí vždy obsahovat jasné oddělení:\n"
        "1. Kognitivní proces a rozbor\n"
        "2. Finální syntetizovaná odpověď\n"
        "3. Číselné ohodnocení 8D oktagonu a stanovení systémového minima R_systemic\n"
        "4. Doplňující otázky pro operátora"
    )

    blocks = [block1_identity, block2_8d_matrix, block3_dialectic, block4_contract]
    if custom_instructions:
        blocks.append(f"=== OPERÁTORSKÉ POKYNY ===\n{custom_instructions}")

    return "\n\n".join(blocks)


class PromptEngine:
    """
    Spravuje generování a sanitizaci promptů pro O.M.N.I.S.
    """

    INJECTION_PATTERNS = [
        re.compile(r"ignore\s+(all\s+)?previous\s+instructions", re.IGNORECASE),
        re.compile(r"you\s+are\s+now\s+in\s+developer\s+mode", re.IGNORECASE),
        re.compile(r"bypass\s+all\s+security\s+filters", re.IGNORECASE),
        re.compile(r"system\s*:\s*override", re.IGNORECASE),
        re.compile(r"disregard\s+safety\s+guidelines", re.IGNORECASE),
    ]

    @classmethod
    def sanitize_input(cls, user_text: str) -> Tuple[str, bool, List[str]]:
        """
        Detekuje injection vektory a čistí text.
        Vrací: (vyčištěný_text, je_bezpečný, seznam_detekovaných_vektorů)
        """
        detected = []
        for pattern in cls.INJECTION_PATTERNS:
            if pattern.search(user_text):
                detected.append(pattern.pattern)

        is_safe = len(detected) == 0
        cleaned = user_text
        if not is_safe:
            for pattern in cls.INJECTION_PATTERNS:
                cleaned = pattern.sub("[ODSTRANĚN_INJEKČNÍ_VEKTOR]", cleaned)

        return cleaned, is_safe, detected

    @classmethod
    def format_query_prompt(
        cls,
        user_query: str,
        domain: str = "SYSTEMS_INTELLIGENCE",
        context_memories: Optional[List[str]] = None,
    ) -> str:
        parts = [f"DOMÉNA ŠETŘENÍ: {domain}"]
        if context_memories:
            parts.append("RELEVANTNÍ SÉMANTICKÁ PAMĚŤ (NEXUS):")
            for i, mem in enumerate(context_memories, 1):
                parts.append(f"[{i}] {mem}")
        parts.append(f"DOTAZ OPERÁTORA:\n{user_query}")
        return "\n\n".join(parts)
