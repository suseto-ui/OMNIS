# ==============================================================================
# O.M.N.I.S. - Production Multi-Stage Dockerfile for Google Cloud Run (Free Tier)
# Cílová velikost obrazu: < 120 MB | Spotřeba RAM při startu: < 65 MB | 0 USD v nečinnosti
# ==============================================================================

# --- FÁZE 1: Builder (Instalace závislostí do virtuálního prostředí) ---
FROM python:3.11-slim AS builder

WORKDIR /build

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Vytvoření izolovaného virtuálního prostředí
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Instalace pouze produkčních závislostí backendu
COPY backend/requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt && \
    find /opt/venv -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true && \
    find /opt/venv -type f -name "*.pyc" -delete

# --- FÁZE 2: Runner (Ultra-čistý produkční runtime bez zbytečných nástrojů) ---
FROM python:3.11-slim AS runner

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH" \
    PORT=8080

# Vytvoření bezpečného neprivilegovaného systémového uživatele
RUN addgroup --system --gid 10001 appuser && \
    adduser --system --uid 10001 --ingroup appuser --no-create-home appuser

# Přenést zkompilované virtuální prostředí z builderu
COPY --from=builder /opt/venv /opt/venv

# Přenést pouze zdrojový kód backendu O.M.N.I.S.
COPY backend/ ./backend/

# Nastavení vlastnictví pro neprivilegovaného uživatele na aplikaci i virtuální prostředí
RUN chown -R appuser:appuser /app /opt/venv && chmod -R a+rx /opt/venv

# Přepnutí na neprivilegovaného uživatele (Security Best Practice)
USER appuser

EXPOSE 8080

# Dynamické navázání na Cloud Run PORT proměnnou prostředí s 1 workerem (Free Tier optimalizace)
CMD exec gunicorn -k uvicorn.workers.UvicornWorker backend.main:app --bind 0.0.0.0:${PORT:-8080} --workers 1 --access-logfile -