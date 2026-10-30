import json
import os
import urllib.request
import urllib.error
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from backend.database import get_db

system_router = APIRouter(tags=["System"])

@system_router.get("/healthz")
@system_router.get("/api/health-check")
async def health_check() -> dict[str, str]:
    """Health check endpoint for Cloud Run and orchestrator probes."""
    return {"status": "healthy", "service": "O.M.N.I.S. Cognitive Architecture"}

@system_router.get("/api/system/db-check")
async def db_check(db: AsyncSession = Depends(get_db)) -> dict[str, str]:
    """Checks the live connectivity status of the Google Cloud SQL instance."""
    try:
        from backend.database import engine
        if engine is None:
            return {"status": "offline", "reason": "No database URL configured"}
            
        await db.execute(text("SELECT 1"))
        return {"status": "online"}
    except Exception as e:
        return {"status": "offline", "reason": str(e)}

@system_router.get("/api/system/gemini-check")
async def gemini_check() -> dict:
    """
    Live diagnostics endpoint for verifying GEMINI_API_KEY validity,
    multi-key failover pool status, quota exhaustion (402/429), and model access.
    """
    try:
        from backend.gemini_service import key_pool
        pool_status = key_pool.get_pool_status()
        active_entry = key_pool.get_active_entry()
    except Exception:
        key_pool = None
        pool_status = []
        active_entry = None

    if not active_entry:
        return {
            "configured": False,
            "status": "MISSING_KEY",
            "message": "V Secrets panelu AI Studio nebyl nalezen žádný platný GEMINI_API_KEY.",
            "action": "Vložte GEMINI_API_KEY, GEMINI_API_KEY2 nebo GEMINI_API_KEY3 v Secrets panelu Google AI Studio."
        }

    api_key = active_entry.raw_key
    masked = active_entry.masked
    active_name = active_entry.name
    test_models = ["gemini-3.5-flash", "gemini-flash-latest", "gemma-4-26b-a4b-it"]
    last_err: Optional[Exception] = None
    last_err_body: str = ""
    last_model_tested = test_models[0]

    for model_name in test_models:
        last_model_tested = model_name
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"
        payload = json.dumps({"contents": [{"parts": [{"text": "ping"}]}]}).encode("utf-8")
        req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})

        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                if key_pool:
                    key_pool.mark_success(active_entry)
                return {
                    "configured": True,
                    "pool_size": len(pool_status),
                    "active_key": active_name,
                    "key_preview": masked,
                    "model_tested": model_name,
                    "status": "ACTIVE_HEALTHY",
                    "http_code": 200,
                    "message": f"Klíč {active_name} je platný a funkční. Spojení s modelem {model_name} bylo úspěšně ověřeno.",
                    "key_pool": key_pool.get_pool_status() if key_pool else pool_status
                }
        except urllib.error.HTTPError as err:
            last_err = err
            last_err_body = err.read().decode("utf-8", errors="ignore")
            # If 402 or 429, the entire key has quota/credit issues across all models
            if err.code in (402, 429):
                break
            # If 404 or 503, try next model in cascade
            continue
        except Exception as exc:
            last_err = exc
            continue

    if last_err and isinstance(last_err, urllib.error.HTTPError):
        body = last_err_body
        try:
            err_json = json.loads(body)
            api_message = err_json.get("error", {}).get("message", body)
            api_status = err_json.get("error", {}).get("status", str(last_err.code))
        except Exception:
            api_message = body
            api_status = str(last_err.code)

        if key_pool:
            key_pool.mark_exhausted(active_entry, api_message)
            pool_status = key_pool.get_pool_status()
            next_active = key_pool.get_active_entry()
        else:
            next_active = None

        if last_err.code == 402 or "depleted" in api_message.lower():
            return {
                "configured": True,
                "pool_size": len(pool_status),
                "active_key": active_name,
                "next_key": next_active.name if next_active else None,
                "key_preview": masked,
                "model_tested": last_model_tested,
                "status": "DEPLETED_PREPAYMENT_CREDITS",
                "http_code": 402,
                "google_status": api_status,
                "google_message": api_message,
                "explanation": f"Předplacený kredit pro {active_name} byl vyčerpán. Systém automaticky rotuje na další klíč v Secrets fondu.",
                "action": "Doplňte kredit na https://ai.studio/projects nebo nechte systém využívat záložní klíče (GEMINI_API_KEY2, GEMINI_API_KEY3).",
                "key_pool": pool_status
            }
        elif last_err.code == 429:
            return {
                "configured": True,
                "pool_size": len(pool_status),
                "active_key": active_name,
                "next_key": next_active.name if next_active else None,
                "key_preview": masked,
                "model_tested": last_model_tested,
                "status": "RATE_LIMIT_EXCEEDED",
                "http_code": 429,
                "google_status": api_status,
                "google_message": api_message,
                "explanation": f"Byla překročena minutová kvóta pro {active_name}. Systém rotuje na další klíč v Secrets fondu.",
                "action": "Počkejte 30-60 sekund nebo pokračujte s dalším klíčem.",
                "key_pool": pool_status
            }
        else:
            return {
                "configured": True,
                "pool_size": len(pool_status),
                "active_key": active_name,
                "key_preview": masked,
                "model_tested": last_model_tested,
                "status": "API_ERROR",
                "http_code": last_err.code,
                "google_status": api_status,
                "google_message": api_message,
                "key_pool": pool_status
            }

    return {
        "configured": True,
        "pool_size": len(pool_status),
        "active_key": active_name,
        "key_preview": masked,
        "status": "NETWORK_TIMEOUT",
        "message": f"Chyba při testu spojení: {str(last_err) if last_err else 'Neznámá chyba'}",
        "key_pool": pool_status
    }

