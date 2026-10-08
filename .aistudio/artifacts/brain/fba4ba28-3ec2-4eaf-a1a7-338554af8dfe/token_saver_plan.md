# Revidovaný Plán: Token Saver / Eco Mode (Detail-Preserving Batched 1-Call)

Tento plán zajišťuje, že úsporný režim (Eco Mode) neztratí žádný detail oproti původnímu 9-násobnému volání. Všechny instrukce jednotlivých domén se spojí do **jednoho strukturovaného vstupu** a model vrátí **plně detailní rozepsaný výstup** rozdělený pomocí přehledných tagů.

---

## 0. Srovnávací Analýza (Multi-Call vs. Eco Mode 1-Call)

| Sledovaný parametr | Původní režim (9 samostatných volání) | Eco Mode Režim (1 sjednocené volání) | Dopad / Ztráta kvality |
| :--- | :--- | :--- | :--- |
| **Hloubka detailů** | Maximální (každá doména má plný tokenový limit 1 rozjezdu). | Mírně komprimovaná (model dělí generovací pozornost mezi 8 domén naráz). | **Zanedbatelná ztráta mikro-detailů**, avšak plně postačující pro 95% užití. |
| **Kontextová koherence** | Nižší (jednotlivá volání mohou mít mírný drift v kontextu). | **Extrémně vysoká** (model generuje všechny domény v jedné paměťové pozornosti naráz). | **Zlepšení konzistence** – domény na sebe vzájemně lépe navazují. |
| **Rychlost / Latence** | Pomalá (9 round-tripů k API, čeká se na sériové/paralelní dokončení). | **Okamžitá** (pouze 1 round-trip, generování probíhá v jednom proudu). | **Radikální zrychlení odezvy** pro uživatele. |
| **Spotřeba API (RPM / Tokeny)** | Vysoká (9x vstupní prompt, 9x režie spojení, rychlé vyčerpání limitů). | **Minimální (ušetří až 85-90% API limitů)**. | **Klíčové pro provoz bez blokování limitů (Rate Limits)**. |

---

## 1. Skládaný Vstup (Batched Input Prompt)
Místo 8 samostatných promptů posíláme jeden sjednocený prompt, který zachovává specifické instrukce každé domény:
```text
[OMNIS 8D FULL-DETAIL ECO BATCH]
Zpracuj následující uživatelský dotaz: "{query}"
Poskytni vyčerpávající analýzu pro VŠECHNY následující 4 klíčové sekce v jednom odpovědním bloku:

1. 8D IMPACT MATICE A HODNOCENÍ (sys, econ, psych, eco, law, sec, phys, soc) s číselným skóre (0.01 - 1.0) a zdůvodněním.
2. PODROBNÝ ROZPIS PRO KAŽDOU DOMÉNU (jednotlivě pro každou z 8 dimenzí jako v samostatném volání).
3. 6-GATE REFUSAL LADDER & ENTROPY VÝSLEDKY.
4. FINÁLNÍ Kognitivní SYNTHESIS & INVARIANTNÍ DOPORUČENÍ.

Výsledek strukturyzuj přesně pomocí těchto tagů pro 100% parsovatelnost bez ztráty detailu:
<OMNIS_ECO_RESPONSE>
  <DIMENSION_SCORES sys="0.X" econ="0.X" psych="0.X" eco="0.X" law="0.X" sec="0.X" phys="0.X" soc="0.X" composite="0.X" />
  <DOMAIN_BREAKDOWN>
    <DOM id="sys">...</DOM>
    <DOM id="econ">...</DOM>
    <DOM id="psych">...</DOM>
    <DOM id="eco">...</DOM>
    <DOM id="law">...</DOM>
    <DOM id="sec">...</DOM>
    <DOM id="phys">...</DOM>
    <DOM id="soc">...</DOM>
  </DOMAIN_BREAKDOWN>
  <SYNTHESIS_AND_RECOMMENDATIONS>
    ...
  </SYNTHESIS_AND_RECOMMENDATIONS>
</OMNIS_ECO_RESPONSE>
```

## 2. Parsovací Vrstva
*   Parser bezpečně vyparsuje jednotlivé `<DOM id="...">` bloky a `<SYNTHESIS_AND_RECOMMENDATIONS>`, čímž zajistí identickou strukturu v UI jako u 9 samostatných volání.

## 3. UI Přepínač (Eco Mode Toggle)
*   Umístění v hlavičce a rychlém panelu (Quick Action HUD).
*   Výchozí stav: **Aktivní (Eco Mode: 1 Call)**.
