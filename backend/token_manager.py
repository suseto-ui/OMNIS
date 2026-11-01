def calculate_token_budget(system_stress_sigma: float) -> dict[str, float | str]:
    """Algoritmus rebalancování tokenového toku (FÁZE 12.3)."""
    if system_stress_sigma < 0.2:
        return {
            "compression_rate": 0.70,
            "forensic_budget": "MINIMAL",
            "synthesis_depth": "STANDARD"
        }
    elif 0.2 <= system_stress_sigma < 0.6:
        return {
            "compression_rate": 0.30,
            "forensic_budget": "MODERATE",
            "synthesis_depth": "BALANCED"
        }
    else:
        return {
            "compression_rate": 0.0,
            "forensic_budget": "EXTENDED_FORENSIC",
            "synthesis_depth": "DEEP_ANALYTICS"
        }
