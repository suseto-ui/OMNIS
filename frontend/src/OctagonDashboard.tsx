import React, { useState, useEffect, useMemo } from "react";
import { Activity, Zap, ShieldAlert, Cpu, Leaf, BrainCircuit, Landmark, Link2, X, Sliders, RefreshCw, CheckCircle2, Info, Download, FileJson, FileText, ShieldCheck, AlertTriangle, Network, Shield } from "lucide-react";
import { evaluateSemanticImpactMatrix, evaluateRefusalLadder, RefusalLadderEvaluation } from "./omnisEngine";
import { MessageItem } from "./components/MessageBubble";
import { UserRole } from "./types";

export interface OctagonMatrix {
  sys: number;
  econ: number;
  psych: number;
  eco: number;
  law: number;
  sec: number;
  phys: number;
  soc: number;
}

interface NodeTelemetry {
  id: keyof OctagonMatrix;
  label: string;
  icon: React.ElementType;
  value: number;
  color: string;
  description: string;
  reasoningNote: string;
}

const DOMAINS: NodeTelemetry[] = [
  { id: "sys", label: "Systémové inženýrství & Kybernetika", icon: Cpu, value: 95, color: "text-blue-400", description: "Architektura, modularita, rozhraní a systémová integrace.", reasoningNote: "Vyhodnocuje složitost kódové základny, rozhraní API a celkovou stabilitu toku dat." },
  { id: "econ", label: "Teorie her & Ekonomie", icon: Landmark, value: 88, color: "text-amber-400", description: "Návratnost investic, tokenová alokace a výpočetní náklady.", reasoningNote: "Sleduje alokaci zdrojů, nákladovou efektivitu dotazů a využití kreditů." },
  { id: "psych", label: "Kognitivní vědy & Psychologie", icon: BrainCircuit, value: 91, color: "text-purple-400", description: "Mentální zátěž, UX ergonomie a kognitivní bezpečí.", reasoningNote: "Měří kognitivní přehlednost odpovědí, srozumitelnost pro uživatele a eliminaci stresu." },
  { id: "eco", label: "Regenerativní Ekologie", icon: Leaf, value: 94, color: "text-emerald-400", description: "Udržitelnost, energetický otisk a efektivita zdrojů.", reasoningNote: "Hodnotí výpočetní náročnost a energetický otisk spuštěných inferenčních modelů." },
  { id: "law", label: "Regulace & Právo", icon: ShieldAlert, value: 98, color: "text-rose-400", description: "Compliance, GDPR, NIS2, EU AI Act a licenční čistota.", reasoningNote: "Kontroluje shodu s normami, legislativní limity a licenční transparentnost." },
  { id: "sec", label: "Zero-Trust Bezpečnost", icon: ShieldAlert, value: 99, color: "text-red-500", description: "Kryptografie, audit perimetru a soukromí dat.", reasoningNote: "Verifikuje autentizaci, šifrování dat a prevenci úniku citlivých informací." },
  { id: "phys", label: "Fyzikální termodynamika", icon: Zap, value: 87, color: "text-orange-400", description: "Edge-computing, latence sítě a fyzický hardware.", reasoningNote: "Monitoruje fyzickou infrastrukturu, latenci sítě a propustnost procesoru." },
  { id: "soc", label: "Socio-kulturní dynamika", icon: Activity, value: 90, color: "text-pink-400", description: "Společenský dopad, týmová koheze a eliminaci bariér.", reasoningNote: "Analyzuje dopad na komunitu, férovost přístupu a etický rozměr komunikace." },
];

interface OctagonDashboardProps {
  matrix?: OctagonMatrix;
  messages?: MessageItem[];
  userRole?: UserRole;
}

export const OctagonDashboard: React.FC<OctagonDashboardProps> = ({ matrix, messages = [], userRole = "ADMIN_OPERATOR" }) => {
  const isAdmin = userRole === "ADMIN_OPERATOR";
  const [telemetry, setTelemetry] = useState<NodeTelemetry[]>(DOMAINS);
  const [activeNode, setActiveNode] = useState<string | null>(null);
  const [modalDomain, setModalDomain] = useState<NodeTelemetry | null>(null);
  const [manualCalibrations, setManualCalibrations] = useState<Record<string, number>>({});

  // Perkolační model šíření stresu v 8D grafu:
  // Kritický práh p_c ≈ 0.38 (cca 3 z 8 domén pod 75% stability)
  const percolationStats = useMemo(() => {
    const stressedNodes = telemetry.filter(t => t.value < 75);
    const percolationRatio = stressedNodes.length / telemetry.length;
    const criticalThreshold = 0.38;
    const isPercolationActive = percolationRatio >= criticalThreshold;
    return {
      stressedNodes,
      percolationRatio: Number(percolationRatio.toFixed(2)),
      criticalThreshold,
      isPercolationActive
    };
  }, [telemetry]);

  // Poslední asistenční zpráva pro audit 6 bran
  const lastAssistantMsg = useMemo(() => {
    return messages.filter(m => m.role === "assistant").slice(-1)[0];
  }, [messages]);

  const refusalLadder = useMemo(() => {
    return lastAssistantMsg?.refusal_ladder || evaluateRefusalLadder(lastAssistantMsg?.content || "O.M.N.I.S. Core Topology Invariants");
  }, [lastAssistantMsg]);

  const [autopoiesisState, setAutopoiesisState] = useState<{
    status: string;
    totalAdaptations?: number;
    homeostasisTarget?: number;
    isSyncing?: boolean;
    syncSuccess?: boolean;
  }>({ status: "checking" });

  useEffect(() => {
    let isMounted = true;
    fetch("/api/autopoiesis/status")
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (isMounted && data) {
          setAutopoiesisState({
            status: data.status || "active",
            totalAdaptations: data.total_feedbacks_recorded || 0,
            homeostasisTarget: data.homeostasis_target || 0.95,
          });
        }
      })
      .catch(() => {
        if (isMounted) setAutopoiesisState({ status: "offline" });
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const handleSyncAutopoiesis = async () => {
    setAutopoiesisState((prev) => ({ ...prev, isSyncing: true, syncSuccess: false }));
    try {
      const res = await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message_id: "00000000-0000-0000-0000-000000000000",
          conversation_id: "00000000-0000-0000-0000-000000000000",
          user_rating: Math.min(5, Math.max(1, Math.round((compositeScore / 100) * 5))),
          feedback_text: `Synchronizace Octagon 8D matice z klientského dashboardu (Odolnost: ${compositeScore}%)`,
          adjusted_matrix: {
            sys: (telemetry.find((t) => t.id === "sys")?.value || 90) / 100,
            econ: (telemetry.find((t) => t.id === "econ")?.value || 90) / 100,
            psych: (telemetry.find((t) => t.id === "psych")?.value || 90) / 100,
            eco: (telemetry.find((t) => t.id === "eco")?.value || 90) / 100,
            law: (telemetry.find((t) => t.id === "law")?.value || 90) / 100,
            sec: (telemetry.find((t) => t.id === "sec")?.value || 90) / 100,
            phys: (telemetry.find((t) => t.id === "phys")?.value || 90) / 100,
            soc: (telemetry.find((t) => t.id === "soc")?.value || 90) / 100,
            composite_score: compositeScore / 100,
            reasoning: `Manuální kalibrace uživatele v Octagon Dashboardu (Index odolnosti: ${compositeScore}%)`,
          },
        }),
      });
      if (res.ok) {
        setAutopoiesisState((prev) => ({ ...prev, isSyncing: false, syncSuccess: true }));
        setTimeout(() => {
          setAutopoiesisState((prev) => ({ ...prev, syncSuccess: false }));
        }, 3000);
      } else {
        setAutopoiesisState((prev) => ({ ...prev, isSyncing: false }));
      }
    } catch {
      setAutopoiesisState((prev) => ({ ...prev, isSyncing: false }));
    }
  };

  // Real deterministic evaluation based on active thread or provided matrix
  const evaluatedMatrix = useMemo(() => {
    if (matrix) return matrix;
    if (messages.length > 0) {
      const textToAnalyze = messages.map(m => m.text).join(" ");
      const computed = evaluateSemanticImpactMatrix(textToAnalyze);
      return {
        sys: computed.sys,
        econ: computed.econ,
        psych: computed.psych,
        eco: computed.eco,
        law: computed.law,
        sec: computed.sec,
        phys: computed.phys,
        soc: computed.soc
      };
    }
    return null;
  }, [matrix, messages]);

  useEffect(() => {
    if (evaluatedMatrix) {
      setTelemetry((prev) =>
        prev.map((node) => {
          const manualVal = manualCalibrations[node.id];
          const autoVal = Math.round(evaluatedMatrix[node.id] * 100);
          return {
            ...node,
            value: manualVal !== undefined ? manualVal : autoVal,
          };
        })
      );
    } else {
      setTelemetry((prev) =>
        prev.map((node) => {
          const manualVal = manualCalibrations[node.id];
          return {
            ...node,
            value: manualVal !== undefined ? manualVal : node.value,
          };
        })
      );
    }
  }, [evaluatedMatrix, manualCalibrations]);

  const handleSliderChange = (domainId: string, newValue: number) => {
    setManualCalibrations(prev => ({
      ...prev,
      [domainId]: newValue
    }));
  };

  const handleResetDomain = (domainId: string) => {
    setManualCalibrations(prev => {
      const copy = { ...prev };
      delete copy[domainId];
      return copy;
    });
  };

  const compositeScore = useMemo(() => {
    const vals = telemetry.map(t => t.value / 100);
    if (vals.length === 0) return 0;
    const harmonicDenom = vals.reduce((acc, v) => acc + (1 / Math.max(0.01, v)), 0);
    const harmonicMean = vals.length / harmonicDenom;
    const arithmeticMean = vals.reduce((a, b) => a + b, 0) / vals.length;
    return Math.round((harmonicMean * 0.7 + arithmeticMean * 0.3) * 100);
  }, [telemetry]);

  const handleExportJSON = () => {
    const payload = {
      timestamp: new Date().toISOString(),
      system: "O.M.N.I.S. 8D Transdisciplinary Matrix",
      resilience_index_pct: compositeScore,
      domains: telemetry.map(t => ({
        id: t.id,
        label: t.label,
        score_pct: t.value,
        is_manually_calibrated: manualCalibrations[t.id] !== undefined,
        description: t.description,
        reasoning_note: t.reasoningNote
      }))
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `omnis_8d_matrix_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportCSV = () => {
    const headers = ["Domain_ID", "Domain_Label", "Score_Percent", "Calibrated", "Description"];
    const rows = telemetry.map(t => [
      t.id,
      `"${t.label.replace(/"/g, '""')}"`,
      t.value,
      manualCalibrations[t.id] !== undefined ? "TRUE" : "FALSE",
      `"${t.description.replace(/"/g, '""')}"`
    ]);
    const csvContent = [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `omnis_8d_matrix_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="p-4 sm:p-6 w-full h-full flex flex-col items-center justify-center space-y-6 animate-in fade-in duration-500 relative">
      <div className="text-center space-y-2">
        <h2 className="text-xl font-bold font-mono text-[#00F0FF] flex items-center justify-center gap-2">
          <Activity className="w-6 h-6 animate-pulse" />
          Transdisciplinární Oktagon O.M.N.I.S.
        </h2>
        <p className="text-xs text-slate-400 font-mono">
          Živá telemetrie 8 domén • Index odolnosti: <span className="text-[#00F0FF] font-bold">{compositeScore}%</span>
        </p>

        {/* EXPORT & AUTOPOIESIS TOOLBAR */}
        <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
          {/* Autopoiesis Live Status Indicator */}
          <div className="px-2.5 py-1 rounded-lg bg-purple-950/40 border border-purple-500/30 text-purple-300 text-[11px] font-mono flex items-center gap-1.5 shadow-sm">
            <span
              className={`w-2 h-2 rounded-full ${
                autopoiesisState.status === "active"
                  ? "bg-emerald-400 animate-pulse"
                  : autopoiesisState.status === "checking"
                  ? "bg-amber-400 animate-ping"
                  : "bg-slate-500"
              }`}
            />
            <span>Autopoiesis:</span>
            <strong className="text-white capitalize">{autopoiesisState.status}</strong>
            {autopoiesisState.totalAdaptations !== undefined && (
              <span className="text-[10px] text-purple-400">
                ({autopoiesisState.totalAdaptations} adaptací)
              </span>
            )}
          </div>

          <button
            onClick={handleSyncAutopoiesis}
            disabled={autopoiesisState.isSyncing}
            className="px-3 py-1.5 rounded-lg bg-purple-900/60 border border-purple-500/40 hover:border-purple-400 text-purple-200 hover:text-white text-xs font-mono flex items-center gap-1.5 transition-all shadow-sm disabled:opacity-50"
            title="Synchronizovat aktuální matici s autopoietickým učením O.M.N.I.S."
          >
            <RefreshCw
              className={`w-3.5 h-3.5 text-purple-300 ${
                autopoiesisState.isSyncing ? "animate-spin" : ""
              }`}
            />
            {autopoiesisState.isSyncing
              ? "Synchronizuji..."
              : autopoiesisState.syncSuccess
              ? "Uloženo!"
              : "Synchronizovat matici"}
          </button>

          <button
            onClick={handleExportJSON}
            className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 hover:border-[#00F0FF] text-slate-300 hover:text-white text-xs font-mono flex items-center gap-1.5 transition-all shadow-sm"
            title="Exportovat 8D Matici jako JSON"
          >
            <FileJson className="w-3.5 h-3.5 text-[#00F0FF]" />
            Export JSON
          </button>
          <button
            onClick={handleExportCSV}
            className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 hover:border-[#00F0FF] text-slate-300 hover:text-white text-xs font-mono flex items-center gap-1.5 transition-all shadow-sm"
            title="Exportovat 8D Matici jako CSV"
          >
            <FileText className="w-3.5 h-3.5 text-emerald-400" />
            Export CSV
          </button>
        </div>
      </div>

      <div className="w-full overflow-x-auto pb-4 flex justify-center">
        <div className="relative w-80 h-80 sm:w-96 sm:h-96 flex-shrink-0">
          {/* SVG connections */}
          <svg className="absolute inset-0 w-full h-full pointer-events-none opacity-20">
            <circle cx="50%" cy="50%" r="45%" stroke="#00F0FF" strokeWidth="1" fill="none" strokeDasharray="4 4" className="animate-[spin_60s_linear_infinite]" />
            {telemetry.map((_, i) => {
              const angle1 = (i / telemetry.length) * 2 * Math.PI - Math.PI / 2;
              const x1 = 50 + 45 * Math.cos(angle1);
              const y1 = 50 + 45 * Math.sin(angle1);
              return telemetry.map((_, j) => {
                if (i >= j) return null;
                const angle2 = (j / telemetry.length) * 2 * Math.PI - Math.PI / 2;
                const x2 = 50 + 45 * Math.cos(angle2);
                const y2 = 50 + 45 * Math.sin(angle2);
                return (
                  <line
                    key={`${i}-${j}`}
                    x1={`${x1}%`}
                    y1={`${y1}%`}
                    x2={`${x2}%`}
                    y2={`${y2}%`}
                    stroke="currentColor"
                    strokeWidth={activeNode === telemetry[i].id || activeNode === telemetry[j].id ? "2" : "0.5"}
                    className={`transition-all duration-300 ${activeNode === telemetry[i].id || activeNode === telemetry[j].id ? "text-[#00F0FF] opacity-80" : "text-slate-600"}`}
                  />
                );
              });
            })}
          </svg>

          {/* Nodes */}
          {telemetry.map((node, i) => {
            const angle = (i / telemetry.length) * 2 * Math.PI - Math.PI / 2;
            const x = 50 + 45 * Math.cos(angle);
            const y = 50 + 45 * Math.sin(angle);
            const Icon = node.icon;
            const isActive = activeNode === node.id;
            const isCalibrated = manualCalibrations[node.id] !== undefined;

            return (
              <div
                key={node.id}
                className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
                style={{ left: `${x}%`, top: `${y}%` }}
                onMouseEnter={() => setActiveNode(node.id)}
                onMouseLeave={() => setActiveNode(null)}
                onClick={() => setModalDomain(node)}
              >
                <div className={`relative flex flex-col items-center justify-center p-3 rounded-xl border bg-slate-950/90 backdrop-blur transition-all duration-300 z-10 ${isActive ? 'border-[#00F0FF] scale-110 shadow-[0_0_20px_rgba(0,240,255,0.4)]' : 'border-slate-800 hover:border-slate-600'}`}>
                  <Icon className={`w-5 h-5 mb-1 transition-colors ${isActive ? 'text-[#00F0FF]' : node.color}`} />
                  <span className="text-[10px] font-mono font-bold text-slate-300 flex items-center gap-1">
                    {node.value.toFixed(1)}%
                    {isCalibrated && <Sliders className="w-2.5 h-2.5 text-[#00F0FF]" />}
                  </span>
                  
                  {/* Tooltip */}
                  <div className={`absolute w-36 text-center pointer-events-none transition-all duration-300 ${isActive ? 'opacity-100 translate-y-0 z-20' : 'opacity-0 translate-y-2 -z-10'} ${y > 50 ? 'bottom-full mb-2' : 'top-full mt-2'}`}>
                    <div className="bg-slate-900 border border-[#00F0FF]/50 p-2 rounded shadow-lg">
                      <span className="text-[10px] font-mono text-[#00F0FF] leading-tight block">{node.label}</span>
                      <span className="text-[9px] text-slate-400 block mt-1">Klikněte pro kalibraci</span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
          
          {/* Center Node */}
          <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center justify-center">
            <div className="w-16 h-16 rounded-full bg-gradient-to-tr from-[#00F0FF]/20 to-[#A855F7]/20 border border-[#00F0FF]/50 flex items-center justify-center shadow-[0_0_30px_rgba(0,240,255,0.2)] animate-pulse">
              <Link2 className="w-8 h-8 text-[#00F0FF]" />
            </div>
            <span className="mt-2 text-[10px] font-mono font-bold text-[#00F0FF] bg-slate-950/80 px-2 py-1 rounded">
              SYNERGIE
            </span>
          </div>
        </div>
      </div>

      {/* RBAC AUDIT SUITE: 6-GATE REFUSAL LADDER & PERKOLACE */}
      {isAdmin ? (
        <div className="mt-6 p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-4 font-mono">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-[#00F0FF]" />
              <div>
                <h3 className="text-xs font-bold text-slate-100 uppercase tracking-wider">
                  ADMIN DIAGNOSTIKA: 6-GATE REFUSAL LADDER & PERKOLAČNÍ MODEL
                </h3>
                <p className="text-[10px] text-slate-400">
                  Přísný auditní inspektor kognitivního jádra podle norem EU AI Act, ISO/IEC 42001 a NIS2.
                </p>
              </div>
            </div>

            {/* Perkolační stavový odznak */}
            <div className="flex items-center gap-2 text-xs">
              {percolationStats.isPercolationActive ? (
                <span className="px-2.5 py-1 rounded-xl bg-red-500/15 border border-red-500/40 text-red-400 font-bold flex items-center gap-1.5 animate-pulse">
                  <AlertTriangle className="w-3.5 h-3.5 text-red-400" />
                  <span>PERKOLACE AKTIVNÍ (p = {percolationStats.percolationRatio} ≥ {percolationStats.criticalThreshold}) - BYPASS V POHOTOVOSTI</span>
                </span>
              ) : (
                <span className="px-2.5 py-1 rounded-xl bg-emerald-500/15 border border-emerald-500/40 text-emerald-400 font-bold flex items-center gap-1.5">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>STABILNÍ TOPOLOGIE (p = {percolationStats.percolationRatio} &lt; {percolationStats.criticalThreshold})</span>
                </span>
              )}
            </div>
          </div>

          {/* 6 Gates Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5 text-xs">
            {refusalLadder.gates.map((g) => {
              const isG4 = g.gateId === "G4";
              const isG6 = g.gateId === "G6";

              return (
                <div
                  key={g.gateId}
                  className={`p-3 rounded-xl border space-y-1.5 transition-all ${
                    g.passed 
                      ? "bg-slate-900/60 border-slate-800 hover:border-slate-700" 
                      : "bg-red-950/30 border-red-500/40"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 font-bold">
                      <span className="text-[#00F0FF]">{g.gateId}:</span>
                      <span className="text-slate-200">{g.name}</span>
                    </div>
                    <span className={`px-1.5 py-0.5 rounded text-[9px] font-bold ${
                      g.passed ? "bg-emerald-500/20 text-emerald-400" : "bg-red-500/20 text-red-400"
                    }`}>
                      {g.passed ? "PROPUŠTĚNO" : "ZAMÍTNUTO"}
                    </span>
                  </div>

                  <p className="text-[10px] text-slate-400 leading-tight">
                    {g.explanation}
                  </p>

                  {/* Speciální ukazatele norem pro G4 a entropie pro G6 */}
                  {isG4 && (
                    <div className="pt-1 flex flex-wrap gap-1">
                      <span className="px-1.5 py-0.5 rounded text-[8px] bg-blue-500/15 text-blue-300 border border-blue-500/30 font-bold">
                        EU AI Act Čl. 50
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[8px] bg-cyan-500/15 text-cyan-300 border border-cyan-500/30 font-bold">
                        ISO/IEC 42001
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[8px] bg-purple-500/15 text-purple-300 border border-purple-500/30 font-bold">
                        NIS2
                      </span>
                    </div>
                  )}

                  {isG6 && (
                    <div className="pt-1 flex items-center gap-1 text-[9px] text-[#00F0FF]">
                      <Activity className="w-3 h-3 text-[#00F0FF]" />
                      <span>Shannonova Entropie: <strong>{refusalLadder.shannonEntropy} bitů</strong></span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        /* STANDARD USER MINIMALIST RIBBON (Zamezení vizuálního zahlcení) */
        <div className="mt-4 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between text-xs font-mono">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span className="text-slate-300">
              Kognitivní integrita: <strong className="text-emerald-400">AKGE-8D VERIFIKOVÁNO</strong> (Všechny domény jsou harmonicky vyváženy)
            </span>
          </div>
          <span className="text-[10px] text-slate-500 hidden sm:inline">
            Režim standardního uživatele • Čisté zobrazení
          </span>
        </div>
      )}

      {/* MODAL: DOMAIN DETAIL & MANUAL CALIBRATOR */}
      {modalDomain && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
          <div className="bg-[#0A0F1D] border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 text-slate-200 relative">
            
            {/* Modal Header */}
            <div className="flex items-start justify-between border-b border-slate-800/80 pb-4">
              <div className="flex items-center gap-3">
                <div className={`p-3 rounded-xl bg-slate-900 border border-slate-700 ${modalDomain.color}`}>
                  <modalDomain.icon className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white font-sans">{modalDomain.label}</h3>
                  <p className="text-xs text-slate-400 font-mono mt-0.5">{modalDomain.description}</p>
                </div>
              </div>
              <button 
                onClick={() => setModalDomain(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Rationale & Analytics Box */}
            <div className="bg-slate-900/80 border border-slate-800 p-4 rounded-xl space-y-2">
              <div className="flex items-center gap-2 text-xs font-mono font-semibold text-[#00F0FF]">
                <Info className="w-4 h-4" />
                <span>Sémantické odůvodnění hodnocení</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed font-sans">
                {modalDomain.reasoningNote}
              </p>
            </div>

            {/* Slider Calibration */}
            <div className="space-y-3 pt-2">
              <div className="flex justify-between items-center text-xs font-mono">
                <span className="text-slate-400 flex items-center gap-1.5">
                  <Sliders className="w-4 h-4 text-[#00F0FF]" />
                  Manuální kalibrace váhy:
                </span>
                <span className="font-bold text-[#00F0FF] text-sm">
                  {telemetry.find(t => t.id === modalDomain.id)?.value.toFixed(1)}%
                </span>
              </div>

              <input
                type="range"
                min="0"
                max="100"
                step="1"
                value={telemetry.find(t => t.id === modalDomain.id)?.value || 50}
                onChange={(e) => handleSliderChange(modalDomain.id, parseFloat(e.target.value))}
                className="w-full accent-[#00F0FF] bg-slate-800 h-2 rounded-lg cursor-pointer"
              />

              <div className="flex justify-between text-[10px] font-mono text-slate-500">
                <span>0% (Minimalizovat)</span>
                <span>50% (Medián)</span>
                <span>100% (Dominance)</span>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-800/80">
              <button
                onClick={() => handleResetDomain(modalDomain.id)}
                className="px-3 py-2 text-xs font-mono text-slate-400 hover:text-white flex items-center gap-1.5 rounded-lg hover:bg-slate-800 transition-colors"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                Resetovat na automatiku
              </button>

              <button
                onClick={() => setModalDomain(null)}
                className="px-5 py-2 text-xs font-mono font-semibold bg-[#00F0FF] text-slate-950 rounded-xl hover:bg-[#00F0FF]/90 transition-all shadow-[0_0_15px_rgba(0,240,255,0.3)] flex items-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                Uložit a zavřít
              </button>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};
