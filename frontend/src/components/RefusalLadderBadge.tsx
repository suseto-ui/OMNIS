import React, { useState } from "react";
import { RefusalLadderEvaluation } from "../omnisEngine";
import { CheckCircle2, XCircle, AlertTriangle, ShieldCheck, Network, BookOpen, ChevronDown, ChevronUp, ExternalLink, Activity } from "lucide-react";

interface RefusalLadderBadgeProps {
  evaluation?: RefusalLadderEvaluation;
  compact?: boolean;
}

export const RefusalLadderBadge: React.FC<RefusalLadderBadgeProps> = ({ evaluation, compact = false }) => {
  const [isOpen, setIsOpen] = useState(false);

  if (!evaluation) return null;

  const { isGrounded, groundingScore, gates, matchedNodes, graphPath, citations, isRefusal, refusalReason, recommendedAction, shannonEntropy, entropyGrade } = evaluation;

  const scoreColor = isRefusal
    ? "text-red-400 border-red-500/40 bg-red-950/40"
    : groundingScore >= 80
    ? "text-[#00F0FF] border-[#00F0FF]/40 bg-[#00F0FF]/10"
    : "text-amber-400 border-amber-500/40 bg-amber-950/40";

  return (
    <div className="mt-2 text-xs font-mono select-none">
      {/* Micro Status Bar */}
      <div 
        onClick={() => setIsOpen(!isOpen)}
        className={`inline-flex items-center gap-2 px-2.5 py-1 rounded-xl border transition-all cursor-pointer hover:opacity-90 ${scoreColor} shadow-sm`}
        title="Klikněte pro zobrazení 6-úrovňového auditu ukotvení (AKGE-8D)"
      >
        <div className="flex items-center gap-1.5 font-bold">
          {isRefusal ? (
            <AlertTriangle className="w-3.5 h-3.5 text-red-400 animate-pulse" />
          ) : (
            <ShieldCheck className="w-3.5 h-3.5 text-[#00F0FF]" />
          )}
          <span>
            {isRefusal ? "UNGROUNDED (REFUSAL)" : `AKGE-8D: ${groundingScore}% UKOTVENO`}
          </span>
        </div>

        {/* 6 Gates Mini Indicator */}
        <div className="flex items-center gap-1 pl-1 border-l border-slate-700/60">
          {gates.map((g) => (
            <span
              key={g.gateId}
              title={`${g.gateId}: ${g.name} (${g.passed ? "PASS" : "FAIL"}) - ${g.explanation}`}
              className={`w-4 h-4 rounded text-[9px] font-bold flex items-center justify-center ${
                g.passed
                  ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                  : "bg-red-500/20 text-red-400 border border-red-500/30"
              }`}
            >
              {g.gateId.replace("G", "")}
            </span>
          ))}
        </div>

        {/* Entropy pill */}
        <span className="text-[10px] text-slate-400 font-normal hidden sm:inline">
          H={shannonEntropy}b
        </span>

        {isOpen ? <ChevronUp className="w-3 h-3 text-slate-400" /> : <ChevronDown className="w-3 h-3 text-slate-400" />}
      </div>

      {/* Expanded Diagnostic Inspector */}
      {isOpen && (
        <div className="mt-2.5 p-3.5 rounded-xl bg-[#080D1D] border border-slate-700/80 text-slate-200 shadow-xl animate-in fade-in duration-200 space-y-3">
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <div className="flex items-center gap-2">
              <Network className="w-4 h-4 text-[#00F0FF]" />
              <span className="font-bold text-slate-100 text-xs tracking-wider uppercase">
                6-Úrovňový Refusal Ladder & CSR Graph Audit
              </span>
            </div>
            <span className="text-[10px] text-slate-400">
              Informační entropie: <strong className="text-slate-200">{shannonEntropy} bitů</strong> ({entropyGrade})
            </span>
          </div>

          {/* Refusal Warning if triggered */}
          {isRefusal && refusalReason && (
            <div className="p-2.5 rounded-lg bg-red-950/50 border border-red-500/40 text-red-300 text-xs space-y-1">
              <div className="font-bold flex items-center gap-1.5">
                <XCircle className="w-4 h-4 text-red-400" />
                <span>Kognitivní odmítnutí dotazu:</span>
              </div>
              <p className="text-[11px] leading-relaxed pl-5">{refusalReason}</p>
              {recommendedAction && (
                <p className="text-[10px] text-amber-300/90 pl-5 font-sans">
                  💡 <strong>Doporučení:</strong> {recommendedAction}
                </p>
              )}
            </div>
          )}

          {/* 6 Gates Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
            {gates.map((gate) => (
              <div
                key={gate.gateId}
                className={`p-2 rounded-lg border text-[11px] ${
                  gate.passed
                    ? "bg-slate-900/80 border-slate-800 text-slate-300"
                    : "bg-red-950/30 border-red-500/40 text-red-300"
                }`}
              >
                <div className="flex items-center justify-between font-bold mb-1">
                  <div className="flex items-center gap-1.5">
                    {gate.passed ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                    ) : (
                      <XCircle className="w-3.5 h-3.5 text-red-400 flex-shrink-0" />
                    )}
                    <span>{gate.gateId}: {gate.name}</span>
                  </div>
                  <span className={`text-[10px] font-mono px-1.5 py-0.2 rounded ${
                    gate.passed ? "bg-emerald-500/20 text-emerald-300" : "bg-red-500/20 text-red-300"
                  }`}>
                    {Math.round(gate.score * 100)}%
                  </span>
                </div>
                <p className="text-[10px] text-slate-400 leading-snug">
                  {gate.explanation}
                </p>
              </div>
            ))}
          </div>

          {/* CSR Graph Traversal Path */}
          {graphPath && graphPath.length > 0 && (
            <div className="p-2 rounded-lg bg-slate-900/90 border border-slate-800 text-[11px] space-y-1">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">
                CSR Propojovací Cesta v Kognitivním Grafu:
              </span>
              <div className="flex items-center gap-1.5 flex-wrap text-slate-200">
                {graphPath.map((nodeId, idx) => (
                  <React.Fragment key={nodeId}>
                    <span className="px-2 py-0.5 rounded bg-[#00F0FF]/15 border border-[#00F0FF]/30 text-[#00F0FF] text-[10px] font-bold">
                      {nodeId}
                    </span>
                    {idx < graphPath.length - 1 && (
                      <span className="text-slate-500">→</span>
                    )}
                  </React.Fragment>
                ))}
              </div>
            </div>
          )}

          {/* Authoritative Citations */}
          {citations && citations.length > 0 && (
            <div className="p-2 rounded-lg bg-slate-900/90 border border-slate-800 text-[11px] space-y-1">
              <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider flex items-center gap-1.5">
                <BookOpen className="w-3 h-3 text-purple-400" /> Ověřené Standardy a Citace:
              </span>
              <ul className="list-disc list-inside space-y-0.5 text-slate-300 text-[10px]">
                {citations.map((cit, i) => (
                  <li key={i} className="truncate">{cit}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
