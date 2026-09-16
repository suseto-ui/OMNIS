import React, { useState } from "react";
import { 
  X, 
  ArrowRight, 
  ArrowLeft, 
  CheckCircle2, 
  Sparkles, 
  MessageSquare, 
  Mic, 
  BarChart3, 
  Brain, 
  Layers, 
  Keyboard,
  Activity
} from "lucide-react";

interface OnboardingTourModalProps {
  onClose: () => void;
  onNavigateTab: (tab: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "dev_lab") => void;
}

interface TourStep {
  title: string;
  subtitle: string;
  description: string;
  icon: React.ReactNode;
  highlightColor: string;
  accentBadge: string;
  tabTarget?: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "dev_lab";
  bullets: string[];
}

const TOUR_STEPS: TourStep[] = [
  {
    title: "Vítejte v O.M.N.I.S.",
    subtitle: "Omni-Modal Networked Intelligence System",
    description: "Váš autonomní kognitivní asistent pro analytickou dekompozici, systémový audit a hlasové diktování dotazů.",
    icon: <Sparkles className="w-8 h-8 text-[#00F0FF]" />,
    highlightColor: "#00F0FF",
    accentBadge: "ÚVODNÍ PRŮVODCE",
    bullets: [
      "Integrované rozhraní se 6 specializovanými pracovními záložkami",
      "Plná podpora offline redundance v lokální IndexedDB databázi",
      "Pokročilá analytická dekompozice odpovědí v reálném čase"
    ]
  },
  {
    title: "1. Krok: Chat a Hlasové Diktování",
    subtitle: "Interaktivní konverzační časová osa",
    description: "Zadávejte dotazy klasicky psaním nebo využijte hlasové diktování v češtině díky rozhraní Web Speech API.",
    icon: <Mic className="w-8 h-8 text-red-400" />,
    highlightColor: "#EF4444",
    accentBadge: "VOICE & CHAT",
    tabTarget: "chat",
    bullets: [
      "Tlačítko mikrofonu u textového pole spustí diktování v cs-CZ",
      "Odpovědi si můžete nechat přečíst stiskem ikony reproduktoru (TTS)",
      "Možnost připojování příloh a rychlých šablon dotazů"
    ]
  },
  {
    title: "2. Krok: Centrální Analýza & 8D Matice",
    subtitle: "Octagon 8D Matice Dopadů",
    description: "Hodnoťte dopady řešení na 8 klíčových systémových dimenzí od ekonomie po ekologii a fyziku.",
    icon: <BarChart3 className="w-8 h-8 text-blue-400" />,
    highlightColor: "#3B82F6",
    accentBadge: "ANALÝZA & AUDIT",
    tabTarget: "analytics",
    bullets: [
      "Souhrnný přehled stavu kognitivní homeostázy a odchylek",
      "Simulace systémových šoků a bezpečnostní audit v 8D grafu",
      "Okamžitý náhled kompozitního skóre systémových rizik"
    ]
  },
  {
    title: "3. Krok: Kognitivní Uzly & Deep Dive",
    subtitle: "Dekompozice inferenčního procesu",
    description: "Nahlédněte pod kapotu myšlení modelu O.M.N.I.S. a prozkoumejte atomické uzly reakcí.",
    icon: <Brain className="w-8 h-8 text-[#A855F7]" />,
    highlightColor: "#A855F7",
    accentBadge: "DEEP DIVE UZLY",
    tabTarget: "nodes",
    bullets: [
      "Rozklad odpovědí na sémantické jádro a entropickou redukci",
      "Homeostatické kotvení garance stability odpovědí",
      "Spuštění hloubkové Deep Dive analýzy pro jakoukoliv zprávu"
    ]
  },
  {
    title: "4. Krok: Archiv Vláken & Redundance",
    subtitle: "Správa konverzačního kontextu",
    description: "Spravujte neomezené množství konverzačních vláken s garancí automatického zálohování v IndexedDB.",
    icon: <Layers className="w-8 h-8 text-emerald-400" />,
    highlightColor: "#10B981",
    accentBadge: "ARCHIV & ZÁLOHY",
    tabTarget: "archive",
    bullets: [
      "Full-textové vyhledávání napříč všemi minulými konverzacemi",
      "Sloučení dvou vláken do jedné chronologické časové osy",
      "Export a Import kompletní historie ve formátu JSON"
    ]
  },
  {
    title: "5. Krok: Klávesové Zkratky & Hotkeys",
    subtitle: "Maximální efektivita práce",
    description: "Ovládejte systém O.M.N.I.S. bleskově přímo z klávesnice bez nutnosti klikání myší.",
    icon: <Keyboard className="w-8 h-8 text-amber-400" />,
    highlightColor: "#F59E0B",
    accentBadge: "EFEKTIVITA",
    bullets: [
      "Ctrl + K : Otevřít full-textové vyhledávání",
      "Ctrl + M : Zapnout / vypnout hlasové diktování",
      "Ctrl + J : Otevřít modul pro sloučení vláken",
      "? : Kdykoliv zobrazit nápovědu a zkratky"
    ]
  }
];

export const OnboardingTourModal: React.FC<OnboardingTourModalProps> = ({
  onClose,
  onNavigateTab,
}) => {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const step = TOUR_STEPS[currentStepIndex];
  const isFirst = currentStepIndex === 0;
  const isLast = currentStepIndex === TOUR_STEPS.length - 1;

  const handleNext = () => {
    if (!isLast) {
      const nextIdx = currentStepIndex + 1;
      setCurrentStepIndex(nextIdx);
      if (TOUR_STEPS[nextIdx].tabTarget) {
        onNavigateTab(TOUR_STEPS[nextIdx].tabTarget!);
      }
    } else {
      handleComplete();
    }
  };

  const handlePrev = () => {
    if (!isFirst) {
      const prevIdx = currentStepIndex - 1;
      setCurrentStepIndex(prevIdx);
      if (TOUR_STEPS[prevIdx].tabTarget) {
        onNavigateTab(TOUR_STEPS[prevIdx].tabTarget!);
      }
    }
  };

  const handleComplete = () => {
    localStorage.setItem("omnis_onboarding_completed", "true");
    onNavigateTab("chat");
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-in fade-in duration-300 font-sans text-slate-200">
      <div 
        className="bg-[#0A0F1D] border rounded-2xl w-full max-w-xl overflow-hidden shadow-[0_0_60px_rgba(0,0,0,0.8)] flex flex-col transition-all duration-300"
        style={{ borderColor: `${step.highlightColor}60` }}
      >
        {/* Header Bar */}
        <div className="p-5 bg-[#060A17] border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span 
              className="px-2.5 py-1 rounded-md text-[10px] font-mono font-bold tracking-wider uppercase border"
              style={{ 
                backgroundColor: `${step.highlightColor}15`, 
                color: step.highlightColor,
                borderColor: `${step.highlightColor}40`
              }}
            >
              {step.accentBadge}
            </span>
            <span className="text-xs font-mono text-slate-400">
              Krok {currentStepIndex + 1} z {TOUR_STEPS.length}
            </span>
          </div>

          <button
            onClick={handleComplete}
            className="p-1.5 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
            title="Přeskočit průvodce"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="p-6 space-y-5">
          {/* Main Visual Title Card */}
          <div className="flex items-start gap-4 p-4 rounded-xl bg-[#070B18] border border-slate-800/80">
            <div 
              className="p-3 rounded-xl border flex-shrink-0"
              style={{ 
                backgroundColor: `${step.highlightColor}10`,
                borderColor: `${step.highlightColor}30`
              }}
            >
              {step.icon}
            </div>
            <div className="space-y-1">
              <h3 className="text-lg font-mono font-bold text-slate-100">{step.title}</h3>
              <p className="text-xs font-mono text-slate-400 uppercase tracking-wide">{step.subtitle}</p>
            </div>
          </div>

          {/* Description */}
          <p className="text-xs font-sans text-slate-300 leading-relaxed">
            {step.description}
          </p>

          {/* Bullets List */}
          <div className="space-y-2 pt-2 border-t border-slate-800/60 font-sans text-xs">
            {step.bullets.map((b, idx) => (
              <div key={idx} className="flex items-center gap-2.5 text-slate-200">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span>{b}</span>
              </div>
            ))}
          </div>

          {/* Progress Bar Indicators */}
          <div className="flex items-center gap-1.5 pt-2">
            {TOUR_STEPS.map((_, i) => (
              <div
                key={i}
                className={`h-1.5 rounded-full flex-1 transition-all duration-300 ${
                  i === currentStepIndex
                    ? "bg-[#00F0FF] shadow-[0_0_8px_#00F0FF]"
                    : i < currentStepIndex
                    ? "bg-slate-700"
                    : "bg-slate-850"
                }`}
              />
            ))}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-[#060A17] border-t border-slate-800 flex items-center justify-between">
          <button
            onClick={handlePrev}
            disabled={isFirst}
            className={`px-4 py-2 rounded-xl text-xs font-mono font-bold flex items-center gap-1.5 transition-all ${
              isFirst
                ? "opacity-40 cursor-not-allowed text-slate-600"
                : "bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-700"
            }`}
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Zpět</span>
          </button>

          <div className="flex items-center gap-2">
            {!isLast && (
              <button
                onClick={handleComplete}
                className="px-3 py-2 text-xs font-mono text-slate-400 hover:text-slate-200 transition-colors"
              >
                Přeskočit
              </button>
            )}

            <button
              onClick={handleNext}
              className="px-5 py-2.5 rounded-xl text-xs font-mono font-bold bg-[#00F0FF] hover:bg-[#00F0FF]/80 text-slate-950 shadow-[0_0_15px_rgba(0,240,255,0.3)] flex items-center gap-1.5 transition-all"
            >
              <span>{isLast ? "Dokončit Průvodce" : "Pokračovat"}</span>
              {!isLast && <ArrowRight className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
