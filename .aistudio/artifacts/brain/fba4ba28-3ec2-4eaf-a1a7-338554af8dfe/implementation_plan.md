# Plán aktualizace textů a dokumentace O.M.N.I.S. podľa aktuální stavby

Tento plán popisuje postup aktualizace veškerých textových zdrojů v aplikaci a navazující dokumentace podle reálné stavby systému O.M.N.I.S. 4.2.

---

## 1. Aktualizace řetězců v aplikaci (`strings.xml`)
- Rozšíření `app/src/main/res/values/strings.xml` o všechny reálné uživatelské texty, hlášky, názvy komponent, chyby a navigační položky.
- Přidání řetězců pro **Role a RBAC**:
  - `STANDARD_USER`: Srozumitelná lidská řeč, skryté tenzory, přehledné bodové hodnocení.
  - `ADMIN_OPERATOR`: Surové 8D vektory, CSR topologie, telemetrie, ZK-SNARK otisk a přepínač `MODIFY_SYSTEM_STATE` (`OVERRIDE_ACTIVE`).
- Přidání hlášek pro 6-úrovňový **Refusal Ladder Gatekeeper** (G1 až G6) a transparentní diagnostiku.
- Přidání textů pro moduly: Octagon Dashboard, 8D Radar, Multi-Agent Arena, Epistemic Memory, Monte Carlo Risk Inspector a ZK Audit Ledger.

---

## 2. Aktualizace hlavního `README.md`
- Synchronizace architektonického popisu s reálným kódem (Kotlin 2.0, Jetpack Compose M3, Room DB v11, Gemini REST API streaming, Python FastAPI backend, Multi-Agent Orchestrator).
- Přesný popis 8D domén (`SYS`, `ECON`, `PSYCH`, `ECO`, `LAW`, `SEC`, `PHYS`, `SOC`), 28-párového korelačního tenzoru a 6 bran verifikačního žebříčku (G1–G6).
- Aktualizace sekcí sestavení, spuštění testů a deploymentu na GitHub.

---

## 3. Aktualizace specializované dokumentace (`popis*.md`, `popis.txt`)
- **`popis_licky.md`**: Uživatelská příručka pro `STANDARD_USER` vysvětlující přívětivé odpovědi, jednoduchá doporučení a bezpečné použití bez technické zátěže.
- **`popis_admin.md`**: Technický manuál pro `ADMIN_OPERATOR` popisující kognitivní telemetrii, 8D CSR topologii, ruční override (`MODIFY_SYSTEM_STATE`), rotaci API klíčů a ZK-SNARK audit.
- **`popis.md` & `popis.txt`**: Komplexní technicko-operativní dokumentace propojující architektonické vrstvy, datové modely a exekuční pravidla O.M.N.I.S. AKGE-8D.

---

## 4. Verifikace
- Spuštění `compile_applet` pro ověření bezchybného sestavení s novými řetězci.
- Ověření konzistence názvů napříč `metadata.json`, `strings.xml` a dokumentací.
