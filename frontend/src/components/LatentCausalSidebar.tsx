import React, { useState } from 'react';
import { Activity, Network, ShieldAlert, ShieldCheck, ChevronRight, ChevronLeft, Zap, Eye, Lock, Layers, RefreshCw, Layers3, AlertTriangle } from 'lucide-react';

export interface LatentCausalState {
  detected_domain_name: string;
  betti_number_beta1: number;
  percolation_index_pc: number;
  is_percolation_critical: boolean;
  hidden_causal_links: Array<{
    intervention: string;
    predicted_cascade: string;
    confidence: number;
    causal_bridge: string;
  }>;
  butterfly_effect_chains: Array<{
    origin: string;
    ripple_t2: string;
    ripple_t3: string;
  }>;
  pon_iceberg: {
    trigger_vector: string;
    exclusion_proof: string;
    minimization_check: string;
    commitment_hash: string;
    iceberg_tip: string;
    deep_evidence_matrix: Record<string, { weight: number; status: string; note: string }>;
  };
}

interface Props {
  data: LatentCausalState | null;
  isLoading?: boolean;
  isOpen: boolean;
  onToggle: () => void;
  onRefresh?: () => void;
}

export function LatentCausalSidebar({ data, isLoading, isOpen, onToggle, onRefresh }: Props) {
  const [showDeepIceberg, setShowDeepIceberg] = useState(false);

  if (!isOpen) {
    return (
      <button
        onClick={onToggle}
        title="Otevřít Živý panel Latent Causal Discovery (AKGE-8D)"
        className="fixed right-0 top-24 z-40 bg-[#090f1d] border-l border-t border-b border-cyan-500/40 p-2.5 rounded-l-xl text-cyan-400 hover:text-white hover:bg-cyan-950/60 shadow-[0_0_20px_rgba(6,182,212,0.2)] transition-all cursor-pointer font-mono text-xs flex items-center gap-1.5"
      >
        <Network className="w-4 h-4 animate-pulse text-cyan-400" />
        <span className="hidden md:inline font-bold">LATENT CAUSAL 8D</span>
        <ChevronLeft className="w-4 h-4" />
      </button>
    );
  }

  const pc = data?.percolation_index_pc ?? 0.05;
  const isCritical = data?.is_percolation_critical ?? (pc >= 0.38);
  const beta1 = data?.betti_number_beta1 ?? 1;

  return (
    <aside className="fixed right-0 top-16 bottom-0 z-40 w-80 sm:w-96 bg-[#080d1a]/95 backdrop-blur-xl border-l border-cyan-500/30 shadow-2xl flex flex-col font-sans transition-all animate-slideInRight">
      
      {/* Záhlaví panelu */}
      <div className="p-3.5 border-b border-slate-800/80 flex items-center justify-between bg-[#050810]/90">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-cyan-950 border border-cyan-500/50 text-cyan-400">
            <Network className="w-4 h-4 animate-pulse" />
          </div>
          <div>
            <h3 className="text-white font-bold text-xs tracking-wider flex items-center gap-1.5">
              <span>LATENT CAUSAL DISCOVERY</span>
              <span className="px-1.5 py-0.5 rounded bg-cyan-950 text-[9px] font-mono text-cyan-400 border border-cyan-500/40 font-bold">
                AKGE-8D
              </span>
            </h3>
            <p className="text-slate-400 text-[10px] font-mono">Topologická kognice & Do-Calculus</p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          {onRefresh && (
            <button
              onClick={onRefresh}
              className="p-1.5 rounded hover:bg-slate-800 text-slate-400 hover:text-cyan-400 transition-colors"
              title="Obnovit analýzu"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          )}
          <button
            onClick={onToggle}
            className="p-1.5 rounded hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Obsah panelu */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 font-mono text-xs">
        
        {/* Detekovaná Oborová Fokusace */}
        <div className="bg-[#050810] p-3 rounded-xl border border-slate-800">
          <span className="text-[10px] text-slate-500 uppercase tracking-widest block mb-1">
            Autonomní Oborový Profil:
          </span>
          <p className="text-cyan-400 font-bold text-xs">
            {data?.detected_domain_name || 'Všeobecné Systémové Inženýrství'}
          </p>
        </div>

        {/* Metriky Bettiho čísel & Perkolace */}
        <div className="grid grid-cols-2 gap-2.5">
          {/* Betti-1 */}
          <div className="bg-[#050810] p-3 rounded-xl border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 text-[10px] mb-1">
              <span>Betti Číslo (β1)</span>
              <Layers3 className="w-3.5 h-3.5 text-violet-400" />
            </div>
            <span className="text-lg font-black text-violet-400">{beta1}</span>
            <p className="text-[9px] text-slate-500 mt-0.5 leading-tight">
              {beta1 > 0 ? `${beta1} cyklické kognitivní trhliny` : 'Síť bez trhlin'}
            </p>
          </div>

          {/* Perkolace pc */}
          <div className={`p-3 rounded-xl border ${
            isCritical ? 'bg-rose-950/30 border-rose-500/50' : 'bg-[#050810] border-slate-800'
          }`}>
            <div className="flex items-center justify-between text-[10px] mb-1">
              <span className={isCritical ? 'text-rose-400 font-bold' : 'text-slate-400'}>
                Perkolace (p_c)
              </span>
              {isCritical ? (
                <ShieldAlert className="w-3.5 h-3.5 text-rose-400 animate-bounce" />
              ) : (
                <Activity className="w-3.5 h-3.5 text-emerald-400" />
              )}
            </div>
            <span className={`text-lg font-black ${isCritical ? 'text-rose-400' : 'text-emerald-400'}`}>
              {pc.toFixed(3)}
            </span>
            <p className="text-[9px] text-slate-500 mt-0.5 leading-tight">
              {isCritical ? 'KRITICKÝ PRÁH >= 0.38!' : 'Práh stabilní (< 0.38)'}
            </p>
          </div>
        </div>

        {/* Motýlí efekt (Butterfly Effect Ripple) */}
        {data?.butterfly_effect_chains && data.butterfly_effect_chains.length > 0 && (
          <div className="bg-[#050810] p-3 rounded-xl border border-slate-800 space-y-2">
            <span className="text-cyan-400 font-bold text-[11px] flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5 text-cyan-400" /> Motýlí Efekt & Kaskáda Zranitelností:
            </span>
            {data.butterfly_effect_chains.map((chain, idx) => (
              <div key={idx} className="bg-slate-950 p-2 rounded border border-slate-900 text-[10px] space-y-1">
                <p className="text-slate-300"><strong className="text-cyan-400">Příčina:</strong> {chain.origin}</p>
                <p className="text-slate-400"><strong className="text-violet-400">T+2 Sekundární:</strong> {chain.ripple_t2}</p>
                <p className="text-slate-400"><strong className="text-amber-400">T+3 Terciární Drift:</strong> {chain.ripple_t3}</p>
              </div>
            ))}
          </div>
        )}

        {/* Skryté Do-Calculus Simulace */}
        {data?.hidden_causal_links && data.hidden_causal_links.length > 0 && (
          <div className="bg-[#050810] p-3 rounded-xl border border-slate-800 space-y-2">
            <span className="text-violet-400 font-bold text-[11px] flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-violet-400" /> Kontrafaktuální Do-Calculus P(Y|do(X)):
            </span>
            {data.hidden_causal_links.map((link, idx) => (
              <div key={idx} className="bg-slate-950 p-2 rounded border border-slate-900 text-[10px] space-y-1">
                <p className="text-slate-200 font-bold">{link.intervention}</p>
                <p className="text-slate-400 font-sans">{link.predicted_cascade}</p>
                <div className="flex justify-between text-[9px] text-slate-500 pt-0.5">
                  <span>Můstky: {link.causal_bridge}</span>
                  <span className="text-cyan-400">Jistota: {(link.confidence * 100).toFixed(0)}%</span>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Progressive Disclosure Proof of Necessity (PoN) Ledovec */}
        {data?.pon_iceberg && (
          <div className="bg-cyan-950/20 border border-cyan-500/40 p-3 rounded-xl space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-cyan-400 text-[11px] flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" /> Proof of Necessity (PoN):
              </span>
              <button
                onClick={() => setShowDeepIceberg(!showDeepIceberg)}
                className="text-[10px] text-cyan-400 hover:text-white underline font-bold"
              >
                {showDeepIceberg ? 'Skrýt Ledovec' : 'Rozbalit 8D Důkazy'}
              </button>
            </div>

            <p className="text-[11px] text-slate-300 font-sans leading-relaxed">
              {data.pon_iceberg.iceberg_tip}
            </p>

            {showDeepIceberg && (
              <div className="pt-2 border-t border-cyan-900/60 space-y-2 text-[10px] animate-fadeIn">
                <p className="text-slate-400"><strong className="text-cyan-400">Trigger Vector:</strong> {data.pon_iceberg.trigger_vector}</p>
                <p className="text-slate-400"><strong className="text-violet-400">Exclusion Proof:</strong> {data.pon_iceberg.exclusion_proof}</p>
                <p className="text-slate-400"><strong className="text-amber-400">Minimization Check:</strong> {data.pon_iceberg.minimization_check}</p>
                <p className="text-[#00F0FF] break-all"><strong className="text-slate-400">CommitmentHash:</strong> {data.pon_iceberg.commitment_hash}</p>

                <div className="pt-2">
                  <span className="text-slate-400 block font-bold mb-1">Detailní 8D Důkazní Matice:</span>
                  <div className="grid grid-cols-2 gap-1.5">
                    {Object.entries(data.pon_iceberg.deep_evidence_matrix || {}).map(([d, val]) => (
                      <div key={d} className="bg-slate-950 p-1.5 rounded border border-slate-800 text-[9.5px]">
                        <span className="text-cyan-400 font-bold">{d}:</span> {val.status} ({val.weight})
                        <span className="block text-[8.5px] text-slate-500 truncate" title={val.note}>{val.note}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

      </div>
    </aside>
  );
}
