import os
import logging
from typing import Optional, Dict

logger = logging.getLogger("omnis.gcp_secrets")

_SECRET_CACHE: Dict[str, Optional[str]] = {}

def get_secret(secret_id: str, default: Optional[str] = None) -> Optional[str]:
    """
    Získá tajný klíč prioritně z:
    1. Vnitřní paměťové mezipaměti (_SECRET_CACHE)
    2. Proměnných prostředí (AI Studio Secrets panel / Cloud Run env)
    3. Google Cloud Secret Manageru s přísným 2.0s timeoutem
    4. Výchozí hodnoty (default)
    
    Garantuje okamžitý start kontejneru a ochranu před gRPC timeouty.
    """
    if secret_id in _SECRET_CACHE:
        val = _SECRET_CACHE[secret_id]
        return val if val is not None else default

    # 1. Kontrola v environment variables
    env_val = os.getenv(secret_id)
    if env_val and env_val.strip():
        _SECRET_CACHE[secret_id] = env_val.strip()
        return env_val.strip()

    # 2. Kontrola GCP Secret Manageru (přísný 2s timeout)
    project_id = os.getenv("GOOGLE_CLOUD_PROJECT") or os.getenv("PROJECT_ID")
    if project_id:
        try:
            from google.cloud import secretmanager
            client = secretmanager.SecretManagerServiceClient()
            name = f"projects/{project_id}/secrets/{secret_id}/versions/latest"
            response = client.access_secret_version(request={"name": name}, timeout=2.0)
            payload = response.payload.data.decode("UTF-8").strip()
            if payload:
                logger.info(f"Secret '{secret_id}' načten z Google Cloud Secret Manageru.")
                _SECRET_CACHE[secret_id] = payload
                return payload
        except Exception as e:
            logger.warning(f"Secret Manager nezískal '{secret_id}' (použije se fallback): {e}")

    # Uložit None pro zabránění opakovanému gRPC zpoždění
    _SECRET_CACHE[secret_id] = None
    return default
