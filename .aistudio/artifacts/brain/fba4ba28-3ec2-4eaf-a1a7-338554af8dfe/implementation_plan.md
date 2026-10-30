# Plán aktualizace grafiky, ikon a vizuálních podkladů O.M.N.I.S.

## Cíl
Vytvořit kompletní sadu nových vizuálních podkladů v moderním **sci-fi stylu s 8D neonovým oktagonem a tmavým pozadím** (#0B0F19). Sada zahrnuje novou adaptivní ikonu aplikace (popředí, pozadí, rasterizované PNG pro všechny rozlišení Androidu), sci-fi hero banner, onboarding ilustraci a kybernetický profilový avatar operátora. Následně tyto podklady integrovat do rozhraní Jetpack Compose, ověřit sestavení a odeslat na GitHub.

## Navrhovaný postup

### 1. Vygenerování a konfigurace adaptivní ikony aplikace (App Icon)
- **Generování ikony**: Pomocí nástroje `generate_image` vygenerovat `img_omnis_app_icon` (poměr 1:1, zářící 8D neonový oktagon s kognitivním uzlem v centru na pevně definovaném pozadí `#0B0F19`).
- **Konfigurace layer-listu**:
  - Aktualizovat `res/drawable/ic_launcher_foreground.xml` na 66dp bezpečný výřez vygenerovaného obrázku.
  - Nastavit `res/drawable/ic_launcher_background.xml` na přesný barvový hex `#0B0F19`.
- **Generování rasterizovaných PNG ikonek**:
  - Pomocí skriptu ImageMagick vygenerovat `ic_launcher.png` a zaoblené `ic_launcher_round.png` pro všech 5 rozlišení (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
  - Odstranit staré šablonové `.webp` ikony, aby nedocházelo ke konfliktům v AAPT2.
- **Manifest**: Ověřit správné propojení v `AndroidManifest.xml` (`@mipmap/ic_launcher` a `@mipmap/ic_launcher_round`).

### 2. Generování sci-fi vizuálních podkladů pro aplikaci
- **Hero Banner (`img_omnis_hero_banner`)**: Poměr 16:9, futuristický sci-fi kognitivní graf s neonovými spoji v 8D prostoru, tmavé skleněné pozadí.
- **Onboarding Ilustrace (`img_omnis_onboarding`)**: Poměr 4:3, hologram AI asistenta O.M.N.I.S. s 8D radarovými uzly.
- **Avatar Operátora (`img_omnis_avatar`)**: Poměr 1:1, kybernetický profilový avatar s futuristickým průhledným průzorem.

### 3. Integrace do rozhraní Jetpack Compose
- Zapojit nový hero banner do přehledového kokpitu (`UserDashboardView` / `AdminHubView`).
- Zapojit avatar operátora a onboarding grafiku do chatu a postranního menu (`AppHeader`, `NavigationSidebar`).
- Zkontrolovat Material 3 tmavé sci-fi barevné schéma a vizuální soulad.

### 4. Verifikace sestavení a GitHub Push
- Provést kompilaci aplikace skrze `compile_applet`.
- Spustit testy `gradle :app:testDebugUnitTest`.
- Zkomitovat novou grafiku a odeslat změny na GitHub repozitář `suseto-ui/OMNIS.git`.

## Rizika a mitigace
- **Riziko**: AAPT2 chyba při konverzi nebo nesprávném MIME typu obrázků (JPG vs PNG).
- **Mitigace**: Použití ImageMagick `convert "PNG32:..."` pro garanci platného formátu RGBA PNG.
