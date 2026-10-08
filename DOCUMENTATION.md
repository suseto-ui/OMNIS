# O.M.N.I.S. – KOMPLETNÍ OPERÁTORSKÝ, TECHNICKÝ A METODICKÝ MANUÁL (v4.2)

**O.M.N.I.S. (Agentic Knowledge Graph & Invariant Inference Engine — AKGE-8D)** je transdisciplinární kognitivní architektura a analytický ekosystém pro Android (Kotlin 2.0 / Jetpack Compose Material 3) integrovaný s Python FastAPI backendem, Compressed Sparse Row (CSR) znalostními grafy a 8D tenzorovou logikou pro nelineární modelování systémových dopadů, kauzální intervence, řízení systémových rizik a autonomní orchestraci v reálném čase.

---

## 📖 ČÁST I: UŽIVATELSKÝ A OPERÁTORSKÝ MANUÁL

### 1.1 Dvoudimenzionální Systém Řízení Rozhraní (Dimenze A & B)
Architektura rozhraní O.M.N.I.S. je řízena dvěma **ortogonálními (na sobě nezávislými) dimenzemi**:

| Dimenze | Typ Řízení | Hodnoty | Primární Účel |
| :--- | :--- | :--- | :--- |
| **Dimenze A** | **Bezpečnostní Role (RBAC)** | `STANDARD_USER` vs `ADMIN_OPERATOR` | Řízení přístupových práv k systémovým akcím, diagnostice infrastruktury a izolaci konverzací. |
| **Dimenze B** | **Kognitivní Úroveň Pokročilosti** | `BEGINNER` (`STANDARD`) vs `EXPERT` | Řízení hustoty informací, vizuální složitosti, formátu výstupů a dostupnosti pokročilých ovládacích prvků. |

> **Principy nezávislosti:** Bezpečnostní role a kognitivní úroveň jsou plně odděleny. Administrátor (`ADMIN_OPERATOR`) může pracovat v režimu `Začátečník` pro získání rychlého a přehledného manažerského shrnutí v přirozeném jazyce. Stejně tak běžný uživatel (`STANDARD_USER`) může přepnout do režimu `Expert` a sledovat 8D tenzorové matice či provádět kauzální simulace $do(X)$, aniž by tím získal přístup k administraci infrastruktury.

---

### 1.2 Dimenze A: Logika Bezpečnostních Rolí a Vliv na Frontend

#### A. Běžný Uživatel (`STANDARD_USER`):
1. **Frontendový Routing & Navigation Layout:**
   - Směrování probíhá přes izolovaný `UserNavigationLayout` a reaktivní `UserTabRouter`.
   - Vlastní horní lišta `UserTopAppBar` a postranní panel `UserDrawerContent` neobsahují žádné systémové odznaky, stavové jističe ani diagnostické indikátory.
2. **Přístupné Záložky v Bottom Navigation Bar (`OmnisBottomNavigationBar`):**
   - **Kognitivní Chat (`CHAT`):** Konverzační rozhraní s podporou multimodálního vstupu.
   - **Přehledový Kokpit (`DASHBOARD`):** Rychlé metriky, aktivní cíle a nedávné artefakty.
   - **8D Radar & Analytika (`ANALYTICS`):** Interaktivní radarový graf 8D stavového vektoru.
   - **Plánovač Scénářů (`SCENARIOS`):** Rozhodovací stromy a zátěžové testování.
   - **Sledování Cílů (`GOALS`):** Správa strategických milníků s perzistencí v databázi.
   - **Galerie Artefaktů (`ARTIFACTS`):** Prohlížení a export generovaných dokumentů.
   - **Paměťový Kontext (`MEMORY`):** Přehled konsolidovaných paměťových fragmentů.
   - **Kauzální Simulátor (`CAUSAL_SIMULATOR`):** Intervenční analýza Judea Pearl SCM.
   - **Dialektický Motor (`DIALECTICS`):** Sokratovská syntéza teze a antiteze.
   - **Neurální Nexus (`NEXUS`):** Multi-agentní debata 4 autonomních agentů.
   - **Metodologický Průvodce (`GUIDE`):** Interaktivní metodika a nápověda.
3. **Izolace Konverzačních Vláken (Multi-Tenant Isolation):**
   - Reaktivní filtrace v `OmnisViewModel` zajišťuje, že běžný uživatel vidí výhradně svá vlastní konverzační vlákna (`userName == currentUsername`).
4. **Restrikce Infrastruktury:**
   - Bezpečný návrat do chatu při pokusu o přístup k neautorizovaným záložkám.
   - Znemožněno spouštění systémových akcí (`canAccessSystemActions() == false`), úprava prahů sémantické brány, manuální spouštění Self-Healing procedur či čištění Cloud SQL databáze.

#### B. Administrátor / Operátor (`ADMIN_OPERATOR`):
1. **Frontendový Routing & Navigation Layout:**
   - Směrování probíhá přes vyhrazený `AdminNavigationLayout` a `AdminTabRouter`.
   - Horní lišta `AdminTopAppBar` a panel `AdminDrawerContent` zobrazují systémové stavové odznaky, stav jističe (Circuit Breaker) a možnost globálního filtrování uživatelů.
2. **Exkluzivní Administrátorské Moduly a Záložky:**
   - **Admin Hub (`ADMIN`):** Diagnostika API key poolu, ruční spuštění Self-Healing auditu, spuštění Cloud SQL dual-write synchronizace, odstraňování synchronizovaných lokálních dat.
   - **Kognitivní Uzly (`NODES`):** Vizualizace výpočetní sítě a topologie.
   - **Matrix / Octagon (`MATRIX`):** Tenzorové simulace, zamykání domén a profilové presety.
   - **Dev Prompt Lab (`DEV_PROMPT_LAB`):** Testování mutací promptů, sledování toku tokenů (`TokenFlowVisualizer`), LLM error diagnostika a dynamické nastavení citlivosti sémantické brány.
   - **Telemetrický Dashboard (`TELEMETRY`):** Živý stream fyzické telemetrie a systémových logů.
   - **Auditní Centrum (`PRODUCTION_AUDIT`):** Sledování dodržování EU AI Act, NIS2 a ISO/IEC 42001 normativů.
   - **ZK Audit Ledger (`ZK_LEDGER`):** Kryptografický otisk $\text{CommitmentHash}$ pro verifikaci výstupů.

---

## 🛡️ ČÁST II: REFUSAL LADDER GATEKEEPER & DIAGNOSTIKA (G1–G6)

Systém O.M.N.I.S. 4.2 realizuje nulovou halucinaci (Zero-Hallucination Gatekeeper) prostřednictvím 6 bran:
1. **G1 (Entry Point Resolution):** Mapování vstupních uzlů v 8D ontologii (`SYS`, `ECON`, `PSYCH`, `ECO`, `LAW`, `SEC`, `PHYS`, `SOC`).
2. **G2 (Concept Coupling):** Provázání alespoň 2 konceptů nebo verifikace sémantického kontextu.
3. **G3 (Causal Graph Path):** Spojitá kauzální cesta v CSR grafu $P(Y \mid do(X=x))$.
4. **G4 (Quotable Evidence):** Ukotvení vůči standardům (ISO/IEC/IEEE, NIST SP 800-207, EU AI Act, NIS2).
5. **G5 (Retraction & Integrity):** Ochrana před zastaralými či falsifikovanými premisami.
6. **G6 (Entailment & Shannon Entropy):** Výpočet informační entropie $S = -\sum p_i \ln p_i$.

Pokud dotaz neprojde kritickými branami, systém pro běžné uživatele navrátí srozumitelné doporučení na upřesnění dotazu, zatímco administrátorovi poskytne plnou diagnostiku s vypnutím bran pouze v případě schváleného `OVERRIDE_ACTIVE` příznaku.
