# Implementační Plán: Migrace O.M.N.I.S. na AKGE-8D rámec

Tento plán popisuje radikální posílení integrity systému O.M.N.I.S. prostřednictvím architektury AKGE-8D a Middle-Tier API Interceptoru.

---

## 1. Architektonické pilíře migrace

### A. Middle-Tier API Interceptor (Jádro)
*   **PromptGateway & PromptOptimizer:** Implementace jako čistě asynchronní mezivrstvy.
*   **G4 Validační logika:** Integrace Pydantic v2 wrapperů vynucujících shodu s etalonem **[EU AI Act, ISO/IEC 42001, NIS2]**.
*   **G6 Engine:** Výpočet Shannonovy entropie přímo v rámci interceptoru pro detekci halucinací.

### B. Datová Persistence & CSR Graf
*   **OmnisCorrelationEngine.kt:** Rozšíření o CSR graf (Causal Structural Representation).
*   **PostgreSQL/PgVector:** Optimalizace pro bleskové vyhledávání kauzálních cest pomocí HNSW indexu a RRF (Reciprocal Rank Fusion).
*   **Perkolační model:** Autonomní záchranné bypassy při překročení $p_c \approx 0.38$.

### C. Prezentační Vrstva & RBAC
*   **STANDARD_USER:** Vizualizace pouze sumárního stavu (OK/Refusal) a kognitivních mikro-odznaků (např. "AKGE-8D: 94% UKOTVENO").
*   **ADMIN_OPERATOR:** Přístup k detailní diagnostice bran (Gate Status) v `OctagonDashboard.kt`.

---

## 2. Harmonogram Implementace

| Fáze | Modul | Cíl |
| :--- | :--- | :--- |
| **I** | **PromptGateway** | Implementace Middle-Tier Interceptoru s validací norem (EU AI Act, ISO, NIS2). |
| **II** | **CognitiveEngine** | Rozšíření o CSR graf a integrace s PostgreSQL pgvector HNSW indexem. |
| **III** | **RBAC UI** | Implementace vizuálních mikro-odznaků a restrikce diagnostiky pro ADMIN_OPERATOR. |
| **IV** | **Monitoring** | Kalibrace perkolačního modelu a záchranných bypassů. |

---

## 3. Technické poznámky
* **G4 Quotable Evidence:** Validační wrapper bude dynamicky přepínat mezi normami (EU AI Act, ISO/IEC 42001, NIS2) dle kontextu požadavku a prioritní klasifikace rizika.
* **Čistota rozhraní:** Žádný vizuální clutter pro standardní uživatele; veškerá složitost zůstává zapouzdřena v API interceptoru.
