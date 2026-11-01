# Důkladný plán implementace: Oborová Fokusace, Latent Causal Discovery & Živý Panel Progressive Disclosure

Tento plán detailně popisuje architekturu a postup implementace tří klíčových kognitivních modulů systému O.M.N.I.S.:

---

## 1. Oborová Fokusace & Adaptivní CSR Tenziometr (`backend/csr_graph.py` & `backend/domain_dictionaries.py`)

### A. Autonomní Detekce & Dynamická Váhovost CsrKnowledgeGraph
- **Dynamický oborový slovník (`domain_dictionaries.py`):**
  - Autonomní modul pro extrakci oborových slovníků (FinTech, Průmysl 4.0 / Výroba, HealthTech, CyberSec, BioTech, Smart Governance) na základě sémantické hustoty a "Motýlího efektu" (Butterfly Effect Expansion).
- **Adaptivní 28-párový Tenziometr v `CsrKnowledgeGraph`:**
  - Zachování 8 základních dimenzí (SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC).
  - Dynamické přenaštění vah v CSR matici podle detekovaného oboru (např. v Průmyslu má vazba `PHYS-ECO` váhu 0.95, ve FinTechu vazba `ECON-LAW` váhu 0.98).

---

## 2. Latent Causal Discovery Engine (`backend/latent_causal_engine.py` & `backend/percolation.py`)

### A. Topologická Analýza Síťových Děr & Bettiho Čísla ($\beta_1$)
- **Výpočet Bettiho čísel ($\beta_1$):**
  - Detekce 1-dimenzionálních cyklů a "kognitivních děr" v grafech závislostí.
  - Výpočet kritického prahu perkolation threshold $p_c \approx 0.38$.
- **Autonomní Kontrafaktuální Simulace Do-Calculus ($P(Y \mid do(X=x))$):**
  - Asynchronní 4-agentní simulace (Architekt, Skeptik, Regulátor, Inženýr) běží na pozadí.
  - Odhaluje skryté kaskádové zranitelnosti (např. *"Úspora v cloudu za 3 měsíce způsobí vyhoření týmu a bezpečnostní mezeru"*).
  - Vytváří neprůstřelný kryptografický **Proof of Necessity (PoN)**.

---

## 3. Živý Postranní Panel & Progressive Disclosure (`frontend/src/components/LatentCausalSidebar.tsx`)

### A. Živý Postranní Panel v Reálném Čase (`LatentCausalSidebar.tsx`)
- Zobrazení v pravé části rozhraní:
  - **Bettiho čísla ($\beta_1$):** Počet detekovaných cyklických zranitelností a síťových trhlin.
  - **Perkolační Tenzometr ($p_c$):** Vizuální ukazatel s varovným prahem při $p_c \ge 0.38$.
  - **Motýlí Efekt & Skryté Vazby:** Zobrazení neviditelných kauzálních řetězců dopadů 2. a 3. řádu.
  - **PoN (Proof of Necessity) Ledovec:** Tlačítko pro postupné odkrývání (Progressive Disclosure) hluboké 8D důkazní matice pro administrátora.

---

## 4. Mobilní & Android Integrace (`app/src/main/java/com/example/ui/`)

### A. Topologický Pás v Jetpack Compose (`app/src/main/java/com/example/ui/chat/ChatStatusBanner.kt`)
- Zobrazení živého stavu perkolace $p_c$ a Bettiho čísla $\beta_1$ přímo v záhlaví chatu.
- Možnost rozbalit PoN detail na mobilním zařízení.

---

## 5. Postup Implementace

1. **Backend (Fáze 1):**
   - Vytvoření `backend/domain_dictionaries.py` a úprava `backend/csr_graph.py`.
   - Vytvoření `backend/latent_causal_engine.py` pro Bettiho čísla $\beta_1$ a perkolaci $p_c$.
   - Přidání endpointu `GET /api/omnis/latent-causal-stream` a `POST /api/omnis/analyze-latent-causality`.
2. **Frontend (Fáze 2):**
   - Vytvoření `LatentCausalSidebar.tsx`.
   - Propojení s `App.tsx` a živým kognitivním stavovým strojem.
3. **Android (Fáze 3):**
   - Aktualizace Compose UI komponent pro zobrazení perkolace a Bettiho čísel.
4. **Verifikace a Sestavení (Fáze 4):**
   - Zkompilování aplikace (`compile_applet`).
   - Vygenerování instalačního balíčku APK a Git synchronizace.
