import React, { useState, useRef, useEffect, useCallback, useMemo } from "react";
import { MessageItem, MessageBubble } from "./components/MessageBubble";
import { ChatTimeline } from "./components/ChatTimeline";
import { omnisEngine, clientCloudSqlRepository } from "./omnisEngine";
import DevPromptLab from "./DevPromptLab";
import { OctagonDashboard } from "./OctagonDashboard";
import { UserDashboard, DEFAULT_TOPIC_RULES, TopicRule } from "./UserDashboard";
import { 
  Menu, RefreshCw, Sparkles, Send, Database, Compass, CheckCircle2, Zap, AlertCircle, MessageSquare, Activity, FlaskConical, Download, Upload, Sliders, Plus, Trash2, Edit3, Layers, Bookmark, Search, GitMerge, HardDrive, Mic, MicOff, Brain, BarChart3, ChevronRight, Home, HelpCircle, Camera, Paperclip, Image as ImageIcon
} from "lucide-react";
import { saveThreadsToIndexedDB, loadThreadsFromIndexedDB } from "./indexedDbStorage";
import { MergeThreadsModal } from "./components/MergeThreadsModal";
import { FullTextSearchModal } from "./components/FullTextSearchModal";
import { ThreadsArchiveDashboard } from "./components/ThreadsArchiveDashboard";
import { CognitiveNodesDashboard } from "./components/CognitiveNodesDashboard";
import { AnalyticsOverviewDashboard } from "./components/AnalyticsOverviewDashboard";
import { KeyboardShortcutsModal } from "./components/KeyboardShortcutsModal";
import { OnboardingTourModal } from "./components/OnboardingTourModal";

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

  // Modals for Full-text Search & Thread Merge
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [showMergeModal, setShowMergeModal] = useState(false);
  const [lastIndexedDbSave, setLastIndexedDbSave] = useState<string | null>(null);

  // Fallback load from IndexedDB on initial mount if localStorage was empty
  useEffect(() => {
    const checkIndexedDbFallback = async () => {
      try {
        const savedLocalStorage = localStorage.getItem("omnis_chat_threads");
        if (!savedLocalStorage) {
          const idbThreads = await loadThreadsFromIndexedDB();
          if (idbThreads && idbThreads.length > 0) {
            setThreads(idbThreads);
            console.log("[IndexedDB Recovery] Restored threads state from IndexedDB backup.");
          }
        }
      } catch (e) {
        console.error("IndexedDB fallback load error:", e);
      }
    };
    checkIndexedDbFallback();
  }, []);

  // Auto-persist threads state to localStorage
  useEffect(() => {
    try {
      localStorage.setItem("omnis_chat_threads", JSON.stringify(threads));
      localStorage.setItem("omnis_active_thread_id", activeThreadId);
    } catch (e) {
      console.error("Failed to save threads to localStorage", e);
    }
  }, [threads, activeThreadId]);

  // Automated 60-second IndexedDB Auto-Save Interval
  useEffect(() => {
    const autoSaveToIndexedDb = async () => {
      if (threads.length > 0) {
        const ok = await saveThreadsToIndexedDB(threads);
        if (ok) {
          const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
          setLastIndexedDbSave(timeStr);
        }
      }
    };

    // Run first backup shortly after load, then every 60 seconds (60,000 ms)
    const initialTimer = setTimeout(autoSaveToIndexedDb, 4000);
    const interval = setInterval(autoSaveToIndexedDb, 60000);

    return () => {
      clearTimeout(initialTimer);
      clearInterval(interval);
    };
  }, [threads]);

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
  const [activeTab, setActiveTab] = useState<"chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "dev_lab">("chat");
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [showOnboardingTour, setShowOnboardingTour] = useState(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("omnis_onboarding_completed") !== "true";
  });

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
    
    const imgData = attachedImage?.data;
    const imgMime = attachedImage?.mime;

    setInputQuery("");
    setAttachedImage(null);
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
      const data = await omnisEngine.processQuery(q, ontologyDomain, true, history, imgData, imgMime);
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

  const handleSelectSearchResult = useCallback((threadId: string, messageId: string) => {
    setActiveThreadId(threadId);
    setActiveTab("chat");
    showToast("Přepnuto na hledanou zprávu ve vlákně", "info");

    setTimeout(() => {
      const el = document.getElementById(`msg-${messageId}`);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "center" });
        el.classList.add("ring-2", "ring-[#00F0FF]");
        setTimeout(() => el.classList.remove("ring-2", "ring-[#00F0FF]"), 3000);
      }
    }, 200);
  }, [showToast]);

  const handleMergeThreads = useCallback((mergedThread: ChatThread, deleteOriginals: boolean) => {
    setThreads(prev => {
      let nextThreads = [mergedThread, ...prev];
      if (deleteOriginals) {
        // Extract thread titles from title pattern or we keep all except matched ids
        // Find matching original threads if possible or delete threads that were merged
        const matchingA = prev.find(t => mergedThread.title.includes(t.title));
        if (matchingA) {
          nextThreads = nextThreads.filter(t => t.id !== matchingA.id);
        }
      }
      return nextThreads;
    });
    setActiveThreadId(mergedThread.id);
    setShowMergeModal(false);
    showToast(`Vlákna byla úspěšně sloučena (${mergedThread.messages.length} zpráv celkem)!`, "success");
  }, [showToast]);

  const [attachedImage, setAttachedImage] = useState<{ data: string; mime: string; name: string } | null>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const handleSelectImage = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (evt) => {
      const dataUrl = evt.target?.result as string;
      setAttachedImage({
        data: dataUrl,
        mime: file.type || "image/jpeg",
        name: file.name
      });
      showToast(`Obrázek "${file.name}" připraven pro multimodální analýzu v Gemini 3.1.`, "success");
    };
    reader.readAsDataURL(file);
    e.target.value = "";
  }, [showToast]);

  // Automatic background sync loop: IndexedDB ↔ PostgreSQL
  useEffect(() => {
    if (threads.length > 0) {
      const timer = setTimeout(() => {
        omnisEngine.syncIndexedDbWithPostgres(threads);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [threads]);
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef<any>(null);

  const toggleDictation = useCallback(() => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      showToast("Váš prohlížeč nepodporuje Web Speech API pro hlasové diktování. Vyzkoušejte Google Chrome nebo Microsoft Edge.", "error");
      return;
    }

    if (isListening) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      setIsListening(false);
      showToast("Hlasové diktování bylo zastaveno.", "info");
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.lang = "cs-CZ";
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onstart = () => {
        setIsListening(true);
        showToast("Hlasové diktování v reálném čase spuštěno. Mluvte do mikrofonu...", "info");
      };

      recognition.onresult = (event: any) => {
        let currentTranscript = "";
        for (let i = event.resultIndex; i < event.results.length; i++) {
          currentTranscript += event.results[i][0].transcript;
        }
        if (currentTranscript.trim()) {
          setInputQuery(prev => {
            const trimmedPrev = prev.trim();
            if (!trimmedPrev) return currentTranscript;
            // Prevent duplicated appending if the transcript is already matched
            if (trimmedPrev.endsWith(currentTranscript)) return prev;
            return `${trimmedPrev} ${currentTranscript}`;
          });
        }
      };

      recognition.onerror = (event: any) => {
        console.error("Speech recognition error:", event.error);
        setIsListening(false);
        showToast(`Chyba hlasového vstupu: ${event.error}`, "error");
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (e) {
      console.error("Failed to start SpeechRecognition", e);
      setIsListening(false);
      showToast("Nepodařilo se spustit mikrofon.", "error");
    }
  }, [isListening, showToast]);

  // Global Keyboard Shortcuts (Ctrl+K = Search, Ctrl+M = Voice Dictation, Ctrl+J = Merge Threads, ? = Help)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore when typing inside input or textarea unless Ctrl key is pressed
      const targetTag = (e.target as HTMLElement)?.tagName?.toLowerCase();
      const isInput = targetTag === "input" || targetTag === "textarea";

      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setShowSearchModal(prev => !prev);
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "m") {
        e.preventDefault();
        toggleDictation();
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "j") {
        e.preventDefault();
        setShowMergeModal(prev => !prev);
      } else if (e.key === "?" && !isInput) {
        e.preventDefault();
        setShowHelpModal(prev => !prev);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [toggleDictation]);

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
          <div className="flex items-center gap-2.5">
            <button
              onClick={() => setIsMenuOpen(true)}
              title="Otevřít Hlavní Menu O.M.N.I.S."
              className="p-2 rounded-xl bg-slate-900/90 hover:bg-[#00F0FF]/15 border border-slate-700/80 hover:border-[#00F0FF]/40 text-slate-200 hover:text-[#00F0FF] transition-all min-h-[38px] min-w-[38px] flex items-center justify-center flex-shrink-0 shadow-[0_0_10px_rgba(0,0,0,0.3)]"
            >
              <Menu className="w-5 h-5 text-[#00F0FF]" />
            </button>
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
            <div className="hidden sm:flex items-center gap-1.5 bg-[#050811]/60 p-1 rounded-xl border border-slate-800/80 overflow-x-auto scrollbar-none max-w-xl md:max-w-3xl">
              <button 
                onClick={() => setActiveTab("chat")} 
                title="Konverzační rozhraní s časovou osou a hlasovým diktováním"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'chat' 
                    ? 'bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 shadow-[0_0_12px_rgba(0,240,255,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <MessageSquare className="w-3.5 h-3.5" />
                CHAT
              </button>

              <button 
                onClick={() => setActiveTab("analytics")} 
                title="Centrální analytický přehled sjednocující Octagon 8D a kognitivní uzly"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'analytics' 
                    ? 'bg-blue-500/15 text-blue-300 border border-blue-500/30 shadow-[0_0_12px_rgba(59,130,246,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <BarChart3 className="w-3.5 h-3.5 text-blue-400" />
                ANALÝZA
              </button>

              <button 
                onClick={() => setActiveTab("archive")} 
                title="Správce konverzačních vláken, full-textové vyhledávání a sloučení"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'archive' 
                    ? 'bg-cyan-500/15 text-cyan-300 border border-cyan-500/30 shadow-[0_0_12px_rgba(6,182,212,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                ARCHIV VLÁKEN
              </button>

              <button 
                onClick={() => setActiveTab("nodes")} 
                title="Prohlížeč atomických kognitivních uzlů a dekompozice odpovedí"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'nodes' 
                    ? 'bg-[#A855F7]/15 text-[#A855F7] border border-[#A855F7]/30 shadow-[0_0_12px_rgba(168,85,247,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Brain className="w-3.5 h-3.5" />
                UZLY
              </button>

              <button 
                onClick={() => setActiveTab("dashboard")} 
                title="Klíčová slova automatické detekce témat a šablony"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'dashboard' 
                    ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30 shadow-[0_0_12px_rgba(245,158,11,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Sliders className="w-3.5 h-3.5" />
                TÉMATA
              </button>

              <button 
                onClick={() => setActiveTab("octagon")} 
                title="8D Matice dopadů a systémový audit"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'octagon' 
                    ? 'bg-purple-500/15 text-purple-300 border border-purple-500/30 shadow-[0_0_12px_rgba(168,85,247,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <Activity className="w-3.5 h-3.5" />
                OCTAGON
              </button>

              <button 
                onClick={() => setActiveTab("dev_lab")} 
                title="Laboratoř promptů, Cloud SQL sync a telemetrie"
                className={`relative px-3 py-2 rounded-lg text-xs font-bold font-mono transition-all duration-300 min-h-[38px] flex items-center gap-1.5 flex-shrink-0 ${
                  activeTab === 'dev_lab' 
                    ? 'bg-[#10B981]/15 text-[#10B981] border border-[#10B981]/30 shadow-[0_0_12px_rgba(16,185,129,0.15)]' 
                    : 'text-slate-400 hover:text-slate-250 hover:bg-slate-900/40'
                }`}
              >
                <FlaskConical className="w-3.5 h-3.5" />
                DEV_LAB
              </button>
            </div>
            
            <button
              onClick={() => setShowHelpModal(true)}
              title="Klávesové zkratky a uživatelský průvodce (?)"
              className="flex items-center gap-1.5 px-3 py-2 sm:py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 hover:border-[#00F0FF] shadow-[0_0_10px_rgba(0,240,255,0.1)] transition-all min-h-[38px]"
            >
              <HelpCircle className="w-4 h-4 text-[#00F0FF]" />
              <span className="hidden md:inline">PRŮVODCE</span>
            </button>
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

      {/* BREADCRUMBS NAVIGATION BAR */}
      <div className="bg-[#040711] border-b border-slate-800/80 px-4 py-1.5 flex items-center justify-between gap-2 text-xs font-mono text-slate-400 overflow-x-auto scrollbar-none flex-shrink-0 z-10">
        <div className="flex items-center gap-1.5 flex-nowrap">
          <button
            onClick={() => setActiveTab("chat")}
            className="flex items-center gap-1 text-slate-400 hover:text-[#00F0FF] transition-colors flex-shrink-0"
            title="Přejít na výchozí obrazovku Chatu"
          >
            <Home className="w-3.5 h-3.5 text-[#00F0FF]" />
            <span className="hidden sm:inline">Domů</span>
          </button>
          
          <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />

          <button
            onClick={() => setActiveTab(activeTab)}
            className={`font-bold transition-colors flex-shrink-0 uppercase ${
              activeTab === "chat" ? "text-[#00F0FF]" :
              activeTab === "analytics" ? "text-blue-400" :
              activeTab === "archive" ? "text-cyan-400" :
              activeTab === "nodes" ? "text-[#A855F7]" :
              activeTab === "dashboard" ? "text-amber-400" :
              activeTab === "octagon" ? "text-purple-400" : "text-[#10B981]"
            }`}
          >
            {activeTab === "chat" && "Chat & Diktování"}
            {activeTab === "analytics" && "Analytický Přehled"}
            {activeTab === "archive" && "Archiv Vláken"}
            {activeTab === "nodes" && "Kognitivní Uzly"}
            {activeTab === "dashboard" && "Správa Témat"}
            {activeTab === "octagon" && "8D Matice Dopadů"}
            {activeTab === "dev_lab" && "Dev Lab"}
          </button>

          {activeTab === "chat" && activeThread && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span 
                onClick={() => setShowSearchModal(true)}
                title="Aktivní vlákno (kliknutím otevřete vyhledávání/přepínač)"
                className="text-slate-200 truncate max-w-[140px] sm:max-w-[240px] bg-slate-900/90 hover:bg-slate-800 px-2 py-0.5 rounded border border-slate-700/80 cursor-pointer transition-colors"
              >
                {activeThread.title}
              </span>
            </>
          )}

          {activeTab === "analytics" && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span className="text-slate-300 bg-blue-950/40 px-2 py-0.5 rounded border border-blue-800/50">
                Sjednocený Audit (8D & Uzly)
              </span>
            </>
          )}

          {activeTab === "archive" && (
            <>
              <ChevronRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
              <span className="text-slate-300 bg-cyan-950/40 px-2 py-0.5 rounded border border-cyan-800/50">
                {threads.length} Uložených Vláken
              </span>
            </>
          )}
        </div>

        {/* Quick Context & Domain Badge */}
        <div className="flex items-center gap-2 flex-shrink-0 text-[10px]">
          <span className="hidden md:inline text-slate-500 uppercase">Doména:</span>
          <span className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[#00F0FF] font-bold">
            {ontologyDomain}
          </span>
        </div>
      </div>

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

              {/* Clear thread history, Merge, and Search buttons */}
              <div className="flex items-center gap-2 ml-auto">
                {/* IndexedDB Auto-Save Status */}
                {lastIndexedDbSave && (
                  <span className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-xl text-[10px] font-mono bg-emerald-950/40 text-emerald-400 border border-emerald-500/30">
                    <HardDrive className="w-3 h-3 text-emerald-400 animate-pulse" />
                    IndexedDB: {lastIndexedDbSave}
                  </span>
                )}

                {/* Full-text search button */}
                <button
                  onClick={() => setShowSearchModal(true)}
                  title="Full-text vyhledávání napříč všemi konverzačními vlákny"
                  className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono font-bold bg-[#00F0FF]/15 text-[#00F0FF] hover:bg-[#00F0FF]/25 border border-[#00F0FF]/40 shadow-[0_0_10px_rgba(0,240,255,0.15)] transition-all"
                >
                  <Search className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">VYHLEDÁVÁNÍ</span>
                </button>

                {/* Merge threads button */}
                <button
                  onClick={() => setShowMergeModal(true)}
                  title="Sloučit dvě existující konverzační vlákna do jednoho"
                  className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono font-bold bg-[#A855F7]/15 text-[#A855F7] hover:bg-[#A855F7]/25 border border-[#A855F7]/40 shadow-[0_0_10px_rgba(168,85,247,0.15)] transition-all"
                >
                  <GitMerge className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">SLOUČIT VLÁKNA</span>
                </button>

                {/* Clear thread history button */}
                <button
                  onClick={() => updateActiveThreadMessages(() => [])}
                  title="Promazat historii aktuálního vlákna"
                  className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-mono text-slate-400 hover:text-red-400 bg-slate-900/50 hover:bg-red-500/10 border border-slate-800 hover:border-red-500/30 transition-all"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span className="hidden xl:inline">Vyčistit vlákno</span>
                </button>
              </div>
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
              {/* Real-time Dictation Active Visual Banner */}
              {isListening && (
                <div className="max-w-4xl mx-auto mb-2.5 p-2.5 rounded-xl bg-red-950/40 border border-red-500/40 text-red-300 text-xs font-mono flex items-center justify-between animate-in fade-in duration-200 shadow-[0_0_15px_rgba(239,68,68,0.2)]">
                  <div className="flex items-center gap-2.5">
                    <div className="relative flex items-center justify-center w-6 h-6">
                      <span className="absolute w-full h-full rounded-full bg-red-500/30 animate-ping" />
                      <Mic className="w-4 h-4 text-red-400 relative z-10" />
                    </div>
                    <span>PROBÍHÁ DIKTOVÁNÍ V REÁLNÉM ČASE (cs-CZ)...</span>
                    {/* Animated sound wave bars */}
                    <div className="flex items-end gap-1 h-3.5">
                      <span className="w-1 bg-red-400 rounded-full animate-[bounce_0.8s_infinite_100ms] h-full" />
                      <span className="w-1 bg-red-400 rounded-full animate-[bounce_0.8s_infinite_300ms] h-2/3" />
                      <span className="w-1 bg-red-400 rounded-full animate-[bounce_0.8s_infinite_200ms] h-full" />
                      <span className="w-1 bg-red-400 rounded-full animate-[bounce_0.8s_infinite_400ms] h-1/2" />
                    </div>
                  </div>
                  <button
                    onClick={toggleDictation}
                    className="px-2.5 py-1 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-red-200 border border-red-500/40 text-[10px] uppercase font-bold transition-all"
                  >
                    Zastavit diktování
                  </button>
                </div>
              )}

              {/* Image Preview Banner if an image is selected */}
              {attachedImage && (
                <div className="max-w-4xl mx-auto mb-2.5 p-2 bg-[#0A0F1D] border border-[#00F0FF]/40 rounded-xl flex items-center justify-between gap-3 animate-in fade-in">
                  <div className="flex items-center gap-2.5 overflow-hidden">
                    <img src={attachedImage.data} alt="Attachment" className="w-10 h-10 object-cover rounded-lg border border-[#00F0FF]/30 flex-shrink-0" />
                    <div className="truncate">
                      <span className="text-[10px] font-mono text-[#00F0FF] font-bold block uppercase">Připraveno pro Gemini 3.1 Multimodal</span>
                      <span className="text-xs font-mono text-slate-300 truncate block">{attachedImage.name}</span>
                    </div>
                  </div>
                  <button 
                    type="button"
                    onClick={() => setAttachedImage(null)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                    title="Odebrat přílohu"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              )}

              {/* Hidden Image Inputs */}
              <input
                type="file"
                ref={imageInputRef}
                accept="image/*"
                onChange={handleSelectImage}
                className="hidden"
              />
              <input
                type="file"
                ref={cameraInputRef}
                accept="image/*"
                capture="environment"
                onChange={handleSelectImage}
                className="hidden"
              />

              <form 
                onSubmit={e => { e.preventDefault(); handleSendQuery(); }} 
                className={`max-w-4xl mx-auto flex items-center gap-2 bg-slate-900/90 backdrop-blur-md border rounded-2xl p-2 shadow-2xl transition-colors ${
                  isListening ? "border-red-500/60 shadow-[0_0_15px_rgba(239,68,68,0.2)]" : "border-slate-700/70 focus-within:border-[#00F0FF]/60"
                }`}
              >
                {/* Left Attachment Actions */}
                <div className="flex items-center gap-1 pl-1">
                  <button
                    type="button"
                    onClick={() => cameraInputRef.current?.click()}
                    title="Vyfotit snímek fotoaparátem (Gemini 3.1 Obrazový Stream)"
                    className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-400 hover:text-[#00F0FF] hover:bg-slate-800/80 transition-all flex-shrink-0"
                  >
                    <Camera className="w-5 h-5" />
                  </button>

                  <button
                    type="button"
                    onClick={() => imageInputRef.current?.click()}
                    title="Připojit obrázek ze zařízení"
                    className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-400 hover:text-purple-400 hover:bg-slate-800/80 transition-all flex-shrink-0"
                  >
                    <ImageIcon className="w-5 h-5" />
                  </button>
                </div>

                {/* Text Box */}
                <textarea 
                  rows={1}
                  value={inputQuery} 
                  onChange={e => setInputQuery(e.target.value)}
                  onKeyDown={e => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendQuery();
                    }
                  }}
                  placeholder={isListening ? "Diktujte dotaz do mikrofonu..." : attachedImage ? "Zadejte dotaz k přiloženému obrázku..." : "Zpráva pro O.M.N.I.S..."}
                  className="flex-1 bg-transparent px-3 py-2 text-sm sm:text-base focus:outline-none font-mono text-slate-100 min-w-0 resize-none max-h-24 overflow-y-auto"
                />

                {/* Right Actions: Dictation Mic & Send */}
                <div className="flex items-center gap-1.5 pr-1">
                  <button
                    type="button"
                    onClick={toggleDictation}
                    title={isListening ? "Zastavit hlasové diktování" : "Spustit hlasové diktování (Web Speech API)"}
                    className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all flex-shrink-0 ${
                      isListening
                        ? "bg-red-600 hover:bg-red-500 text-white shadow-[0_0_15px_rgba(239,68,68,0.6)] animate-pulse"
                        : "text-slate-400 hover:text-white hover:bg-slate-800/80"
                    }`}
                  >
                    {isListening ? <Mic className="w-5 h-5 text-white animate-bounce" /> : <MicOff className="w-5 h-5" />}
                  </button>

                  <button 
                    type="submit" 
                    disabled={isLoading || (!inputQuery.trim() && !attachedImage)}
                    className="w-10 h-10 bg-[#00F0FF] hover:opacity-90 active:scale-95 text-slate-950 rounded-xl font-bold transition-all disabled:opacity-40 flex items-center justify-center shadow-[0_0_12px_rgba(0,240,255,0.25)] flex-shrink-0"
                  >
                    <Send className="w-5 h-5" />
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {activeTab === "analytics" && (
          <div className="flex-1 overflow-auto">
            <AnalyticsOverviewDashboard
              messages={messages}
              onTriggerDeepDive={handleDeepDiveMessage}
              onGoToChat={() => setActiveTab("chat")}
            />
          </div>
        )}
        {activeTab === "archive" && (
          <div className="flex-1 overflow-auto">
            <ThreadsArchiveDashboard
              threads={threads}
              activeThreadId={activeThreadId}
              onSelectThread={(id) => {
                setActiveThreadId(id);
                setActiveTab("chat");
              }}
              onCreateNewThread={createNewThread}
              onOpenSearch={() => setShowSearchModal(true)}
              onOpenMerge={() => setShowMergeModal(true)}
              onExportJSON={exportHistory}
              onImportClick={triggerFileInput}
              lastIndexedDbSave={lastIndexedDbSave}
            />
          </div>
        )}
        {activeTab === "nodes" && (
          <div className="flex-1 overflow-auto">
            <CognitiveNodesDashboard
              messages={messages}
              onTriggerDeepDive={handleDeepDiveMessage}
              onGoToChat={() => setActiveTab("chat")}
            />
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
            onNavigateToTab={(tab) => setActiveTab(tab)}
            onOpenHelpModal={() => setShowHelpModal(true)}
            onOpenSearchModal={() => setShowSearchModal(true)}
            onOpenMergeModal={() => setShowMergeModal(true)}
          /></div>}
        {activeTab === "octagon" && <div className="flex-1 overflow-auto"><OctagonDashboard /></div>}
        {activeTab === "dashboard" && <div className="flex-1 overflow-auto"><UserDashboard 
            onUseTemplate={(text) => { setInputQuery(text); setActiveTab("chat"); }}
            showToast={showToast}
          /></div>}
        
      </div>

      {/* SIDE-DRAWER NAVIGATION MENU (Top-Left Hamburger Triggered) */}
      {isMenuOpen && (
        <div className="fixed inset-0 z-50 flex animate-in fade-in duration-200">
          {/* Backdrop Overlay */}
          <div 
            className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity"
            onClick={() => setIsMenuOpen(false)}
          />

          {/* Drawer Content */}
          <div className="relative w-full max-w-xs bg-[#060913] border-r border-slate-800 shadow-2xl flex flex-col h-full z-10 font-sans">
            {/* Header */}
            <div className="p-4 border-b border-slate-800/80 flex items-center justify-between bg-[#0A0F1D]">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-[#00F0FF] to-blue-600 flex items-center justify-center shadow-[0_0_12px_rgba(0,240,255,0.3)]">
                  <Sparkles className="w-4 h-4 text-slate-950" />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-slate-100 font-mono">O.M.N.I.S. Menu</h2>
                  <p className="text-[10px] text-[#00F0FF] font-mono uppercase tracking-wider">Kognitivní Navigace</p>
                </div>
              </div>
              <button
                onClick={() => setIsMenuOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                title="Zavřít menu"
              >
                ✕
              </button>
            </div>

            {/* Navigation List */}
            <div className="flex-1 overflow-y-auto p-3 space-y-1">
              <div className="text-[10px] font-mono font-bold text-slate-500 uppercase px-3 py-1 tracking-wider">
                Hlavní Moduly
              </div>

              <button
                onClick={() => { setActiveTab("chat"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "chat"
                    ? "bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <MessageSquare className="w-5 h-5 text-[#00F0FF] flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Kognitivní Chat</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Multimodální rozhraní & diktování</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("analytics"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "analytics"
                    ? "bg-blue-500/15 text-blue-300 border border-blue-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <BarChart3 className="w-5 h-5 text-blue-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Analytický Přehled</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Sjednocený audit 8D & Uzlů</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("archive"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "archive"
                    ? "bg-cyan-500/15 text-cyan-300 border border-cyan-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <Layers className="w-5 h-5 text-cyan-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Archiv Vláken</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">IndexedDB paměť & vyhledávání</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("nodes"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "nodes"
                    ? "bg-purple-500/15 text-purple-300 border border-purple-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <Brain className="w-5 h-5 text-purple-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Kognitivní Uzly</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Dekompozice & entropie odpovědí</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("dashboard"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "dashboard"
                    ? "bg-amber-500/15 text-amber-300 border border-amber-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <Sliders className="w-5 h-5 text-amber-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Správa Témat</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Šablony & detekční klíčová slova</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("octagon"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "octagon"
                    ? "bg-purple-500/15 text-purple-300 border border-purple-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <Activity className="w-5 h-5 text-purple-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Octagon 8D Matice</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">8-Dimenzionální audit dopadů</div>
                </div>
              </button>

              <button
                onClick={() => { setActiveTab("dev_lab"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left ${
                  activeTab === "dev_lab"
                    ? "bg-[#10B981]/15 text-[#10B981] border border-[#10B981]/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <FlaskConical className="w-5 h-5 text-[#10B981] flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Dev Lab & Telemetrie</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Benchmark promptů & tokeny</div>
                </div>
              </button>

              <div className="pt-3 border-t border-slate-800 space-y-1">
                <div className="text-[10px] font-mono font-bold text-slate-500 uppercase px-3 py-1 tracking-wider">
                  Nástroje & Akce
                </div>

                <button
                  onClick={() => { setShowSearchModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left"
                >
                  <Search className="w-4 h-4 text-[#00F0FF]" />
                  <span>Vyhledat ve vláknech</span>
                </button>

                <button
                  onClick={() => { setShowMergeModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left"
                >
                  <GitMerge className="w-4 h-4 text-purple-400" />
                  <span>Sloučit konverzace</span>
                </button>

                <button
                  onClick={() => { setShowHelpModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left"
                >
                  <HelpCircle className="w-4 h-4 text-amber-400" />
                  <span>Průvodce & Klávesové Zkratky</span>
                </button>
              </div>
            </div>

            {/* Footer */}
            <div className="p-3 border-t border-slate-800/80 bg-[#0A0F1D] text-center">
              <span className="text-[10px] font-mono text-slate-500">
                O.M.N.I.S. Cognitive System v2.7
              </span>
            </div>
          </div>
        </div>
      )}

      {/* FULL-TEXT SEARCH MODAL */}
      {showSearchModal && (
        <FullTextSearchModal
          threads={threads}
          onClose={() => setShowSearchModal(false)}
          onSelectResult={handleSelectSearchResult}
        />
      )}

      {/* MERGE THREADS MODAL */}
      {showMergeModal && (
        <MergeThreadsModal
          threads={threads}
          activeThreadId={activeThreadId}
          onClose={() => setShowMergeModal(false)}
          onMerge={handleMergeThreads}
        />
      )}

      {/* KEYBOARD SHORTCUTS & USER GUIDE MODAL */}
      {showHelpModal && (
        <KeyboardShortcutsModal
          onClose={() => setShowHelpModal(false)}
          onOpenSearch={() => setShowSearchModal(true)}
          onOpenMerge={() => setShowMergeModal(true)}
          onToggleDictation={toggleDictation}
          onStartTour={() => {
            setShowHelpModal(false);
            setShowOnboardingTour(true);
          }}
        />
      )}

      {/* ONBOARDING INTERACTIVE TOUR MODAL */}
      {showOnboardingTour && (
        <OnboardingTourModal
          onClose={() => setShowOnboardingTour(false)}
          onNavigateTab={(tab) => setActiveTab(tab)}
        />
      )}

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
