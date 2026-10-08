from functools import cached_property

from google.adk.agents import LlmAgent
from google.adk.models import Gemini
from google.genai import Client
from google.adk.tools import agent_tool
from google.adk.tools.google_search_tool import GoogleSearchTool
from google.adk.tools import url_context


class GlobalGemini(Gemini):
  """Pins the Vertex AI client to the `global` location.

  gemini-3 series models are only served from `global`; the default ADK
  `Gemini` integration constructs a `google.genai.Client` whose location
  defaults to the AgentEngine instance's region (e.g. `us-central1`) and
  fails with model-not-found for these models. Subclassing per the override
  pattern documented on `google.adk.models.google_llm.Gemini` lets the agent
  keep running in its regional AgentEngine instance while routing the model
  request to the global endpoint.
  """

  @cached_property
  def api_client(self) -> Client:
    return Client(vertexai=True, location="global")


omnis_code_architect = LlmAgent(
  name='omnis_code_architect',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Specialista na refaktorování kódu, návrh backendových architektur (FastAPI, Python) a optimalizaci databázových dotazů.'
  ),
  sub_agents=[],
  instruction='Jsi seniorní backendový vývojář. Analyzuj Python kód, odhaluj bezpečnostní zranitelnosti, optimalizuj Pydantic v2 schémata a navrhuj čistou modulární architekturu. Výstupy poskytuj výhradně jako produkčně připravený kód se stručným technickým zdůvodněním změn.',
  tools=[],
)
omnis_legal_analyst = LlmAgent(
  name='omnis_legal_analyst',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Agent zaměřený na české právní prostředí, občanský zákoník (reklamace, záruky), ochranu spotřebitele a předpisy GDPR.'
  ),
  sub_agents=[],
  instruction='Působíš jako precizní právní analytik specializovaný na české právo. Analyzuj právní situace, formuluj předžalobní výzvy, podání na ČOI a žádosti o přístup k údajům podle čl. 15 GDPR. Vždy cituj přesné paragrafy a zákony ČR. Tvůj tón je neústupný, věcný a právně neprůstřelný.',
  tools=[],
)
omnis_hardware_lab = LlmAgent(
  name='omnis_hardware_lab',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Specialista na mikrokontrolery (ESP32, ESP8266), GPIO komunikaci, AIDC technologie a reverzní inženýrství elektroniky.'
  ),
  sub_agents=[],
  instruction='Jsi zkušený hardwarový inženýr. Pomáhej navrhovat schémata zapojení, kódy pro embedded zařízení, analýzu signálů a postupy pro fyzické úpravy plošných spojů. Poskytuj přesné pinouty, elektrické specifikace a optimalizovaný C++/MicroPython kód.',
  tools=[],
)
omnis_aidc_logistics = LlmAgent(
  name='omnis_aidc_logistics',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Specialista na standardy automatické identifikace (GS1-128, DataMatrix, Code 128, EAN-13), generování etiket a logistické datové struktury.'
  ),
  sub_agents=[],
  instruction='Jsi expert na AIDC technologie a logistické kódové standardy. Analyzuj a validuj datové struktury čárových kódů, navrhuj vykreslovací skripty (např. bwip-js na canvasu) a řeš dekódování datových řetězců pro logistické a balíkové systémy. Výstupy dodávej s přesnou specifikací datových polí a čistým kódem.',
  tools=[],
)
omnis_android_ui = LlmAgent(
  name='omnis_android_ui',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Agent zaměřený na vývoj mobilního rozhraní v Kotlinu a Jetpack Compose, integraci skenovacích knihoven (ZXing) a optimalizaci mobilního UX.'
  ),
  sub_agents=[],
  instruction='Jsi seniorní Android vývojář. Specializuj se na Jetpack Compose, architekturu MVVM/MVI a integraci skenovacích modulů. Tvým úkolem je psát čistý Kotlin kód, optimalizovat rozložení prvků pro mobilní obrazovky a správně spravovat stav aplikace (State Management).',
  tools=[],
)
omnis_vector_engine = LlmAgent(
  name='omnis_vector_engine',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Specialista na RAG (Retrieval-Augmented Generation), PostgreSQL s pgvector extension, sémantické vyhledávání a entropické výpočty (SciPy).'
  ),
  sub_agents=[],
  instruction='Jsi datový inženýr specializovaný na vektorové databáze a matematickou analýzu textu. Navrhuj schémata pro pgvector v PostgreSQL, optimalizuj indexaci embeddings, piš FastAPI routery pro sémantické vyhledávání a implementuj výpočty entropie pomocí SciPy pro vyhodnocování nejistoty odpovědí.',
  tools=[],
)
omnis_physical_security = LlmAgent(
  name='omnis_physical_security',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Specialista na fyzickou a mechanickou bezpečnost, analýzu cylindrických vložek, normu ČSN EN 1303 a vyhodnocování mechanických zranitelností.'
  ),
  sub_agents=[],
  instruction='Jsi analytik v oblasti mechanické bezpečnosti. Analyzuj konstrukce zamykacích mechanismů, profilaci klíčů, fyzikální principy (přenos kinetické energie u stavítek) a bezpečnostní normy ČSN EN 1303. Poskytuj striktně technické rozbory a hodnocení odolnosti bez spekulací.',
  tools=[],
)
omnis_trade_ops = LlmAgent(
  name='omnis_trade_ops',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Správa nákupních procesů, velkoobchodních registrací (IČO / živnostenská agenda), kalkulace marží a správa dodavatelských databází.'
  ),
  sub_agents=[],
  instruction='Jsi analytik nákupu a podnikatelské operativy. Zpracovávej specifikace zboží, kalkuluj nákupní a prodejní ceny s DPH, kontroluj správnost fakturačních údajů a připravuj podklady pro nákupy u velkoobchodních partnerů.',
  tools=[],
)
omnis_prompt_refiner = LlmAgent(
  name='omnis_prompt_refiner',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Agent určený k auditování a průběžnému vylepšování System Promptů všech ostatních agentů v síti O.M.N.I.S.'
  ),
  sub_agents=[],
  instruction='Jsi expert na prompt engineering a AI architekturu. Analyzuj selhání v odpovědích ostatních agentů, odstraňuj nejednoznačnosti v jejich instrukcích a navrhuj optimalizované struktury JSON payloadů pro Akční dispečer.',
  tools=[],
)
omnis_core_orchestrator_google_search_agent = LlmAgent(
  name='omnis_core_orchestrator_google_search_agent',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Agent specialized in performing Google searches.'
  ),
  sub_agents=[],
  instruction='Use the GoogleSearchTool to find information on the web.',
  tools=[
    GoogleSearchTool()
  ],
)
omnis_core_orchestrator_url_context_agent = LlmAgent(
  name='omnis_core_orchestrator_url_context_agent',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Agent specialized in fetching content from URLs.'
  ),
  sub_agents=[],
  instruction='Use the UrlContextTool to retrieve content from provided URLs.',
  tools=[
    url_context
  ],
)
root_agent = LlmAgent(
  name='omnis_core_orchestrator',
  model=GlobalGemini(model='gemini-3.5-flash'),
  description=(
      'Řídicí agent provádějící multivrstvé ověření, kognitivní introspekci a směrování uživatelských požadavků.'
  ),
  sub_agents=[omnis_code_architect, omnis_legal_analyst, omnis_hardware_lab, omnis_aidc_logistics, omnis_android_ui, omnis_vector_engine, omnis_physical_security, omnis_trade_ops, omnis_prompt_refiner],
  instruction='Jsi orchestrační uzel O.M.N.I.S. Přijímej uživatelské vstupy, prováděj kognitivní introspekci (dekompozici problému) a generuj strukturovaný payload pro další zpracování.\n\n- Úkoly týkající se návrhu architektury, Python kódu, refaktorování a optimalizace databází deleguj na podagenta **omnis-code-architect**.\n- Úkoly týkající se českého práva, GDPR, reklamací a spotřebitelských sporů deleguj na podagenta **omnis-legal-analyst**.\n- Úkoly týkající se mikrokontrolerů, hardwaru, IoT a embedded systémů deleguj na podagenta **omnis-hardware-lab**.\n- Úkoly týkající se AIDC technologií, čárových kódů, generování etiket a logistických struktur deleguj na podagenta **omnis-aidc-logistics**.\n- Úkoly týkající se vývoje Android aplikací v Kotlinu, Jetpack Compose a mobilního UI deleguj na podagenta **omnis-android-ui**.\n- Úkoly týkající se RAG, pgvectoru, sémantického vyhledávání a výpočtů entropie deleguj na podagenta **omnis-vector-engine**.\n- Úkoly týkající se mechanické bezpečnosti, cylindrických vložek a norem ČSN EN 1303 deleguj na podagenta **omnis-physical-security**.\n- Úkoly týkající se nákupních procesů, kalkulace marží a dodavatelské agendy deleguj na podagenta **omnis-trade-ops**.\n- Úkoly týkající se auditování a optimalizace promptů či JSON struktur deleguj na podagenta **omnis-prompt-refiner**.\n\nSyntetizuj výstupy specializovaných agentů do jednoho uceleného a verifikovaného závěru.',
  tools=[
    agent_tool.AgentTool(agent=omnis_core_orchestrator_google_search_agent),
    agent_tool.AgentTool(agent=omnis_core_orchestrator_url_context_agent)
  ],
)

if __name__ == "__main__":
    print(f"Successfully loaded root agent: {root_agent.name}")
    print(f"Subagents ({len(root_agent.sub_agents)}): {[sa.name for sa in root_agent.sub_agents]}")
    print(f"Tools ({len(root_agent.tools)}): {[type(t).__name__ for t in root_agent.tools]}")
