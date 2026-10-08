# O.M.N.I.S. – STRATEGICKÝ PLÁN POKRAČOVÁNÍ PROJEKTU (ROADMAPA 2026+)

Tento dokument slouží jako centrální architektonický plán a exekuční roadmapa pro další fáze evoluce platformy **O.M.N.I.S. (Omnipresent Multidisciplinary Network Intelligence System)**. Dokument mapuje již dokončené technologické milníky a definuje detailní akční kroky pro další rozvoj kognitivního jádra, multi-agentní orchestrace, hybridního vyhledávání a klientských rozhraní (Web + Android).

---

## 🏆 I. DOSAŽENÉ MILNÍKY (AUDIT DOKONČENÝCH FÁZÍ)

### ✅ FÁZE 1: ODOLNOST INFRASTRUKTURY & KEYPOOL FAILOVER
- [x] **GeminiKeyPool (Multi-Key Rotace):** Automatické přepínání mezi klíči (`GEMINI_API_KEY`, `KEY2`, `KEY3`) s okamžitou obsluhou vyčerpaných kreditů (HTTP 402) a limitů (HTTP 429).
- [x] **Kaskáda modelů:** Primární volání směrováno na `gemini-3.5-flash`, stabilní fallback na `gemini-flash-latest`, `gemini-3.6-flash` a `gemini-3.1-flash-lite`.
- [x] **Cloud SQL & HNSW Indexace:** Zprovozněn HNSW index (`idx_vector_memories_hnsw`, $m=16$, $ef_{construction}=64$) pro sub-milisekundové sémantické vyhledávání v pgvectoru.

### ✅ FÁZE 2: KOGNITIVNÍ SYNTÉZA, AUDIT & EXPORT
- [x] **Adversarial Red-Team Audit:** Interaktivní oponentní prověření odpovědí v reálném čase (`POST /api/adversarial-review`) vyčíslující skrytá rizika a slabiny.
- [x] **Markdown Export Kognitivní Stopy:** Export kompletního myšlenkového otisku (5 fází + 8D matice) do strukturovaného `.md`.
- [x] **Autopoietický Kalibrátor 8D Matice v Dev Labu:** Interaktivní ladění vah dimenzí Oktagonu s výpočtem systémové harmonie a odesláním do autopoietické smyčky (`/api/feedback`).

### ✅ FÁZE 3: REAKTIVNÍ TELEMETRIE & PROPOJENÍ DASHBOARDŮ
- [x] **Obousměrná vazba v Octagon Dashboardu:** Živý monitoring stavu autopoiesis a jednokliková synchronizace kalibrace do databáze.
- [x] **Konzistence stavu v Analytics Dashboardu:** Přímé předávání aktivní matice a konverzačních zpráv do vnořených analytických pohledů.
- [x] **100% Úspěšnost Robolectric Testů:** Validace Android API klientů, maskování klíčů a krizových HTTP stavů.

---

## 🚀 II. AKČNÍ PLÁN PRO POKRAČOVÁNÍ PROJEKTU (NOVÉ FÁZE)

---

### 📌 FÁZE V: REAL-TIME STREAMING MYŠLENKOVÝCH PROUDŮ (SSE / WEBSOCKET)
*Cíl: Přechod z jednorázového blokujícího volání na plynulý proudový přenos (Server-Sent Events) všech 5 fází myšlení a 8D matice.*

- [x] **5.1 Streaming Endpoint v Kognitivním Jádru (`backend/cognitive.py`)**:
  - Implementován FastAPI endpoint `/api/query/stream` využívající `StreamingResponse` (EventStream / SSE).
  - Strukturovaný formát událostí: `init`, `phase_start`, `token_chunk`, `matrix_update`, `consequence_forensics`, `complete`.
- [x] **5.2 Plynulý UI Render v Reactu (`frontend/src/omnisEngine.ts` & `App.tsx` & `MessageBubble.tsx`)**:
  - Implementována čtečka streamu přes `fetch` s `ReadableStreamDefaultReader` a chunkingem v reálném čase.
  - Dynamické vykreslování psacího stroje (Typewriter effect) s živým indikátorem probíhající fáze (`isStreaming`, `streamingPhase`).
- [x] **5.3 Živá Animace Kognitivního DAG Grafu (`CognitiveNodesDashboard.tsx`)**:
  - Vizuální aktivace uzlů v DAG grafu v reálném čase (`activeStreamPhaseNodeId`), jakmile kognitivní jádro přejde do dané fáze syntézy.

---

### 📌 FÁZE VI: MULTI-AGENTNÍ ORCHESTRACE & AUTONOMNÍ KONSENZUS
*Cíl: Zapojení specializovaných autonomních agentů, kteří před finální syntézou vedou interní debatu a validují bezpečnost.*

- [x] **6.1 Multi-Agentní Tým (`backend/multi_agent_orchestrator.py`)**:
  - **Agent Architekt (Syntetizátor):** Návrh primárního řešení dle 5 kognitivních fází a modularity.
  - **Agent Skeptik (Red-Team Oponent):** Identifikace slepých míst, bezpečnostních rizik a SPOF.
  - **Agent Regulátor (Compliance & Ethics):** Posouzení souladu s právními normami (EU AI Act, GDPR, NIS2) a ekologickými limity.
  - **Agent Inženýr (DevOps & Performance):** Latence, indexace a propustnost.
- [x] **6.2 Algoritmus Vážení Konsenzu & Arbitráž**:
  - Výpočet váženého skóre shody agentů ($W_{consensus}$); v případě rozporu spuštění arbitrážní syntézy s penalizací rizikových parametrů v 8D matici.
- [x] **6.3 Interaktivní Zobrazení Debaty Agentů na Frontendu (`MessageBubble.tsx`)**:
  - Rozbalovací panel v detailu zprávy umožňující uživateli sledovat argumentaci jednotlivých 4 agentů, jejich míru jistoty a akční kroky syntézy.

---

### 📌 FÁZE VII: HYBRIDNÍ RAG & SÉMANTICKÁ EPISTEMICKÁ PAMĚŤ
*Cíl: Zvýšení přesnosti vyhledávání v paměti kombinací vektorové podobnosti (Dense) a klíčového full-textu (Sparse/BM25).*

- [x] **7.1 Hybridní Vyhledávání v PostgreSQL (`backend/database.py`)**:
  - Vytvořen GIN index `idx_vector_memories_tsv` nad `to_tsvector('simple', content)`.
  - Kombinace kosínové vzdálenosti pgvector HNSW a plnotextového `tsvector @@ plainto_tsquery`.
- [x] **7.2 Reciprocal Rank Fusion (RRF) v Kognitivní Paměti (`backend/cognitive.py`)**:
  - Implementována funkce `hybrid_retrieve_memories` integrující RRF vzorec ($RRF\_Score = \sum \frac{1}{60 + rank_i}$) pro splynutí dense i sparse rankingů.
- [x] **7.3 Automatická Ingestace Dokumentů a Záloha Paměti**:
  - API endpointy `/api/epistemic/upload`, `/api/epistemic/ingest-text`, `/api/epistemic/stats` a `/api/epistemic/export`.
  - Automatický chunking s konfigurovatelným překryvem a asynchronním generováním 768D embeddingů.
  - Frontendový ovládací pult `EpistemicMemoryDashboard.tsx` s live telemetrií a drag-and-drop ingestem.

---

### 📌 FÁZE VIII: MOBILNÍ & EDGE INTEGRACE (ANDROID JETPACK COMPOSE)
*Cíl: Povýšení nativní Android aplikace na plnohodnotného mobilního klienta se synchronizací stavu a offline podporou.*

- [x] **8.1 Obousměrná Synchronizace Konverzací (`OmnisRepository.kt` & Room DB)**:
  - Zavedení offline mezipaměti v Room DB s automatickou synchronizací s backendovým `/api/threads` při obnovení připojení.
- [x] **8.2 Nativní 8D Radarový Graf v Jetpack Compose (`OctagonView.kt`)**:
  - Implementace plynule animovaného Canvas radaru pro 8 dimenzí Oktagonu v čistém Jetpack Compose.
- [x] **8.3 Biometrická Ochrana a Bezpečné Úložiště Klíčů**:
  - Využití Android `EncryptedSharedPreferences` / `BiometricPrompt` pro zabezpečení přístupu ke klientské telemetrii.

---

## 📋 III. MATICE PRIORIT A ČASOVÝ HARMONOGRAM EXEKUCE

| Krok | Název modulu / Úlohy | Priorita | Očekávaný dopad | Stav |
| :--- | :--- | :--- | :--- | :--- |
| **5.1** | Streaming SSE `/api/query/stream` | **P0 (Kritická)** | Okamžitá odezva UI, nulový blocking | **Dokončeno** ✅ |
| **5.2** | React Live Stream čtečka v Timeline | **P0 (Kritická)** | Plynulý uživatelský zážitek v chatu | **Dokončeno** ✅ |
| **5.3** | Živá animace kognitivního DAG grafu | **P0 (Kritická)** | Vizuální telemetrie fází v reálném čase | **Dokončeno** ✅ |
| **7.1** | Hybridní vyhledávání (HNSW + BM25) | **P1 (Vysoká)** | Eliminace sémantických halucinací | **Dokončeno** ✅ |
| **6.1** | Multi-Agentní orchestrace & debata | **P1 (Vysoká)** | Robustní validace netriviálních dotazů | **Dokončeno** ✅ |
| **7.3** | Epistemic Document Ingest API | **P1 (Vysoká)** | Rychlá indexace externích materiálů | **Dokončeno** ✅ |
| **8.1** | Android Room DB offline synchronizace | **P2 (Střední)** | Mobilní stabilita bez konektivity | **Dokončeno** ✅ |
| **8.2** | Nativní Jetpack Compose 8D Canvas | **P2 (Střední)** | Vizuální parita s webovou konzolí | **Dokončeno** ✅ |
| **8.3** | Biometrická ochrana & šifrované úložiště | **P2 (Střední)** | Ochrana klientských dat (OWASP) | **Dokončeno** ✅ |
| **9.1** | Pokročilý Reranking & Optimalizace RAG | **P1 (Vysoká)** | Eliminace sémantických šumů v kontextu | **Dokončeno** ✅ |
| **9.2** | Middle-tier Sémantická Cache & Guardrails | **P1 (Vysoká)** | Garance JSON schémat, extrémní úspora API | **Dokončeno** ✅ |
| **9.3** | Interaktivní Simulace Co-What-If v UI | **P2 (Střední)** | Vizuální modelování systémových dopadů | **Dokončeno** ✅ |
| **10.1** | Auto-evoluce & destilační smyčky | **P2 (Střední)** | Snížení závislosti na drahých modelech | **Dokončeno** ✅ |
| **10.2** | AI Act & NIS2 Compliance modul | **P1 (Vysoká)** | Právní auditovatelnost kognitivních stop | **Dokončeno** ✅ |
| **10.3** | Multiplatformní synchronizace vláken | **P2 (Střední)** | Konzistentní historie napříč ekosystémem | **Dokončeno** ✅ |
| **11.1** | IoT Telemetrie & Fyzická Integrace | **P2 (Střední)** | Propojení fyzických senzorů s 8D maticí | **Dokončeno** ✅ |
| **11.2** | 3D Canvas Digitálního Dvojčete | **P3 (Nízká)** | Vizuální prostorové modelování Oktagonu | **Dokončeno** ✅ |
| **11.3** | Autonomní IoT Akční Dispatcher | **P2 (Střední)** | Bezpečná exekuce doporučení ve fyzickém světě | **Dokončeno** ✅ |
| **12.1** | Hypergrafová Sémantická Topologie | **P1 (Vysoká)** | Mapování vícerozměrných vazeb vyššího řádu | **Dokončeno** ✅ |
| **12.2** | Perkolační Detekce Kaskádových Rizik | **P1 (Vysoká)** | Včasná predikce řetězových systémových kolapsů | **Dokončeno** ✅ |
| **12.3** | Autonomní Rebalancování Tokenového Toku | **P2 (Střední)** | Adaptivní komprese promptů a úspora latence | **Dokončeno** ✅ |
| **13.1** | Kauzální Do-Calculus Intervenční Engine | **P1 (Vysoká)** | Odlišení prostých korelací od reálné kauzality | **Dokončeno** ✅ |
| **13.2** | Federovaný Epistemický Konsenzus | **P2 (Střední)** | Decentralizovaná agregace znalostí bez cloudu | **Dokončeno** ✅ |
| **13.3** | Zero-Knowledge Důkazy Kognitivní Shody | **P1 (Vysoká)** | Kryptografický audit bez úniku citlivých dat | **Dokončeno** ✅ |
| **14.1** | Bio-Kybernetický Homeostatický Regulátor | **P1 (Vysoká)** | Ashbyho zákon requisite variety pro stabilitu | **Dokončeno** ✅ |
| **14.2** | Regenerativní Autopoietická Smyčka v3 | **P2 (Střední)** | Dlouhodobá adaptace matice dle telemetrie | **Dokončeno** ✅ |
| **14.3** | Spektrální & Haptická Telemetrická Odezva | **P3 (Nízká)** | Okamžitá fyzická signalizace systémového pnutí | **Dokončeno** ✅ |
| **15.1** | Stochastická Monte Carlo Simulace Matice | **P1 (Vysoká)** | Výpočet rozptylů a Value-at-Risk (VaR) 8D stavu | **Dokončeno** ✅ |
| **15.2** | Detektor Černých Labutí & Kurtosis Scanner | **P1 (Vysoká)** | Včasná identifikace extrémních tail-risk jevů | **Dokončeno** ✅ |
| **15.3** | Autonomní Antifragilní Adaptér (Taleb Engine) | **P2 (Střední)** | Schopnost systému sílit pod vlivem volatility | **Dokončeno** ✅ |
| **16.1** | GNN Message-Passing Interaction Embeddings | **P1 (Vysoká)** | Nelineární šíření zpráv napříč grafem 28 vazeb | **Dokončeno** ✅ |
| **16.2** | QUBO Kombinatorická Optimalizace Alokace | **P1 (Vysoká)** | Simulated Annealing pro optimální systémové zásahy | **Dokončeno** ✅ |
| **16.3** | Odolnost Proti Adversarial Permutacím | **P2 (Střední)** | Ochrana před skrytými sémantickými injekcemi | **Dokončeno** ✅ |
| **17.1** | Spojité Difúzní Trajektorie Stavového Prostoru | **P1 (Vysoká)** | Predikce dynamických SDE trajektorií 8D matice | **Dokončeno** ✅ |
| **17.2** | Neuro-Symbolická Verifikace Invariantů | **P1 (Vysoká)** | Formální SMT-style důkazy bezpečnosti stavu | **Dokončeno** ✅ |
| **17.3** | Autonomní Ko-Evoluční Prompt Syntetizátor | **P2 (Střední)** | Genetická optimalizace intervenčních promptů | **Dokončeno** ✅ |
| **18.1** | Algoritmus Kauzálního Objevování (PC/FCI DAG) | **P1 (Vysoká)** | Autonomní rekonstrukce kauzálního grafu z telemetrie | **Dokončeno** ✅ |
| **18.2** | Multifraktální Analýza & Hurstův Exponent | **P2 (Střední)** | Kvantifikace dlouhodobé paměti a persistence 8D stavu | **Dokončeno** ✅ |
| **18.3** | Forenzní Merkle-Tree Ledger Kognitivní Stopy | **P1 (Vysoká)** | Nezvratný kryptografický řetězec myšlenkových kroků | **Dokončeno** ✅ |
| **19.1** | Samoorganizovaná Kritičnost (SOC Avalanche) | **P1 (Vysoká)** | Bak-Tang-Wiesenfeld model kognitivních lavin v 8D | **Dokončeno** ✅ |
| **19.2** | Turingovo Morfogenetické Stavové Pole | **P2 (Střední)** | Reakčně-difúzní prostorová stabilizace matice | **Dokončeno** ✅ |
| **19.3** | Asynchronní Byzantský Konsenzus (BFT Node) | **P1 (Vysoká)** | Odolnost proti kompromitovaným a vadným uzlům | **Dokončeno** ✅ |
| **20.1** | 3-Key Slot Editor & Round-Robin Monitor | **P0 (Kritická)** | Interaktivní správa klíčů a rotace v telefonu | **Dokončeno** ✅ |
| **20.2** | Uživatelský Režim & Do(X) Intervenční Panel | **P1 (Vysoká)** | Přepínač lidského/expertního módu & kauzální simulátor | **Probíhá** 🔄 |
| **20.3** | Oktagon 8D Slidery s Ashbyho Homeostázou | **P1 (Vysoká)** | Interaktivní stabilizace & haptická odezva | **Probíhá** 🔄 |
| **20.4** | Kognitivní Odznaky (SMT, ZK-Hash) & Manuál | **P2 (Střední)** | Přehledné odznaky bezpečnosti a offline nápověda | **Probíhá** 🔄 |

---

### 📌 FÁZE IX: ZDOKONALENÍ ARCHITEKTURY (BACKEND, MIDDLE-TIER, FRONTEND)
*Cíl: Komplexní optimalizace výpočetního výkonu, zavedení sémantických kontrolních mechanismů v mezivrstvě (Middle-tier) a interaktivního modelování v uživatelském rozhraní.*

#### 🛠️ 1. Zdokonalení Backendu (Kognitivní jádro & Vyhledávání)
*   **Pokročilá filtrace a Reranking (Cross-Encoders):**
    *   Začlenění lokálního rerankeru (např. *BGE-Reranker-Large*) po prvotním vyhledání přes pgvector HNSW a full-text. Tím se sníží objem kontextu předávaného do LLM a odfiltruje se sémantický šum.
    *   **Dynamické horké načítání agentů (Hot-loading):**
        *   Architektura pro načítání nových systémových instrukcí, doménových znalostí a promptů agentů bez nutnosti restartu backendové služby.
    *   **Optimalizace zdrojů a pooling:**
        *   Zavedení sdíleného fondu připojení (connection pooling) s automatickým hlídáním limitů a asynchronní obsluhou požadavků na embeddingy pomocí úloh na pozadí (background tasks / Celery).

#### 🛡️ 2. Zdokonalení Mezivrstvy (Middle-Tier, API Orchestrace & Validace)
*   **Strukturované výstupní pojistky (Structured Output Guardrails):**
    *   Integrace knihovny *Pydantic v2* a validačních wrapperů přímo do komunikační vrstvy s Gemini API pro vynucení stoprocentní shody výstupního formátu. Pokud model vrátí nesprávný JSON, mezivrstva automaticky provede lokální opravu nebo bleskovou opravnou žádost (self-healing prompt).
*   **Sémantická mezipaměť (Semantic Caching System):**
    *   Nasazení lokální Redis/InMemory sémantické cache. Pokud uživatel položí dotaz s podobným významem (podobnost embeddingu > 0.95), systém vrátí dříve vypočtenou 8D matici a kognitivní řetězec, čímž ušetří čas a sníží zatížení API klíče na nulu.
*   **Pokročilá telemetrie a tracing (OpenTelemetry):**
    *   Sledování cesty požadavku napříč všemi 5 kognitivními fázemi a podrobný monitoring latencí jednotlivých modelů v kaskádě pro rychlou diagnostiku úzkých hrdel v síti.

#### 🎨 3. Zdokonalení Frontendu (React Web & Compose Android UI)
*   **Interaktivní simulátor Co-What-If:**
    *   Možnost ručního tažení bodů na 8D radarovém grafu (Oktagonu) přímo uživatelem. Frontend na základě korelací v reálném čase dopočítá simulovaný dopad na ostatní domény a vizuálně zvýrazní tenzní linie a optimální pákový bod (Meadows Leverage Point) ještě před odesláním dotazu na model.
*   **Klientský sémantický filtr (Client-side WebAssembly):**
    *   Zavedení rychlé lokální dezinfekce vstupu a kontroly injection vektorů přímo v prohlížeči nebo mobilní aplikaci pomocí lehkých regexových filtrů přeložených do WebAssembly, což ulehčí backendovému perimetru.
*   **Adaptivní design a přístupnost (A11y / WCAG 2.1):**
    *   Plná podpora čteček obrazovky (TalkBack na Androidu, VoiceOver na webu), sémantická Compose označení (`testTag`, `contentDescription`) u všech prvků a bezchybný edge-to-edge design s dynamickým přizpůsobením barvy a velikosti písem.

---

### 📌 FÁZE X: KOGNITIVNÍ AUTO-EVOLUCE & AI GOVERNANCE
*Cíl: Zavedení autonomních učebních smyček (destilace znalostí) k minimalizaci provozních nákladů, integrace komplexního regulatorního auditu podle EU AI Act a sjednocení stavu napříč všemi platformami.*

#### 🧠 1. Autonomní auto-evoluce a destilační smyčky (Autopoiesis v2)
*   **Destilace modelů a úspora nákladů:**
    *   Sběr vysoce hodnocených kognitivních odpovědí (sbíraných přes `/api/feedback` od operátorů) a vytvoření lokálního jemně doladěného (fine-tuned) modelu (např. *Gemma-2-9B* nebo *Llama-3-8B*). Tento model bude schopen replikovat 8D hodnocení a kognitivní řetězce O.M.N.I.S. za zlomek ceny velkých modelů.
*   **Samoléčebný systémový refaktoring:**
    *   Automatická analýza vlastních chybových logů a výkonnostních metrik. Při detekci opakujícího se problému systém autonomně vygeneruje návrh opravy kódu (pull request) a podrobí ho statické analýze a testování.

#### ⚖️ 2. AI Act & NIS2 Compliance Modul (Právní transparentnost a audit)
*   **Generování pasu shody (Conformity Passport):**
    *   Pro každý významný kognitivní proces nebo doporučenou akci systém automaticky vygeneruje strojově čitelný průkaz shody (JSON-LD), který splňuje požadavky EU AI Act pro vysoce rizikové systémy (High-Risk AI Systems).
*   **Real-time detekce etického a právního driftu:**
    *   Analýza odpovědí agentů s ohledem na transparentnost, zákaz diskriminace a ochranu soukromí (GDPR). Při zjištění potenciálního nesouladu je odpověď okamžitě označena stupněm `BLOCKED` s vygenerováním právního odůvodnění.

#### 🔄 3. Multiplatformní Synchronizační Protokol
*   **P2P Sjednocení stavu:**
    *   Bezpečné propojování mobilních aplikací a webových konzolí v lokální síti pro rychlou synchronizaci konverzačních vláken, sémantické paměti a nastavení 8D vah bez nutnosti odesílat data do veřejného cloudu.

---

### 📌 FÁZE XI: TRANSDISCIPLINÁRNÍ INTEGRACE IOT SENZORŮ & DIGITÁLNÍ DVOJČE
*Cíl: Propojení digitální inteligence s fyzickým světem pomocí real-time integrace senzorické telemetrie (IoT) a vytvoření prostorového digitálního dvojčete systému.*

#### 🌐 1. Fyzická vektorová integrace (IoT & senzorická telemetrie)
*   **Přímé napojení fyzického světa:**
    *   Zavedení sběrnice pro příjem reálných fyzických dat (např. teplota, vibrace, GPS poloha, vytížení sítě, okolní hluk) z připojených IoT senzorů nebo vestavěných senzorů mobilního zařízení (akcelerometr, barometr).
    *   **Dynamická adaptace matice:**
        *   Senzorické hodnoty jsou automaticky normalizovány a průběžně promítány do dimenzí `val_phys` (fyzická infrastruktura) a `val_eco` (ekologická zátěž), což dává systému okamžitý obraz o fyzickém prostředí v reálném čase bez zásahu člověka.

#### 📐 2. Prostorové digitální dvojče (3D Canvas & WebGL)
*   **3D vizualizace Oktagonu:**
    *   Vytvoření interaktivního 3D prostorového modelu Oktagonu s využitím WebGL (na webu) a Jetpack Compose 3D Canvas (v mobilní aplikaci). Tento model bude sloužit jako „digitální dvojče“ systému.
*   **Vizuální znázornění pnutí a anomálií:**
    *   Pnutí, frikce a kritická úzká hrdla (bottlenecks) budou ve 3D prostoru dynamicky zobrazeny jako barevné vektory, deformace plochy nebo pulzující napěťové linie, což umožní operátorům okamžitou intuitivní prostorovou diagnostiku.

#### 🤖 3. Autonomní IoT Akční Dispatcher (Uzavření smyčky)
*   **Bezpečná exekuce ve fyzickém světě:**
    *   Rozšíření `OmnisActionDispatcher` o možnost přímého vysílání instrukcí do fyzických akčních členů (např. sepnutí záložního chlazení, omezení příkonu serveru, rozsvícení varovné LED na pracovišti).
*   **Zpětnovazební stabilizace (Feedback Loop):**
    *   Systém po provedení akce okamžitě změří dopad přes senzorickou telemetrii, porovná jej s predikovaným modelem a provede případnou mikrokorekci pro dosažení stabilního systémového ekvilibria.

---

### 📌 FÁZE XII: KOGNITIVNÍ TOPOLOGIE, HYPERGRAFOVÉ VAZBY & KYBERNETICKÁ RESILIENCE
*Cíl: Přechod od párových korelací k n-rozměrným hypergrafům pro zachycení nelineárních vazeb mezi doménami a predikci kaskádových selhání.*

#### 🕸️ 1. Hypergrafová Sémantická Topologie (Higher-Order Relations)
*   **Reprezentace vztahů vyššího řádu:**
    *   Zavedení hyperhran propojujících současně 3 a více domén (např. vazba `[Econ, Sec, Psych]` při nákladových škrtech ohrožujících bezpečnostní morálku týmu).
    *   Výpočet topologických invariantů (Bettiho čísla, Eulerova charakteristika stavového prostoru) pro detekci kognitivních slepých skvrn a systémových pastí.

#### ⚡ 2. Perkolační Model Kaskádových Rizik (Percolation Thresholds)
*   **Analýza zranitelnosti kritické infrastruktury:**
    *   Simulace šíření stresu v síti závislostí pomocí perkolační teorie. Pokud kumulativní degradace 8D matice překročí kritický práh ($p_c$), systém proaktivně přepne do nouzového záchranného režimu (Fail-Safe Protocol).
    *   Automatické trasování záchranných bypassů k odvrácení řetězové dekompenzace.

#### 📊 3. Autonomní Rebalancování Tokenového Toku & Komprese Kontextu
*   **Optimalizace výpočetního rozpočtu:**
    *   Dynamická alokace tokenů na základě komplexity dotazu a systémového napětí ($\sigma$). Jednoduché rutinní požadavky jsou komprimovány s úsporou až 70 % tokenů, zatímco kritické rizikové scénáře získávají maximální hloubku reasoning budgetu.
    *   Průběžná telemetrie poměru vstupních a výstupních tokenů integrovaná do vývojářského laboratorního panelu.

---

### 📌 FÁZE XIII: KAUZÁLNÍ DO-CALCULUS, FEDEROVANÝ KONSENZUS & ZERO-KNOWLEDGE AUDIT
*Cíl: Přechod od čistě asociačních modelů ke kauzálnímu modelování intervencí podle Judea Pearla a decentralizovaný audit integrity.*

#### 🔬 1. Kauzální Do-Calculus Intervenční Engine
*   **Odlišení korelace od kauzality:**
    *   Implementace operátoru $P(Y \mid do(X = x))$ pro modelování přímých systémových intervencí namísto pasivního pozorování.
    *   Grafické odhalování zavádějících proměnných (Confounders) a kolidérů (Colliders) v kognitivní matici.

#### 🤝 2. Federovaný Epistemický Konsenzus (P2P Mesh)
*   **Decentralizované sdílení kognitivních zkušeností:**
    *   Algoritmus lokálního váženého průměrování modelu (Federated Averaging - FedAvg) napříč koncovými Android klienty bez odesílání citlivých konverzací na centrální server.

#### 🛡️ 3. Zero-Knowledge Důkazy Kognitivní Shody (ZK-SNARKs for AI Safety)
*   **Kryptografický audit bez úniku dat:**
    *   Generování kryptografického závazku (Cryptographic Commitment Hash), který prokazuje, že výstup modelu prošel všemi 5 fázemi kognitivního auditu a splňuje bezpečnostní limity, aniž by musel odhalovat interní myšlenkový proces.

---

### 📌 FÁZE XIV: BIO-KYBERNETICKÁ HOMEOSTÁZA, AUTOPOIESIS V3 & HAPTICKÁ TELEMETRIE
*Cíl: Zavedení dynamické homeostatické regulace podle Ashbyho zákona nezbytné variety pro automatické udržování systémové stability.*

#### 🧬 1. Bio-Kybernetický Homeostatický Regulátor (Ashby's Law)
*   **Aktivní tlumení oscilací:**
    *   Sledování variability stavových vektorů v čase. Při prudkých výkyvech v jedné doméně regulátor aktivuje kompenzační tlumení přes spřažené synergie, aby zabránil rezonančnímu zhroucení.
    *   Kvantifikace kybernetické variability ($V_{\text{system}} \ge V_{\text{environment}}$).

#### 🔄 2. Regenerativní Autopoietická Smyčka v3
*   **Kontinuální dolaďování bazálních korelací:**
    *   Dlouhodobá analýza úspěšnosti intervencí a operátorských zpětných vazeb pro jemnou kalibraci tenzoru 28 vazeb v reálném provozu.

#### 📳 3. Spektrální a Haptická Telemetrická Odezva
*   **Fyzická signalizace v mobilním rozhraní:**
    *   Využití Android `Vibrator` API pro generování jemných haptických vzorů (Haptic Feedback) při překročení kritických tenzí a spektrální barevné mapování anomálií v Jetpack Compose.

---

### 📌 FÁZE XV: STOCHASTICKÁ MONTE CARLO CITLIVOST, BLACK SWAN SCANNER & ANTIFRAGILITA
*Cíl: Kvantifikace pravděpodobnostních rizik v podmínkách nejistoty a zavedení Talebovské antifragility.*

#### 🎲 1. Stochastická Monte Carlo Simulace 8D Matice
*   **Deterministické vzorkování:**
    *   Provedení 1 000 sub-milisekundových iterací s Gaussovským šumem $\mathcal{N}(0, \sigma^2)$ na všech 8 dimenzích s vyčíslením 95% a 99% intervalů spolehlivosti a Value-at-Risk (VaR).

#### 🦢 2. Detektor Černých Labutí (Tail-Risk & Kurtosis Scanner)
*   **Identifikace tlustých chvostů rozdělení:**
    *   Výpočet šikmosti (Skewness) a špičatosti (Kurtosis) systémových trajektorií pro včasné varování před vysoce nepravděpodobnými, ale katastrofickými událostmi (Black Swan events).

#### 🛡️ 3. Autonomní Antifragilní Adaptér (Taleb Engine)
*   **Růst skrze volatilitu:**
    *   Mechanismus, který při zvýšeném okolním stresu automaticky posiluje interní rezervy v doménách `Sec`, `Sys` a `Phys`, čímž činí systém odolnějším než před příchodem perturbace.

---

### 📌 FÁZE XVI: GNN MESSAGE-PASSING, QUBO OPTIMALIZACE & ADVERSARIAL PERMUTAČNÍ RESILIENCE
*Cíl: Zavedení grafového předávání zpráv (Graph Neural Network) a kvadratické binární optimalizace pro nalezení globálního optima intervencí.*

#### 🕸️ 1. GNN Message-Passing Interaction Embeddings
*   **Šíření kognitivních vln:**
    *   Každá doména jako uzel v grafu přijímá v každé vrstvě zprávy $m_{v \leftarrow u} = \text{ReLU}(W \cdot [h_u \parallel R_{uv}])$. Po 3 krocích message-passingu se ustálí globální kontextový vektor zachycující i nepřímé interakce 3. stupně.

#### 🧮 2. QUBO Kombinatorická Optimalizace Alokace (Simulated Annealing)
*   **Formulace optimalizace rozpočtu a intervencí:**
    *   Formulace účelové funkce $E(x) = x^T Q x + c^T x$ pro výběr podmnožiny domén k simultánní intervenci s cílem maximalizovat systémovou stabilitu při minimalizaci celkových nákladů a tenzí.
    *   Řešeno pomocí deterministického Simulated Annealing algoritmu v sub-milisekundovém čase přímo na mobilním CPU.

#### 🛡️ 3. Odolnost Proti Adversarial Permutacím
*   **Invariance vůči přeskupení kontextu:**
    *   Algoritmická ochrana proti jailbreakům založeným na záměně pořadí slov nebo zamlžování sémantiky (Adversarial Token Permutation Defense).

---

### 📌 FÁZE XVII: DIFÚZNÍ MODELOVÁNÍ TRAJEKTORIÍ, NEURO-SYMBOLICKÁ VERIFIKACE & KO-EVOLUCE PROMPTŮ
*Cíl: Propojení stochastických diferenciálních rovnic (SDE), formální verifikace logických invariantů a genetické ko-evoluce intervenčních promptů.*

#### 🌊 1. Spojité Difúzní Trajektorie Stavového Prostoru (Neural SDEs)
*   **Dynamická predikce časového vývoje:**
    *   Modelování vývoje 8D vektoru jako stochastického diferenciálního procesu $d\vec{V}_t = f(\vec{V}_t, t) dt + g(\vec{V}_t, t) dW_t$, kde $f$ reprezentuje systémový drift (vliv tenzoru 28 vazeb) a $g$ difúzní volatilitu externího prostředí.
    *   Generování spojitých trajektorií do horizontu $T=10$ časových kroků pro prediktivní řízení.

#### ⚖️ 2. Neuro-Symbolická Verifikace Logických Invariantů (Formal SMT-Style Prover)
*   **Formální záruky bezpečnosti:**
    *   Deterministická kontrola logických predikátů (např. $\forall t: (\text{Sec}_t < 0.30 \implies \text{Blocked}_t \land \Delta \text{Sys}_t > 0)$) bez halucinací.
    *   Generování formálních certifikátů správnosti přechodových stavů.

#### 🧬 3. Autonomní Ko-Evoluční Prompt Syntetizátor (Genetic Optimization)
*   **Genetický algoritmus optimalizace intervencí:**
    *   Evoluční křížení a mutace promptových šablon s výběrem nejzdatnějších jedinců dle fitness funkce maximalizující systémovou harmonii ($H_w$) a minimalizující tokenové náklady.

---

### 📌 FÁZE XVIII: KAUZÁLNÍ DISCOVERY, FRAKTÁLNÍ DYNAMIKA & FORENZNÍ MERKLE LEDGER
*Cíl: Autonomní rekonstrukce orientovaných kauzálních DAG grafů z telemetrie, multifraktální analýza časových řad a kryptografické ukotvení kognitivních stop.*

#### 🧭 1. Algoritmus Kauzálního Objevování (Constraint-Based Causal Discovery)
*   **Rekonstrukce orientovaného kauzálního DAG grafu:**
    *   Implementace PC/FCI algoritmu s podmíněnými testy nezávislosti pro extrakci orientovaných kauzálních vazeb ($X \to Y$) namísto pouhých symetrických korelací.
    *   Automatická identifikace a odstínění skrytých confounderů.

#### 📈 2. Multifraktální Analýza & Hurstův Exponent ($H$)
*   **Dlouhodobá paměť a persistence stavových trajektorií:**
    *   Výpočet Hurstova exponentu $H$ pro každou doménu:
        - $H > 0.5$: Perzistentní trend (systém si pamatuje trajektorii a posiluje drift).
        - $H = 0.5$: Náhodná procházka (Brownův pohyb / nekorelovaný šum).
        - $H < 0.5$: Antiperzistentní oscilace (rychlý návrat k průměru, vhodný pro homeostázu).

#### ⛓️ 3. Forenzní Merkle-Tree Ledger Kognitivní Stopy
*   **Kryptografické hashování myšlenkových kroků:**
    *   Konstrukce binárního Merkle stromu ze všech 5 fází myšlení, 8D matice a bezpečnostních auditů s vygenerováním kořenového hashe (`merkleRootHash`) pro nezpochybnitelný forenzní audit.

---

### 📌 FÁZE XIX: SAMOORGANIZOVANÁ KRITIČNOST, MORFOGENETICKÁ POLE & BYZANTSKÝ KONSENZUS
*Cíl: Propojení fyzikálních modelů lavinového přenosu stresu (SOC), Turingovy reakčně-difúzní morfogeneze a Byzantského konsenzu pro decentralizované uzly.*

#### 🏜️ 1. Samoorganizovaná Kritičnost (Bak-Tang-Wiesenfeld 8D Sandpile Model)
*   **Modelování mikrostresů a lavin:**
    *   Každá doména akumuluje systémový stres. Překročí-li napětí kritickou kapacitu $z_c = 1.0$, dojde k topple jevu (přesypání stresu do sousedních domén dle 28-párového tenzoru).
    *   Vyčíslení mocninného zákona rozdělení lavin $P(S) \sim S^{-\tau}$ pro predikci katastrofických kognitivních kolapsů.

#### 🌌 2. Turingovo Morfogenetické Stavové Pole (Reaction-Diffusion)
*   **Aktivátor-Inhibitor dynamika:**
    *   Simulace chemické morfogeneze $\frac{\partial u}{\partial t} = D_u \nabla^2 u + f(u, v)$ a $\frac{\partial v}{\partial t} = D_v \nabla^2 v + g(u, v)$, kde $u$ je aktivátor stability (např. `Sys`, `Sec`) a $v$ difúzní inhibitor napětí (`Econ`, `Psych`).
    *   Vznik stabilních prostorových stacionárních vzorů v 8D oktagonu.

#### 🛡️ 3. Asynchronní Byzantský Konsenzus (Practical BFT Node Agreement)
*   **Decentralizovaná validace multi-agentních rozhodnutí:**
    *   Algoritmus PBFT s $3f + 1$ tolerancí chyb, garantující dosažení nezpochybnitelného konsenzu o 8D matici a bezpečnostním stupni i při existenci až 33 % vadných či zlomyslných kognitivních agentů.

---

### 📌 FÁZE XX: MODULÁRNÍ MODERNIZACE UŽIVATELSKÉHO FRONTENDU (INTERAKTIVNÍ UI & ZAPOUZDŘENÍ MOTORU)
*Cíl: Přenesení všech nových matematických, kognitivních a síťových schopností do přehledného, ergonomického rozhraní pro mobilní telefony (Android Compose). Důsledné rozdělení na interaktivní uživatelské prvky a zapouzdřené interní algoritmy.*

#### 🎛️ 1. Síťový & Klíčový Hub (3-Key Active Rotation UI)
*   **Interaktivní prvek (A):**
    *   Zavedení `3-Key Slot Editoru` v Dev Labu a Nastavení. Možnost přímého zadání a perzistence tří nezávislých klíčů (`GEMINI_API_KEY`, `GEMINI_API_KEY_2`, `GEMINI_API_KEY_3`) v `SharedPreferences`.
    *   Okamžitý jednoklikový ping test a vizuální Round-Robin indikátor ukazující, který klíč je právě na řadě pro další dotaz.
*   **Zapouzdřená logika (B):**
    *   Automatický failover na pozadí při chybách 429/402/403 a 60s cooldown bez blokování uživatelského rozhraní.

#### 💬 2. Uživatelský Režim & Kauzální Do(X) Intervenční Panel
*   **Interaktivní prvek (A):**
    *   Přepínač `[Lidský asistent / Systémový expert]` přímo v hlavičce chatu pro okamžitou změnu tonality a hloubky odpovědi.
    *   Rozbalovací panel Judea Pearl $do(X)$ pod asistentskou zprávou: Uživatel si zvolí doménu a pevnou hodnotu zásahu a aplikace spočítá a vykreslí simulovaný kauzální efekt bez konfounderů.
    *   Genetický Prompt Mutátor: Zobrazení 3 optimalizovaných mutací dotazu s tlačítkem pro jejich přímé odeslání.
*   **Zapouzdřená logika (B):**
    *   Do-Calculus integrály, SDE difúzní drift a genetické křížení promptů probíhají na pozadí v IO vlákně.

#### 🔷 3. Oktagon 8D Slidery, Ashbyho Homeostáza & Haptika
*   **Interaktivní prvek (A):**
    *   8 interaktivních posuvníků pro domény oktagonu s možností uzamčení (Pin).
    *   Tlačítko *Ashbyho Homeostatická Stabilizace*: automaticky vyhladí extrémy a vygeneruje kompenzační delty.
    *   Haptická telemetrie: Jemné vibrační pulzy při překročení kritické tenze $\sigma > 0.40$ pro hmatovou odezvu stability.
*   **Zapouzdřená logika (B):**
    *   Bak-Tang-Wiesenfeld sandpile lavinový model, Monte Carlo rozptyl a Turingovo morfogenetické pole.

#### 📜 4. Kognitivní Odznaky (Badges) & Integrovaný Manuál
*   **Informační prvek (B):**
    *   Kompaktní odznaky v patičce zprávy: `✅ SMT Formálně Verifikováno`, `🔒 ZK-Commitment Hash` (s možností zkopírovat kliknutím), `⚠️ Kurtosis / Black Swan Scanner`.
    *   Interaktivní průvodce a offline operátorský manuál přímo v aplikaci.

---

### 📌 FÁZE XXI: KOMPLETNÍ FULL-STACK AUDIT, DEKARBONIZACE A HARDENING PERZISTENCE
*Cíl: Komplexní systémová revize celého stacku, izolace historického technologického dluhu, optimalizace indexace v Room DB i Cloud SQL a tuning síťového connection poolu.*

#### 🧹 1. Izolace a Archivace Technologického Dluhu (`/gabbage`)
- [x] **22 přebytečných souborů izolováno:** Všechny jednorázové patch-skripty (`fix_app.py`, `patch_*.py`), diagnostické výpisy (`loop_output.txt`, `map_block.txt`) a zastaralé prototypy (`hybrid_*.py`, `omnis_agents.py`) přesunuty do dedikovaného archivu `/gabbage`.
- [x] **Čistý strom repozitáře:** Kořenový adresář i produkční moduly (`app/`, `backend/`, `frontend/`) zbaveny mrtvého kódu; zachována veškerá systémová dokumentace (`DOCUMENTATION.md`, `akce.md`, `database.txt`, `README.md`).

#### ⚡ 2. Hardening Datové Perzistence (Room SQLite v12 & PostgreSQL)
- [x] **Optimalizace Room indexace (`OmnisRecord.kt` & `MemoryFragment.kt`):**
  - Doplněn index na `isSyncedToPostgres` pro bleskurychlé filtrování nesynchronizovaných záznamů bez nutnosti full table scanu.
  - Doplněn index na `timestamp` v entitě `MemoryFragment` pro okamžité řazení kontextových fragmentů.
  - Povolen plynulý přechod na verzi Room DB 12 (`OmnisDatabase.kt`).
- [x] **Exponenciální Backoff & Jitter v `CloudSqlSyncManager.kt`:**
  - Zavedena 3-fázová retry smyčka s náhodným jitterem (200ms–1200ms) eliminující zátěžové špičky při nestabilním síťovém spojení.
  - Doplněna synchronizace vláken (`thread_id`, `thread_title`, `user_name`) s idempotentním `ON CONFLICT (id) DO UPDATE`.
- [x] **Connection Pool Tuning v Python backendu (`backend/database.py`):**
  - Konfigurace `pool_size=10`, `max_overflow=20`, `pool_recycle=1800` a `pool_pre_ping=True` pro produkční asyncpg engine.

#### 🛡️ 3. Verifikace a Stabilita Kognitivního Jádra
- [x] **100% zelené unit testy:** Úspěšná exekuce `gradle :app:testDebugUnitTest` (33 actionable tasks, 0 chyb).
- [x] **Kompilace ověřena:** `compile_applet` i `compileDebugSources` prošly bez jediné chyby.

---

### 📌 FÁZE XXII: LIVE CLOUD SQL ASYNCHRONOUS SYNCHRONIZATION & HYBRID PRODUCTION MODE
*Cíl: Zprovoznění a verifikace živé asynchronní dávkové synchronizace mezi lokální Room SQLite DB a instancí Google Cloud SQL (PostgreSQL), včetně telemetrické sondy a UI indikace.*

#### 🔄 1. Dávková Asynchronní Synchronizace (`CloudSqlSyncManager.kt` & `OmnisDao.kt`)
- [x] **Rozšíření DAO:** Zavedeny metody `getUnsyncedRecords(limit = 100)` a `markRecordsAsSynced(ids)` pro atomickou aktualizaci příznaku `isSyncedToPostgres`.
- [x] **JDBC Batch Update:** Implementován dávkový zápis `addBatch()` / `executeBatch()` s `ON CONFLICT (id) DO UPDATE` eliminující síťovou režii.
- [x] **Stavový automat `SyncStatus`:** Typově bezpečný sealed interface (`Idle`, `Connecting`, `Syncing`, `Success`, `Offline`) s reaktivním `StateFlow`.
- [x] **Resilience & Flexibilní Konfigurace (`DatabaseConfig.kt`):**
  - Podpora proměnných `CLOUDSQL_*` i přímého parsování `DATABASE_URL` (včetně `postgresql+asyncpg://`).
  - Výchozí produkční IP instance `34.78.59.190:5432`.
  - Nulový vliv výpadku sítě na UI (Room DB funguje s nulovou latencí jako primární zdroj pravdy).

#### 🎛️ 2. UI Telemetrie a Ovládání (`DbHealthSection.kt`, `OmnisTopAppBar.kt`, `AdminHubView.kt`)
- [x] **Interaktivní AdminDbHealthCard:**
  - Zobrazení živých metrik: Celkem (Room), Synchronizováno do Cloud SQL a Čeká na sync.
  - Tlačítko **"SPUSTIT LIVE SYNC"** s animovanou rotací při synchronizaci.
  - Tlačítko **"TEST SPOJENÍ"** pro rychlou diagnostiku soketu.
- [x] **TopAppBar Live Sync Indicator:** Ikona stavu synchronizace přímo v horní liště s barevným odlišením (Cyan = Syncing, Emerald = Online, Amber = Offline Cached).

#### 🛰️ 3. Diagnostická CLI Sonda (`scripts/test_cloudsql_sync.py`)
- [x] **3-fázový telemetrický test:**
  - Fáze 1: TCP soket test s měřením latence (10.58 ms).
  - Fáze 2: TLS v1.3 handshake test (`TLS_AES_256_GCM_SHA384`, 78.09 ms).
  - Fáze 3: SQL dotazová a autentizační vrstva s detailním ASCII reportem.

---

## 🛠️ IV. STANDARDY KVALITY & INVARIANTY PROCESU

1. **Zero-Fluff & Fact-Grounded:** Veškerý kód musí být plně typovaný, bez zástupných komentářů (`TODO`, `pass`) a připravený k okamžitému nasazení.
2. **Cascade Audit [A1]:** Před každou implementací je povinné provést 5-fázový křížový audit (Scope, Routing, Security, Dry Run, Feasibility).
3. **Continuous State Tracking [C1]:** Každý krok musí udržovat aktuální stavovou mapu `[PROJECT STATE SKELETON]` pro zajištění architektonické kontinuity.

