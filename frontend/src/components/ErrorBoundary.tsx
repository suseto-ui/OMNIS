import React, { Component, ErrorInfo, ReactNode } from "react";
import { AlertTriangle, RefreshCw, Home, Download, Database, ShieldCheck } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  savedToStorage: boolean;
  exported: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
    savedToStorage: false,
    exported: false,
  };

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("CRITICAL EXCEPTION in O.M.N.I.S. UI:", error, errorInfo);
    this.setState({ errorInfo });

    try {
      // 1. Záchrana a snapshot konverzačních vláken do nouzového klíče v localStorage
      const existingThreads = localStorage.getItem("omnis_chat_threads");
      const activeThreadId = localStorage.getItem("omnis_active_thread_id");

      const crashDump = {
        timestamp: new Date().toISOString(),
        error: {
          name: error.name,
          message: error.message,
          stack: error.stack,
        },
        componentStack: errorInfo.componentStack,
        activeThreadId: activeThreadId,
        threadsBackup: existingThreads ? JSON.parse(existingThreads) : [],
        url: window.location.href,
        userAgent: navigator.userAgent,
      };

      localStorage.setItem("omnis_crash_dump", JSON.stringify(crashDump));
      localStorage.setItem("omnis_emergency_threads_backup", existingThreads || "[]");
      this.setState({ savedToStorage: true });
      console.info("[ErrorBoundary] Konverzační vlákna byla úspěšně zálohována do localStorage (omnis_crash_dump).");
    } catch (storageError) {
      console.error("[ErrorBoundary] Nepodařilo se zálohovat stav do localStorage:", storageError);
    }
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null, exported: false });
  };

  private handleReload = () => {
    window.location.reload();
  };

  private handleExportDiagnostics = () => {
    try {
      const dumpRaw = localStorage.getItem("omnis_crash_dump");
      let dumpObj: Record<string, any> = {};
      if (dumpRaw) {
        dumpObj = JSON.parse(dumpRaw);
      } else {
        dumpObj = {
          timestamp: new Date().toISOString(),
          error: {
            name: this.state.error?.name,
            message: this.state.error?.message,
            stack: this.state.error?.stack,
          },
          componentStack: this.state.errorInfo?.componentStack,
          threadsBackup: JSON.parse(localStorage.getItem("omnis_chat_threads") || "[]"),
        };
      }

      const jsonStr = JSON.stringify(dumpObj, null, 2);
      const blob = new Blob([jsonStr], { type: "application/json;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `omnis_diagnostic_dump_${Date.now()}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      this.setState({ exported: true });
    } catch (e) {
      console.error("[ErrorBoundary] Export diagnostiky selhal:", e);
      alert("Nepodařilo se vygenerovat export diagnostiky.");
    }
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen bg-[#050810] text-slate-200 flex items-center justify-center p-4 font-sans selection:bg-[#00F0FF]/30">
          <div className="max-w-xl w-full bg-[#0A0F1D]/95 backdrop-blur-md border border-rose-500/40 rounded-2xl p-6 shadow-[0_0_35px_rgba(244,63,94,0.2)] relative overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-rose-500 via-amber-500 to-rose-500" />

            <div className="flex items-start gap-4 mb-4">
              <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-400 flex-shrink-0">
                <AlertTriangle className="w-6 h-6 animate-pulse" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-100 font-mono tracking-wide">
                  KRITICKÁ VÝJIMKA KOGNITIVNÍHO ROZHRANÍ
                </h2>
                <p className="text-xs text-rose-400 font-mono mt-0.5">
                  Došlo k neočekávanému pádu rozhraní. Stav konverzačních vláken byl zabezpečen.
                </p>
              </div>
            </div>

            {/* Oznámení o záchraně dat */}
            <div className="bg-[#050811] rounded-xl p-3 border border-emerald-500/30 mb-4 flex items-center justify-between text-xs font-mono text-emerald-400">
              <div className="flex items-center gap-2">
                <Database className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span>
                  {this.state.savedToStorage
                    ? "Konverzační vlákna byla automaticky uložena do localStorage."
                    : "Příprava zálohy vláken..."}
                </span>
              </div>
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
            </div>

            {/* Diagnostický stack trace */}
            <div className="bg-[#050811] rounded-xl p-3.5 border border-slate-800/80 mb-5 font-mono text-xs overflow-x-auto max-h-44 text-slate-400 scrollbar-thin">
              <p className="text-rose-300 font-semibold mb-1">
                {this.state.error?.name}: {this.state.error?.message || "Neznámá výjimka při vykreslování"}
              </p>
              {this.state.errorInfo?.componentStack && (
                <pre className="text-[10px] text-slate-500 whitespace-pre-wrap leading-relaxed">
                  {this.state.errorInfo.componentStack}
                </pre>
              )}
            </div>

            {/* Tlačítka akcí */}
            <div className="flex flex-wrap items-center justify-between gap-2.5 pt-2 border-t border-slate-800/80">
              <button
                onClick={this.handleExportDiagnostics}
                className="px-4 py-2 rounded-xl text-xs font-bold font-mono bg-cyan-950/40 hover:bg-cyan-900/60 text-cyan-300 border border-cyan-500/40 hover:border-cyan-400 transition-all flex items-center gap-1.5 min-h-[38px] shadow-[0_0_12px_rgba(6,182,212,0.15)]"
                title="Stáhnout kompletní stav a diagnostický JSON report"
              >
                <Download className="w-3.5 h-3.5 text-cyan-400" />
                <span>{this.state.exported ? "DIAGNOSTIKA STÁŽENA" : "EXPORTOVAT DIAGNOSTIKU"}</span>
              </button>

              <div className="flex items-center gap-2">
                <button
                  onClick={this.handleReset}
                  className="px-3.5 py-2 rounded-xl text-xs font-bold font-mono bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700/80 transition-all flex items-center gap-1.5 min-h-[38px]"
                >
                  <Home className="w-3.5 h-3.5 text-slate-400" />
                  <span>OBNOVIT</span>
                </button>
                <button
                  onClick={this.handleReload}
                  className="px-4 py-2 rounded-xl text-xs font-bold font-mono bg-gradient-to-r from-rose-600 to-amber-600 hover:from-rose-500 hover:to-amber-500 text-white transition-all flex items-center gap-1.5 min-h-[38px] shadow-[0_0_15px_rgba(244,63,94,0.25)]"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>RESTART</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
