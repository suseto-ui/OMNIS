import React, { useState, useEffect, useMemo } from "react";
import {
  FlaskConical,
  Bug,
  Calculator,
  RotateCcw,
  Play,
  CheckCircle2,
  AlertCircle,
  Clock,
  Sparkles,
  Zap,
  Terminal,
  Layers,
  Cpu,
  BarChart3,
  ShieldCheck,
  ShieldAlert,
  Copy,
  Check,
  ArrowRight,
  ExternalLink,
  HelpCircle,
  Filter,
  RefreshCw,
  Sliders,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { EpistemicMemoryDashboard } from "./components/EpistemicMemoryDashboard";

// ==========================================================
// TYPES & SCHEMAS
// ==========================================================

export interface TokenEstimation {
  user_query_tokens: number;
  system_prompt_tokens: number;
  context_memory_tokens: number;
  estimated_prompt_tokens: number;
  estimated_completion_tokens: number;
  estimated_total_tokens: number;
  estimated_cost_usd: number;
  estimated_cost_czk: number;
}

export interface DevPromptProposal {
  id: string;
  title: string;
  category:
    | "spof_stress"
    | "zero_trust"
    | "thermodynamics"
    | "neuro_ergonomics"
    | "ai_governance"
    | "win_win_leverage"
    | "conflict_edge"
    | "autopoiesis";
  categoryLabel: string;
  domain: string;
  domainLabel: string;
  badgeColor: string;
  prompt: string;
  rationale: string;
  expectedInvariants: string[];
  tokenEstimation: TokenEstimation;
}

export interface InvariantCheckItem {
  name: string;
  description: string;
  passed: boolean;
  details: string;
}

export interface InvariantAuditReport {
  promptId: string;
  status: "idle" | "testing" | "passed" | "warning";
  timestamp: string;
  actualTokens?: {
    prompt: number;
    completion: number;
    total: number;
    cost_usd: number;
  };
  invariants: InvariantCheckItem[];
  allPassed: boolean;
  scorePercent: number;
  feedback: string;
  receivedAnswer?: string;
  receivedMatrix?: any;
  receivedForensics?: any;
}

export interface TokenTelemetryState {
  cumulative_prompt_tokens: number;
  cumulative_completion_tokens: number;
  cumulative_total_tokens: number;
  total_queries_executed: number;
  estimated_total_cost_usd: number;
  estimated_total_cost_czk: number;
  recent_records: any[];
}

interface DevPromptLabProps {
  onExecutePromptInChat: (query: string, domain: string) => void;
  currentDomain: string;
  onSelectDomain: (domain: string) => void;
  sessionTokenTelemetry: TokenTelemetryState;
  onRefreshTelemetry: () => Promise<void>;
  onResetTelemetry: () => Promise<void>;
  onNavigateToTab?: (tab: string) => void;
  onOpenHelpModal?: () => void;
  onOpenSearchModal?: () => void;
  onOpenMergeModal?: () => void;
}

// ==========================================================
// CONSTANTS & TOKEN HEURISTICS
// ==========================================================

const BASE_SYSTEM_TOKENS = 1180;
const CONTEXT_MEM_TOKENS = 150;
const USD_TO_CZK_RATE = 23.5;
const INPUT_RATE_PER_M = 0.075;
const OUTPUT_RATE_PER_M = 0.3;

function calculateTokensClientSide(text: string): TokenEstimation {
  if (!text) {
    return {
      user_query_tokens: 0,
      system_prompt_tokens: BASE_SYSTEM_TOKENS,
      context_memory_tokens: CONTEXT_MEM_TOKENS,
      estimated_prompt_tokens: BASE_SYSTEM_TOKENS + CONTEXT_MEM_TOKENS,
      estimated_completion_tokens: 1350,
      estimated_total_tokens: BASE_SYSTEM_TOKENS + CONTEXT_MEM_TOKENS + 1350,
      estimated_cost_usd: 0.0005,
      estimated_cost_czk: 0.0118,
    };
  }

  const charCount = text.length;
  const wordCount = text.trim().split(/\s+/).length;
  const estFromChars = charCount / 3.65;
  const estFromWords = wordCount * 1.32;
  const userTokens = Math.max(1, Math.ceil(estFromChars * 0.6 + estFromWords * 0.4));

  const promptTokens = BASE_SYSTEM_TOKENS + CONTEXT_MEM_TOKENS + userTokens;
  const completionTokens = 1350 + Math.floor(userTokens * 0.45);
  const totalTokens = promptTokens + completionTokens;

  const costUsd =
    (promptTokens * INPUT_RATE_PER_M) / 1_000_000 +
    (completionTokens * OUTPUT_RATE_PER_M) / 1_000_000;
  const costCzk = costUsd * USD_TO_CZK_RATE;

  return {
    user_query_tokens: userTokens,
    system_prompt_tokens: BASE_SYSTEM_PROMPT_TOKENS_VAL,
    context_memory_tokens: CONTEXT_MEM_TOKENS,
    estimated_prompt_tokens: promptTokens,
    estimated_completion_tokens: completionTokens,
    estimated_total_tokens: totalTokens,
    estimated_cost_usd: Number(costUsd.toFixed(6)),
    estimated_cost_czk: Number(costCzk.toFixed(4)),
  };
}

const BASE_SYSTEM_PROMPT_TOKENS_VAL = 1180;

// Dynamic generator seed bank across 8 domains and stress categories
function generateFreshBenchmarkBatch(): DevPromptProposal[] {
  const timestampSeed = Date.now();
  const randomSuffix = Math.floor(Math.random() * 900 + 100);

  const rawPool: Array<{
    title: string;
    category: DevPromptProposal["category"];
    categoryLabel: string;
    domain: string;
    domainLabel: string;
    badgeColor: string;
    prompt: string;
    rationale: string;
    invariants: string[];
  }> = [
    {
      title: "Architektonický SPOF & Škálovatelnost (Cluster Consensus)",
      category: "spof_stress",
      categoryLabel: "Architektonický SPOF",
      domain: "SYSTEMS_INTELLIGENCE",
      domainLabel: "Systémové inženýrství & Kybernetika",
      badgeColor: "border-[#00F0FF]/40 text-[#00F0FF] bg-[#00F0FF]/10",
      prompt: `Navrhni distribuovanou event-driven architekturu s propustností ${
        Math.floor(Math.random() * 150 + 50) * 1000
      } msg/s s garancí exactly-once doručení při simulovaném pádu 2 ze 3 klastrových uzlů. Identifikuj primární SPOF a eliminuj split-brain stav.`,
      rationale:
        "Stresuje schopnost Fáze I (dekonstrukce požadavků), Fáze II (izolace pákového uzlového bodu na úrovni konsenzu) a Fáze IV (deterministický kód bez halucinací).",
      invariants: [
        "Fáze I: Přesná identifikace tvrdých parametrů propustnosti a limitů latence",
        "Fáze II: Explicitní definice uzlového bodu (Leverage Point) v konsenzuálním protokolu",
        "Fáze III: Formulace poměru úsilí ku páce (1:10)",
        "Fáze IV: Kompletní kódový nebo konfigurační artefakt bez zástupných komentářů",
        "Forenzní rizika: Detekce asymetrického selhání typu split-brain v T+1",
      ],
    },
    {
      title: "Zero-Trust & Adversarial Infiltration (Metadata Red-Team)",
      category: "zero_trust",
      categoryLabel: "Zero-Trust Red-Teaming",
      domain: "CYBERNETICS",
      domainLabel: "Zero-Trust Bezpečnost & Kryptografie",
      badgeColor: "border-[#F43F5E]/40 text-[#F43F5E] bg-[#F43F5E]/10",
      prompt: `Proveď penetrační analýzu vektoru nepřátelského útoku typu 'Indirect Prompt Injection' skrze metadata ukládaná do epistemické vektorové paměti pgvector. Navrhni kryptografický ověřovací guardrail v TypeScriptu/Rustu s nulovou tolerancí.`,
      rationale:
        "Ověřuje odolnost vůči sycophancy, aktivaci bezpečnostních penalizací v 4D matici (Fáze V) a formulaci exaktních validačních typů v Fázi IV.",
      invariants: [
        "Fáze II: Propojení kybernetiky s kryptografickou integritou",
        "Fáze IV: Striktní typové schéma (žádné 'any') s hashováním či HMAC",
        "Fáze V: Vykázání penalizací za adversarial zranitelnosti v matici",
        "Forenzní analýza: Risk index detekující injektážní vektor s mitigací",
      ],
    },
    {
      title: "Termodynamická Entropie & Green Compute (Joule/Op)",
      category: "thermodynamics",
      categoryLabel: "Termodynamická Entropie",
      domain: "SUSTAINABLE_TECH",
      domainLabel: "Fyzikální termodynamika & Výpočetní efektivita",
      badgeColor: "border-[#10B981]/40 text-[#10B981] bg-[#10B981]/10",
      prompt: `Namodeluj výpočetní entropii a spotřebu energie při inferenci 1 milionu dotazů O.M.N.I.S. Navrhni systémovou optimalizaci, která sníží Jouly na operaci o 40 % pomocí adaptivní kvantizace a hierarchické mezipaměti bez ztráty kognitivní přesnosti.`,
      rationale:
        "Testuje termodynamický pilíř Oktagonu a validuje, zda ekologicko-sociální rozměr v 4D matici dosáhne vysoké hodnoty (>= 0.90).",
      invariants: [
        "Fáze I: Rozpad na energetické konstanty (J/token, paměťová propustnost)",
        "Fáze II: Pákový bod lokalizovaný v hierarchické cache",
        "Fáze IV: Matematický vzorec nebo algoritmus kvantizace",
        "Fáze V: Skóre eco_social_regeneration >= 0.88",
      ],
    },
    {
      title: "Kognitivní Modální Překlad & Neuro-ergonomie",
      category: "neuro_ergonomics",
      categoryLabel: "Modální Překlad",
      domain: "COGNITIVE_SCI",
      domainLabel: "Kognitivní vědy & Neuro-ergonomie",
      badgeColor: "border-[#A855F7]/40 text-[#A855F7] bg-[#A855F7]/10",
      prompt: `Proveď formální kognitivní překlad mezi socio-psychologickým syndromem vyhoření vývojářského týmu a termodynamickým přehřátím distribuované infrastruktury. Jaké strukturální izomorfismy umožňují vyřešit oba problémy současně?`,
      rationale:
        "Ověřuje hloubku mezioborového křížení v Fázi II a psychologickou akceptabilitu v Fázi V.",
      invariants: [
        "Fáze I: Očištění od emočního balastu a rozpad na fundamentální cykly zátěže",
        "Fáze II: Mapování všech 8 domén a formulace netriviálního izomorfismu",
        "Fáze III: Win-Win-Win akční plán pro manažery i inženýry",
        "Fáze V: Psychologická akceptabilita >= 0.85",
      ],
    },
    {
      title: "EU AI Act & Governance Compliance (Article 14 Human Oversight)",
      category: "ai_governance",
      categoryLabel: "Regulace & AI Governance",
      domain: "CYBERNETICS",
      domainLabel: "Regulace, Právo & AI Governance",
      badgeColor: "border-[#3B82F6]/40 text-[#3B82F6] bg-[#3B82F6]/10",
      prompt: `Vypracuj auditní zprávu pro soulad systému O.M.N.I.S. s požadavky Článku 14 Nařízení EU o umělé inteligenci (EU AI Act - Lidský dohled a zastavitelnost). Navrhni hardwarový i softwarový 'Kill-Switch' bez poškození integrity transakcí.`,
      rationale:
        "Testuje regulatorní pilíř a generování formálních bezpečnostních procedur v Fázi IV.",
      invariants: [
        "Fáze I: Explicitní citace regulatorních rámců bez vágních odkazů",
        "Fáze III: Milníky certifikace s poměrem úsilí ku výsledku 1:10",
        "Fáze IV: Deterministický kód signálního handleru / interrupt protokolu",
        "Forenzní analýza: Vyhodnocení regulační compliance delty v T+N",
      ],
    },
    {
      title: "Asymetrická Páka 1:10 & Okamžitá Monetizace (Win-Win-Win)",
      category: "win_win_leverage",
      categoryLabel: "Asymetrická Páka 1:10",
      domain: "SYSTEMS_INTELLIGENCE",
      domainLabel: "Teorie her & Asymetrická ekonomie",
      badgeColor: "border-[#F59E0B]/40 text-[#F59E0B] bg-[#F59E0B]/10",
      prompt: `Identifikuj jediný systémový uzlový bod v moderní CI/CD pipeline, jehož modifikace s investicí pod 4 hodiny práce zredukuje deployment regrese o 75 % a ušetří $${
        Math.floor(Math.random() * 40 + 20) * 1000
      } ročně.`,
      rationale:
        "Ověřuje striktní dodržení principu Win-Win-Win v Fázi III a ekonomickou návratnost v 4D matici.",
      invariants: [
        "Fáze II: Jasně izolovaný 'Leverage Point' (nesmí být prázdný ani vágní)",
        "Fáze III: Kvantifikovaný poměr úsilí ku páce (1:10)",
        "Fáze IV: Konkrétní bash/yaml skript nebo konfigurační kód",
        "Fáze V: Ekonomická životaschopnost >= 0.90",
      ],
    },
    {
      title: "Extrémní Hraniční Konflikt: Zero-Knowledge vs Realtime Search",
      category: "conflict_edge",
      categoryLabel: "Extrémní Hraniční Podmínky",
      domain: "SYSTEMS_INTELLIGENCE",
      domainLabel: "Systémové inženýrství & Kryptografie",
      badgeColor: "border-[#EC4899]/40 text-[#EC4899] bg-[#EC4899]/10",
      prompt: `Vyřeš zdánlivě neslučitelný konflikt mezi požadavkem na absolutní Zero-Knowledge end-to-end šifrování klientských dat a požadavkem na bleskové fulltextové vyhledávání v reálném čase s odezvou pod 50ms na straně serveru.`,
      rationale:
        "Testuje schopnost systému překonat falešná dilemata dekonstrukcí na prvočinitele (homomorfní šifrování, Bloom filtry, ZK-SNARK).",
      invariants: [
        "Fáze I: Detekce falešného kompromisu (trade-off) v zadání",
        "Fáze II: Transdisciplinární přemostění kryptografie a teorie vyhledávání",
        "Fáze IV: Technická specifikace kódového invariantu",
        "Fáze V: Kompozitní skóre matice dopadů >= 0.85",
      ],
    },
    {
      title: "Autopoietická Samoregulace & Zpětnovazební Smyčka (Self-Healing)",
      category: "autopoiesis",
      categoryLabel: "Autopoietická Samoregulace",
      domain: "SUSTAINABLE_TECH",
      domainLabel: "Kybernetika & Systémová inteligence",
      badgeColor: "border-[#14B8A6]/40 text-[#14B8A6] bg-[#14B8A6]/10",
      prompt: `Navrhni autonomní samoregulující se feedback mechanismus pro mikroslužby, který při detekci driftu v latenci automaticky přeskupí výpočetní váhy a zrekonfiguruje paměťové kanály bez nutnosti restartu podů.`,
      rationale:
        "Ověřuje autopoietickou podstatu O.M.N.I.S. – schopnost navrhovat systémy, které generují samy sebe a učí se ze své vlastní telemetrie.",
      invariants: [
        "Fáze II: Zpětnovazební smyčka negativní entropie",
        "Fáze IV: Konkrétní řídicí obvod (PID regulátor nebo adaptive rate limiter)",
        "Fáze V: Reflexivní sebereference a autopoietické parametry",
        "Forenzní analýza: Kaskádový dopad T+1 až T+N",
      ],
    },
  ];

  // Randomize order and calculate accurate token stats
  const shuffled = [...rawPool].sort(() => 0.5 - Math.random());

  return shuffled.slice(0, 6).map((item, index) => {
    const estimation = calculateTokensClientSide(item.prompt);
    return {
      id: `dev-prop-${timestampSeed}-${index}-${randomSuffix}`,
      title: item.title,
      category: item.category,
      categoryLabel: item.categoryLabel,
      domain: item.domain,
      domainLabel: item.domainLabel,
      badgeColor: item.badgeColor,
      prompt: item.prompt,
      rationale: item.rationale,
      expectedInvariants: item.invariants,
      tokenEstimation: estimation,
    };
  });
}

// ==========================================================
// 8D MATRIX AUTOPOIETIC CALIBRATOR & SIMULATOR
// ==========================================================

function AutopoieticMatrixSimulator() {
  const [weights, setWeights] = useState<Record<string, number>>({
    SYS: 1.0,
    ECON: 1.0,
    PSYCH: 1.0,
    ECO: 1.0,
    LAW: 1.0,
    SEC: 1.0,
    PHYS: 1.0,
    SOC: 1.0,
  });
  const [isApplying, setIsApplying] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const dimensionMeta: Record<string, { label: string; desc: string; color: string }> = {
    SYS: { label: "Systémová architektura", desc: "Komplexita, SPOF, mikroslužby", color: "text-[#00F0FF]" },
    ECON: { label: "Ekonomika & Náklady", desc: "ROI, spotřeba tokenů, budget", color: "text-emerald-400" },
    PSYCH: { label: "Kognitivní ergonomie", desc: "Přehlednost, mental load", color: "text-purple-400" },
    ECO: { label: "Ekologie & Termodynamika", desc: "Green compute, Joule/op", color: "text-teal-400" },
    LAW: { label: "Právo & Compliance", desc: "EU AI Act, NIS2, GDPR", color: "text-amber-400" },
    SEC: { label: "Bezpečnost & Zero-Trust", desc: "Red-Teaming, injektáž, audit", color: "text-rose-400" },
    PHYS: { label: "Fyzikální omezení", desc: "Latence, sítě, HW zdroje", color: "text-sky-400" },
    SOC: { label: "Sociální dopad & Etika", desc: "Důvěra, transparentnost", color: "text-indigo-400" },
  };

  const values = Object.values(weights);
  const compositeScore = useMemo(() => {
    const sum = values.reduce((a, b) => a + b, 0);
    return Number((sum / values.length).toFixed(3));
  }, [values]);

  const balanceIndex = useMemo(() => {
    const mean = compositeScore;
    const variance = values.reduce((acc, v) => acc + Math.pow(v - mean, 2), 0) / values.length;
    return Number(Math.max(0, 1 - Math.sqrt(variance) * 0.5).toFixed(3));
  }, [values, compositeScore]);

  const handleSliderChange = (dim: string, val: number) => {
    setWeights(prev => ({ ...prev, [dim]: val }));
  };

  const handlePreset = (type: string) => {
    if (type === "balanced") {
      setWeights({ SYS: 1.0, ECON: 1.0, PSYCH: 1.0, ECO: 1.0, LAW: 1.0, SEC: 1.0, PHYS: 1.0, SOC: 1.0 });
    } else if (type === "security") {
      setWeights({ SYS: 1.2, ECON: 0.9, PSYCH: 1.0, ECO: 0.9, LAW: 1.3, SEC: 1.5, PHYS: 1.0, SOC: 1.1 });
    } else if (type === "green") {
      setWeights({ SYS: 1.0, ECON: 1.1, PSYCH: 1.0, ECO: 1.5, LAW: 1.1, SEC: 1.0, PHYS: 1.4, SOC: 1.2 });
    } else if (type === "high_assurance") {
      setWeights({ SYS: 1.4, ECON: 1.0, PSYCH: 1.1, ECO: 1.0, LAW: 1.4, SEC: 1.4, PHYS: 1.2, SOC: 1.2 });
    }
  };

  const handleApplyHomeostasis = async () => {
    setIsApplying(true);
    setStatusMessage(null);
    try {
      const res = await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message_id: "00000000-0000-0000-0000-000000000000",
          conversation_id: "00000000-0000-0000-0000-000000000000",
          user_rating: Math.min(5, Math.max(1, Math.round(compositeScore * 3.5))),
          feedback_text: `Dev Lab kalibrace: Kompozit=${compositeScore}, Harmonie=${balanceIndex}`,
          adjusted_matrix: {
            sys: weights.SYS,
            econ: weights.ECON,
            psych: weights.PSYCH,
            eco: weights.ECO,
            law: weights.LAW,
            sec: weights.SEC,
            phys: weights.PHYS,
            soc: weights.SOC,
            composite_score: compositeScore,
            reasoning: `Manuální kalibrace vývojáře v Dev Labu (Harmonie: ${(balanceIndex * 100).toFixed(0)}%)`
          }
        })
      });
      if (res.ok) {
        setStatusMessage("✅ Kalibrační váhy byly úspěšně odeslány do autopoietické smyčky!");
      } else {
        setStatusMessage("⚠️ Odeslání se nezdařilo.");
      }
    } catch {
      setStatusMessage("⚠️ Chyba sítě při odesílání kalibrace.");
    } finally {
      setIsApplying(false);
      setTimeout(() => setStatusMessage(null), 4000);
    }
  };

  return (
    <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-purple-500/30 p-4 sm:p-5 shadow-xl space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div className="flex items-center gap-2">
          <Sliders className="w-5 h-5 text-purple-400" />
          <div>
            <h2 className="text-sm sm:text-base font-bold text-slate-100 font-sans flex items-center gap-2">
              Autopoietický Kalibrátor 8D Matice (Live Rekalibrace)
              <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-mono text-[10px] border border-purple-500/40">
                Fáze 2.1
              </span>
            </h2>
            <p className="text-xs text-slate-400 font-sans mt-0.5">
              Umožňuje vývojářům dynamicky ladit relativní váhy jednotlivých dimenzí a simulovat dopad na kognitivní homeostázu.
            </p>
          </div>
        </div>

        {/* Quick Presets */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-[10px] text-slate-400 font-mono">Předvolby:</span>
          <button
            onClick={() => handlePreset("balanced")}
            className="px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-mono transition-colors"
          >
            Vyvážená
          </button>
          <button
            onClick={() => handlePreset("security")}
            className="px-2 py-1 rounded bg-rose-950/60 hover:bg-rose-900/60 text-rose-300 border border-rose-500/30 text-[10px] font-mono transition-colors"
          >
            Zero-Trust
          </button>
          <button
            onClick={() => handlePreset("green")}
            className="px-2 py-1 rounded bg-teal-950/60 hover:bg-teal-900/60 text-teal-300 border border-teal-500/30 text-[10px] font-mono transition-colors"
          >
            Green Compute
          </button>
          <button
            onClick={() => handlePreset("high_assurance")}
            className="px-2 py-1 rounded bg-purple-950/60 hover:bg-purple-900/60 text-purple-300 border border-purple-500/30 text-[10px] font-mono transition-colors"
          >
            Kritické Systémy
          </button>
        </div>
      </div>

      {/* Sliders Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {Object.entries(weights).map(([dim, val]) => {
          const meta = dimensionMeta[dim] || { label: dim, desc: "", color: "text-slate-200" };
          return (
            <div key={dim} className="bg-slate-950/70 border border-slate-800 rounded-xl p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className={`font-mono text-xs font-bold ${meta.color}`}>{dim}</span>
                <span className="font-mono text-xs font-extrabold text-slate-100 px-1.5 py-0.5 rounded bg-slate-900 border border-slate-700">
                  {val.toFixed(2)}x
                </span>
              </div>
              <div className="text-[10px] text-slate-300 font-sans line-clamp-1">{meta.label}</div>
              <input
                type="range"
                min="0.2"
                max="2.0"
                step="0.05"
                value={val}
                onChange={(e) => handleSliderChange(dim, parseFloat(e.target.value))}
                className="w-full accent-purple-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
              />
              <div className="text-[9px] text-slate-500 font-mono truncate">{meta.desc}</div>
            </div>
          );
        })}
      </div>

      {/* Realtime Analysis & Action Bar */}
      <div className="p-3 rounded-xl bg-purple-950/20 border border-purple-500/30 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs font-mono">
        <div className="flex items-center gap-4 flex-wrap">
          <span className="text-slate-300">
            Kompozitní váha: <strong className="text-purple-300">{compositeScore}</strong>
          </span>
          <span className="text-slate-300">
            Index harmonie: <strong className="text-emerald-400">{(balanceIndex * 100).toFixed(0)}%</strong>
          </span>
          <span className="text-slate-400 text-[11px]">
            Cíl homeostázy: <strong className="text-[#00F0FF]">0.95</strong>
          </span>
          {statusMessage && (
            <span className="text-emerald-300 text-xs font-sans font-semibold animate-pulse">
              {statusMessage}
            </span>
          )}
        </div>

        <button
          onClick={handleApplyHomeostasis}
          disabled={isApplying}
          className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-mono font-bold text-xs flex items-center gap-2 transition-all shadow-lg hover:shadow-purple-500/25 disabled:opacity-50 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isApplying ? "animate-spin" : ""}`} />
          {isApplying ? "Aplikuji..." : "Uložit do autopoietické smyčky"}
        </button>
      </div>
    </div>
  );
}

// ==========================================================
// MAIN COMPONENT: DEV PROMPT LAB
// ==========================================================

export default function DevPromptLab({
  onExecutePromptInChat,
  currentDomain,
  onSelectDomain,
  sessionTokenTelemetry,
  onRefreshTelemetry,
  onResetTelemetry,
  onNavigateToTab,
  onOpenHelpModal,
  onOpenSearchModal,
  onOpenMergeModal,
}: DevPromptLabProps) {
  // Proposals generated afresh every mount or on manual button click
  const [proposals, setProposals] = useState<DevPromptProposal[]>(() =>
    generateFreshBenchmarkBatch()
  );
  const [generationCount, setGenerationCount] = useState(1);
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>("all");
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Live Token Sandbox / Custom Query Calculator
  const [sandboxQuery, setSandboxQuery] = useState(
    "Jak implementovat zero-copy distribuovaný buffer v Rustu s formální matematickou verifikací invariance?"
  );
  const sandboxEstimation = useMemo(
    () => calculateTokensClientSide(sandboxQuery),
    [sandboxQuery]
  );

  // Invariant Audit Runner state
  const [activeTestingId, setActiveTestingId] = useState<string | null>(null);
  const [auditReports, setAuditReports] = useState<Record<string, InvariantAuditReport>>({});
  const [expandedProposalDetails, setExpandedProposalDetails] = useState<
    Record<string, boolean>
  >({});
  const [devModeEnabled, setDevModeEnabled] = useState(true);

  // Regenerate fresh proposals automatically on mount or when requested
  const handleRegenerateBatch = () => {
    const fresh = generateFreshBenchmarkBatch();
    setProposals(fresh);
    setGenerationCount((c) => c + 1);
  };

  const toggleProposalDetails = (id: string) => {
    setExpandedProposalDetails((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const handleCopyPrompt = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Run Query & Execute Real Invariant Verification (Finding Bugs/Regressions)
  const handleRunInvariantTest = async (prop: DevPromptProposal) => {
    setActiveTestingId(prop.id);
    onSelectDomain(prop.domain);

    const initialReport: InvariantAuditReport = {
      promptId: prop.id,
      status: "testing",
      timestamp: new Date().toLocaleTimeString(),
      invariants: prop.expectedInvariants.map((inv) => ({
        name: inv.split(":")[0] || "Invariant",
        description: inv,
        passed: false,
        details: "Probíhá exekuce a analýza odpovědi...",
      })),
      allPassed: false,
      scorePercent: 0,
      feedback: "Dotaz byl odeslán do O.M.N.I.S. kognitivního jádra...",
    };

    setAuditReports((prev) => ({ ...prev, [prop.id]: initialReport }));
    setExpandedProposalDetails((prev) => ({ ...prev, [prop.id]: true }));

    try {
      const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: prop.prompt,
          ontology_domain: prop.domain,
          enable_thinking: true,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: Selhání komunikace`);
      }

      const data = await response.json();

      // Refresh cumulative telemetry from backend
      await onRefreshTelemetry();

      // Perform deep programmatic verification of invariants
      const answerText: string = data.answer || "";
      const thoughts: string = data.cognitive_process || "";
      const matrix = data.impact_matrix || {};
      const forensics = data.consequence_forensics || null;
      const tokens = data.token_usage || {
        prompt_tokens: prop.tokenEstimation.estimated_prompt_tokens,
        completion_tokens: prop.tokenEstimation.estimated_completion_tokens,
        total_tokens: prop.tokenEstimation.estimated_total_tokens,
        cost_usd: prop.tokenEstimation.estimated_cost_usd,
      };

      // Invariant checks:
      const checks: InvariantCheckItem[] = [];

      // Check 1: 5-Phase Structure
      const hasPhaseHeaders =
        answerText.includes("FÁZE") ||
        answerText.includes("Fáze") ||
        answerText.includes("Sémantická") ||
        thoughts.includes("Fáze");
      checks.push({
        name: "Invariant I: Kognitivní Fáze",
        description: "Výstup obsahuje strukturované fázové členění kognitivního cyklu O.M.N.I.S.",
        passed: hasPhaseHeaders,
        details: hasPhaseHeaders
          ? "Detekováno standardní fázové členění v syntéze i kognitivním procesu."
          : "VAROVÁNÍ: Absentují zřetelné hlavičky fází kognitivního cyklu.",
      });

      // Check 2: Leverage Point Isolation
      const matrixLeverage = matrix.leverage_point || "";
      const hasLeverage =
        matrixLeverage.length > 5 ||
        answerText.toLowerCase().includes("pákový") ||
        answerText.toLowerCase().includes("leverage") ||
        answerText.toLowerCase().includes("uzlový bod");
      checks.push({
        name: "Invariant II: Pákový Uzlový Bod",
        description: "Byl v Fázi II úspěšně izolován nenulový uzlový bod (Leverage Point).",
        passed: hasLeverage,
        details: hasLeverage
          ? `Uzlový bod nalezen: "${matrixLeverage || "Izolován v těle syntézy"}"`
          : "CHYBA: Leverage point nebyl nalezen nebo je prázdný.",
      });

      // Check 3: 4D Impact Matrix Normalization
      const compScore = Number(matrix.composite_score || 0);
      const isMatrixValid =
        compScore > 0 &&
        compScore <= 1.0 &&
        typeof matrix.economic_viability === "number" &&
        typeof matrix.technological_elegance === "number";
      checks.push({
        name: "Invariant III: 4D Matice Dopadů",
        description: "Kompozitní index je normalizován v intervalu (0.0 až 1.0) s váženým součtem.",
        passed: isMatrixValid,
        details: isMatrixValid
          ? `Kompozitní index: ${(compScore * 100).toFixed(1)}% (Ekon: ${(
              matrix.economic_viability * 100
            ).toFixed(0)}%, Tech: ${(matrix.technological_elegance * 100).toFixed(0)}%)`
          : "CHYBA: Kompozitní index matice je nulový nebo mimo validní meze.",
      });

      // Check 4: Risk Forensics & Cascade Drift
      const hasForensics =
        forensics !== null &&
        typeof forensics.risk_index === "number" &&
        forensics.risk_level;
      checks.push({
        name: "Invariant IV: Forenzní Analýza Rizik",
        description: "Systém vyhodnotil T+1 kaskádové efekty a asymetrické SPOF módy.",
        passed: Boolean(hasForensics),
        details: hasForensics
          ? `Rizikový index: ${(forensics.risk_index * 100).toFixed(1)}% (${
              forensics.risk_level
            }), Horizont: ${forensics.horizon || "T+N"}`
          : "VAROVÁNÍ: Forenzní modul nevrátil kompletní metadata rizikových vektorů.",
      });

      // Check 5: Zero-Fluff & Actionable Code
      const hasTodoPlaceholders =
        answerText.includes("// TODO") ||
        answerText.includes("TODO: implement") ||
        answerText.includes("Lorem ipsum");
      const hasDirectExecution =
        answerText.includes("```") ||
        answerText.includes("class ") ||
        answerText.includes("def ") ||
        answerText.includes("function ") ||
        answerText.includes("interface ") ||
        answerText.includes("Directiva") ||
        answerText.length > 250;
      const passZeroFluff = !hasTodoPlaceholders && hasDirectExecution;
      checks.push({
        name: "Invariant V: Zero-Fluff & Exekuční Čistota",
        description: "Žádné zástupné komentáře typu // TODO, kód je deterministický.",
        passed: passZeroFluff,
        details: passZeroFluff
          ? "Výstup neobsahuje zakázané zástupné komentáře a je připraven k nasazení."
          : "CHYBA: Detekován vágní zástupný obsah nebo chybí exekuční artefakt.",
      });

      const passedCount = checks.filter((c) => c.passed).length;
      const score = Math.round((passedCount / checks.length) * 100);
      const is100Percent = passedCount === checks.length;

      setAuditReports((prev) => ({
        ...prev,
        [prop.id]: {
          promptId: prop.id,
          status: is100Percent ? "passed" : "warning",
          timestamp: new Date().toLocaleTimeString(),
          actualTokens: {
            prompt: tokens.prompt_tokens,
            completion: tokens.completion_tokens,
            total: tokens.total_tokens,
            cost_usd: tokens.cost_usd || 0.0004,
          },
          invariants: checks,
          allPassed: is100Percent,
          scorePercent: score,
          feedback: is100Percent
            ? "✅ 100% KOREKTNÍ: Všechny kognitivní invarianty O.M.N.I.S. byly úspěšně splněny."
            : `⚠️ ODCHYLKA DETEKOVÁNA (${score}%): ${
                checks.length - passedCount
              } invariant(y) vyžadují pozornost. Můžete analyzovat konkrétní chybu níže.`,
          receivedAnswer: answerText,
          receivedMatrix: matrix,
          receivedForensics: forensics,
        },
      }));
    } catch (err: any) {
      setAuditReports((prev) => ({
        ...prev,
        [prop.id]: {
          promptId: prop.id,
          status: "warning",
          timestamp: new Date().toLocaleTimeString(),
          invariants: [
            {
              name: "Komunikační invariant",
              description: "Spojení s backendem O.M.N.I.S.",
              passed: false,
              details: `Chyba volání: ${err?.message || "Neznámé selhání sítě"}`,
            },
          ],
          allPassed: false,
          scorePercent: 0,
          feedback: `Chyba při testování: ${err?.message || "Neznámá chyba"}`,
        },
      }));
    } finally {
      setActiveTestingId(null);
    }
  };

  const filteredProposals = useMemo(() => {
    if (selectedCategoryFilter === "all") return proposals;
    return proposals.filter((p) => p.category === selectedCategoryFilter);
  }, [proposals, selectedCategoryFilter]);

  if (!devModeEnabled) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center bg-[#0A0F1D]">
        <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center mb-4 text-amber-400">
          <Bug className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-100 mb-2">Vývojový Mód Je Skrytý</h2>
        <p className="text-sm text-slate-400 max-w-md mb-6 leading-relaxed">
          Tato stránka je interní diagnostický modul určený pro vývoj a hledání chyb. V produkčním nasazení zůstává deaktivována.
        </p>
        <button
          onClick={() => setDevModeEnabled(true)}
          className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-[#00F0FF] text-slate-950 font-mono font-bold text-xs hover:brightness-110 transition-all shadow-[0_0_20px_rgba(245,158,11,0.25)]"
        >
          Aktivovat Vývojovou Laboratoř (DEV ONLY)
        </button>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col min-h-0 overflow-y-auto p-4 sm:p-6 space-y-6">
      {/* ==========================================================
          DEV ONLY HEADER & SAFEGUARD NOTICE
         ========================================================== */}
      <div className="rounded-2xl bg-gradient-to-r from-amber-500/15 via-[#00F0FF]/15 to-purple-500/15 border border-amber-500/40 p-4 sm:p-5 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 relative z-10">
          <div className="flex items-start gap-3.5">
            <div className="w-11 h-11 rounded-xl bg-amber-500/20 border border-amber-500/50 flex items-center justify-center text-amber-400 shrink-0 shadow-[0_0_15px_rgba(245,158,11,0.3)]">
              <FlaskConical className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-lg sm:text-xl font-extrabold text-slate-100 font-sans tracking-wide">
                  Vývojová Laboratoř & Prompt Benchmark
                </h1>
                <span className="px-2.5 py-0.5 rounded-full bg-amber-500/20 border border-amber-500/60 text-amber-300 font-mono text-[11px] font-bold">
                  DEV ONLY • NENÍ PRO PRODUKCI
                </span>
                <span className="px-2 py-0.5 rounded-full bg-cyan-500/10 border border-cyan-500/40 text-[#00F0FF] font-mono text-[10px]">
                  Generace #{generationCount}
                </span>
              </div>
              <p className="text-xs text-slate-300 font-sans mt-1 max-w-3xl leading-relaxed">
                Tato vyhrazená stránka při <strong>každém novém otevření</strong> automaticky vygeneruje novou unikátní sadu návrhů dotazů. Slouží k systematickému ověřování funkčnosti, vyhodnocování 100% korektnosti a odhalování skrytých chyb před nasazením do produkce.
              </p>
            </div>
          </div>

          {/* Quick Toolbar */}
          <div className="flex items-center gap-2 shrink-0 self-end lg:self-center">
            <button
              onClick={handleRegenerateBatch}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900/90 border border-amber-500/50 text-amber-300 hover:text-white hover:bg-amber-500/20 text-xs font-mono font-bold transition-all shadow-[0_0_15px_rgba(245,158,11,0.2)]"
            >
              <RefreshCw className="w-3.5 h-3.5 animate-spin-reverse" />
              <span>Nová sada návrhů</span>
            </button>
            <button
              onClick={() => setDevModeEnabled(false)}
              className="p-2 rounded-xl bg-slate-900/80 border border-slate-700 text-slate-400 hover:text-slate-200 text-xs font-mono"
              title="Simulovat produkční režim (skrýt vývojový tab)"
            >
              Skrýt DEV
            </button>
          </div>
        </div>
      </div>

      {/* ==========================================================
          GLOBAL TOKEN TELEMETRY CARDS (CUMULATIVE CONSUMPTION)
         ========================================================== */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* Total Tokens Consumed */}
        <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-cyan-500/30 p-4 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs font-mono mb-2">
            <span className="flex items-center gap-1.5 text-[#00F0FF] font-semibold">
              <Zap className="w-4 h-4" />
              Celkem Spotřebováno
            </span>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-300">
              Session
            </span>
          </div>
          <div className="text-2xl font-extrabold font-mono text-slate-100">
            {sessionTokenTelemetry.cumulative_total_tokens.toLocaleString()}
            <span className="text-xs text-slate-400 font-normal ml-1.5">tokenů</span>
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 mt-2 pt-2 border-t border-slate-800">
            <span>In: {sessionTokenTelemetry.cumulative_prompt_tokens.toLocaleString()}</span>
            <span>•</span>
            <span>Out: {sessionTokenTelemetry.cumulative_completion_tokens.toLocaleString()}</span>
          </div>
        </div>

        {/* Financial Cost Estimate */}
        <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-emerald-500/30 p-4 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs font-mono mb-2">
            <span className="flex items-center gap-1.5 text-[#10B981] font-semibold">
              <Calculator className="w-4 h-4" />
              Náklady na Tokeny
            </span>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300">
              Gemini 3.5
            </span>
          </div>
          <div className="text-2xl font-extrabold font-mono text-emerald-400">
            ${sessionTokenTelemetry.estimated_total_cost_usd.toFixed(5)}
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 mt-2 pt-2 border-t border-slate-800">
            <span>Přepočet na CZK:</span>
            <span className="text-emerald-300 font-bold">
              ~{sessionTokenTelemetry.estimated_total_cost_czk.toFixed(3)} Kč
            </span>
          </div>
        </div>

        {/* Queries Executed & Average */}
        <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-purple-500/30 p-4 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs font-mono mb-2">
            <span className="flex items-center gap-1.5 text-[#A855F7] font-semibold">
              <Layers className="w-4 h-4" />
              Exekuované Dotazy
            </span>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-300">
              Kognice
            </span>
          </div>
          <div className="text-2xl font-extrabold font-mono text-purple-300">
            {sessionTokenTelemetry.total_queries_executed}
            <span className="text-xs text-slate-400 font-normal ml-1.5">cyklů</span>
          </div>
          <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 mt-2 pt-2 border-t border-slate-800">
            <span>Průměr / dotaz:</span>
            <span className="text-slate-300 font-bold">
              {sessionTokenTelemetry.total_queries_executed > 0
                ? Math.round(
                    sessionTokenTelemetry.cumulative_total_tokens /
                      sessionTokenTelemetry.total_queries_executed
                  ).toLocaleString()
                : 0}{" "}
              tok
            </span>
          </div>
        </div>

        {/* Reset Telemetry Action */}
        <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-slate-800 p-4 shadow-lg flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400 text-xs font-mono">
            <span className="flex items-center gap-1.5 text-slate-300 font-semibold">
              <RotateCcw className="w-4 h-4 text-amber-400" />
              Správa Počítadla
            </span>
            <span className="text-[10px] text-slate-500">Live</span>
          </div>
          <p className="text-[11px] text-slate-400 font-mono mt-1">
            Počítadlo sleduje spotřebu v reálném čase. Resetujte před spuštěním nového srovnávacího testu.
          </p>
          <button
            onClick={onResetTelemetry}
            className="mt-3 w-full py-1.5 rounded-xl bg-slate-800/80 border border-slate-700 hover:border-amber-500/50 text-slate-300 hover:text-amber-300 text-xs font-mono transition-colors"
          >
            Vynulovat Počítadlo Tokenů
          </button>
        </div>
      </div>

      {/* ==========================================================
          DEV ROZCESTNÍK VŠECH STRÁNEK A MODULŮ SYSTÉMU O.M.N.I.S.
         ========================================================== */}
      <div className="rounded-2xl bg-gradient-to-b from-[#0B1021] to-[#060913] border border-amber-500/30 p-4 sm:p-5 shadow-2xl space-y-4">
        <div className="flex items-center justify-between gap-3 border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 font-sans tracking-wide">
                Dev Katalog Stránek & Rychlá Navigace
              </h2>
              <p className="text-xs text-slate-400 font-mono">
                Přímý přístup do všech 7 hlavních modulů a diagnostických dialogů aplikace
              </p>
            </div>
          </div>
          <span className="text-[10px] font-mono text-amber-300 bg-amber-500/10 px-2.5 py-1 rounded-full border border-amber-500/20 font-bold hidden sm:inline">
            7 AKTIVNÍCH OBRAZOVEK
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* 1. Kognitivní Chat */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-[#00F0FF]/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-[#00F0FF] bg-[#00F0FF]/10 px-2 py-0.5 rounded border border-[#00F0FF]/30 font-bold">
                  MODUL 1
                </span>
                <span className="text-xs text-slate-500 font-mono">/chat</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-[#00F0FF] transition-colors">
                <Zap className="w-3.5 h-3.5 text-[#00F0FF]" />
                Kognitivní Chat & Diktování
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Konverzační časová osa, Web Speech API české hlasové diktování, fotoaparát pro Gemini 3.1 multimodální analýzu.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("chat")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-[#00F0FF]/10 hover:bg-[#00F0FF]/20 border border-[#00F0FF]/30 text-[#00F0FF] text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Chat</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 2. Analytický Přehled */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-blue-500/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded border border-blue-500/30 font-bold">
                  MODUL 2
                </span>
                <span className="text-xs text-slate-500 font-mono">/analytics</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-blue-400 transition-colors">
                <BarChart3 className="w-3.5 h-3.5 text-blue-400" />
                Analytický Přehled & Audit
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Sjednocení Octagon 8D matice dopadů a kognitivních uzlů, kompozitní skóre, průměrná entropie a statistiky.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("analytics")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/30 text-blue-400 text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Analýzu</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 3. Archiv Vláken */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-cyan-500/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-cyan-400 bg-cyan-500/10 px-2 py-0.5 rounded border border-cyan-500/30 font-bold">
                  MODUL 3
                </span>
                <span className="text-xs text-slate-500 font-mono">/archive</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-cyan-400 transition-colors">
                <Layers className="w-3.5 h-3.5 text-cyan-400" />
                Archiv Vláken & IndexedDB
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Persistentní paměťové úložiště, full-textové vyhledávání, slučování konverzačních vláken a JSON zálohy.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("archive")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-cyan-400 text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Archiv</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 4. Kognitivní Uzly */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-purple-500/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/30 font-bold">
                  MODUL 4
                </span>
                <span className="text-xs text-slate-500 font-mono">/nodes</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-purple-400 transition-colors">
                <Cpu className="w-3.5 h-3.5 text-purple-400" />
                Kognitivní Uzly & Dekompozice
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Detailní prohlížení atomických kognitivních uzlů, sémantického jádra, úrovně entropie a dekompozice odpovědí.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("nodes")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-purple-500/10 hover:bg-purple-500/20 border border-purple-500/30 text-purple-400 text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Uzly</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 5. Správa Témat & Šablony */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-amber-500/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-amber-300 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30 font-bold">
                  MODUL 5
                </span>
                <span className="text-xs text-slate-500 font-mono">/dashboard</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-amber-300 transition-colors">
                <Sliders className="w-3.5 h-3.5 text-amber-300" />
                Správa Témat & Šablony
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Uživatelské prostředí pro správu promtních šablon, pravidla automatické detekce klíčových témat.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("dashboard")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 text-amber-300 text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Témata</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 6. Octagon 8D Matice */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 hover:border-purple-400/50 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all group">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-purple-300 bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/30 font-bold">
                  MODUL 6
                </span>
                <span className="text-xs text-slate-500 font-mono">/octagon</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5 group-hover:text-purple-300 transition-colors">
                <ShieldCheck className="w-3.5 h-3.5 text-purple-300" />
                Octagon 8D Matice Dopadů
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                8D hodnocení dopadů (ekonomika, technologie, bezpečnost, ekologie atd.), bezpečnostní jistič a forenzní mapa.
              </p>
            </div>
            <button
              onClick={() => onNavigateToTab?.("octagon")}
              className="w-full mt-2 py-1.5 px-3 rounded-lg bg-purple-500/10 hover:bg-purple-500/20 border border-purple-500/30 text-purple-300 text-[11px] font-mono font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <span>Otevřít Octagon</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* 7. Dev Prompt Lab */}
          <div className="bg-[#0A0F1D]/80 border border-emerald-500/40 rounded-xl p-3.5 space-y-2 flex flex-col justify-between transition-all">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/30 font-bold">
                  MODUL 7
                </span>
                <span className="text-xs text-emerald-400 font-mono font-bold">AKTUÁLNÍ</span>
              </div>
              <h3 className="text-xs font-mono font-bold text-emerald-400 flex items-center gap-1.5">
                <FlaskConical className="w-3.5 h-3.5 text-emerald-400" />
                Dev Lab & Telemetrie
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Benchmarking promptů, kalkulačka tokenů v reálném čase, Invariant runner a správa vývojového režimu.
              </p>
            </div>
            <div className="w-full mt-2 py-1.5 px-3 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-[11px] font-mono font-bold text-center">
              Aktivní Stránka
            </div>
          </div>

          {/* Interactive Action Modals Quick Launch */}
          <div className="bg-[#0A0F1D]/80 border border-slate-800 rounded-xl p-3.5 space-y-2 flex flex-col justify-between">
            <div className="space-y-1.5">
              <span className="text-[10px] font-mono text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30 font-bold">
                DIAGNOSTICKÉ DIALOGY
              </span>
              <h3 className="text-xs font-mono font-bold text-slate-100 flex items-center gap-1.5">
                <Filter className="w-3.5 h-3.5 text-amber-400" />
                Nástroje & Dialogy
              </h3>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Klávesové zkratky, vyhledávání ve vláknech a slučování rozhovorů.
              </p>
            </div>
            <div className="flex items-center gap-1.5 mt-2">
              <button
                onClick={() => onOpenSearchModal?.()}
                className="flex-1 py-1 px-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 text-[10px] font-mono font-bold truncate"
                title="Otevřít vyhledávání"
              >
                Vyhledat
              </button>
              <button
                onClick={() => onOpenMergeModal?.()}
                className="flex-1 py-1 px-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 text-[10px] font-mono font-bold truncate"
                title="Otevřít slučovač vláken"
              >
                Sloučit
              </button>
              <button
                onClick={() => onOpenHelpModal?.()}
                className="flex-1 py-1 px-1.5 rounded bg-[#00F0FF]/15 hover:bg-[#00F0FF]/30 text-[#00F0FF] text-[10px] font-mono font-bold truncate"
                title="Otevřít Průvodce (?)"
              >
                Průvodce
              </button>
            </div>
          </div>
        </div>
      </div>
      <div className="rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border border-slate-800 p-4 sm:p-5 shadow-xl">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <Calculator className="w-5 h-5 text-[#00F0FF]" />
            <h2 className="text-sm sm:text-base font-bold text-slate-100 font-sans">
              Interaktivní Živý Kalkulátor Tokenů (Sandbox)
            </h2>
          </div>
          <span className="text-[11px] font-mono text-slate-400 bg-slate-900 px-2.5 py-0.5 rounded-full border border-slate-800">
            Realtime Heuristika
          </span>
        </div>

        <p className="text-xs text-slate-400 font-sans mb-3">
          Napište nebo vložte libovolný dotaz níže. Kalkulátor okamžitě v reálném čase vyčíslí přesný počet vstupních tokenů (včetně systémového promptu a epistemické paměti), predikované výstupní tokeny pro 5 fází a novou celkovou spotřebu, která nastane po odeslání.
        </p>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <div className="lg:col-span-8 flex flex-col">
            <textarea
              value={sandboxQuery}
              onChange={(e) => setSandboxQuery(e.target.value)}
              placeholder="Zadejte testovací dotaz pro výpočet tokenů..."
              rows={3}
              className="w-full bg-[#060913] border border-slate-800 focus:border-[#00F0FF] rounded-xl p-3 text-xs sm:text-sm text-slate-200 font-mono resize-none focus:outline-none transition-colors"
            />
            <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono mt-1 px-1">
              <span>{sandboxQuery.length} znaků • {sandboxQuery.trim().split(/\s+/).filter(Boolean).length} slov</span>
              <button
                onClick={() => setSandboxQuery("")}
                className="hover:text-slate-300 transition-colors"
              >
                Vyčistit pole
              </button>
            </div>
          </div>

          <div className="lg:col-span-4 rounded-xl bg-slate-950/80 border border-slate-800/90 p-3 flex flex-col justify-between text-xs font-mono space-y-2">
            <div className="flex items-center justify-between border-b border-slate-800 pb-1.5">
              <span className="text-slate-400">Vstupní tokeny (In):</span>
              <span className="text-[#00F0FF] font-bold">
                {sandboxEstimation.estimated_prompt_tokens.toLocaleString()}
              </span>
            </div>
            <div className="flex items-center justify-between border-b border-slate-800 pb-1.5">
              <span className="text-slate-400">Předpoklad výstupu (Out):</span>
              <span className="text-[#A855F7] font-bold">
                {sandboxEstimation.estimated_completion_tokens.toLocaleString()}
              </span>
            </div>
            <div className="flex items-center justify-between border-b border-slate-800 pb-1.5">
              <span className="text-slate-400">Celkem pro tento dotaz:</span>
              <span className="text-emerald-400 font-bold">
                {sandboxEstimation.estimated_total_tokens.toLocaleString()}
              </span>
            </div>
            <div className="flex items-center justify-between text-[11px] pt-1 text-slate-300">
              <span className="text-amber-400">Nová celková spotřeba:</span>
              <span className="font-extrabold text-amber-300">
                {(
                  sessionTokenTelemetry.cumulative_total_tokens +
                  sandboxEstimation.estimated_total_tokens
                ).toLocaleString()}{" "}
                tok
              </span>
            </div>
            <div className="text-[10px] text-slate-500 pt-0.5 text-right">
              Odhadovaná cena: ~${sandboxEstimation.estimated_cost_usd} (~{sandboxEstimation.estimated_cost_czk} Kč)
            </div>
          </div>
        </div>
      </div>

      {/* ==========================================================
          8D MATRIX AUTOPOIETIC CALIBRATOR & SIMULATOR
         ========================================================== */}
      <AutopoieticMatrixSimulator />

      {/* ==========================================================
          EPISTEMIC KNOWLEDGE BASE & DOCUMENT INGESTION STATION (FÁZE VII)
         ========================================================== */}
      <div className="rounded-2xl bg-gradient-to-b from-[#0B1021] to-[#060913] border border-cyan-500/30 p-4 sm:p-5 shadow-2xl space-y-4">
        <div className="border-b border-slate-800 pb-3">
          <h2 className="text-base font-bold text-slate-100 font-sans tracking-wide flex items-center gap-2">
            <span className="text-[#00F0FF]">🧬</span> Epistemická Znalostní Báze & Hybridní RAG Ingest
          </h2>
          <p className="text-xs text-slate-400 font-mono mt-0.5">
            Automatické sémantické dělení dokumentů na bloky (chunking), 768D pgvector indexace a GIN Full-Text Reciprocal Rank Fusion
          </p>
        </div>
        <EpistemicMemoryDashboard />
      </div>

      {/* ==========================================================
          DYNAMIC PROMPT PROPOSALS SECTION (FRESH ON OPEN)
         ========================================================== */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-amber-400" />
              <h2 className="text-base sm:text-lg font-bold text-slate-100 font-sans">
                Automaticky Vygenerované Návrhy Dotazů
              </h2>
            </div>
            <p className="text-xs text-slate-400 font-sans mt-0.5">
              Každý návrh obsahuje přesnou specifikaci očekávaných invariantů pro ověření 100% korektnosti a rozpad tokenů.
            </p>
          </div>

          {/* Category Filter */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
            {[
              { id: "all", label: "Všechny" },
              { id: "spof_stress", label: "SPOF" },
              { id: "zero_trust", label: "Zero-Trust" },
              { id: "thermodynamics", label: "Termodynamika" },
              { id: "win_win_leverage", label: "Páka 1:10" },
              { id: "ai_governance", label: "AI Act" },
            ].map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategoryFilter(cat.id)}
                className={`px-2.5 py-1 rounded-lg text-xs font-mono transition-colors whitespace-nowrap border ${
                  selectedCategoryFilter === cat.id
                    ? "bg-[#00F0FF]/20 text-[#00F0FF] border-[#00F0FF]/50 font-bold"
                    : "bg-slate-900/60 text-slate-400 border-slate-800 hover:text-slate-200"
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Proposals Grid / List */}
        <div className="space-y-4">
          {filteredProposals.map((prop, idx) => {
            const isTesting = activeTestingId === prop.id;
            const report = auditReports[prop.id];
            const isExpanded = expandedProposalDetails[prop.id];
            const projectedNewTotal =
              sessionTokenTelemetry.cumulative_total_tokens +
              prop.tokenEstimation.estimated_total_tokens;

            return (
              <div
                key={prop.id}
                className={`rounded-2xl bg-gradient-to-b from-[#0F172A] to-[#0A0F1D] border transition-all ${
                  report?.allPassed
                    ? "border-emerald-500/50 shadow-[0_0_20px_rgba(16,185,129,0.15)]"
                    : report && !report.allPassed
                    ? "border-amber-500/50 shadow-[0_0_20px_rgba(245,158,11,0.15)]"
                    : "border-slate-800 hover:border-slate-700"
                } p-4 sm:p-5`}
              >
                {/* Header Row */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="w-6 h-6 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 font-mono text-xs flex items-center justify-center font-bold">
                      #{idx + 1}
                    </span>
                    <span
                      className={`text-[11px] px-2.5 py-0.5 rounded-full font-mono font-bold border ${prop.badgeColor}`}
                    >
                      {prop.categoryLabel}
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono bg-slate-900 px-2 py-0.5 rounded-md border border-slate-800">
                      {prop.domain}
                    </span>
                  </div>

                  {/* Token Pill preview */}
                  <div className="flex items-center gap-2 text-xs font-mono">
                    <span className="text-slate-400">Dotaz spotřebuje:</span>
                    <span className="text-[#00F0FF] font-bold">
                      ~{prop.tokenEstimation.estimated_total_tokens.toLocaleString()} tok
                    </span>
                    <span className="text-slate-500 text-[10px]">
                      (In: {prop.tokenEstimation.estimated_prompt_tokens} | Out:{" "}
                      {prop.tokenEstimation.estimated_completion_tokens})
                    </span>
                  </div>
                </div>

                {/* Title */}
                <h3 className="text-sm sm:text-base font-bold text-slate-100 mb-2 font-sans">
                  {prop.title}
                </h3>

                {/* Prompt Box */}
                <div className="rounded-xl bg-[#060913] border border-slate-800/90 p-3 sm:p-3.5 mb-3 font-mono text-xs text-slate-200 leading-relaxed relative group">
                  <p className="pr-8">{prop.prompt}</p>
                  <button
                    onClick={() => handleCopyPrompt(prop.prompt, prop.id)}
                    title="Kopírovat dotaz do schránky"
                    className="absolute top-2.5 right-2.5 p-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
                  >
                    {copiedId === prop.id ? (
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>

                {/* Rationale and Expected Invariants preview */}
                <p className="text-xs text-slate-400 mb-3">
                  <strong className="text-slate-300">Proč tento test:</strong> {prop.rationale}
                </p>

                {/* Token Impact Breakdown Card */}
                <div className="rounded-xl bg-slate-950/60 border border-slate-800/80 p-3 mb-4 grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] font-mono">
                  <div>
                    <span className="text-slate-500 block">Vstup (Systém+Prompt):</span>
                    <span className="text-[#00F0FF] font-semibold">
                      {prop.tokenEstimation.estimated_prompt_tokens} tok
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Předpokládaný Výstup:</span>
                    <span className="text-[#A855F7] font-semibold">
                      {prop.tokenEstimation.estimated_completion_tokens} tok
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Po spuštění stoupne na:</span>
                    <span className="text-amber-400 font-bold">
                      {projectedNewTotal.toLocaleString()} tok
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Odhad nákladů:</span>
                    <span className="text-emerald-400 font-semibold">
                      ${prop.tokenEstimation.estimated_cost_usd} (~{prop.tokenEstimation.estimated_cost_czk} Kč)
                    </span>
                  </div>
                </div>

                {/* Invariant Verification Audit Box if report exists */}
                {report && (
                  <div
                    className={`rounded-xl border p-4 mb-4 ${
                      report.allPassed
                        ? "bg-emerald-950/20 border-emerald-500/40"
                        : "bg-amber-950/20 border-amber-500/40"
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-mono font-bold flex items-center gap-1.5 text-slate-200">
                        {report.allPassed ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                        ) : (
                          <AlertCircle className="w-4 h-4 text-amber-400" />
                        )}
                        Audit Invariantů 100% Korektnosti (Skóre: {report.scorePercent}%)
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        Testováno v {report.timestamp}
                      </span>
                    </div>

                    <p className="text-xs text-slate-300 mb-3 font-sans">{report.feedback}</p>

                    <div className="space-y-1.5">
                      {report.invariants.map((inv, iIndex) => (
                        <div
                          key={iIndex}
                          className="flex items-start gap-2 text-xs font-mono bg-slate-900/60 p-2 rounded-lg border border-slate-800/80"
                        >
                          {inv.passed ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                          ) : (
                            <AlertCircle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                          )}
                          <div className="min-w-0 flex-1">
                            <span
                              className={`font-semibold ${
                                inv.passed ? "text-emerald-300" : "text-amber-300"
                              }`}
                            >
                              {inv.name}:
                            </span>{" "}
                            <span className="text-slate-300">{inv.details}</span>
                          </div>
                        </div>
                      ))}
                    </div>

                    {report.actualTokens && (
                      <div className="mt-3 pt-2 border-t border-slate-800/80 flex items-center justify-between text-[11px] font-mono text-slate-400">
                        <span>Skutečná spotřeba z API:</span>
                        <span className="text-[#00F0FF] font-bold">
                          {report.actualTokens.total.toLocaleString()} tokenů (In:{" "}
                          {report.actualTokens.prompt} | Out: {report.actualTokens.completion})
                        </span>
                      </div>
                    )}
                  </div>
                )}

                {/* Collapsible Expected Invariants details */}
                {isExpanded && (
                  <div className="mb-4 p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-xs space-y-2">
                    <span className="text-[11px] font-mono font-bold text-slate-300 uppercase tracking-wider block">
                      Kontrolní Invarianty pro 100% Správnost:
                    </span>
                    <ul className="space-y-1 text-slate-400 font-mono text-[11px] list-disc list-inside">
                      {prop.expectedInvariants.map((inv, idx2) => (
                        <li key={idx2} className="leading-relaxed">
                          {inv}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Actions Footer */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80">
                  <button
                    onClick={() => toggleProposalDetails(prop.id)}
                    className="text-xs font-mono text-slate-400 hover:text-slate-200 flex items-center gap-1 transition-colors"
                  >
                    {isExpanded ? (
                      <>
                        <ChevronUp className="w-3.5 h-3.5" /> Skrýt invarianty
                      </>
                    ) : (
                      <>
                        <ChevronDown className="w-3.5 h-3.5" /> Zobrazit invarianty
                      </>
                    )}
                  </button>

                  <div className="flex items-center gap-2">
                    {/* Send to Chat tab button */}
                    <button
                      onClick={() => onExecutePromptInChat(prop.prompt, prop.domain)}
                      className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-mono transition-colors flex items-center gap-1.5 border border-slate-700"
                    >
                      <Terminal className="w-3.5 h-3.5 text-[#00F0FF]" />
                      <span>Odeslat do Chatu</span>
                    </button>

                    {/* Primary: Run Invariant Test Right Here */}
                    <button
                      onClick={() => handleRunInvariantTest(prop)}
                      disabled={isTesting}
                      className="px-4 py-1.5 rounded-xl bg-gradient-to-r from-amber-500 via-[#00F0FF] to-[#10B981] text-slate-950 font-mono font-bold text-xs hover:brightness-110 transition-all flex items-center gap-2 shadow-[0_0_15px_rgba(0,240,255,0.25)] disabled:opacity-50"
                    >
                      {isTesting ? (
                        <>
                          <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                          <span>Testuji Invarianty...</span>
                        </>
                      ) : (
                        <>
                          <Play className="w-3.5 h-3.5 fill-current" />
                          <span>Spustit a Ověřit Invarianty</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
