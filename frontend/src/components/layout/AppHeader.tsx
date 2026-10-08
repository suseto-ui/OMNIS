import React from "react";
import { 
  Menu, Brain, Upload, Download, Home, ChevronRight, FileText, Shield, Key, User 
} from "lucide-react";
import { ChatThread, UserRole } from "../../types";
import { ThemeToggle } from "../ThemeToggle";

interface AppHeaderProps {
  isMenuOpen: boolean;
  setIsMenuOpen: (open: boolean | ((prev: boolean) => boolean)) => void;
  activeTab: string;
  setActiveTab: (tab: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon") => void;
  dbStatus: "online" | "offline" | "checking";
  activeThread?: ChatThread;
  threadsCount: number;
  endpoint: string;
  lastError: string | null;
  resetToPrimary: () => void;
  ontologyDomain: string;
  importChatHistory: (e: React.ChangeEvent<HTMLInputElement>) => void;
  exportChatHistory: () => void;
  exportToPDF?: () => void;
  exportAdminPDF?: () => void;
  onOpenSearchModal: () => void;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
  theme?: string;
  setTheme?: (mode: any) => void;
  userRole?: UserRole;
  onToggleRole?: () => void;
  onOpenKeyRotator?: () => void;
  activeKeySlot?: number;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  isMenuOpen,
  setIsMenuOpen,
  activeTab,
  setActiveTab,
  dbStatus,
  activeThread,
  threadsCount,
  endpoint,
  lastError,
  resetToPrimary,
  ontologyDomain,
  importChatHistory,
  exportChatHistory,
  exportToPDF,
  exportAdminPDF,
  onOpenSearchModal,
  fileInputRef,
  theme,
  setTheme,
  userRole = "ADMIN_OPERATOR",
  onToggleRole,
  onOpenKeyRotator,
  activeKeySlot = 1,
}) => {
  const isAdmin = userRole === "ADMIN_OPERATOR";

  return (
    <>
      <header className="bg-[#060A17]/95 backdrop-blur-md border-b border-slate-800/80 sticky top-0 z-40 px-4 py-2.5 shadow-2xl flex-shrink-0">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-2 sm:gap-4">
          <div className="flex items-center gap-2 sm:gap-3">
            <button
              onClick={() => setIsMenuOpen((prev) => !prev)}
              className="p-2 rounded-xl text-slate-300 hover:text-[#00F0FF] bg-slate-900/80 border border-slate-800 hover:border-[#00F0FF]/40 transition-all focus:outline-none cursor-pointer"
              title="Postranní navigace"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-2 cursor-pointer" onClick={() => setActiveTab("chat")}>
              <div className="relative">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#00F0FF] via-purple-600 to-emerald-400 p-0.5 shadow-[0_0_15px_rgba(0,240,255,0.4)]">
                  <div className="w-full h-full bg-[#060A17] rounded-[10px] flex items-center justify-center">
                    <Brain className="w-4 h-4 text-[#00F0FF] animate-pulse" />
                  </div>
                </div>
                <span
                  className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-[#060A17] ${
                    dbStatus === "online" ? "bg-emerald-400" : "bg-amber-500 animate-ping"
                  }`}
                />
              </div>

              <div>
                <h1 className="text-sm sm:text-base font-bold font-mono tracking-wider text-slate-100 flex items-center gap-1.5">
                  O.M.N.I.S.
                  <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-[#00F0FF]/10 text-[#00F0FF] border border-[#00F0FF]/30 hidden sm:inline-block">
                    v2.7
                  </span>
                </h1>
                <p className="text-[10px] text-slate-400 font-mono hidden md:block">Cognitive Operating System</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1.5 sm:gap-2">
            {/* RBAC Role Switcher Button */}
            {onToggleRole && (
              <button
                onClick={onToggleRole}
                title={isAdmin ? "Role: ADMIN_OPERATOR (klikněte pro přepnutí do STANDARD_USER)" : "Role: STANDARD_USER (klikněte pro přepnutí do ADMIN_OPERATOR)"}
                className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono font-bold transition-all border shadow-sm cursor-pointer ${
                  isAdmin
                    ? "bg-amber-500/15 border-amber-500/40 text-amber-300 hover:bg-amber-500/25 shadow-[0_0_12px_rgba(245,158,11,0.2)]"
                    : "bg-[#00F0FF]/15 border-[#00F0FF]/40 text-[#00F0FF] hover:bg-[#00F0FF]/25 shadow-[0_0_12px_rgba(0,240,255,0.2)]"
                }`}
              >
                {isAdmin ? <Shield className="w-3.5 h-3.5 text-amber-400" /> : <User className="w-3.5 h-3.5 text-[#00F0FF]" />}
                <span className="hidden xs:inline">{isAdmin ? "ADMIN" : "UŽIVATEL"}</span>
              </button>
            )}

            {/* 3-Slot API Key Rotator Button (Admin Only) */}
            {isAdmin && onOpenKeyRotator && (
              <button
                onClick={onOpenKeyRotator}
                title="Otevřít 3-Slotový API Rotátor Klíčů Google Gemini"
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono font-bold bg-slate-900 border border-amber-500/30 text-amber-300 hover:border-amber-400 hover:text-white transition-all shadow-sm cursor-pointer"
              >
                <Key className="w-3.5 h-3.5 text-amber-400" />
                <span className="hidden sm:inline">ROTÁTOR</span>
                <span className="px-1.5 py-0.2 rounded text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/30 font-mono">
                  S{activeKeySlot}
                </span>
              </button>
            )}

            <input
              type="file"
              ref={fileInputRef}
              onChange={importChatHistory}
              accept=".json"
              className="hidden"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              title="Importovat historii z JSON souboru"
              className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-slate-800 text-slate-300 border border-slate-800 hover:border-slate-700 transition-all cursor-pointer"
            >
              <Upload className="w-4 h-4" />
              <span className="hidden xs:inline">IMPORT</span>
            </button>
            <button
              onClick={exportChatHistory}
              title="Exportovat historii chatu jako JSON"
              className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-slate-800 text-slate-300 border border-slate-800 hover:border-slate-700 transition-all cursor-pointer"
            >
              <Download className="w-4 h-4" />
              <span className="hidden xs:inline">JSON</span>
            </button>
            {exportToPDF && (
              <button
                onClick={exportToPDF}
                title="Exportovat aktivní vlákno do PDF"
                className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 hover:border-[#00F0FF] shadow-[0_0_10px_rgba(0,240,255,0.1)] hover:shadow-[0_0_15px_rgba(0,240,255,0.25)] transition-all cursor-pointer"
              >
                <FileText className="w-4 h-4" />
                <span className="hidden xs:inline">PDF</span>
              </button>
            )}
            {isAdmin && exportAdminPDF && (
              <button
                onClick={exportAdminPDF}
                title="Kompletní administrativní export vlákna (všechny navrhované varianty, plné texty, kognitivní data)"
                className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 sm:py-2 rounded-xl text-xs font-bold font-mono bg-red-950/20 hover:bg-red-500/20 text-red-400 border border-red-500/30 hover:border-red-500 shadow-[0_0_10px_rgba(239,68,68,0.1)] hover:shadow-[0_0_15px_rgba(239,68,68,0.25)] transition-all cursor-pointer"
              >
                <Shield className="w-4 h-4" />
                <span className="hidden xs:inline">ADMIN PDF</span>
              </button>
            )}
            {theme && setTheme && (
              <ThemeToggle theme={theme as any} setTheme={setTheme} />
            )}
          </div>
        </div>
      </header>

      {/* BREADCRUMBS NAVIGATION BAR */}
      <div className="bg-[#040711] border-b border-slate-800/80 px-4 py-1.5 flex items-center justify-between gap-2 text-xs font-mono text-slate-400 overflow-x-auto scrollbar-none flex-shrink-0 z-10">
        <div className="flex items-center gap-1.5 flex-nowrap">
          <button
            onClick={() => setActiveTab("chat")}
            className="flex items-center gap-1 text-slate-400 hover:text-[#00F0FF] transition-colors flex-shrink-0"
            title="Přejít na výchozí obrazovku Chatu"
          >
            <Home className="w-3.5 h-3.5 text-[#00F0FF]" />
            <span className="hidden sm:inline">Domů</span>
          </button>

          <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />

          <button
            onClick={() => setActiveTab(activeTab as any)}
            className={`font-bold transition-colors flex-shrink-0 uppercase ${
              activeTab === "chat"
                ? "text-[#00F0FF]"
                : activeTab === "analytics"
                ? "text-blue-400"
                : activeTab === "archive"
                ? "text-cyan-400"
                : activeTab === "nodes"
                ? "text-[#A855F7]"
                : activeTab === "dashboard"
                ? "text-amber-400"
                : activeTab === "octagon"
                ? "text-purple-400"
                : "text-slate-400"
            }`}
          >
            {activeTab === "chat" && "Chat & Diktování"}
            {activeTab === "analytics" && "Analytický Přehled"}
            {activeTab === "archive" && "Archiv Vláken"}
            {activeTab === "nodes" && "Kognitivní Uzly"}
            {activeTab === "dashboard" && "Správa Témat"}
            {activeTab === "octagon" && "8D Matice Dopadů"}
          </button>

          {activeTab === "chat" && activeThread && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span
                onClick={onOpenSearchModal}
                title="Aktivní vlákno (kliknutím otevřete vyhledávání/přepínač)"
                className="text-slate-200 truncate max-w-[140px] sm:max-w-[240px] bg-slate-900/90 hover:bg-slate-800 px-2 py-0.5 rounded border border-slate-700/80 cursor-pointer transition-colors"
              >
                {activeThread.title}
              </span>
            </>
          )}

          {activeTab === "analytics" && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span className="text-slate-300 bg-blue-950/40 px-2 py-0.5 rounded border border-blue-800/50">
                Sjednocený Audit (8D & Uzly)
              </span>
            </>
          )}

          {activeTab === "archive" && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span className="text-slate-300 bg-cyan-950/40 px-2 py-0.5 rounded border border-cyan-800/50">
                {threadsCount} Uložených Vláken
              </span>
            </>
          )}
        </div>

        {/* Quick Context & Domain Badge */}
        <div className="flex items-center gap-2.5 flex-shrink-0 text-[10px] font-mono">
          <span className="hidden md:inline text-slate-500 uppercase">Stav:</span>
          {endpoint === "/api/query" ? (
            <span
              className="px-2 py-0.5 rounded bg-emerald-950/40 border border-emerald-800 text-emerald-400 font-bold flex items-center gap-1"
              title="Online model je aktivní a připraven"
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
              ONLINE
            </span>
          ) : (
            <button
              onClick={resetToPrimary}
              title="Klikněte pro znovupřipojení k online modelu"
              className="px-2 py-0.5 rounded bg-amber-950/40 border border-amber-800 hover:border-amber-600 text-amber-300 font-bold flex items-center gap-1 transition-all"
            >
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse"></span>
              ZÁLOŽNÍ REŽIM
            </button>
          )}

          <span className="hidden md:inline text-slate-500 uppercase ml-1">Doména:</span>
          <span className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[#00F0FF] font-bold">
            {ontologyDomain}
          </span>
        </div>
      </div>
    </>
  );
};
