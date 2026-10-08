# O.M.N.I.S. – KOMPLETNÍ OPERÁTORSKÝ, TECHNICKÝ A METODICKÝ MANUÁL (v4.0)

**O.M.N.I.S. (Omnipresent Multidisciplinary Network Intelligence System)** je transdisciplinární kognitivní architektura a analytický ekosystém pro Android (Kotlin 2.0 / Jetpack Compose Material 3), určený k nelineárnímu modelování systémových dopadů, kauzálním intervencím, řízení systémových rizik a autonomní orchestraci v reálném čase.

---

## 📖 ČÁST I: UŽIVATELSKÝ A OPERÁTORSKÝ MANUÁL

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
Systém striktně odděluje kód i uživatelské rozhraní pro běžné uživatele a systémové operátory.

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
   - **Dialektický Motor (`DIALECTICS`):** Sokratovská synteze teze a antiteze.
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
   - **Admin Hub (`ADMIN`):** Diagnostika 3-slotového API key poolu (`GeminiApiKeyDiagnosticCard`), ruční spuštění Self-Healing auditu, spuštění Cloud SQL dual-write synchronizace, odstraňování synchronizovaných lokálních dat.
   - **Kognitivní Uzly (`NODES`):** Vizualizace výpočetní sítě a topologie.
   - **Matrix / Octagon (`MATRIX`):** Tenzorové simulace, zamykání domén a profilové presety.
   - **Dev Prompt Lab (`DEV_PROMPT_LAB`):** Testování mutací promptů, sledování toku tokenů (`TokenFlowVisualizer`), LLM error diagnostika a dynamické nastavení citlivosti sémantické brány (`PromptGatewayThreshold`).
   - **Telemetrický Dashboard (`TELEMETRY`):** Živý stream fyzické telemetrie a systémových logů.
   - **Test Semantic (`TEST_SEMANTIC`):** Testovací polygon pro re-syntézu a ladění vah.
   - **Auditní Centrum (`PRODUCTION_AUDIT`):** Sledování dodržování EU AI Act a NIS2 normativů.
3. **Globální Dohled nad Konverzacemi:**
   - Operátor má přístup ke všem konverzačním vláknům v systému s možností filtrování podle jednotlivých uživatelů skrze drop-down selector `adminSelectedUserFilter`.

---

### 1.3 Dimenze B: Logika Kognitivních Úrovní a Vliv na Frontend
Kognitivní úroveň určuje způsob prezentace informací v UI a přepíná se tlačítkem `🌱 Zač.` / `⚡ Exp.` v horní liště (`UserTopAppBar` / `AdminTopAppBar`).

#### A. Režim Začátečník (`BEGINNER` / `STANDARD`):
1. **Vizuální Prvky v Chatu (`BeginnerChatHeader`):**
   - Nad konverzací se zobrazuje kompaktní, prostorově úsporná 1-řádková lišta.
   - Obsahuje rychlé nápovědní čipy pro okamžité spuštění nejčastějších dotazů (např. *Porovnat práce*, *Rodinný rozpočet*, *Nájemní smlouva*, *Naplánovat cíl*).
   - Poskytuje jednoklikový přístup k interaktivnímu průvodci (`BeginnerGuideDialog`).
2. **Přizpůsobení Výstupů (Human-Centric Formatting):**
   - Generované odpovědi využívají srozumitelnou, lidskou češtinu/angličtinu.
   - Skrývají se vnitřní tenzorové vzorce, matice a surové JSON struktury.
   - Složité systémové metriky jsou převáděny na jednoduchá bodová doporučení.

#### B. Režim Expert (`EXPERT`):
1. **Odblokování Pokročilých Nástrojů:**
   - Skryje začátečníckou lištu s čipy pro maximální vertikální prostor konverzace.
   - Zpřístupní víceřádkový pokročilý prompt editor s podporou syntaktického zvýrazňování (`OmnisSyntaxHighlighter`).
   - Zobrazuje kompletní 8D stavové vektory `[Sys, Econ, Psych, Eco, Law, Sec, Phys, Soc]` a 28-párový tenzor korelačních vazeb přímo v kartě odpovědi (`Response8dUpliftCard`).
2. **Dopad na Algoritmy a Introspekci:**
   - Aktivuje zobrazení myšlenkových proudů v reálném čase (5-fázový SSE streaming).
   - Zpřístupní interaktivní posuvníky pro kauzální intervence $do(X)$ podle Judea Pearl SCM.
   - Umožňuje inspekci kryptografických otisků ZK-SNARK Commitment a genetických mutací promptů.

---

### 1.4 Navigace a Hlavní Obrazovky
Aplikace využívá ergonomické rozložení s adaptivní navigací pro mobilní zařízení i tablety:

1. **Kognitivní Chat & Timeline (`ChatView`):**
   - **Paralelní konverzační vlákna:** Možnost vytváření, přepínání, přejmenování, exportu a mazání tématických vláken.
   - **Streaming myšlenkových proudů (SSE):** Zobrazení 5 fází deduktivního uvažování v reálném čase.
   - **Multi-Agentní panel:** Rozbalovací rozbor argumentace 4 autonomních agentů (Architekt, Skeptik, Regulátor, Inženýr) s vyčíslením míry konsenzu.
   - **Multimodální vstup:** Hlasové diktování dotazů a přikládání souborů (PDF, Markdown, obrázky) s integrovanou OCR extrakcí textu.

2. **Neurální Nexus & 8D Radar (`NexusView` / `OctagonDashboard`):**
   - **Živý 8D Canvas:** Vizuální radarový graf mapující vektory `[Sys, Econ, Psych, Eco, Law, Sec, Phys, Soc]`.
   - **Přepínač Prioritních Profilů:** Okamžitá adaptace výpočtů dle uživatelských preferencí (Vyvážený profil, Maximální bezpečnost bez ohledu na náklady, Ekonomická optimalizace apod.).
   - **Interaktivní simulátor Co-What-If:** Tažením bodů na radaru lze simulovat hypotetické dopady na zbývající domény před odesláním dotazu.
   - **Pákový bod (Meadows Leverage Point):** Jednoklikové spuštění intervenčního promptu zacíleného na doménu s nejvyšším systémovým multiplikátorem.

3. **Plánovač Scénářů a Sledování Cílů (`ScenarioPlannerView` & `GoalTrackerView`):**
   - Správa strategických cílů s vazbou na 8D metriky a perzistencí v lokální databázi Room.
   - Simulace alternativních rozhodovacích stromů a zátěžové testování.

4. **Vývojářská Kognitivní Laboratoř (`DevPromptLab`):**
   - Manuální kalibrace vah, vizualizace toku tokenů (`TokenFlowVisualizer`) a testování hypergrafové topologie.
   - Generování kryptografických Zero-Knowledge potvrzení o shodě (ZK-Safety Commitment).

5. **Galerie Artefaktů & Export:**
   - Prohlížení generovaných zpráv, tabulek a kódových bloků s pokročilým zvýrazňováním syntaxe.
   - Export kompletních vláken a kognitivních stop do strukturovaného Markdownu (.md) nebo PDF.

---

## 🧮 ČÁST II: MATEMATICKÝ A METODOLOGICKÝ MANUÁL

### 2.1 8D Stavový Vektor a 28-Párový Tenzor Vazeb
Stav systému v libovolném okamžiku je definován normalizovaným vektorem:
$$\vec{V} = [v_{\text{Sys}}, v_{\text{Econ}}, v_{\text{Psych}}, v_{\text{Eco}}, v_{\text{Law}}, v_{\text{Sec}}, v_{\text{Phys}}, v_{\text{Soc}}] \in [0, 1]^8$$

Interakce mezi každou dvojicí domén $(i, j)$ pro $1 \le i < j \le 8$ ($\binom{8}{2} = 28$ vazeb) je řízena korelační maticí $R_{ij} \in [-1.0, +1.0]$:
- **Kritické frikce:** $\text{Econ} \leftrightarrow \text{Eco} \; (r = -0.54)$, $\text{Sec} \leftrightarrow \text{Psych} \; (r = -0.48)$, $\text{Econ} \leftrightarrow \text{Sec} \; (r = -0.35)$
- **Klíčové synergie:** $\text{Psych} \leftrightarrow \text{Soc} \; (r = +0.85)$, $\text{Sys} \leftrightarrow \text{Sec} \; (r = +0.78)$, $\text{Eco} \leftrightarrow \text{Phys} \; (r = +0.68)$

### 2.2 Vážená Leontiefova Odolnost (Weighted Harmonic Mean)
K eliminaci selhání nejslabšího článku (Weakest Link) při zohlednění uživatelských vah $w_i \in [0.05, 1.0]$ slouží vážený harmonický průměr:
$$H_w(\vec{V}, \vec{W}) = \frac{\sum_{i=1}^8 w_i}{\sum_{i=1}^8 \frac{w_i}{v_i}}$$
Celková systémová resilience je dána konvexní kombinací:
$$\mathcal{R}_{\text{systemic}} = 0.70 \cdot H_w + 0.30 \cdot \mu_A$$
kde $\mu_A = \frac{1}{8}\sum v_i$. Kritický stav `isCriticalFailure = true` nastává při poklesu libovolné prioritní domény ($w_i > 0.20$) pod hranici $v_i < 0.20$.

### 2.3 Hypergrafová Topologie Vyššího Řádu (FÁZE XII)
Hyperhrany $\mathcal{E}_k$ propojují $N \ge 3$ domén současně. Topologické invarianty stavového prostoru:
- **Eulerova charakteristika:** $\chi = V - E + F$, kde $V=8$, $E$ je počet aktivních hran a $F$ počet kohezních 3-hyperhran.
- **Bettiho čísla:** $\beta_0 = 1$ (spojitost grafu), $\beta_1 = E - V + \beta_0$ (počet nezávislých kognitivních dutin/smyček).
- **Systémová informační entropie:** $S = -\sum_{i=1}^8 p_i \ln p_i$, kde $p_i = \frac{v_i}{\sum v_k}$.

### 2.4 Perkolační Model Kaskádových Rizik (Percolation Cascade)
Simulace šíření kaskádové dekompenzace při externím šoku v doméně $k$. Pnutí se šíří přes silné závislosti a frikce:
$$\Delta v_j = |R_{kj}| \cdot (1 - v_k) \cdot 0.25$$
Překročí-li podíl kolabujících uzlů kritický perkolační práh $p_c \approx 0.38$, systém autonomně aktivuje nouzové stabilizační bypassy.

### 2.5 Kauzální Do-Calculus (Judea Pearl SCM, FÁZE XIII)
Odlišení pasivní korelace $P(Y \mid X=x)$ od aktivní intervence $P(Y \mid do(X=x))$:
Při intervenci $do(X_k = x_k)$ jsou odstraněny všechny příchozí kauzální šipky do uzlu $X_k$ a deterministicky propagován kauzální účinek do následníků přes strukturní kauzální model (SCM).

### 2.6 Stochastická Monte Carlo Citlivost & Antifragilita (FÁZE XV)
- **Deterministické vzorkování (1 000 iterací):** Vyčíslení 95% a 99% Value-at-Risk ($VaR_{95}, VaR_{99}$).
- **Detektor Černých Labutí:** Výpočet 4. centrálního momentu (Kurtosis) pro odhalení tlustých chvostů distribuce.
- **Talebovský Antifragilní Adaptér:** Posílení systémového jádra v reakci na zvýšenou volatilitu.

### 2.7 GNN Message-Passing & QUBO Kombinatorika (FÁZE XVI)
- **GNN Message-Passing:** Šíření vlnových interakcí přes vrstvy $m_{v \leftarrow u} = \text{ReLU}(W \cdot [h_u \parallel R_{uv}])$ pro ustálení hlubokých kontextových embeddingů.
- **QUBO Simulated Annealing:** Řešení formulace $E(x) = x^T Q x + c^T x$ pro deterministické nalezení optimální kombinace intervencí v sub-milisekundovém čase.

---

## 🛠️ ČÁST III: VÝVOJÁŘSKÝ A INTEGRAČNÍ MANUÁL

### 3.1 Adresářová Struktura
```
/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt               # Entrypoint & Compose Theme
│   ├── OmnisApplication.kt          # Inicializace kontextu a Room DB
│   ├── data/                         # Room DB, Entity, DAO, Converters
│   │   ├── OmnisDatabase.kt          # Room Database instance (v11)
│   │   ├── OmnisRecord.kt            # Databázová entita pro zprávy a telemetrii
│   │   ├── OmnisDao.kt               # Reaktivní SQL dotazy (Flow/Suspend)
│   │   └── OmnisSyncManager.kt       # Asynchronní synchronizace s Cloud SQL
│   ├── domain/                       # Repozitáře a integrační logiky
│   ├── defense/                      # Confidence Gate, Circuit Breaker, Sanitizér
│   ├── api/                          # Gemini Client, Multi-Key Pool
│   └── ui/                           # Jetpack Compose obrazovky a komponenty
│       ├── OmnisCorrelationEngine.kt # Kognitivní jádro, Hypergrafy, Do-Calculus
│       ├── OctagonDashboard.kt       # 8D Canvas radar a prioritní profily
### 1.6 Modulární Frontendová Architektura (FÁZE XX)
Architektura rozhraní striktně odděluje **uživatelsky přínosné interaktivní ovládací prvky** od **interních algoritmických motorů**:

#### A. Interaktivní Uživatelské Komponenty (přístupné a nastavitelné v UI):
1. **Multi-Key 3-Slot Rotátor (`GeminiApiKeyDiagnosticCard`):**
   - Správa a editace klíčů ve slotech `Slot 1`, `Slot 2`, `Slot 3`.
   - Přepínač aktivní rotace (Round-Robin), monitoring chybovosti a individuální test odezvy (`Ping`).
2. **Kauzální Intervenční Simulátor Do(X) (`ChatMessageItem`):**
   - Interaktivní simulátor kauzálních zásahů podle Judea Pearl SCM.
   - Posuvníky pro $do(X)$ intervenci do 8D domén s okamžitou vizualizací přímých a nepřímých kauzálních dopadů ($\Delta V$).
3. **Genetický Mutátor Promptů & Prompt Lab (`ChatMessageItem` & `DevPromptLab`):**
   - Tvorba variant promptů (Expanzivní, Restriktivní, Antifragilní, Sokratovská).
   - Hodnocení fitness skóre a selekce nejstabilnější formulace.
4. **8D Tenzorový Profiler & Preset Switcher (`OctagonDashboard`):**
   - Manuální zamykání domén (Lock/Unlock), dynamické ladění prioritních vah.
   - Ukládání a načítání uživatelských profilů (Slot $\alpha$, Slot $\beta$).

#### B. Vnitřní Zapouzdřené Algoritmické Moduly (Kognitivní Motor):
Tyto moduly běží na pozadí a v UI se projevují formou stavových indikátorů (Badges), telemetrických grafů a výstupních metrik bez nutnosti manuálního zásahu uživatele:
- **Turingovo Morfogenetické Stavové Pole (19.2):** Reakčně-difúzní stabilizace 8D tenzoru.
- **Samoorganizovaná Kritičnost / SOC (19.1):** Automatická detekce a tlumení lavinového šíření kognitivního pnutí.
- **Asynchronní PBFT Konsenzus (19.3):** Multi-agentní byzantská validace se zárukou $3f+1$ tolerance chyb.
- **GNN Message Passing & QUBO Annealing (XVI):** Sub-milisekundová optimalizace systémových vah.
- **Stochastická Monte Carlo VaR & Kurtosis Analýza (XV):** Predikce 95%/99% rizik a detekce černých labutí.

---

## 🛠️ ČÁST III: VÝVOJÁŘSKÝ A INTEGRAČNÍ MANUÁL

### 3.1 Adresářová Struktura
```
/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt               # Entrypoint & Compose Theme
│   ├── OmnisApplication.kt          # Inicializace kontextu a Room DB
│   ├── data/                         # Room DB, Entity, DAO, Converters
│   │   ├── OmnisDatabase.kt          # Room Database instance (v12, optimalizované indexy)
│   │   ├── OmnisRecord.kt            # Databázová entita (indexy: threadId, userName, isSyncedToPostgres)
│   │   ├── MemoryFragment.kt         # Konsolidované paměťové fragmenty (index: timestamp)
│   │   ├── OmnisDao.kt               # Reaktivní SQL dotazy (Flow/Suspend)
│   │   ├── DatabaseConfig.kt         # JDBC klient pro Cloud SQL s TLS/mTLS autodetekcí
│   │   └── CloudSqlSyncManager.kt    # Dual-write sync s exponenciálním backoffem a jitterem
│   ├── domain/                       # Repozitáře a integrační logiky
│   ├── defense/                      # Confidence Gate, Circuit Breaker, Sanitizér
│   ├── api/                          # Gemini Client, Multi-Key Pool (3 sloty)
│   └── ui/                           # Jetpack Compose obrazovky a komponenty
│       ├── OmnisCorrelationEngine.kt # Kognitivní jádro, Hypergrafy, Do-Calculus
│       ├── OctagonDashboard.kt       # 8D Canvas radar a prioritní profily
│       ├── DevPromptLab.kt           # Vývojářská telemetrie a ZK audit
│       ├── chat/                     # Kognitivní chat, Do(X) panel, Mutátor promptů
│       └── admin/                    # Diagnostika 3 klíčů a rotace
├── backend/                          # Volitelný FastAPI Python backend
│   ├── cognitive.py                  # SSE streaming endpoint & 5 fází
│   ├── multi_agent_orchestrator.py   # Multi-agentní debata a konsenzus
│   └── database.py                   # pgvector HNSW, pool_size=10, max_overflow=20
├── /gabbage/                         # Izolovaný historický archiv jednorázových patchů a dumpů
├── akce.md                           # Strategická roadmapa fází I–XXI
└── DOCUMENTATION.md                  # Tento kompletní manuál
```

### 3.2 Zabezpečení a Zero-Knowledge Audit (ZK-SNARK Commitment)
Pro splnění požadavků **EU AI Act (High-Risk AI Systems)** a **NIS2** generuje systém pro každou schválenou odpověď kryptografický závazek:
$$\text{CommitmentHash} = \text{SHA-256}(\text{"OMNIS-ZK-PROOF"} \parallel \text{QueryHash} \parallel \mathcal{R}_{\text{systemic}} \parallel \text{Tier} \parallel \text{Nonce})$$
Tento otisk zaručuje, že výstup prošel 5-fázovým kognitivním auditem bez nutnosti odhalovat interní myšlenkový řetězec.

### 3.3 RBAC Architektura a Návrhový Vzor Strategie (Strategy Pattern)
Systém autorizace v balíčku `com.example.auth` je navržen podle objektového návrhového vzoru **Strategie (Behavioral Strategy Pattern)**. Zcela eliminuje monolitické `if-else` řetězce a udržuje cyklomatickou složitost metod na konstantní hodnotě $CC = 1$:

1. **Rozhraní `UserAuthorizationStrategy`:**
   Definuje kontrakty pro ověřování oprávnění:
   ```kotlin
   interface UserAuthorizationStrategy {
       val role: UserRole
       val maxConcurrentTransactions: Int
       fun hasPermission(permission: SystemPermission): Boolean
       fun canAccessSystemActions(): Boolean
       fun canAccessDevDiagnostic(): Boolean
       fun canModifySystemState(): Boolean
       fun canExecuteTransaction(transactionType: String): Boolean
       fun validateCredentials(secret: String, hasher: (String) -> String): Boolean
       fun getAllowedEndpoints(): Set<String>
   }
   ```

2. **Fine-Grained Granulární Oprávnění (`SystemPermission`):**
   - `ACCESS_SYSTEM_ACTIONS`: Spouštění zásahů do infrastruktury.
   - `ACCESS_DEV_DIAGNOSTIC`: Přístup k diagnostice klíčů a profilování tokenů.
   - `MODIFY_SYSTEM_STATE`: Změna nastavení jističů a prahů sémantické brány.
   - `EXECUTE_DATA_SYNC`: Reaktivní synchronizace s Cloud SQL Postgres.
   - `AUDIT_GOVERNANCE`: Export auditních zpráv EU AI Act & NIS2.
   - `ACCESS_KNOWLEDGE_BASE`: Dotazování lokalního paměťového grafu.
   - `ACCESS_TELEMETRY`: Čtení fyzických a systémových logů.

3. **Registr Strategií (`UserAuthorizationStrategyRegistry`):**
   - Poskytuje polymorfní strategii s časovou složitostí $O(1)$ na základě požadované role (`AdminOperatorStrategy`, `StandardUserStrategy`, `AuditorAuthorizationStrategy`).

4. **Správa Relací (`AuthenticationManager` & `OmnisAuthService`):**
   - Bezpečná autentizace a perzistence relace v kryptovaném/přivátním `SharedPreferences` úložišti (`omnis_auth_prefs`).
   - Automatické obnovení relace při startu aplikace a bezpečné odhlášení vyčištěním paměťových indikátorů.

---

## 🚀 ČÁST IV: PŘÍKAZY PRO VERIFIKACI A NASAZENÍ

```bash
# 1. Spuštění kompletní sady unit testů kognitivního jádra:
gradle :app:testDebugUnitTest

# 2. Sestavení finální debug verze aplikace pro mobilní zařízení:
gradle :app:assembleDebug

# 3. Kontrola statické analýzy a kompilace:
gradle :app:compileDebugSources
```
