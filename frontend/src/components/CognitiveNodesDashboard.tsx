import React from "react";
import { Brain, Sparkles, Zap, Activity, ArrowRight, CornerDownRight, CheckCircle2 } from "lucide-react";
import { MessageItem } from "./MessageBubble";

interface CognitiveNodesDashboardProps {
  messages: MessageItem[];
  onTriggerDeepDive: (msg: MessageItem) => void;
  onGoToChat: () => void;
}

export const CognitiveNodesDashboard: React.FC<CognitiveNodesDashboardProps> = ({
  messages,
  onTriggerDeepDive,
  onGoToChat,
}) => {
  const assistantMessages = messages.filter((m) => m.role === "assistant");
  const deepDiveMessages = assistantMessages.filter((m) =>
    m.cognitive_process || m.impact_matrix || m.consequence_forensics
  );

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 max-w-7xl mx-auto w-full font-sans text-slate-200 scrollbar-thin scrollbar-thumb-slate-800">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-[#070B18] via-[#100720] to-[#070B18] border border-[#A855F7]/40 shadow-[0_0_30px_rgba(168,85,247,0.15)] flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Brain className="w-6 h-6 text-[#A855F7] animate-pulse" />
            <h2 className="text-xl font-mono font-bold text-slate-100 tracking-wide uppercase">
              Kognitivní Uzly & Dekompozice Procesa
            </h2>
          </div>
          <p className="text-xs text-slate-400 font-sans max-w-2xl">
            Prohlížeč atomických sémantických uzlů, homeostatických kotvení a stochastického větvění systému SIGMA-OMEGA.
          </p>
        </div>

        <button
          onClick={onGoToChat}
          className="px-4 py-2.5 rounded-xl text-xs font-mono font-bold bg-[#A855F7] hover:bg-[#A855F7]/80 text-white shadow-[0_0_15px_rgba(168,85,247,0.3)] flex items-center gap-2 transition-all"
        >
          <span>Přejít do Chatu</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Cognitive Layer Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 font-mono text-xs">
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
          <span className="text-[10px] text-cyan-400 font-bold uppercase block">Uzel 1: Sémantické Jádro</span>
          <p className="text-xs font-sans text-slate-300">
            Identifikace fundamentálních konceptů a systémových axiomů v dotazu.
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
          <span className="text-[10px] text-purple-400 font-bold uppercase block">Uzel 2: Entropická Redukce</span>
          <p className="text-xs font-sans text-slate-300">
            Eliminace šumu a stochastického váhání v inferenčním řetězci modelů.
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
          <span className="text-[10px] text-amber-400 font-bold uppercase block">Uzel 3: Homeostatické Kotvení</span>
          <p className="text-xs font-sans text-slate-300">
            Garance stability odpovědi vůči nečekaným adversariálním šokům.
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1.5">
          <span className="text-[10px] text-emerald-400 font-bold uppercase block">Uzel 4: Stochastické Větvění</span>
          <p className="text-xs font-sans text-slate-300">
            Generování alternativních návratových bodů a scénářů T+1 driftu.
          </p>
        </div>
      </div>

      {/* Available Assistant Messages for Deep Dive */}
      <div className="p-5 rounded-2xl bg-[#060A17] border border-slate-800 space-y-4">
        <h3 className="text-sm font-mono font-bold text-[#A855F7] uppercase flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-[#A855F7]" />
          Analýza Odpovědí v Aktuálním Vlákně ({assistantMessages.length})
        </h3>

        {assistantMessages.length === 0 ? (
          <div className="py-10 text-center text-slate-500 font-mono text-xs">
            V aktuálním konverzačním vlákně dosud nejsou žádné odpovídající syntézy O.M.N.I.S. Položte dotaz v chatu pro zahájení dekompozice uzlů.
          </div>
        ) : (
          <div className="space-y-3">
            {assistantMessages.map((msg, idx) => (
              <div
                key={msg.id}
                className="p-4 rounded-xl bg-[#080D1F] border border-slate-800 hover:border-[#A855F7]/50 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1 flex-1">
                  <div className="flex items-center gap-2 text-[10px] font-mono">
                    <span className="text-[#A855F7] font-bold"># Uzel Syntézy #{idx + 1}</span>
                    <span className="text-slate-500">
                      {new Date(msg.created_at).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                  <p className="text-xs text-slate-300 font-sans line-clamp-2 leading-relaxed">
                    {msg.content}
                  </p>
                </div>

                <button
                  onClick={() => {
                    onTriggerDeepDive(msg);
                    onGoToChat();
                  }}
                  className="px-4 py-2 rounded-xl text-xs font-mono font-bold bg-[#A855F7]/15 hover:bg-[#A855F7]/25 text-[#A855F7] border border-[#A855F7]/40 shadow-[0_0_10px_rgba(168,85,247,0.15)] flex items-center gap-2 flex-shrink-0 transition-all"
                >
                  <Activity className="w-3.5 h-3.5" />
                  <span>Provést Deep Dive Analýzu</span>
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
