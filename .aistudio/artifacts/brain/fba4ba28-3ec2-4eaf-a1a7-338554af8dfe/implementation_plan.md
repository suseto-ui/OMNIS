# Plán Autonomní Kauzální Kognice, Dopadů 2. & 3. Řádu a Interaktivního Spoludoplňovače Vstupu

Tento plán popisuje rozšíření systému O.M.N.I.S. o autonomní kognitivní analýzu dopadů 2. a 3. řádu (T+2 a T+3 časové kaskády), deterministickou kauzalitu Do-Calculus a interaktivní před-exekuční spoludoplňovač vstupu s našeptávačem souvislostí.

---

## 1. Backend: Kauzální Kaskáda 2. a 3. Řádu & Co-Synthesis Engine

### A. Rozšíření Forenzního Modulu (`backend/risk_forensics.py`)
- **T+2 a T+3 Kaskádové Vektory Dopadů:**
  - Implementovat sekvenční vyhodnocování reakčních řetězců:
    - **1. řád (T+1):** Přímé, okamžité systémové a bezpečnostní dopady.
    - **2. řád (T+2):** Nezamýšlené sekundární reakce (např. dominový efekt v ekonomické a právní doméně).
    - **3. řád (T+3):** Terciární dlouhodobý drift, systémové kolize a emergující chování.
- **Kauzální Do-Calculus Matrix ($P(Y \mid do(X=x))$):**
  - Výpočet přímého intervence v 8D prostoru s vyčíslením dojezdových vln do souvisejících domén.

### B. Endpoint Před-Exekuční Analýzy a Spoludoplňovače (`backend/engine.py` & `backend/gemini/prompt_engine.py`)
- **`POST /api/omnis/co-synthesize-input`:**
  - Převezme rozpracovaný nebo kompletní dotaz operátora.
  - Vypočítá v real-time:
    1. Sémantické díry a nedořečené souvislosti.
    2. Předběžnou kaskádu dopadů 2. a 3. řádu.
    3. 3 variantní návrhy na doplnění/dokreslení dotazu (např. *Konzervativní, Bezpečnostní, Transdisciplinární*).

---

## 2. Frontend: Interaktivní Před-Exekuční Rozbor & Našeptávač

### A. Interaktivní Spoludoplňovač Vstupního Pole (`frontend/src/components/InteractiveCoSynthesisModal.tsx` & `ChatInputBar`)
- **Našeptávač & Spoludoplňovač v Chatovacím Poli:**
  - Dynamické detekování klíčových pojmů během psaní.
  - Nabídka inteligentního doplňování souvislostí a navazujících doménových parametrů.
- **Před-exekuční Modal Modal "Kauzální Rozbor Vstupu" (`InteractiveCoSynthesisModal.tsx`):**
  - Tlačítko **"Analýza dopadů 2. a 3. řádu"** vedle tlačítka odeslat.
  - Zobrazení stromové struktury kaskády dopadů:
    - `T+1` Přímý dopad $\to$ `T+2` Sekundární efekt $\to$ `T+3` Terciární systémový drift.
  - Možnost vybrat navrženou variantu spoludoplnění a jedním klikem ji vložit do promptu.

### B. Vylepšená Karta Zprávy s Kaskádou 2. a 3. Řádu (`frontend/src/components/MessageBubble.tsx`)
- Přidání interaktivního panelu **"Kauzální Řetězec Dopadů 2. a 3. Řádu"**:
  - Přehledné vizuální karty ukazující návazné systémové kaskády, pravděpodobnost a mitigační doporučení.

---

## 3. Mobilní & Android Integrace (`app/src/main/java/com/example/ui/`)

### A. Kaskádový Panel v Jetpack Compose (`app/src/main/java/com/example/ui/chat/Response8dUpliftCard.kt`)
- Zobrazení T+1..T+3 kaskádového stromu přímo v mobilním zobrazení.
- Možnost prokliknout jednotlivé uzly dopadů pro detailní náhled.

---

## 4. Postup Implementace

1. **Backend (Fáze 1):**
   - Aktualizace `backend/risk_forensics.py` o T+2 a T+3 sekvence a Do-Calculus.
   - Přidání `/api/omnis/co-synthesize-input` v `backend/engine.py`.
2. **Frontend (Fáze 2):**
   - Vytvoření `InteractiveCoSynthesisModal.tsx`.
   - Úprava `MessageBubble.tsx` pro zobrazení kaskády dopadů 2./3. řádu.
   - Propojení s chatovacím polem v `App.tsx`.
3. **Android (Fáze 3):**
   - Doplnění zobrazení T+2/T+3 kaskád v Compose UI.
4. **Verifikace a Sestavení (Fáze 4):**
   - Zkompilování aplikace (`compile_applet`).
   - Sestavení Android APK a synchronizace do GitHub repozitáře.
