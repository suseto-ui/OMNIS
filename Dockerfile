# --- Fáze 0: Kompilace React frontendu (Vite / Dashboard) ---
FROM node:20-alpine AS frontend-builder
WORKDIR /build
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# --- Fáze 1: Základní obraz se všemi produkčními závislostmi ---
FROM python:3.12-slim AS base

WORKDIR /app

# Instalace systémových závislostí pro PostgreSQL klienta
RUN apt-get update && apt-get install -y libpq-dev gcc && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
# Přenesení produkčních assetů frontendu do struktury pro FastAPI servírování
COPY --from=frontend-builder /build/dist ./frontend/dist

# --- Fáze 2: Testovací vrstva (Spuštění integračních a unit testů) ---
FROM base AS tester
RUN pip install --no-cache-dir pytest httpx
ENV PYTHONPATH=/app
RUN pytest backend/test_routers.py backend/test_omnis_phases.py -v
# Vytvoření markeru úspěchu – vynutí spuštění této fáze v BuildKitu
RUN touch /app/.tests_passed

# --- Fáze 3: Čistý produkční runtime obraz ---
FROM base AS final
# Vynucení závislosti na předchozí testovací fázi
COPY --from=tester /app/.tests_passed /app/.tests_passed
EXPOSE 8000
CMD ["uvicorn", "backend.main:app", "--host", "0.0.0.0", "--port", "8000"]