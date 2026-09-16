import React, { useState } from "react";
import { GitMerge, X, Check, ArrowRight, AlertCircle, Layers } from "lucide-react";
import { ChatThread } from "../App";

interface MergeThreadsModalProps {
  threads: ChatThread[];
  activeThreadId: string;
  onClose: () => void;
  onMerge: (mergedThread: ChatThread, deleteOriginals: boolean) => void;
}

export const MergeThreadsModal: React.FC<MergeThreadsModalProps> = ({
  threads,
  activeThreadId,
  onClose,
  onMerge,
}) => {
  const [threadIdA, setThreadIdA] = useState<string>(activeThreadId || (threads[0]?.id || ""));
  const [threadIdB, setThreadIdB] = useState<string>(
    threads.find((t) => t.id !== activeThreadId)?.id || (threads[1]?.id || "")
  );
  const [customTitle, setCustomTitle] = useState<string>("");
  const [deleteOriginals, setDeleteOriginals] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string>("");

  const threadA = threads.find((t) => t.id === threadIdA);
  const threadB = threads.find((t) => t.id === threadIdB);

  const handleExecuteMerge = () => {
    if (!threadIdA || !threadIdB) {
      setErrorMsg("Musíte vybrat dvě platná konverzační vlákna.");
      return;
    }
    if (threadIdA === threadIdB) {
      setErrorMsg("Nemůžete sloučit vlákno samo se sebou. Vyberte dvě odlišná vlákna.");
      return;
    }
    if (!threadA || !threadB) {
      setErrorMsg("Některé ze zvolených vláken nebylo nalezeno.");
      return;
    }

    // Merge messages and sort chronologically
    const combinedMessages = [...threadA.messages, ...threadB.messages].sort(
      (m1, m2) => new Date(m1.created_at).getTime() - new Date(m2.created_at).getTime()
    );

    const mergedTitle = customTitle.trim() || `[Sloučeno] ${threadA.title} + ${threadB.title}`;
    const mergedThread: ChatThread = {
      id: "thread-merged-" + Date.now(),
      title: mergedTitle,
      messages: combinedMessages,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    onMerge(mergedThread, deleteOriginals);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-[#070B18] border border-[#00F0FF]/40 rounded-2xl shadow-[0_0_30px_rgba(0,240,255,0.15)] flex flex-col overflow-hidden text-slate-200 font-sans">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-[#050811]">
          <div className="flex items-center gap-2">
            <GitMerge className="w-5 h-5 text-[#00F0FF]" />
            <h2 className="text-sm font-mono font-bold text-[#00F0FF] tracking-wider uppercase">
              Sloučení Konverzačních Vláken
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 space-y-4 max-h-[75vh] overflow-y-auto font-mono text-xs">
          <p className="text-slate-400 leading-relaxed font-sans text-xs">
            Vyberte dvě konverzační vlákna O.M.N.I.S., která chcete chronologicky sloučit do jednoho nového vlákna se zachováním časové posloupnosti zpráv.
          </p>

          {errorMsg && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Selectors Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* Thread A */}
            <div className="space-y-1.5">
              <label className="text-[10px] text-cyan-400 font-bold uppercase block">První vlákno (A):</label>
              <select
                value={threadIdA}
                onChange={(e) => {
                  setThreadIdA(e.target.value);
                  setErrorMsg("");
                }}
                className="w-full bg-slate-900 border border-slate-700 focus:border-[#00F0FF] rounded-xl px-3 py-2 text-xs text-slate-200 outline-none"
              >
                {threads.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.title} ({t.messages.length} zpráv)
                  </option>
                ))}
              </select>
            </div>

            {/* Thread B */}
            <div className="space-y-1.5">
              <label className="text-[10px] text-purple-400 font-bold uppercase block">Druhé vlákno (B):</label>
              <select
                value={threadIdB}
                onChange={(e) => {
                  setThreadIdB(e.target.value);
                  setErrorMsg("");
                }}
                className="w-full bg-slate-900 border border-slate-700 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 outline-none"
              >
                {threads.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.title} ({t.messages.length} zpráv)
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Merge Preview Card */}
          {threadA && threadB && threadIdA !== threadIdB && (
            <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between text-[10px] text-slate-400">
                <span>Výsledný kontext po sloučení:</span>
                <span className="text-[#00F0FF] font-bold">
                  {(threadA.messages.length + threadB.messages.length)} zpráv celkem
                </span>
              </div>
              <div className="flex items-center justify-between text-[11px] text-slate-300">
                <span className="truncate max-w-[180px] text-cyan-300">{threadA.title}</span>
                <ArrowRight className="w-3.5 h-3.5 text-slate-500" />
                <span className="truncate max-w-[180px] text-purple-300">{threadB.title}</span>
              </div>
            </div>
          )}

          {/* Custom title input */}
          <div className="space-y-1.5">
            <label className="text-[10px] text-slate-400 font-bold uppercase block">
              Název sloučeného vlákna (volitelné):
            </label>
            <input
              type="text"
              placeholder={`[Sloučeno] ${threadA?.title || "A"} + ${threadB?.title || "B"}`}
              value={customTitle}
              onChange={(e) => setCustomTitle(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 focus:border-[#00F0FF] rounded-xl px-3 py-2 text-xs text-slate-200 outline-none font-mono"
            />
          </div>

          {/* Delete original threads checkbox */}
          <label className="flex items-center gap-2.5 p-2.5 rounded-xl bg-slate-900/50 border border-slate-800/80 cursor-pointer hover:bg-slate-900 transition-colors">
            <input
              type="checkbox"
              checked={deleteOriginals}
              onChange={(e) => setDeleteOriginals(e.target.checked)}
              className="rounded border-slate-700 text-[#00F0FF] focus:ring-[#00F0FF] bg-slate-950"
            />
            <span className="text-xs font-sans text-slate-300">
              Odstranit původní samostatná vlákna po úspěšném sloučení
            </span>
          </label>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-[#050811] flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-mono font-bold text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            ZRUŠIT
          </button>
          <button
            onClick={handleExecuteMerge}
            className="px-5 py-2 rounded-xl text-xs font-mono font-bold bg-[#00F0FF]/20 hover:bg-[#00F0FF]/30 text-[#00F0FF] border border-[#00F0FF]/50 shadow-[0_0_12px_rgba(0,240,255,0.2)] transition-all flex items-center gap-2"
          >
            <GitMerge className="w-4 h-4" />
            <span>SLOUČIT VLÁKNA</span>
          </button>
        </div>
      </div>
    </div>
  );
};
