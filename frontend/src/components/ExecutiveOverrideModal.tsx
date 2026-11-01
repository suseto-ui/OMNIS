import React, { useState } from "react";
import { 
  AlertTriangle, ShieldAlert, Fingerprint, Key, CheckCircle2, X, Lock, FileText, Activity 
} from "lucide-react";

interface ExecutiveOverrideModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirmOverride: (pin: string, reason: string) => void;
  targetQuery: string;
  refusalReason?: string;
}

export const ExecutiveOverrideModal: React.FC<ExecutiveOverrideModalProps> = ({
  isOpen,
  onClose,
  onConfirmOverride,
  targetQuery,
  refusalReason = "Detekován právní či etický drift. Agent Regulátor uvalil veto."
}) => {
  const [pin, setPin] = useState("");
  const [reason, setReason] = useState("Zátěžový test & výzkumná intervence operátora");
  const [authMethod, setAuthMethod] = useState<"pin" | "biometric">("biometric");
  const [biometricScanning, setBiometricScanning] = useState(false);
  const [biometricSuccess, setBiometricSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSimulateBiometric = () => {
    setBiometricScanning(true);
    setErrorMsg(null);
    setTimeout(() => {
      setBiometricScanning(false);
      setBiometricSuccess(true);
    }, 1200);
  };

  const handleExecute = () => {
    if (authMethod === "biometric") {
      if (!biometricSuccess) {
        setErrorMsg("Nejprve proveďte biometrickou autorizaci otiskem prstu.");
        return;
      }
      onConfirmOverride("BIOMETRIC_AUTH_VERIFIED", reason);
      onClose();
    } else {
      if (!pin || pin.length < 4) {
        setErrorMsg("Zadejte platný administrátorský 4-místný PIN (např. 4201).");
        return;
      }
      onConfirmOverride(pin, reason);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-200 font-mono">
      <div className="bg-[#080D1D] border-2 border-red-500/80 rounded-2xl max-w-lg w-full p-6 shadow-[0_0_50px_rgba(239,68,68,0.3)] space-y-5 text-slate-100 relative">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-red-500/30 pb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-red-950/80 border border-red-500/60 text-red-400">
              <ShieldAlert className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white tracking-wider">EXECUTIVE OVERRIDE</h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-500/20 text-red-300 border border-red-500/40">
                  MODIFY_SYSTEM_STATE
                </span>
              </div>
              <p className="text-xs text-red-300/80 mt-0.5 font-sans">
                Vědomé manuální přepsání veta Agenta Regulátora a brány G4.
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Regulatory Veto Notice */}
        <div className="p-3.5 rounded-xl bg-red-950/40 border border-red-500/30 space-y-1.5 text-xs">
          <span className="font-bold text-red-400 flex items-center gap-1.5">
            <AlertTriangle className="w-4 h-4 text-red-400" />
            DŮVOD PŮVODNÍHO VETA REGULÁTORA:
          </span>
          <p className="text-slate-300 font-sans leading-relaxed text-[11px] pl-5">
            {refusalReason}
          </p>
          <div className="text-[10px] text-slate-400 pl-5 font-mono">
            Cílový dotaz: <span className="text-slate-200 italic truncate block">"{targetQuery}"</span>
          </div>
        </div>

        {/* Audit Warning */}
        <div className="p-3 rounded-xl bg-amber-950/25 border border-amber-500/30 text-[11px] text-amber-200/90 leading-relaxed space-y-1">
          <div className="flex items-center gap-1.5 font-bold text-amber-400">
            <Lock className="w-3.5 h-3.5" />
            <span>KRYPTOGRAFICKÝ AUDITNÍ ZÁVAZEK:</span>
          </div>
          <p className="font-sans">
            Tento zásah bude nevratně zapečetěn do Merkle kořene s příznakem <strong className="text-red-400">OVERRIDE_ACTIVE</strong> podle článku 50 EU AI Act. Plnou odpovědnost přebírá operátor.
          </p>
        </div>

        {/* Override Justification */}
        <div className="space-y-1.5">
          <label className="text-xs text-slate-300 font-bold flex items-center gap-1.5">
            <FileText className="w-3.5 h-3.5 text-[#00F0FF]" />
            Odůvodnění manuálního přepsání (do forenzního ledgeru):
          </label>
          <input
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-[#00F0FF]"
            placeholder="Uveďte důvod zásahu..."
          />
        </div>

        {/* 2-Factor Authorization Section */}
        <div className="space-y-3 pt-1">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-300 font-bold">Způsob autorizace operátora:</span>
            <div className="flex gap-1.5">
              <button
                type="button"
                onClick={() => { setAuthMethod("biometric"); setErrorMsg(null); }}
                className={`px-2.5 py-1 rounded-lg text-xs transition-all ${
                  authMethod === "biometric"
                    ? "bg-[#00F0FF]/20 text-[#00F0FF] border border-[#00F0FF]/40 font-bold"
                    : "text-slate-400 hover:bg-slate-900 border border-transparent"
                }`}
              >
                Otisk prstu
              </button>
              <button
                type="button"
                onClick={() => { setAuthMethod("pin"); setErrorMsg(null); }}
                className={`px-2.5 py-1 rounded-lg text-xs transition-all ${
                  authMethod === "pin"
                    ? "bg-[#00F0FF]/20 text-[#00F0FF] border border-[#00F0FF]/40 font-bold"
                    : "text-slate-400 hover:bg-slate-900 border border-transparent"
                }`}
              >
                Master PIN
              </button>
            </div>
          </div>

          {authMethod === "biometric" ? (
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center gap-2 text-center">
              <button
                type="button"
                onClick={handleSimulateBiometric}
                disabled={biometricScanning || biometricSuccess}
                className={`p-4 rounded-full border-2 transition-all cursor-pointer ${
                  biometricSuccess
                    ? "bg-emerald-950/40 border-emerald-400 text-emerald-400"
                    : biometricScanning
                    ? "bg-[#00F0FF]/20 border-[#00F0FF] text-[#00F0FF] animate-pulse"
                    : "bg-slate-900 border-slate-700 text-slate-300 hover:border-[#00F0FF] hover:text-[#00F0FF]"
                }`}
              >
                <Fingerprint className="w-8 h-8" />
              </button>
              <span className="text-xs text-slate-300">
                {biometricSuccess ? (
                  <strong className="text-emerald-400 flex items-center gap-1 justify-center">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Biometrie úspěšně ověřena
                  </strong>
                ) : biometricScanning ? (
                  "Snímám otisk prstu operátora..."
                ) : (
                  "Klikněte pro ověření otisku prstu"
                )}
              </span>
            </div>
          ) : (
            <div className="space-y-1.5">
              <label className="text-[11px] text-slate-400 block">Zadejte administrátorský PIN (Master: 4201):</label>
              <input
                type="password"
                maxLength={6}
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                placeholder="••••"
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-base tracking-widest text-center text-white focus:outline-none focus:border-red-400"
              />
            </div>
          )}

          {errorMsg && (
            <div className="text-xs text-red-400 font-bold flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>{errorMsg}</span>
            </div>
          )}
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-between pt-4 border-t border-slate-800">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Zrušit (Ponechat Veto)
          </button>

          <button
            type="button"
            onClick={handleExecute}
            className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs shadow-[0_0_20px_rgba(239,68,68,0.4)] transition-all flex items-center gap-2 cursor-pointer active:scale-95"
          >
            <Activity className="w-4 h-4 text-white" />
            <span>PROTLAČIT VSTUP (EXECUTIVE OVERRIDE)</span>
          </button>
        </div>
      </div>
    </div>
  );
};
