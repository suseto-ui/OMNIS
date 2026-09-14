import os
import pytest
import logging
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

# Set dummy DATABASE_URL environment variable before importing main to prevent bootstrap failures
os.environ["OMNIS_XOR_KEY"] = "test-xor-key-for-unit-tests"
os.environ["DATABASE_URL"] = "postgresql+asyncpg://mock_user:mock_pass@localhost:5432/mock_db"

from backend.main import app

def test_aggregated_routers_are_mounted():
    """
    Verify that all aggregated architectural blueprint routers 
    are correctly initialized and registered onto the FastAPI application instance.
    """
    # Collect all registered route path endpoints from the live application instance
    routes = [route.path for route in app.routes]
    
    # Expected endpoints checking prefixes and path parameters matching the decoupled routers
    expected_paths = [
        "/healthz",
        "/api/health-check",
        "/api/query",
        "/api/omnis/synthesize-5phases",
        "/api/feedback",
        "/api/memory",
        "/api/conversations",
        "/api/epistemic/ingest",
        "/omnis/phase-4/evaluate-matrix",
        "/api/dev/token-telemetry",
        "/api/dev/estimate-tokens",
        "/api/dev/reset-tokens",
    ]
    
    for path in expected_paths:
        assert path in routes, f"Critical Architecture Blueprint Failure: Path '{path}' is not mounted."

def test_system_router_health():
    """
    Perform a live end-to-end integration check against the system health probe router.
    """
    with TestClient(app) as client:
        response = client.get("/healthz")
        assert response.status_code == 200
        assert response.json() == {"status": "healthy", "service": "O.M.N.I.S. Cognitive Architecture"}
        
        api_response = client.get("/api/health-check")
        assert api_response.status_code == 200

def test_gemini_api_outage_zero_simulation_fallback():
    """
    Integration test verifying the Zero-Simulation Policy guardrail.
    When Gemini API is unavailable or unconfigured, the system must not hallucinate
    or fail with 500, but return a clean, structured diagnostic fallback with 0.0 scores.
    """
    # Force the cognitive service client to be None, simulating an API outage or missing credentials
    with patch("backend.gemini_service.cognitive_service._client", None):
        with TestClient(app) as client:
            payload = {
                "query": "Kritický test integrity systému při výpadku LLM",
                "ontology_domain": "CYBERNETICS",
                "enable_thinking": True
            }
            response = client.post("/api/query", json=payload)
            
            assert response.status_code == 200
            data = response.json()
            assert "Zero-Simulation Policy" in data["answer"]
            assert "⚠️ **Upozornění:**" in data["answer"]
            assert data["impact_matrix"]["composite_score"] == 0.0
            assert "Falešná simulace je zakázána." in data["impact_matrix"]["reasoning"]

def test_spa_fallback_routing_logic():
    """
    Verify that non-API routes are handled by the SPA wildcard,
    while valid API routes retain priority.
    """
    with TestClient(app) as client:
        # 1. Existující API route musí fungovat normálně (priorita)
        health_resp = client.get("/healthz")
        assert health_resp.status_code == 200
        
        # 2. Náhodná cesta by měla být zachycena SPA handlerem.
        # Poznámka: Pokud frontend/dist neexistuje v testovacím prostředí, 
        # serve_spa v main.py nemusí být registrována. Testujeme přítomnost v routes.
        spa_route_exists = any(
            route.path == "/{full_path:path}" and "serve_spa" in str(route.endpoint)
            for route in app.routes
        )
        # V CI/CD prostředí bez buildu frontendu může být False, 
        # ale architektura vyžaduje registraci, pokud dist existuje.
        if os.path.exists(os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "dist"))):
            assert spa_route_exists, "SPA Fallback route is missing despite frontend/dist existence."

def test_secret_masking_integrity():
    """
    Verify that SecretMasker correctly redacts sensitive keys from log records.
    """
    from backend.gemini_service import SecretMasker
    
    sensitive_key = "x-gemini-key-12345-super-secret"
    masker = SecretMasker([sensitive_key])
    
    # Vytvoření falešného log záznamu obsahujícího klíč
    record = logging.LogRecord(
        name="omnis.test",
        level=logging.ERROR,
        pathname="test_routers.py",
        lineno=10,
        msg=f"Failed to connect using key {sensitive_key} to provider.",
        args=(),
        exc_info=None
    )
    
    # Aplikace filtru
    masker.filter(record)
    
    assert sensitive_key not in record.msg, "Sensitive key was leaked into the log message!"
    assert "[REDACTED_SECRET]" in record.msg, "SecretMasker failed to apply the redaction placeholder."