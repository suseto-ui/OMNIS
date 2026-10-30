# Implementační Plán: Fáze II - Kauzální CSR Graf v O.M.N.I.S.

Tento plán definuje integraci kauzální reprezentace (CSR) do kognitivního jádra systému.

---

## 1. Datové struktury (CSR Model)
*   **Uzly (Nodes):**
    *   `ConceptNode` (AKGE-8D domény)
    *   `StressEventNode` (Kauzální události)
    *   `ThreatNode` (Bezpečnostní hrozby)
    *   `BypassNode` (Technická řešení)
*   **Hrany (Edges):**
    *   `CausalEdge` (příčina-následek)
    *   `InfluenceEdge` (korelace)
    *   `MitigationEdge` (bypass -> threat)

## 2. PostgreSQL & HNSW Indexace
*   **Global HNSW Index:** Implementace indexu nad vektorovými poli uzlů v PostgreSQL pro bleskové vyhledávání souvisejících kauzálních cest.
*   **RRF (Reciprocal Rank Fusion):** Pro vážení relevance výsledků z různých kauzálních větví.

## 3. Implementační kroky
1.  **Definice modelů:** Rozšíření `OmnisCorrelationEngine` o datové třídy `CsrNode` a `CsrEdge`.
2.  **HNSW Repository:** Implementace dotazů pro prohledávání kauzálních cest v PostgreSQL.
3.  **Perkolační algoritmus:** Aktualizace modelu šíření stresu, který využívá novou CSR topologii pro detekci kritických bodů $p_c \approx 0.38$.
4.  **Integrace:** Propojení `OmnisCorrelationEngine` s novým CSR grafem.
