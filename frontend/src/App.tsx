import React, { useState, useRef, useEffect, useCallback } from "react";
import { MessageItem, MessageBubble } from "./components/MessageBubble";
import { omnisEngine } from "./omnisEngine";
import DevPromptLab from "./DevPromptLab";
import { OctagonDashboard } from "./OctagonDashboard";
import { 
  Menu, RefreshCw, Sparkles, Send, Database, Compass, CheckCircle2, Zap
} from "lucide-react";

export default function App() {
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [inputQuery, setInputQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"chat" | "dev_lab" | "octagon">("chat");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");

  const [tokenTelemetry, setTokenTelemetry] = useState({ cumulative_prompt_tokens: 0, cumulative_completion_tokens: 0, cumulative_total_tokens: 0, total_queries_executed: 0, estimated_total_cost_usd: 0, estimated_total_cost_czk: 0, recent_records: [] });

  const fetchTelemetry = useCallback(async () => {
    try {
      const res = await fetch("/api/dev/token-telemetry");
      if (res.ok) {
        const data = await res.json();
        setTokenTelemetry(data);
      }
    } catch (e) {
      console.error("Failed to fetch telemetry", e);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "dev_lab") fetchTelemetry();
  }, [activeTab, fetchTelemetry]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };
  useEffect(() => { scrollToBottom(); }, [messages]);

  const handleSendQuery = useCallback(async (queryOverride?: string) => {
    const q = typeof queryOverride === "string" ? queryOverride : inputQuery;
    if (!q.trim() || isLoading) return;
    
    setInputQuery("");
    setIsLoading(true);
    
    const userMsg: MessageItem = {
      id: "u-" + Date.now(),
      role: "user",
      content: q,
      created_at: new Date().toISOString()
    };
    
    setMessages(prev => [...prev, userMsg]);
    
    try {
      const data = await omnisEngine.processQuery(q, ontologyDomain, true);
      const assistantMsg: MessageItem = {
        id: "a-" + Date.now(),
        role: "assistant",
        content: data.answer,
        cognitive_process: data.cognitive_process,
        impact_matrix: data.impact_matrix,
        consequence_forensics: data.consequence_forensics,
        follow_up_questions: data.follow_up_questions,
        token_usage: data.token_usage,
        created_at: data.created_at || new Date().toISOString(),
      };
      setMessages(prev => [...prev, assistantMsg]);
    } catch (e) {
      console.error(e);
      setMessages(prev => [...prev, {
        id: "err-" + Date.now(),
        role: "assistant",
        content: "Došlo k chybě připojení na O.M.N.I.S. Backend.",
        created_at: new Date().toISOString()
      }]);
    } finally {
      setIsLoading(false);
    }
  }, [inputQuery, isLoading, ontologyDomain]);

  const handleRefineMessage = useCallback(async (msgId: string, originalContent: string, instruction: string) => {
    if (!instruction) return;
    try {
      const refinementQuery = `[Refaktoruj tento výstup]:\n\n${originalContent}\n\nPokyny: ${instruction}`;
      const data = await omnisEngine.processQuery(refinementQuery, ontologyDomain, true);
      const refinedMsg: MessageItem = {
        id: "r-" + Date.now(),
        role: "assistant",
        content: data.answer,
        cognitive_process: data.cognitive_process,
        impact_matrix: data.impact_matrix,
        consequence_forensics: data.consequence_forensics,
        follow_up_questions: data.follow_up_questions,
        token_usage: data.token_usage,
        created_at: new Date().toISOString(),
      };
      setMessages(prev => [...prev, refinedMsg]);
    } catch (e) {
      console.error(e);
    }
  }, [ontologyDomain]);

  const handleRate = useCallback(async (msgId: string, rating: number) => {
    try {
      await fetch("/api/feedback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message_id: msgId,
          conversation_id: "00000000-0000-0000-0000-000000000000",
          user_rating: rating,
          feedback_text: `Uživatel ohodnotil odpověď jako ${rating}/5`,
        }),
      });
    } catch (e) {
      console.error(e);
    }
  }, []);

  return (
    <div className="min-h-screen bg-[#050810] text-slate-300 font-sans flex flex-col h-screen overflow-hidden selection:bg-[#00F0FF]/30">
      
      {/* HEADER */}
      <header className="flex-shrink-0 bg-[#0A0F1D]/80 backdrop-blur-md border-b border-slate-800/80 px-4 sm:px-6 py-3">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#00F0FF] to-blue-600 flex items-center justify-center shadow-[0_0_15px_rgba(0,240,255,0.3)]">
              <Sparkles className="w-5 h-5 text-slate-950" />
            </div>
            <div>
              <h1 className="text-sm sm:text-base font-bold text-slate-100 tracking-wide">O.M.N.I.S.</h1>
              <p className="text-[10px] sm:text-xs text-[#00F0FF] font-mono tracking-widest uppercase">Cognitive Synthesis</p>
            </div>
          </div>
          <div className="flex items-center gap-2 overflow-x-auto">
            <button onClick={() => setActiveTab("chat")} className={`px-3 py-1.5 rounded-lg text-xs font-bold font-mono transition-colors ${activeTab === 'chat' ? 'bg-[#00F0FF]/20 text-[#00F0FF]' : 'text-slate-400 hover:text-slate-200'}`}>CHAT</button>
            <button onClick={() => setActiveTab("octagon")} className={`px-3 py-1.5 rounded-lg text-xs font-bold font-mono transition-colors ${activeTab === 'octagon' ? 'bg-[#A855F7]/20 text-[#A855F7]' : 'text-slate-400 hover:text-slate-200'}`}>OCTAGON</button>
            <button onClick={() => setActiveTab("dev_lab")} className={`px-3 py-1.5 rounded-lg text-xs font-bold font-mono transition-colors ${activeTab === 'dev_lab' ? 'bg-[#10B981]/20 text-[#10B981]' : 'text-slate-400 hover:text-slate-200'}`}>DEV_LAB</button>
          </div>
        </div>
      </header>

      {/* MAIN */}
      <div className="flex-1 flex flex-col max-w-7xl w-full mx-auto sm:p-5 min-h-0">
        
        {activeTab === "chat" && (
          <div className="flex-1 flex flex-col bg-[#0A0F1D] sm:border border-slate-800/80 sm:rounded-2xl overflow-hidden shadow-2xl min-h-0">
            <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
              {messages.length === 0 && (
                <div className="h-full flex flex-col items-center justify-center opacity-50">
                  <Sparkles className="w-12 h-12 text-[#00F0FF] mb-4" />
                  <p className="font-mono text-sm">Systém připraven. Zadejte dotaz.</p>
                </div>
              )}
              {messages.map(msg => (
                <MessageBubble 
                  key={msg.id} 
                  msg={msg} 
                  onSetActiveTab={setActiveTab as any} 
                  onSendQuery={handleSendQuery}
                  onRefineMessage={handleRefineMessage}
                  onRateMessage={handleRate}
                />
              ))}
              {isLoading && (
                <div className="flex justify-center p-4">
                  <RefreshCw className="w-6 h-6 text-[#00F0FF] animate-spin" />
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
            
            <div className="p-4 bg-slate-950/50 border-t border-slate-800">
              <form 
                onSubmit={e => { e.preventDefault(); handleSendQuery(); }} 
                className="flex gap-3 max-w-4xl mx-auto"
              >
                <input 
                  type="text" 
                  value={inputQuery} 
                  onChange={e => setInputQuery(e.target.value)}
                  placeholder="Zpráva pro O.M.N.I.S..."
                  className="flex-1 bg-slate-900 border border-slate-700/60 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-[#00F0FF]/50"
                />
                <button 
                  type="submit" 
                  disabled={isLoading || !inputQuery.trim()}
                  className="bg-[#00F0FF] hover:opacity-80 text-slate-950 px-6 py-3 rounded-xl font-bold font-mono transition-opacity disabled:opacity-50"
                >
                  <Send className="w-5 h-5" />
                </button>
              </form>
            </div>
          </div>
        )}

        {activeTab === "dev_lab" && <div className="flex-1 overflow-auto"><DevPromptLab 
            onExecutePromptInChat={(q, d) => { setOntologyDomain(d); handleSendQuery(q); setActiveTab("chat"); }}
            currentDomain={ontologyDomain}
            onSelectDomain={setOntologyDomain}
            sessionTokenTelemetry={tokenTelemetry}
            onRefreshTelemetry={fetchTelemetry}
            onResetTelemetry={async () => {
              await fetch("/api/dev/reset-tokens", { method: "POST" });
              fetchTelemetry();
            }}
          /></div>}
        {activeTab === "octagon" && <div className="flex-1 overflow-auto"><OctagonDashboard /></div>}
        
      </div>
    </div>
  );
}
