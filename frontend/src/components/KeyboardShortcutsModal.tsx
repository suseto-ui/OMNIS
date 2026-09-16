import React from "react";
import { X, Command, Mic, Search, GitMerge, Brain, Layers, Activity, HardDrive, Keyboard } from "lucide-react";

interface KeyboardShortcutsModalProps {
  onClose: () => void;
  onOpenSearch: () => void;
  onOpenMerge: () => void;
  onToggleDictation: () => void;
}

export const KeyboardShortcutsModal: React.FC<KeyboardShortcutsModalProps> = ({
  onClose,
  onOpenSearch,
  onOpenMerge,
  onToggleDictation,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-[#0A0F1D] border border-[#00F0FF]/40 rounded-2xl w-full max-w-2xl overflow-hidden shadow-[0_0_50px_rgba(0,240,255,0.15)] flex flex-col font-sans text-slate-200">
        {/* Header */}
        <div className="p-5 bg-[#060A17] border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Keyboard className="w-5 h-5 text-[#00F0FF]" />
            <h3 className="text-base font-mono font-bold text-slate-100 uppercase tracking-wide">
              Průvodce A Klávesové Zkratky O.M.N.I.S.
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto scrollbar-thin scrollbar-thumb-slate-800 text-xs">
          {/* Main Keyboard Shortcuts Grid */}
          <div className="space-y-3">
            <h4 className="font-mono font-bold text-[#00F0FF] uppercase text-xs flex items-center gap-1.5">
              <Command className="w-4 h-4 text-[#00F0FF]" />
              Klávesové Zkratky Pro Rychlou Navigaci
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 font-mono">
              <div
                onClick={() => {
                  onClose();
                  onOpenSearch();
                }}
                className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-[#00F0FF]/50 transition-all cursor-pointer flex items-center justify-between"
              >
                <div className="flex items-center gap-2">
                  <Search className="w-4 h-4 text-[#00F0FF]" />
                  <span>Full-text Vyhledávání</span>
                </div>
                <kbd className="px-2 py-1 rounded bg-slate-800 text-slate-300 text-[10px] border border-slate-700">
                  Ctrl + K
                </kbd>
              </div>

              <div
                onClick={() => {
                  onClose();
                  onToggleDictation();
                }}
                className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-red-500/50 transition-all cursor-pointer flex items-center justify-between"
              >
                <div className="flex items-center gap-2">
                  <Mic className="w-4 h-4 text-red-400" />
                  <span>Hlasové Diktování</span>
                </div>
                <kbd className="px-2 py-1 rounded bg-slate-800 text-slate-300 text-[10px] border border-slate-700">
                  Ctrl + M
                </kbd>
              </div>

              <div
                onClick={() => {
                  onClose();
                  onOpenMerge();
                }}
                className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-[#A855F7]/50 transition-all cursor-pointer flex items-center justify-between"
              >
                <div className="flex items-center gap-2">
                  <GitMerge className="w-4 h-4 text-[#A855F7]" />
                  <span>Sloučit Vlákna</span>
                </div>
                <kbd className="px-2 py-1 rounded bg-slate-800 text-slate-300 text-[10px] border border-slate-700">
                  Ctrl + J
                </kbd>
              </div>

              <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <HardDrive className="w-4 h-4 text-emerald-400" />
                  <span>IndexedDB Auto-Save</span>
                </div>
                <span className="text-[10px] text-emerald-400 font-bold">Autonomní (60s)</span>
              </div>
            </div>
          </div>

          {/* Feature Highlights Guide */}
          <div className="space-y-3 pt-3 border-t border-slate-800">
            <h4 className="font-mono font-bold text-[#A855F7] uppercase text-xs flex items-center gap-1.5">
              <Brain className="w-4 h-4 text-[#A855F7]" />
              Klíčové Inovativní Prvky Aplikace
            </h4>

            <div className="space-y-2 font-sans text-slate-300">
              <div className="p-3 rounded-xl bg-[#080D1F] border border-slate-800 space-y-1">
                <span className="font-mono font-bold text-xs text-cyan-400 block">
                  🎙️ Hlasové Diktování Web Speech API
                </span>
                <p>
                  Mluvte přímo do mikrofonu s okamžitým přepisováním řeči do češtiny v reálném čase.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-[#080D1F] border border-slate-800 space-y-1">
                <span className="font-mono font-bold text-xs text-purple-400 block">
                  🧬 Deep Dive Dekompozice Kognitivních Uzlů
                </span>
                <p>
                  Rozložte reakci modelu na sémantické jádro, entropickou redukci, homeostatickou kotvu a stochastické větvění.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-[#080D1F] border border-slate-800 space-y-1">
                <span className="font-mono font-bold text-xs text-amber-400 block">
                  📊 Octagon 8D Matice Dopadů & Forenzní Audit
                </span>
                <p>
                  Simulujte dopady řešení na 8 systémových dimenzí (systémy, ekonomie, bezpečnost, ekologie atd.).
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-[#060A17] border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl text-xs font-mono font-bold bg-[#00F0FF] hover:bg-[#00F0FF]/80 text-slate-950 transition-all"
          >
            Rozumím
          </button>
        </div>
      </div>
    </div>
  );
};
