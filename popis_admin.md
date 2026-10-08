# O.M.N.I.S. — Administrátorská a Systémová Příručka (Admin & DevOps Manual)

Tento manuál je určen administrátorům, systémovým inženýrům a bezpečnostním auditorům. Detailně popisuje správcovské rozhraní, diagnostické nástroje, zátěžové testy velkých dat, synchronizaci s cloudem a postupy pro škálování a ladění platformy O.M.N.I.S.

---

## 1. Administrátorská Architektura a Bezpečnostní Model

Správcovské rozhraní O.M.N.I.S. je striktně odděleno od běžného klientského zobrazení:
- **`AdminNavigationLayout`**: Specializovaný scaffold pro administrátory (`UserRole.ADMIN_OPERATOR`), který zpřístupňuje systémové panely, správu všech uživatelských vláken a přímé zásahy do databáze.
- **Role-Based Access Control (RBAC)**: Přístup k diagnostice, prořezávání databáze a certifikovaným auditům vyžaduje úspěšné ověření operátora (včetně podpory biometrického ověření otiskem prstu).

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ADMINISTRÁTORSKÝ ŘÍDICÍ PANEL                        │
├────────────────────┬────────────────────┬──────────────────────────────┤
│ Diagnostika & Hub  │ Zátěžový Benchmark │ Bezpečnost & Certifikace     │
│ - AdminHubView     │ - BigDataBenchmark │ - ProductionAuditManager     │
│ - DevPromptLab     │ - LiveMemoryGraph  │ - CircuitBreaker             │
│ - SelfHealing      │ - Latence dotazů   │ - CloudSqlSyncManager        │
└────────────────────┴────────────────────┴──────────────────────────────┘
```

---

## 2. Přehled Administrátorských Modulů a Funkcionalit

### 2.1 Admin Hub (`AdminHubView.kt`)
Centrální dispečink pro správu lokální paměti a systémových akcí:
- **Konsolidace paměti (`triggerMemoryConsolidation`)**: Spustí kognitivní agregaci fragmentů do dlouhodobých syntetických záznamů.
- **Samoopravný mechanismus (`performSelfHealing`)**: Provede kontrolu integrity tabulek SQLite, opraví indexy a vyčistí neukončené transakce.
- **Test Cloud SQL spojení (`testCloudSqlConnection`)**: Ověří dostupnost vzdálené PostgreSQL databáze a změří latenci handshake spojení.

### 2.2 Dev Prompt Lab (`DevPromptLab.kt`)
Vývojářská laboratoř pro testování systémových instrukcí a diagnostiku chyb:
- **Prompt Gateway**: Brána, která filtruje vstupy a počítá ušetřené tokeny. Lze nastavit práh přísnosti (Threshold: 0.1 až 1.0).
- **AI Analýza Chybových Logů**: Pokud systém zachytí varování nebo výjimku, jedním kliknutím odešle stacktrace do dedikovaného modelu k vygenerování návrhu opravy.
- **Simulace anomálií**: Tlačítko pro simulaci chybového stavu (pro ověření reakce jističe Circuit Breaker).

### 2.3 Produkční Audit & 360° Certifikace (`ProductionAuditView.kt`)
Komplexní kontrolní seznam (checklist) pro nasazení do Google Play:
- **Bezpečnost**: Ověření nepřítomnosti pevných hesel v kódu, šifrování síťového provozu TLS, injektování klíčů přes `BuildConfig`.
- **Politiky Google Play**: Kontrola délky názvu aplikace (do 30 znaků), absence širokých oprávnění k úložišti (`READ_EXTERNAL_STORAGE`), zákaz dynamického nahrávání kódu (Zero DCL).
- **Certifikovaný PDF Export**: Tlačítko na horní liště vygeneruje podepsaný PDF auditní report pro investory nebo bezpečnostní kontrolu.

---

## 3. How-To Kuchařka: Praktické Postupy Krok za Krokem

---

### HOW-TO #1: Spuštění Zátěžového Testu Velkých Dat a Sledování RAM

**Záměr**: Prověřit stabilitu aplikace a rychlost databáze při nárůstu dat na 1 000 až 10 000 zpráv.

1. **Krok 1: Otevření benchmarkového panelu**:
   - V levém administrátorském menu klikněte na **"Produkční Audit & Benchmark"**.
   - Na horní záložce přepněte z *"360° AUDIT KVALITY"* na **"ZÁTĚŽOVÝ BENCHMARK"**.
2. **Krok 2: Spuštění generování syntetických dat**:
   - V sekci *Generátor syntetických dat* zvolte zátěžový stupeň:
     - **`+1 000`**: Rychlý test průchodnosti (cca 1–2 sekundy).
     - **`+5 000`**: Střední simulace velkého vlákna (cca 5–8 sekund).
     - **`+10 000`**: Extrémní zátěžový test pro ověření chování při masivním objemu dat.
   - Sledujte modrý pruh postupu a údaj **"Rychlost: X položek/s"** (typicky 1 200 až 2 500 položek za sekundu).
3. **Krok 3: Analýza živého grafu paměti (LiveMemoryGraph)**:
   - Sledujte vlnovku na grafu:
     - Výchozí stav: např. **45 MB / 256 MB**.
     - Během generování: Křivka stoupne o cca 15–30 MB, ale nesmí překročit červenou čárkovanou čáru **(75% Heap Threshold)**.
     - Po dokončení: Automatický Garbage Collector by měl paměť vrátit zpět k normálu.
4. **Krok 4: Spuštění a vyhodnocení testu latence dotazů**:
   - Klepněte na tlačítko **"TEST DOTAZŮ"**.
   - V kartě výsledků zkontrolujte odečtené časy:
     - `Agregace COUNT(*)`: Optimální hodnota je `< 5 ms`.
     - `SELECT 1 000 záznamů`: Optimální hodnota je `< 25 ms` (označeno zeleným odznakem).
     - Celková propustnost: Měla by dosahovat **> 1 000 dotazů/s**.
5. **Krok 5: Bezpečné vyčištění testovacích dat**:
   - Klepněte na červené tlačítko **"VYČISTIT BENCHMARK"**.
   - V potvrzovacím okně potvrďte smazání. Tím se smažou výhradně záznamy s ID vlákna `thread_benchmark`. Reálná uživatelská historie zůstane 100% zachována.

---

### HOW-TO #2: Konfigurace a Spuštění Synchronizace Cloud SQL (PostgreSQL)

**Záměr**: Nastavit bezpečnou zálohu a replikaci lokálních SQLite dat do vzdáleného PostgreSQL serveru.

1. **Krok 1: Kontrola přihlašovacích údajů**:
   - V AI Studio Secrets panelu (nebo v `.env` souboru) zkontrolujte proměnné:
     ```bash
     CLOUD_SQL_HOST=10.0.0.1
     CLOUD_SQL_PORT=5432
     CLOUD_SQL_DATABASE=omnis_db
     CLOUD_SQL_USERNAME=postgres
     CLOUD_SQL_PASSWORD=vase_silne_heslo
     ```
2. **Krok 2: Test spojení v administraci**:
   - Přejděte do **Admin Hub**.
   - V sekci *Cloud SQL Replikace* klepněte na **"TESTOVAT SPOJENÍ"**.
   - Systém provede zkušební připojení a zobrazí stav `CONNECTED (X ms)`.
3. **Krok 3: Spuštění replikace**:
   - Klepněte na ikonu obousměrné synchronizace v horní liště (nebo na tlačítko *"SYNCHRONIZOVAT NYNÍ"*).
   - `CloudSqlSyncManager` načte všechny záznamy s `isSyncedToPostgres == false`, zabalí je do transakce a odešle.
   - Po úspěšném zápisu se v SQLite lokálně nastaví příznak `isSyncedToPostgres = 1`.
4. **Krok 4: Prořezání starých dat (Data Pruning)**:
   - Jakmile jsou data bezpečně v cloudu, můžete uvolnit místo na disku telefonu:
   - V horní liště nebo v Produkčním auditu klepněte na tlačítko **"PROŘEZAT DB"**.
   - Odstraní se lokální data starší než 14 dní, která již mají potvrzenou zálohu v PostgreSQL.

---

### HOW-TO #3: Ovládání Bezpečnostního Jističe (Circuit Breaker)

**Záměr**: Rychle reagovat na výpadek externího API nebo vyčerpání kvóty a přepnout aplikaci do nouzového režimu.

1. **Kdy jistič zasahuje automaticky**:
   - Pokud 3 po sobě jdoucí požadavky na Gemini API skončí chybou HTTP 429 (překročen limit volání) nebo vypršením časového limitu (Timeout).
   - Jistič se automaticky přepne do stavu `OPEN` a na horní liště se rozsvítí červený indikátor.
2. **Manuální shození jističe administrátorem**:
   - Otevřete vysouvací Admin menu.
   - V horní části najděte přepínač **"Systémový Jistič (Circuit Breaker)"** a přepněte jej do polohy VYPNUTO/TRIPPED.
3. **Chování aplikace při rozpojeném jističi**:
   - Žádný dotaz uživatele neodchází do internetu (šetří se peníze a zamezuje se chybovým hláškám).
   - Aplikace okamžitě odpovídá pomocí lokálního deterministického simulačního algoritmu s odznakem `[OFFLINE SIMULACE]`.
4. **Obnova provozu (Reset)**:
   - Po vyřešení výpadku přepněte přepínač zpět do zeleného stavu. Jistič přejde do stavu `HALF_OPEN`, provede ověřovací dotaz a při úspěchu plně obnoví běžný provoz.

---

### HOW-TO #4: Export Certifikovaného PDF Auditního Reportu

**Záměr**: Získat oficiální technický a bezpečnostní report pro vedení, investory nebo certifikační autority.

1. **Spuštění auditu**:
   - Otevřete **Produkční Audit & Benchmark** a klepněte na **"PŘEZKOUMAT SYSTÉM"**.
   - Počkejte, až indikátor doběhne a celkové skóre připravenosti dosáhne zelené hodnoty (např. 90–100 %).
2. **Generování PDF**:
   - Na horní liště klikněte na ikonu dokumentu se štítem (**"Exportovat Certifikovaný Audit"**).
   - Systém vygeneruje vícestránkový formátovaný PDF dokument obsahující:
     - Jméno operátora a časové razítko s přesností na sekundy.
     - Celkové skóre shody s normami Google Play a OWASP MASVS.
     - Tabulku všech úspěšných a neúspěšných kontrol.
     - Telemetrické grafy a historii spotřeby paměti.
3. **Uložení a sdílení**:
   - Systém otevře standardní dialog Androidu pro uložení souboru do složky Dokumenty nebo odeslání e-mailem.

---

## 4. Škálovací Pravidla a Ladění Výkonu pro Vývojáře

Pro zajištění špičkové odezvy i při provozu na slabších zařízeních dodržujte tato pravidla:

1. **Zákaz Main Thread operací**:
   - Veškeré databázové dotazy a manipulace s velkými daty **MUSÍ** probíhat výhradně v kontextu `Dispatchers.IO`.
2. **Dávkování (Chunking) s voláním `yield()`**:
   - Při vkládání více než 100 položek vždy dělte pole do dávek (doporučeno 250 ks) a po každé dávce volejte `yield()`. Tím umožníte procesoru odbavit vykreslení snímku uživatelského rozhraní v Jetpack Compose.
3. **Indexace v Room**:
   - Pokud přidáváte novou tabulku nebo nový způsob vyhledávání, vždy přidejte anotaci `@Index` do entity `OmnisRecord`:
     ```kotlin
     @Entity(
         tableName = "omnis_messages",
         indices = [
             Index(value = ["threadId", "timestamp"]),
             Index(value = ["isSyncedToPostgres"])
         ]
     )
     ```
4. **Plovoucí telemetrická okna**:
   - Do grafů a paměťových ukazatelů nikdy neukládejte neomezené seznamy. Vždy aplikujte pravidlo maximální délky (např. `MAX_MEMORY_SAMPLES = 60`) s odmazáváním nejstaršího prvku:
     ```kotlin
     if (historyList.size > 60) historyList.removeAt(0)
     ```
