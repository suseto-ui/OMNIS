import React, { useState, useMemo } from "react";
import { Search, X, MessageSquare, Clock, User, Brain, CornerDownRight } from "lucide-react";
import { ChatThread } from "../App";

interface SearchResultItem {
  threadId: string;
  threadTitle: string;
  messageId: string;
  role: "user" | "assistant";
  content: string;
  createdAt: string;
  matchIndex: number;
}

interface FullTextSearchModalProps {
  threads: ChatThread[];
  onClose: () => void;
  onSelectResult: (threadId: string, messageId: string) => void;
}

export const FullTextSearchModal: React.FC<FullTextSearchModalProps> = ({
  threads,
  onClose,
  onSelectResult,
}) => {
  const [searchTerm, setSearchTerm] = useState("");

  const searchResults = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query || query.length < 2) return [];

    const results: SearchResultItem[] = [];

    threads.forEach((thread) => {
      thread.messages.forEach((msg) => {
        const textLower = msg.content.toLowerCase();
        const matchIdx = textLower.indexOf(query);
        if (matchIdx !== -1) {
          results.push({
            threadId: thread.id,
            threadTitle: thread.title,
            messageId: msg.id,
            role: msg.role,
            content: msg.content,
            createdAt: msg.created_at,
            matchIndex: matchIdx,
          });
        }
      });
    });

    return results;
  }, [searchTerm, threads]);

  const highlightMatch = (text: string, query: string) => {
    if (!query) return text;
    const lowerText = text.toLowerCase();
    const lowerQuery = query.toLowerCase();
    const startIdx = lowerText.indexOf(lowerQuery);
    if (startIdx === -1) return text;

    // Show context around match snippet
    const snippetStart = Math.max(0, startIdx - 40);
    const snippetEnd = Math.min(text.length, startIdx + query.length + 60);

    const prefix = snippetStart > 0 ? "..." : "";
    const suffix = snippetEnd < text.length ? "..." : "";

    const before = text.substring(snippetStart, startIdx);
    const match = text.substring(startIdx, startIdx + query.length);
    const after = text.substring(startIdx + query.length, snippetEnd);

    return (
      <span>
        {prefix}
        {before}
        <mark className="bg-[#00F0FF]/30 text-[#00F0FF] font-bold px-0.5 rounded border border-[#00F0FF]/50">
          {match}
        </mark>
        {after}
        {suffix}
      </span>
    );
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-2xl bg-[#070B18] border border-[#00F0FF]/40 rounded-2xl shadow-[0_0_35px_rgba(0,240,255,0.2)] flex flex-col overflow-hidden text-slate-200 font-sans max-h-[85vh]">
        {/* Search Header Input */}
        <div className="p-4 border-b border-slate-800 bg-[#050811] flex items-center gap-3">
          <Search className="w-5 h-5 text-[#00F0FF] flex-shrink-0 animate-pulse" />
          <input
            type="text"
            autoFocus
            placeholder="Prohledat celou historii konverzací napříč všemi vlákny..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-transparent text-sm sm:text-base font-mono text-slate-100 placeholder-slate-500 outline-none"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <button
            onClick={onClose}
            className="px-3 py-1 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-mono text-slate-300 transition-colors flex-shrink-0"
          >
            ZRUŠIT
          </button>
        </div>

        {/* Results Metadata Bar */}
        <div className="px-4 py-2 bg-slate-950 border-b border-slate-800/80 flex items-center justify-between text-[11px] font-mono text-slate-400">
          <span>
            {searchTerm.trim().length < 2
              ? "Zadejte alespoň 2 znaky pro vyhledávání"
              : `Nalezeno ${searchResults.length} výskytů`}
          </span>
          <span>
            Prohledává se {threads.length} vláken / {threads.reduce((acc, t) => acc + t.messages.length, 0)} zpráv
          </span>
        </div>

        {/* Search Results List */}
        <div className="flex-1 overflow-y-auto p-3 space-y-2 scrollbar-thin scrollbar-thumb-slate-800">
          {searchResults.length === 0 && searchTerm.trim().length >= 2 && (
            <div className="py-12 text-center text-slate-500 font-mono text-xs">
              Žádné výsledky neodpovídají hledanému výrazu "{searchTerm}".
            </div>
          )}

          {searchResults.map((item, idx) => {
            const isUser = item.role === "user";
            const timeStr = new Date(item.createdAt).toLocaleString([], {
              month: "numeric",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            });

            return (
              <div
                key={`${item.threadId}-${item.messageId}-${idx}`}
                onClick={() => {
                  onSelectResult(item.threadId, item.messageId);
                  onClose();
                }}
                className="group p-3 rounded-xl bg-[#080D1F]/80 border border-slate-800 hover:border-[#00F0FF]/50 hover:bg-[#0A1128] transition-all cursor-pointer space-y-1.5"
              >
                {/* Result header */}
                <div className="flex items-center justify-between text-[10px] font-mono">
                  <span className="text-amber-400 font-bold flex items-center gap-1 truncate max-w-[280px]">
                    <MessageSquare className="w-3 h-3 text-amber-400" />
                    {item.threadTitle}
                  </span>
                  <span className="text-slate-500 flex items-center gap-1 flex-shrink-0">
                    <Clock className="w-3 h-3" />
                    {timeStr}
                  </span>
                </div>

                {/* Match context snippet */}
                <div className="flex items-start gap-2 text-xs font-sans text-slate-300">
                  <span className={`p-1 rounded flex-shrink-0 ${isUser ? "bg-cyan-500/10 text-[#00F0FF]" : "bg-purple-500/10 text-purple-400"}`}>
                    {isUser ? <User className="w-3.5 h-3.5" /> : <Brain className="w-3.5 h-3.5" />}
                  </span>
                  <div className="flex-1 leading-snug">
                    {highlightMatch(item.content, searchTerm)}
                  </div>
                </div>

                {/* Jump prompt indicator */}
                <div className="pt-1 flex items-center justify-end text-[10px] font-mono text-[#00F0FF] opacity-0 group-hover:opacity-100 transition-opacity gap-1">
                  <span>Přejít na zprávu ve vlákně</span>
                  <CornerDownRight className="w-3 h-3" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
