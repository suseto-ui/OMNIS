# O.M.N.I.S. — Administrátorská a Systémová Příručka (v4.2 Admin & DevOps Manual)

Tento manuál je určen administrátorům (`ADMIN_OPERATOR`), systémovým inženýrům a bezpečnostním auditorům. Detailně popisuje správcovské rozhraní, diagnostické nástroje, zátěžové testy velkých dat, synchronizaci s cloudem, 6-úrovňový Refusal Ladder Gatekeeper, CSR topologii a ZK-SNARK auditní ledger.

---

## 1. Administrátorská Architektura a Bezpečnostní Model

Správcovské rozhraní O.M.N.I.S. je striktně odděleno od běžného klientského zobrazení (`STANDARD_USER`):
- **`AdminNavigationLayout`**: Specializovaný scaffold pro administrátory (`UserRole.ADMIN_OPERATOR`), který zpřístupňuje systémové panely, správu všech uživatelských vláken a přímé zásahy do databáze skrze příznak `MODIFY_SYSTEM_STATE`.
- **Role-Based Access Control (RBAC)**: Přístup k diagnostice, prořezávání databáze a certifikovaným auditům vyžaduje úspěšné ověření operátora.

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ADMINISTRÁTORSKÝ ŘÍDICÍ PANEL                        │
│ ├──────────────────┬──────────────────┬──────────────────────────────┤ │
│ │ Diagnostika & Hub│ Zátěžový Benchmark│ Bezpečnost & ZK Audit        │ │
│ │ - AdminHubView   │ - BigDataBenchmark│ - Refusal Ladder G1-G6 Gate  │ │
│ │ - DevPromptLab   │ - LiveMemoryGraph │ - CircuitBreaker             │ │
│ │ - SelfHealing    │ - Latence dotazů  │ - ZK-SNARK CommitmentHash    │ │
└─┴──────────────────┴──────────────────┴──────────────────────────────┴─┘
```

---

## 2. Přehled Administrátorských Modulů

### 2.1 Refusal Ladder Gatekeeper Diagnostika (G1 až G6)
Administrátor má přístup k detailnímu stavu průchodu 6 branami:
- **G1 (Entry Point)**: Ontologické uzly v 8D prostoru (`SYS`, `ECON`, `PSYCH`, `ECO`, `LAW`, `SEC`, `PHYS`, `SOC`).
- **G2 (Concept Coupling)**: Detekce provázanosti konceptů.
- **G3 (Causal Graph Path)**: Existence kauzální cesty v CSR matici.
- **G4 (Quotable Evidence)**: Důkazy vůči ISO/IEC, NIST SP 800-207, EU AI Act, NIS2.
- **G5 (Retraction & Integrity)**: Validace platnosti premis.
- **G6 (Entailment & Shannon Entropy)**: Hodnota entropie $S = -\sum p_i \ln p_i$.

### 2.2 Executive Override (`MODIFY_SYSTEM_STATE`)
V případě potřeby ručního zásahu může administrátor uplatnit Executive Override s aktivací kryptografického příznaku `OVERRIDE_ACTIVE` zapísaného do auditního řetězce.

### 2.3 ZK Audit Ledger & Commitment Hash
Každá odpověď pro operátora generuje kryptografický otisk:
$$\text{CommitmentHash} = \text{SHA-256}(\text{"OMNIS-ZK-PROOF"} \parallel \text{QueryHash} \parallel \mathcal{R}_{\text{systemic}} \parallel \text{Tier} \parallel \text{Nonce})$$

---

## 3. How-To Kuchařka pro Operátory

### HOW-TO #1: Zátěžový Test a Sledování RAM
1. Otevřete **Produkční Audit & Benchmark**.
2. Zvolte syntetickou zátěž (`+1 000`, `+5 000` nebo `+10 000` položek).
3. Sledujte `LiveMemoryGraph` (udržujte RAM pod 75% prahem).
4. Po dokončení proveďte **"VYČISTIT BENCHMARK"**.

### HOW-TO #2: Test Cloud SQL Replikace
1. Ověřte proměnné prostředí pro PostgreSQL.
2. V **Admin Hub** klepněte na **"TESTOVAT SPOJENÍ"**.
3. Spusťte asynchronní synchronizaci nesynchronizovaných záznamů (`isSyncedToPostgres == false`).
