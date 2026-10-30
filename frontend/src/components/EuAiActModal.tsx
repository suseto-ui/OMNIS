import React from "react";
import { Shield, Info, CheckCircle2, AlertTriangle, Cpu, Scale, FileText, X } from "lucide-react";

interface EuAiActModalProps {
  onClose: () => void;
}

export const EuAiActModal: React.FC<EuAiActModalProps> = ({ onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        className="fixed inset-0"
        onClick={onClose}
      />
      <div className="relative w-full max-w-2xl bg-[#070B18] border border-blue-500/40 rounded-2xl shadow-[0_0_40px_rgba(59,130,246,0.2)] overflow-hidden z-10 font-mono text-slate-200 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 bg-[#0A1024] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-500/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
              <Scale className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-100 uppercase tracking-wider flex items-center gap-2">
                EU AI Act & Informační Audit
                <span className="text-[10px] bg-blue-500/20 text-blue-300 border border-blue-500/40 px-2 py-0.5 rounded">
                  Čl. 50 Transparentnost
                </span>
              </h3>
              <p className="text-[11px] text-slate-400">
                Soulad se směrnicí Evropského parlamentu a Rady o umělé inteligenci
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-4 text-xs leading-relaxed text-slate-300">
          <div className="p-3.5 rounded-xl bg-blue-950/30 border border-blue-500/30 space-y-1">
            <span className="text-[#00F0FF] font-bold block uppercase text-[11px]">
              1. Identifikace AI Systému & Účel
            </span>
            <p>
              O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis) je kognitivní asistenční systém založený na hybridní neuronově-symbolické syntéze. Systém je navržen výhradně pro podporu rozhodování, analýzu komplexních systémů a mezioborový výzkum.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="p-3.5 rounded-xl bg-[#0B1228] border border-slate-800 space-y-1">
              <span className="text-purple-400 font-bold block uppercase text-[11px] flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5" />
                Použité Modely & Architektura
              </span>
              <p className="text-[11px] text-slate-400">
                Primární inference: Google Gemini 3.5 Flash & Gemini 3.1 Pro s lokální 8D constraint maticí a heuristickým kognitivním zámkem.
              </p>
            </div>

            <div className="p-3.5 rounded-xl bg-[#0B1228] border border-slate-800 space-y-1">
              <span className="text-emerald-400 font-bold block uppercase text-[11px] flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5" />
                Lidský dohled (Human-in-the-Loop)
              </span>
              <p className="text-[11px] text-slate-400">
                Uživatel má plnou kontrolu nad vstupy a výstupy. Kognitivní zámek zabraňuje náhodnému nebo ukvapenému odeslání neúplných dat.
              </p>
            </div>
          </div>

          <div className="p-3.5 rounded-xl bg-[#0B1228] border border-slate-800 space-y-2">
            <span className="text-amber-400 font-bold block uppercase text-[11px] flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
              Omezení a Zřeknutí se Odpovědnosti
            </span>
            <ul className="list-disc pl-4 space-y-1 text-[11px] text-slate-400">
              <li>Výstupy modelu mají doporučující a analytický charakter a nepředstavují právní, finanční ani lékařské poradenství.</li>
              <li>Generovaný obsah může obsahovat nepřesnosti a uživatel by měl kritická rozhodnutí nezávisle verifikovat.</li>
              <li>Data z konverzace jsou zpracovávána lokálně a v souladu s GDPR; do trénovací množiny se neukládají.</li>
            </ul>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-[#0A1024] flex items-center justify-between">
          <span className="text-[10px] text-slate-500">
            Audit verze: 2026.4 • O.M.N.I.S. Core
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-[#00F0FF]/15 hover:bg-[#00F0FF]/25 border border-[#00F0FF]/40 text-[#00F0FF] font-bold text-xs transition-all"
          >
            Rozumím a zavřít
          </button>
        </div>
      </div>
    </div>
  );
};
