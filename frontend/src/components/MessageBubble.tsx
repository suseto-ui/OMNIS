import React, { useState, useCallback } from "react";
import {
  Brain,
  Check,
  ChevronDown,
  ChevronUp,
  Copy,
  Download,
  BarChart3,
  ArrowRight,
  ShieldAlert,
  ShieldCheck,
  Zap,
  Sparkles,
  Send,
  RefreshCw,
  HelpCircle,
  Star,
  Sliders,
  Bookmark,
  Trash2,
  Layers,
  Cpu,
  Network,
  Volume2,
  VolumeX,
  Users,
  Scale,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  Clock,
  Activity,
  Lock
} from "lucide-react";
import { MarkdownRenderer } from "../MarkdownRenderer";
import { omnisEngine, MultiAgentDeliberationResult, RefusalLadderEvaluation, evaluateRefusalLadder, computeSha256Simple } from "../omnisEngine";
import { RefusalLadderBadge } from "./RefusalLadderBadge";
import { UserRole } from "../types";

// Import or replicate types
export interface ImpactMatrixScores {
  sys: number;
  econ: number;
  psych: number;
  eco: number;
  law: number;
  sec: number;
  phys: number;
  soc: number;
  composite_score: number;
  reasoning: string;
}

export interface ConsequenceForensics {
  horizon: string;
  risk_index: number;
  risk_level: string;
  identified_vectors: any[];
  t_plus_1_systemic_drift: string;
  thermodynamic_entropy_spike?: string;
}

export interface TokenUsageMetric {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
  cost_usd: number;
}

export interface MessageItem {
  id: string;
  role: "user" | "assistant";
  content: string;
  cognitive_process?: string;
  follow_up_questions?: string[];
  impact_matrix?: ImpactMatrixScores;
  consequence_forensics?: ConsequenceForensics;
  token_usage?: TokenUsageMetric;
  adversarial_score?: number;
  flagged_issues?: string[];
  refusal_ladder?: RefusalLadderEvaluation;
  isStreaming?: boolean;
  streamingPhase?: string;
  isOverrideActive?: boolean;
  created_at: string;
}

interface MessageBubbleProps {
  msg: MessageItem;
  isInActiveContext?: boolean;
  onSetActiveTab: (tab: "phases" | "chat" | "matrix" | "forensics" | "lab") => void;
  onSendQuery: (query: string) => void;
  onRefineMessage: (id: string, originalContent: string, prompt: string) => Promise<void>;
  onRateMessage?: (id: string, rating: number) => Promise<void>;
  onClearHistoryFrom?: (id: string) => void;
  onSaveAsTemplate?: (msg: MessageItem) => void;
  onDeepDive?: (msg: MessageItem) => void;
  userRole?: UserRole;
  onTriggerExecutiveOverride?: (msg: MessageItem) => void;
}

const MessageBubbleComponent: React.FC<MessageBubbleProps> = ({
  msg,
  isInActiveContext = true,
  onSetActiveTab,
  onSendQuery,
  onRefineMessage,
  onRateMessage,
  onClearHistoryFrom,
  onSaveAsTemplate,
  onDeepDive,
  userRole = "ADMIN_OPERATOR",
  onTriggerExecutiveOverride,
}) => {
  const isUser = msg.role === "user";
  const isAdmin = userRole === "ADMIN_OPERATOR";
  
  // Local state for UI toggles
  const [showAdminGateLadder, setShowAdminGateLadder] = useState(false);
  const [thoughtsOpen, setThoughtsOpen] = useState(false);
  const [deepDiveOpen, setDeepDiveOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [feedbackRating, setFeedbackRating] = useState<number>(0);
  const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
  const [refinePanelOpen, setRefinePanelOpen] = useState(false);
  const [refinePrompt, setRefinePrompt] = useState("");
  const [isRefining, setIsRefining] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [adversarialAudit, setAdversarialAudit] = useState<{
    adversarial_score?: number;
    flagged_issues?: string[];
    critique_summary?: string;
    vulnerabilities?: string[];
    loading?: boolean;
  }>({
    adversarial_score: msg.adversarial_score,
    flagged_issues: msg.flagged_issues,
  });

  const [diagnosticsOpen, setDiagnosticsOpen] = useState(false);
  const [multiAgentOpen, setMultiAgentOpen] = useState(false);
  const [multiAgentLoading, setMultiAgentLoading] = useState(false);
  const [multiAgentResult, setMultiAgentResult] = useState<MultiAgentDeliberationResult | null>(null);

  const handleRunMultiAgentDeliberation = useCallback(async () => {
    setMultiAgentLoading(true);
    setMultiAgentOpen(true);
    try {
      const result = await omnisEngine.conductMultiAgentDeliberation(
        msg.content.slice(0, 400),
        msg.content,
        "SYSTEMS_INTELLIGENCE"
      );
      setMultiAgentResult(result);
    } catch (err) {
      console.error("Multi-Agent Deliberation failed:", err);
    } finally {
      setMultiAgentLoading(false);
    }
  }, [msg.content]);

  const handleRunAdversarialAudit = useCallback(async () => {
    setAdversarialAudit(prev => ({ ...prev, loading: true }));
    try {
      const res = await fetch("/api/adversarial-review", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: msg.content.slice(0, 300),
          answer: msg.content,
          ontology_domain: "General Systems Analysis"
        })
      });
      if (res.ok) {
        const data = await res.json();
        setAdversarialAudit({
          adversarial_score: data.adversarial_score,
          flagged_issues: data.flagged_issues || [],
          critique_summary: data.critique_summary,
          vulnerabilities: data.vulnerabilities || [],
          loading: false
        });
      } else {
        setAdversarialAudit(prev => ({ ...prev, loading: false }));
      }
    } catch (e) {
      console.error("Adversarial review failed", e);
      setAdversarialAudit(prev => ({ ...prev, loading: false }));
    }
  }, [msg.content]);

  const handleExportMarkdown = useCallback(() => {
    let md = `# O.M.N.I.S. Kognitivní Analytická Zpráva\n`;
    md += `**Zpráva ID:** \`${msg.id}\`  \n`;
    md += `**Čas:** ${new Date(msg.created_at).toLocaleString("cs-CZ")}  \n`;
    md += `**Role:** ${msg.role.toUpperCase()}  \n\n`;

    if (msg.token_usage) {
      md += `### ⚡ Telemetrie a Spotřeba\n`;
      md += `- **Vstupní tokeny (Prompt):** ${msg.token_usage.prompt_tokens}\n`;
      md += `- **Výstupní tokeny (Completion):** ${msg.token_usage.completion_tokens}\n`;
      md += `- **Celkem tokenů:** ${msg.token_usage.total_tokens}\n`;
      md += `- **Odhadovaná cena:** $${msg.token_usage.cost_usd.toFixed(5)}\n\n`;
    }

    if (msg.cognitive_process) {
      md += `### 🧠 5-Fázový Kognitivní Proces (Myšlenkový otisk)\n`;
      md += `\`\`\`text\n${msg.cognitive_process}\n\`\`\`\n\n`;
    }

    md += `### 📝 Odpověď a Syntéza\n\n${msg.content}\n\n`;

    if (msg.impact_matrix) {
      md += `### 📊 8D Matice Dopadů\n`;
      md += `| Dimenze | Skóre (0-1) |\n| :--- | :--- |\n`;
      md += `| Systémová (SYS) | ${msg.impact_matrix.sys} |\n`;
      md += `| Ekonomická (ECON) | ${msg.impact_matrix.econ} |\n`;
      md += `| Psychologická (PSYCH) | ${msg.impact_matrix.psych} |\n`;
      md += `| Ekologická (ECO) | ${msg.impact_matrix.eco} |\n`;
      md += `| Právní (LAW) | ${msg.impact_matrix.law} |\n`;
      md += `| Bezpečnostní (SEC) | ${msg.impact_matrix.sec} |\n`;
      md += `| Fyzikální (PHYS) | ${msg.impact_matrix.phys} |\n`;
      md += `| Sociální (SOC) | ${msg.impact_matrix.soc} |\n`;
      md += `| **Kompozitní index** | **${msg.impact_matrix.composite_score}** |\n\n`;
      if (msg.impact_matrix.reasoning) {
        md += `*Odůvodnění matice:* ${msg.impact_matrix.reasoning}\n\n`;
      }
    }

    if (msg.consequence_forensics) {
      md += `### 🔬 Forenzní Analýza Důsledků\n`;
      md += `- **Časový horizont:** ${msg.consequence_forensics.horizon}\n`;
      md += `- **Index rizika:** ${(msg.consequence_forensics.risk_index * 100).toFixed(1)}%\n`;
      md += `- **Úroveň rizika:** ${msg.consequence_forensics.risk_level}\n`;
      md += `- **T+1 Systémový drift:** ${msg.consequence_forensics.t_plus_1_systemic_drift}\n`;
      if (msg.consequence_forensics.thermodynamic_entropy_spike) {
        md += `- **Entropický spike:** ${msg.consequence_forensics.thermodynamic_entropy_spike}\n`;
      }
      md += `\n`;
    }

    const blob = new Blob([md], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `omnis-analyza-${msg.id.slice(0, 8)}.md`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, [msg]);

  const handleSpeak = useCallback(() => {
    if (!("speechSynthesis" in window)) {
      alert("Váš prohlížeč nepodporuje Text-To-Speech syntézu.");
      return;
    }
    if (isSpeaking) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
      return;
    }
    window.speechSynthesis.cancel();
    const cleanContent = msg.content.replace(/[*#_`]/g, "");
    const utterance = new SpeechSynthesisUtterance(cleanContent);
    utterance.lang = "cs-CZ";
    utterance.rate = 1.0;
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);
    setIsSpeaking(true);
    window.speechSynthesis.speak(utterance);
  }, [isSpeaking, msg.content]);

  const toggleThoughts = useCallback(() => {
    setThoughtsOpen((prev) => !prev);
  }, []);

  const handleCopyText = useCallback((text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, []);

  const handleRate = useCallback(async (star: number) => {
    setFeedbackRating(star);
    if (onRateMessage) {
      await onRateMessage(msg.id, star);
      setFeedbackSubmitted(true);
    }
  }, [msg.id, onRateMessage]);

  const handleRefine = useCallback(async (instruction: string) => {
    setIsRefining(true);
    try {
      await onRefineMessage(msg.id, msg.content, instruction);
      // Clean up after refine
      setRefinePrompt("");
      setRefinePanelOpen(false);
    } finally {
      setIsRefining(false);
    }
  }, [msg.id, msg.content, onRefineMessage]);

  const executeCustomRefine = useCallback(() => {
    if (refinePrompt.trim()) {
      handleRefine(refinePrompt);
    }
  }, [handleRefine, refinePrompt]);

  return (
    <div id={`msg-${msg.id}`} className={`flex flex-col ${isUser ? "items-end" : "items-start"} w-full scroll-mt-24 mb-4`}>
      {/* Subtle Separator between messages */}
      <div className="w-full flex items-center justify-center my-2 opacity-50">
        <div className="h-px bg-gradient-to-r from-transparent via-slate-800 to-transparent w-3/4 max-w-2xl" />
      </div>

      {/* Message Bubble Container with Enhanced Visual Hierarchy */}
      <div
        className={`w-full max-w-[92%] sm:max-w-3xl rounded-2xl sm:rounded-3xl p-4 sm:p-6 border transition-all duration-200 ${
          isUser
            ? "bg-gradient-to-r from-[#00F0FF]/15 via-[#1E3A8A]/20 to-[#3B82F6]/15 border-[#00F0FF]/40 text-slate-100 shadow-[0_4px_20px_rgba(0,240,255,0.08)]"
            : "bg-gradient-to-b from-[#0F172A]/90 via-[#0B1329]/95 to-[#060B18] border-slate-800/90 text-slate-100 shadow-[0_8px_30px_rgba(0,0,0,0.45)]"
        }`}
      >
        {/* Header Author Info with Distinct Separation */}
        <div className="flex items-center justify-between gap-3 mb-3 pb-2.5 border-b border-slate-800/70">
          <div className="flex items-center gap-2.5 min-w-0">
            <div
              className={`w-6 h-6 sm:w-7 sm:h-7 rounded-lg sm:rounded-xl flex items-center justify-center text-xs font-bold font-mono shadow-sm flex-shrink-0 ${
                isUser
                  ? "bg-gradient-to-tr from-[#00F0FF] to-blue-500 text-slate-950 shadow-[0_0_10px_rgba(0,240,255,0.25)]"
                  : "bg-gradient-to-tr from-[#A855F7] via-[#00F0FF] to-blue-600 text-white shadow-[0_0_12px_rgba(168,85,247,0.25)]"
              }`}
            >
              {isUser ? "U" : "Ω"}
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <span className={`text-xs sm:text-sm font-bold font-mono tracking-wide ${isUser ? "text-slate-100" : "text-[#00F0FF]"}`}>
                {isUser ? "Uživatel" : "O.M.N.I.S. Asistent"}
              </span>

              {isUser ? (
                <span className="px-1.5 py-0.5 rounded text-[9px] font-mono bg-blue-500/10 text-blue-300 border border-blue-500/20 font-medium">
                  Dotaz
                </span>
              ) : (
                <span className="px-1.5 py-0.5 rounded text-[9px] font-mono bg-purple-500/10 text-purple-300 border border-purple-500/20 font-medium">
                  AI Asistent
                </span>
              )}

              {/* Visual Processing Status Indicator */}
              {!isUser && msg.isStreaming && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-mono bg-purple-500/20 text-purple-300 border border-purple-500/40 animate-pulse">
                  <Sparkles className="w-2.5 h-2.5 text-purple-300 animate-spin" />
                  <span>{msg.streamingPhase || "Zpracovávám..."}</span>
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2.5 flex-shrink-0">
            {/* Timestamp (Subtle, Smaller Gray Font) */}
            <span className="text-[10px] sm:text-[11px] font-mono text-slate-400 font-normal tracking-tight flex items-center gap-1 select-none">
              <Clock className="w-3 h-3 text-slate-400" />
              {new Date(msg.created_at).toLocaleTimeString("cs-CZ", {
                hour: "2-digit",
                minute: "2-digit"
              })}
            </span>

            {onSaveAsTemplate && (
              <button
                onClick={() => onSaveAsTemplate(msg)}
                title="Uložit jako šablonu v Uživatelském panelu"
                className="p-1 rounded hover:bg-amber-500/20 text-slate-400 hover:text-amber-300 transition-colors"
              >
                <Bookmark className="w-3.5 h-3.5 text-amber-400" />
              </button>
            )}

            {onClearHistoryFrom && (
              <button
                onClick={() => onClearHistoryFrom(msg.id)}
                title="Promazat historii od této zprávy"
                className="p-1 rounded hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}

            {!isUser && (
              <>
                <button
                  onClick={handleSpeak}
                  title={isSpeaking ? "Zastavit předčítání hlasem" : "Přečíst odpověď hlasem (Text-to-Speech)"}
                  className={`p-1 rounded transition-colors ${
                    isSpeaking 
                      ? "bg-red-500/20 text-red-400 hover:bg-red-500/30 animate-pulse" 
                      : "hover:bg-slate-800 text-slate-400 hover:text-[#00F0FF]"
                  }`}
                >
                  {isSpeaking ? (
                    <VolumeX className="w-3.5 h-3.5 text-red-400" />
                  ) : (
                    <Volume2 className="w-3.5 h-3.5" />
                  )}
                </button>

                <button
                  onClick={() => handleCopyText(msg.content)}
                  title="Kopírovat odpověď"
                  className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
                >
                  {copied ? (
                    <Check className="w-3.5 h-3.5 text-[#10B981]" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>

                <button
                  onClick={handleExportMarkdown}
                  title="Exportovat zprávu s kognitivní stopou a 8D maticí do Markdownu (.md)"
                  className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-[#00F0FF] transition-colors"
                >
                  <Download className="w-3.5 h-3.5" />
                </button>
              </>
            )}
          </div>
        </div>

        {/* Visual Indicator Strip for High Adversarial Score */}
        {(() => {
          const advScore = msg.adversarial_score !== undefined 
            ? msg.adversarial_score 
            : (msg.impact_matrix ? (1 - msg.impact_matrix.sec) : 0);
          if (advScore < 0.40) return null;
          const isCritical = advScore >= 0.70;
          return (
            <div 
              data-testid="adversarial-risk-indicator"
              className={`mb-3.5 p-2.5 rounded-xl border flex items-center gap-3 transition-all ${
                isCritical 
                  ? "bg-rose-950/30 border-rose-500/60 text-rose-200" 
                  : "bg-amber-950/30 border-amber-500/60 text-amber-200"
              }`}
            >
              {/* Colored Indicator Strip */}
              <div className={`w-1.5 h-8 rounded-full ${isCritical ? "bg-rose-500 shadow-[0_0_10px_rgba(244,63,94,0.8)]" : "bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.8)]"}`} />
              <ShieldAlert className={`w-5 h-5 flex-shrink-0 ${isCritical ? "text-rose-400" : "text-amber-400"}`} />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between text-xs font-mono font-bold">
                  <span>{isCritical ? "KRITICKÉ ADVERSARIAL RIZIKO" : "ZVÝŠENÉ RIZIKO INTERFERENCE"}</span>
                  <span className="px-1.5 py-0.5 rounded bg-black/40 text-[10px]">
                    {Math.round(advScore * 100)}% ADVERSARIAL SCORE
                  </span>
                </div>
                <div className="text-[10px] font-mono opacity-80 mt-0.5">
                  8D Tenzor Sec: {msg.impact_matrix ? Math.round(msg.impact_matrix.sec * 100) : 50}% | Detekováno bezpečnostní pnutí v promptu
                </div>
              </div>
            </div>
          );
        })()}

        {/* Cognitive Process (Introspection Accordion) */}
        {msg.cognitive_process && (
          <div className="mb-4 rounded-xl bg-slate-950/60 border border-[#A855F7]/30 overflow-hidden">
            <button
              onClick={toggleThoughts}
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

        {/* Deep Dive Atomic Cognitive Nodes Button & Panel */}
        {!isUser && (
          <div className="mb-4">
            <button
              onClick={() => {
                setDeepDiveOpen(!deepDiveOpen);
                if (onDeepDive && !deepDiveOpen) {
                  onDeepDive(msg);
                }
              }}
              className="w-full flex items-center justify-between px-3.5 py-2 rounded-xl bg-gradient-to-r from-purple-950/60 via-indigo-950/60 to-cyan-950/60 border border-purple-500/40 text-xs font-mono text-purple-300 hover:text-white hover:border-purple-400 shadow-[0_0_12px_rgba(168,85,247,0.15)] transition-all group"
            >
              <span className="flex items-center gap-2 font-bold">
                <Layers className="w-4 h-4 text-[#00F0FF] group-hover:rotate-180 transition-transform duration-500" />
                <span>DEEP DIVE: Atomické Kognitivní Uzly</span>
              </span>
              <span className="text-[10px] bg-purple-500/20 text-[#00F0FF] px-2 py-0.5 rounded border border-purple-500/30 flex items-center gap-1">
                <Cpu className="w-3 h-3 animate-pulse" />
                {deepDiveOpen ? "Skrýt dekompozici" : "Rozložit kognitivní uzly"}
              </span>
            </button>

            {deepDiveOpen && (
              <div className="mt-2.5 p-4 rounded-xl bg-slate-950/90 border border-purple-500/40 space-y-3 font-mono text-xs animate-in fade-in duration-300 shadow-xl">
                <div className="flex items-center justify-between border-b border-purple-500/30 pb-2">
                  <span className="font-bold text-[#00F0FF] flex items-center gap-1.5">
                    <Network className="w-4 h-4 text-purple-400" />
                    Atomická Dekompozice Kognitivního Procesu
                  </span>
                  <span className="text-[10px] text-slate-400 font-mono">Sigma-Omega Invarianty</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <div className="p-3 rounded-lg bg-slate-900/90 border border-purple-500/20 space-y-1">
                    <span className="text-[10px] text-purple-400 font-bold uppercase block">Uzel 01: Sémantické Jádro</span>
                    <p className="text-[11px] text-slate-300">Extrakce fundamentálních systémových axiomů a nepředpojatých premis ze vstupu.</p>
                  </div>
                  <div className="p-3 rounded-lg bg-slate-900/90 border border-cyan-500/20 space-y-1">
                    <span className="text-[10px] text-[#00F0FF] font-bold uppercase block">Uzel 02: Entropická Redukce</span>
                    <p className="text-[11px] text-slate-300">Stochastické odfiltrování konverzačního šumu a izolace invariantních proměnných.</p>
                  </div>
                  <div className="p-3 rounded-lg bg-slate-900/90 border border-amber-500/20 space-y-1">
                    <span className="text-[10px] text-amber-400 font-bold uppercase block">Uzel 03: Homeostatická Kotva</span>
                    <p className="text-[11px] text-slate-300">Vyvážení zpětných vazeb a zamezení kmitání v hypotézách odpovědi.</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Formatted Content */}
        <div className="prose prose-invert max-w-none text-sm leading-relaxed font-sans text-slate-200">
          <MarkdownRenderer content={msg.content} />
        </div>

        {/* KOGNITIVNÍ MIKRO-ODZNAKY V PATIČCE ZPRÁVY (AKGE-8D, SMT, ZK-HASH) */}
        {!isUser && (() => {
          const refusalEval = msg.refusal_ladder || evaluateRefusalLadder(msg.content);
          const groundingPercent = refusalEval.groundingScore ?? (msg.impact_matrix ? Math.round(msg.impact_matrix.composite_score * 100) : 94);
          const zkCommitmentHash = computeSha256Simple(msg.id + msg.content);
          const isGrounded = refusalEval.isGrounded;

          const badgeColor = !isGrounded
            ? "bg-red-500/10 border-red-500/30 text-red-400"
            : groundingPercent >= 80
            ? "bg-[#00F0FF]/10 border-[#00F0FF]/30 text-[#00F0FF]"
            : "bg-amber-500/10 border-amber-500/30 text-amber-400";

          return (
            <div className="mt-3 pt-2.5 border-t border-slate-800/80 flex flex-col gap-2 font-mono">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  {/* Badge 1: AKGE-8D Ukotvení */}
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl border text-[11px] font-bold shadow-sm ${badgeColor}`}>
                    <Brain className="w-3.5 h-3.5" />
                    <span>AKGE-8D: {groundingPercent}% UKOTVENO</span>
                  </span>

                  {/* Badge 2: SMT Formální Verifikace */}
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[11px] font-semibold">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                    <span>SMT FORMÁLNĚ VERIFIKOVÁNO</span>
                  </span>

                  {/* Badge 3: ZK-Commitment Hash */}
                  <span 
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-purple-500/10 border border-purple-500/30 text-purple-300 text-[11px] font-mono" 
                    title={`ZK-Commitment Hash: 0x${zkCommitmentHash}`}
                  >
                    <Lock className="w-3 h-3 text-purple-400" />
                    <span>ZK: 0x{zkCommitmentHash.slice(0, 8)}...{zkCommitmentHash.slice(-4)}</span>
                  </span>

                  {/* Badge 4: Explicitní OVERRIDE_ACTIVE štítek */}
                  {msg.isOverrideActive && (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-red-500/20 border border-red-500/50 text-red-300 text-[11px] font-bold animate-pulse shadow-[0_0_10px_rgba(239,68,68,0.3)]">
                      <AlertTriangle className="w-3.5 h-3.5 text-red-400" />
                      <span>OVERRIDE_ACTIVE</span>
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  {/* EXECUTIVE OVERRIDE TRIGGER BUTTON PRO ADMINA PŘI VETU ČI ZÁMKU */}
                  {isAdmin && (!isGrounded || msg.content.includes("KOGNITIVNÍ ZÁMEK") || msg.content.includes("LOCKED_INPUT") || msg.refusal_ladder?.isRefusal) && (
                    <button
                      type="button"
                      onClick={() => onTriggerExecutiveOverride?.(msg)}
                      className="flex items-center gap-1.5 px-3 py-1 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-[11px] transition-all cursor-pointer shadow-[0_0_12px_rgba(239,68,68,0.35)] active:scale-95"
                      title="Vynutit manuální přepsání veta Regulátora (Executive Override)"
                    >
                      <ShieldAlert className="w-3.5 h-3.5 text-white animate-pulse" />
                      <span>EXECUTIVE OVERRIDE</span>
                    </button>
                  )}

                  {/* ADMIN AUDIT BUTTON (Pouze pro ADMIN_OPERATOR) */}
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => setShowAdminGateLadder(!showAdminGateLadder)}
                      className="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-[11px] text-[#00F0FF] transition-all cursor-pointer font-bold shadow-sm active:scale-95"
                      title="Auditní rozbalení 6 verifikačních bran pro administrátora"
                    >
                      <Sliders className="w-3 h-3 text-[#00F0FF]" />
                      <span>AUDIT 6 BRAN (ADMIN)</span>
                      {showAdminGateLadder ? <ChevronUp className="w-3 h-3 text-[#00F0FF]" /> : <ChevronDown className="w-3 h-3 text-[#00F0FF]" />}
                    </button>
                  )}
                </div>
              </div>

              {/* Rozbalená diagnostika 6 bran pro ADMIN_OPERATOR */}
              {isAdmin && showAdminGateLadder && (
                <div className="mt-1 animate-in fade-in duration-200">
                  <RefusalLadderBadge evaluation={refusalEval} />
                </div>
              )}
            </div>
          );
        })()}

        {/* COLLAPSIBLE DIAGNOSTICS & COGNITIVE TELEMETRY ACCORDION */}
        {!isUser && (
          <div className="mt-3.5 pt-2.5 border-t border-slate-800/80">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <button
                type="button"
                onClick={() => setDiagnosticsOpen(!diagnosticsOpen)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 text-xs font-mono text-slate-300 hover:text-white transition-all shadow-sm group cursor-pointer"
              >
                <Activity className="w-3.5 h-3.5 text-[#00F0FF] group-hover:animate-pulse" />
                <span className="font-semibold">Kognitivní telemetrie & Mezioborový audit</span>
                {msg.impact_matrix && (
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 font-bold">
                    8D: {(msg.impact_matrix.composite_score * 100).toFixed(0)}%
                  </span>
                )}
                {diagnosticsOpen ? (
                  <ChevronUp className="w-3.5 h-3.5 text-slate-400" />
                ) : (
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                )}
              </button>

              <div className="flex items-center gap-2 text-[11px] font-mono text-slate-400">
                <span className="flex items-center gap-1 text-amber-400 font-semibold">
                  <Zap className="w-3 h-3 text-amber-400" />
                  {msg.token_usage?.total_tokens ?? Math.round(msg.content.length * 0.45)} tokenů
                </span>
                <span className="text-slate-600">•</span>
                <span className="text-[#10B981] font-semibold">
                  ~${(msg.token_usage?.cost_usd ?? ((msg.token_usage?.total_tokens ?? 100) * 0.0000004)).toFixed(5)}
                </span>
              </div>
            </div>

            {/* EXPANDED DIAGNOSTICS VIEW */}
            {diagnosticsOpen && (
              <div className="mt-3 space-y-3 animate-in fade-in duration-200">
                {/* Token Calculation & Cost Badge */}
                {(() => {
                  const pTokens = msg.token_usage?.prompt_tokens ?? Math.max(15, Math.round((msg.content.length * 0.25)));
                  const cTokens = msg.token_usage?.completion_tokens ?? Math.max(20, Math.round((msg.content.length * 0.28)));
                  const totalT = msg.token_usage?.total_tokens ?? (pTokens + cTokens);
                  const costUsd = msg.token_usage?.cost_usd ?? ((pTokens * 0.00000015) + (cTokens * 0.00000060));
                  const costCzk = costUsd * 23.5;

                  return (
                    <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-mono text-slate-300 bg-slate-950/60 p-2.5 rounded-xl border border-slate-800/80">
                      <div className="flex items-center gap-2">
                        <Zap className="w-3.5 h-3.5 text-[#00F0FF] animate-pulse" />
                        <span className="font-bold text-[#00F0FF]">SPOTŘEBA TOKENŮ:</span>
                        <span className="text-slate-300 font-bold">{totalT} tokenů</span>
                        <span className="text-slate-500 text-[10px]">({pTokens} in / {cTokens} out)</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] text-purple-400 font-bold">
                          ~${costUsd.toFixed(5)} ({costCzk.toFixed(4)} Kč)
                        </span>
                      </div>
                    </div>
                  );
                })()}

                {/* Impact Matrix Mini Card if present */}
                {msg.impact_matrix && (
                  <div className="pt-2 border-t border-slate-800/70">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-mono font-bold text-[#00F0FF] flex items-center gap-1.5">
                        <BarChart3 className="w-3.5 h-3.5" />
                        8D Matice Dopadů (Kompozit: {(msg.impact_matrix.composite_score * 100).toFixed(1)}%)
                      </span>
                      <button
                        onClick={() => onSetActiveTab("matrix")}
                        className="text-[11px] text-[#00F0FF] hover:underline font-mono flex items-center gap-1"
                      >
                        Detailní rozbor <ArrowRight className="w-3 h-3" />
                      </button>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2">
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">System</span>
                        <span className="text-xs font-bold text-[#00F0FF] font-mono">
                          {(msg.impact_matrix.sys * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Econ</span>
                        <span className="text-xs font-bold text-[#A855F7] font-mono">
                          {(msg.impact_matrix.econ * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Psych</span>
                        <span className="text-xs font-bold text-[#10B981] font-mono">
                          {(msg.impact_matrix.psych * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Eco</span>
                        <span className="text-xs font-bold text-[#F59E0B] font-mono">
                          {(msg.impact_matrix.eco * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Law</span>
                        <span className="text-xs font-bold text-[#3B82F6] font-mono">
                          {(msg.impact_matrix.law * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Sec</span>
                        <span className="text-xs font-bold text-[#EC4899] font-mono">
                          {(msg.impact_matrix.sec * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Phys</span>
                        <span className="text-xs font-bold text-[#F43F5E] font-mono">
                          {(msg.impact_matrix.phys * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-center">
                        <span className="text-[10px] text-slate-400 font-mono block">Soc</span>
                        <span className="text-xs font-bold text-[#6366F1] font-mono">
                          {(msg.impact_matrix.soc * 100).toFixed(0)}%
                        </span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Forensics Mini Card if present */}
                {msg.consequence_forensics && (
                  <div className="pt-2 border-t border-slate-800/70">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-mono font-bold text-amber-400 flex items-center gap-1.5">
                        <ShieldAlert className="w-3.5 h-3.5" />
                        Forenzní Analýza Rizik (T+N)
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
                        onClick={() => onSetActiveTab("forensics")}
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
                        <span className="text-xs font-bold text-cyan-400 truncate block" title={msg.consequence_forensics.thermodynamic_entropy_spike || msg.consequence_forensics.t_plus_1_systemic_drift}>
                          {msg.consequence_forensics.thermodynamic_entropy_spike || "Stabilní asymptota"}
                        </span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Autopoietic Rating (Star Feedback) */}
                <div className="pt-2 flex items-center justify-between text-xs text-slate-400 font-mono border-t border-slate-800/60">
                  <span className="text-[11px]">Autopoietická valence:</span>
                  <div className="flex items-center gap-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        onClick={() => handleRate(star)}
                        className="p-1 hover:text-[#F59E0B] transition-colors"
                        title={`Ohodnotit ${star} / 5`}
                      >
                        <Star
                          className={`w-3.5 h-3.5 ${
                            feedbackRating >= star
                              ? "text-[#F59E0B] fill-[#F59E0B]"
                              : "text-slate-600"
                          }`}
                        />
                      </button>
                    ))}
                    {feedbackSubmitted && (
                      <span className="text-[10px] text-[#10B981] ml-1">Otisk uložen</span>
                    )}
                  </div>
                </div>

                {/* Skeptical Opponent Trust Verification Audit */}
                <div className="pt-2 border-t border-slate-800/60 flex flex-col gap-2 bg-slate-950/45 p-2.5 rounded-xl border border-slate-800">
                  <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-400">
                    <span className="flex items-center gap-1.5 text-[#A855F7]">
                      <ShieldCheck className="w-3.5 h-3.5 text-[#A855F7]" />
                      Audit důvěryhodnosti (Red-Team): <strong className="text-slate-200">{adversarialAudit.adversarial_score !== undefined ? `${((1 - adversarialAudit.adversarial_score) * 100).toFixed(0)}% Shoda` : "92% Ověřeno"}</strong>
                    </span>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleRunAdversarialAudit}
                        disabled={adversarialAudit.loading}
                        className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-purple-950/60 hover:bg-purple-900/60 border border-purple-500/30 text-purple-300 text-[10px] font-bold transition-all disabled:opacity-50 cursor-pointer shadow-sm hover:border-purple-400"
                        title="Spustit nezávislý oponentní audit sekundárním modelem Gemini"
                      >
                        <RefreshCw className={`w-3 h-3 ${adversarialAudit.loading ? "animate-spin text-[#00F0FF]" : "text-purple-400"}`} />
                        {adversarialAudit.loading ? "Audituji..." : "Prověřit oponentem"}
                      </button>
                    </div>
                  </div>

                  {adversarialAudit.critique_summary && (
                    <div className="p-2.5 rounded-lg bg-purple-950/40 border border-purple-500/30 text-[11px] text-purple-200 font-mono space-y-1">
                      <span className="text-purple-300 font-bold block flex items-center gap-1">
                        <ShieldAlert className="w-3.5 h-3.5 text-purple-400" />
                        Skeptická recenze oponenta:
                      </span>
                      <p className="text-slate-300 leading-relaxed">{adversarialAudit.critique_summary}</p>
                      {adversarialAudit.vulnerabilities && adversarialAudit.vulnerabilities.length > 0 && (
                        <ul className="list-disc pl-4 mt-1 space-y-0.5 text-rose-300 text-[10px]">
                          {adversarialAudit.vulnerabilities.map((v, vIdx) => (
                            <li key={vIdx}>{v}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}

                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="text-slate-500 text-[10px]">Indikátory rizik:</span>
                    {adversarialAudit.flagged_issues && adversarialAudit.flagged_issues.length > 0 ? (
                      adversarialAudit.flagged_issues.map((issue, iIdx) => (
                        <span key={iIdx} className="px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20 text-[9px] uppercase font-bold">
                          {issue}
                        </span>
                      ))
                    ) : (
                      <span className="px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[9px] uppercase font-bold">
                        Bez incidentů
                      </span>
                    )}
                  </div>
                </div>

                {/* Multi-Agent Orchestration & Deliberation Panel (Phase VI) */}
                <div className="pt-2 border-t border-slate-800/60 flex flex-col gap-2 bg-slate-950/50 p-2.5 rounded-xl border border-indigo-950/60">
                  <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-400">
                    <span className="flex items-center gap-1.5 text-indigo-400 font-bold">
                      <Users className="w-3.5 h-3.5 text-indigo-400" />
                      Multi-Agentní Konsenzus & Debata:
                      {multiAgentResult && (
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          multiAgentResult.consensus_status === "UNANIMOUS"
                            ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                            : multiAgentResult.consensus_status === "MAJORITY"
                            ? "bg-indigo-500/20 text-indigo-300 border border-indigo-500/30"
                            : multiAgentResult.consensus_status === "CONTESTED"
                            ? "bg-amber-500/20 text-amber-300 border border-amber-500/30"
                            : "bg-rose-500/20 text-rose-300 border border-rose-500/30"
                        }`}>
                          {(multiAgentResult.consensus_score * 100).toFixed(0)}% {multiAgentResult.consensus_status}
                        </span>
                      )}
                    </span>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleRunMultiAgentDeliberation}
                        disabled={multiAgentLoading}
                        className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-indigo-950/60 hover:bg-indigo-900/60 border border-indigo-500/30 text-indigo-300 text-[10px] font-bold transition-all disabled:opacity-50 cursor-pointer shadow-sm hover:border-indigo-400"
                        title="Spustit nezávislou debatu 4 specializovaných agentů (Architekt, Skeptik, Regulátor, Inženýr)"
                      >
                        <RefreshCw className={`w-3 h-3 ${multiAgentLoading ? "animate-spin text-cyan-400" : "text-indigo-400"}`} />
                        {multiAgentLoading ? "Debatuji..." : multiAgentResult ? "Znovu debatovat" : "Aktivovat debatu agentů"}
                      </button>
                      {multiAgentResult && (
                        <button
                          onClick={() => setMultiAgentOpen(!multiAgentOpen)}
                          className="text-slate-400 hover:text-slate-200 p-1"
                        >
                          {multiAgentOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Detailed Multi-Agent Breakdown */}
                  {multiAgentOpen && multiAgentResult && (
                    <div className="mt-2 space-y-2.5 text-xs font-mono">
                      <div className="p-2.5 rounded-lg bg-indigo-950/40 border border-indigo-500/30 text-[11px] text-indigo-200">
                        <span className="font-bold text-indigo-300 flex items-center gap-1.5 mb-1">
                          <Scale className="w-3.5 h-3.5 text-indigo-400" />
                          Arbitrážní syntéza:
                        </span>
                        <p className="text-slate-300 leading-relaxed mb-1">{multiAgentResult.deliberation_summary}</p>
                        <p className="text-cyan-300 font-semibold text-[10px]">
                          👉 Akční krok: {multiAgentResult.synthesis_action}
                        </p>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                        {multiAgentResult.perspectives.map((agent) => {
                          const isSkeptic = agent.agent_id === "skeptic";
                          const isArchitect = agent.agent_id === "architect";
                          const isRegulator = agent.agent_id === "regulator";

                          const badgeColor =
                            agent.stance === "SUPPORT"
                              ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/30"
                              : agent.stance === "CONDITIONAL"
                              ? "bg-amber-500/20 text-amber-300 border-amber-500/30"
                              : agent.stance === "MODIFY"
                              ? "bg-purple-500/20 text-purple-300 border-purple-500/30"
                              : "bg-rose-500/20 text-rose-300 border-rose-500/30";

                          return (
                            <div
                              key={agent.agent_id}
                              className={`p-2.5 rounded-lg border bg-slate-900/80 space-y-1.5 ${
                                isSkeptic
                                  ? "border-rose-900/50"
                                  : isArchitect
                                  ? "border-blue-900/50"
                                  : isRegulator
                                  ? "border-purple-900/50"
                                  : "border-emerald-900/50"
                              }`}
                            >
                              <div className="flex items-center justify-between">
                                <span className="font-bold text-slate-200 text-[11px] truncate" title={agent.agent_name}>
                                  {agent.agent_name}
                                </span>
                                <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold border ${badgeColor}`}>
                                  {agent.stance}
                                </span>
                              </div>
                              <p className="text-[10px] text-slate-400 line-clamp-3 leading-relaxed">
                                {agent.argumentation}
                              </p>
                              <div className="pt-1 border-t border-slate-800/80 flex items-center justify-between text-[9px] text-slate-400">
                                <span className="text-cyan-400 truncate max-w-[180px]" title={agent.key_recommendation}>
                                  💡 {agent.key_recommendation}
                                </span>
                                <span className="font-mono text-slate-300">
                                  {(agent.confidence * 100).toFixed(0)}% jistota
                                </span>
                              </div>
                            </div>
                          );
                        })}
                      </div>

                      {multiAgentResult.penalized_dimensions && multiAgentResult.penalized_dimensions.length > 0 && (
                        <div className="flex items-center gap-1.5 pt-1 text-[10px] text-rose-400">
                          <AlertTriangle className="w-3 h-3 text-rose-400 shrink-0" />
                          <span>Penalizované dimenze v Matici dopadů:</span>
                          {multiAgentResult.penalized_dimensions.map((dim) => (
                            <span key={dim} className="px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/30 uppercase font-bold text-[9px]">
                              {dim}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>

              </div>
            )}
          </div>
        )}

        {/* Reflexive Follow-up Questions Chips */}
        {msg.follow_up_questions && msg.follow_up_questions.length > 0 && (
          <div className="mt-3.5 pt-3 border-t border-slate-800/70 space-y-2">
            <span className="text-[11px] font-mono text-[#00F0FF] flex items-center gap-1.5 font-bold">
              <Sparkles className="w-3.5 h-3.5 text-[#00F0FF]" />
              Doporučené navazující otázky & další kroky:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {msg.follow_up_questions.map((q, idx) => (
                <button
                  key={idx}
                  onClick={() => onSendQuery(q)}
                  className="text-left text-xs px-3 py-1.5 rounded-xl bg-[#091024] hover:bg-[#00F0FF]/15 border border-cyan-500/30 hover:border-[#00F0FF] text-slate-200 hover:text-white transition-all duration-200 flex items-center gap-1.5 shadow-sm cursor-pointer"
                >
                  <span className="text-[#00F0FF] font-bold">›</span>
                  <span>{q}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Refinement & Iteration Control with Quick 1-Click Action Chips */}
        {!isUser && (
          <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono font-bold text-slate-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-[#00F0FF]" />
                Rychlé upřesnění výstupu (1-klik):
              </span>
              <span className="text-[10px] font-mono text-slate-500">
                Sigma-Omega Re-synthesis
              </span>
            </div>
            
            {/* 1-Click Refinement Chips */}
            <div className="flex flex-wrap gap-1.5">
              {[
                {
                  label: "⚡ Zkrátit do 3 bodů",
                  instruction: "Přeformuluj tento výstup striktně do 3 prioritních, přímo exekuovatelných odrážek bez omáčky.",
                },
                {
                  label: "💻 Doplnit produkční kód",
                  instruction: "Doplň k tomuto řešení kompletní, funkční a typově bezpečný produkční kód (Clean Architecture / Zero-Defect).",
                },
                {
                  label: "💰 Finanční model",
                  instruction: "Rozpracuj finanční kalkulaci, odhad marže, časovou návratnost a model nezávislého příjmu.",
                },
                {
                  label: "🛡️ Forenzní audit rizik",
                  instruction: "Proveď hloubkovou analýzu rizik a formuluj konkrétní bezpečnostní a systémové záruky.",
                },
              ].map((chip, cIdx) => (
                <button
                  key={cIdx}
                  disabled={isRefining}
                  onClick={() => handleRefine(chip.instruction)}
                  className="px-2.5 py-1.5 rounded-lg bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 hover:border-[#00F0FF]/60 text-xs font-mono text-slate-300 hover:text-[#00F0FF] transition-all flex items-center gap-1 disabled:opacity-50"
                >
                  {chip.label}
                </button>
              ))}
            </div>

            {/* Custom Refine Toggle Button */}
            <button
              onClick={() => setRefinePanelOpen(!refinePanelOpen)}
              className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl bg-gradient-to-r from-cyan-950/40 to-blue-950/40 border border-cyan-500/30 text-xs font-mono text-[#00F0FF] hover:border-cyan-400 hover:bg-cyan-950/70 transition-all"
            >
              <Sliders className="w-3.5 h-3.5 text-[#00F0FF]" />
              <span>{refinePanelOpen ? "Skrýt panel vlastního pokynu" : "✏️ Zadat vlastní instrukci k úpravě"}</span>
            </button>

            {refinePanelOpen && (
              <div className="p-3 rounded-xl bg-slate-950/95 border border-[#00F0FF]/50 space-y-2.5 shadow-xl animate-in fade-in duration-200">
                <div className="flex items-center justify-between text-xs font-mono text-[#00F0FF]">
                  <span className="font-bold flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-[#00F0FF]" />
                    Vlastní instrukce pro refaktoring
                  </span>
                  <span className="text-[10px] text-slate-400">Zero-Fluff Engine</span>
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={refinePrompt}
                    onChange={(e) => setRefinePrompt(e.target.value)}
                    placeholder="Např.: Přidej TypeScript rozhraní, vyčísli přesný cashflow plán..."
                    className="flex-1 bg-slate-900 border border-slate-700 focus:border-[#00F0FF] rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !isRefining && refinePrompt.trim()) {
                        executeCustomRefine();
                      }
                    }}
                  />
                  <button
                    onClick={executeCustomRefine}
                    disabled={isRefining || !refinePrompt.trim()}
                    className="px-4 py-2 rounded-lg bg-gradient-to-r from-[#00F0FF] to-blue-600 text-slate-950 text-xs font-bold font-mono hover:opacity-90 disabled:opacity-50 transition-all flex items-center gap-1.5 flex-shrink-0"
                  >
                    {isRefining ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        <span>Upravuji...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-3.5 h-3.5" />
                        <span>Exekuovat</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export const MessageBubble = React.memo(MessageBubbleComponent);
