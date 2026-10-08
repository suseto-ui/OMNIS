import React, { useState, useEffect, useCallback } from "react";
import {
  Database,
  Upload,
  FileText,
  RefreshCw,
  Download,
  CheckCircle2,
  AlertCircle,
  Layers,
  Sparkles,
  Search,
  HardDrive,
  Cpu,
  ArrowRight,
  ShieldCheck,
  Tag
} from "lucide-react";

export interface EpistemicStats {
  status: string;
  total_memories: number;
  memory_types: Record<string, number>;
  vector_dimension: number;
  indexing_engine: string;
  timestamp: string;
}

export interface MemoryExportItem {
  id: string;
  content: string;
  memory_type: string;
  importance_score: number;
  metadata?: any;
  created_at: string;
}

export function EpistemicMemoryDashboard() {
  const [stats, setStats] = useState<EpistemicStats | null>(null);
  const [loadingStats, setLoadingStats] = useState(false);
  const [recentMemories, setRecentMemories] = useState<MemoryExportItem[]>([]);
  
  // Ingest form state
  const [docTitle, setDocTitle] = useState("");
  const [docContent, setDocContent] = useState("");
  const [docDomain, setDocDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [chunkSize, setChunkSize] = useState(500);
  const [chunkOverlap, setChunkOverlap] = useState(100);
  const [importanceScore, setImportanceScore] = useState(1.0);
  const [isIngesting, setIsIngesting] = useState(false);
  const [ingestStatus, setIngestStatus] = useState<string | null>(null);
  const [statusType, setStatusType] = useState<"success" | "error" | "info">("info");

  // File upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const fetchStatsAndRecent = useCallback(async () => {
    setLoadingStats(true);
    try {
      const statsRes = await fetch("/api/epistemic/stats");
      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(statsData);
      }
      const exportRes = await fetch("/api/epistemic/export?limit=15");
      if (exportRes.ok) {
        const exportData = await exportRes.json();
        setRecentMemories(exportData.items || []);
      }
    } catch (e) {
      console.error("Failed to fetch epistemic stats", e);
    } finally {
      setLoadingStats(false);
    }
  }, []);

  useEffect(() => {
    fetchStatsAndRecent();
  }, [fetchStatsAndRecent]);

  const handleIngestText = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!docTitle.trim() || !docContent.trim()) {
      setIngestStatus("Vyplňte prosím název i obsah dokumentu.");
      setStatusType("error");
      return;
    }

    setIsIngesting(true);
    setIngestStatus(null);
    try {
      const res = await fetch("/api/epistemic/ingest-text", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: docTitle.trim(),
          content: docContent.trim(),
          domain: docDomain,
          chunk_size: chunkSize,
          chunk_overlap: chunkOverlap,
          importance_score: importanceScore
        })
      });

      if (!res.ok) {
        throw new Error(`Chyba ingestace (HTTP ${res.status})`);
      }

      const data = await res.json();
      setIngestStatus(`✅ ${data.message} (Celkem ${data.total_chunks} bloků)`);
      setStatusType("success");
      setDocContent("");
      setDocTitle("");
      setTimeout(fetchStatsAndRecent, 1500);
    } catch (err: any) {
      setIngestStatus(`❌ Selhání ingestace: ${err?.message || "Neznámá chyba"}`);
      setStatusType("error");
    } finally {
      setIsIngesting(false);
    }
  };

  const handleFileUpload = async () => {
    if (!selectedFile) return;
    setIsUploading(true);
    setIngestStatus(null);
    try {
      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("domain", docDomain);

      const res = await fetch("/api/epistemic/upload", {
        method: "POST",
        body: formData
      });

      if (!res.ok) {
        throw new Error(`Chyba při nahrávání souboru (HTTP ${res.status})`);
      }

      const data = await res.json();
      setIngestStatus(`✅ ${data.message}`);
      setStatusType("success");
      setSelectedFile(null);
      setTimeout(fetchStatsAndRecent, 1500);
    } catch (err: any) {
      setIngestStatus(`❌ Selhání nahrání souboru: ${err?.message || "Neznámá chyba"}`);
      setStatusType("error");
    } finally {
      setIsUploading(false);
    }
  };

  const handleExportBackup = async () => {
    try {
      const res = await fetch("/api/epistemic/export?limit=500");
      if (!res.ok) throw new Error("Chyba exportu paměti");
      const data = await res.json();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `omnis_epistemic_memory_backup_${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Failed to export backup", err);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header telemetry cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div className="p-4 rounded-2xl bg-gradient-to-br from-[#00F0FF]/10 to-transparent border border-[#00F0FF]/30 backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-1">
            <span className="flex items-center gap-1.5 text-[#00F0FF]">
              <Database className="w-4 h-4 text-[#00F0FF]" />
              Indexované entity
            </span>
            <span className="px-1.5 py-0.5 rounded bg-[#00F0FF]/15 text-[#00F0FF] text-[10px] font-bold">
              pgvector
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-100 font-mono">
            {stats ? stats.total_memories.toLocaleString() : "..."}
          </div>
          <div className="text-[10px] text-slate-400 font-mono mt-1">
            Vektorová dimenze: <strong className="text-slate-200">768D HNSW</strong>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-gradient-to-br from-purple-500/10 to-transparent border border-purple-500/30 backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-1">
            <span className="flex items-center gap-1.5 text-purple-400">
              <Layers className="w-4 h-4 text-purple-400" />
              Chunky dokumentů
            </span>
            <span className="px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-300 text-[10px] font-bold">
              RRF Hybrid
            </span>
          </div>
          <div className="text-2xl font-extrabold text-slate-100 font-mono">
            {stats?.memory_types?.document_chunk || 0}
          </div>
          <div className="text-[10px] text-slate-400 font-mono mt-1">
            Konverzační stopy: <strong className="text-slate-200">{stats?.memory_types?.semantic || 0}</strong>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-gradient-to-br from-emerald-500/10 to-transparent border border-emerald-500/30 backdrop-blur-md">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-1">
            <span className="flex items-center gap-1.5 text-emerald-400">
              <HardDrive className="w-4 h-4 text-emerald-400" />
              Full-Text Index
            </span>
            <span className="px-1.5 py-0.5 rounded bg-emerald-500/15 text-emerald-300 text-[10px] font-bold">
              GIN tsvector
            </span>
          </div>
          <div className="text-lg font-bold text-emerald-300 font-mono truncate">
            {stats?.indexing_engine?.includes("GIN") ? "Aktivní & Provázaný" : "Operativní"}
          </div>
          <div className="text-[10px] text-slate-400 font-mono mt-1">
            Reciprocal Rank Fusion k=60
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-gradient-to-br from-amber-500/10 to-transparent border border-amber-500/30 backdrop-blur-md flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs font-mono text-slate-400">
            <span className="flex items-center gap-1.5 text-amber-400">
              <ShieldCheck className="w-4 h-4 text-amber-400" />
              Záloha & Export
            </span>
          </div>
          <div className="flex items-center gap-2 mt-2">
            <button
              onClick={handleExportBackup}
              className="flex-1 px-3 py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/40 text-amber-300 font-mono text-xs font-bold transition-all flex items-center justify-center gap-1.5 shadow-sm"
            >
              <Download className="w-3.5 h-3.5" />
              Export JSON
            </button>
            <button
              onClick={fetchStatsAndRecent}
              disabled={loadingStats}
              className="p-1.5 rounded-xl bg-slate-900 border border-slate-700 text-slate-300 hover:text-white"
              title="Obnovit telemetrii paměti"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingStats ? "animate-spin text-[#00F0FF]" : ""}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Main Grid: Ingest Console + File Drag & Drop + Recent Memories */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Direct Text Ingest Console */}
        <div className="lg:col-span-7 bg-slate-900/80 border border-slate-800 rounded-2xl p-5 backdrop-blur-md space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-100 font-bold font-sans">
              <FileText className="w-4 h-4 text-[#00F0FF]" />
              <h3>Přímá Ingestace Znalostního Textu do Sémantické Báze</h3>
            </div>
            <span className="text-[11px] font-mono text-[#00F0FF] bg-[#00F0FF]/10 px-2 py-0.5 rounded border border-[#00F0FF]/30">
              Auto-Chunking
            </span>
          </div>

          <form onSubmit={handleIngestText} className="space-y-3.5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-mono text-slate-400 mb-1">
                  Název dokumentu / modulu *
                </label>
                <input
                  type="text"
                  placeholder="např. Bezpečnostní Standardy NIS2"
                  value={docTitle}
                  onChange={(e) => setDocTitle(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 font-mono text-xs focus:border-[#00F0FF] outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono text-slate-400 mb-1">
                  Ontologická doména
                </label>
                <select
                  value={docDomain}
                  onChange={(e) => setDocDomain(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 font-mono text-xs focus:border-[#00F0FF] outline-none"
                >
                  <option value="SYSTEMS_INTELLIGENCE">SYSTEMS_INTELLIGENCE (Systémy)</option>
                  <option value="CYBERNETICS">CYBERNETICS (Zero-Trust Bezpečnost)</option>
                  <option value="SUSTAINABLE_TECH">SUSTAINABLE_TECH (Termodynamika)</option>
                  <option value="COGNITIVE_SCI">COGNITIVE_SCI (Kognice & Psychologie)</option>
                  <option value="GAME_THEORY">GAME_THEORY (Ekonomie & Trhy)</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-mono text-slate-400 mb-1">
                Obsah dokumentu (Markdown / Kód / Technická specifikace) *
              </label>
              <textarea
                rows={6}
                placeholder="Vložte text, architekturu, standardy nebo pravidla, které má O.M.N.I.S. používat při hybridním vyhledávání..."
                value={docContent}
                onChange={(e) => setDocContent(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 font-mono text-xs focus:border-[#00F0FF] outline-none resize-y"
              />
            </div>

            {/* Chunking Parameters */}
            <div className="grid grid-cols-3 gap-2.5 p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] font-mono text-slate-400">
              <div>
                <label className="block mb-1">Velikost bloku: {chunkSize} znaků</label>
                <input
                  type="range"
                  min="200"
                  max="1500"
                  step="50"
                  value={chunkSize}
                  onChange={(e) => setChunkSize(parseInt(e.target.value))}
                  className="w-full accent-[#00F0FF]"
                />
              </div>
              <div>
                <label className="block mb-1">Překryv: {chunkOverlap} znaků</label>
                <input
                  type="range"
                  min="0"
                  max="300"
                  step="25"
                  value={chunkOverlap}
                  onChange={(e) => setChunkOverlap(parseInt(e.target.value))}
                  className="w-full accent-purple-400"
                />
              </div>
              <div>
                <label className="block mb-1">Priorita RAG: {importanceScore.toFixed(1)}x</label>
                <input
                  type="range"
                  min="0.5"
                  max="3.0"
                  step="0.1"
                  value={importanceScore}
                  onChange={(e) => setImportanceScore(parseFloat(e.target.value))}
                  className="w-full accent-emerald-400"
                />
              </div>
            </div>

            <div className="flex items-center justify-between pt-1">
              <button
                type="submit"
                disabled={isIngesting || !docTitle.trim() || !docContent.trim()}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#00F0FF] to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-mono font-bold text-xs flex items-center gap-2 transition-all shadow-[0_0_15px_rgba(0,240,255,0.3)] disabled:opacity-50 cursor-pointer"
              >
                <Sparkles className={`w-4 h-4 ${isIngesting ? "animate-spin" : ""}`} />
                {isIngesting ? "Indexuji chunky..." : "Indexovat do pgvector báze"}
              </button>
            </div>
          </form>

          {/* Status banner */}
          {ingestStatus && (
            <div
              className={`p-3 rounded-xl border text-xs font-mono flex items-center gap-2 ${
                statusType === "success"
                  ? "bg-emerald-950/40 border-emerald-500/40 text-emerald-300"
                  : statusType === "error"
                  ? "bg-rose-950/40 border-rose-500/40 text-rose-300"
                  : "bg-blue-950/40 border-blue-500/40 text-cyan-300"
              }`}
            >
              {statusType === "success" ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              )}
              <span>{ingestStatus}</span>
            </div>
          )}
        </div>

        {/* Right Column: File Upload + Recent Memories */}
        <div className="lg:col-span-5 space-y-6">
          {/* File Upload Box */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 backdrop-blur-md space-y-3">
            <div className="flex items-center gap-2 text-slate-100 font-bold font-sans">
              <Upload className="w-4 h-4 text-purple-400" />
              <h3>Nahrát soubor (.md, .txt, .json, .py, .kt)</h3>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed font-sans">
              Automaticky přečte a rozseká obsah souboru do překrývajících se sémantických bloků.
            </p>

            <div className="border-2 border-dashed border-slate-700 hover:border-purple-500/60 rounded-xl p-4 text-center space-y-2 transition-all">
              <input
                type="file"
                accept=".md,.txt,.json,.py,.kt,.csv,.yaml,.yml"
                onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                className="hidden"
                id="epistemic-file-input"
              />
              <label
                htmlFor="epistemic-file-input"
                className="cursor-pointer flex flex-col items-center justify-center gap-1.5"
              >
                <Upload className="w-6 h-6 text-purple-400" />
                <span className="text-xs font-mono text-slate-200">
                  {selectedFile ? selectedFile.name : "Klikněte pro výběr souboru"}
                </span>
                <span className="text-[10px] font-mono text-slate-500">
                  {selectedFile ? `${(selectedFile.size / 1024).toFixed(1)} KB` : "Maximální velikost 5 MB"}
                </span>
              </label>
            </div>

            {selectedFile && (
              <button
                onClick={handleFileUpload}
                disabled={isUploading}
                className="w-full px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-mono font-bold text-xs flex items-center justify-center gap-2 transition-all disabled:opacity-50"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isUploading ? "animate-spin" : ""}`} />
                {isUploading ? "Nahrávám a indexuji..." : "Potvrdit a indexovat soubor"}
              </button>
            )}
          </div>

          {/* Recent Memories List */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 backdrop-blur-md space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-slate-100 font-bold font-sans">
                <Database className="w-4 h-4 text-emerald-400" />
                <h3>Poslední záznamy v paměti</h3>
              </div>
              <span className="text-[10px] font-mono text-slate-400">
                Top {recentMemories.length}
              </span>
            </div>

            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {recentMemories.length === 0 ? (
                <div className="text-xs font-mono text-slate-500 text-center py-4">
                  Žádné paměťové otisky k zobrazení.
                </div>
              ) : (
                recentMemories.map((m) => (
                  <div
                    key={m.id}
                    className="p-2.5 rounded-xl bg-slate-950/70 border border-slate-800/80 space-y-1 text-xs font-mono"
                  >
                    <div className="flex items-center justify-between text-[10px]">
                      <span className="text-cyan-400 font-bold truncate max-w-[180px]">
                        {m.memory_type}
                      </span>
                      <span className="text-slate-500">
                        {m.created_at ? new Date(m.created_at).toLocaleTimeString() : ""}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-300 line-clamp-2 leading-relaxed">
                      {m.content}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
