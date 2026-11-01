import React, { useState } from 'react';
import { ShieldCheck, Activity, AlertTriangle, CheckCircle2, Sparkles, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';

export interface PromptCorrectionData {
  original_prompt: string;
  detected_inconsistencies: string[];
  corrected_prompt: string;
  confidence_score: float;
  prompt_health_index: number;
  healing_actions_applied: string[];
  requires_user_authorization?: boolean;
}

interface Props {
  correction: PromptCorrectionData;
  onApplyCorrection?: (correctedText: string) => void;
  className?: string;
}

export function SelfHealingPromptMonitor({ correction, onApplyCorrection, className = '' }: Props) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isApplied, setIsApplied] = useState(false);

  const health = correction.prompt_health_index || 98.5;
  
  let healthBadgeColor = "text-emerald-400 border-emerald-500/30 bg-emerald-950/20";
  let healthBarColor = "bg-emerald-500";
  if (health < 60) {
    healthBadgeColor = "text-rose-400 border-rose-500/30 bg-rose-950/20";
    healthBarColor = "bg-rose-500";
  } else if (health < 85) {
    healthBadgeColor = "text-amber-400 border-amber-500/30 bg-amber-950/20";
    healthBarColor = "bg-amber-500";
  }

  const handleApply = () => {
    if (onApplyCorrection && correction.corrected_prompt) {
      onApplyCorrection(correction.corrected_prompt);
      setIsApplied(true);
      setTimeout(() => setIsApplied(false), 3000);
    }
  };

  return (
    <div className={`bg-[#080d19]/90 border border-slate-800 rounded-xl p-3.5 shadow-lg font-sans text-xs ${className}`}>
      {/* Hlavní lišta zdraví promptu */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-cyan-950/60 border border-cyan-800/80 text-cyan-400">
            <Activity className="w-4 h-4 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-slate-200 tracking-wide">
                SELF-HEALING PROMPT MONITOR
              </span>
              <span className={`px-2 py-0.5 rounded-md border font-mono text-[10px] font-bold ${healthBadgeColor}`}>
                {health.toFixed(1)}% INTEGRITA
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-mono mt-0.5">
              {correction.detected_inconsistencies.length > 0 
                ? `Detekovány ${correction.detected_inconsistencies.length} anomálie – logická rekonvalescence aktivní.`
                : 'Prompt vykazuje vysokou logickou integritu (0 anomálií).'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-800 transition-colors flex items-center gap-1 font-mono text-[10px]"
          >
            <span>{isExpanded ? 'SKRÝT DETAIL' : 'DETAIL'}</span>
            {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      {/* ProgressBar zdraví promptu */}
      <div className="w-full bg-slate-900 rounded-full h-1 mt-2.5 overflow-hidden">
        <div 
          className={`h-full transition-all duration-500 ${healthBarColor}`} 
          style={{ width: `${Math.min(100, Math.max(0, health))}%` }}
        />
      </div>

      {/* Rozbalitelný detail korekčních kroků */}
      {isExpanded && (
        <div className="mt-3 pt-3 border-t border-slate-800/80 space-y-3 font-mono text-[11px]">
          {/* Anomálie */}
          {correction.detected_inconsistencies.length > 0 && (
            <div>
              <span className="text-amber-400 font-bold flex items-center gap-1 mb-1">
                <AlertTriangle className="w-3.5 h-3.5" /> Detekované logické rozporuplnosti:
              </span>
              <ul className="list-disc list-inside text-slate-400 space-y-0.5 pl-1">
                {correction.detected_inconsistencies.map((inc, i) => (
                  <li key={i}>{inc}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Aplikované self-healing akce */}
          <div>
            <span className="text-cyan-400 font-bold flex items-center gap-1 mb-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Rekonvalescenční zásahy:
            </span>
            <ul className="list-disc list-inside text-slate-300 space-y-0.5 pl-1">
              {correction.healing_actions_applied.map((act, i) => (
                <li key={i}>{act}</li>
              ))}
            </ul>
          </div>

          {/* Náhled korigovaného promptu */}
          {correction.corrected_prompt && (
            <div className="bg-[#050810] p-2.5 rounded-lg border border-slate-800">
              <div className="flex items-center justify-between mb-1">
                <span className="text-slate-400 font-bold flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-cyan-400" /> Optimalizovaný prompt:
                </span>
                {onApplyCorrection && (
                  <button
                    onClick={handleApply}
                    className="px-2 py-1 rounded bg-cyan-600 hover:bg-cyan-500 text-black font-bold text-[10px] flex items-center gap-1 transition-all"
                  >
                    <RefreshCw className={`w-3 h-3 ${isApplied ? 'animate-spin' : ''}`} />
                    <span>{isApplied ? 'APLIKOVÁNO' : 'APLIKOVAT PROMPT'}</span>
                  </button>
                )}
              </div>
              <p className="text-slate-300 whitespace-pre-wrap leading-relaxed text-[11px] font-sans bg-slate-950/60 p-2 rounded border border-slate-900">
                {correction.corrected_prompt}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
