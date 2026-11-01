# O.M.N.I.S. - Dokončení nasazení do plně samostatného provozu na Google Cloud

Implementační plán pro dokončení samostatného produkčního provozu ekosystému O.M.N.I.S. na Google Cloud Platform (GCP) s využitím bezserverové infrastruktury Cloud Run, Cloud SQL PostgreSQL databáze, Google Cloud Secret Manageru a automatické synchronizace mobilní aplikace Android.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Potvrzená rozhodnutí uživatelem v Fázi 1:**
> 1. **Databázová persistence:** **Cloud SQL PostgreSQL s automatickým zálohováním** (přímá podpora `asyncpg` a `pgvector` rozšíření pro kognitivní paměť).
> 2. **Správa tajných klíčů:** **Google Cloud Secret Manager** (bezpečné načítání JWT klíčů, Gemini API klíčů a databázových přístupů bez rizika úniku).
> 3. **Škálování a rozpočet:** **Free Tier optimalizovaný režim** (`min-instances = 0`, `max-instances = 3`, 512 MiB RAM, 1 vCPU) s nulovými náklady v nečinnosti (pod $5/měsíc).

---

## 1. Overview & Core Concept

Cílem je převést systém O.M.N.I.S. do stavu 100% autoremoceného a samostatně schopného produkčního provozu na Google Cloud Platform:
- **FastAPI Backend na Cloud Run:** Zabezpečený REST & gRPC Gateway s automatickou rotací klíčů, zpracováním 8D dialectiky a kognitivního enginu.
- **Cloud SQL PostgreSQL:** Trvalé úložistě uživatelských profilů, konverzačních vláken, zpráv a Impact Matrix vektorů.
- **Secret Manager & IAM:** Striktní správa přístupových práv přes samostatný Service Account `omnis-cloud-run@${PROJECT_ID}.iam.gserviceaccount.com`.
- **Android Client Sync:** Mobilní klient (Kotlin / Jetpack Compose) plynule komunikuje s Cloud Run API, zobrazuje stav konektivity a podporuje offline režim.

---

## 2. User Experience & Visual Design

### Klíčové uživatelské toky (User Flows)
1. **Plynulý start aplikace:**
   - Android aplikace otestuje spojení s Cloud Run `/api/v1/health`.
   - Pokud je Cloud Run aktivní, zobrazí se zelený stavový odznak `☁️ ONLINE`. Pokud je zařízení bez internetu nebo Cloud Run spí, odznak indikuje `📡 OFFLINE` a aplikace funguje z lokální SQLite mezipaměti.
2. **Přihlášení a správa profilu:**
   - Uživatel se přihlásí přes Google Sign-In nebo jméno/heslo.
   - Profilové údaje, avatar a preference jsou synchronizovány s Cloud SQL PostgreSQL.
3. **Persistentní kognitivní relace:**
   - Každé konverzační vlákno a odpověď 8D enginu jsou atomicky ukládány do databáze v cloudu s podporou pozdějšího vyhledávání.

---

## 3. Key Product Decisions & Trade-Offs

### Rozhodnutí 1: Cloud SQL PostgreSQL + Asyncpg Connection Pooling
*   *Zvolený přístup:* Integrovaná konektivita přes Cloud SQL Auth Proxy nebo přímé Unix/TCP socket spojení v SQLAlchemy s `asyncpg` a `pool_pre_ping=True`.
*   *Proč:* Zabezpečuje ochranu před výpadky spojení při škálování Cloud Run na nulu a udržuje rychlé reakční doby.

### Rozhodnutí 2: Hybridní Secret Manager Client
*   *Zvolený přístup:* Použití `backend/gcp_secrets.py` s in-memory cachingem (TTL 300 s).
*   *Proč:* Zabraňuje nadbytečnému zpoplatněnému volání Secret Manager API při každém HTTP dotazu.

---

## 4. Technical Architecture & Data Strategy

### Diagram produkční architektury GCP

```
┌────────────────────────────────────────────────────────────────────────┐
│                   GCP Production Architecture                           │
│                                                                        │
│  ┌───────────────────────┐          ┌──────────────────────────────┐   │
│  │ Android App (Client)  │ ───────> │ Cloud Run (FastAPI Backend)  │   │
│  │ (Kotlin / Compose)    │  HTTPS   │ - 512 MiB RAM / 1 vCPU       │   │
│  └───────────────────────┘          │ - min: 0 / max: 3 instances  │   │
│                                     └──────────────┬───────────────┘   │
│                                                    │                   │
│                     ┌──────────────────────────────┼───────────────┐   │
│                     │                              │               │   │
│                     ▼                              ▼               │   │
│      ┌────────────────────────────┐  ┌───────────────────────────┐ │   │
│      │ Cloud SQL PostgreSQL       │  │ GCP Secret Manager        │ │   │
│      │ - User profiles & threads  │  │ - JWT Secret              │ │   │
│      │ - 8D Impact Matrix metrics │  │ - Gemini API Keys         │ │   │
│      └────────────────────────────┘  └───────────────────────────┘ │   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Akční plán kroků k dokončení nasazení

1. **Krok 1: Kontrola a aktualizace Terraform / GCP skriptů:**
   - Ověřit nastavení `main.tf`, `iam.tf` a `variables.tf` pro Cloud Run a Secret Manager.
   - Spustit a ověřit `scripts/setup_gcp_project.sh`.
2. **Krok 2: Ověření databázové vrstvy backendu:**
   - Zkontrolovat `backend/database.py` a `backend/gcp_secrets.py` pro bezchybnou inicializaci v cloudu.
3. **Krok 3: Finální sestavení a test klientské kompilace:**
   - Spustit `compile_applet` a provést synchronizaci do Git repozitáře `suseto-ui/OMNIS`.
