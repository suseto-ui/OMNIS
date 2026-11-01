import React, { useState, useMemo } from "react";
import { 
  Users, Swords, Trophy, ShieldCheck, Zap, Brain, Scale, Activity, CheckCircle2, 
  AlertTriangle, RotateCcw, ChevronRight, Sparkles, MessageSquare
} from "lucide-react";
import { MessageItem } from "./MessageBubble";
import { 
  executeMultiAgentDeliberationBattle, 
  MultiAgentBattleResult, 
  OMNIS_DELIBERATION_AGENTS 
} from "../omnisEngine";

interface MultiAgentArenaDashboardProps {
  messages: MessageItem[];
  onGoToChat: () => void;
}

export const MultiAgentArenaDashboard: React.FC<MultiAgentArenaDashboardProps> = ({
  messages,
  onGoToChat,
}) => {
  const assistantMessages = messages.filter((m) => m.role === "assistant");
  const lastAssistantMsg = assistantMessages[assistantMessages.length - 1];

  const [arenaQuery, setArenaQuery] = useState<string>(
    lastAssistantMsg?.content ? lastAssistantMsg.content.slice(0, 140) : "Optimalizace mikroservisní architektury s vysokou bezpečností a nízkými náklady"
  );

  const [activeRound, setActiveRound] = useState<1 | 2 | 3>(1);

  const battleResult: MultiAgentBattleResult = useMemo(() => {
    return executeMultiAgentDeliberationBattle(arenaQuery, lastAssistantMsg?.impact_matrix);
  }, [arenaQuery, lastAssistantMsg]);

  const winningAgent = useMemo(() => {
    return OMNIS_DELIBERATION_AGENTS.find(a => a.id === battleResult.winningAgentId);
  }, [battleResult.winningAgentId]);

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-7xl mx-auto font-sans text-slate-100">
      {/* Top Banner */}
      <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 shadow-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Swords className="w-5 h-5 text-[#00F0FF]" />
            <h1 className="text-lg sm:text-xl font-bold font-mono text-slate-100 tracking-wider uppercase">
              MULTI-AGENTNÍ DELIBERAČNÍ ARÉNA (ROZSTŘEL)
            </h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30">
              4 AGENTI
            </span>
          </div>
          <p className="text-xs text-slate-400 max-w-2xl font-mono">
            3-fázová oponentura a syntéza 4 autonomních agentů (Alpha, Beta, Gamma, Delta) s měřením systémového tenzního indexu.
          </p>
        </div>

        {/* Round Navigation Tabs */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setActiveRound(1)}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeRound === 1
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Fáze 1: Téze
          </button>
          <button
            onClick={() => setActiveRound(2)}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeRound === 2
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Fáze 2: Rozstřel
          </button>
          <button
            onClick={() => setActiveRound(3)}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeRound === 3
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Fáze 3: Verdikt
          </button>
        </div>
      </div>

      {/* Query Bar */}
      <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-2 shadow-xl">
        <label className="text-[10px] font-mono text-slate-400 uppercase font-bold block">
          Předmět Deliberačního Souboje:
        </label>
        <div className="flex gap-2">
          <input
            type="text"
            value={arenaQuery}
            onChange={(e) => setArenaQuery(e.target.value)}
            placeholder="Zadejte zadání pro multi-agentní rozstřel..."
            className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-slate-100 focus:outline-none focus:border-[#00F0FF]"
          />
          <button
            onClick={() => setArenaQuery("Optimalizace mikroservisní architektury s vysokou bezpečností a nízkými náklady")}
            className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition-all flex items-center gap-1"
            title="Resetovat dotaz"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* ROUND 1: AXIOMATIC PROPOSALS GRID */}
      {activeRound === 1 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {battleResult.proposals.map((prop) => {
            const agent = battleResult.agents.find(a => a.id === prop.agentId)!;
            const isWinner = prop.agentId === battleResult.winningAgentId;

            return (
              <div
                key={prop.agentId}
                className={`p-4 rounded-2xl border space-y-3 font-mono text-xs shadow-xl transition-all ${
                  isWinner
                    ? "bg-[#080D1D] border-[#00F0FF]/60 shadow-[0_0_20px_rgba(0,240,255,0.15)]"
                    : "bg-[#080D1D] border-slate-800"
                }`}
              >
                {/* Agent Header */}
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-full flex-shrink-0"
                      style={{ backgroundColor: agent.avatarColor, boxShadow: `0 0 10px ${agent.avatarColor}` }}
                    />
                    <span className="font-bold text-slate-100">{prop.agentName}</span>
                  </div>
                  {isWinner && (
                    <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-[#00F0FF]/20 text-[#00F0FF] border border-[#00F0FF]/40 flex items-center gap-1">
                      <Trophy className="w-3 h-3 text-[#00F0FF]" /> VÍTĚZNÝ PROFIL
                    </span>
                  )}
                </div>

                {/* Ethos */}
                <p className="text-[11px] text-slate-400 italic leading-relaxed">
                  "{agent.ethos}"
                </p>

                {/* Thesis */}
                <div className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 space-y-1">
                  <span className="text-[10px] text-[#00F0FF] uppercase font-bold block">Axiomatická Téze:</span>
                  <p className="text-[11px] text-slate-200">{prop.thesis}</p>
                </div>

                {/* Key Arguments */}
                <div className="space-y-1">
                  <span className="text-[10px] text-slate-400 uppercase font-bold block">Hlavní Pilíře:</span>
                  <ul className="space-y-1">
                    {prop.keyArguments.map((arg, i) => (
                      <li key={i} className="flex items-start gap-1.5 text-[10px] text-slate-300">
                        <CheckCircle2 className="w-3 h-3 text-emerald-400 flex-shrink-0 mt-0.5" />
                        <span>{arg}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Vulnerabilities */}
                <div className="space-y-1 pt-1 border-t border-slate-800/80">
                  <span className="text-[10px] text-amber-400 uppercase font-bold block">Identifikované Zranitelnosti:</span>
                  <ul className="space-y-1">
                    {prop.vulnerabilities.map((vuln, i) => (
                      <li key={i} className="flex items-start gap-1.5 text-[10px] text-amber-300/80">
                        <AlertTriangle className="w-3 h-3 text-amber-400 flex-shrink-0 mt-0.5" />
                        <span>{vuln}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ROUND 2: CROSSFIRE DUEL & TENSION GAUGE */}
      {activeRound === 2 && (
        <div className="space-y-4">
          {/* Tension Gauge Bar */}
          <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-2 shadow-xl">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-slate-400 uppercase font-bold flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-amber-400" /> Tenzní Měřič Arény (Argumentative Tension):
              </span>
              <span className="text-amber-400 font-bold">{battleResult.tensionIndex}% TENZE</span>
            </div>
            <div className="w-full h-2.5 bg-slate-950 rounded-full overflow-hidden">
              <div
                style={{ width: `${battleResult.tensionIndex}%` }}
                className="h-full bg-gradient-to-r from-emerald-500 via-amber-400 to-red-500 transition-all duration-500 shadow-[0_0_12px_rgba(245,158,11,0.5)]"
              />
            </div>
          </div>

          {/* Crossfire List */}
          <div className="space-y-3">
            {battleResult.crossfire.map((cf, idx) => {
              const attacker = battleResult.agents.find(a => a.id === cf.attackerId)!;
              const defender = battleResult.agents.find(a => a.id === cf.defenderId)!;

              return (
                <div
                  key={idx}
                  className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-3 font-mono text-xs shadow-xl"
                >
                  <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-amber-400">{attacker.name}</span>
                      <Swords className="w-4 h-4 text-red-400" />
                      <span className="font-bold text-cyan-400">{defender.name}</span>
                    </div>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-red-500/15 text-red-400 border border-red-500/30 font-bold">
                      Střet #{idx + 1} (Tenzní váha {Math.round(cf.tensionScore * 100)}%)
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {/* Critique */}
                    <div className="p-3 rounded-xl bg-red-950/30 border border-red-500/30 space-y-1">
                      <span className="text-[10px] text-red-400 font-bold uppercase block">
                        Oponentní Kritika ({attacker.name}):
                      </span>
                      <p className="text-[11px] text-red-200">{cf.critique}</p>
                    </div>

                    {/* Counter Argument */}
                    <div className="p-3 rounded-xl bg-cyan-950/30 border border-cyan-500/30 space-y-1">
                      <span className="text-[10px] text-cyan-400 font-bold uppercase block">
                        Proti-Argument Obhajoby ({defender.name}):
                      </span>
                      <p className="text-[11px] text-cyan-200">{cf.counterArgument}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ROUND 3: SYNTHESIS VERDICT & WINNING STRATEGY */}
      {activeRound === 3 && (
        <div className="space-y-4">
          <div className="p-5 rounded-2xl bg-[#080D1D] border border-[#00F0FF]/50 space-y-4 shadow-[0_0_25px_rgba(0,240,255,0.15)]">
            {/* Winner Trophy Header */}
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2.5">
                <Trophy className="w-6 h-6 text-[#00F0FF] animate-bounce" />
                <div>
                  <h3 className="text-sm font-bold font-mono text-slate-100 uppercase tracking-wider">
                    Finální Konsensuální Verdikt a Vítězná Linie
                  </h3>
                  <span className="text-xs text-[#00F0FF] font-mono font-bold">
                    Vítěz: {winningAgent?.name} ({winningAgent?.title})
                  </span>
                </div>
              </div>
            </div>

            {/* Synthesis Verdict Box */}
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-200 leading-relaxed space-y-1">
              <span className="text-[10px] text-[#00F0FF] font-bold uppercase block">Syntéza Rozhodčího:</span>
              <p>{battleResult.synthesisVerdict}</p>
            </div>

            {/* Invariant Recommendations */}
            <div className="space-y-2">
              <span className="text-[10px] font-mono text-slate-400 uppercase font-bold block">
                Nekompromisní Invariantní Doporučení:
              </span>
              <div className="space-y-2">
                {battleResult.invariantRecommendations.map((rec, i) => (
                  <div
                    key={i}
                    className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs font-mono text-slate-200 flex items-start gap-2.5"
                  >
                    <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                    <span>{rec}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
