import React, { useState } from 'react';
import { X, Sparkles, AlertTriangle, Layers, ArrowRight, Check, Activity, Network, ShieldCheck, Zap } from 'lucide-react';

export interface CoSynthesisProposal {
  id: string;
  title: string;
  addition: string;
}

export interface CascadeOrderData {
  order: number;
  domain: string;
  title: string;
  cascade_description: str;
  probability: number;
  impact: number;
  mitigation: str;
  causal_coupling: str;
}

export interface CoSynthesisData {
  original_query: string;
  detected_gaps: string[];
  cascade_preview: {
    risk_index: number;
    risk_level: string;
    cascades_order_1: CascadeOrderData[];
    cascades_order_2: CascadeOrderData[];
    cascades_order_3: CascadeOrderData[];
    do_calculus_matrix: Record<string, number>;
  };
  completion_proposals: CoSynthesisProposal[];
}

interface Props {
  data: CoSynthesisData;
  onClose: () => void;
  onExecute: (query: string) => void;
}

export function InteractiveCoSynthesisModal({ data, onClose, onExecute }: Props) {
  const [selectedProposalId, setSelectedProposalId] = useState<string>(
    data.completion_proposals[0]?.id || ''
  );
  const [activeCascadeTab, setActiveCascadeTab] = useState<1 | 2 | 3>(1);

  const selectedProposal = data.completion_proposals.find(p => p.id === selectedProposalId);
  const activeCascades = 
    activeCascadeTab === 1 ? data.cascade_preview.cascades_order_1 :
    activeCascadeTab === 2 ? data.cascade_preview.cascades_order_2 :
    data.cascade_preview.cascades_order_3;

  const handleConfirmAndRun = () => {
    const finalQuery = selectedProposal ? selectedProposal.addition : data.original_query;
    onExecute(finalQuery);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn font-sans">
      <div className="bg-[#090f1d] border border-cyan-500/40 rounded-2xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-[0_0_40px_rgba(6,182,212,0.2)] overflow-hidden">
        
        {/* Záhlaví modalu */}
        <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-[#050810]/80">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-cyan-950/80 border border-cyan-500/40 text-cyan-400">
              <Network className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h2 className="text-white font-bold text-base tracking-wide flex items-center gap-2">
                <span>PŘED-EXEKUČNÍ KAUZÁLNÍ ROZBOR VSTUPU</span>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-cyan-950 border border-cyan-500/50 text-cyan-400">
                  AKGE-8D
                </span>
              </h2>
              <p className="text-slate-400 text-xs font-mono">
                Multidisiplinární analýza dopadů 2. a 3. řádu + Spoludoplňovač vstupu
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tělo modalu */}
        <div className="p-5 overflow-y-auto space-y-5 text-xs text-slate-300">
          
          {/* Vstupní dotaz operátora */}
          <div className="bg-[#050810] p-3 rounded-xl border border-slate-800">
            <span className="text-[10px] font-mono font-bold text-slate-500 uppercase tracking-widest block mb-1">
              Původní Vstup Operátora:
            </span>
            <p className="text-slate-200 font-medium whitespace-pre-wrap">{data.original_query}</p>
          </div>

          {/* Sémantické mezery */}
          {data.detected_gaps.length > 0 && (
            <div className="bg-amber-950/20 border border-amber-500/30 p-3 rounded-xl space-y-1">
              <span className="font-mono font-bold text-amber-400 flex items-center gap-1.5 text-[11px]">
                <AlertTriangle className="w-4 h-4" /> Detekované sémantické mezery a slepé skvrny:
              </span>
              <ul className="list-disc list-inside text-slate-300 font-mono text-[11px] space-y-0.5 pl-1">
                {data.detected_gaps.map((gap, i) => (
                  <li key={i}>{gap}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Kaskádový rozbor dopadů (T+1, T+2, T+3) */}
          <div className="bg-[#050810] p-4 rounded-xl border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-mono font-bold text-cyan-400 flex items-center gap-1.5 text-xs">
                <Layers className="w-4 h-4" /> Kaskádový Řetězec Dopadů (2. & 3. Řád):
              </span>
              <div className="flex items-center gap-1 bg-slate-900 p-1 rounded-lg border border-slate-800">
                <button
                  onClick={() => setActiveCascadeTab(1)}
                  className={`px-2.5 py-1 rounded text-[10px] font-mono font-bold transition-all ${
                    activeCascadeTab === 1 ? 'bg-cyan-600 text-black shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  T+1 (1. Řád)
                </button>
                <button
                  onClick={() => setActiveCascadeTab(2)}
                  className={`px-2.5 py-1 rounded text-[10px] font-mono font-bold transition-all ${
                    activeCascadeTab === 2 ? 'bg-violet-600 text-white shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  T+2 (2. Řád)
                </button>
                <button
                  onClick={() => setActiveCascadeTab(3)}
                  className={`px-2.5 py-1 rounded text-[10px] font-mono font-bold transition-all ${
                    activeCascadeTab === 3 ? 'bg-amber-600 text-black shadow' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  T+3 (3. Řád - Drift)
                </button>
              </div>
            </div>

            {/* Zobrazení aktivních kaskád */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5">
              {activeCascades.map((cas, i) => (
                <div key={i} className="bg-slate-950/80 border border-slate-800 p-3 rounded-lg space-y-1 font-mono">
                  <div className="flex items-center justify-between">
                    <span className="text-cyan-400 font-bold text-[11px]">{cas.title}</span>
                    <span className="px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800 text-[9px] text-slate-400">
                      {cas.domain}
                    </span>
                  </div>
                  <p className="text-slate-300 text-[11px] leading-relaxed font-sans">{cas.cascade_description}</p>
                  <div className="pt-1 text-[10px] text-slate-500 border-t border-slate-900 flex justify-between">
                    <span>Mitigace: {cas.mitigation}</span>
                    <span className="text-violet-400 font-bold">{cas.causal_coupling}</span>
                  </div>
                </div>
              ))}
            </div>

            {/* Do-Calculus Matrix preview */}
            {data.cascade_preview.do_calculus_matrix && (
              <div className="pt-2 border-t border-slate-900">
                <span className="text-[10px] font-mono text-slate-500 block mb-1">
                  Do-Calculus P(Y | do(X=x)) Intervence:
                </span>
                <div className="flex flex-wrap gap-2 font-mono text-[10px]">
                  {Object.entries(data.cascade_preview.do_calculus_matrix).map(([k, v], idx) => (
                    <span key={idx} className="bg-slate-900/80 px-2 py-0.5 rounded border border-slate-800 text-slate-300">
                      <strong className="text-cyan-400">{k}</strong> = {(v * 100).toFixed(0)}%
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Návrhy spoludoplnění a rozšíření */}
          <div className="space-y-2">
            <span className="font-mono font-bold text-white flex items-center gap-1.5 text-xs">
              <Sparkles className="w-4 h-4 text-cyan-400" /> Zvolte variantu spoludoplnění vstupu pro nejdokonalejší výstup:
            </span>

            <div className="space-y-2">
              {data.completion_proposals.map((prop) => {
                const isSelected = selectedProposalId === prop.id;
                return (
                  <div
                    key={prop.id}
                    onClick={() => setSelectedProposalId(prop.id)}
                    className={`p-3 rounded-xl border cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-cyan-950/30 border-cyan-500 shadow-[0_0_15px_rgba(6,182,212,0.15)]'
                        : 'bg-[#050810] border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-slate-200 flex items-center gap-2">
                        <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${
                          isSelected ? 'border-cyan-400 bg-cyan-400 text-black' : 'border-slate-600'
                        }`}>
                          {isSelected && <Check className="w-2.5 h-2.5" />}
                        </span>
                        {prop.title}
                      </span>
                    </div>
                    <p className="text-slate-400 font-mono text-[11px] whitespace-pre-wrap pl-5">
                      {prop.addition}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Zápatí a tlačítka akce */}
        <div className="p-4 border-t border-slate-800 bg-[#050810]/90 flex items-center justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-slate-800 hover:bg-slate-900 text-slate-400 font-mono font-bold transition-colors"
          >
            ZRUŠIT
          </button>

          <button
            onClick={handleConfirmAndRun}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-600 hover:from-cyan-400 hover:to-violet-500 text-black font-bold flex items-center gap-2 shadow-[0_0_20px_rgba(6,182,212,0.3)] transition-all font-mono"
          >
            <span>POTVRDIT & SPUSTIT KO-SYNTÉZU</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
