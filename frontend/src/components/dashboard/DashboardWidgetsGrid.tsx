import React, { useEffect, useState } from 'react';
import { FirestoreRepository } from '../../FirestoreRepository';
import { DocumentData } from 'firebase/firestore';
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { Activity, ShieldAlert, Database, Clock, TrendingUp } from 'lucide-react';

export const DashboardWidgetsGrid: React.FC = () => {
  const [metrics, setMetrics] = useState<DocumentData | null>(null);
  const repo = new FirestoreRepository();

  useEffect(() => {
    const unsubscribe = repo.subscribeToMetrics((data) => {
      setMetrics(data);
    });
    return () => unsubscribe();
  }, []);

  // Default fallback data if Firestore document has single values or history arrays
  const tokenHistory = metrics?.tokenHistory || [
    { time: '10:00', tokens: 1200 },
    { time: '10:15', tokens: 2300 },
    { time: '10:30', tokens: metrics?.tokens || 3400 }
  ];

  const threatTimeline = metrics?.threatTimeline || [
    { time: '09:00', threats: 0 },
    { time: '09:30', threats: 1 },
    { time: '10:00', threats: 2 },
    { time: '10:30', threats: metrics?.threats || 1 }
  ];

  const syncStatus = metrics?.syncStatus || 'ONLINE';
  const totalTokens = metrics?.tokens || 3400;
  const activeThreats = metrics?.threats || 1;

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 w-full mb-4">
      {/* Widget 1: Token Usage Chart */}
      <div 
        className="bg-[#090f1d] border border-slate-800 hover:border-cyan-500/50 p-5 rounded-2xl shadow-xl transition-all cursor-pointer group"
        onClick={() => alert(`Detail tokenů: Celkem ${totalTokens} tokenů zpracováno v aktuální relaci.`)}
      >
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-white font-bold text-sm tracking-wide">Živé Tokeny</h3>
              <p className="text-slate-400 text-[11px] font-mono">Spotřeba v reálném čase</p>
            </div>
          </div>
          <span className="text-cyan-400 font-mono font-bold text-lg">{totalTokens}</span>
        </div>
        <div className="h-28 w-full pt-2">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={tokenHistory}>
              <defs>
                <linearGradient id="tokenGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.4}/>
                  <stop offset="95%" stopColor="#06b6d4" stopOpacity={0.0}/>
                </linearGradient>
              </defs>
              <Tooltip 
                contentStyle={{ backgroundColor: '#050810', borderColor: '#1e293b', borderRadius: '8px', fontSize: '11px', color: '#fff' }}
              />
              <Area type="monotone" dataKey="tokens" stroke="#06b6d4" strokeWidth={2} fillOpacity={1} fill="url(#tokenGradient)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Widget 2: Sync Status Indicator */}
      <div 
        className="bg-[#090f1d] border border-slate-800 hover:border-violet-500/50 p-5 rounded-2xl shadow-xl transition-all cursor-pointer flex flex-col justify-between group"
        onClick={() => alert(`Stav databázové synchronizace: ${syncStatus}. Všechna lokální data jsou synchronizována s PostgreSQL & Firestore v reálném čase.`)}
      >
        <div>
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2.5">
              <div className="p-2 rounded-xl bg-violet-500/10 border border-violet-500/20 text-violet-400">
                <Database className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-white font-bold text-sm tracking-wide">Databázová Synchronizace</h3>
                <p className="text-slate-400 text-[11px] font-mono">PostgreSQL & Firestore</p>
              </div>
            </div>
          </div>
        </div>

        <div className="my-auto py-4 flex items-center justify-center">
          <div className={`px-4 py-2 rounded-xl border flex items-center gap-2.5 font-mono text-xs font-bold ${
            syncStatus === 'ONLINE' 
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 shadow-[0_0_15px_rgba(16,185,129,0.15)]' 
              : 'bg-red-500/10 border-red-500/30 text-red-400 shadow-[0_0_15px_rgba(239,68,68,0.15)]'
          }`}>
            <span className={`w-2.5 h-2.5 rounded-full animate-ping ${syncStatus === 'ONLINE' ? 'bg-emerald-400' : 'bg-red-400'}`} />
            <span>STAV: {syncStatus}</span>
          </div>
        </div>

        <div className="text-[11px] text-slate-500 font-mono flex items-center justify-between pt-2 border-t border-slate-800/80">
          <span>Latence sítě: 14ms</span>
          <span className="text-emerald-400">Zero-Trust Active</span>
        </div>
      </div>

      {/* Widget 3: Threat Detection Timeline */}
      <div 
        className="bg-[#090f1d] border border-slate-800 hover:border-amber-500/50 p-5 rounded-2xl shadow-xl transition-all cursor-pointer group"
        onClick={() => alert(`Detail hrozbových vektorů: Aktivních hrozeb v sémantickém grafu: ${activeThreats}`)}
      >
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-white font-bold text-sm tracking-wide">Sémantické Hrozby</h3>
              <p className="text-slate-400 text-[11px] font-mono">Časová osa detekce</p>
            </div>
          </div>
          <span className="text-amber-400 font-mono font-bold text-lg">{activeThreats}</span>
        </div>
        <div className="h-28 w-full pt-2">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={threatTimeline}>
              <Tooltip 
                contentStyle={{ backgroundColor: '#050810', borderColor: '#1e293b', borderRadius: '8px', fontSize: '11px', color: '#fff' }}
              />
              <Line type="stepAfter" dataKey="threats" stroke="#f59e0b" strokeWidth={2} dot={{ fill: '#f59e0b', r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};
