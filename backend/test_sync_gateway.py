"""
Unit a integrační testy pro FastAPI REST Sync Gateway vrstvu O.M.N.I.S.
Testuje health ping, zprávy, paměťové fragmenty a 8D telemetrii.
"""

import os
import pytest
from fastapi.testclient import TestClient

os.environ["OMNIS_XOR_KEY"] = "test-xor-key-for-unit-tests"
os.environ["DATABASE_URL"] = "postgresql+asyncpg://mock_user:mock_pass@localhost:5432/mock_db"

from backend.main import app


def test_sync_gateway_health_ping():
    """
    Ověří, že FastAPI endpointy /api/v1/health a /sync/health/ping odpovídají
    a vrací diagnostiku connection poolu.
    """
    with TestClient(app) as client:
        # 1. Health check v1
        response = client.get("/api/v1/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "database" in data
        assert data["service"] == "O.M.N.I.S. Core REST Gateway"
        assert data["version"] == "4.5"
        assert data["connection_pool"]["engine"] == "SQLAlchemy AsyncEngine (asyncpg)"

        # 2. Sync Ping
        ping_response = client.get("/sync/health/ping")
        assert ping_response.status_code == 200
        ping_data = ping_response.json()
        assert ping_data["status"] in ["HEALTHY", "DEGRADED"]


def test_sync_messages_batch():
    """
    Ověří příjem dávky zpráv přes /api/v1/sync/messages i /sync/records/batch.
    """
    payload = {
        "messages": [
            {
                "id": 101,
                "role": "user",
                "content": "Testovací kognitivní zpráva pro replikaci",
                "cognitive_process": "QUERY",
                "follow_up_questions": "",
                "val_sys": 0.85,
                "val_econ": 0.70,
                "val_psych": 0.90,
                "val_eco": 0.65,
                "val_law": 0.80,
                "val_sec": 0.95,
                "val_phys": 0.50,
                "val_soc": 0.75,
                "composite_score": 0.76,
                "domain": "SYSTEMS_INTELLIGENCE",
                "timestamp": 1700000000000,
                "defense_tier": "APPROVED",
                "thread_id": "thread_main",
                "thread_title": "Hlavní vlákno",
                "user_name": "operator"
            }
        ],
        "client_version": "4.5"
    }

    with TestClient(app) as client:
        headers = {"Authorization": "Bearer omnis-internal-gateway-token-2026"}
        response = client.post("/api/v1/sync/messages", json=payload, headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "SUCCESS"
        assert data["synced_count"] == 1

        # Ověření alternativního endpointu /sync/records/batch
        alt_response = client.post("/sync/records/batch", json=payload, headers=headers)
        assert alt_response.status_code == 200
        assert alt_response.json()["synced_count"] == 1


def test_sync_memory_fragments_batch():
    """
    Ověří příjem paměťových fragmentů přes /api/v1/sync/memory i /sync/fragments/batch.
    """
    payload = {
        "fragments": [
            {
                "id": 201,
                "title": "Architektonický Fragment",
                "summary": "Důležitý koncept kognitivní fúze O.M.N.I.S.",
                "domain": "SYSTEMS_INTELLIGENCE",
                "val_sys": 0.9,
                "val_econ": 0.6,
                "val_psych": 0.8,
                "val_eco": 0.7,
                "val_law": 0.9,
                "val_sec": 0.95,
                "val_phys": 0.8,
                "val_soc": 0.85,
                "importance_score": 1.0,
                "timestamp": 1700000000000,
                "user_name": "operator"
            }
        ],
        "client_version": "4.5"
    }

    with TestClient(app) as client:
        headers = {"X-OMNIS-API-KEY": "omnis-internal-gateway-token-2026"}
        response = client.post("/api/v1/sync/memory", json=payload, headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "SUCCESS"
        assert data["synced_count"] == 1

        # Ověření alternativního endpointu /sync/fragments/batch
        alt_response = client.post("/sync/fragments/batch", json=payload, headers=headers)
        assert alt_response.status_code == 200
        assert alt_response.json()["synced_count"] == 1


def test_sync_telemetry_batch():
    """
    Ověří příjem telemetrických záznamů a 8D metrik přes /api/v1/sync/telemetry.
    """
    payload = {
        "logs": [
            {
                "level": "INFO",
                "tag": "HARDWARE_TELEMETRY",
                "message": "RAM Pressure: 0.42 | Thermal: NORMAL | SQLite I/O: 12ms",
                "metadata_json": '{"ram_pressure": 0.42, "battery_temp": 32.5}',
                "timestamp": 1700000000000
            }
        ],
        "client_version": "4.5"
    }

    with TestClient(app) as client:
        headers = {"Authorization": "Bearer omnis-internal-gateway-token-2026"}
        response = client.post("/api/v1/sync/telemetry", json=payload, headers=headers)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "SUCCESS"
        assert data["synced_count"] == 1


def test_sync_unauthorized_request():
    """
    Ověří, že požadavek bez autorizační hlavičky je odmítnut kódem 401.
    """
    with TestClient(app) as client:
        response = client.post("/api/v1/sync/messages", json={"messages": []})
        assert response.status_code == 401
