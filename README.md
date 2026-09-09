# O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis)

Kompletní produkční kognitivní architektura a systémový dashboard pro hodnocení systémových invariantů pomocí **čtyřdimenzionální Matice dopadů**.

---

## 🏛️ Architektura Systému

Aplikace je dekomponována do 4 provázaných vrstev:

1. **Databázová vrstva (PostgreSQL + pgvector):**
   - Asynchronní SQLAlchemy 2.0 ORM modely (`Conversation`, `Message`, `VectorMemory`, `ImpactMatrixMetric`).
   - Podpora `pgvector` pro sémantické ukládání a vyhledávání 768-dimenzionálních embeddingů (kosinová vzdálenost `<=>`).

2. **Backend (Python 3.11+ / FastAPI):**
   - Asynchronní REST API (`/api/query`, `/api/memory`, `/api/feedback`, `/api/conversations`).
   - Pydantic v2 validace a strukturovaný ontologický výstup.
   - Oficiální Google GenAI SDK s rate limit ochranou přes `asyncio.Semaphore(10)` a chybovým ošetřením.
   - Background tasks pro asynchronní vektorizaci a persistenci metrik.

3. **Frontend (React 18+ / Tailwind CSS / Lucide Icons):**
   - Tmavý reaktivní cyberpunkový dashboard.
   - Chatovací okno s real-time introspekcí kognitivních procesů a reflexivními otázkami.
   - Interaktivní vizualizace Matice dopadů (čtyři dimenze: ekonomická životaschopnost, ekologicko-sociální regenerace, technologická elegance, psychologická přijatelnost).
   - Autopoietická zpětná vazba a úprava vah.

4. **Infrastruktura & Deployment:**
   - Produkční multi-stage `Dockerfile` pro Google Cloud Run.
   - `docker-compose.yml` pro lokální orchestraci (Postgres s pgvector, FastAPI, React).
   - `cloudbuild.yaml` pro CI/CD pipeline do Google Cloud Run.

---

## 🚀 Rychlé Spuštění (Lokální Vývoj)

### Požadavky
- Docker a Docker Compose
- API klíč pro Gemini (v souboru `.env`)

```bash
# 1. Klonování a příprava prostředí
cp .env.example .env

# 2. Spuštění kompletního stacku (Postgres + pgvector, Backend, Frontend)
docker-compose up --build
```

- **Frontend Dashboard:** http://localhost:3000
- **FastAPI Swagger Dokumentace:** http://localhost:8000/docs
- **PostgreSQL pgvector:** localhost:5432

---

## ☁️ Deploy na Google Cloud Run

Jednorázové nasazení přes Google Cloud Build:

```bash
gcloud builds submit --config=cloudbuild.yaml
```
