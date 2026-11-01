# Plán refaktorizace zpracování promptů a real-time self-healing korekce

Tento plán popisuje celkovou architekturu a kroky pro refaktorizaci logiky prompt enginu, automatickou korekci logických procesů, real-time self-healing zpětnou vazbu a zpřehlednění výstupů v celém ekosystému O.M.N.I.S. (Backend, Frontend, Android).

---

## 1. Architektura Self-Healing Prompt Enginu (Backend)

### A. Modul Korekce a Rekonvalescence (`backend/gemini/prompt_engine.py` & `backend/omnis_pipeline.py`)
- **Self-Healing Feedback Loop:**
  - Implementovat autonomní hodnotitel výstupů, který po syntéze vyhodnotí entropii $S = -\sum p_i \ln p_i$, 8D matici dopadů a průchod 6 branami (G1–G6).
  - Pokud výstup vykazuje sémantickou nekonzistenci, vysokou entropii ($S > 0.55$) nebo nedostatečné ukotvení, systém automaticky zkoriguje prompt a provede 1-krokovou mikrokorekci ještě před navrácením odpovědi uživateli.
- **Strukturované Pydantic v2 Schéma Pro Korekční Záznam:**
  - `PromptCorrectionRecord`:
    - `original_prompt`: Původní dotaz
    - `detected_inconsistencies`: Detekované logické rozporuplnosti nebo slepé skvrny
    - `corrected_prompt_context`: Zpřesněný a rekonvalescovaný prompt
    - `confidence_score`: Skóre jistoty po korekci (0.0 až 1.0)
    - `healing_actions_applied`: Seznam aplikovaných úprav (např. doplnění systémových invariatů, redukce balastu)

---

## 2. Real-Time Interaktivní Korekce & UI Praktiky (Frontend)

### A. Komponenta Self-Healing Prompt Monitor (`frontend/src/components/SelfHealingPromptMonitor.tsx`)
- Zobrazení real-time diagnostiky promptu přímo v chatovacím rozhraní:
  - **Kognitivní zdraví promptu (Prompt Health Index):** Procentuální vyjádření čistoty a logické konzistence.
  - **Detekované anomálie & auto-korekce:** Zobrazení původního vs. zkororigovaného kontextu.
  - **Interaktivní tlačítka akcí:**
    - "Aplikovat doporučenou korekci"
    - "Rozbalit plný self-healing diagnostický protokol"

### B. Zpřehlednění a Zjednodušení Výstupů (`frontend/src/omnisEngine.ts` & `frontend/src/components/MessageBubble.tsx`)
- Aplikace principu **Progressive Disclosure**:
  - Pro `STANDARD_USER`: Čistý, srozumitelný strukturovaný výstup bez zbytečného technického balastu.
  - Pro `ADMIN_OPERATOR`: Jedním klikem rozbalitelné záložky s 8D maticí, CSR topologií, PoN logem a self-healing záznamem.

---

## 3. Mobilní & Android Synchronizace (`app/src/main/java/com/example/ui/`)

### A. Self-Healing Indikátor v Jetpack Compose
- Přidání vizuálního indikátoru do `OmnisChatScreen` / `MessageItem`, který zobrazuje:
  - Odznak `[SELF-HEALING ACTIVE]`
  - Stav kognitivního zdraví a aplikované korekce
- Automatická synchronizace self-healing telemetrie s PostgreSQL přes `/api/sync/telemetry`.

---

## 4. Postup Implementace

1. **Backend (Fáze 1):**
   - Vytvoření modulu self-healing v `backend/gemini/prompt_engine.py`.
   - Propojení s `assemble_omnis_cognitive_cycle` v `backend/omnis_pipeline.py`.
2. **Frontend (Fáze 2):**
   - Vytvoření `SelfHealingPromptMonitor.tsx`.
   - Integrace do `MessageBubble.tsx` a `App.tsx`.
3. **Android (Fáze 3):**
   - Aktualizace Android kognitivního zobrazení a synchronizace s backendem.
4. **Verifikace a Sestavení (Fáze 4):**
   - Sestavení aplikace a otestování `compile_applet`.
   - Sestavení APK a Git synchronizace.
