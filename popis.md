# O.M.N.I.S. (Agentic Knowledge Graph & Invariant Inference Engine — AKGE-8D) — Technická a Architektonická Analýza

Verze systému: 4.2 (Production Ready, Q1 2026)  
Cílová platforma: Android OS (API 26–36) | Jetpack Compose M3 | Room SQLite | Google Cloud SQL PostgreSQL | Python FastAPI CSR Graph  
Základní balíček: `com.example` | Runtime Application ID: `com.aistudio.omnis.quantum.v2026`

---

## 1. Globální Architektura Systému

Aplikace O.M.N.I.S. je navržena podle principů **Clean Architecture** a **Unidirectional Data Flow (UDF)** s reaktivním stavem postaveným na Kotlin Coroutines a Kotlin `StateFlow`. Systém fyzicky a logicky odděluje klientské rozhraní pro běžné uživatele (`UserNavigationLayout`) od správcovského a diagnostického scaffoldingu (`AdminNavigationLayout`).

### Sekvenční Diagram Zpracování Dotazu (UDF Pipeline)

```mermaid
sequenceDiagram
    autonumber
    actor U jako Operátor / Uživatel
    participant UI jako Jetpack Compose UI
    participant VM jako OmnisViewModel
    participant GATE jako Refusal Ladder (G1-G6)
    participant TENZOR jako 8D Tensor Kernel
    participant GEMINI jako Gemini REST API / SSE
    participant DB jako Room DB (SQLite)
    participant CLOUD jako PostgreSQL Cloud SQL

    U->>UI: Zadání dotazu / podnětu
    UI->>VM: sendQuery(prompt)
    VM->>GATE: evaluatePrompt(rawPrompt)
    alt Brána zamítnuta (REFUSED)
        GATE-->>VM: RefusalVerdict (Karanténa / Diagnostika)
        VM-->>UI: Zobrazení doporučení na upřesnění
    else Brána schválena (PASSED)
        GATE-->>VM: VerifiedPrompt
        VM->>TENZOR: evaluateSemantic8dVector(text)
        TENZOR-->>VM: 8D Vector Scores
        VM->>GEMINI: synthesize(query, 8DContext)
        GEMINI-->>VM: Stream tokenů SSE + Odpověď
        VM->>DB: insertRecord(OmnisRecord)
        VM-->>UI: Aktualizace UIState (Reaktivní rendering)
        VM->>CLOUD: Asynchronní replication (Dual-Write)
    end
```

### Blokové ASCII Schéma Systémových Vrstev

```
┌────────────────────────────────────────────────────────────────────────┐
│                   PREZENTAČNÍ VRSTVA (Jetpack Compose M3)              │
│                                                                        │
│  ┌──────────────────────────────┐    ┌──────────────────────────────┐  │
│  │     UserNavigationLayout     │    │     AdminNavigationLayout    │  │
│  │  (Chat, Kokpit, Cíle, Nexus) │    │  (Hub, PromptLab, Audit, DB) │  │
│  └──────────────┬───────────────┘    └──────────────┬───────────────┘  │
│                 │                                   │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                    OmnisViewModel / UIStateFlows                       │
│    (aktivní vlákno, streaming, filtry, 8D matice, telemetrie)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│             KOGNITIVNÍ, BEZPEČNOSTNÍ A AUDITNÍ ENGINE                  │
│                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ GeminiApiService │  │ Refusal Ladder   │  │ Defense Tier Auditor │  │
│  │ (REST / SSE)     │  │ Gatekeeper (G1-G6│  │ (Bezpečnostní štít)  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ 8D Vector Engine │  │ Causal Simulator │  │ Dialectic Engine     │  │
│  │ (Multidoménovost)│  │ (Kontrafaktuály) │  │ (Teze/Antiteze/Synt) │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ CircuitBreaker   │  │ CSR Graph Engine │  │ ZK Audit Ledger      │  │
│  │ (Pojistka výpadku│  │ (Topologie CSR)  │  │ (Kryptografické ZK)  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│             DATOVÁ PERSISTENCE A SYNCHRONIZAČNÍ VRSTVA                 │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                  OmnisDatabase (Room SQLite v12)                 │  │
│  │  Tabulky: omnis_messages, omnis_memory_fragments, omnis_goals,   │  │
│  │           omnis_artifacts, omnis_telemetry                       │  │
│  └──────────────────────────────┬───────────────────────────────────┘  │
│                                 │ Asynchronní Sync Pipeline            │
│                                 ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │            CloudSqlSyncManager / PostgreSQL (pgvector)           │  │
│  │            Bezpečný vzdálený cloudový sklad & záloha             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Datový Model a Entity Relationship (ER) Diagram

```mermaid
erDiagram
    OmnisRecord {
        Long id PK
        String role "user / assistant"
        String content
        String threadId FK
        Float valSys
        Float valEcon
        Float valPsych
        Float valEco
        Float valLaw
        Float valSec
        Float valPhys
        Float valSoc
        Float compositeScore
        String defenseTier
        Boolean isSyncedToPostgres
        Long timestamp
    }

    MemoryFragment {
        Long id PK
        String title
        String summary
        Int sourceRecordCount
        String tags
        Long timestamp
    }

    OmnisGoal {
        Long id PK
        String title
        String description
        Float progress
        String status
        Long deadline
    }

    OmnisArtifact {
        Long id PK
        String title
        String artifactType
        String content
        Long timestamp
    }

    OmnisRecord ||--o{ MemoryFragment : "konsolidováno do"
    OmnisRecord ||--o{ OmnisArtifact : "generuje"
```

---

## 3. Moduly, Třídy a Funkční Návaznosti

### 3.1 Modul Kognitivního Jádra a AI Komunikace (`com.example.ai`, `com.example.api`)

1. **`GeminiApiService`**: Zajišťuje síťovou komunikaci s REST API modelů Google Gemini (`gemini-2.5-flash` / `gemini-2.5-pro`) a reaktivní proudové vysílání tokenů přes Server-Sent Events (SSE).
2. **`RefusalLadderGatekeeper`**: Provádí 6-úrovňovou verifikaci (G1 až G6) garantující nulovou halucinaci a ověření normativních standardů (ISO/IEC, EU AI Act, NIS2).
3. **`CircuitBreaker` (`com.example.defense`)**: Softwarový jistič chránící aplikaci před kaskádovým selháním při výpadku sítě nebo vyčerpání kvóty API.

### 3.2 Modul 8D Vektorové Matice a Analytiky (`com.example.data`, `com.example.ui.octagon`)
Systém analyzuje každý podnět v osmirozměrném vektorovém prostoru `[SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC]` s výpočtem 28-párového tenzoru vzájemných vazeb.

---

## 4. Prezentační Úrovně a RBAC
- **`STANDARD_USER`**: Skryté technické logy a tenzory, čistý uživatelský chat, srozumitelná lidská řeč.
- **`ADMIN_OPERATOR`**: Surové 8D vektory, CSR topologie, telemetrie RAM/latencí, ZK-SNARK kryptografický otisk a oprávnění `MODIFY_SYSTEM_STATE`.
