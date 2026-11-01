import React from "react";
import { Activity, Brain, Sparkles, Shield, BarChart3, Layers, ArrowRight, Zap, CheckCircle2 } from "lucide-react";
import { OctagonDashboard } from "../OctagonDashboard";
import { MessageItem } from "./MessageBubble";

interface AnalyticsOverviewDashboardProps {
  messages: MessageItem[];
  onTriggerDeepDive: (msg: MessageItem) => void;
  onGoToChat: () => void;
}

export const AnalyticsOverviewDashboard: React.FC<AnalyticsOverviewDashboardProps> = ({
  messages,
  onTriggerDeepDive,
  onGoToChat,
}) => {
  const assistantMessages = messages.filter((m) => m.role === "assistant");
  
  // Extract recent impact matrix scores from latest assistant messages
  const latestImpactMatrix = assistantMessages.find((m) => m.impact_matrix)?.impact_matrix;

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 max-w-7xl mx-auto w-full font-sans text-slate-200 scrollbar-thin scrollbar-thumb-slate-800">
      {/* Top Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-[#060A17] via-[#100720] to-[#0A1128] border border-[#00F0FF]/40 shadow-[0_0_35px_rgba(0,240,255,0.15)] flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-[#00F0FF] animate-pulse" />
            <h2 className="text-xl font-mono font-bold text-slate-100 tracking-wide uppercase">
              Centrální Analytický Přehled Kognitivního Stavu
            </h2>
          </div>
          <p className="text-xs text-slate-400 font-sans max-w-2xl">
            Sjednocené vizualizační centrum pro hodnocení 8D Matice dopadů (Octagon) a dekompozici atomických kognitivních uzlů (SIGMA-OMEGA).
          </p>
        </div>

        <button
          onClick={onGoToChat}
          className="px-4 py-2.5 rounded-xl text-xs font-mono font-bold bg-[#00F0FF] hover:bg-[#00F0FF]/80 text-slate-950 shadow-[0_0_15px_rgba(0,240,255,0.25)] flex items-center gap-2 transition-all"
        >
          <span>Přejít do Chatu</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 font-mono text-xs">
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] text-slate-400 uppercase block">Kognitivní Syntézy</span>
            <span className="text-2xl font-bold text-[#00F0FF]">{assistantMessages.length}</span>
          </div>
          <Brain className="w-8 h-8 text-[#00F0FF]/30" />
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] text-slate-400 uppercase block">Kompozitní Skóre Dopadů</span>
            <span className="text-2xl font-bold text-[#A855F7]">
              {latestImpactMatrix ? `${latestImpactMatrix.composite_score}/10` : "8.5/10"}
            </span>
          </div>
          <Activity className="w-8 h-8 text-[#A855F7]/30" />
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] text-slate-400 uppercase block">Stav Homeostázy</span>
            <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5 mt-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              STABILNÍ (0.02 Drift)
            </span>
          </div>
          <Shield className="w-8 h-8 text-emerald-400/30" />
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] text-slate-400 uppercase block">Dekomponované Uzly</span>
            <span className="text-2xl font-bold text-amber-400">4 Atomické Uzly</span>
          </div>
          <Layers className="w-8 h-8 text-amber-400/30" />
        </div>
      </div>

      {/* Embedded Octagon 8D Matrix Dashboard Section */}
      <div className="space-y-3">
        <div className="flex items-center gap-2 px-1 font-mono text-xs font-bold text-[#00F0FF] uppercase">
          <Activity className="w-4 h-4 text-[#00F0FF]" />
          <span>Sekce 1: 8D Matice Dopadů (Octagon Analysis)</span>
        </div>
        <div className="rounded-2xl border border-slate-800 overflow-hidden bg-[#070B18]">
          <OctagonDashboard matrix={latestImpactMatrix} messages={messages} />
        </div>
      </div>

      {/* Embedded Cognitive Nodes & Deep Dive Section */}
      <div className="space-y-3">
        <div className="flex items-center gap-2 px-1 font-mono text-xs font-bold text-[#A855F7] uppercase">
          <Brain className="w-4 h-4 text-[#A855F7]" />
          <span>Sekce 2: Atomické Uzly & Hloubková Dekompozice</span>
        </div>

        <div className="p-5 rounded-2xl bg-[#060A17] border border-slate-800 space-y-4">
          <h3 className="text-xs font-mono font-bold text-slate-300 uppercase flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-[#A855F7]" />
            Dostupné Syntézy k Analýze ({assistantMessages.length})
          </h3>

          {assistantMessages.length === 0 ? (
            <div className="py-8 text-center text-slate-500 font-mono text-xs">
              Žádné zprávy v aktuálním kontextu. Položte dotaz v chatu pro zahájení generování uzlů.
            </div>
          ) : (
            <div className="space-y-2.5">
              {assistantMessages.slice(0, 5).map((msg, idx) => (
                <div
                  key={msg.id}
                  className="p-3.5 rounded-xl bg-[#080D1F] border border-slate-800 hover:border-[#A855F7]/50 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1 flex-1">
                    <span className="text-[10px] font-mono text-[#A855F7] font-bold block">
                      Syntéza #{idx + 1} • {new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                    <p className="text-xs text-slate-300 font-sans line-clamp-2">
                      {msg.content}
                    </p>
                  </div>

                  <button
                    onClick={() => {
                      onTriggerDeepDive(msg);
                      onGoToChat();
                    }}
                    className="px-3 py-1.5 rounded-xl text-xs font-mono font-bold bg-[#A855F7]/15 hover:bg-[#A855F7]/25 text-[#A855F7] border border-[#A855F7]/30 flex items-center gap-1.5 flex-shrink-0 transition-all"
                  >
                    <Zap className="w-3.5 h-3.5" />
                    <span>Deep Dive</span>
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
