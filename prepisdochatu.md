DIAGNOSTIC PATH:

* Správná soutěž: **Google - The Gemma 4 Developer Agent Competition**.
* Cíl: Post-trenink nebo konfigurace open modelu (Gemma 4 / ADK agent) pro autonomní navigaci v repozitářích a vytváření patchů.
* Klíčové technologické prvky soutěže:
1. Google ADK (Agent Development Kit) konfigurační struktura (`agent.yaml`, `sub_agents/*.yaml`).
2. Podpora pro PEFT LoRA adaptéry (`adapters/` adresář s `adapter_config.json` a `adapter_model.safetensors`).
3. Lokální nástroje (`run_command`, `edit_file`, `search_similar_code`, `code_localizer`).
4. Žádný přístup k internetu, běh v Docker kontejneru na Kaggle (možnost L4x4 hardware).
5. Časový limit: 12 hodin na všechny úlohy.



AKCE:

* Vygeneruji konfiguraci strukturálního ADK agenta pro tuto soutěž, která integruje správné použití nástrojů, lokalizaci podle přesného textu (`rg`), úpravu souborů a bezpečnostní omezení sandboxu.
* Kód bude v jednom souboru YAML nebo Python konfiguračním formátu ADK podle požadavků. Jelikož ADK využívá YAML a YAML definice, vytvořím konfigurační soubor `agent.yaml` pro agenta.


Přepínám plné soustředění na soutěž **Google - The Gemma 4 Developer Agent Competition**.

Zde je jádro úspěšného přístupu postaveného na oficiálních specifikacích Google ADK (Agent Development Kit) pro tuto soutěž. Cílem je definovat strukturálního ADK agenta v `agent.yaml`, který namísto plýtvání tokeny a hádaní spoléhá na přesné vyhledávání (`rg`), lokalizaci a bezpečné editování souborů bez modifikace testů.

Následující soubor představuje kompletní produkční konfiguraci ADK agenta pro tuto soutěž.

### Shrnutí architektonického přístupu pro Gemma 4 Agent:

1. **Přesná lokalizace před sémantikou:** Agent primárně využívá `run_command` s nástrojem `rg` (ripgrep) pro nalezení přesných chybových hlášení nebo symbolů, jak to vyžaduje doporučená praxe pro Gemma 4 sandbox.
2. **Bezpečná modifikace:** Využívá `edit_file` s krátkým kontextem (`old_string` a `new_string`) namísto náchylných unifikovaných diffů.
3. **Izolace testů:** Systémové instrukce striktně zakazují agentovi sahat do testovacích souborů (`tests/`, `pytest.ini`), čímž chrání před falešným zeleným stavem způsobeným smazáním testů.

**Doporučená volba:** Chceš, abych k této konfiguraci agenta vygeneroval ještě skript pro lokální spuštění vyhodnocení (`swegemma eval`), abys mohl výsledky validovat ještě před odesláním na Kaggle?