import React, { useState, useEffect } from "react";
import { Activity, Zap, ShieldAlert, Cpu, Leaf, BrainCircuit, Landmark, Link2 } from "lucide-react";

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
}

const DOMAINS: NodeTelemetry[] = [
  { id: "sys", label: "Systémové inženýrství & Kybernetika", icon: Cpu, value: 95, color: "text-blue-400" },
  { id: "econ", label: "Teorie her & Ekonomie", icon: Landmark, value: 88, color: "text-amber-400" },
  { id: "psych", label: "Kognitivní vědy & Psychologie", icon: BrainCircuit, value: 91, color: "text-purple-400" },
  { id: "eco", label: "Regenerativní Ekologie", icon: Leaf, value: 94, color: "text-emerald-400" },
  { id: "law", label: "Regulace & Právo", icon: ShieldAlert, value: 98, color: "text-rose-400" },
  { id: "sec", label: "Zero-Trust Bezpečnost", icon: ShieldAlert, value: 99, color: "text-red-500" },
  { id: "phys", label: "Fyzikální termodynamika", icon: Zap, value: 87, color: "text-orange-400" },
  { id: "soc", label: "Socio-kulturní dynamika", icon: Activity, value: 90, color: "text-pink-400" },
];

interface OctagonDashboardProps {
  matrix?: OctagonMatrix;
}

export const OctagonDashboard: React.FC<OctagonDashboardProps> = ({ matrix }) => {
  const [telemetry, setTelemetry] = useState<NodeTelemetry[]>(DOMAINS);
  const [activeNode, setActiveNode] = useState<string | null>(null);

  // Sync with real data or simulate if none
  useEffect(() => {
    if (matrix) {
      setTelemetry((prev) =>
        prev.map((node) => ({
          ...node,
          value: matrix[node.id] * 100, // Matrix values are 0-1, we want 0-100%
        }))
      );
    } else {
      const interval = setInterval(() => {
        setTelemetry((prev) =>
          prev.map((node) => ({
            ...node,
            value: Math.min(100, Math.max(80, node.value + (Math.random() * 4 - 2))),
          }))
        );
      }, 2000);
      return () => clearInterval(interval);
    }
  }, [matrix]);

  return (
    <div className="p-4 sm:p-6 w-full h-full flex flex-col items-center justify-center space-y-8 animate-in fade-in duration-500">
      <div className="text-center space-y-2">
        <h2 className="text-xl font-bold font-mono text-[#00F0FF] flex items-center justify-center gap-2">
          <Activity className="w-6 h-6 animate-pulse" />
          Transdisciplinární Oktagon O.M.N.I.S.
        </h2>
        <p className="text-xs text-slate-400 font-mono">
          Živá telemetrie 8 domén a interaktivní syntéza
        </p>
      </div>

      <div className="relative w-80 h-80 sm:w-96 sm:h-96">
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

          return (
            <div
              key={node.id}
              className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
              style={{ left: `${x}%`, top: `${y}%` }}
              onMouseEnter={() => setActiveNode(node.id)}
              onMouseLeave={() => setActiveNode(null)}
              onClick={() => setActiveNode(node.id)}
            >
              <div className={`relative flex flex-col items-center justify-center p-3 rounded-xl border bg-slate-950/80 backdrop-blur transition-all duration-300 z-10 ${isActive ? 'border-[#00F0FF] scale-110 shadow-[0_0_20px_rgba(0,240,255,0.4)]' : 'border-slate-800 hover:border-slate-600'}`}>
                <Icon className={`w-5 h-5 mb-1 transition-colors ${isActive ? 'text-[#00F0FF]' : node.color}`} />
                <span className="text-[10px] font-mono font-bold text-slate-300">
                  {node.value.toFixed(1)}%
                </span>
                
                {/* Tooltip */}
                <div className={`absolute w-32 text-center pointer-events-none transition-all duration-300 ${isActive ? 'opacity-100 translate-y-0 z-20' : 'opacity-0 translate-y-2 -z-10'} ${y > 50 ? 'bottom-full mb-2' : 'top-full mt-2'}`}>
                  <div className="bg-slate-900 border border-[#00F0FF]/50 p-2 rounded shadow-lg">
                    <span className="text-[10px] font-mono text-[#00F0FF] leading-tight block">{node.label}</span>
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
  );
};
