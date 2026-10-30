# O.M.N.I.S. – STRATEGICKÝ PLÁN POKRAČOVÁNÍ PROJEKTU (ROADMAPA 2026+)

Tento dokument slouží jako centrální architektonický plán a exekuční roadmapa pro další fáze evoluce platformy **O.M.N.I.S. (Omnipresent Multidisciplinary Network Intelligence System)**. Dokument mapuje již dokončené technologické milníky a definuje detailní akční kroky pro další rozvoj kognitivního jádra, multi-agentní orchestrace, hybridního vyhledávání a klientských rozhraní (Web + Android).

---

## 🏆 I. DOSAŽENÉ MILNÍKY (AUDIT DOKONČENÝCH FÁZÍ)

```mermaid
graph LR
    A[Fáze 1-3: Odolnost & Telemetrie] --> B[Fáze 5-8: Real-Time Stream & Multi-Agent]
    B --> C[Fáze 9-15: Kauzální Do-Calculus & Monte Carlo]
    C --> D[Fáze 21-22: Cloud SQL Dual-Write & Hardening]
    D --> E[Fáze 23: Reálné Hlasové TTS/STT Streamování]
```

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

### 📌 FÁZE XXIII: REÁLNÉ HLASOVÉ TTS/STT STREAMOVÁNÍ V KOGNITIVNÍM CHATU
*Cíl: Plné zprovoznění obousměrného hlasového rozhraní v kognitivním chatu (Text-To-Speech syntéza a Speech-To-Text diktování s oprávněním RECORD_AUDIO).*

- [x] **23.1 Nativní Android STT Contract (`RECORD_AUDIO` & RecognizerIntent)**:
  - Zprovoznění rozpoznávání řeči přes Android `SpeechRecognizer` API s automatickým přepisováním diktovaného hlasu do textového pole.
- [x] **23.2 Dynamická Hlasová TTS Syntéza (`SpeechSettingsManager.kt`)**:
  - Reaktivní perzistence rychlosti čtení (`speechRate`), výšky tónu (`speechPitch`) a pohlaví hlasu (`voiceGender`) v `SharedPreferences`.
  - Automatický fallback při chybějících českých hlasových balíčkách (`TextToSpeech.LANG_MISSING_DATA`).
- [x] **23.3 Hlasové Ovládání v Kognitivním Chatu (`ChatTimelineView.kt`)**:
  - Přímé spuštění hlasového čtení odpovědi po dokončení SSE streamu a tlačítko pro hlasové diktování u zprávy.

---

## 📋 III. MATICE PRIORIT A ČASOVÝ HARMONOGRAM EXEKUCE

| Krok | Název modulu / Úlohy | Priorita | Očekávaný dopad | Stav |
| :--- | :--- | :--- | :--- | :--- |
| **23.1** | Nativní STT kontrakt & RECORD_AUDIO oprávnění | **P0 (Kritická)** | Bezproblémové diktování řeči do chatu | **Dokončeno** ✅ |
| **23.2** | Dynamická TTS syntéza & SpeechSettingsManager | **P0 (Kritická)** | Hlasové čtení odpovedí s volbou tempa | **Dokončeno** ✅ |
| **23.3** | Hlasové tlačítko v kognitivním chatu | **P1 (Vysoká)** | Kompletní obousměrný hlasový zážitek | **Dokončeno** ✅ |

---

## 🛠️ IV. STANDARDY KVALITY & INVARIANTY PROCESU

1. **Zero-Fluff & Fact-Grounded:** Veškerý kód musí být plně typovaný, bez zástupných komentářů (`TODO`, `pass`) a připravený k okamžitému nasazení.
2. **Cascade Audit [A1]:** Před každou implementací je povinné provést 5-fázový křížový audit (Scope, Routing, Security, Dry Run, Feasibility).
3. **Continuous State Tracking [C1]:** Každý krok musí udržovat aktuální stavovou mapu `[PROJECT STATE SKELETON]` pro zajištění architektonické kontinuity.
