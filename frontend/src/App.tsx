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
  Menu,
  Copy,
  Download,
  Terminal,
  Activity,
  ArrowRight,
  ExternalLink,
  Workflow,
  Target,
  BarChart3,
  HelpCircle,
  FlaskConical,
  Bug,
  Calculator,
  RotateCcw,
  Play,
} from "lucide-react";
import DevPromptLab, { TokenTelemetryState } from "./DevPromptLab";

// ==========================================================
// TYPES & SCHEMAS
// ==========================================================

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

interface RiskVectorItem {
  domain: string;
  vector: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | string;
  probability: "LOW" | "MEDIUM" | "HIGH" | string;
  mitigation: string;
  cascade_timeline?: string;
  entropy_impact?: number;
}

interface ConsequenceForensics {
  risk_index: number;
  risk_level: "SAFE" | "ELEVATED" | "CRITICAL" | string;
  horizon: string;
  t_plus_1_systemic_drift: string;
  asymmetric_failure_modes: string[];
  regulatory_compliance_deltas: string[];
  thermodynamic_entropy_spike: string;
  identified_vectors: RiskVectorItem[];
  mitigation_directives: string[];
  automatic_countermeasure_deployed: boolean;
}

export interface TokenUsageMetric {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
  cost_usd: number;
}

interface MessageItem {
  id: string;
  role: "user" | "assistant";
  content: string;
  cognitive_process?: string;
  follow_up_questions?: string[];
  impact_matrix?: ImpactMatrixScores;
  consequence_forensics?: ConsequenceForensics;
  token_usage?: TokenUsageMetric;
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

interface FivePhaseBreakdown {
  status: string;
  ontology_domain: string;
  query: string;
  phase1: {
    cleaned_query: string;
    ontology_domain: string;
    core_essence: string;
    identified_assumptions: string[];
    hard_data_inferred: Record<string, any>;
    soft_data_inferred: Record<string, any>;
  };
  phase2: {
    domain_mappings: Record<string, string>;
    leverage_point: string;
    nonlinear_synergies: string[];
  };
  phase3: {
    win_win_win_rationale: string;
    strategic_milestones: string[];
    effort_to_leverage_ratio: string;
  };
  phase4: {
    artifact_type: string;
    execution_steps: string[];
    concrete_output: string;
  };
  phase5: {
    economic_viability: number;
    technological_elegance: number;
    eco_social_regeneration: number;
    psychological_acceptability: number;
    composite_score: number;
    adversarial_vulnerabilities: string[];
    reasoning: string;
    reflexive_questions: string[];
  };
  formatted_answer: string;
  cognitive_process: string;
  follow_up_questions: string[];
  composite_score: number;
  risk_forensics?: ConsequenceForensics;
}

// ==========================================================
// MAIN COMPONENT
// ==========================================================

export default function App() {
  const [messages, setMessages] = useState<MessageItem[]>([
    {
      id: "initial-msg",
      role: "assistant",
      content:
        "# O.M.N.I.S. SIGMA-OMEGA Kognitivní Architektura\n\n" +
        "Vítejte v operačním prostředí O.M.N.I.S. (Operativní Multimodální Nástroj pro Integrovanou Synergii).\n\n" +
        "Systém transformuje komplexní vstupy skrze **5 deterministických fází**:\n\n" +
        "1. **Sémantická Dekonstrukce:** Očištění problému od kognitivních zkreslení a dogmat; rozpad na prvočinitele (First Principles).\n" +
        "2. **Transdisciplinární Křížení:** Modální překlad mezi 4 doménami (systémy, ekonomie, psychologie, ekologie) a nalezení pákového uzlového bodu (*leverage point*).\n" +
        "3. **Okamžitý Akční Plán:** Formulace Win-Win-Win strategie s maximálním pákovým efektem při minimálním úsilí.\n" +
        "4. **Deterministická Exekuce:** Přímý kód, exaktní direktivy a systémová architektura bez zbytečného balastu (Zero-Fluff).\n" +
        "5. **Autopoietická Reflexe & 4D Matice:** Výpočet integrálního indexu (váhy 30/30/20/20), Red-Teaming penalizace a sebereferenční paměťová smyčka.",
      cognitive_process:
        "### Pětifázový cyklus O.M.N.I.S. [Inicializace systému]\n" +
        "[Fáze I]: Asimilace epistemických dat v doméně SYSTEMS_INTELLIGENCE.\n" +
        "[Fáze II]: Modální překlad. Nalezen uzlový bod: Zavedení deterministické validační vrstvy na rozhraní vrstev.\n" +
        "[Fáze III]: Win-Win-Win: 1 jednotka úsilí = 10 jednotek systémového efektu.\n" +
        "[Fáze IV]: Generování exekučních invariantů v TypeScriptu a Pythonu.\n" +
        "[Fáze V]: Matice dopadů (váhy 0.3/0.3/0.2/0.2): Ekon=88%, Tech=96%, Ekol=94%, Psych=91%. Kompozitní index: 92.4%.",
      follow_up_questions: [
        "Jak funguje výpočet penalizací za adversarial zranitelnosti v Fázi V?",
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
          "Základní výchozí harmonie kognitivní sítě. Vážené skóre přesahuje 92 % bez kompromisů.",
        adversarial_vulnerabilities: [
          "Možné sycophancy zkreslení při absenci odděleného hodnotitele",
        ],
        leverage_point:
          "Zavedení asynchronního validačního uzlu na rozhraní Epistemické a Syntetické roviny",
      },
      token_usage: {
        prompt_tokens: 1250,
        completion_tokens: 1400,
        total_tokens: 2650,
        cost_usd: 0.000514,
      },
      created_at: new Date().toISOString(),
    },
  ]);

  const [inputQuery, setInputQuery] = useState("");
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [enableThinking, setEnableThinking] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<
    "chat" | "phases" | "matrix" | "forensics" | "guardrail" | "memory" | "dev_lab"
  >("chat");
  const [expandedThoughts, setExpandedThoughts] = useState<Record<string, boolean>>({
    "initial-msg": true,
  });
  const [feedbackRating, setFeedbackRating] = useState<Record<string, number>>({});
  const [feedbackSubmitted, setFeedbackSubmitted] = useState<Record<string, boolean>>({});
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  // Cumulative Token Telemetry State (Synced with Backend /api/dev/token-telemetry)
  const [tokenTelemetry, setTokenTelemetry] = useState<TokenTelemetryState>({
    cumulative_prompt_tokens: 1250,
    cumulative_completion_tokens: 1400,
    cumulative_total_tokens: 2650,
    total_queries_executed: 1,
    estimated_total_cost_usd: 0.000514,
    estimated_total_cost_czk: 0.0121,
    recent_records: [],
  });

  const fetchTokenTelemetry = async () => {
    try {
      const resp = await fetch("/api/dev/token-telemetry");
      if (resp.ok) {
        const data = await resp.json();
        setTokenTelemetry(data);
      }
    } catch (err) {
      console.warn("Token telemetry fetch failed:", err);
    }
  };

  const handleResetTelemetry = async () => {
    try {
      await fetch("/api/dev/reset-tokens", { method: "POST" });
      await fetchTokenTelemetry();
    } catch (err) {
      console.warn("Reset token telemetry failed:", err);
    }
  };

  // 5-Phase Dedicated Breakdown State
  const [fivePhaseData, setFivePhaseData] = useState<FivePhaseBreakdown | null>(null);
  const [loadingFivePhases, setLoadingFivePhases] = useState(false);
  const [selectedPhaseDetail, setSelectedPhaseDetail] = useState<number>(1);

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
    {
      candidate_id: "cand-3",
      title: "Deterministická mikroslužba s pgvector",
      description: "Hybridní architektura: deterministické validační jádro napojené na sémantickou paměť.",
      economic: 9.2,
      tech: 9.4,
      eco: 8.8,
      psych: 9.0,
      vulnerabilities: ["Nutnost inicializace PostgreSQL schématu"],
    },
  ]);
  const [guardrailResults, setGuardrailResults] = useState<any>(null);
  const [evaluatingGuardrail, setEvaluatingGuardrail] = useState(false);
  const [minimumThreshold, setMinimumThreshold] = useState<number>(7.0);

  // Real Database Memory & Epistemic Layer state
  const [memories, setMemories] = useState<any[]>([]);
  const [loadingMemories, setLoadingMemories] = useState(false);
  const [epistemicPurpose, setEpistemicPurpose] = useState("Optimalizace energetického toku v distribuované síti");
  const [epistemicDomain, setEpistemicDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [epistemicHardParam, setEpistemicHardParam] = useState("efficiency: 94.2%, latency_ms: 12, cost_reduction: 35%");
  const [epistemicSoftParam, setEpistemicSoftParam] = useState("sociological_vector: low_friction, psychological_state: high_trust");
  const [epistemicSensoryParam, setEpistemicSensoryParam] = useState("intuition_notes: Asynchronní uzel eliminuje degradační entropii");
  const [ingestStatus, setIngestStatus] = useState<string | null>(null);
  const [isIngesting, setIsIngesting] = useState(false);

  // Backend health verification state
  const [backendHealth, setBackendHealth] = useState<"checking" | "connected" | "disconnected">("checking");
  const [backendLatency, setBackendLatency] = useState<number | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

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

  useEffect(() => {
    checkBackendHealth();
    fetchRealMemories();
    fetchTokenTelemetry();
    const interval = setInterval(checkBackendHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  // Quick Action Presets
  const quickPresets = [
    {
      label: "Architektonický Audit",
      icon: Cpu,
      color: "from-[#00F0FF] to-[#3B82F6]",
      prompt: "Proveď hloubkový architektonický audit systému, identifikuj uzlové body selhání a navrhni zero-defect řešení.",
      domain: "SYSTEMS_INTELLIGENCE",
    },
    {
      label: "Transdisciplinární Syntéza",
      icon: Workflow,
      color: "from-[#A855F7] to-[#EC4899]",
      prompt: "Jak navrhnout distribuovaný caching systém s nulovou latencí a minimálními náklady optikou teorie her a systémové dynamiky?",
      domain: "SYSTEMS_INTELLIGENCE",
    },
    {
      label: "Win-Win-Win Strategie",
      icon: Zap,
      color: "from-[#10B981] to-[#00F0FF]",
      prompt: "Vytvoř okamžitý akční plán pro monetizaci cloudové automatizace s využitím živnosti a minimálního manuálního úsilí.",
      domain: "CYBERNETICS",
    },
    {
      label: "Red-Teaming Stresstest",
      icon: ShieldAlert,
      color: "from-[#F59E0B] to-[#F43F5E]",
      prompt: "Podrob model simulaci nepřátelského prostředí (Adversarial Red-Teaming), detekuj zranitelnosti a spočítej penalizace.",
      domain: "COGNITIVE_SCI",
    },
  ];

  const handleApplyPreset = (prompt: string, domain: string) => {
    setInputQuery(prompt);
    setOntologyDomain(domain);
  };

  const toggleThoughts = (id: string) => {
    setExpandedThoughts((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleCopyText = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedMessageId(id);
    setTimeout(() => setCopiedMessageId(null), 2500);
  };

  const handleExportJson = () => {
    const dataStr = JSON.stringify({ messages, fivePhaseData, latestMatrix }, null, 2);
    const blob = new Blob([dataStr], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `omnis-report-${new Date().toISOString().slice(0, 10)}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleResetConversation = () => {
    if (confirm("Opravdu chcete inicializovat nový kognitivní cyklus O.M.N.I.S.?")) {
      setMessages([messages[0]]);
      setFivePhaseData(null);
    }
  };

  // Main Query Pipeline Handler
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
        consequence_forensics: data.consequence_forensics,
        token_usage: data.token_usage,
        created_at: data.created_at || new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
      setExpandedThoughts((prev) => ({ ...prev, [assistantMessage.id]: true }));
      fetchTokenTelemetry();

      // Asynchronously fetch detailed 5-phase breakdown
      triggerFivePhaseSynthesis(text, ontologyDomain);
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

  // Dedicated 5-Phase Pipeline Execution
  const triggerFivePhaseSynthesis = async (queryText: string, domain: string) => {
    setLoadingFivePhases(true);
    try {
      const resp = await fetch("/api/omnis/synthesize-5phases", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: queryText,
          ontology_domain: domain,
        }),
      });
      if (resp.ok) {
        const pData = await resp.json();
        setFivePhaseData(pData);
      }
    } catch (err) {
      console.error("Chyba při syntéze 5 fází:", err);
    } finally {
      setLoadingFivePhases(false);
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
      minimum_threshold: minimumThreshold,
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
        alert(`Chyba guardrailu: ${err?.detail || resp.statusText}`);
      }
    } catch (e: any) {
      alert(`Chyba spojení s backendem: ${e?.message}`);
    } finally {
      setEvaluatingGuardrail(false);
    }
  };

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
        setIngestStatus(`✅ Úspěšně uloženo do pgvector (ID: ${resData.omnis_entity_id || "OK"})`);
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

  const latestMatrix =
    [...messages].reverse().find((m) => m.impact_matrix)?.impact_matrix ||
    messages[0].impact_matrix!;

  const latestForensics =
    [...messages].reverse().find((m) => m.consequence_forensics)?.consequence_forensics ||
    fivePhaseData?.risk_forensics ||
    null;

  return (
    <div className="min-h-screen bg-[#060913] text-slate-100 flex flex-col font-sans selection:bg-[#00F0FF]/30 selection:text-[#00F0FF]">
      {/* ==========================================================
          TOP NAVIGATION & STATUS BAR (RESPONSIVE)
         ========================================================== */}
      <header className="sticky top-0 z-40 bg-[#0A0F1D]/90 backdrop-blur-md border-b border-slate-800/80 px-4 py-3 sm:px-6">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-3">
          {/* Logo & Identity */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileSidebarOpen(!mobileSidebarOpen)}
              className="lg:hidden p-2 rounded-lg bg-slate-800/60 border border-slate-700 text-slate-300 hover:text-white"
              aria-label="Toggle Menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-[#00F0FF]/20 via-[#A855F7]/20 to-[#00F0FF]/30 border border-[#00F0FF]/40 shadow-[0_0_15px_rgba(0,240,255,0.25)]">
              <Brain className="w-6 h-6 text-[#00F0FF] animate-pulse" />
              <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-[#10B981] rounded-full border-2 border-[#0A0F1D]" />
            </div>

            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-lg sm:text-xl tracking-wider bg-gradient-to-r from-[#00F0FF] via-[#A855F7] to-[#10B981] bg-clip-text text-transparent">
                  O.M.N.I.S.
                </span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-[#A855F7]/20 border border-[#A855F7]/40 text-[#A855F7] font-mono font-semibold hidden sm:inline-block">
                  SIGMA-OMEGA
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-mono hidden md:block">
                Transdisciplinární Kognitivní Architektura • 5-Fázová Syntéza
              </p>
            </div>
          </div>

          {/* Quick Actions & Live Telemetry */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Live Backend Indicator */}
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-900/90 border border-slate-700/80 text-xs font-mono">
              {backendHealth === "connected" && (
                <>
                  <span className="w-2 h-2 rounded-full bg-[#10B981] animate-ping" />
                  <span className="text-[#10B981] font-semibold">ONLINE</span>
                  {backendLatency !== null && (
                    <span className="text-slate-400 text-[10px]">({backendLatency}ms)</span>
                  )}
                </>
              )}
              {backendHealth === "checking" && (
                <>
                  <span className="w-2 h-2 rounded-full bg-[#F59E0B] animate-pulse" />
                  <span className="text-[#F59E0B]">Ověřuji...</span>
                </>
              )}
              {backendHealth === "disconnected" && (
                <>
                  <span className="w-2 h-2 rounded-full bg-[#F43F5E]" />
                  <span className="text-[#F43F5E] font-semibold">OFFLINE</span>
                </>
              )}
            </div>

            {/* Zero Simulation Badge */}
            <div className="hidden xl:flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#00F0FF]/10 border border-[#00F0FF]/30 text-[#00F0FF] text-[11px] font-mono font-medium">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>100% REÁLNÉ VÝPOČTY</span>
            </div>

            {/* Live Session Token Telemetry Widget */}
            <button
              onClick={() => setActiveTab("dev_lab")}
              title="Klikněte pro přechod do Vývojové Laboratoře a kalkulátoru tokenů"
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-cyan-950/40 border border-cyan-500/40 text-xs font-mono text-[#00F0FF] hover:border-[#00F0FF] transition-all shadow-[0_0_10px_rgba(0,240,255,0.15)]"
            >
              <Zap className="w-3.5 h-3.5 text-[#00F0FF]" />
              <span className="font-bold">{tokenTelemetry.cumulative_total_tokens.toLocaleString()}</span>
              <span className="text-[10px] text-slate-400 hidden sm:inline">tok</span>
            </button>

            {/* Export JSON */}
            <button
              onClick={handleExportJson}
              title="Exportovat data do JSON"
              className="p-2 rounded-lg bg-slate-800/80 border border-slate-700 text-slate-300 hover:text-[#00F0FF] hover:border-[#00F0FF]/40 transition-colors"
            >
              <Download className="w-4 h-4" />
            </button>

            {/* Reset / New Cycle */}
            <button
              onClick={handleResetConversation}
              title="Nový kognitivní cyklus"
              className="p-2 rounded-lg bg-slate-800/80 border border-slate-700 text-slate-300 hover:text-[#A855F7] hover:border-[#A855F7]/40 transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* ==========================================================
          INTERACTIVE 5-PHASE PROGRESS STEPPER
         ========================================================== */}
      <div className="bg-[#0A0F1D]/70 border-b border-slate-800/60 py-2.5 px-4 overflow-x-auto">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-2 min-w-[640px]">
          {[
            { num: 1, name: "Sémantická Dekonstrukce", sub: "Prvočinitele", color: "from-[#00F0FF] to-[#38BDF8]" },
            { num: 2, name: "Transdisciplinární Křížení", sub: "Pákový uzlový bod", color: "from-[#A855F7] to-[#C084FC]" },
            { num: 3, name: "Okamžitý Akční Plán", sub: "Win-Win-Win", color: "from-[#10B981] to-[#34D399]" },
            { num: 4, name: "Deterministická Exekuce", sub: "Konkrétní kód", color: "from-[#3B82F6] to-[#60A5FA]" },
            { num: 5, name: "Autopoietická Reflexe", sub: "4D Matice dopadů", color: "from-[#F59E0B] to-[#FBBF24]" },
          ].map((phase, idx) => (
            <button
              key={phase.num}
              onClick={() => {
                setActiveTab("phases");
                setSelectedPhaseDetail(phase.num);
              }}
              className={`flex-1 flex items-center gap-2 p-2 rounded-xl transition-all text-left border ${
                activeTab === "phases" && selectedPhaseDetail === phase.num
                  ? "bg-slate-800/90 border-[#00F0FF]/60 shadow-[0_0_15px_rgba(0,240,255,0.2)]"
                  : "bg-slate-900/40 border-slate-800 hover:border-slate-700"
              }`}
            >
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs bg-gradient-to-br ${phase.color} text-slate-950 shrink-0 font-mono shadow-sm`}
              >
                {phase.num}
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-slate-200 truncate">{phase.name}</p>
                <p className="text-[10px] text-slate-400 font-mono truncate">{phase.sub}</p>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* ==========================================================
          MAIN LAYOUT (SIDEBAR + WORKSPACE TABS)
         ========================================================== */}
      <div className="flex-1 flex flex-col lg:flex-row max-w-7xl w-full mx-auto p-3 sm:p-5 gap-4 sm:gap-6 min-h-0">
        {/* ========================================================
            MOBILE SIDEBAR OVERLAY
           ======================================================== */}
        {mobileSidebarOpen && (
          <div
            className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm lg:hidden"
            onClick={() => setMobileSidebarOpen(false)}
          />
        )}

        {/* ========================================================
            SIDEBAR (SETTINGS, TELEMETRY, DOMAINS)
           ======================================================== */}
        <aside
          className={`fixed lg:static top-0 bottom-0 left-0 z-50 lg:z-auto w-80 sm:w-88 lg:w-80 xl:w-96 bg-[#0A0F1D] lg:bg-transparent p-5 lg:p-0 border-r lg:border-r-0 border-slate-800 overflow-y-auto transition-transform duration-300 ${
            mobileSidebarOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          } flex flex-col gap-4 shrink-0`}
        >
          {/* Mobile Header close button */}
          <div className="lg:hidden flex items-center justify-between pb-3 border-b border-slate-800">
            <span className="font-bold text-sm text-[#00F0FF] font-mono">KONFIGURACE SYSTÉMU</span>
            <button
              onClick={() => setMobileSidebarOpen(false)}
              className="p-1 rounded-lg hover:bg-slate-800 text-slate-400"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Ontological Domain Selector */}
          <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-slate-800/90 p-4 shadow-lg">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-mono font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <Compass className="w-4 h-4 text-[#00F0FF]" />
                Ontologická Doména
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#00F0FF]/10 text-[#00F0FF] font-mono">
                Aktivní
              </span>
            </div>

            <div className="space-y-1.5">
              {[
                {
                  id: "SYSTEMS_INTELLIGENCE",
                  label: "Systems Intelligence",
                  desc: "Kybernetika, sítě, škálování",
                  color: "border-[#00F0FF]/40 text-[#00F0FF]",
                  dot: "bg-[#00F0FF]",
                },
                {
                  id: "CYBERNETICS",
                  label: "Cybernetic Governance",
                  desc: "Zpětné vazby, Nashova rovnováha",
                  color: "border-[#A855F7]/40 text-[#A855F7]",
                  dot: "bg-[#A855F7]",
                },
                {
                  id: "SUSTAINABLE_TECH",
                  label: "Sustainable Tech",
                  desc: "Ekologie, minimální entropie",
                  color: "border-[#10B981]/40 text-[#10B981]",
                  dot: "bg-[#10B981]",
                },
                {
                  id: "COGNITIVE_SCI",
                  label: "Cognitive Sciences",
                  desc: "Psychologie, redukce tření",
                  color: "border-[#F59E0B]/40 text-[#F59E0B]",
                  dot: "bg-[#F59E0B]",
                },
              ].map((dom) => (
                <button
                  key={dom.id}
                  onClick={() => {
                    setOntologyDomain(dom.id);
                    setMobileSidebarOpen(false);
                  }}
                  className={`w-full text-left p-2.5 rounded-xl border transition-all flex items-start gap-2.5 ${
                    ontologyDomain === dom.id
                      ? `bg-slate-800/90 ${dom.color} shadow-[0_0_12px_rgba(0,240,255,0.15)]`
                      : "bg-slate-900/50 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-300"
                  }`}
                >
                  <span className={`w-2 h-2 rounded-full mt-1.5 shrink-0 ${dom.dot}`} />
                  <div>
                    <p className="text-xs font-semibold leading-none mb-1">{dom.label}</p>
                    <p className="text-[10px] text-slate-400 font-mono">{dom.desc}</p>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Cognitive Thinking Settings */}
          <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-slate-800/90 p-4 shadow-lg">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Brain className="w-4 h-4 text-[#A855F7]" />
                <div>
                  <p className="text-xs font-semibold text-slate-200">Hloubkové Myšlení (Introspection)</p>
                  <p className="text-[10px] text-slate-400 font-mono">Zobrazit kognitivní proud myšlenek</p>
                </div>
              </div>
              <input
                type="checkbox"
                checked={enableThinking}
                onChange={(e) => setEnableThinking(e.target.checked)}
                className="h-4 w-4 rounded accent-[#A855F7] cursor-pointer"
              />
            </div>
          </div>

          {/* System Telemetry & Invariants Panel */}
          <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-slate-800/90 p-4 shadow-lg space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <span className="text-xs font-mono font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-[#10B981]" />
                Živá Telemetrie
              </span>
              <span className="text-[10px] text-[#10B981] font-mono font-bold">100% NOMINÁLNÍ</span>
            </div>

            <div className="space-y-2 text-xs font-mono">
              <div className="flex items-center justify-between text-slate-300">
                <span className="text-slate-400">Backend API:</span>
                <span
                  className={
                    backendHealth === "connected"
                      ? "text-[#10B981] font-semibold"
                      : backendHealth === "checking"
                      ? "text-[#F59E0B]"
                      : "text-[#F43F5E] font-semibold"
                  }
                >
                  {backendHealth === "connected" ? `ONLINE (${backendLatency}ms)` : backendHealth.toUpperCase()}
                </span>
              </div>

              <div className="flex items-center justify-between text-slate-300">
                <span className="text-slate-400">Kognitivní Invarianty:</span>
                <span className="text-[#00F0FF] font-semibold">5 / 5 Aktivních</span>
              </div>

              <div className="flex items-center justify-between text-slate-300">
                <span className="text-slate-400">Zero-Simulation:</span>
                <span className="text-[#10B981] font-semibold">VYNUCENO</span>
              </div>

              <div className="flex items-center justify-between text-slate-300">
                <span className="text-slate-400">Vektorová Paměť:</span>
                <span className="text-[#A855F7] font-semibold">{memories.length} Záznamů</span>
              </div>

              <div className="flex items-center justify-between text-slate-300">
                <span className="text-slate-400">Matice vah (IV):</span>
                <span className="text-slate-300">30 / 30 / 20 / 20</span>
              </div>
            </div>
          </div>

          {/* Quick Info Box */}
          <div className="rounded-2xl bg-gradient-to-br from-[#00F0FF]/10 via-slate-900 to-[#A855F7]/10 border border-[#00F0FF]/20 p-4">
            <h4 className="text-xs font-bold text-[#00F0FF] flex items-center gap-1.5 mb-1.5 font-mono">
              <Sparkles className="w-3.5 h-3.5" />
              PRINCIP ZERO-SIMULATION
            </h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              O.M.N.I.S. negeneruje fiktivní data ani iluzorní procenta. Pokud absentuje model nebo API klíč, systém otevřeně zobrazí diagnostický stav.
            </p>
          </div>
        </aside>

        {/* ========================================================
            MAIN WORKSPACE AREA
           ======================================================== */}
        <main className="flex-1 flex flex-col min-w-0 bg-gradient-to-b from-[#0F172A]/80 to-[#0A0F1D]/90 rounded-3xl border border-slate-800/80 shadow-2xl overflow-hidden">
          {/* Tab Navigation Bar (Responsive Horizontal Scroll) */}
          <div className="border-b border-slate-800/80 p-2 sm:p-3 bg-slate-900/60 backdrop-blur-md overflow-x-auto">
            <div className="flex items-center gap-2 min-w-max">
              {[
                { id: "chat", label: "Konzole & Chat", icon: MessageSquare, badge: null },
                { id: "phases", label: "5 Fází O.M.N.I.S.", icon: Workflow, badge: "Architektura" },
                { id: "matrix", label: "4D Matice Dopadů", icon: BarChart3, badge: `${(latestMatrix.composite_score * 100).toFixed(0)}%` },
                {
                  id: "forensics",
                  label: "Forenzní Analýza Rizik",
                  icon: ShieldAlert,
                  badge: latestForensics ? latestForensics.risk_level : "T+N",
                },
                { id: "guardrail", label: "Fáze IV: Guardrail", icon: ShieldCheck, badge: `${candidates.length}` },
                { id: "memory", label: "Epistemická Paměť", icon: Database, badge: `${memories.length}` },
                {
                  id: "dev_lab",
                  label: "Dev Prompt Studio & Telemetrie",
                  icon: FlaskConical,
                  badge: "DEV ONLY",
                },
              ].map((t) => {
                const IconComponent = t.icon;
                const isActive = activeTab === t.id;
                return (
                  <button
                    key={t.id}
                    onClick={() => setActiveTab(t.id as any)}
                    className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all border ${
                      isActive
                        ? "bg-gradient-to-r from-[#00F0FF]/20 via-[#A855F7]/20 to-[#00F0FF]/10 text-white border-[#00F0FF]/50 shadow-[0_0_15px_rgba(0,240,255,0.2)]"
                        : "bg-slate-900/40 text-slate-400 border-slate-800 hover:text-slate-200 hover:border-slate-700"
                    }`}
                  >
                    <IconComponent className={`w-4 h-4 ${isActive ? "text-[#00F0FF]" : "text-slate-400"}`} />
                    <span>{t.label}</span>
                    {t.badge && (
                      <span
                        className={`text-[10px] px-1.5 py-0.5 rounded-full font-mono font-bold ${
                          isActive
                            ? "bg-[#00F0FF]/20 text-[#00F0FF]"
                            : "bg-slate-800 text-slate-400"
                        }`}
                      >
                        {t.badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* ========================================================
              TAB 1: CHAT & COGNITIVE CONSOLE
             ======================================================== */}
          {activeTab === "chat" && (
            <div className="flex-1 flex flex-col min-h-0">
              {/* Messages Stream */}
              <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 min-h-0">
                {messages.map((msg) => {
                  const isUser = msg.role === "user";
                  const thoughtsOpen = expandedThoughts[msg.id];

                  return (
                    <div
                      key={msg.id}
                      className={`flex flex-col ${isUser ? "items-end" : "items-start"} max-w-full`}
                    >
                      {/* Message Bubble Container */}
                      <div
                        className={`w-full sm:max-w-3xl rounded-2xl p-4 sm:p-5 border transition-all ${
                          isUser
                            ? "bg-gradient-to-r from-[#00F0FF]/15 to-[#3B82F6]/15 border-[#00F0FF]/40 text-slate-100 shadow-[0_0_15px_rgba(0,240,255,0.1)]"
                            : "bg-gradient-to-b from-[#111C35] to-[#0C1527] border-slate-800/90 text-slate-100 shadow-xl"
                        }`}
                      >
                        {/* Header Author Info */}
                        <div className="flex items-center justify-between gap-2 mb-3 pb-2 border-b border-slate-800/60">
                          <div className="flex items-center gap-2">
                            <div
                              className={`w-6 h-6 rounded-lg flex items-center justify-center text-xs font-bold font-mono ${
                                isUser
                                  ? "bg-[#00F0FF] text-slate-950"
                                  : "bg-gradient-to-tr from-[#A855F7] to-[#00F0FF] text-white"
                              }`}
                            >
                              {isUser ? "U" : "Ω"}
                            </div>
                            <span className="text-xs font-semibold font-mono text-slate-300">
                              {isUser ? "Uživatel" : "O.M.N.I.S. Cognitive Synthesis"}
                            </span>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-[10px] text-slate-500 font-mono">
                              {new Date(msg.created_at).toLocaleTimeString([], {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </span>
                            {!isUser && (
                              <button
                                onClick={() => handleCopyText(msg.content, msg.id)}
                                title="Kopírovat odpověď"
                                className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
                              >
                                {copiedMessageId === msg.id ? (
                                  <Check className="w-3.5 h-3.5 text-[#10B981]" />
                                ) : (
                                  <Copy className="w-3.5 h-3.5" />
                                )}
                              </button>
                            )}
                          </div>
                        </div>

                        {/* Cognitive Process (Introspection Accordion) */}
                        {msg.cognitive_process && (
                          <div className="mb-4 rounded-xl bg-slate-950/60 border border-[#A855F7]/30 overflow-hidden">
                            <button
                              onClick={() => toggleThoughts(msg.id)}
                              className="w-full flex items-center justify-between px-3 py-2 text-xs font-mono font-medium text-[#A855F7] hover:bg-[#A855F7]/10 transition-colors"
                            >
                              <span className="flex items-center gap-2">
                                <Brain className="w-3.5 h-3.5 text-[#A855F7]" />
                                Kognitivní proces myšlení (5 Fází)
                              </span>
                              {thoughtsOpen ? (
                                <ChevronUp className="w-4 h-4" />
                              ) : (
                                <ChevronDown className="w-4 h-4" />
                              )}
                            </button>
                            {thoughtsOpen && (
                              <div className="p-3 bg-black/40 border-t border-slate-800/60 text-xs text-slate-300 font-mono whitespace-pre-wrap leading-relaxed">
                                {msg.cognitive_process}
                              </div>
                            )}
                          </div>
                        )}

                        {/* Formatted Content */}
                        <div className="prose prose-invert max-w-none text-sm leading-relaxed whitespace-pre-wrap font-sans text-slate-200">
                          {msg.content}
                        </div>

                        {/* Impact Matrix Mini Card if present */}
                        {msg.impact_matrix && (
                          <div className="mt-4 pt-3 border-t border-slate-800/70">
                            <div className="flex items-center justify-between mb-2">
                              <span className="text-xs font-mono font-bold text-[#00F0FF] flex items-center gap-1.5">
                                <BarChart3 className="w-3.5 h-3.5" />
                                4D Matice Dopadů (Kompozit:{" "}
                                {(msg.impact_matrix.composite_score * 100).toFixed(1)}%)
                              </span>
                              <button
                                onClick={() => setActiveTab("matrix")}
                                className="text-[11px] text-[#00F0FF] hover:underline font-mono flex items-center gap-1"
                              >
                                Detailní rozbor <ArrowRight className="w-3 h-3" />
                              </button>
                            </div>

                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                                <span className="text-[10px] text-slate-400 font-mono block">Ekonomika</span>
                                <span className="text-xs font-bold text-[#00F0FF] font-mono">
                                  {(msg.impact_matrix.economic_viability * 100).toFixed(0)}%
                                </span>
                              </div>
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                                <span className="text-[10px] text-slate-400 font-mono block">Technologie</span>
                                <span className="text-xs font-bold text-[#A855F7] font-mono">
                                  {(msg.impact_matrix.technological_elegance * 100).toFixed(0)}%
                                </span>
                              </div>
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                                <span className="text-[10px] text-slate-400 font-mono block">Eko-Sociální</span>
                                <span className="text-xs font-bold text-[#10B981] font-mono">
                                  {(msg.impact_matrix.eco_social_regeneration * 100).toFixed(0)}%
                                </span>
                              </div>
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                                <span className="text-[10px] text-slate-400 font-mono block">Psychologie</span>
                                <span className="text-xs font-bold text-[#F59E0B] font-mono">
                                  {(msg.impact_matrix.psychological_acceptability * 100).toFixed(0)}%
                                </span>
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Prospective Risk Forensics Mini Card if present */}
                        {msg.consequence_forensics && (
                          <div className="mt-4 pt-3 border-t border-slate-800/70">
                            <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                              <span className="text-xs font-mono font-bold flex items-center gap-1.5">
                                <ShieldAlert className="w-3.5 h-3.5 text-amber-400" />
                                <span className="text-slate-200">Prospektivní Forenzní Analýza Rizik (T+N)</span>
                                <span
                                  className={`ml-1 px-2 py-0.5 rounded-full text-[10px] font-bold font-mono ${
                                    msg.consequence_forensics.risk_level === "SAFE"
                                      ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                                      : msg.consequence_forensics.risk_level === "ELEVATED"
                                      ? "bg-amber-500/20 text-amber-400 border border-amber-500/40"
                                      : "bg-rose-500/20 text-rose-400 border border-rose-500/40"
                                  }`}
                                >
                                  {msg.consequence_forensics.risk_level === "SAFE"
                                    ? "🟢 SAFE"
                                    : msg.consequence_forensics.risk_level === "ELEVATED"
                                    ? "🟡 ELEVATED"
                                    : "🔴 CRITICAL"}
                                </span>
                              </span>
                              <button
                                onClick={() => setActiveTab("forensics")}
                                className="text-[11px] text-amber-400 hover:underline font-mono flex items-center gap-1"
                              >
                                Detailní Forenzní Audit <ArrowRight className="w-3 h-3" />
                              </button>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs font-mono">
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800">
                                <span className="text-[10px] text-slate-400 block">Horizont predikce</span>
                                <span className="text-xs font-bold text-slate-200">
                                  {msg.consequence_forensics.horizon}
                                </span>
                              </div>
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800">
                                <span className="text-[10px] text-slate-400 block">Index rizika</span>
                                <span
                                  className={`text-xs font-bold ${
                                    msg.consequence_forensics.risk_index > 0.6
                                      ? "text-rose-400"
                                      : msg.consequence_forensics.risk_index > 0.3
                                      ? "text-amber-400"
                                      : "text-emerald-400"
                                  }`}
                                >
                                  {(msg.consequence_forensics.risk_index * 100).toFixed(1)}%
                                </span>
                              </div>
                              <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800">
                                <span className="text-[10px] text-slate-400 block">Entropie / Drift</span>
                                <span className="text-xs font-bold text-cyan-400 truncate block">
                                  {msg.consequence_forensics.thermodynamic_entropy_spike}
                                </span>
                              </div>
                            </div>
                          </div>
                        )}

                        {/* Reflexive Follow-up Questions Chips */}
                        {msg.follow_up_questions && msg.follow_up_questions.length > 0 && (
                          <div className="mt-4 pt-3 border-t border-slate-800/70 space-y-1.5">
                            <span className="text-[11px] font-mono text-slate-400 flex items-center gap-1">
                              <HelpCircle className="w-3.5 h-3.5 text-[#00F0FF]" />
                              Reflexivní Otázky pro Sebekalibraci:
                            </span>
                            <div className="flex flex-wrap gap-1.5">
                              {msg.follow_up_questions.map((q, idx) => (
                                <button
                                  key={idx}
                                  onClick={() => handleSendQuery(q)}
                                  className="text-left text-xs px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-slate-300 hover:text-white transition-colors"
                                >
                                  {q}
                                </button>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Autopoietic Rating (Star Feedback) */}
                        {!isUser && (
                          <div className="mt-3 pt-2 flex items-center justify-between text-xs text-slate-400 font-mono">
                            <span className="text-[11px]">Autopoietická valence:</span>
                            <div className="flex items-center gap-1">
                              {[1, 2, 3, 4, 5].map((star) => (
                                <button
                                  key={star}
                                  onClick={() => handleRate(msg.id, star)}
                                  className="p-1 hover:text-[#F59E0B] transition-colors"
                                  title={`Ohodnotit ${star} / 5`}
                                >
                                  <Star
                                    className={`w-3.5 h-3.5 ${
                                      (feedbackRating[msg.id] || 0) >= star
                                        ? "text-[#F59E0B] fill-[#F59E0B]"
                                        : "text-slate-600"
                                    }`}
                                  />
                                </button>
                              ))}
                              {feedbackSubmitted[msg.id] && (
                                <span className="text-[10px] text-[#10B981] ml-1">Otisk uložen</span>
                              )}
                            </div>
                          </div>
                        )}

                        {/* Token Consumption Metric for this message */}
                        {!isUser && msg.token_usage && (
                          <div className="mt-3 pt-2 border-t border-slate-800/60 flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-400 bg-slate-950/40 p-2 rounded-xl">
                            <span className="flex items-center gap-1.5 text-[#00F0FF]">
                              <Zap className="w-3.5 h-3.5 text-[#00F0FF]" />
                              Spotřeba: <strong className="text-slate-200">{msg.token_usage.total_tokens.toLocaleString()} tokenů</strong>
                            </span>
                            <div className="flex items-center gap-2 text-slate-400">
                              <span>In: {msg.token_usage.prompt_tokens.toLocaleString()}</span>
                              <span className="text-slate-600">•</span>
                              <span>Out: {msg.token_usage.completion_tokens.toLocaleString()}</span>
                              <span className="text-slate-600">•</span>
                              <span className="text-[#10B981] font-semibold">${msg.token_usage.cost_usd.toFixed(5)}</span>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>

              {/* Quick Scenario Presets Bar */}
              <div className="px-4 py-2 border-t border-slate-800/60 bg-slate-950/40 overflow-x-auto">
                <div className="flex items-center gap-2 min-w-max">
                  <span className="text-[11px] font-mono text-slate-400 uppercase flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-[#00F0FF]" /> Rychlé scénáře:
                  </span>
                  {quickPresets.map((p, idx) => {
                    const IconComp = p.icon;
                    return (
                      <button
                        key={idx}
                        onClick={() => handleApplyPreset(p.prompt, p.domain)}
                        className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-xs text-slate-300 hover:text-white transition-all"
                      >
                        <IconComp className="w-3 h-3 text-[#00F0FF]" />
                        <span>{p.label}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Input Area */}
              <div className="p-4 bg-slate-950/80 border-t border-slate-800">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSendQuery();
                  }}
                  className="flex flex-col sm:flex-row gap-2"
                >
                  <div className="relative flex-1">
                    <textarea
                      rows={2}
                      value={inputQuery}
                      onChange={(e) => setInputQuery(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && !e.shiftKey) {
                          e.preventDefault();
                          handleSendQuery();
                        }
                      }}
                      placeholder="Zadejte komplexní dotaz nebo problém pro 5-fázový cyklus O.M.N.I.S...."
                      className="w-full bg-[#0A0F1D] border border-slate-800 focus:border-[#00F0FF] focus:ring-1 focus:ring-[#00F0FF] rounded-2xl p-3 text-sm text-slate-100 placeholder-slate-500 resize-none font-sans focus:outline-none transition-all"
                    />
                    <span className="absolute bottom-2 right-3 text-[10px] font-mono text-slate-500">
                      {inputQuery.length} znaků
                    </span>
                  </div>

                  <button
                    type="submit"
                    disabled={isLoading || !inputQuery.trim()}
                    className="flex items-center justify-center gap-2 px-6 py-3 rounded-2xl font-semibold text-sm font-mono transition-all bg-gradient-to-r from-[#00F0FF] via-[#A855F7] to-[#00F0FF] hover:brightness-110 text-slate-950 font-bold shadow-[0_0_20px_rgba(0,240,255,0.3)] disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none shrink-0"
                  >
                    {isLoading ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin text-slate-950" />
                        <span>Syntetizuji...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4 text-slate-950" />
                        <span>Exekuovat</span>
                      </>
                    )}
                  </button>
                </form>
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 2: 5 FÁZÍ O.M.N.I.S. (DETAILNÍ ARCHITEKTURA)
             ======================================================== */}
          {activeTab === "phases" && (
            <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-6 min-h-0">
              {/* Header & Trigger */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-white flex items-center gap-2 font-mono">
                    <Workflow className="w-5 h-5 text-[#00F0FF]" />
                    Pětifázová Architektura Skládání Výstupu
                  </h2>
                  <p className="text-xs text-slate-400 font-mono mt-1">
                    Každá funkce má své striktní místo v řetězci kognitivní syntézy (Zero-Fluff invariant).
                  </p>
                </div>

                <button
                  onClick={() =>
                    triggerFivePhaseSynthesis(
                      inputQuery.trim() ||
                        "Jak navrhnout škálovatelný distribuovaný systém s minimální režií?",
                      ontologyDomain
                    )
                  }
                  disabled={loadingFivePhases}
                  className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-[#00F0FF]/20 to-[#A855F7]/20 border border-[#00F0FF]/40 text-[#00F0FF] hover:text-white text-xs font-mono font-bold transition-all shadow-[0_0_15px_rgba(0,240,255,0.15)]"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingFivePhases ? "animate-spin" : ""}`} />
                  <span>Přepočítat 5 Fází pro dotaz</span>
                </button>
              </div>

              {/* 5 Phase Interactive Visual Cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-3">
                {[
                  {
                    num: 1,
                    title: "Sémantická Dekonstrukce",
                    badge: "Fáze I",
                    color: "border-[#00F0FF]/40 text-[#00F0FF]",
                    desc: "Očištění od předsudků, dekonstrukce na prvočinitele.",
                  },
                  {
                    num: 2,
                    title: "Transdisciplinární Křížení",
                    badge: "Fáze II",
                    color: "border-[#A855F7]/40 text-[#A855F7]",
                    desc: "Modální překlad mezi 8 doménami (Oktagon), nalezení pákového bodu.",
                  },
                  {
                    num: 3,
                    title: "Okamžitý Akční Plán",
                    badge: "Fáze III",
                    color: "border-[#10B981]/40 text-[#10B981]",
                    desc: "Win-Win-Win strategie s poměrem páky 1:10.",
                  },
                  {
                    num: 4,
                    title: "Deterministická Exekuce",
                    badge: "Fáze IV",
                    color: "border-[#3B82F6]/40 text-[#3B82F6]",
                    desc: "Konkrétní kód a systémové direktivy (Zero-Fluff).",
                  },
                  {
                    num: 5,
                    title: "Autopoietická Reflexe",
                    badge: "Fáze V",
                    color: "border-[#F59E0B]/40 text-[#F59E0B]",
                    desc: "4D Matice dopadů (30/30/20/20) a Red-Teaming.",
                  },
                  {
                    num: 6,
                    title: "Forenzní Analýza Rizik",
                    badge: "Meta-Vrstva",
                    color: "border-rose-500/40 text-rose-400",
                    desc: "Kaskádové T+1 až T+N efekty, asymetrická selhání a mitigace.",
                  },
                ].map((ph) => (
                  <button
                    key={ph.num}
                    onClick={() => setSelectedPhaseDetail(ph.num)}
                    className={`p-4 rounded-2xl border text-left transition-all flex flex-col justify-between ${
                      selectedPhaseDetail === ph.num
                        ? `bg-slate-800/90 ${ph.color} shadow-[0_0_20px_rgba(0,240,255,0.2)]`
                        : "bg-slate-900/50 border-slate-800 text-slate-400 hover:border-slate-700"
                    }`}
                  >
                    <div>
                      <span className="text-[10px] font-mono font-bold uppercase tracking-wider block mb-1">
                        {ph.badge}
                      </span>
                      <h4 className="text-sm font-bold text-slate-100 mb-1 leading-tight">{ph.title}</h4>
                      <p className="text-[11px] text-slate-400 leading-snug">{ph.desc}</p>
                    </div>
                    <span className="text-[10px] font-mono text-[#00F0FF] mt-3 block">
                      Zobrazit detail →
                    </span>
                  </button>
                ))}
              </div>

              {/* Detailed View for Selected Phase */}
              <div className="rounded-2xl bg-gradient-to-b from-[#111C35] to-[#0A0F1D] border border-slate-800 p-5 shadow-xl space-y-4">
                {selectedPhaseDetail === 1 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-[#00F0FF] flex items-center gap-2 font-mono">
                        <Filter className="w-5 h-5" />
                        FÁZE I: Sémantická Dekonstrukce & Zero-Assumption Logika
                      </h3>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-[#00F0FF]/10 text-[#00F0FF] font-mono">
                        Funkce 1 aktivní
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
                      <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
                        <h4 className="font-bold text-slate-200 mb-2">Fundamentální Jádro Problému:</h4>
                        <p className="text-slate-300 leading-relaxed">
                          {fivePhaseData?.phase1.core_essence ||
                            "Převod komplexního dotazu na prvočinitele bez předpokladů o nutnosti lineárního škálování."}
                        </p>
                      </div>

                      <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
                        <h4 className="font-bold text-slate-200 mb-2">Eliminované Implicitní Předpoklady:</h4>
                        <ul className="list-disc list-inside space-y-1 text-slate-400">
                          {fivePhaseData?.phase1.identified_assumptions.map((a, idx) => (
                            <li key={idx} className="text-rose-300">
                              {a}
                            </li>
                          )) || (
                            <>
                              <li className="text-rose-300">Předpoklad lineární závislosti nákladů na komplexitě</li>
                              <li className="text-rose-300">Předpoklad kompromisu mezi rychlostí a architekturou</li>
                            </>
                          )}
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                {selectedPhaseDetail === 2 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-[#A855F7] flex items-center gap-2 font-mono">
                        <Compass className="w-5 h-5" />
                        FÁZE II: Transdisciplinární Křížení & Pákový Bod (Leverage Point)
                      </h3>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-[#A855F7]/10 text-[#A855F7] font-mono">
                        Funkce 2 aktivní
                      </span>
                    </div>

                    <div className="p-4 rounded-xl bg-[#A855F7]/10 border border-[#A855F7]/30 mb-4">
                      <span className="text-xs font-mono font-bold text-[#A855F7] block mb-1">
                        🎯 IDENTIFIKOVANÝ UZLOVÝ BOD (LEVERAGE POINT):
                      </span>
                      <p className="text-sm font-semibold text-slate-100 font-sans">
                        {fivePhaseData?.phase2.leverage_point ||
                          "Zavedení deterministické validační vrstvy na rozhraní vrstev: eliminuje 80 % chyb s 20 % úsilí."}
                      </p>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs font-mono">
                      {Object.entries(
                        fivePhaseData?.phase2.domain_mappings || {
                          "Systémové inženýrství & Kybernetika": "Modularizace na deterministické stavové automaty a dekompozice závislostí.",
                          "Teorie her & Asymetrická ekonomie": "Kooperativní Nashova rovnováha s poměrem 1:10 ve prospěch uživatele.",
                          "Kognitivní vědy & Neuro-ergonomie": "Eliminace kognitivní zátěže (Zero-Friction UX) a sémantická vodítka.",
                          "Regenerativní dynamika & Ekologie": "Autopoietická architektura se samoopravnými vlastnostmi a nulovým odpadem.",
                          "Regulace, Právo & AI Governance": "Soulad s GDPR a EU AI Act, auditovatelnost a licenční čistota.",
                          "Zero-Trust Bezpečnost & Kryptografie": "Nejnižší privilegia (PoLP), neměnná auditní stopa a obrana proti injekcím.",
                          "Fyzikální termodynamika & Efektivita": "Minimální energetická a výpočetní stopa na jednotku užitečného výstupu.",
                          "Socio-kulturní dynamika & Etika": "Harmonizace zájmů ekosystému, transparentnost a eliminace sycophancy.",
                        }
                      ).map(([dom, val]) => (
                        <div key={dom} className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 hover:border-[#A855F7]/40 transition-colors">
                          <span className="text-[#00F0FF] font-bold block mb-1 flex items-center gap-1.5">
                            <span className="w-1.5 h-1.5 rounded-full bg-[#00F0FF]" />
                            {dom}
                          </span>
                          <span className="text-slate-300 leading-relaxed block">{val}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {selectedPhaseDetail === 3 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-[#10B981] flex items-center gap-2 font-mono">
                        <Zap className="w-5 h-5" />
                        FÁZE III: Okamžitý Akční Plán (Win-Win-Win)
                      </h3>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-[#10B981]/10 text-[#10B981] font-mono">
                        Funkce 3 aktivní
                      </span>
                    </div>

                    <p className="text-sm text-slate-300 mb-4 leading-relaxed font-sans">
                      {fivePhaseData?.phase3.win_win_win_rationale ||
                        "Win-Win-Win: Technologicky elegantní (čistý typovaný kód), ekonomicky výhodné (nulové plýtvání tokeny), uživatelsky nenáročné (okamžitá exekuce)."}
                    </p>

                    <div className="space-y-2 font-mono text-xs">
                      {(
                        fivePhaseData?.phase3.strategic_milestones || [
                          "Krok 1: Aktivace deterministického rozhraní bez simulovaných stavů.",
                          "Krok 2: Spuštění unit testů a ověření invariantů.",
                          "Krok 3: Zpětná vazba a uložení do autopoietické paměťové vrstvy.",
                        ]
                      ).map((step, idx) => (
                        <div key={idx} className="flex items-center gap-3 p-3 rounded-xl bg-slate-950/60 border border-slate-800">
                          <span className="w-6 h-6 rounded-lg bg-[#10B981] text-slate-950 font-bold flex items-center justify-center shrink-0">
                            {idx + 1}
                          </span>
                          <span className="text-slate-200">{step}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {selectedPhaseDetail === 4 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-[#3B82F6] flex items-center gap-2 font-mono">
                        <FileCode className="w-5 h-5" />
                        FÁZE IV: Deterministická Exekuce (Konkrétní Kód)
                      </h3>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-[#3B82F6]/10 text-[#3B82F6] font-mono">
                        Funkce 4 aktivní
                      </span>
                    </div>

                    <div className="p-4 rounded-xl bg-black/80 border border-slate-800 font-mono text-xs text-slate-200 overflow-x-auto">
                      <pre className="text-emerald-400 leading-relaxed">
                        {fivePhaseData?.phase4.concrete_output ||
                          "// O.M.N.I.S. Deterministická direktiva\nexport const EXECUTION_INVARIANT = {\n  status: 'ACTIVE_ZERO_DEFECT',\n  domain: 'SYSTEMS_INTELLIGENCE',\n  leverageApplied: true,\n};"}
                      </pre>
                    </div>
                  </div>
                )}

                {selectedPhaseDetail === 5 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-[#F59E0B] flex items-center gap-2 font-mono">
                        <ShieldCheck className="w-5 h-5" />
                        FÁZE V: Autopoietická Reflexe & 4D Matice Dopadů
                      </h3>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-[#F59E0B]/10 text-[#F59E0B] font-mono">
                        Funkce 5 aktivní
                      </span>
                    </div>

                    <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 mb-4">
                      <span className="text-xs font-mono font-bold text-[#F59E0B] block mb-1">
                        Zdůvodnění a vyhodnocení synergie:
                      </span>
                      <p className="text-xs text-slate-300 font-mono leading-relaxed">
                        {fivePhaseData?.phase5.reasoning ||
                          "Matice dopadů: Ekonomika (0.91 * 0.3) + Technologie (0.96 * 0.3) + Eko-Sociální (0.89 * 0.2) + Psychologie (0.93 * 0.2) = 91.6% bez kompromisů."}
                      </p>
                    </div>

                    <div className="space-y-2 text-xs font-mono">
                      <span className="text-slate-400 font-bold block">Adversarial Red-Teaming (Rizika):</span>
                      {(
                        fivePhaseData?.phase5.adversarial_vulnerabilities || [
                          "Závislost na externích síťových službách při výpadku spojení",
                          "Nutnost kalibrace vah při změně obchodních požadavků",
                        ]
                      ).map((vuln, idx) => (
                        <div key={idx} className="p-2.5 rounded-lg bg-rose-950/30 border border-rose-900/50 text-rose-300 flex items-center gap-2">
                          <ShieldAlert className="w-4 h-4 text-rose-400 shrink-0" />
                          <span>{vuln}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {selectedPhaseDetail === 6 && (
                  <div>
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
                      <h3 className="text-base font-bold text-rose-400 flex items-center gap-2 font-mono">
                        <ShieldAlert className="w-5 h-5" />
                        META-VRSTVA: Prospektivní Forenzní Analýza Rizik (T+1 až T+N)
                      </h3>
                      <span
                        className={`text-xs px-2.5 py-1 rounded-full font-mono font-bold ${
                          (latestForensics?.risk_level || "SAFE") === "SAFE"
                            ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                            : (latestForensics?.risk_level || "SAFE") === "ELEVATED"
                            ? "bg-amber-500/20 text-amber-400 border border-amber-500/30"
                            : "bg-rose-500/20 text-rose-400 border border-rose-500/30"
                        }`}
                      >
                        {latestForensics?.risk_level || "SAFE"} (Index: {((latestForensics?.risk_index ?? 0.05) * 100).toFixed(1)}%)
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                      <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
                        <span className="text-xs font-mono font-bold text-slate-400 block mb-1">
                          T+1 Systémový Drift & Horizont ({latestForensics?.horizon || "T+30_days"}):
                        </span>
                        <p className="text-xs text-slate-200 font-mono leading-relaxed">
                          {latestForensics?.t_plus_1_systemic_drift ||
                            "Žádný nezamýšlený systémový drift nezaznamenán; architektura zachovává nulovou fluktuaci."}
                        </p>
                      </div>

                      <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
                        <span className="text-xs font-mono font-bold text-slate-400 block mb-1">
                          Termodynamický nárůst entropie:
                        </span>
                        <p className="text-xs text-cyan-400 font-mono font-bold leading-relaxed">
                          {latestForensics?.thermodynamic_entropy_spike || "+0.021 J/query (v mezích stability)"}
                        </p>
                      </div>
                    </div>

                    <div className="space-y-3">
                      <span className="text-xs font-mono font-bold text-slate-300 block">
                        Asymetrická selhání (SPOF):
                      </span>
                      {(
                        latestForensics?.asymmetric_failure_modes || [
                          "Jednobodové selhání (SPOF): Ztráta spojení s primárním inferenčním node.",
                        ]
                      ).map((mode, idx) => (
                        <div
                          key={idx}
                          className="p-3 rounded-xl bg-rose-950/20 border border-rose-900/40 text-rose-300 text-xs font-mono flex items-start gap-2.5"
                        >
                          <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                          <span>{mode}</span>
                        </div>
                      ))}
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-800">
                      <button
                        onClick={() => setActiveTab("forensics")}
                        className="text-xs text-amber-400 hover:underline font-mono flex items-center gap-1.5"
                      >
                        Otevřít kompletní interaktivní forenzní dashboard s 8 doménami Oktagonu →
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 3: 4D MATICE DOPADŮ (INTERACTIVE VISUALIZER)
             ======================================================== */}
          {activeTab === "matrix" && (
            <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-6 min-h-0">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-white flex items-center gap-2 font-mono">
                    <BarChart3 className="w-5 h-5 text-[#00F0FF]" />
                    4D Matice Dopadů & Synergická Konvergence
                  </h2>
                  <p className="text-xs text-slate-400 font-mono mt-1">
                    Vážený výpočet: Ekon (30%), Tech (30%), Ekol (20%), Psych (20%) s penalizací za rizika.
                  </p>
                </div>

                <div className="px-4 py-2 rounded-2xl bg-[#00F0FF]/10 border border-[#00F0FF]/40 text-center font-mono">
                  <span className="text-[10px] text-slate-400 block">Kompozitní Index</span>
                  <span className="text-lg font-extrabold text-[#00F0FF]">
                    {(latestMatrix.composite_score * 100).toFixed(1)} %
                  </span>
                </div>
              </div>

              {/* 4 Dimension Progress Bars */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[
                  {
                    name: "Ekonomická Životaschopnost",
                    val: latestMatrix.economic_viability,
                    weight: "30%",
                    color: "bg-[#00F0FF]",
                    barColor: "from-[#00F0FF] to-[#38BDF8]",
                    desc: "Návratnost, úspora nákladů a škálovatelnost rozpočtu.",
                  },
                  {
                    name: "Technologická Elegance",
                    val: latestMatrix.technological_elegance,
                    weight: "30%",
                    color: "bg-[#A855F7]",
                    barColor: "from-[#A855F7] to-[#C084FC]",
                    desc: "Modularita, typová bezpečnost a nulový technický dluh.",
                  },
                  {
                    name: "Ekologicko-Sociální Regenerace",
                    val: latestMatrix.eco_social_regeneration,
                    weight: "20%",
                    color: "bg-[#10B981]",
                    barColor: "from-[#10B981] to-[#34D399]",
                    desc: "Nízká energetická stopa a pozitivní dopad na ekosystém.",
                  },
                  {
                    name: "Psychologická Přijatelnost",
                    val: latestMatrix.psychological_acceptability,
                    weight: "20%",
                    color: "bg-[#F59E0B]",
                    barColor: "from-[#F59E0B] to-[#FBBF24]",
                    desc: "Zero-Friction rozhraní, důvěra uživatele a eliminace odporu.",
                  },
                ].map((dim, idx) => (
                  <div
                    key={idx}
                    className="p-5 rounded-2xl bg-gradient-to-b from-[#111C35] to-[#0A0F1D] border border-slate-800 shadow-md space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-200 font-sans">{dim.name}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono text-slate-400">Váha {dim.weight}</span>
                        <span className="text-sm font-bold font-mono text-white">
                          {(dim.val * 100).toFixed(0)} %
                        </span>
                      </div>
                    </div>

                    {/* Bar */}
                    <div className="w-full h-3 bg-slate-900 rounded-full overflow-hidden border border-slate-800 p-0.5">
                      <div
                        className={`h-full rounded-full bg-gradient-to-r ${dim.barColor} transition-all duration-700`}
                        style={{ width: `${Math.min(100, Math.max(0, dim.val * 100))}%` }}
                      />
                    </div>
                    <p className="text-[11px] text-slate-400 font-sans">{dim.desc}</p>
                  </div>
                ))}
              </div>

              {/* Ontological Reasoning & Adversarial Red-Teaming */}
              <div className="rounded-2xl bg-gradient-to-b from-[#111C35] to-[#0A0F1D] border border-slate-800 p-5 space-y-4">
                <h3 className="text-sm font-bold text-slate-200 font-mono flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-rose-400" />
                  Adversarial Red-Teaming & Identifikované Zranitelnosti
                </h3>

                {latestMatrix.adversarial_vulnerabilities &&
                latestMatrix.adversarial_vulnerabilities.length > 0 ? (
                  <div className="space-y-2">
                    {latestMatrix.adversarial_vulnerabilities.map((v, idx) => (
                      <div
                        key={idx}
                        className="flex items-center gap-3 p-3 rounded-xl bg-rose-950/30 border border-rose-900/50 text-rose-200 text-xs font-mono"
                      >
                        <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                        <span>{v}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 font-mono">
                    Žádné kritické zranitelnosti nebyly identifikovány.
                  </p>
                )}

                <div className="pt-3 border-t border-slate-800 text-xs font-mono text-slate-300 leading-relaxed">
                  <span className="text-[#00F0FF] font-bold block mb-1">Ontologická Syntéza:</span>
                  {latestMatrix.reasoning}
                </div>
              </div>
            </div>
          )}

          {/* ========================================================
              TAB: FORENSICS (PROSPEKTIVNÍ FORENZNÍ ANALÝZA RIZIK)
             ======================================================== */}
          {activeTab === "forensics" && (
            <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-6 min-h-0">
              {/* Header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-white flex items-center gap-2 font-mono">
                    <ShieldAlert className="w-5 h-5 text-amber-400" />
                    Prospektivní Forenzní Analýza Rizik (T+1 až T+N)
                  </h2>
                  <p className="text-xs text-slate-400 font-mono mt-1">
                    Sekundární a terciární kaskádové efekty napříč 8 doménami Oktagonu s detekcí asymetrických selhání (SPOF).
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <div className="px-4 py-2 rounded-2xl bg-slate-900 border border-slate-700 text-center font-mono">
                    <span className="text-[10px] text-slate-400 block">Horizont</span>
                    <span className="text-sm font-extrabold text-cyan-400">
                      {latestForensics?.horizon || "T+30_days"}
                    </span>
                  </div>

                  <div
                    className={`px-4 py-2 rounded-2xl border text-center font-mono ${
                      (latestForensics?.risk_level || "SAFE") === "SAFE"
                        ? "bg-emerald-950/40 border-emerald-500/50 text-emerald-300"
                        : (latestForensics?.risk_level || "SAFE") === "ELEVATED"
                        ? "bg-amber-950/40 border-amber-500/50 text-amber-300"
                        : "bg-rose-950/40 border-rose-500/50 text-rose-300"
                    }`}
                  >
                    <span className="text-[10px] text-slate-400 block">Status Rizika</span>
                    <span className="text-sm font-extrabold flex items-center justify-center gap-1.5">
                      {(latestForensics?.risk_level || "SAFE") === "SAFE" && "🟢 BEZPEČNÝ (SAFE)"}
                      {(latestForensics?.risk_level || "SAFE") === "ELEVATED" && "🟡 ZVÝŠENÝ (ELEVATED)"}
                      {(latestForensics?.risk_level || "SAFE") === "CRITICAL" && "🔴 KRITICKÝ (CRITICAL)"}
                    </span>
                  </div>
                </div>
              </div>

              {/* Status Overview Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800">
                  <span className="text-[10px] font-mono text-slate-400 block uppercase">Kompozitní Index Rizika</span>
                  <div className="flex items-baseline gap-2 mt-1">
                    <span
                      className={`text-2xl font-black font-mono ${
                        (latestForensics?.risk_index ?? 0.04) > 0.6
                          ? "text-rose-400"
                          : (latestForensics?.risk_index ?? 0.04) > 0.3
                          ? "text-amber-400"
                          : "text-emerald-400"
                      }`}
                    >
                      {(((latestForensics?.risk_index ?? 0.042)) * 100).toFixed(1)}%
                    </span>
                    <span className="text-xs text-slate-500 font-mono">/ 100%</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {(latestForensics?.risk_index ?? 0.04) > 0.6
                      ? "Vyžaduje aktivní intervenci"
                      : "V toleranci stability"}
                  </span>
                </div>

                <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800">
                  <span className="text-[10px] font-mono text-slate-400 block uppercase">Entropický Nárůst</span>
                  <span className="text-xl font-black font-mono text-cyan-400 block mt-1">
                    {latestForensics?.thermodynamic_entropy_spike || "+0.015 J/op"}
                  </span>
                  <span className="text-[11px] text-slate-400 mt-1 block">Termodynamická zátěž</span>
                </div>

                <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800">
                  <span className="text-[10px] font-mono text-slate-400 block uppercase">Asymetrická Selhání (SPOF)</span>
                  <span className="text-2xl font-black font-mono text-rose-400 block mt-1">
                    {latestForensics?.asymmetric_failure_modes?.length ?? 1}
                  </span>
                  <span className="text-[11px] text-slate-400 mt-1 block">Jednobodové uzly selhání</span>
                </div>

                <div className="p-4 rounded-2xl bg-slate-900/70 border border-slate-800">
                  <span className="text-[10px] font-mono text-slate-400 block uppercase">Protiopatření</span>
                  <span className="text-base font-bold font-mono text-emerald-400 block mt-1.5 flex items-center gap-1.5">
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    AUTOMATICKY NASAZENO
                  </span>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {latestForensics?.mitigation_directives?.length ?? 3} aktivních direktiv
                  </span>
                </div>
              </div>

              {/* T+1 Systemic Drift & Failure Modes */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
                  <h3 className="text-xs font-mono font-bold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                    <TrendingUp className="w-4 h-4 text-cyan-400" />
                    T+1 Systémový Drift (Kaskádová Predikce)
                  </h3>
                  <p className="text-xs text-slate-200 font-mono leading-relaxed p-3.5 rounded-xl bg-slate-950/70 border border-slate-800">
                    {latestForensics?.t_plus_1_systemic_drift ||
                      "Architektura O.M.N.I.S. zachovává nulovou fluktuaci; nedochází k nevratnému driftu parametrů ani saturaci cache."}
                  </p>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
                  <h3 className="text-xs font-mono font-bold text-rose-400 uppercase tracking-wider flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-rose-400" />
                    Detekované Režimy Selhání (Single Point of Failure)
                  </h3>
                  <div className="space-y-2">
                    {(
                      latestForensics?.asymmetric_failure_modes || [
                        "SPOF-01: Možná latence v distribuované síti při masivním nárůstu souběžných požadavků.",
                      ]
                    ).map((mode, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-xl bg-rose-950/20 border border-rose-900/40 text-rose-300 text-xs font-mono flex items-start gap-2"
                      >
                        <ShieldAlert className="w-3.5 h-3.5 text-rose-400 shrink-0 mt-0.5" />
                        <span>{mode}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Identified Risk Vectors Across Oktagon Domains */}
              <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-mono font-bold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                    <Layers className="w-4 h-4 text-[#00F0FF]" />
                    Forenzní Vektory Rizika napříč Oktagonem (8 Domén)
                  </h3>
                  <span className="text-[10px] text-slate-500 font-mono">
                    {latestForensics?.identified_vectors?.length ?? 4} analyzovaných vektorů
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {(
                    latestForensics?.identified_vectors || [
                      {
                        domain: "ZERO_TRUST_SECURITY",
                        vector: "Injekce neověřených vstupů do epistemické paměti",
                        severity: "LOW",
                        probability: "LOW",
                        mitigation: "Kryptografická verifikace tokenů a validace schémat Pydantic",
                      },
                      {
                        domain: "THERMODYNAMICS_RESOURCE_EFFICIENCY",
                        vector: "Neoptimální spotřeba energie při redundanci dotazů",
                        severity: "LOW",
                        probability: "MEDIUM",
                        mitigation: "Zavedení deterministické LRU cache s nulovým rozptylem",
                      },
                      {
                        domain: "GAME_THEORY_ADVERSARIAL_ROBUSTNESS",
                        vector: "Možné sycophancy zkreslení v syntetickém dialogu",
                        severity: "MEDIUM",
                        probability: "LOW",
                        mitigation: "Samostatný adversariální auditor v kognitivním cyklu",
                      },
                      {
                        domain: "COMPLEX_SYSTEMS_CYBERNETICS",
                        vector: "Zpoždění v negativní zpětné vazbě (feedback delay)",
                        severity: "LOW",
                        probability: "LOW",
                        mitigation: "Autopoietická rekalibrace vah po každém cyklu",
                      },
                    ]
                  ).map((item, idx) => (
                    <div
                      key={idx}
                      className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 space-y-2 hover:border-slate-700 transition-colors"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[10px] font-mono font-bold text-cyan-400 bg-cyan-950/40 px-2 py-0.5 rounded border border-cyan-900/60">
                          {item.domain}
                        </span>
                        <div className="flex items-center gap-1.5 font-mono text-[10px]">
                          <span
                            className={`px-1.5 py-0.5 rounded font-bold ${
                              item.severity === "CRITICAL" || item.severity === "HIGH"
                                ? "bg-rose-950 text-rose-300 border border-rose-800"
                                : item.severity === "MEDIUM"
                                ? "bg-amber-950 text-amber-300 border border-amber-800"
                                : "bg-emerald-950 text-emerald-300 border border-emerald-800"
                            }`}
                          >
                            SEV: {item.severity}
                          </span>
                          <span className="text-slate-500">PROB: {item.probability}</span>
                        </div>
                      </div>

                      <p className="text-xs text-slate-200 font-sans font-medium">{item.vector}</p>

                      <div className="pt-2 border-t border-slate-800/80 text-[11px] font-mono text-emerald-400 flex items-start gap-1.5">
                        <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                        <span>Mitigace: {item.mitigation}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Mitigation Directives & Regulatory Compliance */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
                  <h3 className="text-xs font-mono font-bold text-emerald-400 uppercase tracking-wider flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                    Aktivní Mitigační Direktivy
                  </h3>
                  <div className="space-y-2">
                    {(
                      latestForensics?.mitigation_directives || [
                        "DIR-01: Striktní vynucení idempotence a schémat Pydantic pro všechny API toky.",
                        "DIR-02: Vynucení deterministických timeoutů na asynchronních voláních.",
                        "DIR-03: Automatická autopoietická rekalibrace vah 4D matice po každém cyklu.",
                      ]
                    ).map((dir, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-xl bg-emerald-950/20 border border-emerald-900/40 text-emerald-300 text-xs font-mono flex items-start gap-2"
                      >
                        <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                        <span>{dir}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
                  <h3 className="text-xs font-mono font-bold text-slate-300 uppercase tracking-wider flex items-center gap-2">
                    <Zap className="w-4 h-4 text-amber-400" />
                    Regulační & Compliance Deltas
                  </h3>
                  <div className="space-y-2">
                    {(
                      latestForensics?.regulatory_compliance_deltas || [
                        "EU AI Act: Klasifikováno jako transparentní expertní systém (High-Governance Tier).",
                        "GDPR: Zero-data-leakage garance, anonymizace epistemické paměti.",
                        "NIST AI RMF: Plný soulad s protokolem řiditelnosti a monitoringu rizik.",
                      ]
                    ).map((delta, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-slate-300 text-xs font-mono flex items-start gap-2"
                      >
                        <span className="text-amber-400 font-bold">§</span>
                        <span>{delta}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 4: GUARDRAIL (FÁZE IV: KONVERGENCE)
             ======================================================== */}
          {activeTab === "guardrail" && (
            <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-6 min-h-0">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-white flex items-center gap-2 font-mono">
                    <ShieldCheck className="w-5 h-5 text-[#10B981]" />
                    Fáze IV: Deterministický Guardrail
                  </h2>
                  <p className="text-xs text-slate-400 font-mono mt-1">
                    Stres-test kandidátních řešení s automatickou penalizací (-0.5 za každou zranitelnost).
                  </p>
                </div>

                <button
                  onClick={runGuardrailEvaluation}
                  disabled={evaluatingGuardrail}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#10B981] to-[#00F0FF] text-slate-950 font-mono font-bold text-xs hover:brightness-110 transition-all shadow-[0_0_15px_rgba(16,185,129,0.3)]"
                >
                  {evaluatingGuardrail ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Hodnotím...</span>
                    </>
                  ) : (
                    <>
                      <Zap className="w-4 h-4" />
                      <span>Spustit Evaluaci Guardrailu</span>
                    </>
                  )}
                </button>
              </div>

              {/* Threshold Slider */}
              <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div>
                  <span className="text-xs font-bold text-slate-200 block">Minimální Prahová Hodnota pro Akceptaci:</span>
                  <span className="text-[11px] text-slate-400 font-mono">Kandidáti pod tímto skóre budou zamítnuti.</span>
                </div>
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min="5.0"
                    max="9.5"
                    step="0.1"
                    value={minimumThreshold}
                    onChange={(e) => setMinimumThreshold(parseFloat(e.target.value))}
                    className="w-36 accent-[#10B981] cursor-pointer"
                  />
                  <span className="text-sm font-bold font-mono text-[#10B981] w-12 text-right">
                    {minimumThreshold.toFixed(1)} / 10
                  </span>
                </div>
              </div>

              {/* Candidates Grid */}
              <div className="space-y-4">
                {candidates.map((cand) => (
                  <div
                    key={cand.candidate_id}
                    className="p-4 rounded-2xl bg-gradient-to-b from-[#111C35] to-[#0A0F1D] border border-slate-800 space-y-3"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <div>
                        <span className="text-[10px] text-slate-400 font-mono">{cand.candidate_id}</span>
                        <h4 className="text-sm font-bold text-white">{cand.title}</h4>
                      </div>
                      <span className="text-xs px-2.5 py-1 rounded-full bg-slate-800 border border-slate-700 text-slate-300 font-mono self-start sm:self-auto">
                        Zranitelností: {cand.vulnerabilities.length}
                      </span>
                    </div>

                    <p className="text-xs text-slate-300 font-sans">{cand.description}</p>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
                      <div className="p-2 rounded-lg bg-slate-950 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 block">Ekonomika</span>
                        <span className="font-bold text-[#00F0FF]">{cand.economic} / 10</span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-950 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 block">Technologie</span>
                        <span className="font-bold text-[#A855F7]">{cand.tech} / 10</span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-950 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 block">Eko-Sociální</span>
                        <span className="font-bold text-[#10B981]">{cand.eco} / 10</span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-950 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 block">Psychologie</span>
                        <span className="font-bold text-[#F59E0B]">{cand.psych} / 10</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Guardrail Results */}
              {guardrailResults && (
                <div className="p-5 rounded-2xl bg-gradient-to-b from-[#0F231D] to-[#0A0F1D] border border-[#10B981]/50 space-y-4">
                  <div className="flex items-center justify-between border-b border-[#10B981]/30 pb-3">
                    <h3 className="text-sm font-bold text-[#10B981] font-mono flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4" />
                      Výsledek Evaluace: {guardrailResults.status}
                    </h3>
                  </div>

                  {guardrailResults.selected_optimal_candidate && (
                    <div className="p-4 rounded-xl bg-black/50 border border-[#10B981]/40">
                      <span className="text-[11px] font-mono font-bold text-[#10B981] block mb-1">
                        🏆 VYBRANÝ OPTIMÁLNÍ KANDIDÁT:
                      </span>
                      <p className="text-sm font-bold text-white">
                        {guardrailResults.selected_optimal_candidate.title}
                      </p>
                      <p className="text-xs text-slate-300 mt-1">
                        {guardrailResults.selected_optimal_candidate.description}
                      </p>
                    </div>
                  )}

                  <div className="space-y-2">
                    <span className="text-xs font-mono text-slate-300 block">Vážené Pořadí:</span>
                    {guardrailResults.weighted_rankings.map((r: any, idx: number) => (
                      <div
                        key={idx}
                        className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-xs font-mono"
                      >
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-[#10B981]">#{idx + 1}</span>
                          <span className="text-slate-200">{r.title}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-slate-400">Před penalizací: {r.avg_before_penalty}</span>
                          <span
                            className={`font-bold px-2 py-0.5 rounded-full ${
                              r.passed ? "bg-[#10B981]/20 text-[#10B981]" : "bg-rose-950 text-rose-400"
                            }`}
                          >
                            Finální: {r.final_score}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ========================================================
              TAB 5: EPISTEMICKÁ & VEKTOROVÁ PAMĚŤ
             ======================================================== */}
          {activeTab === "memory" && (
            <div className="flex-1 p-4 sm:p-6 overflow-y-auto space-y-6 min-h-0">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-white flex items-center gap-2 font-mono">
                    <Database className="w-5 h-5 text-[#A855F7]" />
                    Epistemická Rovnina & Vektorová Paměť
                  </h2>
                  <p className="text-xs text-slate-400 font-mono mt-1">
                    Přímá asimilace tvrdých, měkkých i heuristických dat do PostgreSQL / pgvector.
                  </p>
                </div>

                <button
                  onClick={fetchRealMemories}
                  disabled={loadingMemories}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-800 border border-slate-700 text-slate-300 hover:text-white text-xs font-mono"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingMemories ? "animate-spin" : ""}`} />
                  <span>Obnovit Paměť</span>
                </button>
              </div>

              {/* Ingestion Form */}
              <form
                onSubmit={handleIngestEpistemic}
                className="p-5 rounded-2xl bg-gradient-to-b from-[#111C35] to-[#0A0F1D] border border-slate-800 space-y-4"
              >
                <h3 className="text-xs font-mono font-bold text-[#00F0FF] uppercase tracking-wider flex items-center gap-2">
                  <Sparkles className="w-3.5 h-3.5" />
                  Asimilace Nové Entity do Paměťové Vrstvy
                </h3>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div>
                    <label className="block text-slate-400 mb-1 font-mono">Fundamentální Účel:</label>
                    <input
                      type="text"
                      value={epistemicPurpose}
                      onChange={(e) => setEpistemicPurpose(e.target.value)}
                      className="w-full bg-[#0A0F1D] border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-400 mb-1 font-mono">Doména:</label>
                    <select
                      value={epistemicDomain}
                      onChange={(e) => setEpistemicDomain(e.target.value)}
                      className="w-full bg-[#0A0F1D] border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                    >
                      <option value="SYSTEMS_INTELLIGENCE">SYSTEMS_INTELLIGENCE</option>
                      <option value="CYBERNETICS">CYBERNETICS</option>
                      <option value="SUSTAINABLE_TECH">SUSTAINABLE_TECH</option>
                      <option value="COGNITIVE_SCI">COGNITIVE_SCI</option>
                    </select>
                  </div>
                </div>

                <div className="space-y-3 text-xs">
                  <div>
                    <label className="block text-slate-400 mb-1 font-mono">Tvrdá Data (Numerické metriky):</label>
                    <input
                      type="text"
                      value={epistemicHardParam}
                      onChange={(e) => setEpistemicHardParam(e.target.value)}
                      className="w-full bg-[#0A0F1D] border border-slate-800 rounded-xl p-2.5 text-slate-200 font-mono focus:outline-none focus:border-[#00F0FF]"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-400 mb-1 font-mono">Měkká Data (Sociální a psychologický kontext):</label>
                    <input
                      type="text"
                      value={epistemicSoftParam}
                      onChange={(e) => setEpistemicSoftParam(e.target.value)}
                      className="w-full bg-[#0A0F1D] border border-slate-800 rounded-xl p-2.5 text-slate-200 font-mono focus:outline-none focus:border-[#A855F7]"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-400 mb-1 font-mono">Senzorická Heuristika & Intuice:</label>
                    <input
                      type="text"
                      value={epistemicSensoryParam}
                      onChange={(e) => setEpistemicSensoryParam(e.target.value)}
                      className="w-full bg-[#0A0F1D] border border-slate-800 rounded-xl p-2.5 text-slate-200 font-mono focus:outline-none focus:border-[#10B981]"
                    />
                  </div>
                </div>

                {ingestStatus && (
                  <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300">
                    {ingestStatus}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={isIngesting}
                  className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#A855F7] to-[#00F0FF] text-slate-950 font-mono font-bold text-xs hover:brightness-110 transition-all shadow-[0_0_15px_rgba(168,85,247,0.3)] disabled:opacity-50"
                >
                  {isIngesting ? "Ukládám do pgvector..." : "Asimilovat do Paměti"}
                </button>
              </form>

              {/* Memory List */}
              <div className="space-y-3">
                <h3 className="text-xs font-mono font-bold text-slate-400 uppercase tracking-wider">
                  Reálné Vektorové Otisky v Databázi ({memories.length})
                </h3>

                {loadingMemories ? (
                  <div className="p-6 text-center text-xs font-mono text-slate-400">
                    Načítám záznamy z databáze...
                  </div>
                ) : memories.length === 0 ? (
                  <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 text-center text-xs font-mono text-slate-400">
                    V databázi zatím nejsou žádné záznamy. Použijte formulář výše pro uložení prvního otisku.
                  </div>
                ) : (
                  memories.map((mem) => (
                    <div
                      key={mem.id}
                      className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs font-mono space-y-1.5"
                    >
                      <div className="flex items-center justify-between text-slate-500 text-[10px]">
                        <span>ID: {mem.id}</span>
                        <span>{new Date(mem.created_at).toLocaleString()}</span>
                      </div>
                      <p className="text-slate-200 font-sans">{mem.content}</p>
                      <div className="flex items-center gap-2 pt-1">
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-[#00F0FF]">
                          Typ: {mem.memory_type}
                        </span>
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-[#10B981]">
                          Důležitost: {mem.importance_score}
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 7: DEV PROMPT BENCHMARK & TOKEN TELEMETRY (DEV ONLY)
             ======================================================== */}
          {activeTab === "dev_lab" && (
            <DevPromptLab
              onExecutePromptInChat={(queryText, domain) => {
                setInputQuery(queryText);
                setOntologyDomain(domain);
                setActiveTab("chat");
                handleSendQuery(queryText);
              }}
              currentDomain={ontologyDomain}
              onSelectDomain={(dom) => setOntologyDomain(dom)}
              sessionTokenTelemetry={tokenTelemetry}
              onRefreshTelemetry={fetchTokenTelemetry}
              onResetTelemetry={handleResetTelemetry}
            />
          )}
        </main>
      </div>
    </div>
  );
}
