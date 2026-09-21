# O.M.N.I.S. – Kompletní Technická Dokumentace a Uživatelský Průvodce

## 1. Přehled Architektury
O.M.N.I.S. je pokročilý transdisciplinární kognitivní framework postavený na moderním Android stacku (Kotlin, Jetpack Compose, Material Design 3, Room DB v11, Gemini API).

### Architektonické vrstvy:
- **Presentation Layer (UI/UX):** Modulární Jetpack Compose obrazovky (`OmnisNavigationLayout`, `ChatView`, `NexusView`, `ScenarioPlannerView`, `GoalTrackerView`, `ArtifactGalleryView`, `MethodologyView`). Podpora pro responzivní design, adaptivní panely a dynamický Dark Theme (Cyberpunk/Terminal Dark paleta s odstíny Cyan, Emerald a Amber).
- **Domain & State Management (`OmnisViewModel`):** Centrální kognitivní motor řídící asynchronní toky pomocí Kotlin Coroutines a StateFlow, s robustním ošetřením coroutine jobů a okamžitým čištěním stavů při přepínání vláken.
- **Data & Persistence Layer (Room v11):** Lokální databáze SQLite obsahující entity:
  - `omnis_messages` (indexované přes `[threadId, timestamp]` a `[userName, timestamp]`)
  - `memory_fragments`
  - `omnis_artifacts`
  - `omnis_goals`
  - `omnis_telemetry`
- **Middleware & Security:** 
  - `OmnisAuthService` pro správu uživatelských relací a rolí (`USER` vs `ADMIN_OPERATOR`).
  - Sémantická brána (Prompt Gateway) s konfigurovatelným Circuit Breakerem.
  - OCR pipeline pro obrázky a PDF dokumenty (`PdfTextExtractor`).

---

## 2. Moduly a Klíčové Funkce
1. **Chat & Kognitivní Kernel:**
   - Podpora pro více paralelních konverzačních vláken s možností vytváření, přejmenování, mazání a exportu.
   - Interaktivní uvítací modul s rychlými akčními prompty O.M.N.I.S.
   - Hlasové diktování dotazů (Speech-to-Text).
   - Přikládání souborů (PDF, Office, Markdown, kód) a obrázků s OCR extrakcí a validací v dialogu (`OcrValidationDialog`).
2. **Neurální Nexus & 8D Matice:**
   - Vizualizace vztahů a paměťových fragmentů v kognitivním síťovém grafu.
   - 8D Octagon analýza stavu a tenzorů v reálném čase.
3. **Plánovač Scénářů a Cílů:**
   - Simulace dopadů a sledování plnění strategických cílů s podporou lokální Room perzistence.
4. **Galerie Artefaktů & PDF/Markdown Export:**
   - Správa generovaných artefaktů a okamžitý export celých vláken do PDF nebo Markdownu (.md) s využitím Android `FileProvider`.
5. **Metodika & Inteligentní Nápověda:**
   - Integrovaná metodika a interaktivní inteligentní průvodce pro operátory i uživatele.
