# Plán implementace: Multi-Agentní Aréna s živou 8D debatou a srovnávací vizualizací

Na základě vašeho požadavku a vybraného stylu zobrazení vytvoříme interaktivní **Multi-Agentní Arénu (Dual-Agent Dialectic Arena)** v systému **O.M.N.I.S. 4.2**, kde si uživatel může zvolit dva protichůdné agenty (např. *Konzervativní analytik* vs. *Radikální inovátor*), sledovat jejich živou argumentaci a porovnat jejich hodnocení dopadů v 8D matici.

---

## 🏗️ Klíčové komponenty a architektura

### 1. Archetypy a Modely Agentů (`AgentArenaModels.kt`)
- **Předdefinované archetypy agentů**:
  - 🛡️ **Konzervativní analytik (Regul-Guard)**: Důraz na normativní integritu (LAW, SEC, ECO), bezpečnostní brány G1–G6 a averzi k riziku.
  - 🚀 **Radikální inovátor (Inno-Pulse)**: Důraz na technologické průlomy (SYS, ECON, PHYS), maximální rychlost a tenzorovou optimalizaci.
  - 🕵️ **Skeptik & Auditor (Skept-Beta)**: Důraz na detekci černých labutí, entropii a selhání hraničních uzlů.
  - ⚙️ **Pragmatický inženýr (Engine-Delta)**: Důraz na exekuční stabilitu, paměť v Kotlin 2.0 a ZK-SNARK auditní stopu.
  - 🌍 **Eko-Systémový etik (Eco-Socius)**: Důraz na dlouhodobé společenské a ekologické dopady (ECO, PSYCH, SOC).
- Možnost definice **vlastního agenta** (jméno, role, preference 8D domén, tolerance rizika).

### 2. Engine živé debaty (`AgentArenaEngine.kt`)
- Generování **3-kolové živé debaty**:
  - *Kolo 1: Úvodní pozice a výchozí 8D vektor dopadů*.
  - *Kolo 2: Křížový výslech, antiteze a zpochybnění 8D domén druhé strany*.
  - *Kolo 3: Dialektická konsenzuální syntéza a doporučení*.
- Vypočet 8D vektorů ($[SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC]$) zvlášť pro Agenta A, Agenta B a výslednou Syntézu.

### 3. Uživatelské rozhraní Arény (`MultiAgentArenaView.kt`)
- **Konfigurátor duelu**:
  - Výběr Agenta A a Agenta B z nabídky s vizuálními odznaky.
  - Zadání problému k analýze s čipovými předvolbami.
  - Tlačítko pro spuštění živé arény (`Spustit 8D Duel`).
- **Živý proud argumentů (Live Argument Feed)**:
  - Reálný chronologický výpis reakcí obou agentů.
  - Indikátor probíhajícího kola a probíhající kognitivní deliberace.
- **Srovnávací 8D vizualizace (Pavučinový & Bar graf)**:
  - Vizuální srovnání dopadů v jednotlivých doménách 8D matice vedle sebe (Agent A vs Agent B vs Syntéza).
  - Přehledné zvýraznění domén s největší divergencí/střetem.
- **Syntéza a ZK-SNARK Audit**:
  - Finální index konsenzu, úroveň nejistoty a ZK-SNARK kryptografický otisk (`CommitmentHash`).

### 4. Navigace, Verifikace a Git Export
- Propojení Multi-Agentní Arény do hlavní navigace aplikace O.M.N.I.S.
- Kompilace a ověření funkčnosti přes `compile_applet`.
- Nahrání změn do repozitáře `https://github.com/suseto-ui/OMNIS` (větev `main`).

---

## 📑 Postup realizace

1. **Vytvoření datových modelů** (`app/src/main/java/com/example/dialectics/AgentArenaModels.kt`).
2. **Implementace logiky simulace debaty** (`app/src/main/java/com/example/dialectics/AgentArenaEngine.kt`).
3. **Vytvoření Jetpack Compose UI komponenty Arény** (`app/src/main/java/com/example/ui/dialectics/MultiAgentArenaView.kt`).
4. **Propojení do hlavního menu** (`app/src/main/java/com/example/ui/main/OmnisMainScreen.kt` / `DialecticEngineView.kt`).
5. **Kompilace a Git Commit & Push**.
