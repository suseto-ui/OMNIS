import React from "react";
import { 
  X, MessageSquare, BarChart3, Layers, Brain, Sliders, Activity, 
  HelpCircle, Sparkles 
} from "lucide-react";
import { ThemeToggle } from "../ThemeToggle";

interface NavigationSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  activeTab: string;
  setActiveTab: (tab: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "arena" | "zk_ledger") => void;
  onOpenHelp: () => void;
  theme: string;
  setTheme: (theme: any) => void;
}

export const NavigationSidebar: React.FC<NavigationSidebarProps> = ({
  isOpen,
  onClose,
  activeTab,
  setActiveTab,
  onOpenHelp,
  theme,
  setTheme,
}) => {
  if (!isOpen) return null;

  const handleTabClick = (tab: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "arena" | "zk_ledger") => {
    setActiveTab(tab);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity" 
        onClick={onClose} 
      />

      {/* Drawer */}
      <div className="relative w-80 max-w-[85vw] bg-[#060A17] border-r border-slate-800 h-full flex flex-col z-10 p-5 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-[#00F0FF]" />
            <span className="font-mono font-bold text-slate-100 text-sm tracking-wider">NAVIGACE O.M.N.I.S.</span>
          </div>
          <button 
            onClick={onClose} 
            className="p-1 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-2 py-6 overflow-y-auto">
          <button
            onClick={() => handleTabClick("chat")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "chat"
                ? "bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <MessageSquare className="w-4 h-4 text-[#00F0FF]" />
            <span>CHAT & DIKTOVÁNÍ</span>
          </button>

          <button
            onClick={() => handleTabClick("arena")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "arena"
                ? "bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/40 shadow-[0_0_12px_rgba(0,240,255,0.2)]"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <Activity className="w-4 h-4 text-[#00F0FF]" />
            <span>DELIBERAČNÍ ARÉNA</span>
          </button>

          <button
            onClick={() => handleTabClick("zk_ledger")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "zk_ledger"
                ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/40 shadow-[0_0_12px_rgba(16,185,129,0.2)]"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>ZK-AUDIT LEDGER</span>
          </button>

          <button
            onClick={() => handleTabClick("analytics")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "analytics"
                ? "bg-blue-500/15 text-blue-300 border border-blue-500/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <BarChart3 className="w-4 h-4 text-blue-400" />
            <span>ANALÝZA & AUDIT</span>
          </button>

          <button
            onClick={() => handleTabClick("archive")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "archive"
                ? "bg-cyan-500/15 text-cyan-300 border border-cyan-500/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <Layers className="w-4 h-4 text-cyan-400" />
            <span>ARCHIV VLÁKEN</span>
          </button>

          <button
            onClick={() => handleTabClick("nodes")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "nodes"
                ? "bg-[#A855F7]/15 text-[#A855F7] border border-[#A855F7]/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <Brain className="w-4 h-4 text-[#A855F7]" />
            <span>KOGNITIVNÍ UZLY</span>
          </button>

          <button
            onClick={() => handleTabClick("dashboard")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "dashboard"
                ? "bg-amber-500/15 text-amber-300 border border-amber-500/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <Sliders className="w-4 h-4 text-amber-400" />
            <span>SPRÁVA TÉMAT</span>
          </button>

          <button
            onClick={() => handleTabClick("octagon")}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-mono text-xs font-bold transition-all ${
              activeTab === "octagon"
                ? "bg-purple-500/15 text-purple-300 border border-purple-500/40"
                : "text-slate-400 hover:bg-slate-900 hover:text-slate-200 border border-transparent"
            }`}
          >
            <Activity className="w-4 h-4 text-purple-400" />
            <span>8D OCTAGON MATICE</span>
          </button>
        </nav>

        <div className="pt-4 border-t border-slate-800 space-y-3">
          <button
            onClick={() => {
              onOpenHelp();
              onClose();
            }}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl font-mono text-xs text-slate-300 bg-slate-900 hover:bg-slate-800 border border-slate-800"
          >
            <HelpCircle className="w-4 h-4 text-[#00F0FF]" />
            <span>PRŮVODCE & ZKRATKY</span>
          </button>

          <div className="flex items-center justify-between px-4 py-2 rounded-xl bg-slate-900/60 border border-slate-800">
            <span className="text-xs font-mono text-slate-400">Téma vzhledu</span>
            <ThemeToggle theme={theme} setTheme={setTheme} />
          </div>
        </div>
      </div>
    </div>
  );
};
