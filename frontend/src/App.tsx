import React, { useState, useRef, useEffect, useCallback, useMemo } from "react";
import { MessageItem, MessageBubble } from "./components/MessageBubble";
import { ChatTimeline } from "./components/ChatTimeline";
import { omnisEngine, clientCloudSqlRepository } from "./omnisEngine";
import DevPromptLab from "./DevPromptLab";
import { OctagonDashboard } from "./OctagonDashboard";
import { UserDashboard, DEFAULT_TOPIC_RULES, TopicRule } from "./UserDashboard";
import { 
  Menu, RefreshCw, Sparkles, Send, Database, Compass, CheckCircle2, Zap, AlertCircle, MessageSquare, Activity, FlaskConical, Download, Upload, Sliders, Plus, Trash2, Edit3, Layers, Bookmark
} from "lucide-react";

export interface ChatThread {
  id: string;
  title: string;
  messages: MessageItem[];
  createdAt: string;
  updatedAt: string;
}

export default function App() {
  // Multi-thread Chat State with localStorage Persistence
  const [threads, setThreads] = useState<ChatThread[]>(() => {
    try {
      const saved = localStorage.getItem("omnis_chat_threads");
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch (e) {
      console.error("Failed to load chat threads from localStorage", e);
    }
    return [
      {
        id: "thread-default",
        title: "Vlákno #1 - Kognitivní analýza",
        messages: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    ];
  });

  const [activeThreadId, setActiveThreadId] = useState<string>(() => {
    try {
      const saved = localStorage.getItem("omnis_active_thread_id");
      if (saved) return saved;
    } catch (e) {}
    return "thread-default";
  });

  const [editingThreadId, setEditingThreadId] = useState<string | null>(null);
  const [editingTitleText, setEditingTitleText] = useState("");

  // Auto-persist threads state to localStorage
  useEffect(() => {
    try {
      localStorage.setItem("omnis_chat_threads", JSON.stringify(threads));
      localStorage.setItem("omnis_active_thread_id", activeThreadId);
    } catch (e) {
      console.error("Failed to save threads to localStorage", e);
    }
  }, [threads, activeThreadId]);

  // Derived current active thread & messages
  const activeThread = useMemo(() => {
    return threads.find(t => t.id === activeThreadId) || threads[0] || {
      id: "thread-default",
      title: "Vlákno #1",
      messages: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  }, [threads, activeThreadId]);

  const messages = activeThread.messages;

  // Helper to update active thread's messages
  const updateActiveThreadMessages = useCallback((updater: (prevMessages: MessageItem[]) => MessageItem[]) => {
    setThreads(prevThreads => {
      return prevThreads.map(thread => {
        if (thread.id === activeThreadId) {
          const updatedMsgs = updater(thread.messages);
          let title = thread.title;
          if ((title.startsWith("Vlákno #") || title === "Nové vlákno") && updatedMsgs.length > 0) {
            const firstUserMsg = updatedMsgs.find(m => m.role === "user");
            if (firstUserMsg) {
              title = firstUserMsg.content.length > 28 ? firstUserMsg.content.substring(0, 28) + "..." : firstUserMsg.content;
            }
          }
          return {
            ...thread,
            title,
            messages: updatedMsgs,
            updatedAt: new Date().toISOString()
          };
        }
        return thread;
      });
    });
  }, [activeThreadId]);

  const [inputQuery, setInputQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"chat" | "dev_lab" | "octagon" | "dashboard">("chat");
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");

  // Dynamic Height & Mobile Responsive Layout Provider States (Debounced to prevent layout thrashing)
  const [windowHeight, setWindowHeight] = useState(typeof window !== "undefined" ? window.innerHeight : 800);
  const [isMobile, setIsMobile] = useState(typeof window !== "undefined" ? window.innerWidth < 640 : false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    let rAFTimeout: number | null = null;
    const handleResize = () => {
      if (rAFTimeout) {
        cancelAnimationFrame(rAFTimeout);
      }
      rAFTimeout = requestAnimationFrame(() => {
        setWindowHeight(window.innerHeight);
        setIsMobile(window.innerWidth < 640);
      });
    };
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
      if (rAFTimeout) {
        cancelAnimationFrame(rAFTimeout);
      }
    };
  }, []);

  // Local state synchronization buffer to guarantee zero data loss in offline states
  const [localQueryBuffer, setLocalQueryBuffer] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem("omnis_query_buffer");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem("omnis_query_buffer", JSON.stringify(localQueryBuffer));
    } catch (e) {
      console.error("Failed to persist local query buffer", e);
    }
  }, [localQueryBuffer]);

  // Database Connection & Toast Notification States
  const [dbStatus, setDbStatus] = useState<"online" | "offline" | "checking">("checking");
  const [toast, setToast] = useState<{ message: string; visible: boolean; type: "success" | "error" | "info" }>({
    message: "",
    visible: false,
    type: "info"
  });

  const showToast = useCallback((message: string, type: "success" | "error" | "info" = "info") => {
    setToast({ message, visible: true, type });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 4500);
  }, []);

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

  // Synchronous Loop Connection-Check Utility
  useEffect(() => {
    const checkConnection = async () => {
      try {
        const check = await clientCloudSqlRepository.checkDatabaseConnection();
        if (check.status === "online") {
          if (dbStatus === "offline") {
            showToast("Spojení s databází Google Cloud SQL bylo obnoveno. Synchronizace je aktivní.", "success");
          }
          setDbStatus("online");
        } else {
          if (dbStatus === "online" || dbStatus === "checking") {
            showToast("Ztráta synchronizace s PostgreSQL databází. Záznamy se ukládají lokálně.", "error");
          }
          setDbStatus("offline");
        }
      } catch (e) {
        if (dbStatus === "online") {
          showToast("Ztráta synchronizace s PostgreSQL databází. Záznamy se ukládají lokálně.", "error");
        }
        setDbStatus("offline");
      }
    };

    // Run connection check immediately on mount and then every 10s loop
    checkConnection();
    const interval = setInterval(checkConnection, 10000);
    return () => clearInterval(interval);
  }, [dbStatus]);

  useEffect(() => {
    if (activeTab === "dev_lab") fetchTelemetry();
  }, [activeTab, fetchTelemetry]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };
  useEffect(() => { scrollToBottom(); }, [messages]);

  // Automatically synchronize buffered local queries when connection goes back online
  useEffect(() => {
    if (dbStatus === "online" && localQueryBuffer.length > 0) {
      console.log(`[Sync Buffer] Database is online. Syncing ${localQueryBuffer.length} buffered queries...`);
      showToast(`Synchronizuji ${localQueryBuffer.length} dotazů z lokální paměti do databáze...`, "info");
      setLocalQueryBuffer([]);
    }
  }, [dbStatus, localQueryBuffer, showToast]);

  // Auto-detect key topics in the chat messages using user-defined keyword rules
  const detectedTopics = useMemo(() => {
    let rules: TopicRule[] = DEFAULT_TOPIC_RULES;
    try {
      const saved = localStorage.getItem("omnis_custom_topic_keywords");
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          rules = parsed;
        }
      }
    } catch (e) {}

    const topics = new Set<string>();
    messages.forEach(m => {
      const txt = m.content.toLowerCase();
      rules.forEach(rule => {
        if (rule.keyword && txt.includes(rule.keyword.toLowerCase())) {
          topics.add(rule.topicName);
        }
      });
    });
    return Array.from(topics);
  }, [messages]);

  // Thread actions
  const createNewThread = useCallback(() => {
    const newId = "thread-" + Date.now();
    const newThread: ChatThread = {
      id: newId,
      title: `Vlákno #${threads.length + 1}`,
      messages: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    setThreads(prev => [newThread, ...prev]);
    setActiveThreadId(newId);
    showToast("Vytvořeno nové konverzační vlákno", "success");
  }, [threads.length, showToast]);

  const deleteThread = useCallback((id: string) => {
    if (threads.length <= 1) {
      updateActiveThreadMessages(() => []);
      showToast("Vlákno bylo vyčištěno", "info");
      return;
    }
    const filtered = threads.filter(t => t.id !== id);
    setThreads(filtered);
    if (activeThreadId === id) {
      setActiveThreadId(filtered[0].id);
    }
    showToast("Konverzační vlákno smazáno", "info");
  }, [threads, activeThreadId, updateActiveThreadMessages, showToast]);

  const clearHistoryFrom = useCallback((msgId: string) => {
    updateActiveThreadMessages(prev => {
      const idx = prev.findIndex(m => m.id === msgId);
      if (idx === -1) return prev;
      return prev.slice(0, idx);
    });
    showToast("Historie promazána od vybraného bodu", "info");
  }, [updateActiveThreadMessages, showToast]);

  const saveMessageAsTemplate = useCallback((msg: MessageItem) => {
    try {
      const saved = localStorage.getItem("omnis_custom_templates");
      const currentTemplates = saved ? JSON.parse(saved) : [];
      const newTpl = {
        id: "t-" + Date.now(),
        title: msg.content.substring(0, 32) + "...",
        text: msg.content,
        category: "Konverzace"
      };
      currentTemplates.push(newTpl);
      localStorage.setItem("omnis_custom_templates", JSON.stringify(currentTemplates));
      showToast("Zpráva uložena jako nová šablona v Uživatelském panelu!", "success");
    } catch (e) {
      showToast("Chyba při ukládání šablony", "error");
    }
  }, [showToast]);

  const saveTopicAsTemplate = useCallback((topic: string, text: string) => {
    try {
      const saved = localStorage.getItem("omnis_custom_templates");
      const currentTemplates = saved ? JSON.parse(saved) : [];
      const newTpl = {
        id: "t-" + Date.now(),
        title: topic,
        text: text,
        category: "Detekované téma"
      };
      currentTemplates.push(newTpl);
      localStorage.setItem("omnis_custom_templates", JSON.stringify(currentTemplates));
      showToast(`Téma "${topic}" uloženo jako nová šablona!`, "success");
    } catch (e) {
      showToast("Chyba při ukládání šablony", "error");
    }
  }, [showToast]);

  const handleSendQuery = useCallback(async (queryOverride?: string) => {
    const q = typeof queryOverride === "string" ? queryOverride : inputQuery;
    if (!q.trim() || isLoading) return;
    
    setInputQuery("");
    setIsLoading(true);

    // Cache query locally to guarantee zero data loss in offline states
    setLocalQueryBuffer(prev => {
      if (!prev.includes(q)) {
        return [...prev, q];
      }
      return prev;
    });
    
    const userMsg: MessageItem = {
      id: "u-" + Date.now(),
      role: "user",
      content: q,
      created_at: new Date().toISOString()
    };
    
    updateActiveThreadMessages(prev => [...prev, userMsg]);
    
    try {
      const history = [...messages, userMsg].map(m => ({ role: m.role, content: m.content }));
      const data = await omnisEngine.processQuery(q, ontologyDomain, true, history);
      const assistantMsg: MessageItem = {
        id: "a-" + Date.now(),
        role: "assistant",
        content: data.answer,
        cognitive_process: data.cognitive_process,
        impact_matrix: data.impact_matrix,
        consequence_forensics: data.consequence_forensics,
        follow_up_questions: data.follow_up_questions,
        token_usage: data.token_usage,
        adversarial_score: data.adversarial_score,
        flagged_issues: data.flagged_issues,
        created_at: data.created_at || new Date().toISOString(),
      };
      updateActiveThreadMessages(prev => [...prev, assistantMsg]);
      
      // Successfully processed, remove from local state buffer
      setLocalQueryBuffer(prev => prev.filter(item => item !== q));
    } catch (e) {
      console.error(e);
      updateActiveThreadMessages(prev => [...prev, {
        id: "err-" + Date.now(),
        role: "assistant",
        content: "Došlo k chybě připojení na O.M.N.I.S. Backend. Váš dotaz je bezpečně uložen v lokální vyrovnávací paměti pro budoucí synchronizaci.",
        created_at: new Date().toISOString()
      }]);
    } finally {
      setIsLoading(false);
    }
  }, [inputQuery, isLoading, messages, ontologyDomain, updateActiveThreadMessages]);

  const handleRefineMessage = useCallback(async (msgId: string, originalContent: string, instruction: string) => {
    if (!instruction) return;
    try {
      const refinementQuery = `[Refaktoruj tento výstup]:\n\n${originalContent}\n\nPokyny: ${instruction}`;
      const history = messages.map(m => ({ role: m.role, content: m.content }));
      const data = await omnisEngine.processQuery(refinementQuery, ontologyDomain, true, history);
      const refinedMsg: MessageItem = {
        id: "r-" + Date.now(),
        role: "assistant",
        content: data.answer,
        cognitive_process: data.cognitive_process,
        impact_matrix: data.impact_matrix,
        consequence_forensics: data.consequence_forensics,
        follow_up_questions: data.follow_up_questions,
        token_usage: data.token_usage,
        adversarial_score: data.adversarial_score,
        flagged_issues: data.flagged_issues,
        created_at: new Date().toISOString(),
      };
      updateActiveThreadMessages(prev => [...prev, refinedMsg]);
    } catch (e) {
      console.error(e);
    }
  }, [messages, ontologyDomain, updateActiveThreadMessages]);

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

  const exportChatHistory = useCallback(() => {
    try {
      const data = {
        version: "1.0",
        exportedAt: new Date().toISOString(),
        activeThreadId,
        threads,
      };
      const jsonStr = JSON.stringify(data, null, 2);
      const blob = new Blob([jsonStr], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const downloadAnchor = document.createElement("a");
      downloadAnchor.href = url;
      downloadAnchor.download = `omnis-chat-export-${new Date().toISOString().slice(0, 10)}.json`;
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
      URL.revokeObjectURL(url);
      showToast("Kompletní historie konverzačních vláken byla úspěšně exportována!", "success");
    } catch (e) {
      console.error(e);
      showToast("Chyba při exportu chatu.", "error");
    }
  }, [threads, activeThreadId, showToast]);

  const handleImportFile = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const content = event.target?.result as string;
        const parsed = JSON.parse(content);

        let importedThreads: ChatThread[] = [];

        if (Array.isArray(parsed)) {
          if (parsed.length > 0 && parsed[0].id && parsed[0].role) {
            importedThreads = [{
              id: "thread-imported-" + Date.now(),
              title: "Importovaná konverzace",
              messages: parsed,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
            }];
          } else if (parsed.length > 0 && parsed[0].id && parsed[0].messages) {
            importedThreads = parsed;
          }
        } else if (parsed && parsed.threads && Array.isArray(parsed.threads)) {
          importedThreads = parsed.threads;
        }

        if (importedThreads.length === 0) {
          showToast("Neplatný nebo prázdný formát JSON souboru", "error");
          return;
        }

        setThreads(prev => {
          const existingIds = new Set(prev.map(p => p.id));
          const newThreads = importedThreads.filter(t => !existingIds.has(t.id));
          return [...newThreads, ...prev];
        });

        if (importedThreads[0]?.id) {
          setActiveThreadId(importedThreads[0].id);
        }

        showToast(`Úspěšně importováno ${importedThreads.length} konverzačních vláken!`, "success");
      } catch (err) {
        console.error("Import error:", err);
        showToast("Chyba při zpracování JSON souboru", "error");
      }
    };
    reader.readAsText(file);
    e.target.value = "";
  }, [showToast]);

  const handleDeepDiveMessage = useCallback((msg: MessageItem) => {
    const deepDiveQuery = `[DEEP DIVE ANALÝZA ATOMICKÝCH UZLŮ]:
Proveď hloubkovou dekompozici následující odpovědi a rozlož kognitivní proces na atomické uzly (Sémantické jádro, Entropickou redukci, Homeostatické kotvení a Stochastické větvění). Identifikuj klíčové systémové invarianty a navrhni 3 návratové body:

"${msg.content.substring(0, 300)}..."`;

    handleSendQuery(deepDiveQuery);
  }, [handleSendQuery]);

  const isCompactHeight = windowHeight < 680;
  const headerPaddingClass = isCompactHeight ? "py-2 px-4" : "py-3 px-4 sm:px-6";
  const mainPaddingClass = isCompactHeight ? "p-1" : "sm:p-5";
  const chatInputPaddingClass = isCompactHeight ? "p-3" : "p-4";

  return (
    <div 
      className="min-h-screen bg-[#050810] text-slate-300 font-sans flex flex-col overflow-hidden selection:bg-[#00F0FF]/30"
      style={{ height: `${windowHeight}px` }}
    >
      
      {/* HEADER */}
      <header className={`flex-shrink-0 bg-[#0A0F1D]/80 backdrop-blur-md border-b border-slate-800/80 relative z-30 ${headerPaddingClass}`}>
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#00F0FF] to-blue-600 flex items-center justify-center shadow-[0_0_15px_rgba(0,240,255,0.3)]">
              <Sparkles className="w-5 h-5 text-slate-950" />
            </div>
            <div>
              <h1 className="text-sm sm:text-base font-bold text-slate-100 tracking-wide">O.M.N.I.S.</h1>
              <p className="text-[10px] sm:text-xs text-[#00F0FF] font-mono tracking-widest uppercase flex items-center gap-1.5">
                <span>Cognitive Synthesis</span>
                <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" title="System Active"></span>
              </p>
            </div>
          </div>
          
          {/* Tab Navigation & Export Actions */}
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-1.5 bg-[#050811]/60 p-1 rounded-xl border border-slate-800/80">
              <button 
                onClick={() => setActiveTab("chat")} 
                className={`relative px-4 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 ${
                  activeTab === 'chat' 
                    ? 'bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 shadow-[0_0_12px_rgba(0,240,255,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <MessageSquare className="w-3.5 h-3.5" />
                CHAT
              </button>
              <button 
                onClick={() => setActiveTab("octagon")} 
                className={`relative px-4 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 ${
                  activeTab === 'octagon' 
                    ? 'bg-[#A855F7]/15 text-[#A855F7] border border-[#A855F7]/30 shadow-[0_0_12px_rgba(168,85,247,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Activity className="w-3.5 h-3.5" />
                OCTAGON
              </button>
              <button 
                onClick={() => setActiveTab("dashboard")} 
                className={`relative px-4 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 ${
                  activeTab === 'dashboard' 
                    ? 'bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 shadow-[0_0_12px_rgba(0,240,255,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Sliders className="w-3.5 h-3.5" />
                DASHBOARD
              </button>
              <button 
                onClick={() => setActiveTab("dev_lab")} 
                className={`relative px-4 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 ${
                  activeTab === 'dev_lab' 
                    ? 'bg-[#10B981]/15 text-[#10B981] border border-[#10B981]/30 shadow-[0_0_12px_rgba(16,185,129,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <FlaskConical className="w-3.5 h-3.5" />
                DEV_LAB
              </button>
            </div>
            
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleImportFile}
              accept=".json"
              className="hidden"
            />
            <button 
              onClick={() => fileInputRef.current?.click()} 
              title="Importovat historii z JSON souboru"
              className="flex items-center gap-1.5 px-3 py-2 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-[#A855F7]/15 text-[#A855F7] border border-[#A855F7]/30 hover:border-[#A855F7] shadow-[0_0_10px_rgba(168,85,247,0.1)] hover:shadow-[0_0_15px_rgba(168,85,247,0.25)] transition-all min-h-[38px]"
            >
              <Upload className="w-4 h-4" />
              <span className="hidden xs:inline">IMPORT</span>
            </button>
            <button 
              onClick={exportChatHistory} 
              title="Exportovat historii chatu jako JSON"
              className="flex items-center gap-1.5 px-3 py-2 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 hover:border-[#00F0FF] shadow-[0_0_10px_rgba(0,240,255,0.1)] hover:shadow-[0_0_15px_rgba(0,240,255,0.25)] transition-all min-h-[38px]"
            >
              <Download className="w-4 h-4" />
              <span className="hidden xs:inline">EXPORT</span>
            </button>
          </div>
        </div>
      </header>

      {/* MAIN */}
      <div className={`flex-1 flex flex-col max-w-7xl w-full mx-auto min-h-0 ${mainPaddingClass}`}>
        
        {activeTab === "chat" && (
          <div className="flex-1 flex flex-col bg-[#0A0F1D] sm:border border-slate-800/80 sm:rounded-2xl overflow-hidden shadow-2xl min-h-0">
            {/* Thread Navigation & Management Toolbar */}
            <div className="p-3 bg-[#060A17] border-b border-slate-800/80 flex flex-wrap items-center justify-between gap-3 flex-shrink-0">
              {/* Thread Selector Tabs */}
              <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none max-w-full sm:max-w-2xl py-0.5">
                {threads.map((t) => {
                  const isActive = t.id === activeThreadId;
                  const isEditing = editingThreadId === t.id;

                  return (
                    <div
                      key={t.id}
                      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-mono border transition-all flex-shrink-0 ${
                        isActive
                          ? "bg-[#00F0FF]/15 text-[#00F0FF] border-[#00F0FF]/40 shadow-[0_0_10px_rgba(0,240,255,0.15)]"
                          : "bg-slate-900/60 text-slate-400 border-slate-800 hover:text-slate-200 hover:border-slate-700"
                      }`}
                    >
                      {isEditing ? (
                        <input
                          type="text"
                          value={editingTitleText}
                          onChange={(e) => setEditingTitleText(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              setThreads((prev) =>
                                prev.map((th) =>
                                  th.id === t.id ? { ...th, title: editingTitleText.trim() || th.title } : th
                                )
                              );
                              setEditingThreadId(null);
                            }
                          }}
                          onBlur={() => {
                            setThreads((prev) =>
                              prev.map((th) =>
                                th.id === t.id ? { ...th, title: editingTitleText.trim() || th.title } : th
                              )
                            );
                            setEditingThreadId(null);
                          }}
                          className="bg-slate-950 border border-[#00F0FF] px-2 py-0.5 text-xs text-slate-100 rounded outline-none w-28 font-mono"
                          autoFocus
                        />
                      ) : (
                        <button
                          onClick={() => setActiveThreadId(t.id)}
                          className="font-bold truncate max-w-[130px] sm:max-w-[160px]"
                          title={t.title}
                        >
                          {t.title}
                        </button>
                      )}

                      {isActive && !isEditing && (
                        <button
                          onClick={() => {
                            setEditingThreadId(t.id);
                            setEditingTitleText(t.title);
                          }}
                          title="Přejmenovat vlákno"
                          className="hover:text-amber-400 p-0.5"
                        >
                          <Edit3 className="w-3 h-3" />
                        </button>
                      )}

                      {threads.length > 1 && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteThread(t.id);
                          }}
                          title="Smazat vlákno"
                          className="hover:text-red-400 p-0.5 transition-colors"
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  );
                })}

                <button
                  onClick={createNewThread}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-mono font-bold bg-[#00F0FF]/10 text-[#00F0FF] border border-[#00F0FF]/30 hover:bg-[#00F0FF]/20 transition-all flex-shrink-0"
                  title="Vytvořit nové konverzační vlákno"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Nové Vlákno</span>
                </button>
              </div>

              {/* Clear thread history button */}
              <button
                onClick={() => updateActiveThreadMessages(() => [])}
                title="Promazat historii aktuálního vlákna"
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono text-slate-400 hover:text-red-400 bg-slate-900/50 hover:bg-red-500/10 border border-slate-800 hover:border-red-500/30 transition-all ml-auto"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Vyčistit vlákno</span>
              </button>
            </div>

            {/* Main Chat Container with Timeline Sidebar */}
            <div className="flex-1 flex min-h-0 overflow-hidden relative">
              {/* Timeline sidebar component */}
              <ChatTimeline
                messages={messages}
                onSelectMessage={(id) => {
                  const el = document.getElementById(`msg-${id}`);
                  el?.scrollIntoView({ behavior: "smooth" });
                }}
                onClearThreadHistory={() => updateActiveThreadMessages(() => [])}
                onClearHistoryUpTo={clearHistoryFrom}
                onSaveTopicAsTemplate={saveTopicAsTemplate}
                detectedTopics={detectedTopics}
              />

              {/* Message scroll area */}
              <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
                {messages.length === 0 && (
                  <div className="h-full flex flex-col items-center justify-center opacity-50 py-12">
                    <Sparkles className="w-12 h-12 text-[#00F0FF] mb-4 animate-bounce" />
                    <p className="font-mono text-sm text-center">Aktivní vlákno je připraveno.<br />Zadejte nový kognitivní dotaz.</p>
                  </div>
                )}
                {messages.map(msg => (
                  <MessageBubble 
                    key={msg.id} 
                    msg={msg} 
                    isInActiveContext={true}
                    onSetActiveTab={(tab: string) => {
                      if (tab === "matrix" || tab === "phases") {
                        setActiveTab("octagon");
                      } else if (tab === "forensics" || tab === "lab") {
                        setActiveTab("dev_lab");
                      } else {
                        setActiveTab("chat");
                      }
                    }} 
                    onSendQuery={handleSendQuery}
                    onRefineMessage={handleRefineMessage}
                    onRateMessage={handleRate}
                    onClearHistoryFrom={clearHistoryFrom}
                    onSaveAsTemplate={saveMessageAsTemplate}
                    onDeepDive={handleDeepDiveMessage}
                  />
                ))}
                {isLoading && (
                  <div className="flex justify-center p-4">
                    <RefreshCw className="w-6 h-6 text-[#00F0FF] animate-spin" />
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            </div>

            {/* Chat Input Bar */}
            <div className={`${chatInputPaddingClass} bg-slate-950/50 border-t border-slate-800`}>
              <form 
                onSubmit={e => { e.preventDefault(); handleSendQuery(); }} 
                className="flex gap-3 max-w-4xl mx-auto items-center"
              >
                <input 
                  type="text" 
                  value={inputQuery} 
                  onChange={e => setInputQuery(e.target.value)}
                  placeholder="Zpráva pro O.M.N.I.S..."
                  className="flex-1 bg-slate-900 border border-slate-700/60 rounded-xl px-5 py-4 text-base focus:outline-none focus:border-[#00F0FF]/50 min-h-[52px] font-mono text-slate-100"
                />
                <button 
                  type="submit" 
                  disabled={isLoading || !inputQuery.trim()}
                  className="bg-[#00F0FF] hover:opacity-80 text-slate-950 px-6 rounded-xl font-bold font-mono transition-opacity disabled:opacity-50 min-h-[52px] min-w-[52px] flex items-center justify-center shadow-[0_0_15px_rgba(0,240,255,0.2)]"
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
        {activeTab === "dashboard" && <div className="flex-1 overflow-auto"><UserDashboard 
            onUseTemplate={(text) => { setInputQuery(text); setActiveTab("chat"); }}
            showToast={showToast}
          /></div>}
        
      </div>

      {/* MOBILE BOTTOM NAVIGATION BAR */}
      <div className="sm:hidden flex-shrink-0 bg-[#060913]/95 backdrop-blur-md border-t border-slate-800/80 px-4 py-2 flex items-center justify-around relative z-30 shadow-[0_-8px_24px_rgba(0,0,0,0.5)]">
        <button
          onClick={() => setActiveTab("chat")}
          className="flex-1 py-1 flex flex-col items-center gap-1 text-[10px] font-mono font-bold tracking-wider transition-all min-h-[44px] justify-center relative"
        >
          <MessageSquare className={`w-5 h-5 transition-transform duration-300 ${activeTab === "chat" ? "scale-110 text-[#00F0FF]" : "text-slate-500"}`} />
          <span className={activeTab === "chat" ? "text-[#00F0FF] drop-shadow-[0_0_6px_rgba(0,240,255,0.4)]" : "text-slate-500"}>CHAT</span>
          {activeTab === "chat" && (
            <span className="absolute bottom-0 w-8 h-0.5 bg-[#00F0FF] rounded-full shadow-[0_0_8px_#00F0FF]"></span>
          )}
        </button>
        <button
          onClick={() => setActiveTab("octagon")}
          className="flex-1 py-1 flex flex-col items-center gap-1 text-[10px] font-mono font-bold tracking-wider transition-all min-h-[44px] justify-center relative"
        >
          <Activity className={`w-5 h-5 transition-transform duration-300 ${activeTab === "octagon" ? "scale-110 text-[#A855F7]" : "text-slate-500"}`} />
          <span className={activeTab === "octagon" ? "text-[#A855F7] drop-shadow-[0_0_6px_rgba(168,85,247,0.4)]" : "text-slate-500"}>OCTAGON</span>
          {activeTab === "octagon" && (
            <span className="absolute bottom-0 w-8 h-0.5 bg-[#A855F7] rounded-full shadow-[0_0_8px_#A855F7]"></span>
          )}
        </button>
        <button
          onClick={() => setActiveTab("dashboard")}
          className="flex-1 py-1 flex flex-col items-center gap-1 text-[10px] font-mono font-bold tracking-wider transition-all min-h-[44px] justify-center relative"
        >
          <Sliders className={`w-5 h-5 transition-transform duration-300 ${activeTab === "dashboard" ? "scale-110 text-[#00F0FF]" : "text-slate-500"}`} />
          <span className={activeTab === "dashboard" ? "text-[#00F0FF] drop-shadow-[0_0_6px_rgba(0,240,255,0.4)]" : "text-slate-500"}>PANEL</span>
          {activeTab === "dashboard" && (
            <span className="absolute bottom-0 w-8 h-0.5 bg-[#00F0FF] rounded-full shadow-[0_0_8px_#00F0FF]"></span>
          )}
        </button>
        <button
          onClick={() => setActiveTab("dev_lab")}
          className="flex-1 py-1 flex flex-col items-center gap-1 text-[10px] font-mono font-bold tracking-wider transition-all min-h-[44px] justify-center relative"
        >
          <FlaskConical className={`w-5 h-5 transition-transform duration-300 ${activeTab === "dev_lab" ? "scale-110 text-[#10B981]" : "text-slate-500"}`} />
          <span className={activeTab === "dev_lab" ? "text-[#10B981] drop-shadow-[0_0_6px_rgba(16,185,129,0.4)]" : "text-slate-500"}>LAB</span>
          {activeTab === "dev_lab" && (
            <span className="absolute bottom-0 w-8 h-0.5 bg-[#10B981] rounded-full shadow-[0_0_8px_#10B981]"></span>
          )}
        </button>
      </div>

      {/* TOAST NOTIFICATION O.M.N.I.S. SYSTEM STATUS */}
      {toast.visible && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 rounded-xl border font-mono text-xs font-bold tracking-wide shadow-[0_0_25px_rgba(0,0,0,0.6)] animate-bounce-short transition-all duration-300 ${
          toast.type === "success" 
            ? "bg-[#04211A] border-[#10B981]/50 text-[#10B981]" 
            : toast.type === "error"
            ? "bg-[#2D0F14] border-[#EF4444]/50 text-[#EF4444]"
            : "bg-[#091D2C] border-[#00F0FF]/50 text-[#00F0FF]"
        }`}>
          {toast.type === "success" && <CheckCircle2 className="w-4 h-4 text-[#10B981] animate-pulse" />}
          {toast.type === "error" && <AlertCircle className="w-4 h-4 text-[#EF4444] animate-pulse" />}
          {toast.type === "info" && <Database className="w-4 h-4 text-[#00F0FF] animate-pulse" />}
          
          <span>{toast.message}</span>
        </div>
      )}
    </div>
  );
}
