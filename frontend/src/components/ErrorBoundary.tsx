import React, { Component, ErrorInfo, ReactNode } from "react";
import { AlertTriangle, RefreshCw, Home } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error in O.M.N.I.S. UI:", error, errorInfo);
    this.setState({ errorInfo });
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  private handleReload = () => {
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen bg-[#050810] text-slate-200 flex items-center justify-center p-4 font-sans selection:bg-[#00F0FF]/30">
          <div className="max-w-lg w-full bg-[#0A0F1D]/90 backdrop-blur-md border border-rose-500/40 rounded-2xl p-6 shadow-[0_0_30px_rgba(244,63,94,0.15)] relative overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-rose-500 via-amber-500 to-rose-500" />
            
            <div className="flex items-start gap-4 mb-4">
              <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800 text-rose-400 flex-shrink-0">
                <AlertTriangle className="w-6 h-6 animate-pulse" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-100 font-mono tracking-wide">
                  SYSTEMIC EXCEPTION DETECTED
                </h2>
                <p className="text-xs text-rose-400 font-mono mt-0.5">
                  An unexpected UI error occurred. Active state protected.
                </p>
              </div>
            </div>

            <div className="bg-[#050811] rounded-xl p-3 border border-slate-800/80 mb-5 font-mono text-xs overflow-x-auto max-h-40 text-slate-400 scrollbar-thin">
              <p className="text-rose-300 font-semibold mb-1">
                {this.state.error?.name}: {this.state.error?.message || "Unknown rendering exception"}
              </p>
              {this.state.errorInfo?.componentStack && (
                <pre className="text-[10px] text-slate-500 whitespace-pre-wrap leading-relaxed">
                  {this.state.errorInfo.componentStack}
                </pre>
              )}
            </div>

            <div className="flex flex-wrap items-center justify-end gap-2.5">
              <button
                onClick={this.handleReset}
                className="px-4 py-2 rounded-xl text-xs font-bold font-mono bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700/80 transition-all flex items-center gap-1.5 min-h-[38px]"
              >
                <Home className="w-3.5 h-3.5 text-[#00F0FF]" />
                <span>OBNOVIT POHLED</span>
              </button>
              <button
                onClick={this.handleReload}
                className="px-4 py-2 rounded-xl text-xs font-bold font-mono bg-gradient-to-r from-rose-600 to-amber-600 hover:from-rose-500 hover:to-amber-500 text-white font-bold transition-all flex items-center gap-1.5 min-h-[38px] shadow-[0_0_15px_rgba(244,63,94,0.2)]"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>RESTART APLIKACE</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
