import React, { useState, useEffect, useRef } from "react";
import {
  Brain,
  Cpu,
  Send,
  Sparkles,
  Layers,
  Database,
  Sliders,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  TrendingUp,
  Leaf,
  ShieldCheck,
  Heart,
  MessageSquare,
  Compass,
  Star,
  CheckCircle2,
  AlertCircle,
  Clock,
  ShieldAlert,
  Zap,
  Filter,
  FileCode,
  Check,
  X,
} from "lucide-react";

interface ImpactMatrixScores {
  economic_viability: number;
  eco_social_regeneration: number;
  technological_elegance: number;
  psychological_acceptability: number;
  composite_score: number;
  reasoning: string;
  adversarial_vulnerabilities?: string[];
  leverage_point?: string;
}

interface MessageItem {
  id: string;
  role: "user" | "assistant";
  content: string;
  cognitive_process?: string;
  follow_up_questions?: string[];
  impact_matrix?: ImpactMatrixScores;
  created_at: string;
}

interface CandidateSolution {
  candidate_id: string;
  title: string;
  description: string;
  economic: number; // 0-10
  tech: number; // 0-10
  eco: number; // 0-10
  psych: number; // 0-10
  vulnerabilities: string[];
}

export default function App() {
  const [messages, setMessages] = useState<MessageItem[]>([
    {
      id: "initial-msg",
      role: "assistant",
      content:
        "Vítejte v O.M.N.I.S. (Operativní Multimodální Nástroj pro Integrovanou Synergii).\n\n" +
        "Systém operuje jako pluriversální kognitivní engine v pětifázovém cyklu:\n" +
        "• Fáze I: Holomorfní Sběr (hard & soft data)\n" +
        "• Fáze II: Sémantická Dekonstrukce (Zero-Assumption logika)\n" +
        "• Fáze III: Transdisciplinární Křížení (modální překlad, leverage points)\n" +
        "• Fáze IV: Synergická Konvergence (Matice dopadů s adversarial guardrailem)\n" +
        "• Fáze V: Teleologická Exekuce & Autopoieza (MVS & sebekalibrace)",
      cognitive_process:
        "### Pětifázový cyklus O.M.N.I.S.\n" +
        "1. Holomorfní Sběr: Asimilace epistemických dat v doméně SYSTEMS_INTELLIGENCE.\n" +
        "2. Sémantická Dekonstrukce: Odstranění kognitivních zkreslení a dogmat; převod na fundamentální prvočinitele.\n" +
        "3. Transdisciplinární Křížení: Modální překlad parametrů. Nalezen uzlový bod (leverage point): Asynchronní orchestrace s nulovým sémantickým šumem.\n" +
        "4. Synergická Konvergence: Matice dopadů (váhy 0.3/0.3/0.2/0.2): Ekon=88%, Tech=96%, Ekol=94%, Psych=91%. Vážené skóre: 92.4%.\n" +
        "5. Teleologická Exekuce: Inicializace paměťových vektorů v pgvector.",
      follow_up_questions: [
        "Jak funguje výpočet penalizací za adversarial zranitelnosti v Fázi IV?",
        "Jak navrhnout distribuovanou architekturu s nulovou energetickou stopou?",
        "Můžeme provést modální překlad sociologického napětí do termodynamického tlaku?",
      ],
      impact_matrix: {
        economic_viability: 0.88,
        eco_social_regeneration: 0.94,
        technological_elegance: 0.96,
        psychological_acceptability: 0.91,
        composite_score: 0.924,
        reasoning:
          "Základní výchozí harmonie kognitivní sítě. Vážené skóre přesahuje 90 % bez jakýchkoliv kompromisů.",
        adversarial_vulnerabilities: [
          "Možné sycophancy zkreslení při absenci odděleného hodnotitele",
        ],
        leverage_point:
          "Zavedení asynchronního validačního uzlu na rozhraní Epistemické a Syntetické roviny",
      },
      created_at: new Date().toISOString(),
    },
  ]);

  const [inputQuery, setInputQuery] = useState("");
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [enableThinking, setEnableThinking] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"chat" | "matrix" | "guardrail" | "memory">("chat");
  const [expandedThoughts, setExpandedThoughts] = useState<Record<string, boolean>>({
    "initial-msg": true,
  });
  const [feedbackRating, setFeedbackRating] = useState<Record<string, number>>({});
  const [feedbackSubmitted, setFeedbackSubmitted] = useState<Record<string, boolean>>({});

  // Fáze IV: Guardrail Candidate Testing State
  const [candidates, setCandidates] = useState<CandidateSolution[]>([
    {
      candidate_id: "cand-1",
      title: "Asynchronní Multi-agentní pipeline O.M.N.I.S.",
      description: "Paralelní dekonstrukce vstupů specializovanými agenty se syntetickým překladem.",
      economic: 8.8,
      tech: 9.6,
      eco: 9.0,
      psych: 9.2,
      vulnerabilities: ["Sycophancy bias u nekritických výstupů"],
    },
    {
      candidate_id: "cand-2",
      title: "Monolitická synchronní pipeline",
      description: "Sekvenční zpracování v jediném velkém kontextovém okně bez kognitivní diverzity.",
      economic: 6.5,
      tech: 5.8,
      eco: 6.0,
      psych: 6.2,
      vulnerabilities: [
        "Vysoká latence",
        "Riziko halucinací při dlouhém kontextu",
        "Chybí modální překlad",
      ],
    },
  ]);
  const [guardrailResults, setGuardrailResults] = useState<any>(null);
  const [evaluatingGuardrail, setEvaluatingGuardrail] = useState(false);

  // Real Database Memory & Epistemic Layer state
  const [memories, setMemories] = useState<any[]>([]);
  const [loadingMemories, setLoadingMemories] = useState(false);
  const [epistemicPurpose, setEpistemicPurpose] = useState("Optimalizace energetického toku v distribuované síti");
  const [epistemicDomain, setEpistemicDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [epistemicHardParam, setEpistemicHardParam] = useState("efficiency: 94.2%, latency_ms: 12");
  const [epistemicSoftParam, setEpistemicSoftParam] = useState("sociological_vector: low_friction, psychological_state: high_trust");
  const [epistemicSensoryParam, setEpistemicSensoryParam] = useState("intuition_notes: Asynchronní uzel eliminuje degradaci");
  const [ingestStatus, setIngestStatus] = useState<string | null>(null);
  const [isIngesting, setIsIngesting] = useState(false);

  // Backend health verification state
  const [backendHealth, setBackendHealth] = useState<"checking" | "connected" | "disconnected">("checking");
  const [backendLatency, setBackendLatency] = useState<number | null>(null);

  const fetchRealMemories = async () => {
    setLoadingMemories(true);
    try {
      const resp = await fetch("/api/memory");
      if (resp.ok) {
        const data = await resp.json();
        setMemories(data);
      }
    } catch (err) {
      console.error("Chyba při načítání paměti z backendu:", err);
    } finally {
      setLoadingMemories(false);
    }
  };

  useEffect(() => {
    const checkBackendHealth = async () => {
      const startTime = performance.now();
      try {
        const resp = await fetch("/api/health-check");
        if (resp.ok) {
          const latency = Math.round(performance.now() - startTime);
          setBackendLatency(latency);
          setBackendHealth("connected");
        } else {
          setBackendHealth("disconnected");
        }
      } catch (err) {
        setBackendHealth("disconnected");
      }
    };

    checkBackendHealth();
    fetchRealMemories();
  }, []);

  const handleIngestEpistemic = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsIngesting(true);
    setIngestStatus(null);
    try {
      const payload = {
        omnis_entity_id: `omnis-${Date.now().toString(36)}`,
        fundamental_purpose: epistemicPurpose,
        ontology_domain: epistemicDomain,
        epistemic_data_layer: {
          hard_data: {
            description: "Tvrdá numerická telemetrie",
            strict_typing: true,
            parameters: { metrics: epistemicHardParam },
          },
          soft_data: {
            description: "Sociální a psychologické atributy",
            strict_typing: false,
            parameters: { context: epistemicSoftParam },
          },
          sensory_heuristic: {
            description: "Senzorické vstupy a heuristické poznámky",
            strict_typing: false,
            parameters: { heuristic: epistemicSensoryParam },
          },
        },
      };

      const resp = await fetch("/api/epistemic/ingest", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (resp.ok) {
        const resData = await resp.json();
        setIngestStatus(`✅ Úspěšně uloženo do pgvector (ID: ${resData.record_id || resData.status})`);
        await fetchRealMemories();
      } else {
        const err = await resp.json().catch(() => null);
        setIngestStatus(`⚠️ Chyba serveru (${resp.status}): ${err?.detail || resp.statusText}`);
      }
    } catch (e: any) {
      setIngestStatus(`⚠️ Chyba spojení: ${e?.message}`);
    } finally {
      setIsIngesting(false);
    }
  };

  // Active matrix being displayed or inspected in detail
  const latestMatrix =
    [...messages].reverse().find((m) => m.impact_matrix)?.impact_matrix ||
    messages[0].impact_matrix!;

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  const toggleThoughts = (id: string) => {
    setExpandedThoughts((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleSendQuery = async (queryToSend?: string) => {
    const text = (queryToSend || inputQuery).trim();
    if (!text || isLoading) return;

    const userMessage: MessageItem = {
      id: "user-" + Date.now(),
      role: "user",
      content: text,
      created_at: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputQuery("");
    setIsLoading(true);

    try {
      const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: text,
          ontology_domain: ontologyDomain,
          enable_thinking: enableThinking,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();

      const assistantMessage: MessageItem = {
        id: data.message_id || "asst-" + Date.now(),
        role: "assistant",
        content: data.answer,
        cognitive_process: data.cognitive_process,
        follow_up_questions: data.follow_up_questions,
        impact_matrix: data.impact_matrix,
        created_at: data.created_at || new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
      setExpandedThoughts((prev) => ({ ...prev, [assistantMessage.id]: true }));
    } catch (error: any) {
      const errMsg: MessageItem = {
        id: "err-" + Date.now(),
        role: "assistant",
        content: `⚠️ **Chyba O.M.N.I.S. API:** ${error?.message || "Spojení se serverem selhalo"}.\n\nSystém striktně odmítá vracet fiktivní či simulovaná data. Zkontrolujte prosím připojení k serveru a konfiguraci GEMINI_API_KEY v Secrets panelu AI Studio.`,
        cognitive_process: `Volání backendu selhalo: ${error?.message || "Neznámá chyba"}. Žádná simulace nebyla vygenerována.`,
        follow_up_questions: ["Opakovat dotaz?", "Zkontrolovat stav serveru?"],
        created_at: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRate = async (messageId: string, rating: number) => {
    setFeedbackRating((prev) => ({ ...prev, [messageId]: rating }));
    try {
      await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message_id: messageId,
          conversation_id: "00000000-0000-0000-0000-000000000000",
          user_rating: rating,
          feedback_text: `Uživatel ohodnotil odpověď jako ${rating}/5`,
        }),
      });
      setFeedbackSubmitted((prev) => ({ ...prev, [messageId]: true }));
    } catch {
      setFeedbackSubmitted((prev) => ({ ...prev, [messageId]: true }));
    }
  };

  // Run Phase IV Guardrail Evaluation
  const runGuardrailEvaluation = async () => {
    setEvaluatingGuardrail(true);
    const payload = {
      candidates: candidates.map((c) => ({
        candidate_id: c.candidate_id,
        title: c.title,
        description: c.description,
        scores: {
          economic_viability: c.economic,
          tech_elegance: c.tech,
          social_ecological_impact: c.eco,
          psychological_acceptance: c.psych,
        },
        adversarial_vulnerabilities: c.vulnerabilities,
      })),
      minimum_threshold: 7.0,
    };

    try {
      const resp = await fetch("/omnis/phase-4/evaluate-matrix", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (resp.ok) {
        const data = await resp.json();
        setGuardrailResults(data);
      } else {
        const err = await resp.json().catch(() => null);
        setGuardrailResults({
          selected_optimal_candidate: null,
          weighted_rankings: [],
          status: `Chyba API (${resp.status}): ${err?.detail || resp.statusText}. Žádná simulace nebyla provedena.`,
          error: true,
        });
      }
    } catch (err: any) {
      setGuardrailResults({
        selected_optimal_candidate: null,
        weighted_rankings: [],
        status: `Chyba připojení k /omnis/phase-4/evaluate-matrix: ${err?.message || "Server nedostupný"}.`,
        error: true,
      });
    } finally {
      setEvaluatingGuardrail(false);
    }
  };

  return (
    <div className="flex h-screen w-full bg-[#080c14] text-slate-100 overflow-hidden font-sans">
      {/* LEFT COLUMN: Sidebar Navigation & System Telemetry */}
      <aside className="w-80 border-r border-[#1e293b] bg-[#0b1120]/80 backdrop-blur-md flex flex-col justify-between p-5 hidden md:flex">
        <div className="space-y-6">
          {/* Brand Header */}
          <div className="flex items-center space-x-3">
            <div className="h-11 w-11 rounded-xl bg-gradient-to-tr from-[#7C4DFF] to-[#00E5FF] p-0.5 shadow-lg shadow-[#00E5FF]/20 flex items-center justify-center">
              <div className="h-full w-full bg-[#090D16] rounded-[10px] flex items-center justify-center">
                <Brain className="h-6 w-6 text-[#00E5FF]" />
              </div>
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-wider text-white flex items-center gap-2">
                O.M.N.I.S.
                <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30">
                  SIGMA v2.5
                </span>
              </h1>
              <p className="text-xs text-slate-400 font-mono">Pluriversal Resonance Engine</p>
            </div>
          </div>

          {/* Ontological Domain Selector */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
              <Compass className="h-3.5 w-3.5 text-[#00E5FF]" />
              Ontologická Doména
            </label>
            <select
              value={ontologyDomain}
              onChange={(e) => setOntologyDomain(e.target.value)}
              className="w-full bg-[#0f172a] border border-[#1e293b] text-xs rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-[#00E5FF] transition-all"
            >
              <option value="SYSTEMS_INTELLIGENCE">Systémová Inteligence</option>
              <option value="CYBERNETIC_SYNTHESIS">Kybernetická Syntéza</option>
              <option value="REGENERATIVE_TECH">Regenerativní Technologie</option>
              <option value="COGNITIVE_ARCHITECTURE">Kognitivní Architektura</option>
            </select>
          </div>

          {/* Thinking Level Toggle */}
          <div className="p-3.5 rounded-xl bg-[#0f172a]/70 border border-[#1e293b] space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-300 flex items-center gap-1.5">
                <Sparkles className="h-3.5 w-3.5 text-[#7C4DFF]" />
                Hloubková Introspekce
              </span>
              <button
                type="button"
                onClick={() => setEnableThinking(!enableThinking)}
                className={`w-9 h-5 flex items-center rounded-full p-1 transition-colors ${
                  enableThinking ? "bg-[#00E5FF]" : "bg-slate-700"
                }`}
              >
                <div
                  className={`bg-black w-3.5 h-3.5 rounded-full shadow-md transform transition-transform ${
                    enableThinking ? "translate-x-4" : "translate-x-0"
                  }`}
                />
              </button>
            </div>
            <p className="text-[11px] text-slate-400">
              Aktivuje reflexivní myšlenkový proud a kognitivní řetězec invariantů.
            </p>
          </div>

          {/* Navigation Tabs (Aligned with Blueprint) */}
          <div className="space-y-1">
            <button
              onClick={() => setActiveTab("chat")}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === "chat"
                  ? "bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30 shadow-sm"
                  : "text-slate-400 hover:text-slate-200 hover:bg-[#0f172a]"
              }`}
            >
              <MessageSquare className="h-4 w-4" />
              Kognitivní Chat & Fáze
            </button>

            <button
              onClick={() => setActiveTab("matrix")}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === "matrix"
                  ? "bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30 shadow-sm"
                  : "text-slate-400 hover:text-slate-200 hover:bg-[#0f172a]"
              }`}
            >
              <Sliders className="h-4 w-4" />
              Matice Dopadů (4D)
            </button>

            <button
              onClick={() => setActiveTab("guardrail")}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === "guardrail"
                  ? "bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30 shadow-sm"
                  : "text-slate-400 hover:text-slate-200 hover:bg-[#0f172a]"
              }`}
            >
              <ShieldCheck className="h-4 w-4" />
              Fáze IV: Guardrail Validátor
            </button>

            <button
              onClick={() => setActiveTab("memory")}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === "memory"
                  ? "bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30 shadow-sm"
                  : "text-slate-400 hover:text-slate-200 hover:bg-[#0f172a]"
              }`}
            >
              <Database className="h-4 w-4" />
              Epistemická & Vektorová Paměť
            </button>
          </div>
        </div>

        {/* Live Telemetry Status Box */}
        <div className="p-3.5 rounded-xl bg-[#090d16] border border-[#1e293b] space-y-2">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Architektura:</span>
            <span className="text-emerald-400 font-semibold flex items-center gap-1">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              SIGMA-OMEGA
            </span>
          </div>
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Backend API:</span>
            {backendHealth === "checking" && (
              <span className="text-amber-400 flex items-center gap-1">
                <span className="h-2 w-2 rounded-full bg-amber-400 animate-ping" />
                Ověřuji...
              </span>
            )}
            {backendHealth === "connected" && (
              <span className="text-emerald-400 font-semibold flex items-center gap-1">
                <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                ONLINE {backendLatency !== null ? `(${backendLatency}ms)` : ""}
              </span>
            )}
            {backendHealth === "disconnected" && (
              <span className="text-rose-400 font-semibold flex items-center gap-1">
                <span className="h-2 w-2 rounded-full bg-rose-500" />
                OFFLINE
              </span>
            )}
          </div>
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Guardrail váhy:</span>
            <span className="text-[#00E5FF]">30/30/20/20</span>
          </div>
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Vektorový prostor:</span>
            <span className="text-[#7C4DFF] font-bold">pgvector 768d</span>
          </div>
        </div>
      </aside>

      {/* CENTER COLUMN: Main Content Area */}
      <main className="flex-1 flex flex-col h-full overflow-hidden bg-[#080c14]">
        {/* Top App Bar */}
        <header className="h-16 border-b border-[#1e293b] bg-[#0b1120]/60 backdrop-blur-md px-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="h-2.5 w-2.5 rounded-full bg-[#00E5FF]" />
            <h2 className="text-sm font-semibold tracking-wide text-slate-200">
              {activeTab === "chat" && "Pětifázový kognitivní cyklus O.M.N.I.S. (Fáze I až V)"}
              {activeTab === "matrix" && "Matice dopadů: 4 ortogonální dimenze & kompozitní index"}
              {activeTab === "guardrail" && "Fáze IV: Synergická Konvergence – Výstupní Guardrail"}
              {activeTab === "memory" && "Epistemická rovina & Vektorová paměť (PostgreSQL + pgvector)"}
            </h2>
          </div>

          <div className="flex items-center gap-4 text-xs font-mono text-slate-400">
            <span className="hidden sm:inline-block px-2.5 py-1 rounded bg-[#0f172a] border border-[#1e293b]">
              POST /omnis/phase-4/evaluate-matrix
            </span>
            <span className="hidden sm:inline-block px-2.5 py-1 rounded bg-[#0f172a] border border-[#1e293b]">
              Google GenAI SDK
            </span>
          </div>
        </header>

        {/* Tab 1: Chat Stream */}
        {activeTab === "chat" && (
          <div className="flex-1 flex flex-col overflow-hidden">
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex flex-col ${
                    msg.role === "user" ? "items-end" : "items-start"
                  }`}
                >
                  {/* Message Bubble */}
                  <div
                    className={`max-w-3xl rounded-2xl p-5 shadow-lg ${
                      msg.role === "user"
                        ? "bg-gradient-to-r from-[#00E5FF]/20 to-[#7C4DFF]/20 border border-[#00E5FF]/40 text-slate-100 rounded-br-none"
                        : "bg-[#0f172a]/90 border border-[#1e293b] text-slate-200 rounded-bl-none"
                    }`}
                  >
                    {/* Role Tag & Time */}
                    <div className="flex items-center justify-between gap-4 mb-3 border-b border-[#1e293b]/60 pb-2">
                      <span className="text-xs font-mono font-semibold uppercase tracking-wider flex items-center gap-2">
                        {msg.role === "user" ? (
                          <>
                            <span className="h-2 w-2 rounded-full bg-[#00E5FF]" />
                            Operátor
                          </>
                        ) : (
                          <>
                            <Brain className="h-3.5 w-3.5 text-[#7C4DFF]" />
                            O.M.N.I.S. Core [SIGMA-OMEGA]
                          </>
                        )}
                      </span>
                      <span className="text-[11px] font-mono text-slate-500">
                        {new Date(msg.created_at).toLocaleTimeString()}
                      </span>
                    </div>

                    {/* Introspection Box (Collapsible Thoughts) */}
                    {msg.cognitive_process && (
                      <div className="mb-4 rounded-xl bg-[#090d16]/90 border border-[#1e293b] overflow-hidden">
                        <button
                          type="button"
                          onClick={() => toggleThoughts(msg.id)}
                          className="w-full flex items-center justify-between px-3.5 py-2 text-xs font-mono text-slate-400 hover:text-slate-200 bg-[#0f172a]/50"
                        >
                          <span className="flex items-center gap-2 text-[#00E5FF]">
                            <Cpu className="h-3.5 w-3.5" />
                            Kognitivní introspekce & Pětifázový cyklus
                          </span>
                          {expandedThoughts[msg.id] ? (
                            <ChevronUp className="h-3.5 w-3.5" />
                          ) : (
                            <ChevronDown className="h-3.5 w-3.5" />
                          )}
                        </button>
                        {expandedThoughts[msg.id] && (
                          <div className="p-3.5 text-xs font-mono text-slate-300 whitespace-pre-line border-t border-[#1e293b]/60 bg-[#080c14]/50 leading-relaxed">
                            {msg.cognitive_process}
                          </div>
                        )}
                      </div>
                    )}

                    {/* Leverage Point Banner */}
                    {msg.impact_matrix?.leverage_point && (
                      <div className="mb-3 p-3 rounded-lg bg-[#00E5FF]/10 border border-[#00E5FF]/30 flex items-start gap-2.5">
                        <Zap className="h-4 w-4 text-[#00E5FF] mt-0.5 flex-shrink-0" />
                        <div>
                          <span className="text-[10px] font-mono uppercase font-bold text-[#00E5FF]">
                            Uzlový Bod (Leverage Point)
                          </span>
                          <p className="text-xs text-slate-200 mt-0.5">
                            {msg.impact_matrix.leverage_point}
                          </p>
                        </div>
                      </div>
                    )}

                    {/* Main Content */}
                    <div className="text-sm leading-relaxed whitespace-pre-line prose prose-invert max-w-none">
                      {msg.content}
                    </div>

                    {/* Adversarial Vulnerabilities Alert */}
                    {msg.impact_matrix?.adversarial_vulnerabilities &&
                      msg.impact_matrix.adversarial_vulnerabilities.length > 0 && (
                        <div className="mt-3 p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 space-y-1">
                          <span className="text-[10px] font-mono uppercase font-bold text-amber-400 flex items-center gap-1.5">
                            <ShieldAlert className="h-3.5 w-3.5" />
                            Adversarial Red-Teaming (Rizika selhání)
                          </span>
                          <ul className="text-xs text-slate-300 list-disc list-inside space-y-0.5">
                            {msg.impact_matrix.adversarial_vulnerabilities.map((v, i) => (
                              <li key={i}>{v}</li>
                            ))}
                          </ul>
                        </div>
                      )}

                    {/* Mini Impact Matrix Preview Bar */}
                    {msg.impact_matrix && (
                      <div className="mt-4 pt-4 border-t border-[#1e293b]/60 grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] font-mono">
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Ekonomika (0.3):</span>
                          <span className="text-emerald-400 font-bold">
                            {(msg.impact_matrix.economic_viability * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Technologie (0.3):</span>
                          <span className="text-cyan-400 font-bold">
                            {(msg.impact_matrix.technological_elegance * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Ekologie (0.2):</span>
                          <span className="text-teal-400 font-bold">
                            {(msg.impact_matrix.eco_social_regeneration * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Psychologie (0.2):</span>
                          <span className="text-indigo-400 font-bold">
                            {(msg.impact_matrix.psychological_acceptability * 100).toFixed(0)}%
                          </span>
                        </div>
                      </div>
                    )}

                    {/* Follow-up Questions Suggestions */}
                    {msg.follow_up_questions && msg.follow_up_questions.length > 0 && (
                      <div className="mt-4 space-y-2">
                        <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block">
                          Reflexivní doplňující otázky:
                        </span>
                        <div className="flex flex-wrap gap-2">
                          {msg.follow_up_questions.map((q, idx) => (
                            <button
                              key={idx}
                              onClick={() => handleSendQuery(q)}
                              className="text-xs text-left bg-[#0b1120] hover:bg-[#00E5FF]/10 text-slate-300 hover:text-[#00E5FF] px-3 py-1.5 rounded-lg border border-[#1e293b] hover:border-[#00E5FF]/40 transition-all font-sans"
                            >
                              → {q}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Autopoietic Feedback Rating */}
                    {msg.role === "assistant" && (
                      <div className="mt-4 pt-3 border-t border-[#1e293b]/40 flex items-center justify-between text-xs">
                        <span className="text-slate-500 font-mono">Autopoietická valence:</span>
                        {feedbackSubmitted[msg.id] ? (
                          <span className="text-emerald-400 flex items-center gap-1 font-mono text-[11px]">
                            <CheckCircle2 className="h-3.5 w-3.5" /> Otisk uložen
                          </span>
                        ) : (
                          <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <button
                                key={star}
                                onClick={() => handleRate(msg.id, star)}
                                className={`p-1 transition-colors ${
                                  (feedbackRating[msg.id] || 0) >= star
                                    ? "text-[#00E5FF]"
                                    : "text-slate-600 hover:text-slate-400"
                                }`}
                              >
                                <Star className="h-3.5 w-3.5 fill-current" />
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {isLoading && (
                <div className="flex items-center space-x-3 p-4 bg-[#0f172a]/60 rounded-2xl border border-[#1e293b] max-w-sm animate-pulse">
                  <RefreshCw className="h-4 w-4 text-[#00E5FF] animate-spin" />
                  <span className="text-xs font-mono text-slate-300">
                    Probíhá pětifázová syntéza SIGMA & výpočet tenzorů...
                  </span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Bar */}
            <div className="p-4 border-t border-[#1e293b] bg-[#0b1120]/90">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendQuery();
                }}
                className="max-w-4xl mx-auto flex items-center gap-3"
              >
                <div className="relative flex-1">
                  <input
                    type="text"
                    value={inputQuery}
                    onChange={(e) => setInputQuery(e.target.value)}
                    placeholder="Zadejte otázku nebo systémový problém k analýze v O.M.N.I.S..."
                    disabled={isLoading}
                    className="w-full bg-[#0f172a] border border-[#1e293b] rounded-xl px-4 py-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-[#00E5FF] transition-all shadow-inner"
                  />
                </div>
                <button
                  type="submit"
                  disabled={!inputQuery.trim() || isLoading}
                  className="px-5 py-3 rounded-xl bg-gradient-to-r from-[#00E5FF] to-[#7C4DFF] text-black font-semibold text-sm hover:opacity-95 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center gap-2 shadow-lg shadow-[#00E5FF]/20"
                >
                  <Send className="h-4 w-4" />
                  <span>Syntetizovat</span>
                </button>
              </form>
            </div>
          </div>
        )}

        {/* Tab 2: Impact Matrix 4D Visualizer */}
        {activeTab === "matrix" && (
          <div className="flex-1 overflow-y-auto p-8 space-y-8 max-w-5xl mx-auto w-full">
            {/* Header & Composite Score Banner */}
            <div className="p-6 rounded-2xl bg-gradient-to-r from-[#0f172a] via-[#111c38] to-[#0f172a] border border-[#1e293b] flex flex-col md:flex-row items-center justify-between gap-6 shadow-xl">
              <div>
                <span className="text-xs font-mono uppercase tracking-widest text-[#00E5FF] font-semibold">
                  Čtyřdimenzionální model (Váhy 30/30/20/20)
                </span>
                <h3 className="text-2xl font-bold text-white mt-1">Matice Dopadů O.M.N.I.S.</h3>
                <p className="text-xs text-slate-400 mt-2 max-w-xl">
                  Holistické hodnocení řešení bez kompromisů napříč 4 ortogonálními osami:
                  ekonomická (0.3), technologická (0.3), ekologicko-sociální (0.2) a psychologická (0.2).
                </p>
              </div>

              <div className="text-center p-4 rounded-xl bg-[#090d16] border border-[#00E5FF]/30 min-w-[160px]">
                <span className="text-xs font-mono text-slate-400 uppercase">Integrální Index</span>
                <div className="text-4xl font-extrabold text-[#00E5FF] mt-1 font-mono">
                  {(latestMatrix.composite_score * 100).toFixed(1)}%
                </div>
                <span className="text-[10px] text-emerald-400 font-mono">STABILNÍ HARMONIE</span>
              </div>
            </div>

            {/* 4 Interactive Dimension Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Dimension 1: Economic (Weight 0.3) */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <TrendingUp className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Ekonomická Životaschopnost</h4>
                      <p className="text-[11px] text-slate-400">Váha 0.3 | Efektivita nákladů a návratnost</p>
                    </div>
                  </div>
                  <span className="text-xl font-bold font-mono text-emerald-400">
                    {(latestMatrix.economic_viability * 100).toFixed(0)}%
                  </span>
                </div>
                <div className="w-full bg-[#090d16] rounded-full h-3 overflow-hidden border border-[#1e293b]">
                  <div
                    className="bg-emerald-400 h-full rounded-full transition-all duration-700"
                    style={{ width: `${latestMatrix.economic_viability * 100}%` }}
                  />
                </div>
              </div>

              {/* Dimension 2: Technological Elegance (Weight 0.3) */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-cyan-500/10 text-[#00E5FF] border border-cyan-500/20">
                      <Cpu className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Technologická Elegance</h4>
                      <p className="text-[11px] text-slate-400">Váha 0.3 | Modularita, čistota a determinismus</p>
                    </div>
                  </div>
                  <span className="text-xl font-bold font-mono text-[#00E5FF]">
                    {(latestMatrix.technological_elegance * 100).toFixed(0)}%
                  </span>
                </div>
                <div className="w-full bg-[#090d16] rounded-full h-3 overflow-hidden border border-[#1e293b]">
                  <div
                    className="bg-[#00E5FF] h-full rounded-full transition-all duration-700"
                    style={{ width: `${latestMatrix.technological_elegance * 100}%` }}
                  />
                </div>
              </div>

              {/* Dimension 3: Ecological & Social (Weight 0.2) */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-teal-500/10 text-teal-400 border border-teal-500/20">
                      <Leaf className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Ekologicko-Sociální Regenerace</h4>
                      <p className="text-[11px] text-slate-400">Váha 0.2 | Udržitelnost a regenerace biosféry</p>
                    </div>
                  </div>
                  <span className="text-xl font-bold font-mono text-teal-400">
                    {(latestMatrix.eco_social_regeneration * 100).toFixed(0)}%
                  </span>
                </div>
                <div className="w-full bg-[#090d16] rounded-full h-3 overflow-hidden border border-[#1e293b]">
                  <div
                    className="bg-teal-400 h-full rounded-full transition-all duration-700"
                    style={{ width: `${latestMatrix.eco_social_regeneration * 100}%` }}
                  />
                </div>
              </div>

              {/* Dimension 4: Psychological & Ethical Acceptability (Weight 0.2) */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      <Heart className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Psychologická Přijatelnost</h4>
                      <p className="text-[11px] text-slate-400">Váha 0.2 | Etický dopad a kognitivní ergonomie</p>
                    </div>
                  </div>
                  <span className="text-xl font-bold font-mono text-indigo-400">
                    {(latestMatrix.psychological_acceptability * 100).toFixed(0)}%
                  </span>
                </div>
                <div className="w-full bg-[#090d16] rounded-full h-3 overflow-hidden border border-[#1e293b]">
                  <div
                    className="bg-indigo-400 h-full rounded-full transition-all duration-700"
                    style={{ width: `${latestMatrix.psychological_acceptability * 100}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Matrix Reasoning Card */}
            <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-3">
              <h4 className="text-xs font-mono uppercase tracking-wider text-slate-400 flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-[#00E5FF]" />
                Zdůvodnění a ontologická analýza
              </h4>
              <p className="text-sm text-slate-300 leading-relaxed font-sans bg-[#090d16] p-4 rounded-xl border border-[#1e293b]">
                {latestMatrix.reasoning}
              </p>
            </div>
          </div>
        )}

        {/* Tab 3: Fáze IV Guardrail Validator */}
        {activeTab === "guardrail" && (
          <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-5xl mx-auto w-full">
            <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div>
                <span className="text-xs font-mono uppercase tracking-widest text-[#00E5FF] font-semibold">
                  Blueprint Fáze IV (str. 9-11)
                </span>
                <h3 className="text-xl font-bold text-white mt-1">
                  Výstupní Guardrail & Adversarial Penalizace
                </h3>
                <p className="text-xs text-slate-400 mt-1 max-w-xl">
                  Endpoint <code className="text-[#00E5FF]">/omnis/phase-4/evaluate-matrix</code> aplikuje
                  váhy 0.3/0.3/0.2/0.2 a penalizaci <code className="text-amber-400">-0.5 bodu</code> za každou
                  zranitelnost. Práh akceptace: <span className="text-[#00E5FF] font-mono">7.0</span> / 10.0.
                </p>
              </div>

              <button
                onClick={runGuardrailEvaluation}
                disabled={evaluatingGuardrail}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#00E5FF] to-[#7C4DFF] text-black font-semibold text-xs flex items-center gap-2 hover:opacity-90 disabled:opacity-50"
              >
                {evaluatingGuardrail ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <ShieldCheck className="h-4 w-4" />
                )}
                Spustit Guardrail Validaci
              </button>
            </div>

            {/* Candidates List */}
            <div className="space-y-4">
              <h4 className="text-xs font-mono uppercase tracking-wider text-slate-400">
                Návrhy kandidátů z Fáze III
              </h4>

              {candidates.map((cand, idx) => (
                <div
                  key={cand.candidate_id}
                  className="p-5 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-3 font-mono"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-white font-sans flex items-center gap-2">
                      <span className="text-[#00E5FF]">#{idx + 1}</span> {cand.title}
                    </span>
                    <span className="text-xs text-slate-400">ID: {cand.candidate_id}</span>
                  </div>
                  <p className="text-xs text-slate-300 font-sans">{cand.description}</p>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs pt-2">
                    <div className="p-2 rounded bg-[#090d16] border border-[#1e293b]">
                      <span className="text-slate-400 block text-[10px]">Ekon (0.3):</span>
                      <span className="text-emerald-400 font-bold">{cand.economic} / 10</span>
                    </div>
                    <div className="p-2 rounded bg-[#090d16] border border-[#1e293b]">
                      <span className="text-slate-400 block text-[10px]">Tech (0.3):</span>
                      <span className="text-cyan-400 font-bold">{cand.tech} / 10</span>
                    </div>
                    <div className="p-2 rounded bg-[#090d16] border border-[#1e293b]">
                      <span className="text-slate-400 block text-[10px]">Ekol (0.2):</span>
                      <span className="text-teal-400 font-bold">{cand.eco} / 10</span>
                    </div>
                    <div className="p-2 rounded bg-[#090d16] border border-[#1e293b]">
                      <span className="text-slate-400 block text-[10px]">Psych (0.2):</span>
                      <span className="text-indigo-400 font-bold">{cand.psych} / 10</span>
                    </div>
                  </div>

                  {cand.vulnerabilities.length > 0 && (
                    <div className="text-xs text-amber-400 bg-amber-500/10 p-2.5 rounded border border-amber-500/20">
                      <span className="font-bold block text-[10px] uppercase">
                        Adversarial Zranitelnosti ({cand.vulnerabilities.length}x = penalizace -
                        {cand.vulnerabilities.length * 0.5}):
                      </span>
                      <span className="text-slate-300">{cand.vulnerabilities.join(", ")}</span>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Guardrail Output Results */}
            {guardrailResults && (
              <div className="p-6 rounded-2xl bg-[#090d16] border border-[#00E5FF]/40 space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono font-bold uppercase tracking-wider text-[#00E5FF] flex items-center gap-2">
                    <CheckCircle2 className="h-4 w-4" />
                    Výsledek Guardrailu: {guardrailResults.status}
                  </span>
                  {guardrailResults.selected_optimal_candidate && (
                    <span className="px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-mono">
                      OPTIMÁLNÍ ŘEŠENÍ VYBRÁNO
                    </span>
                  )}
                </div>

                <div className="space-y-2 font-mono text-xs">
                  {guardrailResults.weighted_rankings.map((r: any) => (
                    <div
                      key={r.candidate_id}
                      className={`p-3 rounded-lg border flex items-center justify-between ${
                        r.passed
                          ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-300"
                          : "bg-red-500/10 border-red-500/30 text-red-300"
                      }`}
                    >
                      <div>
                        <span className="font-bold">{r.title}</span>
                        <div className="text-[11px] text-slate-400">
                          Před penalizací: {r.avg_before_penalty} | Zranitelnosti: {r.vulnerabilities_count}x
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-sm font-bold">Skóre: {r.final_score} / 10</div>
                        <span className="text-[10px] font-bold">
                          {r.passed ? "PROŠLO GUARDRAILEM" : "ZABLOKOVÁNO POD 7.0"}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab 4: Semantic Vector Memory (pgvector) & Epistemic Ingest */}
        {activeTab === "memory" && (
          <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-5xl mx-auto w-full">
            <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b]">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Database className="h-5 w-5 text-[#00E5FF]" />
                Epistemická & Vektorová Paměť (pgvector)
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Uchovává historické kontexty, precedensy a tenzorové matice dopadů v 768-dimenzionálním prostoru.
                Využívá operátor kosinové vzdálenosti <code className="text-[#00E5FF]">&lt;=&gt;</code> pro sémantické vyhledávání.
              </p>
            </div>

            {/* Epistemic Schema Representation (Blueprint Page 3-4) */}
            <div className="p-5 rounded-2xl bg-[#090d16] border border-[#1e293b] space-y-2">
              <span className="text-xs font-mono uppercase text-[#00E5FF] font-bold flex items-center gap-1.5">
                <FileCode className="h-4 w-4" />
                Epistemický Datový Model (JSON Schema Blueprint str. 3-4)
              </span>
              <pre className="text-[11px] font-mono text-slate-300 bg-[#080c14] p-3 rounded-lg overflow-x-auto border border-[#1e293b]/60">
{`{
  "omnis_entity_id": "omnis-epistemic-01",
  "fundamental_purpose": "string_semantic_deconstruction",
  "epistemic_data_layer": {
    "hard_data": {
      "description": "Tvrdá numerická data - statistiky, fyzikální měření",
      "strict_typing": true,
      "parameters": { "metric_name": "value_and_unit" }
    },
    "soft_data": {
      "description": "Měkká data - lidské emoce, sociokulturní kontext",
      "strict_typing": false,
      "parameters": { "sociological_vector": "string", "psychological_state": "string" }
    },
    "sensory_heuristic": {
      "description": "Senzorické vjemy i intuitivní heuristika",
      "strict_typing": false,
      "parameters": { "intuition_notes": "string", "sensory_input_raw": "string" }
    }
  }
}`}
              </pre>
            </div>

            {/* Epistemic Real Ingestion Form */}
            <div className="p-5 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase text-[#00E5FF] font-bold flex items-center gap-1.5">
                  <Database className="h-4 w-4" />
                  Aktivní Ingestace do Epistemické Roviny (POST /api/epistemic/ingest)
                </span>
                <span className="text-[11px] text-slate-400 font-mono">Zero-Simulation Protocol</span>
              </div>
              <form onSubmit={handleIngestEpistemic} className="space-y-3">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] font-mono text-slate-400">Fundamentální účel:</label>
                    <input
                      type="text"
                      value={epistemicPurpose}
                      onChange={(e) => setEpistemicPurpose(e.target.value)}
                      className="w-full bg-[#080c14] border border-[#1e293b] rounded-lg px-3 py-2 text-xs text-slate-200 mt-1 focus:outline-none focus:border-[#00E5FF]"
                      required
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-mono text-slate-400">Ontologická doména:</label>
                    <select
                      value={epistemicDomain}
                      onChange={(e) => setEpistemicDomain(e.target.value)}
                      className="w-full bg-[#080c14] border border-[#1e293b] rounded-lg px-3 py-2 text-xs text-slate-200 mt-1 focus:outline-none focus:border-[#00E5FF]"
                    >
                      <option value="SYSTEMS_INTELLIGENCE">SYSTEMS_INTELLIGENCE</option>
                      <option value="CYBERNETIC_SYNTHESIS">CYBERNETIC_SYNTHESIS</option>
                      <option value="REGENERATIVE_TECH">REGENERATIVE_TECH</option>
                      <option value="QUANTUM_LOGIC">QUANTUM_LOGIC</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div>
                    <label className="text-[11px] font-mono text-slate-400">Hard Data (statistika, metriky):</label>
                    <input
                      type="text"
                      value={epistemicHardParam}
                      onChange={(e) => setEpistemicHardParam(e.target.value)}
                      className="w-full bg-[#080c14] border border-[#1e293b] rounded-lg px-3 py-2 text-xs text-slate-200 mt-1 focus:outline-none focus:border-[#00E5FF]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-mono text-slate-400">Soft Data (sociální/emoční stav):</label>
                    <input
                      type="text"
                      value={epistemicSoftParam}
                      onChange={(e) => setEpistemicSoftParam(e.target.value)}
                      className="w-full bg-[#080c14] border border-[#1e293b] rounded-lg px-3 py-2 text-xs text-slate-200 mt-1 focus:outline-none focus:border-[#00E5FF]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-mono text-slate-400">Sensory Heuristic (intuitivní vjem):</label>
                    <input
                      type="text"
                      value={epistemicSensoryParam}
                      onChange={(e) => setEpistemicSensoryParam(e.target.value)}
                      className="w-full bg-[#080c14] border border-[#1e293b] rounded-lg px-3 py-2 text-xs text-slate-200 mt-1 focus:outline-none focus:border-[#00E5FF]"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between pt-2">
                  <button
                    type="submit"
                    disabled={isIngesting}
                    className="px-4 py-2 bg-gradient-to-r from-[#00E5FF] to-[#7C4DFF] text-black font-semibold text-xs rounded-lg hover:opacity-90 transition-all flex items-center gap-2"
                  >
                    {isIngesting ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Database className="h-3.5 w-3.5" />}
                    <span>Uložit reálná data do pgvector</span>
                  </button>
                  {ingestStatus && (
                    <span className="text-xs font-mono text-slate-300">{ingestStatus}</span>
                  )}
                </div>
              </form>
            </div>

            {/* Live Database Memory Records */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-mono uppercase text-slate-400 tracking-wider flex items-center gap-2">
                  Reálné paměťové otisky v databázi ({memories.length})
                </h4>
                <button
                  onClick={fetchRealMemories}
                  disabled={loadingMemories}
                  className="text-xs font-mono text-[#00E5FF] hover:underline flex items-center gap-1"
                >
                  <RefreshCw className={`h-3 w-3 ${loadingMemories ? "animate-spin" : ""}`} />
                  Aktualizovat z databáze
                </button>
              </div>

              {loadingMemories && memories.length === 0 ? (
                <div className="p-8 text-center text-slate-500 font-mono text-xs">
                  Načítám reálné záznamy z databáze...
                </div>
              ) : memories.length === 0 ? (
                <div className="p-8 rounded-2xl bg-[#0f172a]/40 border border-[#1e293b] text-center text-slate-400 text-xs">
                  V databázi zatím nejsou žádné paměťové záznamy. Vložte nová data přes formulář výše nebo proveďte dotaz v chatu. Žádné simulované položky nejsou zobrazeny.
                </div>
              ) : (
                memories.map((mem) => (
                  <div
                    key={mem.id}
                    className="p-5 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-3 font-mono"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-[#00E5FF] px-2 py-0.5 rounded bg-[#00E5FF]/10 border border-[#00E5FF]/30">
                        {mem.memory_type || "epistemic_imprint"}
                      </span>
                      <span className="text-slate-400">
                        {mem.created_at ? new Date(mem.created_at).toLocaleString() : ""}
                      </span>
                    </div>
                    <p className="text-sm font-sans text-slate-200">{mem.content}</p>
                    {mem.metadata && (
                      <div className="text-[11px] text-slate-500 bg-[#090d16] p-2.5 rounded-lg border border-[#1e293b]/60">
                        Metadata: {JSON.stringify(mem.metadata)}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
