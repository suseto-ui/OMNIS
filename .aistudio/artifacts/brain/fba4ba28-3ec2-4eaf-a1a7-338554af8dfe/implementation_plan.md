# Plán implementace: Nové simulační scénáře, zátěžové testy a rozšíření dialektické arény

Na základě vaší volby rozšíříme kognitivní systém **O.M.N.I.S. (v4.2)** o nové pokročilé simulační scénáře, zátěžové testy a hlubší 4-agentní dialektickou debatu.

---

## 1. Nové simulační scénáře a zátěžové testy (`ScenarioModels.kt` & `ForecastingEngine.kt`)
- **Nové komplexní zátěžové scénáře:**
  - `post_quantum_shock`: Kvantový kryptoanalytický šok a nutnost PQC (Post-Quantum Cryptography) migrace.
  - `supply_chain_collapse`: Globální kolaps logistických a čipových řetězců.
  - `blackout_grid_failure`: Masivní výpadek energetické sítě a zero-power výpočetní záložní režim.
  - `deepfake_disinfo_flood`: Informační válka a synchronizovaný útok syntetického obsahu na sociální dynamiku.
- **Monte Carlo zátěžová simulace:**
  - Simulace 1000 paralelních iterací s variabilitou šumu pro predikci volatilních vln v 8D doménovém prostoru.
  - Výpočet indikátoru Systemic VaR (Value at Risk) a Shannonovy entropie stability $S = -\sum p_i \ln p_i$.

---

## 2. Rozšíření multi-agentní arény a dialektiky (`DialecticEngine.kt` & `NexusOrchestrator.kt`)
- **4-agentní strukturovaná konfrontace:**
  - **Architekt:** Konstruktivní návrh 8D řešení a teze.
  - **Skeptik:** Antiteze, hledání zranitelností, logických klamů a rizika selhání.
  - **Regulátor:** Brána G4 (ISO/IEC/IEEE, EU AI Act, NIS2) a eticko-právní limity.
  - **Inženýr:** Praktická exekuce, výpočetní nároky, CSR grafy a syntéza.
- **Dialektický rozhodovací cyklus:**
  - Generování strukturované teze, antiteze a finální syntézy s bodovým hodnocením konsenzu (0–100 %).
  - Integrace ZK-SNARK verifikačního otisku pro auditovaný výstup debaty.

---

## 3. UI úpravy a vizualizace
- Aktualizace obrazovky **Multi-Agent Arena** s vizuálním průběhem debaty agentů krok za krokem.
- Aktualizace **Monte Carlo Inspector** s možností spuštění zátěžových testů a porovnání scénářů.
- Garance dodržení RBAC: `STANDARD_USER` vidí srozumitelnou lidskou syntézu, `ADMIN_OPERATOR` vidí surové tenzory, entropii a ZK otisky.

---

## 4. Verifikace a synchronizace
- Spuštění `compile_applet` pro ověření kompilace.
- Commit a push na GitHub repozitář (`https://github.com/suseto-ui/OMNIS`).
