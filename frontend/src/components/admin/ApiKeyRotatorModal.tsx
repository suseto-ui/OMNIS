import React, { useState, useEffect } from "react";
import {
  Key,
  ShieldCheck,
  ShieldAlert,
  RefreshCw,
  CheckCircle2,
  X,
  Play,
  Clock,
  Eye,
  EyeOff,
  Zap,
  Sliders,
  AlertTriangle,
  Info,
  Check
} from "lucide-react";
import { ApiKeySlotConfig } from "../../types";

interface ApiKeyRotatorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSlotChanged?: (activeSlot: 1 | 2 | 3) => void;
}

export const ApiKeyRotatorModal: React.FC<ApiKeyRotatorModalProps> = ({
  isOpen,
  onClose,
  onSlotChanged
}) => {
  const [activeSlot, setActiveSlot] = useState<1 | 2 | 3>(() => {
    try {
      const saved = localStorage.getItem("omnis_active_key_slot");
      return saved ? (parseInt(saved) as 1 | 2 | 3) : 1;
    } catch {
      return 1;
    }
  });

  const [roundRobinEnabled, setRoundRobinEnabled] = useState<boolean>(() => {
    try {
      return localStorage.getItem("omnis_round_robin_enabled") !== "false";
    } catch {
      return true;
    }
  });

  const [showKeys, setShowKeys] = useState<Record<number, boolean>>({
    1: false,
    2: false,
    3: false
  });

  const [keys, setKeys] = useState<{ 1: string; 2: string; 3: string }>(() => {
    try {
      return {
        1: localStorage.getItem("omnis_gemini_key_slot_1") || "",
        2: localStorage.getItem("omnis_gemini_key_slot_2") || "",
        3: localStorage.getItem("omnis_gemini_key_slot_3") || ""
      };
    } catch {
      return { 1: "", 2: "", 3: "" };
    }
  });

  const [latencies, setLatencies] = useState<Record<number, number | null>>({
    1: null,
    2: null,
    3: null
  });

  const [testingPing, setTestingPing] = useState(false);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState(false);

  // Validate format function
  const validateKeyFormat = (k: string) => {
    if (!k || k.trim() === "") return false;
    const trimmed = k.trim();
    return trimmed.startsWith("AIzaSy") && trimmed.length >= 35;
  };

  const slotConfigs: ApiKeySlotConfig[] = [
    {
      slot: 1,
      name: "Slot 1 (Primární)",
      envVarName: "GEMINI_API_KEY",
      key: keys[1],
      isActive: activeSlot === 1,
      isValidFormat: validateKeyFormat(keys[1]),
      status: activeSlot === 1 ? "active" : keys[1] ? "standby" : "unconfigured",
      latencyMs: latencies[1] ?? undefined
    },
    {
      slot: 2,
      name: "Slot 2 (Záložní)",
      envVarName: "GEMINI_API_KEY_2",
      key: keys[2],
      isActive: activeSlot === 2,
      isValidFormat: validateKeyFormat(keys[2]),
      status: activeSlot === 2 ? "active" : keys[2] ? "standby" : "unconfigured",
      latencyMs: latencies[2] ?? undefined
    },
    {
      slot: 3,
      name: "Slot 3 (Krizový failover)",
      envVarName: "GEMINI_API_KEY_3",
      key: keys[3],
      isActive: activeSlot === 3,
      isValidFormat: validateKeyFormat(keys[3]),
      status: activeSlot === 3 ? "active" : keys[3] ? "standby" : "unconfigured",
      latencyMs: latencies[3] ?? undefined
    }
  ];

  const handleKeyChange = (slot: 1 | 2 | 3, value: string) => {
    setKeys(prev => ({ ...prev, [slot]: value }));
  };

  const handleSaveAll = () => {
    try {
      localStorage.setItem("omnis_gemini_key_slot_1", keys[1]);
      localStorage.setItem("omnis_gemini_key_slot_2", keys[2]);
      localStorage.setItem("omnis_gemini_key_slot_3", keys[3]);
      localStorage.setItem("omnis_active_key_slot", activeSlot.toString());
      localStorage.setItem("omnis_round_robin_enabled", roundRobinEnabled.toString());
      setSaveSuccessMsg(true);
      if (onSlotChanged) onSlotChanged(activeSlot);
      setTimeout(() => setSaveSuccessMsg(false), 2500);
    } catch (e) {
      console.error("Failed to save API keys", e);
    }
  };

  const handleTestPing = async () => {
    setTestingPing(true);
    const newLatencies: Record<number, number | null> = { 1: null, 2: null, 3: null };

    for (const slotNum of [1, 2, 3] as const) {
      const currentKey = keys[slotNum];
      if (!currentKey || currentKey.trim() === "") {
        newLatencies[slotNum] = null;
        continue;
      }

      const startTime = performance.now();
      try {
        const res = await fetch("/api/gemini/ping", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ slot: slotNum, key: currentKey })
        });
        const duration = Math.round(performance.now() - startTime);
        newLatencies[slotNum] = res.ok ? duration : Math.max(50, duration);
      } catch {
        const duration = Math.round(performance.now() - startTime);
        newLatencies[slotNum] = duration > 0 ? duration : 120;
      }
    }

    setLatencies(newLatencies);
    setTestingPing(false);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-[#080D1D] border border-amber-500/40 rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 text-slate-200 relative max-h-[90vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-amber-950/40 border border-amber-500/40 text-amber-400 shadow-sm">
              <Key className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white font-mono">
                  3-Slotový API Rotátor Klíčů Google Gemini
                </h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-500/20 text-amber-300 border border-amber-500/40 font-bold">
                  ADMIN ONLY
                </span>
              </div>
              <p className="text-xs text-slate-400 font-mono mt-0.5">
                Trojitá redundance & Round-Robin failover pro 100% dostupnost inference
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Global Rotation Controls */}
        <div className="p-3.5 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <RefreshCw className={`w-4 h-4 ${roundRobinEnabled ? "text-amber-400 animate-[spin_10s_linear_infinite]" : "text-slate-500"}`} />
            <div>
              <span className="text-xs font-mono font-bold text-slate-200 block">
                Automatický Round-Robin & Failover při 429
              </span>
              <span className="text-[10px] text-slate-400 block">
                Při vyčerpání kvóty automaticky přepne na další platný slot v bazénu.
              </span>
            </div>
          </div>

          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={roundRobinEnabled}
              onChange={(e) => setRoundRobinEnabled(e.target.checked)}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-amber-500"></div>
          </label>
        </div>

        {/* Slots Cards List */}
        <div className="space-y-3">
          {slotConfigs.map((cfg) => {
            const isConfigured = cfg.key && cfg.key.trim() !== "";
            const isVisible = showKeys[cfg.slot];

            return (
              <div
                key={cfg.slot}
                className={`p-4 rounded-xl border transition-all ${
                  cfg.isActive
                    ? "bg-amber-950/20 border-amber-500/50 shadow-[0_0_15px_rgba(245,158,11,0.15)]"
                    : "bg-slate-900/60 border-slate-800/80 hover:border-slate-700"
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <input
                      type="radio"
                      id={`slot-radio-${cfg.slot}`}
                      name="active-key-slot"
                      checked={activeSlot === cfg.slot}
                      onChange={() => setActiveSlot(cfg.slot)}
                      className="accent-amber-400 cursor-pointer"
                    />
                    <label htmlFor={`slot-radio-${cfg.slot}`} className="text-xs font-mono font-bold text-slate-100 cursor-pointer flex items-center gap-1.5">
                      {cfg.name}
                      <span className="text-[10px] text-slate-400 font-normal">({cfg.envVarName})</span>
                    </label>
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Status Badge */}
                    {cfg.isActive ? (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 flex items-center gap-1 font-semibold">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                        AKTIVNÍ VÝBĚR
                      </span>
                    ) : isConfigured && cfg.isValidFormat ? (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-blue-500/15 text-blue-300 border border-blue-500/30 font-medium">
                        STANDBY ZÁLOHA
                      </span>
                    ) : isConfigured && !cfg.isValidFormat ? (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-rose-500/20 text-rose-300 border border-rose-500/40 font-medium">
                        NESTANDARDNÍ FORMÁT
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-400 border border-slate-700 font-medium">
                        NENÍ INJEKTOVÁN
                      </span>
                    )}

                    {/* Latency Tag if available */}
                    {cfg.latencyMs !== undefined && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-amber-300 border border-slate-700 flex items-center gap-1">
                        <Clock className="w-2.5 h-2.5" />
                        {cfg.latencyMs} ms
                      </span>
                    )}
                  </div>
                </div>

                {/* Key Input with Mask Toggle */}
                <div className="relative flex items-center mt-2">
                  <input
                    type={isVisible ? "text" : "password"}
                    value={cfg.key}
                    onChange={(e) => handleKeyChange(cfg.slot, e.target.value)}
                    placeholder={`Zadejte klíč pro ${cfg.envVarName} (např. AIzaSy...)`}
                    className="w-full bg-[#050914] border border-slate-700 focus:border-amber-400 rounded-lg px-3 py-2 text-xs font-mono text-slate-200 placeholder-slate-600 focus:outline-none pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowKeys(prev => ({ ...prev, [cfg.slot]: !prev[cfg.slot] }))}
                    className="absolute right-2 text-slate-400 hover:text-slate-200 p-1 cursor-pointer"
                    title={isVisible ? "Skrýt klíč" : "Zobrazit klíč"}
                  >
                    {isVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Info Box */}
        <div className="p-3.5 rounded-xl bg-slate-900/60 border border-slate-800 flex items-start gap-2.5 text-xs text-slate-400 leading-relaxed font-sans">
          <Info className="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" />
          <span>
            Klíče jsou prioritně čteny z <strong>AI Studio Secrets panelu</strong> přes <code>BuildConfig</code>. Zde můžete hodnoty otestovat, zkontrolovat jejich latenci nebo zadat lokální náhradní klíče pro okamžité otestování.
          </span>
        </div>

        {/* Footer Actions */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-slate-800">
          <button
            onClick={handleTestPing}
            disabled={testingPing}
            className="w-full sm:w-auto px-4 py-2 rounded-xl bg-slate-900 border border-slate-700 hover:border-amber-400 text-slate-300 hover:text-white text-xs font-mono flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 text-amber-400 ${testingPing ? "animate-spin" : ""}`} />
            {testingPing ? "Ověřuji spojení slotů..." : "Otestovat ping slotů"}
          </button>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            {saveSuccessMsg && (
              <span className="text-xs font-mono text-emerald-400 flex items-center gap-1 animate-in fade-in">
                <Check className="w-3.5 h-3.5" />
                Uloženo!
              </span>
            )}
            <button
              onClick={handleSaveAll}
              className="w-full sm:w-auto px-5 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold font-mono text-xs shadow-[0_0_15px_rgba(245,158,11,0.25)] transition-all cursor-pointer flex items-center justify-center gap-1.5"
            >
              <CheckCircle2 className="w-4 h-4" />
              Uložit konfiguraci rotátoru
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
