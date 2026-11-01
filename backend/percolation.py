import logging

logger = logging.getLogger("omnis.telemetry")

def activate_emergency_stabilization_protocols(ratio: float):
    logger.critical(f"!! KRYTICKÉ RIZIKO KASKÁDY: Detekován perkolační poměr {ratio}. Aktivace stabilizačních protokolů.")

def eval_cascade_risk(domains_status: dict[str, str], critical_threshold: float = 0.38) -> str:
    """Algoritmus perkolačního protokolu (FÁZE 12.2)."""
    try:
        total_domains = len(domains_status)
        if total_domains == 0:
            return "SYSTEM_STABLE"
        failed_domains = sum(1 for status in domains_status.values() if status in ["FAILED", "STRESSED"])
        failing_ratio = failed_domains / total_domains
        
        if failing_ratio >= critical_threshold:
            activate_emergency_stabilization_protocols(failing_ratio)
            return "CRITICAL_CASCADE_RISK_ACTIVATED"
        return "SYSTEM_STABLE"
    except Exception as e:
        logger.error(f"Chyba při perkolační analýze: {e}")
        return "SYSTEM_STABLE"
