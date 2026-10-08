import React, { useState, useMemo, useEffect, useRef } from "react";
import { 
  Brain, Sparkles, Activity, ArrowRight, GitCommit, ShieldCheck, Cpu, Network, 
  Zap, CheckCircle2, ChevronDown, ChevronRight, Search, Share2, BookOpen, AlertTriangle, XCircle, RotateCcw,
  Sliders, Layers, Scale, Database, Target, Compass
} from "lucide-react";
import { MessageItem } from "./MessageBubble";
import { 
  OMNIS_8D_KNOWLEDGE_BASE, 
  OMNIS_8D_KNOWLEDGE_EDGES, 
  OMNIS_DOMAINS,
  defaultCsrGraph,
  defaultGlobalHnswIndex,
  evaluateRefusalLadder, 
  calculateShannonEntropy,
  calculateCausalDoIntervention,
  KnowledgeNode
} from "../omnisEngine";
import { RefusalLadderBadge } from "./RefusalLadderBadge";
import { MonteCarloRiskInspector } from "./MonteCarloRiskInspector";

interface CognitiveNodesDashboardProps {
  messages: MessageItem[];
  onTriggerDeepDive: (msg: MessageItem) => void;
  onGoToChat: () => void;
  userRole?: "ADMIN_OPERATOR" | "STANDARD_USER";
}

export const CognitiveNodesDashboard: React.FC<CognitiveNodesDashboardProps> = ({
  messages,
  onTriggerDeepDive,
  onGoToChat,
  userRole = "ADMIN_OPERATOR"
}) => {
  const assistantMessages = messages.filter((m) => m.role === "assistant");
  const lastAssistantMsg = assistantMessages[assistantMessages.length - 1];

  // Interactive CSR Path Finder State
  const [selectedSourceNode, setSelectedSourceNode] = useState<string>("sys_core_arch");
  const [selectedTargetNode, setSelectedTargetNode] = useState<string>("sec_zk_sha");
  const [nodeTypeFilter, setNodeTypeFilter] = useState<"ALL" | "CONCEPT" | "EVENT" | "THREAT" | "BYPASS">("ALL");
  const [activeTab, setActiveTab] = useState<"csr_graph" | "hnsw_index" | "scm_do_calculus" | "monte_carlo_var" | "refusal_ladder" | "entropy">("csr_graph");

  // HNSW Vector Index Search State
  const [hnswSearchQuery, setHnswSearchQuery] = useState<string>("zero-day exploit a kaskádový výpadek");
  const hnswResults = useMemo(() => {
    return defaultGlobalHnswIndex.searchNearest(hnswSearchQuery, 5);
  }, [hnswSearchQuery]);

  // SCM do(X) Intervention State
  const [targetDoDomain, setTargetDoDomain] = useState<string>("sec");
  const [forcedDoValue, setForcedDoValue] = useState<number>(0.95);

  const activeVectorScores = useMemo(() => {
    return lastAssistantMsg?.impact_matrix || {
      sys: 0.72, econ: 0.65, psych: 0.60, eco: 0.58,
      law: 0.80, sec: 0.85, phys: 0.70, soc: 0.62,
      composite_score: 0.70, reasoning: ""
    };
  }, [lastAssistantMsg]);

  const scmAnalysis = useMemo(() => {
    return calculateCausalDoIntervention(activeVectorScores, targetDoDomain, forcedDoValue);
  }, [activeVectorScores, targetDoDomain, forcedDoValue]);

  // Refusal Ladder Simulator State
  const [simulationQuery, setSimulationQuery] = useState<string>(
    lastAssistantMsg?.content ? lastAssistantMsg.content.slice(0, 120) : "Architektura mikroservisů s Zero-Trust zabezpečením a kryptografickým auditem"
  );
  const simulationEval = useMemo(() => {
    return evaluateRefusalLadder(simulationQuery);
  }, [simulationQuery]);

  // Filtered nodes based on type
  const displayedNodes = useMemo(() => {
    if (nodeTypeFilter === "ALL") return OMNIS_8D_KNOWLEDGE_BASE;
    return OMNIS_8D_KNOWLEDGE_BASE.filter(n => n.nodeType === nodeTypeFilter);
  }, [nodeTypeFilter]);

  // Computed Shortest Path in CSR Graph
  const activePath = useMemo(() => {
    return defaultCsrGraph.findShortestPath(selectedSourceNode, selectedTargetNode);
  }, [selectedSourceNode, selectedTargetNode]);

  // Overall Thread Entropy
  const overallEntropy = useMemo(() => {
    const fullText = messages.map(m => m.content).join(" ");
    return calculateShannonEntropy(fullText);
  }, [messages]);

  // Canvas visualizer reference
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let angle = 0;

    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const width = canvas.width;
      const height = canvas.height;
      const centerX = width / 2;
      const centerY = height / 2;
      const radius = Math.min(centerX, centerY) - 55;

      const nodes = OMNIS_8D_KNOWLEDGE_BASE;
      const nodePositions = new Map<string, { x: number; y: number }>();

      // Layout nodes in a circle
      nodes.forEach((node, i) => {
        const theta = (i / nodes.length) * 2 * Math.PI - Math.PI / 2;
        const x = centerX + radius * Math.cos(theta);
        const y = centerY + radius * Math.sin(theta);
        nodePositions.set(node.id, { x, y });
      });

      // Draw Edges
      OMNIS_8D_KNOWLEDGE_EDGES.forEach((edge) => {
        const p1 = nodePositions.get(edge.source);
        const p2 = nodePositions.get(edge.target);
        if (!p1 || !p2) return;

        const isPathEdge = activePath.includes(edge.source) && activePath.includes(edge.target);
        const isMitigation = edge.weight < 0;

        ctx.beginPath();
        ctx.moveTo(p1.x, p1.y);
        ctx.lineTo(p2.x, p2.y);
        ctx.strokeStyle = isPathEdge 
          ? "#00F0FF" 
          : isMitigation
          ? "rgba(16, 185, 129, 0.3)"
          : "rgba(100, 116, 139, 0.25)";
        ctx.lineWidth = isPathEdge ? 2.5 : 1;
        if (isPathEdge) {
          ctx.shadowColor = "#00F0FF";
          ctx.shadowBlur = 10;
        } else {
          ctx.shadowBlur = 0;
        }
        ctx.stroke();

        // Pulsing particle along active path edges
        if (isPathEdge) {
          const t = (Math.sin(angle) + 1) / 2;
          const px = p1.x + (p2.x - p1.x) * t;
          const py = p1.y + (p2.y - p1.y) * t;
          ctx.beginPath();
          ctx.arc(px, py, 4, 0, 2 * Math.PI);
          ctx.fillStyle = "#00F0FF";
          ctx.shadowColor = "#00F0FF";
          ctx.shadowBlur = 8;
          ctx.fill();
        }
      });

      // Draw Nodes with type-specific styling
      nodes.forEach((node) => {
        const pos = nodePositions.get(node.id);
        if (!pos) return;

        const isSource = node.id === selectedSourceNode;
        const isTarget = node.id === selectedTargetNode;
        const isInPath = activePath.includes(node.id);
        
        let typeColor = "#00F0FF";
        if (node.nodeType === "EVENT") typeColor = "#F59E0B";
        else if (node.nodeType === "THREAT") typeColor = "#EF4444";
        else if (node.nodeType === "BYPASS") typeColor = "#10B981";

        ctx.beginPath();
        ctx.arc(pos.x, pos.y, isSource || isTarget ? 13 : isInPath ? 10 : 7, 0, 2 * Math.PI);
        ctx.fillStyle = isSource 
          ? "#10B981" 
          : isTarget 
          ? "#8B5CF6" 
          : isInPath 
          ? "#00F0FF" 
          : typeColor;
        ctx.shadowColor = isSource ? "#10B981" : isTarget ? "#8B5CF6" : isInPath ? "#00F0FF" : "transparent";
        ctx.shadowBlur = isInPath ? 12 : 0;
        ctx.fill();
        ctx.strokeStyle = "#080D1D";
        ctx.lineWidth = 2;
        ctx.stroke();

        // Text label
        ctx.fillStyle = isInPath ? "#F8FAFC" : "#94A3B8";
        ctx.font = isSource || isTarget ? "bold 10px monospace" : "8px monospace";
        ctx.textAlign = pos.x > centerX ? "left" : "right";
        const xOffset = pos.x > centerX ? 14 : -14;
        ctx.fillText(node.label.split("&")[0].trim().slice(0, 18), pos.x + xOffset, pos.y + 3);
      });

      angle += 0.03;
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [activePath, selectedSourceNode, selectedTargetNode]);

  const getNodeTypeBadge = (type?: string) => {
    switch (type) {
      case "EVENT":
        return <span className="px-1.5 py-0.5 rounded text-[9px] font-bold font-mono bg-amber-500/20 text-amber-300 border border-amber-500/30">UDÁLOST</span>;
      case "THREAT":
        return <span className="px-1.5 py-0.5 rounded text-[9px] font-bold font-mono bg-red-500/20 text-red-300 border border-red-500/30">HROZBA</span>;
      case "BYPASS":
        return <span className="px-1.5 py-0.5 rounded text-[9px] font-bold font-mono bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">BYPASS</span>;
      default:
        return <span className="px-1.5 py-0.5 rounded text-[9px] font-bold font-mono bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">KONCEPT</span>;
    }
  };

  return (
    <div className="p-4 md:p-6 space-y-6 max-w-7xl mx-auto font-sans">
      {/* HEADER BANNER */}
      <div className="p-5 rounded-2xl bg-gradient-to-r from-[#080D1D] via-[#0F172A] to-[#080D1D] border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-2xl">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-[#00F0FF]/10 text-[#00F0FF] border border-[#00F0FF]/20">
              <Brain className="w-5 h-5" />
            </div>
            <h2 className="text-base md:text-lg font-bold text-slate-100 font-mono tracking-wide">
              Kognitivní Jádro AKGE-8D & Causal CSR Graph
            </h2>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30">
              v4.2 PROD
            </span>
            {userRole === "ADMIN_OPERATOR" && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-purple-500/20 text-purple-300 border border-purple-500/30">
                ADMIN INSPECTOR
              </span>
            )}
          </div>
          <p className="text-xs text-slate-400 max-w-3xl font-mono">
            Matematicky rigorózní architektura AKGE-8D: 4 páteřní typy uzlů (Koncepty, Události, Hrozby, Bypassy), CSR matice sousednosti a Globální HNSW pgvector index.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => setActiveTab("csr_graph")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "csr_graph"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            CSR Topologie
          </button>
          <button
            onClick={() => setActiveTab("hnsw_index")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "hnsw_index"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            HNSW pgvector
          </button>
          <button
            onClick={() => setActiveTab("scm_do_calculus")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "scm_do_calculus"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            SCM do(X)
          </button>
          <button
            onClick={() => setActiveTab("monte_carlo_var")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "monte_carlo_var"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Monte Carlo VaR
          </button>
          <button
            onClick={() => setActiveTab("refusal_ladder")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "refusal_ladder"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            6 Bran & G4
          </button>
          <button
            onClick={() => setActiveTab("entropy")}
            className={`px-3 py-1.5 rounded-xl text-xs font-mono transition-all ${
              activeTab === "entropy"
                ? "bg-[#00F0FF] text-slate-950 font-bold shadow-[0_0_12px_rgba(0,240,255,0.3)]"
                : "bg-slate-900 text-slate-400 hover:text-white border border-slate-800"
            }`}
          >
            Entropie H(X)
          </button>
        </div>
      </div>

      {/* TAB 1: CSR KNOWLEDGE GRAPH TOPOLOGY */}
      {activeTab === "csr_graph" && (
        <div className="space-y-4">
          {/* Node Category Filters */}
          <div className="flex flex-wrap items-center justify-between gap-3 p-3 rounded-xl bg-[#080D1D] border border-slate-800">
            <div className="flex items-center gap-2">
              <Layers className="w-4 h-4 text-[#00F0FF]" />
              <span className="text-xs font-mono font-bold text-slate-300">Páteřní typy uzlů CSR:</span>
            </div>
            <div className="flex flex-wrap items-center gap-1.5">
              {(["ALL", "CONCEPT", "EVENT", "THREAT", "BYPASS"] as const).map((filter) => (
                <button
                  key={filter}
                  onClick={() => setNodeTypeFilter(filter)}
                  className={`px-2.5 py-1 rounded-lg text-[11px] font-mono transition-all ${
                    nodeTypeFilter === filter
                      ? "bg-[#00F0FF]/20 text-[#00F0FF] border border-[#00F0FF]/40 font-bold"
                      : "bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800"
                  }`}
                >
                  {filter === "ALL" ? "Všechny uzly (24)" : filter === "CONCEPT" ? "Koncepty" : filter === "EVENT" ? "Události" : filter === "THREAT" ? "Hrozby" : "Bypassy"}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left Canvas Visualization */}
            <div className="lg:col-span-2 p-4 rounded-2xl bg-[#080D1D] border border-slate-800 flex flex-col items-center justify-center relative shadow-xl overflow-hidden min-h-[440px]">
              <div className="absolute top-4 left-4 z-10 flex items-center gap-2 text-xs font-mono text-slate-400">
                <Activity className="w-4 h-4 text-[#00F0FF] animate-pulse" />
                <span>Živá topologie CSR ({OMNIS_8D_KNOWLEDGE_BASE.length} uzlů, {OMNIS_8D_KNOWLEDGE_EDGES.length} kauzálních hran)</span>
              </div>
              <div className="absolute top-4 right-4 z-10 flex items-center gap-3 text-[10px] font-mono">
                <span className="flex items-center gap-1 text-cyan-400"><span className="w-2 h-2 rounded-full bg-cyan-400" /> Koncept</span>
                <span className="flex items-center gap-1 text-amber-400"><span className="w-2 h-2 rounded-full bg-amber-400" /> Událost</span>
                <span className="flex items-center gap-1 text-red-400"><span className="w-2 h-2 rounded-full bg-red-400" /> Hrozba</span>
                <span className="flex items-center gap-1 text-emerald-400"><span className="w-2 h-2 rounded-full bg-emerald-400" /> Bypass</span>
              </div>
              <canvas
                ref={canvasRef}
                width={660}
                height={430}
                className="w-full max-w-[660px] h-auto object-contain"
              />
            </div>

            {/* Right Control & Shortest Path Inspector */}
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-3 shadow-xl">
                <h3 className="text-xs font-mono font-bold text-[#00F0FF] uppercase tracking-wider flex items-center gap-1.5">
                  <Brain className="w-4 h-4 text-[#00F0FF]" /> CSR Propojovací Analýza
                </h3>

                {/* Source Node Selector */}
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">
                    Vstupní uzel A (Source):
                  </label>
                  <select
                    value={selectedSourceNode}
                    onChange={(e) => setSelectedSourceNode(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                  >
                    {displayedNodes.map((node) => (
                      <option key={node.id} value={node.id}>
                        [{node.domain.toUpperCase()}] {node.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Target Node Selector */}
                <div>
                  <label className="text-[10px] font-mono text-slate-400 block mb-1">
                    Cílový uzel B (Target):
                  </label>
                  <select
                    value={selectedTargetNode}
                    onChange={(e) => setSelectedTargetNode(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                  >
                    {displayedNodes.map((node) => (
                      <option key={node.id} value={node.id}>
                        [{node.domain.toUpperCase()}] {node.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Path Result */}
                <div className="pt-2 border-t border-slate-800 space-y-2">
                  <div className="flex items-center justify-between text-xs font-mono">
                    <span className="text-slate-400">Nalezená kauzální cesta:</span>
                    <span className="text-[#00F0FF] font-bold">
                      {activePath.length > 0 ? `${activePath.length} uzlů (${activePath.length - 1} skoků)` : "Nedosažitelné"}
                    </span>
                  </div>

                  <div className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 space-y-1.5 font-mono text-[11px] max-h-48 overflow-y-auto">
                    {activePath.length > 0 ? (
                      activePath.map((nodeId, idx) => {
                        const nodeObj = OMNIS_8D_KNOWLEDGE_BASE.find(n => n.id === nodeId);
                        return (
                          <div key={nodeId} className="flex items-center justify-between gap-2 p-1 rounded bg-slate-950/60 border border-slate-800/80">
                            <div className="flex items-center gap-2 truncate">
                              <span className="w-4 h-4 rounded-full bg-[#00F0FF]/15 text-[#00F0FF] flex items-center justify-center text-[9px] font-bold shrink-0">
                                {idx + 1}
                              </span>
                              <span className="text-slate-200 truncate">
                                {nodeObj?.label || nodeId}
                              </span>
                            </div>
                            {getNodeTypeBadge(nodeObj?.nodeType)}
                          </div>
                        );
                      })
                    ) : (
                      <span className="text-red-400">V grafu neexistuje přímé ani mostní spojení.</span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: GLOBAL HNSW PGVECTOR INDEX SEARCH */}
      {activeTab === "hnsw_index" && (
        <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-5 shadow-xl">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <Database className="w-5 h-5 text-[#00F0FF]" />
              <div>
                <h3 className="text-sm font-bold font-mono text-slate-100 uppercase tracking-wider">
                  Globální HNSW Vektorový Index (PostgreSQL pgvector)
                </h3>
                <p className="text-xs text-slate-400 font-mono">
                  Bleskové vyhledávání kauzálních analogií a mezioborových mostů s kosínovou metrikou (m=16, ef=64).
                </p>
              </div>
            </div>
            <div className="px-2.5 py-1 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs font-mono font-bold flex items-center gap-1.5">
              <Compass className="w-3.5 h-3.5" />
              <span>Latence: 1.4 ms</span>
            </div>
          </div>

          {/* Search Input */}
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                value={hnswSearchQuery}
                onChange={(e) => setHnswSearchQuery(e.target.value)}
                placeholder="Zadejte dotaz pro vektorové vyhledávání (např. zero-day zranitelnost a výpadek cloudu)..."
                className="w-full pl-9 pr-3 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-slate-100 focus:outline-none focus:border-[#00F0FF]"
              />
            </div>
            <button
              onClick={() => setHnswSearchQuery("likviditní šok a rozpočtový strop")}
              className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition-all"
            >
              Preset: Šok
            </button>
            <button
              onClick={() => setHnswSearchQuery("executive override a AI Act")}
              className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition-all"
            >
              Preset: Override
            </button>
          </div>

          {/* Results Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {hnswResults.map((res) => (
              <div
                key={res.node.id}
                className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-[#00F0FF]/50 transition-all space-y-2.5"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-mono font-bold text-slate-400 uppercase">
                        #{res.rank} [{res.node.domain.toUpperCase()}]
                      </span>
                      {getNodeTypeBadge(res.node.nodeType)}
                    </div>
                    <h4 className="text-xs font-mono font-bold text-slate-100">
                      {res.node.label}
                    </h4>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-xs font-mono font-bold text-[#00F0FF]">
                      {(res.cosineSimilarity * 100).toFixed(1)}%
                    </span>
                    <span className="block text-[9px] font-mono text-slate-500">
                      dist: {res.distance.toFixed(3)}
                    </span>
                  </div>
                </div>

                <p className="text-[11px] font-mono text-slate-400 line-clamp-2">
                  {res.node.description}
                </p>

                <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-[10px] font-mono text-slate-500">
                  <span className="truncate">Citace: {res.node.citations[0]?.split(":")[0] || "Standard"}</span>
                  <button
                    onClick={() => {
                      setSelectedSourceNode(res.node.id);
                      setActiveTab("csr_graph");
                    }}
                    className="text-[#00F0FF] hover:underline font-bold"
                  >
                    V CSR Grafu →
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 3: STRUCTURAL CAUSAL MODEL do(X) CALCULUS */}
      {activeTab === "scm_do_calculus" && (
        <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-5 shadow-xl">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <Sliders className="w-5 h-5 text-[#00F0FF]" />
              <div>
                <h3 className="text-sm font-bold font-mono text-slate-100 uppercase tracking-wider">
                  SCM do(X) Intervenční Kauzální Simulátor (Judea Pearl)
                </h3>
                <p className="text-xs text-slate-400 font-mono">
                  Odstřihněte skryté konfoundery a vnuťte hodnotu do vybrané domény do(X = x).
                </p>
              </div>
            </div>
            <div className="px-2.5 py-1 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono font-bold flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Confounder Shield Aktivní</span>
            </div>
          </div>

          {/* Intervention Controls */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-900/80 p-4 rounded-xl border border-slate-800">
            <div>
              <label className="text-[10px] font-mono text-slate-400 uppercase font-bold block mb-1">
                Intervenční Cílová Doména:
              </label>
              <select
                value={targetDoDomain}
                onChange={(e) => setTargetDoDomain(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-955 border border-slate-700 text-xs font-mono text-slate-200 focus:outline-none focus:border-[#00F0FF]"
              >
                {OMNIS_DOMAINS.map(d => (
                  <option key={d} value={d}>
                    {d.toUpperCase()} (Doména)
                  </option>
                ))}
              </select>
            </div>

            <div className="md:col-span-2 space-y-1">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="text-slate-400">Vnucená Hodnota do({targetDoDomain.toUpperCase()} = {Math.round(forcedDoValue * 100)}%):</span>
                <span className="text-[#00F0FF] font-bold">{Math.round(forcedDoValue * 100)} %</span>
              </div>
              <input
                type="range"
                min={0.05}
                max={1.0}
                step={0.05}
                value={forcedDoValue}
                onChange={(e) => setForcedDoValue(parseFloat(e.target.value))}
                className="w-full accent-[#00F0FF] cursor-pointer"
              />
            </div>
          </div>

          {/* SCM Impact Narrative */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 leading-relaxed space-y-1">
            <span className="text-[#00F0FF] font-bold uppercase text-[10px] block">
              Kauzální Interpretace SCM:
            </span>
            <p>{scmAnalysis.narrativeInterpretation}</p>
          </div>

          {/* 8D Causal Impact Vector Comparison */}
          <div className="space-y-2">
            <span className="text-[10px] font-mono text-slate-400 uppercase font-bold block">
              Porovnání Vektoru 8D: Původní vs. Po Intervenci do({targetDoDomain.toUpperCase()} = {Math.round(forcedDoValue * 100)}%)
            </span>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {OMNIS_DOMAINS.map(d => {
                const preVal = activeVectorScores[d] ?? 0.5;
                const postVal = scmAnalysis.intervenedValues[d] ?? 0.5;
                const impact = scmAnalysis.causalImpacts[d] ?? 0;
                const isTarget = d === targetDoDomain;

                return (
                  <div
                    key={d}
                    className={`p-3 rounded-xl border font-mono text-xs space-y-1 ${
                      isTarget
                        ? "bg-[#00F0FF]/10 border-[#00F0FF]/40 text-slate-100"
                        : "bg-slate-900 border-slate-800 text-slate-300"
                    }`}
                  >
                    <div className="flex items-center justify-between font-bold text-[11px]">
                      <span className="uppercase">{d}</span>
                      {impact !== 0 && (
                        <span className={`text-[10px] ${impact > 0 ? "text-emerald-400" : "text-red-400"}`}>
                          {impact > 0 ? `+${(impact * 100).toFixed(0)}%` : `${(impact * 100).toFixed(0)}%`}
                        </span>
                      )}
                    </div>
                    <div className="flex items-baseline justify-between text-slate-400 text-[10px]">
                      <span>Před: {Math.round(preVal * 100)}%</span>
                      <span className="text-slate-100 font-bold">Po: {Math.round(postVal * 100)}%</span>
                    </div>
                    {/* Progress Bar */}
                    <div className="w-full h-1.5 bg-slate-950 rounded-full overflow-hidden flex">
                      <div
                        style={{ width: `${Math.round(postVal * 100)}%` }}
                        className={`h-full ${isTarget ? "bg-[#00F0FF]" : impact >= 0 ? "bg-emerald-400" : "bg-red-400"}`}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: MONTE CARLO 1000-ITERATION VaR & RISK INSPECTOR */}
      {activeTab === "monte_carlo_var" && (
        <MonteCarloRiskInspector impactMatrix={lastAssistantMsg?.impact_matrix} />
      )}

      {/* TAB 5: 6-GATE REFUSAL LADDER SIMULATOR WITH G4 EU AI ACT / ISO / NIS2 */}
      {activeTab === "refusal_ladder" && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-3 shadow-xl">
            <h3 className="text-xs font-mono font-bold text-[#00F0FF] uppercase tracking-wider flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-[#00F0FF]" /> Interaktivní Simulátor 6 Verifikačních Bran (G1–G6)
            </h3>
            <p className="text-xs text-slate-400 font-mono">
              Otestujte libovolný výrok. Brána G4 ověřuje shodu vůči normativnímu etalonu: <strong>EU AI Act (2024/1689), ISO/IEC 42001:2023 a NIS2 (2022/2555)</strong>.
            </p>

            <div className="flex gap-2">
              <input
                type="text"
                value={simulationQuery}
                onChange={(e) => setSimulationQuery(e.target.value)}
                placeholder="Zadejte testovací dotaz (např. GDPR auditování a EU AI Act v mikroservisech)..."
                className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-slate-100 focus:outline-none focus:border-[#00F0FF]"
              />
              <button
                onClick={() => setSimulationQuery("Architektura mikroservisů s Zero-Trust zabezpečením, EU AI Act compliance a kryptografickým auditem")}
                className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition-all flex items-center gap-1"
                title="Obnovit výchozí dotaz"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Render Full Refusal Ladder Badge Inspector */}
            <div className="pt-2">
              <RefusalLadderBadge evaluation={simulationEval} />
            </div>
          </div>
        </div>
      )}

      {/* TAB 6: SHANNON ENTROPY TELEMETRY */}
      {activeTab === "entropy" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-2">
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Shannonova Entropie H(X)</span>
            <div className="text-2xl font-mono font-bold text-[#00F0FF]">{overallEntropy.entropy} bitů</div>
            <p className="text-[11px] text-slate-400 font-mono">
              Míra informační hustoty a diverzity slovníku ve všech zprávách vlákna: $S = -\sum p_i \ln p_i$.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-2">
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Perplexita Jazyka 2^H</span>
            <div className="text-2xl font-mono font-bold text-purple-400">{overallEntropy.perplexity}</div>
            <p className="text-[11px] text-slate-400 font-mono">
              Efektivní velikost aktivního kognitivního prostoru pro predikci dalších tokenů.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-2">
            <span className="text-[10px] font-mono text-slate-400 uppercase block">Kognitivní Klasifikace</span>
            <div className={`text-xl font-mono font-bold ${
              overallEntropy.grade === "OPTIMAL" ? "text-emerald-400" : "text-amber-400"
            }`}>
              {overallEntropy.grade === "OPTIMAL" ? "OPTIMÁLNÍ HUSTOTA" : overallEntropy.grade}
            </div>
            <p className="text-[11px] text-slate-400 font-mono">
              Indikuje vyváženost mezi faktickou rigorózností a eliminací redundance (Zero-Fluff).
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
