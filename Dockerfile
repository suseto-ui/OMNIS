# ==========================================================
# O.M.N.I.S. Production Backend Container
# Optimized for Google Cloud Run (Python 3.11-slim)
# ==========================================================

FROM python:3.11-slim AS builder

WORKDIR /app

# Install system dependencies required for compiling asyncpg and extensions
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    gcc \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY backend/requirements.txt .

RUN pip install --no-cache-dir --user -r requirements.txt

# Final minimal runtime image
FROM python:3.11-slim AS runner

WORKDIR /app

# Install runtime PostgreSQL client libraries
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Copy installed python wheels from builder
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy backend source code
COPY backend /app/backend

# Cloud Run defaults PORT to 8080
ENV PORT=8080
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8080

# Run uvicorn bound to Cloud Run PORT environment variable
CMD exec uvicorn backend.main:app --host 0.0.0.0 --port ${PORT} --workers 2 --loop uvloop
