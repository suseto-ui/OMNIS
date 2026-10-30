# Plán dokončení hlasového TTS/STT streamování a finální dokumentace

## Cíl
Doplnit a zkompletovat reálné hlasové rozhraní (TTS Syntéza čtení + STT Diktování hlasem) v kognitivním chatu, aktualizovat roadmapu `akce.md` o FÁZI XXIII, vygenerovat finální závěrečnou zprávu `final_execution_report.md`, prověřit celou testovací sadu a nahrát změny na GitHub (`suseto-ui/OMNIS.git`).

## Navrhované kroky

### 1. Rozšíření Hlasového Rozhraní (TTS/STT Streamování)
- Prověřit a zdokonalit `SpeechSettingsManager.kt` a `ChatTimelineView.kt` pro plynulé hlasové diktování (STT) i automatické syntetické čtení (TTS).
- Zkontrolovat správnost oprávnění `android.permission.RECORD_AUDIO` v `AndroidManifest.xml` a Compose runtime žádosti.

### 2. Aktualizace Roadmapy `akce.md`
- Do dokumentu `akce.md` doplnit sekci **FÁZE XXIII: REÁLNÉ HLASOVÉ TTS/STT STREAMOVÁNÍ V KOGNITIVNÍM CHATU** a označit všechny dílčí milníky jako `Dokončeno ✅`.

### 3. Vytvoření Finální Zprávy O.M.N.I.S.
- Vytvořit soubor `final_execution_report.md` v artefaktovém adresáři s kompletním přehledem všech 23 fází, výsledky 143 unit testů, stavu bezpečnosti a GitHub synchronizace.

### 4. Verifikace a GitHub Push
- Spustit kompletní sadu testů `gradle :app:testDebugUnitTest`.
- Spustit kompilaci aplikace `compile_applet`.
- Zkomitovat a odeslat všechny změny na větev `main` repozitáře `suseto-ui/OMNIS.git` s využitím zadaného tokenu.

## Rizika a mitigace
- **Riziko**: Android TextToSpeech engine na emulátoru nebo některých zařízeních nemusí mít nainstalované české hlasové balíčky.
- **Mitigace**: Zavedení automatického fallbacku na anglickou výslovnost a bezpečné odchytávání chyb při nepodporovaném jazyku (`TextToSpeech.LANG_MISSING_DATA`).
