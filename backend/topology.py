import math

def calculate_euler_characteristic(v: int, e: int, f: int) -> int:
    """Eulerova charakteristika: chi = V - E + F"""
    return v - e + f

def calculate_betti_numbers(v: int, e: int, f: int) -> dict:
    """Výpočet Bettiho čísel pro kognitivní topologii."""
    # Beta0: Počet komponent (zjednodušený model)
    # Beta1: Počet nezávislých smyček v síti
    return {
        "beta0": 1,
        "beta1": e - v + 1  # Eulerův vzorec pro smyčky
    }

def calculate_entropy(probabilities: list[float]) -> float:
    """Systémová informační entropie S = -sum(p_i * ln(p_i))"""
    return -sum(p * math.log(p) for p in probabilities if p > 0)
