# Plán Implementace: Chronologický Sled Zpráv a Izolace Uživatelského Rozhraní

Tento plán řeší perfektní chronologické řazení konverzačních zpráv zpráva po zprávě bez skákání a přeskakování a zároveň zajišťuje, že běžný uživatel (`STANDARD_USER`) neuvidí žádné administrátorské, ladící ani technické metriky.

---

## 1. Chronologické Řazení & Hybridní Tok Zpráv (`ChatView.kt`)
* **Správné Řazení:** Upravit zobrazení v `ChatView.kt` tak, aby zprávy odpovídaly přirozenému časovému sledu (nejstarší nahoře, nejnovější dole u vstupní lišty).
* **Streaming & Generování:** Probíhající generování odpovídající zprávy se bude dynamicky zobrazovat přímo pod předchozími zprávami na samém konci konverzace.
* **Auto-scrolling:** Plynulý posun na nejnovější zprávu bez přeskakování nebo náhlého skákání na začátek relace.

---

## 2. Striktní Skrytí Technických & Admin Metrik (`ChatMessageItem.kt` & `ChatStatusBanner.kt`)
* **Absolutní Skrytí Tokenové Telemetrie:** Odstranit/skrýt štítky o spotřebě tokenů (`SPOTŘEBA`, spotřeba v USD/Kč) pro roli `STANDARD_USER` v režimu `STANDARD`.
* **Skrytí Interních 8D Tenzorů & Bezpečnostních Kódů:** Raw 8D tenzorové vektory (`Sec`, `Law`, `OctagonMetricsGrid`), ZK-SNARK otisky a interní schvalovací odznaky skrýt pro běžné uživatele. Zobrazit pouze čistý text zprávy.
* **Uživatelsky Přívětivé Chybové Hlášky:** Přeformátovat `ChatStatusBanner.kt` tak, aby při chybě nebo fallbacku zobrazoval pouze čistou čatovou zprávu v lidské češtině bez exponování kódů, API klíčů nebo systémových logů.

---

## 3. Verifikace a Testování
* Provedení `compile_applet` pro ověření bezchybného sestavení.
* Ověření funkčnosti přepínání rolí a plynulosti chatu.
