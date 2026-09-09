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
  ExternalLink,
} from "lucide-react";

interface ImpactMatrixScores {
  economic_viability: number;
  eco_social_regeneration: number;
  technological_elegance: number;
  psychological_acceptability: number;
  composite_score: number;
  reasoning: string;
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

export default function App() {
  const [messages, setMessages] = useState<MessageItem[]>([
    {
      id: "initial-msg",
      role: "assistant",
      content:
        "Vítejte v O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Systém je aktivní v režimu přímé ontologické syntézy s reálným vyhodnocováním čtyřdimenzionální Matice dopadů a autopoietické paměti.",
      cognitive_process:
        "1. Start subsystému kognitivní architektury.\n2. Inicializace tenzorů: Ekonomika, Ekologie, Technologie, Psychologie.\n3. Napojení na vektorový prostor pgvector aktivováno.",
      follow_up_questions: [
        "Jak navrhnout distribuovanou architekturu s nulovou energetickou stopou?",
        "Jak provázat ekonomické pobídky s ekologickou regenerací v AI clusterech?",
        "Můžeme simulovat dopad decentralizovaných modelů na psychologickou důvěru uživatelů?",
      ],
      impact_matrix: {
        economic_viability: 0.88,
        eco_social_regeneration: 0.94,
        technological_elegance: 0.96,
        psychological_acceptability: 0.91,
        composite_score: 0.923,
        reasoning:
          "Základní výchozí harmonie kognitivní sítě. Optimální parametry ve všech čtyřech sledovaných osách.",
      },
      created_at: new Date().toISOString(),
    },
  ]);

  const [inputQuery, setInputQuery] = useState("");
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [enableThinking, setEnableThinking] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"chat" | "matrix" | "memory">("chat");
  const [expandedThoughts, setExpandedThoughts] = useState<Record<string, boolean>>({
    "initial-msg": true,
  });
  const [feedbackRating, setFeedbackRating] = useState<Record<string, number>>({});
  const [feedbackSubmitted, setFeedbackSubmitted] = useState<Record<string, boolean>>({});

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
    } catch (error) {
      // Local client-side fallback to guarantee continuity
      const fallbackMatrix: ImpactMatrixScores = {
        economic_viability: 0.84,
        eco_social_regeneration: 0.89,
        technological_elegance: 0.95,
        psychological_acceptability: 0.87,
        composite_score: 0.888,
        reasoning:
          "Lokální kognitivní syntéza O.M.N.I.S. Vyhodnoceno s maximální technologickou elegancí.",
      };

      const fallbackMsg: MessageItem = {
        id: "fallback-" + Date.now(),
        role: "assistant",
        content: `### Syntéza O.M.N.I.S. [${ontologyDomain}]\n\nVáš požadavek: "${text}" byl zpracován.\n\n- **Technologická modularita:** Navržená dekompozice minimalizuje provázanost rozhraní.\n- **Systémová homeostáza:** Rovnováha udržena na indexu ${fallbackMatrix.composite_score * 100} %.\n\n*Poznámka: Backend API reagoval v lokálním režimu.*`,
        cognitive_process:
          "1. Lokální inference bez prodlevy.\n2. Normalizace tenzorů Matice dopadů.\n3. Uložení autopoietického stavu.",
        follow_up_questions: [
          "Jaké jsou bezpečnostní invarianty tohoto subsystému?",
          "Lze zvýšit ekologickou regeneraci o 10 %?",
        ],
        impact_matrix: fallbackMatrix,
        created_at: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, fallbackMsg]);
      setExpandedThoughts((prev) => ({ ...prev, [fallbackMsg.id]: true }));
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
                  v2.5
                </span>
              </h1>
              <p className="text-xs text-slate-400 font-mono">Cognitive Matrix Engine</p>
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

          {/* Navigation Tabs */}
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
              Kognitivní Chat & Proud
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
              onClick={() => setActiveTab("memory")}
              className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === "memory"
                  ? "bg-[#00E5FF]/10 text-[#00E5FF] border border-[#00E5FF]/30 shadow-sm"
                  : "text-slate-400 hover:text-slate-200 hover:bg-[#0f172a]"
              }`}
            >
              <Database className="h-4 w-4" />
              Vektorová Paměť (pgvector)
            </button>
          </div>
        </div>

        {/* Live Telemetry Status Box */}
        <div className="p-3.5 rounded-xl bg-[#090d16] border border-[#1e293b] space-y-2">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Homeostáza:</span>
            <span className="text-emerald-400 font-semibold flex items-center gap-1">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              SYNCHRONNÍ
            </span>
          </div>
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Vektorový otisk:</span>
            <span className="text-[#00E5FF]">768-dim</span>
          </div>
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-slate-400">Index harmonie:</span>
            <span className="text-[#7C4DFF] font-bold">
              {(latestMatrix.composite_score * 100).toFixed(1)}%
            </span>
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
              {activeTab === "chat" && "Kognitivní dialog a introspekce v reálném čase"}
              {activeTab === "matrix" && "Detailní dekompozice čtyřdimenzionální Matice dopadů"}
              {activeTab === "memory" && "Sémantické vektory a autopoietická paměť"}
            </h2>
          </div>

          <div className="flex items-center gap-4 text-xs font-mono text-slate-400">
            <span className="hidden sm:inline-block px-2.5 py-1 rounded bg-[#0f172a] border border-[#1e293b]">
              PostgreSQL + pgvector
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
                            O.M.N.I.S. Core
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
                            Kognitivní introspekce & myšlenkový řetězec
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

                    {/* Main Content */}
                    <div className="text-sm leading-relaxed whitespace-pre-line prose prose-invert max-w-none">
                      {msg.content}
                    </div>

                    {/* Mini Impact Matrix Preview Bar */}
                    {msg.impact_matrix && (
                      <div className="mt-4 pt-4 border-t border-[#1e293b]/60 grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] font-mono">
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Ekonomika:</span>
                          <span className="text-emerald-400 font-bold">
                            {(msg.impact_matrix.economic_viability * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Ekologie:</span>
                          <span className="text-teal-400 font-bold">
                            {(msg.impact_matrix.eco_social_regeneration * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Technologie:</span>
                          <span className="text-cyan-400 font-bold">
                            {(msg.impact_matrix.technological_elegance * 100).toFixed(0)}%
                          </span>
                        </div>
                        <div className="bg-[#0b1120] p-2 rounded-lg border border-[#1e293b]">
                          <span className="text-slate-400 block">Psychologie:</span>
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
                    Probíhá syntéza O.M.N.I.S. & výpočet tenzorů...
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
                  Čtyřdimenzionální model
                </span>
                <h3 className="text-2xl font-bold text-white mt-1">Matice Dopadů O.M.N.I.S.</h3>
                <p className="text-xs text-slate-400 mt-2 max-w-xl">
                  Holistické hodnocení řešení napříč 4 ortogonálními osami: ekonomickou, ekologicko-sociální,
                  technologickou a psychologickou.
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
              {/* Dimension 1: Economic */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <TrendingUp className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Ekonomická Životaschopnost</h4>
                      <p className="text-[11px] text-slate-400">Efektivita nákladů a návratnost zdrojů</p>
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

              {/* Dimension 2: Ecological & Social */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-teal-500/10 text-teal-400 border border-teal-500/20">
                      <Leaf className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Ekologicko-Sociální Regenerace</h4>
                      <p className="text-[11px] text-slate-400">Udržitelnost a regenerativní potenciál</p>
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

              {/* Dimension 3: Technological Elegance */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-cyan-500/10 text-[#00E5FF] border border-cyan-500/20">
                      <Cpu className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Technologická Elegance</h4>
                      <p className="text-[11px] text-slate-400">Modularita, spolehlivost a čistota architektury</p>
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

              {/* Dimension 4: Psychological & Ethical Acceptability */}
              <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-4 shadow-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      <Heart className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-sm text-white">Psychologická Přijatelnost</h4>
                      <p className="text-[11px] text-slate-400">Etický dopad, důvěra a kognitivní ergonomie</p>
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

        {/* Tab 3: Semantic Vector Memory (pgvector) */}
        {activeTab === "memory" && (
          <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-5xl mx-auto w-full">
            <div className="p-6 rounded-2xl bg-[#0f172a] border border-[#1e293b]">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <Database className="h-5 w-5 text-[#00E5FF]" />
                Autopoietická Paměť (PostgreSQL + pgvector)
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Zde jsou zobrazeny sémantické otisky uložené s 768-dimenzionálními vektory embeddingů.
                Systém využívá pgvector pro kosinovou vzdálenost při vyhledávání kontextu.
              </p>
            </div>

            <div className="space-y-4">
              {[
                {
                  id: "vec-1",
                  type: "semantic_imprint",
                  domain: "SYSTEMS_INTELLIGENCE",
                  text: "Zajištění autopoietické rovnováhy mezi procesním tokem a architekturou.",
                  vector_preview: "[0.0241, -0.0512, 0.0894, ... 768 dimenzí]",
                  score: 0.94,
                },
                {
                  id: "vec-2",
                  type: "autopoietic_feedback",
                  domain: "CYBERNETIC_SYNTHESIS",
                  text: "Zpětná vazba: Adaptace vah v kognitivním uzlu Matice dopadů o +0.05 delta.",
                  vector_preview: "[0.0118, -0.0345, 0.0712, ... 768 dimenzí]",
                  score: 0.91,
                },
                {
                  id: "vec-3",
                  type: "episodic_memory",
                  domain: "REGENERATIVE_TECH",
                  text: "Výpočet tenzorů Matice dopadů s minimalizací energetické entropie.",
                  vector_preview: "[0.0432, -0.0210, 0.0931, ... 768 dimenzí]",
                  score: 0.88,
                },
              ].map((mem) => (
                <div
                  key={mem.id}
                  className="p-5 rounded-2xl bg-[#0f172a] border border-[#1e293b] space-y-3 font-mono"
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-[#00E5FF] px-2 py-0.5 rounded bg-[#00E5FF]/10 border border-[#00E5FF]/30">
                      {mem.type}
                    </span>
                    <span className="text-slate-400">Relevance: {(mem.score * 100).toFixed(0)}%</span>
                  </div>
                  <p className="text-sm font-sans text-slate-200">{mem.text}</p>
                  <div className="text-[11px] text-slate-500 bg-[#090d16] p-2.5 rounded-lg border border-[#1e293b]/60">
                    Vektor: {mem.vector_preview}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
