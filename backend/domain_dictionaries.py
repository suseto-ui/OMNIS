"""
O.M.N.I.S. Domain Contextual Dictionaries & Butterfly Effect Expansion Module
Manages domain-specific terminology, 28-pair correlation tensor weights, and butterfly effect ripple graphs.
"""

from __future__ import annotations
import re
from typing import Dict, List, Any, Tuple

# Definice 8 kognitivních dimenzí
DOMAINS_8D = ["SYS", "ECON", "PSYCH", "ECO", "LAW", "SEC", "PHYS", "SOC"]

# Přednastavené profily oborových slovníků
DOMAIN_PROFILES: Dict[str, Dict[str, Any]] = {
    "FINTECH": {
        "name": "FinTech & Algoritmická Ekonomie",
        "keywords": ["fintech", "bank", "krypto", "platby", "investice", "likvidita", "transakce", "blockchain", "swap", "defi"],
        "primary_pair": ("ECON", "LAW"),
        "pair_weights": {
            ("ECON", "LAW"): 0.98,
            ("ECON", "SEC"): 0.95,
            ("SEC", "SYS"): 0.92,
            ("LAW", "SOC"): 0.88,
            ("SYS", "PHYS"): 0.70,
            ("ECO", "PHYS"): 0.50,
        },
        "butterfly_vectors": [
            "Mikrosekundová latence v obchodní síti vyvolá arbitrární kolizi v garanci likvidity.",
            "Změna v regulaci MiCA (LAW) okamžitě změní cenotvorbu kapitálu v tokenové ekonomice (ECON)."
        ]
    },
    "INDUSTRY_40": {
        "name": "Průmyslová Výroba & Průmysl 4.0",
        "keywords": ["průmysl", "výroba", "továrna", "iot", "automatizace", "robotika", "stroj", "dodavatelský", "hardware", "materiál"],
        "primary_pair": ("PHYS", "ECO"),
        "pair_weights": {
            ("PHYS", "ECO"): 0.96,
            ("PHYS", "SYS"): 0.94,
            ("SYS", "SEC"): 0.91,
            ("ECON", "PHYS"): 0.89,
            ("PSYCH", "SOC"): 0.75,
            ("LAW", "ECO"): 0.85,
        },
        "butterfly_vectors": [
            "Mikroskopické opotřebení ložiska (PHYS) vyvolá tepelnou špičku a zastavení celého dodavatelského řetězce (ECON/SYS).",
            "Mírný nárůst emise CO2 na výrobní lince (ECO) spustí automatickou pokutu od regulatorního orgánu (LAW)."
        ]
    },
    "CYBERSEC": {
        "name": "Zero-Trust Kybernetická Bezpečnost",
        "keywords": ["bezpečnost", "security", "zero-trust", "kryptografie", "xss", "injection", "firewall", "zk-proof", "certifikát", "vulnerab"],
        "primary_pair": ("SEC", "SYS"),
        "pair_weights": {
            ("SEC", "SYS"): 0.99,
            ("SEC", "LAW"): 0.95,
            ("SEC", "PSYCH"): 0.90,
            ("SYS", "PHYS"): 0.88,
            ("ECON", "SEC"): 0.85,
        },
        "butterfly_vectors": [
            "Zranitelnost typu zero-day v knihovně nižší úrovně (SEC) umožní převzetí kontrolního uzlu cloudové infrastruktury (SYS).",
            "Sociální inženýrství cílené na operátora (PSYCH) obajde dvoufázovou autentizaci v kognitivním rozhraní (SEC)."
        ]
    },
    "HEALTHTECH": {
        "name": "HealthTech & Biomedicínské Systémy",
        "keywords": ["zdraví", "pacient", "medicína", "diagnostika", "biotech", "farmakologie", "klinické", "genom"],
        "primary_pair": ("PHYS", "PSYCH"),
        "pair_weights": {
            ("PHYS", "PSYCH"): 0.97,
            ("LAW", "SEC"): 0.96,
            ("PHYS", "ECO"): 0.90,
            ("SOC", "PSYCH"): 0.89,
        },
        "butterfly_vectors": [
            "Drobná odchylka v kalibraci diagnostického snímače (PHYS) způsobí chybnou kategorizaci rizika (PSYCH/LAW)."
        ]
    },
    "GENERAL_SYSTEMS": {
        "name": "Všeobecné Systémové Inženýrství",
        "keywords": [],
        "primary_pair": ("SYS", "SEC"),
        "pair_weights": {
            ("SYS", "SEC"): 0.90,
            ("ECON", "SYS"): 0.85,
            ("LAW", "SYS"): 0.80,
            ("PHYS", "ECO"): 0.75,
            ("PSYCH", "SOC"): 0.75,
        },
        "butterfly_vectors": [
            "Systémový drift v reálném čase vyvolá kaskádový rozpad rovnovážného stavu."
        ]
    }
}


class DomainDictionaryEngine:
    """
    Služba pro autonomní detekci oboru, extrakci kontextuálních slovníků
    a výpočet 28-párových tenzorových vah pro CsrKnowledgeGraph.
    """

    @classmethod
    def detect_domain_profile(cls, query_text: str) -> Dict[str, Any]:
        """
        Vyhodnotí sémantickou hustotu dotazu a autonomně určí oborový profil.
        """
        text_lower = query_text.lower()
        matched_scores: Dict[str, int] = {}

        for profile_key, profile in DOMAIN_PROFILES.items():
            if profile_key == "GENERAL_SYSTEMS":
                continue
            matches = sum(1 for kw in profile["keywords"] if kw in text_lower)
            if matches > 0:
                matched_scores[profile_key] = matches

        if not matched_scores:
            selected_key = "GENERAL_SYSTEMS"
        else:
            selected_key = max(matched_scores, key=matched_scores.get)

        selected_profile = DOMAIN_PROFILES[selected_key].copy()
        
        # Motýlí efekt - rozšíření o navazující multidisiplinární vektory
        selected_profile["butterfly_expansion"] = cls.compute_butterfly_expansion(text_lower, selected_key)
        return selected_profile

    @classmethod
    def compute_butterfly_expansion(cls, text_lower: str, profile_key: str) -> List[Dict[str, Any]]:
        """
        Generuje 'Motýlí efekt' (Butterfly Effect Ripple) - odhaluje nenápadné
        sekundární a terciární mikro-změny v síti.
        """
        expansions = []
        if profile_key == "FINTECH":
            expansions.append({
                "origin": "Posun poplatkové struktury o 0.01%",
                "ripple_t2": "Změna optimální strategie v algoritmickém vytěžování likvidity (ECON)",
                "ripple_t3": "Zvýšení systémového tlaku na regulatorní výkaznictví v reálném čase (LAW/SEC)"
            })
        elif profile_key == "INDUSTRY_40":
            expansions.append({
                "origin": "Zvýšení teploty rozvaděče o 2.5°C",
                "ripple_t2": "Exponenciální nárůst pravděpodobnosti chybovosti na montážní lince (PHYS/SYS)",
                "ripple_t3": "Zpoždění finální expedice a smluvní penále v logistickém řetězci (ECON/LAW)"
            })
        else:
            expansions.append({
                "origin": "Sémantická odchylka v operátorské instrukci",
                "ripple_t2": "Zvýšení entropického šumu v asynchronním pracovním uzlu (SYS)",
                "ripple_t3": "Narušení rovnovážného stavu perkolace v celém clusteru (SEC/SOC)"
            })

        return expansions

    @classmethod
    def build_csr_tensor_weights(cls, profile_key: str) -> Dict[Tuple[str, str], float]:
        """
        Vypočte přenaštěné váhy pro 28-párový tenzor 8D domén.
        """
        profile = DOMAIN_PROFILES.get(profile_key, DOMAIN_PROFILES["GENERAL_SYSTEMS"])
        pair_weights = profile.get("pair_weights", {})

        # Základní defaultní matice 28 párových vazeb
        tensor_28: Dict[Tuple[str, str], float] = {}
        for i, d1 in enumerate(DOMAINS_8D):
            for j, d2 in enumerate(DOMAINS_8D):
                if i < j:
                    tensor_28[(d1, d2)] = 0.50

        # Přepsání vah dle oborového profilu
        for (d1, d2), w in pair_weights.items():
            if (d1, d2) in tensor_28:
                tensor_28[(d1, d2)] = w
            elif (d2, d1) in tensor_28:
                tensor_28[(d2, d1)] = w

        return tensor_28
