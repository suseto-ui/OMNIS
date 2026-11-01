import React, { useState, useMemo } from "react";
import { 
  Clock, 
  MessageSquare, 
  User, 
  Brain, 
  Trash2, 
  Bookmark, 
  ChevronRight, 
  ChevronLeft, 
  Zap, 
  Sparkles,
  Layers,
  Activity
} from "lucide-react";
import { MessageItem } from "./MessageBubble";

interface ChatTimelineProps {
  messages: MessageItem[];
  activeMessageId?: string;
  onSelectMessage: (id: string) => void;
  onClearThreadHistory: () => void;
  onClearHistoryUpTo?: (id: string) => void;
  onSaveTopicAsTemplate: (topic: string, text: string) => void;
  detectedTopics: string[];
}

export const ChatTimeline: React.FC<ChatTimelineProps> = ({
  messages,
  activeMessageId,
  onSelectMessage,
  onClearThreadHistory,
  onClearHistoryUpTo,
  onSaveTopicAsTemplate,
  detectedTopics,
}) => {
  const [collapsed, setCollapsed] = useState(false);

  // Compute Cognitive Activity Heatmap scores for each message
  const heatmapData = useMemo(() => {
    return messages.map((msg, idx) => {
      let rawScore = 15;
      if (msg.role === "assistant") {
        rawScore += 25;
        if (msg.token_usage?.total_tokens) {
          rawScore += Math.min(msg.token_usage.total_tokens / 15, 40);
        }
        if (msg.impact_matrix?.composite_score) {
          rawScore += msg.impact_matrix.composite_score * 3;
        }
        if (msg.adversarial_score) {
          rawScore += msg.adversarial_score * 25;
        }
        if (msg.cognitive_process) {
          rawScore += 20;
        }
      }
      const score = Math.min(Math.round(rawScore), 100);

      let colorClass = "bg-slate-700 border-slate-600";
      let label = "Nízká kognitivní zátěž";
      if (score >= 75) {
        colorClass = "bg-gradient-to-t from-red-600 to-amber-500 border-amber-400 shadow-[0_0_8px_rgba(245,158,11,0.5)]";
        label = "Kritická / Adversariální intenzita";
      } else if (score >= 50) {
        colorClass = "bg-gradient-to-t from-purple-600 to-purple-400 border-purple-300 shadow-[0_0_6px_rgba(168,85,247,0.4)]";
        label = "Vysoká kognitivní analýza";
      } else if (score >= 30) {
        colorClass = "bg-gradient-to-t from-cyan-600 to-[#00F0FF] border-cyan-300 shadow-[0_0_6px_rgba(0,240,255,0.4)]";
        label = "Střední analytická zátěž";
      }

      return {
        id: msg.id,
        index: idx + 1,
        role: msg.role,
        score,
        colorClass,
        label,
        snippet: msg.content.substring(0, 30) + "..."
      };
    });
  }, [messages]);

  if (messages.length === 0) {
    return null;
  }

  return (
    <div className={`transition-all duration-300 ${collapsed ? "w-10" : "w-64 sm:w-72"} bg-[#050811]/90 border-r border-slate-800/80 flex flex-col h-full overflow-hidden text-slate-200 select-none z-10`}>
      {/* Header */}
      <div className="p-3 border-b border-slate-800/80 flex items-center justify-between bg-[#070B18]">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <Layers className="w-4 h-4 text-[#00F0FF] animate-pulse" />
            <span className="text-xs font-mono font-bold text-[#00F0FF] tracking-wider uppercase">
              Časová Osa Vlákna
            </span>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          title={collapsed ? "Rozbalit časovou osu" : "Sbalit časovou osu"}
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>

      {!collapsed && (
        <>
          {/* Cognitive Activity Heatmap Bar */}
          <div className="p-3 bg-[#060A17] border-b border-slate-800/80 space-y-1.5">
            <div className="flex items-center justify-between text-[10px] font-mono">
              <span className="text-purple-400 font-bold flex items-center gap-1">
                <Activity className="w-3 h-3 text-[#00F0FF] animate-pulse" />
                HEATMAPA INTENZITY
              </span>
              <span className="text-slate-500 font-mono text-[9px]">
                {heatmapData.length} uzlů
              </span>
            </div>

            {/* Heatmap strip container */}
            <div className="h-6 w-full bg-slate-950 p-1 rounded-lg border border-slate-800 flex items-stretch gap-0.5 overflow-x-auto scrollbar-none">
              {heatmapData.map((item) => (
                <button
                  key={item.id}
                  onClick={() => onSelectMessage(item.id)}
                  title={`#${item.index} (${item.role === 'user' ? 'Uživatel' : 'O.M.N.I.S.'}): Intenzita ${item.score}%\n${item.label}\n"${item.snippet}"`}
                  className={`flex-1 min-w-[6px] rounded-sm border transition-all hover:scale-125 hover:z-20 ${item.colorClass} ${activeMessageId === item.id ? 'ring-2 ring-white scale-110' : ''}`}
                />
              ))}
            </div>

            <div className="flex items-center justify-between text-[8px] font-mono text-slate-500 px-0.5">
              <span>Start</span>
              <div className="flex items-center gap-2">
                <span className="flex items-center gap-0.5"><span className="w-1.5 h-1.5 rounded-full bg-cyan-400" /> Nízká</span>
                <span className="flex items-center gap-0.5"><span className="w-1.5 h-1.5 rounded-full bg-purple-400" /> Vysoká</span>
                <span className="flex items-center gap-0.5"><span className="w-1.5 h-1.5 rounded-full bg-red-400" /> Max</span>
              </div>
              <span>End</span>
            </div>
          </div>

          {/* Thread Metrics Summary */}
          <div className="p-3 bg-slate-900/40 border-b border-slate-800/60 space-y-2 text-[10px] font-mono">
            <div className="flex items-center justify-between text-slate-400">
              <span>Zprávy v kontextu:</span>
              <span className="text-[#00F0FF] font-bold">{messages.length}</span>
            </div>
            <div className="flex items-center justify-between text-slate-400">
              <span>Aktivní stav kontextu:</span>
              <span className="flex items-center gap-1 text-[#10B981] font-bold">
                <span className="w-1.5 h-1.5 rounded-full bg-[#10B981] animate-ping" />
                SYNTETIZOVÁNO
              </span>
            </div>
            <button
              onClick={onClearThreadHistory}
              className="w-full mt-1 px-2 py-1 rounded bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 text-red-400 text-[10px] font-mono flex items-center justify-center gap-1.5 transition-colors"
            >
              <Trash2 className="w-3 h-3" />
              Promazat celou historii vlákna
            </button>
          </div>

          {/* Auto-detected topics section */}
          {detectedTopics.length > 0 && (
            <div className="p-3 border-b border-slate-800/60 bg-[#070B18]/50 space-y-2">
              <span className="text-[10px] font-mono font-bold text-amber-400 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                DETEKOVANÁ TÉMATA (LABS)
              </span>
              <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                {detectedTopics.map((topic, idx) => (
                  <button
                    key={idx}
                    onClick={() => {
                      const lastUserMsg = [...messages].reverse().find(m => m.role === "user");
                      onSaveTopicAsTemplate(topic, lastUserMsg ? lastUserMsg.content : topic);
                    }}
                    className="px-2 py-1 rounded text-[9px] font-mono bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1 transition-all group"
                    title="Uložit téma jako šablonu do Uživatelského panelu"
                  >
                    <Bookmark className="w-2.5 h-2.5 text-amber-400 group-hover:scale-110" />
                    <span className="truncate max-w-[130px]">{topic}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Checkpoints Timeline List */}
          <div className="flex-1 overflow-y-auto p-2 space-y-1.5 scrollbar-thin scrollbar-thumb-slate-800">
            {messages.map((msg, idx) => {
              const isUser = msg.role === "user";
              const isSelected = activeMessageId === msg.id;
              const shortText = msg.content.length > 45 ? msg.content.substring(0, 45) + "..." : msg.content;
              const timeStr = new Date(msg.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

              return (
                <div
                  key={msg.id}
                  onClick={() => onSelectMessage(msg.id)}
                  className={`group relative p-2 rounded-xl border transition-all cursor-pointer ${
                    isSelected
                      ? "bg-[#00F0FF]/15 border-[#00F0FF] shadow-[0_0_10px_rgba(0,240,255,0.15)]"
                      : "bg-[#070B18]/60 border-slate-800/70 hover:border-slate-700 hover:bg-[#070B18]"
                  }`}
                >
                  <div className="flex items-center justify-between text-[10px] font-mono mb-1">
                    <span className={`flex items-center gap-1 font-bold ${isUser ? "text-[#00F0FF]" : "text-[#A855F7]"}`}>
                      {isUser ? <User className="w-3 h-3" /> : <Brain className="w-3 h-3" />}
                      #{idx + 1} {isUser ? "Vstup" : "Syntéza"}
                    </span>
                    <span className="text-slate-500 flex items-center gap-1">
                      <Clock className="w-2.5 h-2.5" />
                      {timeStr}
                    </span>
                  </div>

                  <p className="text-xs text-slate-300 font-sans line-clamp-2 leading-tight">
                    {shortText}
                  </p>

                  <div className="mt-1.5 pt-1 border-t border-slate-800/40 flex items-center justify-between text-[9px] font-mono text-slate-500">
                    <span className="flex items-center gap-1 text-[#10B981]">
                      <Zap className="w-2.5 h-2.5" />
                      Kontext OK
                    </span>
                    {onClearHistoryUpTo && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onClearHistoryUpTo(msg.id);
                        }}
                        className="opacity-0 group-hover:opacity-100 hover:text-red-400 p-0.5 rounded transition-opacity"
                        title="Promazat historii od této zprávy nahoře"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
};
