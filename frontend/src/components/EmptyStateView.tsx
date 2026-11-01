import React, { useState, useEffect } from "react";
import {
  Sparkles,
  MessageSquare,
  Brain,
  Activity,
  Mic,
  Image as ImageIcon,
  ChevronRight,
  Zap,
  Search,
  Layers,
  BarChart3,
} from "lucide-react";

interface EmptyStateViewProps {
  onSendQuery: (q: string) => void;
  ontologyDomain: string;
}

const QUICK_STARTS = [
  {
    label: "Analýza systémové architektury",
    query: "Proveď komplexní analýzu a dekompozici systémové architektury cloudové aplikace z pohledu 8D matice dopadů.",
    icon: <Brain className="w-4 h-4 text-purple-400" />,
    color: "border-purple-500/30 hover:border-purple-500/60 hover:bg-purple-500/5",
    badge: "Kognitivní analýza",
    badgeColor: "bg-purple-900/40 text-purple-300",
  },
  {
    label: "Strategická rozhodovací mapa",
    query: "Vytvoř transdisciplinární strategickou mapu pro optimální rozhodovací procesy v podmínkách vysoké nejistoty.",
    icon: <Activity className="w-4 h-4 text-cyan-400" />,
    color: "border-cyan-500/30 hover:border-cyan-500/60 hover:bg-cyan-500/5",
    badge: "Matice dopadů",
    badgeColor: "bg-cyan-900/40 text-cyan-300",
  },
  {
    label: "Inovační pákový bod",
    query: "Identifikuj klíčový pákový bod pro maximální systémový dopad s minimálním úsilím v oblasti udržitelných technologií.",
    icon: <Zap className="w-4 h-4 text-amber-400" />,
    color: "border-amber-500/30 hover:border-amber-500/60 hover:bg-amber-500/5",
    badge: "Win-Win-Win",
    badgeColor: "bg-amber-900/40 text-amber-300",
  },
  {
    label: "Red Team rizikový audit",
    query: "Spusť Red Team audit a forenzní analýzu rizik pro kybernetický systém AI integrace ve finančním sektoru.",
    icon: <Search className="w-4 h-4 text-red-400" />,
    color: "border-red-500/30 hover:border-red-500/60 hover:bg-red-500/5",
    badge: "Forenzní analýza",
    badgeColor: "bg-red-900/40 text-red-300",
  },
];

const FEATURE_PILLS = [
  { icon: <Mic className="w-3 h-3" />, label: "Hlasové diktování", shortcut: "Ctrl+M" },
  { icon: <ImageIcon className="w-3 h-3" />, label: "Multimodální vstup" },
  { icon: <Search className="w-3 h-3" />, label: "Full-text hledání", shortcut: "Ctrl+K" },
  { icon: <Layers className="w-3 h-3" />, label: "5-fázový pipeline" },
  { icon: <BarChart3 className="w-3 h-3" />, label: "8D Impact Matrix" },
  { icon: <Brain className="w-3 h-3" />, label: "Kognitivní uzly" },
];

const DOMAIN_LABELS: Record<string, string> = {
  SYSTEMS_INTELLIGENCE: "Systémová inteligence",
  CYBERNETICS: "Kybernetika",
  SUSTAINABLE_TECH: "Udržitelné technologie",
  COGNITIVE_SCI: "Kognitivní věda",
};

export const EmptyStateView: React.FC<EmptyStateViewProps> = ({ onSendQuery, ontologyDomain }) => {
  const [visible, setVisible] = useState(false);
  const [activeQuick, setActiveQuick] = useState<number | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 80);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div
      className={`h-full flex flex-col items-center justify-center px-4 py-8 transition-all duration-500 ${
        visible ? "opacity-100 translate-y-0" : "opacity-0 translate-y-4"
      }`}
    >
      {/* Animated logo area */}
      <div className="relative mb-6 flex items-center justify-center">
        {/* Outer ring */}
        <div className="absolute w-28 h-28 rounded-full border border-[#00F0FF]/15 animate-spin" style={{ animationDuration: "18s" }} />
        <div className="absolute w-20 h-20 rounded-full border border-purple-500/15 animate-spin" style={{ animationDuration: "12s", animationDirection: "reverse" }} />
        {/* Orbiting dots */}
        <div className="absolute w-28 h-28 animate-spin" style={{ animationDuration: "6s" }}>
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-1.5 h-1.5 rounded-full bg-[#00F0FF] shadow-[0_0_8px_rgba(0,240,255,0.8)]" />
        </div>
        <div className="absolute w-20 h-20 animate-spin" style={{ animationDuration: "4s", animationDirection: "reverse" }}>
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-purple-400 shadow-[0_0_6px_rgba(168,85,247,0.8)]" />
        </div>
        {/* Core icon */}
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-[#00F0FF]/20 to-blue-600/20 border border-[#00F0FF]/30 flex items-center justify-center shadow-[0_0_30px_rgba(0,240,255,0.2)] z-10 backdrop-blur-sm">
          <Sparkles className="w-7 h-7 text-[#00F0FF]" />
        </div>
      </div>

      {/* Heading */}
      <div className="text-center mb-2">
        <h2 className="text-2xl sm:text-3xl font-bold font-mono gradient-text-omnis tracking-wide">
          O.M.N.I.S.
        </h2>
        <p className="text-xs font-mono text-slate-500 uppercase tracking-[0.25em] mt-1">
          Cognitive Synthesis Engine — Active
        </p>
      </div>

      {/* Domain badge */}
      <div className="mb-6 px-3 py-1.5 rounded-full bg-[#0A0F1D] border border-[#00F0FF]/25 text-[10px] font-mono text-[#00F0FF] uppercase tracking-widest flex items-center gap-1.5">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
        Doména: {DOMAIN_LABELS[ontologyDomain] ?? ontologyDomain}
      </div>

      {/* Quick start cards */}
      <div className="w-full max-w-2xl mb-6">
        <p className="text-[10px] font-mono text-slate-500 uppercase tracking-widest text-center mb-3">
          — Rychlý start —
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {QUICK_STARTS.map((item, i) => (
            <button
              key={i}
              onClick={() => {
                setActiveQuick(i);
                setTimeout(() => onSendQuery(item.query), 150);
              }}
              className={`group relative text-left p-3.5 rounded-xl border bg-[#0A0F1D]/80 transition-all duration-200 animate-fade-in-up interactive-card ${item.color}`}
              style={{ animationDelay: `${i * 60}ms` }}
            >
              <div className="flex items-start gap-2.5">
                <div className="mt-0.5 flex-shrink-0 w-7 h-7 rounded-lg bg-slate-900 border border-slate-700/80 flex items-center justify-center">
                  {item.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <span className="text-xs font-bold font-mono text-slate-100 group-hover:text-white transition-colors">
                      {item.label}
                    </span>
                    <span className={`text-[9px] px-1.5 py-0.5 rounded font-mono uppercase tracking-wide ${item.badgeColor}`}>
                      {item.badge}
                    </span>
                  </div>
                  <p className="text-[10px] text-slate-500 leading-relaxed line-clamp-2 group-hover:text-slate-400 transition-colors">
                    {item.query.substring(0, 90)}…
                  </p>
                </div>
                <ChevronRight className="w-3.5 h-3.5 text-slate-600 group-hover:text-slate-400 flex-shrink-0 mt-1 group-hover:translate-x-0.5 transition-transform" />
              </div>
              {activeQuick === i && (
                <div className="absolute inset-0 rounded-xl border border-[#00F0FF]/50 bg-[#00F0FF]/5 animate-fade-in pointer-events-none" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Feature pills */}
      <div className="flex flex-wrap items-center justify-center gap-2 max-w-xl">
        {FEATURE_PILLS.map((pill, i) => (
          <div
            key={i}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-full bg-slate-900/60 border border-slate-800 text-[10px] font-mono text-slate-400 animate-fade-in"
            style={{ animationDelay: `${300 + i * 50}ms` }}
          >
            {pill.icon}
            <span>{pill.label}</span>
            {pill.shortcut && (
              <span className="px-1 py-px rounded bg-slate-800 text-[9px] text-slate-500 border border-slate-700">
                {pill.shortcut}
              </span>
            )}
          </div>
        ))}
      </div>

      {/* Bottom hint */}
      <p className="mt-5 text-[10px] font-mono text-slate-600 text-center">
        Zadejte dotaz níže • Použijte <kbd className="px-1 py-px rounded bg-slate-800 border border-slate-700 text-slate-500">Enter</kbd> pro odeslání • <kbd className="px-1 py-px rounded bg-slate-800 border border-slate-700 text-slate-500">Shift+Enter</kbd> pro nový řádek
      </p>
    </div>
  );
};
