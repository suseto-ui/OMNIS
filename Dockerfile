# ==========================================================
# O.M.N.I.S. Production Container (Full-Stack)
# Builds Vite Frontend + FastAPI Backend for Google Cloud Run
# ==========================================================

# Stage 1: Build Frontend Assets
FROM node:20-slim AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend ./
RUN npm run build:prod

# Stage 2: Build Python Backend Dependencies
FROM python:3.11-slim AS backend-builder
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    gcc \
    curl \
    && rm -rf /var/lib/apt/lists/*
COPY backend/requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Stage 3: Minimal Runtime Image
FROM python:3.11-slim AS runner
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy backend application and compiled frontend static assets
COPY backend /app/backend
COPY --from=frontend-builder /app/frontend/dist /app/frontend/dist

ENV PORT=8080
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8080

CMD exec uvicorn backend.main:app --host 0.0.0.0 --port ${PORT} --workers 2 --loop uvloop
