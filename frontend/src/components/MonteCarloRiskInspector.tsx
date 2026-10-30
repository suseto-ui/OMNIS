import React, { useState, useMemo } from "react";
import { 
  BarChart3, AlertTriangle, ShieldCheck, Zap, Activity, Sliders, RefreshCw, Scale, TrendingDown, Sparkles
} from "lucide-react";
import { ImpactMatrixScores, simulateMonteCarloResilience, MonteCarloRiskAnalysis } from "../omnisEngine";

interface MonteCarloRiskInspectorProps {
  impactMatrix?: ImpactMatrixScores;
}

export const MonteCarloRiskInspector: React.FC<MonteCarloRiskInspectorProps> = ({
  impactMatrix,
}) => {
  const [perturbationSigma, setPerturbationSigma] = useState<number>(0.08);
  const [simulationSeed, setSimulationSeed] = useState<number>(1);

  const activeVector = useMemo(() => {
    return impactMatrix || {
      sys: 0.72, econ: 0.65, psych: 0.60, eco: 0.58,
      law: 0.80, sec: 0.85, phys: 0.70, soc: 0.62,
      composite_score: 0.70, reasoning: ""
    };
  }, [impactMatrix]);

  const mcResult: MonteCarloRiskAnalysis = useMemo(() => {
    // simulationSeed triggers re-calculation
    return simulateMonteCarloResilience(activeVector, 1000, perturbationSigma);
  }, [activeVector, perturbationSigma, simulationSeed]);

  const maxBinCount = useMemo(() => {
    return Math.max(...mcResult.distributionHistogram.map(b => b.count), 1);
  }, [mcResult]);

  return (
    <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-6 font-mono text-slate-100 shadow-xl">
      {/* Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-3 border-b border-slate-800 pb-3">
        <div className="flex items-center gap-2.5">
          <BarChart3 className="w-5 h-5 text-[#00F0FF]" />
          <div>
            <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider">
              Monte Carlo 1 000-Iterační VaR & Black Swan Inspector
            </h3>
            <p className="text-xs text-slate-400">
              Stochastické perturbační modelování tlustých chvostů (Kurtosis) a systémových ztrát.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {mcResult.isBlackSwanProne ? (
            <span className="px-2.5 py-1 rounded-xl bg-red-500/15 border border-red-500/40 text-red-400 text-xs font-bold flex items-center gap-1.5 animate-pulse">
              <AlertTriangle className="w-4 h-4 text-red-400" />
              <span>BLACK SWAN HAZARD (Kurtosis {mcResult.kurtosis})</span>
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded-xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-400 text-xs font-bold flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Normalizovaný Distribuce (Kurtosis {mcResult.kurtosis})</span>
            </span>
          )}
        </div>
      </div>

      {/* Control Panel: Noise Sigma Slider */}
      <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 grid grid-cols-1 md:grid-cols-3 gap-4 items-center">
        <div>
          <label className="text-[10px] text-slate-400 uppercase font-bold block mb-1">
            Počet Stochastických Iterací:
          </label>
          <span className="text-xs text-[#00F0FF] font-bold">1 000 Monte Carlo vzorků</span>
        </div>

        <div className="space-y-1">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400">Perturbační Šum (σ = {Math.round(perturbationSigma * 100)} %):</span>
            <span className="text-[#00F0FF] font-bold">±{Math.round(perturbationSigma * 100)} %</span>
          </div>
          <input
            type="range"
            min={0.01}
            max={0.20}
            step={0.01}
            value={perturbationSigma}
            onChange={(e) => setPerturbationSigma(parseFloat(e.target.value))}
            className="w-full accent-[#00F0FF] cursor-pointer"
          />
        </div>

        <div className="flex justify-end">
          <button
            onClick={() => setSimulationSeed(s => s + 1)}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <RefreshCw className="w-3.5 h-3.5 text-[#00F0FF]" />
            <span>Přepočítat 1 000 vzorků</span>
          </button>
        </div>
      </div>

      {/* Stat Cards Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {/* Mean Resilience */}
        <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <span className="text-[10px] text-slate-400 uppercase font-bold block">Průměr H_w:</span>
          <span className="text-base text-[#00F0FF] font-bold">
            {Math.round(mcResult.meanResilience * 100)} %
          </span>
          <span className="text-[9px] text-slate-400 block">E[H_w] baseline</span>
        </div>

        {/* Std Dev */}
        <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <span className="text-[10px] text-slate-400 uppercase font-bold block">Rozptyl (σ_H):</span>
          <span className="text-base text-cyan-300 font-bold">
            ±{(mcResult.stdDev * 100).toFixed(1)} %
          </span>
          <span className="text-[9px] text-slate-400 block">Směrodatná odchylka</span>
        </div>

        {/* VaR 95% */}
        <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 space-y-1">
          <span className="text-[10px] text-amber-400 uppercase font-bold block">VaR 95 %:</span>
          <span className="text-base text-amber-300 font-bold">
            {Math.round(mcResult.var95 * 100)} %
          </span>
          <span className="text-[9px] text-amber-400/80 block">5% nejhorší práh</span>
        </div>

        {/* VaR 99% */}
        <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 space-y-1">
          <span className="text-[10px] text-red-400 uppercase font-bold block">VaR 99 %:</span>
          <span className="text-base text-red-300 font-bold">
            {Math.round(mcResult.var99 * 100)} %
          </span>
          <span className="text-[9px] text-red-400/80 block">1% extrémní práh</span>
        </div>

        {/* Kurtosis */}
        <div className={`p-3 rounded-xl border space-y-1 ${
          mcResult.kurtosis > 3.0 ? "bg-red-500/15 border-red-500/40 text-red-200" : "bg-slate-900 border-slate-800"
        }`}>
          <span className="text-[10px] text-slate-400 uppercase font-bold block">Kurtosis (4. moment):</span>
          <span className="text-base font-bold">{mcResult.kurtosis}</span>
          <span className="text-[9px] text-slate-400 block">
            {mcResult.kurtosis > 3.0 ? "Tlusté chvosty (Fat Tails)" : "Normální rozdělení"}
          </span>
        </div>

        {/* Antifragility Index */}
        <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 space-y-1">
          <span className="text-[10px] text-emerald-400 uppercase font-bold block">Antifragility Index:</span>
          <span className="text-base text-emerald-300 font-bold">
            +{(mcResult.antifragileGain * 100).toFixed(1)} %
          </span>
          <span className="text-[9px] text-emerald-400/80 block">Talebův zisk při šumu</span>
        </div>
      </div>

      {/* 10-Bin Distribution Histogram Visualization */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-400 uppercase font-bold">
            10-Binový Histogram Četností Resilience Leontiefova Vektoru (1 000 Vzorků)
          </span>
          <div className="flex items-center gap-3 text-[10px]">
            <span className="flex items-center gap-1 text-amber-400">
              <span className="w-2 h-2 rounded-full bg-amber-400" /> VaR 95% ({Math.round(mcResult.var95 * 100)}%)
            </span>
            <span className="flex items-center gap-1 text-red-400">
              <span className="w-2 h-2 rounded-full bg-red-400" /> VaR 99% ({Math.round(mcResult.var99 * 100)}%)
            </span>
          </div>
        </div>

        {/* SVG/Canvas Style Histogram Container */}
        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-3">
          <div className="h-44 flex items-end gap-2 pt-4 px-2 relative border-b border-slate-800">
            {mcResult.distributionHistogram.map((bin, idx) => {
              const heightPercent = Math.round((bin.count / maxBinCount) * 100);
              const isBelowVar95 = bin.binEnd <= mcResult.var95;
              const isBelowVar99 = bin.binEnd <= mcResult.var99;

              let barBg = "bg-[#00F0FF]";
              if (isBelowVar99) barBg = "bg-red-500";
              else if (isBelowVar95) barBg = "bg-amber-400";

              return (
                <div key={idx} className="flex-1 flex flex-col items-center gap-1 group relative h-full justify-end">
                  {/* Tooltip on hover */}
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-8 px-2 py-1 rounded bg-slate-800 border border-slate-700 text-[9px] text-slate-100 whitespace-nowrap z-20 pointer-events-none">
                    Interval {bin.binStart} - {bin.binEnd}: {bin.count} vzorků
                  </div>

                  <span className="text-[9px] text-slate-400 font-bold">
                    {bin.count}
                  </span>

                  <div
                    style={{ height: `${Math.max(6, heightPercent)}%` }}
                    className={`w-full rounded-t-md ${barBg} transition-all duration-300 shadow-md group-hover:brightness-125`}
                  />
                </div>
              );
            })}
          </div>

          {/* Bin Labels */}
          <div className="flex justify-between text-[9px] text-slate-400">
            {mcResult.distributionHistogram.map((bin, idx) => (
              <span key={idx} className="flex-1 text-center truncate">
                {bin.binStart}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Narrative Risk Diagnosis */}
      <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 leading-relaxed space-y-1">
        <span className="text-[#00F0FF] font-bold uppercase text-[10px] block">
          Monte Carlo Riziková Diagnóza:
        </span>
        <p>
          Při perturbačním šumu <span className="text-slate-100 font-bold">±{Math.round(perturbationSigma * 100)} %</span> vykazuje systém průměrnou resilienci <span className="text-[#00F0FF] font-bold">{Math.round(mcResult.meanResilience * 100)} %</span>. S 95% pravděpodobností klesne resilience maximálně na <span className="text-amber-400 font-bold">{Math.round(mcResult.var95 * 100)} %</span>.
          {mcResult.isBlackSwanProne
            ? " VAROVÁNÍ: Detekován zvýšený koeficient špičatosti (Kurtosis > 3.0). V systému existuje riziko nečekaných propadů (Black Swan events). Doporučuje se zvýšit redundanci v doménách 'sys' a 'sec'."
            : " Distribuce je stabilní s nízkou špičatostí, což indikuje vysokou odolnost vůči stochastickým šokům."
          }
        </p>
      </div>
    </div>
  );
};
