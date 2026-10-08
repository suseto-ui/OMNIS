import React from "react";
import { 
  GitMerge, 
  Search, 
  Download, 
  Upload, 
  HardDrive, 
  Database, 
  MessageSquare, 
  Clock, 
  CheckCircle2, 
  Layers,
  FileJson,
  Plus
} from "lucide-react";
import { ChatThread } from "../App";

interface ThreadsArchiveDashboardProps {
  threads: ChatThread[];
  activeThreadId: string;
  onSelectThread: (id: string) => void;
  onCreateNewThread: () => void;
  onOpenSearch: () => void;
  onOpenMerge: () => void;
  onExportJSON: () => void;
  onImportClick: () => void;
  lastIndexedDbSave: string | null;
}

export const ThreadsArchiveDashboard: React.FC<ThreadsArchiveDashboardProps> = ({
  threads,
  activeThreadId,
  onSelectThread,
  onCreateNewThread,
  onOpenSearch,
  onOpenMerge,
  onExportJSON,
  onImportClick,
  lastIndexedDbSave,
}) => {
  const totalMessages = threads.reduce((acc, t) => acc + t.messages.length, 0);

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6 max-w-7xl mx-auto w-full font-sans text-slate-200 scrollbar-thin scrollbar-thumb-slate-800">
      {/* Header Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-[#060A17] via-[#0A1128] to-[#060A17] border border-[#00F0FF]/30 shadow-[0_0_30px_rgba(0,240,255,0.1)] flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Layers className="w-6 h-6 text-[#00F0FF]" />
            <h2 className="text-xl font-mono font-bold text-slate-100 tracking-wide uppercase">
              Správce Konverzačních Vláken A Archiv
            </h2>
          </div>
          <p className="text-xs text-slate-400 font-sans max-w-2xl">
            Centrální pracovní prostředí pro správu, full-textové vyhledávání, chronologické slučování a zálohování konverzačních vláken O.M.N.I.S.
          </p>
        </div>

        <button
          onClick={onCreateNewThread}
          className="px-4 py-2.5 rounded-xl text-xs font-mono font-bold bg-[#00F0FF] hover:bg-[#00F0FF]/80 text-slate-950 shadow-[0_0_15px_rgba(0,240,255,0.25)] flex items-center gap-2 transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Vytvořit Nové Vlákno</span>
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Threads */}
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Celkem Vláken</span>
            <span className="text-2xl font-mono font-bold text-[#00F0FF]">{threads.length}</span>
          </div>
          <MessageSquare className="w-8 h-8 text-[#00F0FF]/30" />
        </div>

        {/* Total Messages */}
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Indexované Zprávy</span>
            <span className="text-2xl font-mono font-bold text-[#A855F7]">{totalMessages}</span>
          </div>
          <Layers className="w-8 h-8 text-[#A855F7]/30" />
        </div>

        {/* IndexedDB Backup Status */}
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-mono text-slate-400 uppercase block">IndexedDB Redundance</span>
            <span className="text-sm font-mono font-bold text-emerald-400 flex items-center gap-1.5 mt-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              {lastIndexedDbSave ? lastIndexedDbSave : "Aktivní (60s)"}
            </span>
          </div>
          <HardDrive className="w-8 h-8 text-emerald-400/30 animate-pulse" />
        </div>

        {/* Storage Engine */}
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Ukládací Vrstva</span>
            <span className="text-xs font-mono font-bold text-amber-400 mt-1 block">
              IndexedDB + Cloud SQL
            </span>
          </div>
          <Database className="w-8 h-8 text-amber-400/30" />
        </div>
      </div>

      {/* Quick Action Tools Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Full-text Search Action */}
        <button
          onClick={onOpenSearch}
          className="group p-5 rounded-2xl bg-[#080D1F] border border-[#00F0FF]/30 hover:border-[#00F0FF] shadow-[0_0_15px_rgba(0,240,255,0.05)] hover:shadow-[0_0_20px_rgba(0,240,255,0.15)] transition-all text-left flex flex-col justify-between space-y-3"
        >
          <div className="w-10 h-10 rounded-xl bg-[#00F0FF]/15 border border-[#00F0FF]/40 flex items-center justify-center text-[#00F0FF]">
            <Search className="w-5 h-5 group-hover:scale-110 transition-transform" />
          </div>
          <div>
            <h3 className="text-sm font-mono font-bold text-slate-100 uppercase">Full-Text Vyhledávání</h3>
            <p className="text-xs text-slate-400 mt-1">
              Prohledávejte zprávy napříč všemi vlákny s okamžitým přeskakováním.
            </p>
          </div>
        </button>

        {/* Merge Threads Action */}
        <button
          onClick={onOpenMerge}
          className="group p-5 rounded-2xl bg-[#080D1F] border border-[#A855F7]/30 hover:border-[#A855F7] shadow-[0_0_15px_rgba(168,85,247,0.05)] hover:shadow-[0_0_20px_rgba(168,85,247,0.15)] transition-all text-left flex flex-col justify-between space-y-3"
        >
          <div className="w-10 h-10 rounded-xl bg-[#A855F7]/15 border border-[#A855F7]/40 flex items-center justify-center text-[#A855F7]">
            <GitMerge className="w-5 h-5 group-hover:scale-110 transition-transform" />
          </div>
          <div>
            <h3 className="text-sm font-mono font-bold text-slate-100 uppercase">Sloučit Vlákna</h3>
            <p className="text-xs text-slate-400 mt-1">
              Spojte dvě konverzační vlákna do jednoho chronologického celku.
            </p>
          </div>
        </button>

        {/* Export JSON Action */}
        <button
          onClick={onExportJSON}
          className="group p-5 rounded-2xl bg-[#080D1F] border border-emerald-500/30 hover:border-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.05)] hover:shadow-[0_0_20px_rgba(16,185,129,0.15)] transition-all text-left flex flex-col justify-between space-y-3"
        >
          <div className="w-10 h-10 rounded-xl bg-emerald-500/15 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
            <Download className="w-5 h-5 group-hover:scale-110 transition-transform" />
          </div>
          <div>
            <h3 className="text-sm font-mono font-bold text-slate-100 uppercase">Exportovat JSON</h3>
            <p className="text-xs text-slate-400 mt-1">
              Stáhněte si kompletní zálohu konverzací ve strukturovaném formátu.
            </p>
          </div>
        </button>

        {/* Import JSON Action */}
        <button
          onClick={onImportClick}
          className="group p-5 rounded-2xl bg-[#080D1F] border border-amber-500/30 hover:border-amber-500 shadow-[0_0_15px_rgba(245,158,11,0.05)] hover:shadow-[0_0_20px_rgba(245,158,11,0.15)] transition-all text-left flex flex-col justify-between space-y-3"
        >
          <div className="w-10 h-10 rounded-xl bg-amber-500/15 border border-amber-500/40 flex items-center justify-center text-amber-400">
            <Upload className="w-5 h-5 group-hover:scale-110 transition-transform" />
          </div>
          <div>
            <h3 className="text-sm font-mono font-bold text-slate-100 uppercase">Importovat JSON</h3>
            <p className="text-xs text-slate-400 mt-1">
              Nahrát externí soubor historie a obnovit konverzační vlákna.
            </p>
          </div>
        </button>
      </div>

      {/* Threads Overview List */}
      <div className="p-5 rounded-2xl bg-[#060A17] border border-slate-800 space-y-4">
        <h3 className="text-sm font-mono font-bold text-[#00F0FF] uppercase flex items-center gap-2">
          <FileJson className="w-4 h-4 text-[#00F0FF]" />
          Seznam Všech Aktivních Vláken ({threads.length})
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {threads.map((t) => {
            const isActive = t.id === activeThreadId;
            const msgCount = t.messages.length;
            const lastUpdated = new Date(t.updatedAt).toLocaleString([], {
              month: "numeric",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            });

            return (
              <div
                key={t.id}
                onClick={() => onSelectThread(t.id)}
                className={`p-4 rounded-xl border transition-all cursor-pointer flex flex-col justify-between space-y-3 ${
                  isActive
                    ? "bg-[#00F0FF]/10 border-[#00F0FF] shadow-[0_0_15px_rgba(0,240,255,0.15)]"
                    : "bg-[#070B18] border-slate-800 hover:border-slate-700 hover:bg-[#090E20]"
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[10px] font-mono">
                    <span className="text-slate-400">Vlákno ID: {t.id.substring(0, 14)}...</span>
                    {isActive && (
                      <span className="px-1.5 py-0.5 rounded bg-[#00F0FF]/20 text-[#00F0FF] font-bold">
                        AKTIVNÍ
                      </span>
                    )}
                  </div>
                  <h4 className="text-xs font-mono font-bold text-slate-100 truncate">{t.title}</h4>
                </div>

                <div className="pt-2 border-t border-slate-800/60 flex items-center justify-between text-[10px] font-mono text-slate-400">
                  <span>{msgCount} zpráv</span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3 text-slate-500" />
                    {lastUpdated}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
