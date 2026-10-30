import React, { useState, useEffect, useCallback } from "react";
import { 
  Sliders, TrendingUp, Bookmark, Plus, Trash2, Shield, Search, 
  Settings, Info, Calendar, Database, ShieldAlert, Cpu, 
  Activity, Zap, Network, HelpCircle, FileText, CheckCircle, Flame, Tag, RefreshCw
} from "lucide-react";
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from "recharts";

export interface TopicRule {
  id: string;
  topicName: string;
  keyword: string;
}

export const DEFAULT_TOPIC_RULES: TopicRule[] = [
  { id: "tr1", topicName: "Kybernetická Homeostáza", keyword: "homeostáz" },
  { id: "tr2", topicName: "Sémantická Dekonstrukce", keyword: "dekonstrukce" },
  { id: "tr3", topicName: "Adversariální Audit", keyword: "adversari" },
  { id: "tr4", topicName: "Termodynamická Entropie", keyword: "termodynam" },
  { id: "tr5", topicName: "8D Matice Dopadů", keyword: "matice" },
  { id: "tr6", topicName: "Refaktoring Kódu", keyword: "refaktor" },
  { id: "tr7", topicName: "Forenzní Audit Rizik", keyword: "forenz" },
  { id: "tr8", topicName: "Cloud SQL Architektura", keyword: "databáz" },
];

// Memoized Templates List Component
const TemplateListMemo = React.memo<{
  templates: typeof PRESET_TEMPLATES;
  onUseTemplate: (text: string) => void;
  onDeleteTemplate: (id: string) => void;
}>(({ templates, onUseTemplate, onDeleteTemplate }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
      {templates.map(tpl => (
        <div key={tpl.id} className="bg-[#0A0F1D]/50 border border-slate-900 rounded-lg p-3 space-y-2 flex flex-col justify-between hover:border-[#00F0FF]/30 transition-all">
          <div className="space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-mono text-[#00F0FF] bg-[#00F0FF]/10 px-1.5 py-0.5 rounded border border-[#00F0FF]/20 font-bold">
                {tpl.category}
              </span>
              <button 
                onClick={() => onDeleteTemplate(tpl.id)}
                className="text-slate-500 hover:text-red-400 p-1 transition-colors"
                title="Smazat šablonu"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <h5 className="text-xs font-mono font-bold text-slate-200">{tpl.title}</h5>
            <p className="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">{tpl.text}</p>
          </div>
          <button
            onClick={() => onUseTemplate(tpl.text)}
            className="w-full bg-[#00F0FF]/10 hover:bg-[#00F0FF]/20 border border-[#00F0FF]/30 text-[#00F0FF] text-[11px] font-mono font-bold py-1.5 rounded transition-all mt-2"
          >
            Vložit do chatu
          </button>
        </div>
      ))}
    </div>
  );
});

// Memoized Topic Rules List Component
const TopicRulesMemo = React.memo<{
  topicRules: TopicRule[];
  onDeleteRule: (id: string) => void;
}>(({ topicRules, onDeleteRule }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2.5">
      {topicRules.map(rule => (
        <div key={rule.id} className="bg-[#0A0F1D]/50 border border-slate-900 rounded-lg p-2.5 flex items-center justify-between gap-2 hover:border-[#A855F7]/30 transition-all">
          <div className="truncate">
            <span className="text-[10px] font-mono text-purple-400 block truncate font-bold">{rule.topicName}</span>
            <span className="text-[9px] font-mono text-slate-400">klíč: "{rule.keyword}"</span>
          </div>
          <button
            onClick={() => onDeleteRule(rule.id)}
            className="text-slate-500 hover:text-red-400 p-1 transition-colors flex-shrink-0"
            title="Smazat pravidlo"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
});
const PRESET_TEMPLATES = [
  {
    id: "t1",
    title: "Sémantická Dekonstrukce",
    text: "Analyzuj následující text z pohledu transdisciplinárních zranitelností a rozlož jej na základní ontologické komponenty.",
    category: "Analýza"
  },
  {
    id: "t2",
    title: "Simulace Adversariálního Útoku",
    text: "Otestuj tento bezpečnostní protokol na odolnost proti sociálnímu inženýrství a navrhni protiopatření.",
    category: "Red-Team"
  },
  {
    id: "t3",
    title: "Kybernetický Audit Systému",
    text: "Proveď audit zpětnovazebních smyček v tomto systému a identifikuj místa, kde hrozí kmitání nebo nestabilita.",
    category: "Kybernetika"
  }
];

interface UserDashboardProps {
  onUseTemplate: (templateText: string) => void;
  showToast: (msg: string, type: "success" | "error" | "info") => void;
}

export const UserDashboard: React.FC<UserDashboardProps> = ({ onUseTemplate, showToast }) => {
  // --- STATE 1: PREFEROVANÉ DOMÉNY ---
  const [preferredDomains, setPreferredDomains] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem("omnis_preferred_domains");
      return saved ? JSON.parse(saved) : ["SYSTEMS_INTELLIGENCE", "CYBERNETICS", "ECONOMIC_THEORY"];
    } catch {
      return ["SYSTEMS_INTELLIGENCE", "CYBERNETICS", "ECONOMIC_THEORY"];
    }
  });

  useEffect(() => {
    localStorage.setItem("omnis_preferred_domains", JSON.stringify(preferredDomains));
  }, [preferredDomains]);

  const toggleDomain = (domain: string) => {
    setPreferredDomains(prev => 
      prev.includes(domain) ? prev.filter(d => d !== domain) : [...prev, domain]
    );
    showToast(`Doména ${domain} byla upravena.`, "info");
  };

  // --- STATE 2: STATISTIKY VYUŽITÍ TOKENŮ ---
  const [timePeriod, setTimePeriod] = useState<"today" | "7d" | "30d">("7d");
  const [tokenStats, setTokenStats] = useState({
    promptTokens: 14250,
    completionTokens: 8410,
    queriesCount: 38,
    estimatedCost: 0.125
  });

  useEffect(() => {
    // Simulate slight variations based on period for visual fidelity
    if (timePeriod === "today") {
      setTokenStats({ promptTokens: 2150, completionTokens: 1100, queriesCount: 5, estimatedCost: 0.018 });
    } else if (timePeriod === "7d") {
      setTokenStats({ promptTokens: 14250, completionTokens: 8410, queriesCount: 38, estimatedCost: 0.125 });
    } else {
      setTokenStats({ promptTokens: 62400, completionTokens: 38120, queriesCount: 146, estimatedCost: 0.548 });
    }
  }, [timePeriod]);

  // --- STATE 3: ŠABLONY PROMPTŮ ---
  const [templates, setTemplates] = useState<typeof PRESET_TEMPLATES>(() => {
    try {
      const saved = localStorage.getItem("omnis_custom_templates");
      return saved ? JSON.parse(saved) : PRESET_TEMPLATES;
    } catch {
      return PRESET_TEMPLATES;
    }
  });

  const [newTitle, setNewTitle] = useState("");
  const [newText, setNewText] = useState("");
  const [newCategory, setNewCategory] = useState("Vlastní");

  useEffect(() => {
    localStorage.setItem("omnis_custom_templates", JSON.stringify(templates));
  }, [templates]);

  const addTemplate = () => {
    if (!newTitle.trim() || !newText.trim()) {
      showToast("Název i text šablony jsou povinné.", "error");
      return;
    }
    const nt = {
      id: "t-" + Date.now(),
      title: newTitle,
      text: newText,
      category: newCategory
    };
    setTemplates(prev => [...prev, nt]);
    setNewTitle("");
    setNewText("");
    showToast("Nová šablona úspěšně uložena.", "success");
  };

  const deleteTemplate = (id: string) => {
    setTemplates(prev => prev.filter(t => t.id !== id));
    showToast("Šablona smazána.", "info");
  };

  // --- STATE 3.5: KLÍČOVÁ SLOVA PRO AUTOMATICKOU DETEKCI TÉMAT ---
  const [topicRules, setTopicRules] = useState<TopicRule[]>(() => {
    try {
      const saved = localStorage.getItem("omnis_custom_topic_keywords");
      return saved ? JSON.parse(saved) : DEFAULT_TOPIC_RULES;
    } catch {
      return DEFAULT_TOPIC_RULES;
    }
  });

  const [newTopicName, setNewTopicName] = useState("");
  const [newTopicKeyword, setNewTopicKeyword] = useState("");

  useEffect(() => {
    localStorage.setItem("omnis_custom_topic_keywords", JSON.stringify(topicRules));
  }, [topicRules]);

  const addTopicRule = () => {
    if (!newTopicName.trim() || !newTopicKeyword.trim()) {
      showToast("Název témat i klíčové slovo jsou povinné.", "error");
      return;
    }
    const newRule: TopicRule = {
      id: "tr-" + Date.now(),
      topicName: newTopicName.trim(),
      keyword: newTopicKeyword.trim().toLowerCase()
    };
    setTopicRules(prev => [...prev, newRule]);
    setNewTopicName("");
    setNewTopicKeyword("");
    showToast(`Pravidlo pro téma "${newRule.topicName}" bylo úspěšně přidáno.`, "success");
  };

  const deleteTopicRule = (id: string) => {
    setTopicRules(prev => prev.filter(r => r.id !== id));
    showToast("Detekční pravidlo smazáno.", "info");
  };

  const resetTopicRules = () => {
    setTopicRules(DEFAULT_TOPIC_RULES);
    showToast("Pravidla detekce témat byla resetována na výchozí hodnoty.", "info");
  };

  // --- STATE 4: NAVRHOVANÉ STRÁNKY (O.M.N.I.S. LABS SEKCÍCH) ---
  const [activeLabTab, setActiveLabTab] = useState<"sandbox" | "ledger" | "graph" | "autopoiesis">("sandbox");

  // Sandbox Shield States
  const [shields, setShields] = useState([
    { id: "s1", name: "Semantic Boundary Guard", active: true, level: 85 },
    { id: "s2", name: "Adversarial Inoculation Shield", active: true, level: 90 },
    { id: "s3", name: "Zero-Simulation Fallback Firewall", active: false, level: 50 },
    { id: "s4", name: "Autopoietic Safety Anchor", active: true, level: 95 }
  ]);

  const toggleShield = (id: string) => {
    setShields(prev => prev.map(s => s.id === id ? { ...s, active: !s.active } : s));
    showToast("Konfigurace obranného štítu byla aktualizována.", "info");
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 p-4 sm:p-6 text-slate-100 max-w-7xl mx-auto animate-in fade-in duration-500 pb-20">
      {/* HEADER SUMMARY PANEL */}
      <div className="lg:col-span-12 bg-[#050811]/70 border border-[#00F0FF]/20 rounded-2xl p-5 shadow-[0_0_15px_rgba(0,240,255,0.05)] flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1">
          <h2 className="text-xl font-bold font-mono text-[#00F0FF] flex items-center gap-2">
            <Settings className="w-5 h-5 animate-spin-slow" />
            UŽIVATELSKÝ ŘÍDÍCÍ PANEL
          </h2>
          <p className="text-xs text-slate-400 font-mono">
            Správa preferencí, telemetrie tokenů a uložených sémantických šablon O.M.N.I.S.
          </p>
        </div>
        <div className="flex items-center gap-2 bg-[#0A0F1D]/80 px-3 py-1.5 rounded-xl border border-slate-800 self-start md:self-auto text-xs font-mono">
          <span className="w-2 h-2 rounded-full bg-[#10B981] animate-pulse"></span>
          <span>AUTENTIZOVANÝ UŽIVATEL: SYSETOMU</span>
        </div>
      </div>

      {/* LEFT COLUMN: PREFERENCES & TOKENS */}
      <div className="lg:col-span-5 space-y-6">
        
        {/* PREFEROVANÉ DOMÉNY */}
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4">
          <h3 className="text-sm font-bold font-mono text-[#00F0FF] flex items-center gap-2 border-b border-slate-800/80 pb-2">
            <Sliders className="w-4 h-4 text-[#00F0FF]" />
            PREFEROVANÉ ONTOLOGICKÉ DOMÉNY
          </h3>
          <p className="text-xs text-slate-400 font-mono">
            Vyberte domény, které budou upřednostněny při generování transdisciplinárního grafu.
          </p>
          <div className="grid grid-cols-1 gap-2 pt-2">
            {[
              { id: "SYSTEMS_INTELLIGENCE", label: "Systémová kybernetika", color: "border-blue-500/30" },
              { id: "CYBERNETICS", label: "Zpětnovazební smyčky", color: "border-purple-500/30" },
              { id: "ECONOMIC_THEORY", label: "Ekonomické & herní moduly", color: "border-amber-500/30" },
              { id: "REGENERATIVE_ECOLOGY", label: "Regenerativní ekologie", color: "border-emerald-500/30" },
              { id: "REGULATORY_LAW", label: "Právo & zero-trust pravidla", color: "border-rose-500/30" },
              { id: "COGNITIVE_PSYCHOLOGY", label: "Kognitivní psychologie", color: "border-pink-500/30" }
            ].map(domain => {
              const active = preferredDomains.includes(domain.id);
              return (
                <button
                  key={domain.id}
                  onClick={() => toggleDomain(domain.id)}
                  className={`flex items-center justify-between p-2.5 rounded-xl border text-left transition-all duration-300 min-h-[44px] ${
                    active 
                      ? "bg-[#00F0FF]/10 border-[#00F0FF] text-[#00F0FF]" 
                      : "bg-[#070B18]/50 border-slate-800 hover:border-slate-700 text-slate-400 hover:text-slate-200"
                  }`}
                >
                  <span className="text-xs font-mono font-bold">{domain.label}</span>
                  <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                    active ? "border-[#00F0FF] bg-[#00F0FF]/25" : "border-slate-600 bg-transparent"
                  }`}>
                    {active && <span className="w-1.5 h-1.5 rounded-full bg-[#00F0FF]" />}
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* STATISTIKY TOKENŮ */}
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
            <h3 className="text-sm font-bold font-mono text-[#10B981] flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-[#10B981]" />
              STATISTIKY VYUŽITÍ TOKENŮ
            </h3>
            <div className="flex bg-[#0A0F1D] p-1 rounded-lg border border-slate-800 text-[10px] font-mono">
              {(["today", "7d", "30d"] as const).map(p => (
                <button
                  key={p}
                  onClick={() => setTimePeriod(p)}
                  className={`px-2 py-1 rounded transition-colors ${
                    timePeriod === p ? "bg-[#10B981]/20 text-[#10B981] font-bold" : "text-slate-400 hover:text-slate-200"
                  }`}
                >
                  {p.toUpperCase()}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="bg-[#070B18]/60 p-3 rounded-xl border border-slate-800/80 text-center">
              <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">Prompt Tokens</span>
              <span className="text-lg font-mono font-bold text-[#00F0FF] block mt-1">
                {tokenStats.promptTokens.toLocaleString()}
              </span>
            </div>
            <div className="bg-[#070B18]/60 p-3 rounded-xl border border-slate-800/80 text-center">
              <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">Completion Tokens</span>
              <span className="text-lg font-mono font-bold text-[#A855F7] block mt-1">
                {tokenStats.completionTokens.toLocaleString()}
              </span>
            </div>
            <div className="bg-[#070B18]/60 p-3 rounded-xl border border-slate-800/80 text-center">
              <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">Dotazy celkem</span>
              <span className="text-lg font-mono font-bold text-[#10B981] block mt-1">
                {tokenStats.queriesCount}
              </span>
            </div>
            <div className="bg-[#070B18]/60 p-3 rounded-xl border border-slate-800/80 text-center">
              <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider block">Est. Náklady (USD)</span>
              <span className="text-lg font-mono font-bold text-amber-400 block mt-1">
                ${tokenStats.estimatedCost.toFixed(3)}
              </span>
            </div>
          </div>

          {/* Visual Progress/Health of token budget */}
          <div className="space-y-1.5 bg-[#070B18]/40 p-3 rounded-xl border border-slate-900">
            <div className="flex justify-between text-[10px] font-mono">
              <span className="text-slate-400">Čerpání rozpočtu (limit $5.00)</span>
              <span className="text-[#10B981]">{(tokenStats.estimatedCost / 5.0 * 100).toFixed(2)}%</span>
            </div>
            <div className="w-full h-1.5 bg-slate-900 rounded-full overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-[#10B981] to-[#00F0FF] rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, (tokenStats.estimatedCost / 5.0) * 100)}%` }}
              ></div>
            </div>
          </div>
        </div>

        {/* RECHARTS DONUT CHART: ÚSPĚŠNÉ VS ZAMKNUTÉ DOTAZY */}
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
            <h3 className="text-sm font-bold font-mono text-[#00F0FF] flex items-center gap-2">
              <Activity className="w-4 h-4 text-[#00F0FF]" />
              POMĚR ÚSPĚŠNÝCH A ZAMKNUTÝCH DOTAZŮ
            </h3>
            <span className="text-[10px] text-slate-400 font-mono">Reálný čas</span>
          </div>

          <div className="h-48 w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={[
                    { name: "Úspěšně zpracované", value: Math.max(tokenStats.queriesCount - 4, 1), color: "#00F0FF" },
                    { name: "Vyžadující autorizaci (LOCKED)", value: 4, color: "#F59E0B" }
                  ]}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={75}
                  paddingAngle={4}
                  dataKey="value"
                >
                  <Cell key="cell-0" fill="#00F0FF" stroke="#00F0FF" strokeWidth={1} />
                  <Cell key="cell-1" fill="#F59E0B" stroke="#F59E0B" strokeWidth={1} />
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: "#0A0F1D", borderColor: "#1E293B", borderRadius: "8px", fontSize: "11px", fontFamily: "monospace" }}
                  itemStyle={{ color: "#E2E8F0" }}
                />
                <Legend 
                  formatter={(value) => <span className="text-[11px] font-mono text-slate-300">{value}</span>}
                  layout="horizontal" 
                  verticalAlign="bottom" 
                  align="center"
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

      </div>

      {/* RIGHT COLUMN: SAVED PROMPT TEMPLATES */}
      <div className="lg:col-span-7 space-y-6">
        
        {/* ŠABLONY PROMPTŮ */}
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4">
          <h3 className="text-sm font-bold font-mono text-[#A855F7] flex items-center gap-2 border-b border-slate-800/80 pb-2">
            <Bookmark className="w-4 h-4 text-[#A855F7]" />
            OBLÍBENÉ PROMPTNÍ ŠABLONY
          </h3>

          <TemplateListMemo
            templates={templates}
            onUseTemplate={(text) => {
              onUseTemplate(text);
              showToast("Šablona byla vložena do chatu.", "success");
            }}
            onDeleteTemplate={deleteTemplate}
          />

          {/* Form to add custom template */}
          <div className="bg-[#070B18]/80 p-3.5 rounded-xl border border-slate-800/80 space-y-3">
            <span className="text-[10px] font-mono font-bold text-purple-400 block uppercase">
              Uložit novou šablonu
            </span>
            <div className="grid grid-cols-2 gap-2">
              <input 
                type="text" 
                placeholder="Název šablony" 
                value={newTitle}
                onChange={e => setNewTitle(e.target.value)}
                className="col-span-1 bg-[#0A0F1D] border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
              />
              <input 
                type="text" 
                placeholder="Kategorie (např. Audit)" 
                value={newCategory}
                onChange={e => setNewCategory(e.target.value)}
                className="col-span-1 bg-[#0A0F1D] border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
              />
            </div>
            <textarea 
              placeholder="Zadejte text sémantického promptu..."
              value={newText}
              onChange={e => setNewText(e.target.value)}
              rows={2}
              className="w-full bg-[#0A0F1D] border border-slate-800 rounded-lg p-2 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono resize-none"
            />
            <button 
              onClick={addTemplate}
              className="w-full flex items-center justify-center gap-1.5 bg-[#A855F7] hover:bg-[#A855F7]/90 text-white font-mono text-xs font-bold py-2 rounded-lg transition-colors min-h-[36px]"
            >
              <Plus className="w-3.5 h-3.5" />
              ULOŽIT DO KNIHOVNY
            </button>
          </div>
        </div>

        {/* VLASTNÍ KLÍČOVÁ SLOVA PRO DETEKCI TÉMAT */}
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
            <h3 className="text-sm font-bold font-mono text-[#00F0FF] flex items-center gap-2">
              <Tag className="w-4 h-4 text-[#00F0FF]" />
              AUTOMATICKÁ DETEKCE TÉMAT (KLÍČOVÁ SLOVA)
            </h3>
            <button
              onClick={resetTopicRules}
              title="Resetovat pravidla detekce na výchozí"
              className="text-[10px] font-mono text-slate-400 hover:text-cyan-400 flex items-center gap-1 bg-slate-900/60 px-2 py-1 rounded border border-slate-800 hover:border-cyan-500/30 transition-all"
            >
              <RefreshCw className="w-3 h-3" />
              Resetovat
            </button>
          </div>

          <p className="text-xs text-slate-400 font-mono">
            Definujte pravidla pro automatickou detekci klíčových témat v chatu. Pokud zpráva obsahuje definované klíčové slovo, O.M.N.I.S. navrhne uložení téma jako šablonu.
          </p>

          <TopicRulesMemo
            topicRules={topicRules}
            onDeleteRule={deleteTopicRule}
          />

          {/* Form to add custom topic rule */}
          <div className="bg-[#070B18]/80 p-3 rounded-xl border border-slate-800 space-y-2">
            <span className="text-[10px] font-mono font-bold text-[#00F0FF] block uppercase">
              Přidat nové detekční pravidlo
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <input 
                type="text" 
                placeholder="Název témat (např. Blockchain Audit)" 
                value={newTopicName}
                onChange={e => setNewTopicName(e.target.value)}
                className="bg-slate-900 border border-slate-800 focus:border-[#00F0FF] rounded-lg px-3 py-1.5 text-xs font-mono text-slate-200 outline-none"
              />
              <input 
                type="text" 
                placeholder="Klíčové slovo (např. blockchain)" 
                value={newTopicKeyword}
                onChange={e => setNewTopicKeyword(e.target.value)}
                className="bg-slate-900 border border-slate-800 focus:border-[#00F0FF] rounded-lg px-3 py-1.5 text-xs font-mono text-slate-200 outline-none"
              />
            </div>
            <button 
              onClick={addTopicRule}
              className="w-full py-2 bg-[#00F0FF]/15 hover:bg-[#00F0FF]/25 border border-[#00F0FF]/40 text-[#00F0FF] text-xs font-mono font-bold rounded-lg transition-all flex items-center justify-center gap-1.5"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>PŘIDAT DETEKČNÍ PRAVIDLO</span>
            </button>
          </div>
        </div>

      </div>

      {/* --- SECTION 5: THE SUGGESTED O.M.N.I.S. LABS PAGES (EXPERIMENTALLY IMPLEMENTED FOR EVALUATION) --- */}
      <div className="lg:col-span-12 mt-4 space-y-4">
        
        <div className="bg-[#050811]/50 border border-slate-800/80 rounded-2xl p-5 space-y-4 shadow-[0_0_20px_rgba(168,85,247,0.03)]">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800/80 pb-3">
            <div className="space-y-1">
              <h3 className="text-sm font-bold font-mono text-amber-400 flex items-center gap-2">
                <Flame className="w-4 h-4 text-amber-400 animate-pulse" />
                O.M.N.I.S. LABS & EXPERIMENTÁLNÍ FUNKCE (DEV/PREVIEW)
              </h3>
              <p className="text-[11px] text-slate-400 font-mono">
                Experimentální kognitivní podstránky vytvořené k vyhodnocení. Klikněte a otestujte jejich živé chování.
              </p>
            </div>
            
            {/* Lab Tabs Selector */}
            <div className="flex bg-[#0A0F1D] p-1 rounded-xl border border-slate-800 text-[10px] font-mono font-bold overflow-x-auto max-w-full">
              {[
                { id: "sandbox", label: "🛡️ SHIELD CONFIG" },
                { id: "ledger", label: "📊 COGNITION LOG" },
                { id: "graph", label: "🌐 ONTOLOGY GRAPH" },
                { id: "autopoiesis", label: "🧬 AUTOPOIESIS" }
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setActiveLabTab(tab.id as any)}
                  className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition-all ${
                    activeLabTab === tab.id 
                      ? "bg-amber-500/15 text-amber-400 border border-amber-500/30 font-bold" 
                      : "text-slate-400 hover:text-slate-200"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {/* 1. SHIELD CONFIGURATOR (RED-TEAM SANDBOX) */}
          {activeLabTab === "sandbox" && (
            <div className="space-y-4 animate-in fade-in duration-300">
              <div className="bg-amber-500/5 border border-amber-500/10 p-3.5 rounded-xl flex items-start gap-2.5 text-xs text-amber-200 font-mono">
                <Shield className="w-4 h-4 flex-shrink-0 text-amber-400 mt-0.5" />
                <p>
                  <strong>Red-Team Sandbox:</strong> Aktivujte jednotlivé vrstvy štítů pro detekci jailbreaků, adversarial útoků a sémantických dezinformací.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {shields.map(s => (
                  <div key={s.id} className="bg-[#070B18]/70 border border-slate-800 rounded-xl p-3.5 flex items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className={`w-2 h-2 rounded-full ${s.active ? "bg-[#10B981] animate-pulse" : "bg-red-500"}`}></span>
                        <h4 className="text-xs font-mono font-bold text-slate-100">{s.name}</h4>
                      </div>
                      <p className="text-[10px] font-mono text-slate-400">Efektivita filtru: {s.level}%</p>
                    </div>
                    <button
                      onClick={() => toggleShield(s.id)}
                      className={`px-3 py-1.5 rounded-lg font-mono text-[10px] font-bold border transition-all ${
                        s.active 
                          ? "bg-[#10B981]/15 text-[#10B981] border-[#10B981]/30 hover:bg-[#10B981]/25" 
                          : "bg-red-500/15 text-red-400 border-red-500/30 hover:bg-red-500/25"
                      }`}
                    >
                      {s.active ? "AKTIVNÍ" : "VYPNUTO"}
                    </button>
                  </div>
                ))}
              </div>

              {/* Simulation triggers */}
              <div className="bg-[#070B18]/50 border border-slate-800 rounded-xl p-4 space-y-3">
                <h4 className="text-xs font-mono font-bold text-slate-200 flex items-center gap-1.5">
                  <Search className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
                  Simulace Kybernetického Útoku (Adversarial Sandbox)
                </h4>
                <p className="text-[10px] font-mono text-slate-400">
                  Otestujte odolnost sémantických štítů s fiktivním pokusem o obcházení LLM pravidel.
                </p>
                <div className="flex flex-wrap gap-2 pt-1">
                  <button 
                    onClick={() => {
                      const activeShields = shields.filter(s => s.active).length;
                      if (activeShields >= 3) {
                        showToast("Simulace útoku ÚSPĚŠNĚ ZABLOKOVÁNA. Štít Semantic Guard zachytil anomálii.", "success");
                      } else {
                        showToast("VAROVÁNÍ: Útok částečně uspěl. Aktivujte více sémantických štítů!", "error");
                      }
                    }}
                    className="bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 text-[10px] font-mono px-3 py-1.5 rounded-lg transition-all"
                  >
                    Simulovat Sémantický Jailbreak
                  </button>
                  <button 
                    onClick={() => {
                      showToast("Vstřikování znečištěných dat zablokováno Inoculation modulem.", "success");
                    }}
                    className="bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-[10px] font-mono px-3 py-1.5 rounded-lg transition-all"
                  >
                    Simulovat Data Poisoning
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* 2. COGNITIVE PIPELINE LEDGER */}
          {activeLabTab === "ledger" && (
            <div className="space-y-4 animate-in fade-in duration-300">
              <div className="bg-[#070B18]/70 border border-slate-800 rounded-xl p-4 space-y-3">
                <h4 className="text-xs font-mono font-bold text-slate-200 flex items-center justify-between border-b border-slate-800 pb-2">
                  <span className="flex items-center gap-1.5">
                    <FileText className="w-4 h-4 text-purple-400" />
                    Kniha kognitivních procesů (Audit Trail)
                  </span>
                  <span className="text-[9px] text-slate-400">Živá fronta dekonstrukcí</span>
                </h4>

                <div className="space-y-3 text-xs font-mono">
                  {[
                    { id: "L-9281", time: "14:09:21", query: "Kritický test integrity systému při výpadku LLM", status: "Processed", domain: "CYBERNETICS", nodesChecked: 8 },
                    { id: "L-9280", time: "13:58:12", query: "Analýza rizik zranitelnosti dodavatelského řetězce", status: "Processed", domain: "SYSTEMS_INTELLIGENCE", nodesChecked: 5 },
                    { id: "L-9279", time: "13:42:01", query: "Optimalizace sémantického štítu proti jailbreaku", status: "Secure", domain: "REGULATORY_LAW", nodesChecked: 6 }
                  ].map(log => (
                    <div key={log.id} className="bg-[#0A0F1D]/50 border border-slate-900 rounded-lg p-2.5 flex flex-col md:flex-row md:items-center justify-between gap-2 hover:bg-purple-500/5 transition-all">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] text-slate-500">#{log.id}</span>
                          <span className="text-[10px] text-slate-400 font-bold">{log.time}</span>
                          <span className="px-1.5 py-0.5 rounded text-[8px] bg-slate-800 text-slate-300 border border-slate-700">{log.domain}</span>
                        </div>
                        <p className="text-xs text-slate-300 truncate max-w-md">{log.query}</p>
                      </div>
                      <div className="flex items-center gap-3 self-end md:self-auto">
                        <span className="text-[10px] text-slate-400">{log.nodesChecked} kognitivních bodů</span>
                        <span className="flex items-center gap-1 text-[#10B981] text-[10px] font-bold">
                          <CheckCircle className="w-3.5 h-3.5 text-[#10B981]" />
                          OK
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* 3. KNOWLEDGE GRAPH & ONTOLOGY EXPLORER */}
          {activeLabTab === "graph" && (
            <div className="space-y-4 animate-in fade-in duration-300">
              <div className="bg-[#070B18]/70 border border-slate-800 rounded-xl p-4 space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <h4 className="text-xs font-mono font-bold text-slate-200 flex items-center gap-1.5">
                    <Network className="w-4 h-4 text-[#00F0FF]" />
                    Průzkumník Ontologické Sítě
                  </h4>
                  <span className="text-[9px] text-[#00F0FF] font-mono animate-pulse">SÍŤ AKTIVNÍ</span>
                </div>
                
                {/* Visual conceptual network representation */}
                <div className="relative w-full h-44 bg-[#0A0F1D]/60 rounded-xl border border-slate-900 flex items-center justify-center overflow-hidden">
                  <div className="absolute inset-0 bg-grid-slate-800 opacity-20"></div>
                  
                  {/* Floating abstract network nodes */}
                  <div className="absolute w-2.5 h-2.5 rounded-full bg-[#00F0FF] animate-ping" style={{ top: '30%', left: '40%' }}></div>
                  <div className="absolute w-2 h-2 rounded-full bg-[#00F0FF]" style={{ top: '30%', left: '40%' }}></div>
                  <span className="absolute text-[9px] font-mono text-slate-400" style={{ top: '35%', left: '36%' }}>CYBERNETICS</span>
                  
                  <div className="absolute w-2 h-2 rounded-full bg-[#A855F7]" style={{ top: '65%', left: '20%' }}></div>
                  <span className="absolute text-[9px] font-mono text-slate-400" style={{ top: '70%', left: '15%' }}>PSYCHOLOGY</span>
                  
                  <div className="absolute w-2 h-2 rounded-full bg-amber-400" style={{ top: '50%', left: '75%' }}></div>
                  <span className="absolute text-[9px] font-mono text-slate-400" style={{ top: '55%', left: '71%' }}>ECONOMIC</span>
                  
                  <div className="absolute w-2 h-2 rounded-full bg-emerald-400" style={{ top: '20%', left: '60%' }}></div>
                  <span className="absolute text-[9px] font-mono text-slate-400" style={{ top: '25%', left: '56%' }}>ECOLOGY</span>

                  {/* SVG connecting lines concept */}
                  <svg className="absolute inset-0 w-full h-full opacity-30 pointer-events-none">
                    <line x1="40%" y1="30%" x2="20%" y2="65%" stroke="#00F0FF" strokeWidth="1" />
                    <line x1="40%" y1="30%" x2="60%" y2="20%" stroke="#00F0FF" strokeWidth="1" />
                    <line x1="60%" y1="20%" x2="75%" y2="50%" stroke="#00F0FF" strokeWidth="1" />
                    <line x1="20%" y1="65%" x2="75%" y2="50%" stroke="#00F0FF" strokeWidth="1" />
                  </svg>

                  <span className="absolute bottom-2 text-[9px] font-mono text-slate-500 uppercase tracking-widest animate-pulse">
                    Klikněte na uzel v hlavním transdisciplinárním oktagonu pro detailní filtraci.
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* 4. AUTONOMOUS AUTOPOIESIS CONTROL ROOM */}
          {activeLabTab === "autopoiesis" && (
            <div className="space-y-4 animate-in fade-in duration-300">
              <div className="bg-[#070B18]/70 border border-slate-800 rounded-xl p-4 space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <h4 className="text-xs font-mono font-bold text-slate-200 flex items-center gap-1.5">
                    <Cpu className="w-4 h-4 text-emerald-400 animate-pulse" />
                    Autopoiesis & Homeostáze Systému
                  </h4>
                  <span className="px-2 py-0.5 rounded text-[8px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
                    STABILNÍ REŽIM
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="bg-[#0A0F1D]/50 border border-slate-900 p-3 rounded-lg space-y-1.5">
                    <span className="text-[10px] text-slate-400 block font-mono">Homeostatický Index</span>
                    <div className="flex items-center gap-2">
                      <span className="text-base font-bold font-mono text-emerald-400">98.42%</span>
                      <span className="text-[9px] text-emerald-500 font-mono">▲ 0.12%</span>
                    </div>
                  </div>
                  <div className="bg-[#0A0F1D]/50 border border-slate-900 p-3 rounded-lg space-y-1.5">
                    <span className="text-[10px] text-slate-400 block font-mono">Sémantická Mutace</span>
                    <div className="flex items-center gap-2">
                      <span className="text-base font-bold font-mono text-purple-400">Gen-4</span>
                      <span className="text-[9px] text-slate-500 font-mono">Generováno dnes</span>
                    </div>
                  </div>
                  <div className="bg-[#0A0F1D]/50 border border-slate-900 p-3 rounded-lg space-y-1.5">
                    <span className="text-[10px] text-slate-400 block font-mono">Vektorové vazby</span>
                    <div className="flex items-center gap-2">
                      <span className="text-base font-bold font-mono text-[#00F0FF]">1,420</span>
                      <span className="text-[9px] text-[#00F0FF] font-mono">Online</span>
                    </div>
                  </div>
                </div>

                {/* Self-Correction simulation */}
                <button
                  onClick={() => {
                    showToast("Spouštím re-evaluaci vlastních obranných promptů. Homeostatický index obnoven.", "success");
                  }}
                  className="w-full bg-emerald-500/15 hover:bg-emerald-500/25 border border-emerald-500/30 hover:border-emerald-500 text-emerald-400 text-xs font-mono font-bold py-2 rounded-lg transition-all min-h-[36px]"
                >
                  SPUSTIT AUTOMATICKOU SAMO-OPRAVU SYSTÉMU (AUTOPOESIS RUN)
                </button>
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
