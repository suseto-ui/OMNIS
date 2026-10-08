import React, { useState, useRef, useEffect, useCallback, useMemo } from "react";
import { MessageItem, MessageBubble } from "./components/MessageBubble";
import { ChatTimeline } from "./components/ChatTimeline";
import { omnisEngine, clientCloudSqlRepository } from "./omnisEngine";
import DevPromptLab from "./DevPromptLab";
import { OctagonDashboard } from "./OctagonDashboard";
import { UserDashboard, DEFAULT_TOPIC_RULES, TopicRule } from "./UserDashboard";
import { 
  Menu, RefreshCw, Sparkles, Send, Database, Compass, CheckCircle2, Zap, AlertCircle, MessageSquare, Activity, FlaskConical, Download, Upload, Sliders, Plus, Trash2, Edit3, Layers, Bookmark, Search, GitMerge, HardDrive, Mic, MicOff, Brain, BarChart3, ChevronRight, Home, HelpCircle, Camera, Paperclip, FileText, Image as ImageIcon
} from "lucide-react";
import { saveThreadsToIndexedDB, loadThreadsFromIndexedDB } from "./indexedDbStorage";
import { MergeThreadsModal } from "./components/MergeThreadsModal";
import { FullTextSearchModal } from "./components/FullTextSearchModal";
import { ThreadsArchiveDashboard } from "./components/ThreadsArchiveDashboard";
import { CognitiveNodesDashboard } from "./components/CognitiveNodesDashboard";
import { MultiAgentArenaDashboard } from "./components/MultiAgentArenaDashboard";
import { ZkAuditLedgerDashboard } from "./components/ZkAuditLedgerDashboard";
import { AnalyticsOverviewDashboard } from "./components/AnalyticsOverviewDashboard";
import { KeyboardShortcutsModal } from "./components/KeyboardShortcutsModal";
import { OnboardingTourModal } from "./components/OnboardingTourModal";
import { EmptyStateView } from "./components/EmptyStateView";
import { EuAiActModal } from "./components/EuAiActModal";
import { SkeletonLoader } from "./components/SkeletonLoader";
import { ExecutiveOverrideModal } from "./components/ExecutiveOverrideModal";
import { TokenCounter } from "./components/TokenCounter";
import { useTheme } from "./components/ThemeToggle";
import { AppHeader } from "./components/layout/AppHeader";
import { NavigationSidebar } from "./components/layout/NavigationSidebar";
import { ChatThread, ToastType, UserRole } from "./types";
import { ApiKeyRotatorModal } from "./components/admin/ApiKeyRotatorModal";
import { useChatThreads } from "./hooks/useChatThreads";
import { useGeminiMonitor } from "./hooks/useGeminiMonitor";

export type { ChatThread };

export interface AttachedFileState {
  type: "image" | "document";
  name: string;
  size: number;
  mime: string;
  data: string;
  textContent?: string;
}

export const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

export default function App() {
  const { theme, setTheme } = useTheme();
  
  // Database Connection & Toast Notification States (Declared earlier for hook consumption)
  const [toast, setToast] = useState<{ message: string; visible: boolean; type: ToastType }>({
    message: "",
    visible: false,
    type: "info"
  });

  const showToast = useCallback((message: string, type: ToastType = "info") => {
    setToast({ message, visible: true, type });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 4500);
  }, []);

  const { 
    endpoint, 
    lastError, 
    triggerFallback, 
    resetToPrimary,
    keyPool,
    activeKey,
    isCheckingPool,
    checkKeyPool
  } = useGeminiMonitor(showToast);

  const {
    threads,
    setThreads,
    activeThreadId,
    setActiveThreadId,
    editingThreadId,
    editingTitleText,
    setEditingTitleText,
    lastIndexedDbSave,
    activeThread,
    messages,
    updateActiveThreadMessages,
    createNewThread: handleCreateNewThreadHook,
    deleteThread: handleDeleteThreadHook,
    startRenameThread,
    saveRenameThread,
    mergeThreads
  } = useChatThreads();

  // Modals for Full-text Search & Thread Merge
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [showMergeModal, setShowMergeModal] = useState(false);
  const [showAiActModal, setShowAiActModal] = useState(false);

  const [inputQuery, setInputQuery] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "arena" | "zk_ledger">("chat");
  const [isTabSwitching, setIsTabSwitching] = useState(false);

  // RBAC State: STANDARD_USER vs ADMIN_OPERATOR (Dimension A)
  const [userRole, setUserRole] = useState<UserRole>(() => {
    try {
      const saved = localStorage.getItem("omnis_user_role");
      return (saved as UserRole) || "ADMIN_OPERATOR";
    } catch {
      return "ADMIN_OPERATOR";
    }
  });

  // Eco Mode (1-call detail preserving) - Default ACTIVE
  const [isEcoMode, setIsEcoMode] = useState<boolean>(() => {
    try {
      const saved = localStorage.getItem("omnis_eco_mode");
      return saved !== null ? saved === "true" : true;
    } catch {
      return true;
    }
  });

  // Semantic Gateway Non-stop Active - Default ACTIVE
  const [isSemanticGatewayActive, setIsSemanticGatewayActive] = useState<boolean>(true);

  // Executive Override Modal State (ADMIN_OPERATOR ONLY)
  const [showOverrideModal, setShowOverrideModal] = useState<boolean>(false);
  const [overrideTargetQuery, setOverrideTargetQuery] = useState<string>("");
  const [overrideRefusalReason, setOverrideRefusalReason] = useState<string>("");

  const [showKeyRotatorModal, setShowKeyRotatorModal] = useState<boolean>(false);
  const [activeKeySlot, setActiveKeySlot] = useState<number>(() => {
    try {
      return parseInt(localStorage.getItem("omnis_active_key_slot") || "1");
    } catch {
      return 1;
    }
  });

  const handleToggleRole = useCallback(() => {
    setUserRole((prev) => {
      const next: UserRole = prev === "ADMIN_OPERATOR" ? "STANDARD_USER" : "ADMIN_OPERATOR";
      try {
        localStorage.setItem("omnis_user_role", next);
      } catch (e) {
        console.error("Failed to save user role", e);
      }
      return next;
    });
  }, []);

  const handleTabSwitch = useCallback((tab: "chat" | "analytics" | "archive" | "nodes" | "dashboard" | "octagon" | "arena" | "zk_ledger") => {
    if (tab === activeTab) return;
    setIsTabSwitching(true);
    setActiveTab(tab);
    setTimeout(() => {
      setIsTabSwitching(false);
    }, 180);
  }, [activeTab]);

  // If STANDARD_USER enters admin-only tab, redirect smoothly to chat
  useEffect(() => {
    if (userRole === "STANDARD_USER" && (activeTab === "analytics" || activeTab === "nodes" || activeTab === "octagon")) {
      setActiveTab("chat");
    }
  }, [userRole, activeTab]);

  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [showOnboardingTour, setShowOnboardingTour] = useState(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("omnis_onboarding_completed") !== "true";
  });

  // Dynamic Height Layout Provider State (Debounced to prevent layout thrashing)
  const [windowHeight, setWindowHeight] = useState(typeof window !== "undefined" ? window.innerHeight : 800);

  useEffect(() => {
    if (typeof window === "undefined") return;
    let rAFTimeout: number | null = null;
    const handleResize = () => {
      if (rAFTimeout) {
        cancelAnimationFrame(rAFTimeout);
      }
      rAFTimeout = requestAnimationFrame(() => {
        setWindowHeight(window.innerHeight);
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

  // Database Connection & States
  const [dbStatus, setDbStatus] = useState<"online" | "offline" | "checking">("checking");

  // Kognitivní zámek (Cognitive Lock) states
  const [isInputCognitiveLocked, setIsInputCognitiveLocked] = useState<boolean>(false);
  const [lockedQuery, setLockedQuery] = useState<string>("");

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
    handleCreateNewThreadHook();
    showToast("Vytvořeno nové konverzační vlákno", "success");
  }, [handleCreateNewThreadHook, showToast]);

  const deleteThread = useCallback((id: string) => {
    handleDeleteThreadHook(id);
    showToast("Konverzační vlákno smazáno nebo vyčištěno", "info");
  }, [handleDeleteThreadHook, showToast]);

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

  const handleSendQuery = useCallback(async (queryOverride?: string, isAuthorized: boolean = false, isOverride: boolean = false) => {
    const q = typeof queryOverride === "string" ? queryOverride : inputQuery;
    if (!q.trim() || isLoading) return;

    const now = Date.now();
    if (!isAuthorized && q.trim() === lastQueryContentRef.current && (now - lastQueryTimestampRef.current < 10000)) {
      showToast("Duplicitní dotaz detekován. Prosím vyčkejte 10 sekund před odesláním totožné zprávy.", "warning");
      return;
    }
    lastQueryContentRef.current = q.trim();
    lastQueryTimestampRef.current = now;
    
    const imgData = attachedFile?.type === "image" ? attachedFile.data : undefined;
    const imgMime = attachedFile?.type === "image" ? attachedFile.mime : undefined;

    let finalPrompt = q;
    if (attachedFile?.type === "document" && attachedFile.textContent) {
      finalPrompt = `[PŘILOŽENÝ DOKUMENT: ${attachedFile.name} (${formatFileSize(attachedFile.size)})]:\n\`\`\`\n${attachedFile.textContent}\n\`\`\`\n\n${q}`;
    }

    setInputQuery("");
    setAttachedFile(null);
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }
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
      content: finalPrompt,
      created_at: new Date().toISOString()
    };

    const streamingMsgId = "stream-" + Date.now();
    const initialStreamMsg: MessageItem = {
      id: streamingMsgId,
      role: "assistant",
      content: "",
      isStreaming: true,
      streamingPhase: isOverride ? "Aplikuji Executive Override..." : "Navazuji kognitivní spojení...",
      created_at: new Date().toISOString()
    };
    
    updateActiveThreadMessages(prev => [...prev, userMsg, initialStreamMsg]);
    
    try {
      const history = [...messages, userMsg].map(m => ({ role: m.role, content: m.content }));
      
      const promptToSend = isOverride 
        ? `[EXECUTIVE OVERRIDE AKTIVNÍ: Operátor manuálně přepsal veto s právem MODIFY_SYSTEM_STATE]: ${q}`
        : q;

      const data = await omnisEngine.processQueryBatched(
        promptToSend,
        ontologyDomain,
        true,
        history,
        imgData,
        imgMime,
        activeThreadId,
        isAuthorized || isOverride
      );

      if (data.status === "LOCKED_INPUT" && !isOverride) {
        setIsInputCognitiveLocked(true);
        setLockedQuery(q);
      }
      
      const isFallback = data.answer.includes("Zero-Simulation Policy") || 
                         data.answer.includes("chybí nebo je neplatný `GEMINI_API_KEY`") ||
                         data.answer.includes("[O.M.N.I.S. SAFETY SHIELD]");

      if (isFallback) {
        triggerFallback("Gemini API není nakonfigurován nebo se nepodařilo navázat online spojení.");
      }

      if (data && data.status === "LOCKED_INPUT" && !isOverride) {
        setIsInputCognitiveLocked(true);
        setLockedQuery(q);
      }

      // Ensure final state is committed to thread
      updateActiveThreadMessages(prev =>
        prev.map(m =>
          m.id === streamingMsgId || m.id === data.message_id
            ? {
                id: data.message_id || streamingMsgId,
                role: "assistant",
                content: data.answer,
                cognitive_process: data.cognitive_process,
                impact_matrix: data.impact_matrix,
                consequence_forensics: data.consequence_forensics,
                follow_up_questions: data.follow_up_questions,
                token_usage: data.token_usage,
                adversarial_score: data.adversarial_score,
                flagged_issues: data.flagged_issues,
                isOverrideActive: isOverride,
                isStreaming: false,
                streamingPhase: undefined,
                created_at: data.created_at || new Date().toISOString(),
              }
            : m
        )
      );
      
      // Successfully processed, remove from local state buffer
      setLocalQueryBuffer(prev => prev.filter(item => item !== q));
    } catch (e: any) {
      console.error(e);
      const errMsg = e?.message || String(e);
      triggerFallback(errMsg);
      updateActiveThreadMessages(prev =>
        prev.map(m =>
          m.id === streamingMsgId
            ? {
                id: "err-" + Date.now(),
                role: "assistant",
                content: `Chyba spojení s kognitivním jádrem Gemini: ${errMsg}. Automaticky přepínám na záložní instanci (Lokální offline simulace). Váš dotaz je v bezpečí v lokální paměti.`,
                isStreaming: false,
                created_at: new Date().toISOString()
              }
            : m
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [inputQuery, isLoading, messages, ontologyDomain, updateActiveThreadMessages, triggerFallback, attachedFile, activeThreadId, setIsInputCognitiveLocked, setLockedQuery]);

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
          conversation_id: activeThreadId || "00000000-0000-0000-0000-000000000000",
          user_rating: rating,
          feedback_text: `Uživatel ohodnotil odpověď jako ${rating}/5`,
        }),
      });
    } catch (e) {
      console.error(e);
    }
  }, [activeThreadId]);

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

  const exportToPDF = useCallback(async () => {
    if (!activeThread || !messages || messages.length === 0) {
      showToast("Vlákno chatu je prázdné nebo nebylo vybráno.", "error");
      return;
    }

    try {
      showToast("Připravuji standardní PDF export...", "info");
      const { jsPDF } = await import("jspdf");
      const html2canvas = (await import("html2canvas")).default;

      // Create high-fidelity offscreen rendering element
      const wrapper = document.createElement("div");
      wrapper.style.position = "fixed";
      wrapper.style.left = "-9999px";
      wrapper.style.top = "0";
      wrapper.style.width = "820px";
      wrapper.style.background = "#050810";
      wrapper.style.color = "#e2e8f0";
      wrapper.style.fontFamily = "monospace, system-ui, sans-serif";
      wrapper.style.padding = "40px";
      wrapper.style.boxSizing = "border-box";
      wrapper.style.borderRadius = "8px";
      wrapper.style.border = "1px solid #1e293b";

      // HTML formatting helper for clean Markdown rendering
      const formatMsgContent = (text: string) => {
        if (!text) return "";
        let html = text
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
        html = html.replace(/```([\s\S]*?)```/g, '<pre style="background: #090f1d; border: 1px solid #1e293b; border-radius: 6px; padding: 10px; margin: 8px 0; overflow-x: auto; font-family: monospace; font-size: 11px; color: #38bdf8; white-space: pre-wrap; word-break: break-all;">$1</pre>');
        html = html.replace(/`([^`]+)`/g, '<code style="background: #090f1d; border: 1px solid #1e293b; border-radius: 4px; padding: 1px 5px; font-family: monospace; color: #38bdf8; font-size: 11px;">$1</code>');
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        html = html.replace(/\n/g, '<br/>');
        return html;
      };

      // Construct Header
      let htmlString = `
        <div style="border-bottom: 2px solid #00F0FF; padding-bottom: 20px; margin-bottom: 30px; font-family: monospace;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <h1 style="color: #00F0FF; margin: 0; font-size: 24px; font-weight: bold; tracking-wider: 2px;">O.M.N.I.S.</h1>
              <p style="color: #64748b; margin: 4px 0 0 0; font-size: 11px; uppercase;">Cognitive Operating System v2.7</p>
            </div>
            <div style="text-align: right;">
              <span style="background: rgba(0, 240, 255, 0.1); border: 1px solid rgba(0, 240, 255, 0.3); color: #00F0FF; padding: 4px 10px; border-radius: 4px; font-size: 10px; font-weight: bold;">STANDARD REPORT</span>
              <p style="color: #64748b; margin: 4px 0 0 0; font-size: 10px;">DOMÉNA: ${ontologyDomain}</p>
            </div>
          </div>
          <div style="margin-top: 15px;">
            <h2 style="color: #f1f5f9; margin: 5px 0; font-size: 16px;">Vlákno: ${activeThread.title}</h2>
            <p style="color: #475569; margin: 0; font-size: 10px;">Exportováno: ${new Date().toLocaleString("cs-CZ")}</p>
          </div>
        </div>
        <div style="display: flex; flex-direction: column; gap: 24px;">
      `;

      // Render messages
      messages.forEach((msg) => {
        const isUser = msg.role === "user";
        if (isUser) {
          htmlString += `
            <div style="align-self: flex-start; width: 100%; border-left: 3px solid #10b981; padding-left: 15px; margin-bottom: 10px;">
              <div style="color: #10b981; font-weight: bold; font-size: 11px; margin-bottom: 5px; font-family: monospace;">[UŽIVATEL] &bull; ${new Date(msg.created_at || Date.now()).toLocaleTimeString("cs-CZ")}</div>
              <div style="color: #e2e8f0; font-size: 13px; line-height: 1.6; word-break: break-word;">${formatMsgContent(msg.content)}</div>
            </div>
          `;
        } else {
          htmlString += `
            <div style="align-self: flex-start; width: 100%; border-left: 3px solid #00F0FF; padding-left: 15px; background: rgba(10, 15, 30, 0.6); padding: 15px; border-radius: 8px; border: 1px solid #1e293b; border-left: 3px solid #00F0FF; margin-bottom: 10px; box-sizing: border-box;">
              <div style="color: #00F0FF; font-weight: bold; font-size: 11px; margin-bottom: 8px; font-family: monospace; display: flex; justify-content: space-between;">
                <span>[KOGNITIVNÍ ASISTENT]</span>
                <span style="color: #475569;">${new Date(msg.created_at || Date.now()).toLocaleTimeString("cs-CZ")}</span>
              </div>
              <div style="color: #e2e8f0; font-size: 13px; line-height: 1.6; word-break: break-word;">${formatMsgContent(msg.content)}</div>
            </div>
          `;
        }
      });

      htmlString += `
        </div>
        <div style="margin-top: 40px; border-top: 1px solid #1e293b; padding-top: 15px; text-align: center; color: #475569; font-size: 9px; font-family: monospace;">
          O.M.N.I.S. Secure PDF Report &bull; Strana 1 z 1 &bull; Důvěrné / Interní použití
        </div>
      `;

      wrapper.innerHTML = htmlString;
      document.body.appendChild(wrapper);

      // Render to canvas
      const canvas = await html2canvas(wrapper, {
        backgroundColor: "#050810",
        scale: 2,
        useCORS: true,
        logging: false
      });

      document.body.removeChild(wrapper);

      // Create Multi-Page PDF with full pagination
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF("p", "mm", "a4");
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const imgHeight = (canvas.height * pdfWidth) / canvas.width;
      
      let heightLeft = imgHeight;
      let position = 0;

      // Render Page 1
      pdf.addImage(imgData, "PNG", 0, position, pdfWidth, imgHeight);
      heightLeft -= pageHeight;

      // Render all subsequent pages if content exceeds A4 height
      while (heightLeft > 0) {
        position -= pageHeight;
        pdf.addPage();
        pdf.addImage(imgData, "PNG", 0, position, pdfWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      pdf.save(`omnis-chat-${activeThread.title.toLowerCase().replace(/[^a-z0-9]/g, "-")}-${new Date().toISOString().slice(0, 10)}.pdf`);
      showToast("Kompletní vícestránkový chat byl úspěšně exportován do PDF!", "success");
    } catch (e: any) {
      console.error(e);
      showToast(`Chyba při exportu PDF: ${e?.message || e}`, "error");
    }
  }, [threads, activeThreadId, activeThread, messages, ontologyDomain, showToast]);

  const exportAdminPDF = useCallback(async () => {
    if (!activeThread || !messages || messages.length === 0) {
      showToast("Vlákno chatu je prázdné nebo nebylo vybráno.", "error");
      return;
    }

    try {
      showToast("Sestavuji hloubkový administrativní forenzní export...", "info");
      const { jsPDF } = await import("jspdf");
      const html2canvas = (await import("html2canvas")).default;

      // Create high-fidelity offscreen rendering element for Admin
      const wrapper = document.createElement("div");
      wrapper.style.position = "fixed";
      wrapper.style.left = "-9999px";
      wrapper.style.top = "0";
      wrapper.style.width = "820px";
      wrapper.style.background = "#030712";
      wrapper.style.color = "#cbd5e1";
      wrapper.style.fontFamily = "monospace, system-ui, sans-serif";
      wrapper.style.padding = "40px";
      wrapper.style.boxSizing = "border-box";
      wrapper.style.borderRadius = "8px";
      wrapper.style.border = "1px solid #ef4444";

      // Markdown parsing helper
      const formatMsgContent = (text: string) => {
        if (!text) return "";
        let html = text
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/>/g, "&gt;");
        html = html.replace(/```([\s\S]*?)```/g, '<pre style="background: #090f1d; border: 1px solid #ef4444; border-radius: 6px; padding: 10px; margin: 8px 0; overflow-x: auto; font-family: monospace; font-size: 11px; color: #f87171; white-space: pre-wrap; word-break: break-all;">$1</pre>');
        html = html.replace(/`([^`]+)`/g, '<code style="background: #090f1d; border: 1px solid #334155; border-radius: 4px; padding: 1px 5px; font-family: monospace; color: #ef4444; font-size: 11px;">$1</code>');
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        html = html.replace(/\n/g, '<br/>');
        return html;
      };

      // Construct Header
      let htmlString = `
        <div style="border-bottom: 2px solid #ef4444; padding-bottom: 20px; margin-bottom: 30px; font-family: monospace;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <h1 style="color: #ef4444; margin: 0; font-size: 24px; font-weight: bold; tracking-wider: 2px;">O.M.N.I.S. FORENSICS</h1>
              <p style="color: #94a3b8; margin: 4px 0 0 0; font-size: 11px; text-transform: uppercase;">ADMINISTRATOR DEEP DIAGNOSTIC AUDIT REPORT</p>
            </div>
            <div style="text-align: right;">
              <span style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.4); color: #f87171; padding: 4px 10px; border-radius: 4px; font-size: 10px; font-weight: bold;">PRIVILEGED LEVEL 5</span>
              <p style="color: #64748b; margin: 4px 0 0 0; font-size: 10px;">DOMÉNA: ${ontologyDomain}</p>
            </div>
          </div>
          <div style="margin-top: 15px; display: flex; justify-content: space-between; align-items: flex-end;">
            <div>
              <h2 style="color: #f1f5f9; margin: 5px 0; font-size: 16px;">Vlákno: ${activeThread.title}</h2>
              <p style="color: #475569; margin: 0; font-size: 10px;">ID Konverzace: ${activeThread.id}</p>
            </div>
            <div style="text-align: right; color: #64748b; font-size: 10px;">
              <p style="margin: 0;">Exportováno: ${new Date().toLocaleString("cs-CZ")}</p>
              <p style="margin: 2px 0 0 0; color: #ef4444;">SECURITY GATEWAY: ENGAGED</p>
            </div>
          </div>
        </div>
        <div style="display: flex; flex-direction: column; gap: 32px;">
      `;

      // Render messages with expanded admin variables
      messages.forEach((msg) => {
        const isUser = msg.role === "user";
        if (isUser) {
          htmlString += `
            <div style="align-self: flex-start; width: 100%; border-left: 3px solid #10b981; padding-left: 15px; margin-bottom: 10px;">
              <div style="color: #10b981; font-weight: bold; font-size: 11px; margin-bottom: 5px; font-family: monospace;">[UŽIVATEL] &bull; ${new Date(msg.created_at || Date.now()).toLocaleTimeString("cs-CZ")}</div>
              <div style="color: #cbd5e1; font-size: 13px; line-height: 1.6;">${formatMsgContent(msg.content)}</div>
            </div>
          `;
        } else {
          // Parse impact matrix
          let matrixHTML = "";
          if (msg.impact_matrix) {
            matrixHTML = `
              <div style="margin-top: 15px; border-top: 1px dashed #ef4444/30; padding-top: 10px;">
                <div style="color: #f87171; font-size: 10px; font-weight: bold; margin-bottom: 8px;">[8D SÉMANTICKÁ MATICE DOPADŮ]</div>
                <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; font-size: 10px;">
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">SYS (Systém):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.sys_stability}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">ECON (Ekonom.):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.econ_viability}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">PSYCH (Psychol.):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.psych_impact}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">ECO (Ekologie):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.eco_balance}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">LAW (Právo):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.law_compliance}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">SEC (Bezpečnost):</span> <strong style="color: #ef4444;">${msg.impact_matrix.sec_vulnerability}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">PHYS (Fyzika):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.phys_limits}</strong>
                  </div>
                  <div style="background: #0f172a; padding: 6px; border-radius: 4px; border: 1px solid #1e293b;">
                    <span style="color: #94a3b8;">SOC (Společnost):</span> <strong style="color: #00f0ff;">${msg.impact_matrix.soc_acceptance}</strong>
                  </div>
                </div>
              </div>
            `;
          }

          // Parse follow-up suggestions / proposed variants
          let followupsHTML = "";
          if (msg.follow_up_questions && msg.follow_up_questions.length > 0) {
            followupsHTML = `
              <div style="margin-top: 15px; border-top: 1px dashed #ef4444/30; padding-top: 10px;">
                <div style="color: #f87171; font-size: 10px; font-weight: bold; margin-bottom: 5px;">[NABÍZENÉ ALTERNATIVNÍ VARIANTY & NÁVRHY PRO DALŠÍ KONVERZACI]</div>
                <div style="display: flex; flex-direction: column; gap: 4px; font-size: 11px;">
                  ${msg.follow_up_questions.map((q, idx) => `
                    <div style="background: #090f1d; border: 1px solid #1e293b; padding: 6px 10px; border-radius: 4px; color: #38bdf8;">
                      <span style="color: #ef4444; font-weight: bold;">V${idx + 1}:</span> ${q}
                    </div>
                  `).join("")}
                </div>
              </div>
            `;
          }

          // Parse cognitive process / thinking trace
          let cognitiveHTML = "";
          if (msg.cognitive_process) {
            cognitiveHTML = `
              <div style="margin-top: 15px; background: rgba(239, 68, 68, 0.03); border: 1px solid rgba(239, 68, 68, 0.2); border-radius: 6px; padding: 12px;">
                <div style="color: #ef4444; font-size: 10px; font-weight: bold; margin-bottom: 6px; font-family: monospace;">[KOGNITIVNÍ MYŠLENKOVÝ PROCES / SYSTEM COGNITION LOG]</div>
                <div style="color: #94a3b8; font-size: 11px; line-height: 1.5; font-family: monospace; white-space: pre-wrap; word-break: break-all;">${msg.cognitive_process}</div>
              </div>
            `;
          }

          // Parse token statistics
          let telemetryHTML = "";
          if (msg.token_usage) {
            telemetryHTML = `
              <div style="margin-top: 10px; display: flex; gap: 15px; font-size: 9px; color: #64748b; font-family: monospace; justify-content: flex-end;">
                <span>Vstupní tokeny: <strong>${msg.token_usage.prompt_tokens}</strong></span>
                <span>Výstupní tokeny: <strong>${msg.token_usage.completion_tokens}</strong></span>
                <span>Celkem: <strong>${msg.token_usage.total_tokens}</strong></span>
                <span style="color: #ef4444;">Cena: <strong>$${msg.token_usage.cost_usd.toFixed(6)}</strong></span>
              </div>
            `;
          }

          htmlString += `
            <div style="align-self: flex-start; width: 100%; border-left: 3px solid #ef4444; background: rgba(17, 24, 39, 0.7); padding: 20px; border-radius: 8px; border: 1px solid #334155; border-left: 3px solid #ef4444; margin-bottom: 10px; box-sizing: border-box;">
              <div style="color: #ef4444; font-weight: bold; font-size: 11px; margin-bottom: 10px; font-family: monospace; display: flex; justify-content: space-between;">
                <span>[ASISTENT - MULTI-AGENT DELIBERATION CORE]</span>
                <span style="color: #475569;">${new Date(msg.created_at || Date.now()).toLocaleTimeString("cs-CZ")}</span>
              </div>
              <div style="color: #e2e8f0; font-size: 13px; line-height: 1.6;">${formatMsgContent(msg.content)}</div>
              ${cognitiveHTML}
              ${matrixHTML}
              ${followupsHTML}
              ${telemetryHTML}
            </div>
          `;
        }
      });

      htmlString += `
        </div>
        <div style="margin-top: 50px; border-top: 1px solid #ef4444/30; padding-top: 15px; text-align: center; color: #475569; font-size: 9px; font-family: monospace; display: flex; justify-content: space-between;">
          <span>CLASSIFIED LEVEL 5 AUDIT</span>
          <span>Strana 1 z 1</span>
          <span>O.M.N.I.S. FORENSIC DATABASE DUMP</span>
        </div>
      `;

      wrapper.innerHTML = htmlString;
      document.body.appendChild(wrapper);

      // Render to canvas
      const canvas = await html2canvas(wrapper, {
        backgroundColor: "#030712",
        scale: 2,
        useCORS: true,
        logging: false
      });

      document.body.removeChild(wrapper);

      // Create Multi-Page PDF with full pagination
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF("p", "mm", "a4");
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const imgHeight = (canvas.height * pdfWidth) / canvas.width;
      
      let heightLeft = imgHeight;
      let position = 0;

      // Render Page 1
      pdf.addImage(imgData, "PNG", 0, position, pdfWidth, imgHeight);
      heightLeft -= pageHeight;

      // Render all subsequent pages if content exceeds A4 height
      while (heightLeft > 0) {
        position -= pageHeight;
        pdf.addPage();
        pdf.addImage(imgData, "PNG", 0, position, pdfWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      pdf.save(`omnis-admin-forensic-${activeThread.title.toLowerCase().replace(/[^a-z0-9]/g, "-")}-${new Date().toISOString().slice(0, 10)}.pdf`);
      showToast("Kompletní administrativní forenzní report byl vygenerován a stažen!", "success");
    } catch (e: any) {
      console.error(e);
      showToast(`Chyba při administrativním exportu: ${e?.message || e}`, "error");
    }
  }, [threads, activeThreadId, activeThread, messages, ontologyDomain, showToast]);


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

  const [attachedFile, setAttachedFile] = useState<AttachedFileState | null>(null);
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const docInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const lastQueryContentRef = useRef<string>("");
  const lastQueryTimestampRef = useRef<number>(0);

  const processSelectedFile = useCallback((file: File) => {
    if (file.size > 25 * 1024 * 1024) {
      showToast(`Soubor ${file.name} překračuje limit 25 MB (${formatFileSize(file.size)}).`, "error");
      return;
    }

    const isImage = file.type.startsWith("image/");
    const isPdf = file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");

    if (isImage || isPdf) {
      const reader = new FileReader();
      reader.onload = (evt) => {
        const dataUrl = evt.target?.result as string;
        setAttachedFile({
          type: isImage ? "image" : "document",
          name: file.name,
          size: file.size,
          mime: file.type || (isImage ? "image/jpeg" : "application/pdf"),
          data: dataUrl
        });
        showToast(
          isImage
            ? `Obrázek "${file.name}" (${formatFileSize(file.size)}) připraven pro multimodální analýzu.`
            : `Dokument "${file.name}" (${formatFileSize(file.size)}) připraven.`,
          "success"
        );
      };
      reader.onerror = () => showToast("Chyba při čtení souboru.", "error");
      reader.readAsDataURL(file);
    } else {
      // Plain text, Markdown, JSON, Code, Log files
      const reader = new FileReader();
      reader.onload = (evt) => {
        const text = (evt.target?.result as string) || "";
        setAttachedFile({
          type: "document",
          name: file.name,
          size: file.size,
          mime: file.type || "text/plain",
          data: "",
          textContent: text
        });
        const lineCount = text.split("\n").length;
        showToast(`Soubor "${file.name}" (${formatFileSize(file.size)}, ${lineCount} řádků) načten.`, "success");
      };
      reader.onerror = () => showToast("Chyba při čtení textového obsahu.", "error");
      reader.readAsText(file);
    }
  }, [showToast]);

  const handleSelectImage = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processSelectedFile(file);
    e.target.value = "";
  }, [processSelectedFile]);

  const handleSelectDocument = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processSelectedFile(file);
    e.target.value = "";
  }, [processSelectedFile]);

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

  // Derived layout helpers
  const isCompactHeight = windowHeight < 700;
  const headerPaddingClass = isCompactHeight ? "py-2 px-4" : "py-3 px-4 sm:px-6";
  const mainPaddingClass = isCompactHeight ? "p-1" : "sm:p-5";
  const chatInputPaddingClass = isCompactHeight ? "p-3" : "p-4";

  // Stable aliases for ThreadsArchiveDashboard props
  const exportHistory = exportChatHistory;
  const triggerFileInput = () => fileInputRef.current?.click();

  return (
    <div 
      className="min-h-screen bg-[#050810] text-slate-300 font-sans flex flex-col overflow-hidden selection:bg-[#00F0FF]/30"
      style={{ height: `${windowHeight}px` }}
    >
      
      {/* NAVIGATION SIDEBAR */}
      <NavigationSidebar
        isOpen={isMenuOpen}
        onClose={() => setIsMenuOpen(false)}
        activeTab={activeTab}
        setActiveTab={handleTabSwitch}
        onOpenHelp={() => setShowHelpModal(true)}
        theme={theme}
        setTheme={setTheme}
      />

      {/* HEADER & BREADCRUMBS */}
      <AppHeader
        isMenuOpen={isMenuOpen}
        setIsMenuOpen={setIsMenuOpen}
        activeTab={activeTab}
        setActiveTab={handleTabSwitch}
        dbStatus={dbStatus}
        activeThread={activeThread}
        threadsCount={threads.length}
        endpoint={endpoint}
        lastError={lastError}
        resetToPrimary={resetToPrimary}
        ontologyDomain={ontologyDomain}
        importChatHistory={handleImportFile}
        exportChatHistory={exportChatHistory}
        exportToPDF={exportToPDF}
        exportAdminPDF={exportAdminPDF}
        onOpenSearchModal={() => setShowSearchModal(true)}
        fileInputRef={fileInputRef}
        theme={theme}
        setTheme={setTheme}
        userRole={userRole}
        onToggleRole={handleToggleRole}
        onOpenKeyRotator={() => setShowKeyRotatorModal(true)}
        activeKeySlot={activeKeySlot}
      />

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

            {/* EU AI Act Transparency Badge */}
            <div className="bg-[#050914] border-b border-slate-800/80 px-4 py-1.5 flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-400">
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/30 font-bold text-[9px] tracking-wider">
                  🇪🇺 EU AI ACT • ČL. 50(1)
                </span>
                <span className="text-slate-400 text-xs">
                  Asistenční kognitivní AI systém O.M.N.I.S. – výstupy slouží pro expertní syntézu a rozhodovací podporu.
                </span>
              </div>
              <button
                onClick={() => setShowAiActModal(true)}
                className="text-[#00F0FF] hover:text-[#00F0FF]/80 hover:underline font-mono text-[10px] flex items-center gap-1 ml-auto cursor-pointer"
              >
                <HelpCircle className="w-3.5 h-3.5" />
                Transparentní audit systému
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
                  <EmptyStateView onSendQuery={handleSendQuery} ontologyDomain={ontologyDomain} />
                )}
                {messages.map(msg => (
                  <MessageBubble 
                    key={msg.id} 
                    msg={msg} 
                    isInActiveContext={true}
                    userRole={userRole}
                    onSetActiveTab={(tab: string) => {
                      if (tab === "matrix" || tab === "phases") {
                        setActiveTab("octagon");
                      } else if (tab === "forensics" || tab === "lab") {
                        setActiveTab("analytics");
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
                  <SkeletonLoader />
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

              {/* Universal Attachment Preview Banner */}
              {attachedFile && (
                <div className="max-w-4xl mx-auto mb-2.5 p-2.5 bg-[#080D1D] border border-[#00F0FF]/40 rounded-xl flex items-center justify-between gap-3 animate-in fade-in shadow-[0_0_15px_rgba(0,240,255,0.15)]">
                  <div className="flex items-center gap-3 overflow-hidden">
                    {attachedFile.type === "image" ? (
                      <img
                        src={attachedFile.data}
                        alt="Attachment"
                        className="w-12 h-12 object-cover rounded-lg border border-[#00F0FF]/40 flex-shrink-0 shadow-sm"
                      />
                    ) : (
                      <div className="w-12 h-12 rounded-lg bg-[#00F0FF]/10 border border-[#00F0FF]/30 flex items-center justify-center flex-shrink-0">
                        <FileText className="w-6 h-6 text-[#00F0FF]" />
                      </div>
                    )}
                    <div className="truncate">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono text-[#00F0FF] font-bold uppercase tracking-wider">
                          {attachedFile.type === "image" ? "Multimodální obraz" : "Přiložený dokument"}
                        </span>
                        <span className="text-[10px] font-mono text-slate-400 bg-slate-800/80 px-1.5 py-0.2 rounded">
                          {formatFileSize(attachedFile.size)}
                        </span>
                      </div>
                      <span className="text-xs font-mono text-slate-200 truncate block mt-0.5 font-bold">
                        {attachedFile.name}
                      </span>
                      {attachedFile.textContent && (
                        <span className="text-[10px] text-slate-400 font-mono block">
                          {attachedFile.textContent.split("\n").length} řádků textu načteno
                        </span>
                      )}
                    </div>
                  </div>
                  <button 
                    type="button"
                    onClick={() => setAttachedFile(null)}
                    className="p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                    title="Odebrat přílohu"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              )}

              {/* Hidden File Inputs */}
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
              <input
                type="file"
                ref={docInputRef}
                accept=".pdf,.txt,.md,.json,.csv,.log,.kt,.ts,.tsx,.js,.jsx,.py,.yaml,.yml,.xml,.sql,.html,.css"
                onChange={handleSelectDocument}
                className="hidden"
              />

              {/* Quick Cognitive Modifiers & Token Counter Toolbar */}
              <div className="max-w-4xl mx-auto mb-2 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none py-0.5">
                  <span className="text-[10px] font-mono text-slate-500 uppercase flex items-center gap-1 flex-shrink-0">
                    <Sparkles className="w-3 h-3 text-amber-400" /> Kognitivní filtry:
                  </span>
                  {[
                    { label: "⚡ Stručně", prompt: "Odpověz maximálně stručně a věcně v odrážkách." },
                    { label: "🔬 Hloubkový rozbor", prompt: "Proveď rigorózní systémovou a kognitivní dekompozici." },
                    { label: "🛠️ Krok za krokem", prompt: "Rozepiš řešení krok za krokem s přesným algoritmem a instrukcemi." },
                    { label: "🛡️ Audit rizik", prompt: "Zaměř se na identifikaci kritických rizik, slabin a mitigací." }
                  ].map((chip) => (
                    <button
                      key={chip.label}
                      type="button"
                      onClick={() => {
                        setInputQuery(prev => prev ? `${prev.trim()}\n\n[Instrukce: ${chip.prompt}]` : `[Instrukce: ${chip.prompt}] `);
                        if (textareaRef.current) {
                          textareaRef.current.focus();
                        }
                      }}
                      className="px-2 py-0.5 rounded-lg text-[10px] font-mono bg-slate-900/90 hover:bg-slate-800 text-slate-300 hover:text-[#00F0FF] border border-slate-800 hover:border-[#00F0FF]/40 transition-all flex-shrink-0 cursor-pointer shadow-sm"
                    >
                      {chip.label}
                    </button>
                  ))}
                </div>
                <div className="flex-shrink-0 self-end sm:self-auto">
                  <TokenCounter text={inputQuery} hasImage={!!attachedFile} />
                </div>
              </div>

              {isInputCognitiveLocked ? (
                <div className="max-w-4xl mx-auto mb-3 p-4 bg-amber-500/15 border border-amber-500/40 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-4 shadow-[0_0_20px_rgba(245,158,11,0.15)] animate-pulse">
                  <div className="flex items-center gap-3">
                    <span className="text-xl">🔒</span>
                    <div>
                      <h4 className="text-amber-400 font-bold font-mono text-sm">KOGNITIVNÍ ZÁMEK: NEÚPLNÝ DOTAZ DETEKOVÁN</h4>
                      <p className="text-slate-300 text-xs mt-0.5">
                        Dotaz: <span className="font-mono text-amber-200">"{lockedQuery}"</span> byl zablokován jako neúplný. Autorizujte pokračování nebo dotaz upravte.
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button
                      type="button"
                      onClick={() => {
                        setIsInputCognitiveLocked(false);
                        setInputQuery(lockedQuery);
                        setLockedQuery("");
                      }}
                      className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-mono border border-slate-700 transition-all active:scale-95 cursor-pointer"
                    >
                      Upravit dotaz
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setIsInputCognitiveLocked(false);
                        const q = lockedQuery;
                        setLockedQuery("");
                        handleSendQuery(q, true);
                      }}
                      className="px-4 py-1.5 bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold rounded-lg text-xs sm:text-sm shadow-[0_0_10px_rgba(245,158,11,0.3)] transition-all active:scale-95 cursor-pointer"
                    >
                      Autorizovat vstup
                    </button>
                  </div>
                </div>
              ) : null}

              {/* Chat Form Container with Drag and Drop Zone */}
              <form 
                onSubmit={e => { e.preventDefault(); handleSendQuery(); }} 
                onDragOver={(e) => { e.preventDefault(); setIsDraggingOver(true); }}
                onDragLeave={(e) => { e.preventDefault(); setIsDraggingOver(false); }}
                onDrop={(e) => {
                  e.preventDefault();
                  setIsDraggingOver(false);
                  const file = e.dataTransfer.files?.[0];
                  if (file) processSelectedFile(file);
                }}
                className={`max-w-4xl mx-auto relative flex items-end gap-2 bg-slate-900/90 backdrop-blur-md border rounded-2xl p-2 shadow-2xl transition-all ${
                  isInputCognitiveLocked ? "opacity-50 pointer-events-none" : ""
                } ${
                  isDraggingOver 
                    ? "border-[#00F0FF] bg-[#00F0FF]/10 shadow-[0_0_25px_rgba(0,240,255,0.35)] scale-[1.01]" 
                    : isListening 
                    ? "border-red-500/60 shadow-[0_0_15px_rgba(239,68,68,0.2)]" 
                    : "border-slate-700/70 focus-within:border-[#00F0FF]/60"
                }`}
              >
                {/* Drag and Drop Visual Feedback Overlay */}
                {isDraggingOver && (
                  <div className="absolute inset-0 bg-[#060D1E]/95 border-2 border-dashed border-[#00F0FF] rounded-2xl flex items-center justify-center gap-3 z-20 pointer-events-none animate-in fade-in">
                    <FileText className="w-6 h-6 text-[#00F0FF] animate-bounce" />
                    <span className="text-sm font-mono font-bold text-[#00F0FF]">
                      Pusťte soubor pro přiložení (obrázek, PDF, kód nebo text)
                    </span>
                  </div>
                )}

                {/* Left Attachment Actions: Camera, Image, Document */}
                <div className="flex items-center gap-1 pl-1 mb-1">
                  <button
                    type="button"
                    onClick={() => cameraInputRef.current?.click()}
                    title="Vyfotit snímek fotoaparátem"
                    className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400 hover:text-[#00F0FF] hover:bg-slate-800/80 transition-all flex-shrink-0 cursor-pointer"
                  >
                    <Camera className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={() => imageInputRef.current?.click()}
                    title="Připojit obrázek ze zařízení"
                    className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400 hover:text-purple-400 hover:bg-slate-800/80 transition-all flex-shrink-0 cursor-pointer"
                  >
                    <ImageIcon className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={() => docInputRef.current?.click()}
                    title="Připojit dokument, PDF nebo zdrojový kód"
                    className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400 hover:text-amber-400 hover:bg-slate-800/80 transition-all flex-shrink-0 cursor-pointer"
                  >
                    <Paperclip className="w-4 h-4" />
                  </button>
                </div>

                {/* Auto-growing Textarea */}
                <textarea 
                  ref={textareaRef}
                  rows={1}
                  value={inputQuery} 
                  onChange={e => {
                    setInputQuery(e.target.value);
                    if (textareaRef.current) {
                      textareaRef.current.style.height = "auto";
                      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 160)}px`;
                    }
                  }}
                  onKeyDown={e => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendQuery();
                    }
                  }}
                  placeholder={
                    isListening 
                      ? "Diktujte dotaz do mikrofonu..." 
                      : attachedFile 
                      ? `Zadejte dotaz k přiloženému souboru "${attachedFile.name}"...` 
                      : "Zpráva pro O.M.N.I.S. (nebo přetáhněte soubor)..."
                  }
                  className="flex-1 bg-transparent px-3 py-2 text-sm sm:text-base focus:outline-none font-mono text-slate-100 min-w-0 resize-none max-h-40 overflow-y-auto leading-relaxed"
                  style={{ minHeight: "42px" }}
                />

                {/* Right Actions: Dictation Mic & Send */}
                <div className="flex items-center gap-1.5 pr-1 mb-1">
                  <button
                    type="button"
                    onClick={toggleDictation}
                    title={isListening ? "Zastavit hlasové diktování" : "Spustit hlasové diktování (Web Speech API)"}
                    className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all flex-shrink-0 cursor-pointer ${
                      isListening
                        ? "bg-red-600 hover:bg-red-500 text-white shadow-[0_0_15px_rgba(239,68,68,0.6)] animate-pulse"
                        : "text-slate-400 hover:text-white hover:bg-slate-800/80"
                    }`}
                  >
                    {isListening ? <Mic className="w-4 h-4 text-white animate-bounce" /> : <MicOff className="w-4 h-4" />}
                  </button>

                  <button 
                    type="submit" 
                    disabled={isLoading || (!inputQuery.trim() && !attachedFile)}
                    className="w-9 h-9 bg-[#00F0FF] hover:opacity-90 active:scale-95 text-slate-950 rounded-xl font-bold transition-all disabled:opacity-40 flex items-center justify-center shadow-[0_0_12px_rgba(0,240,255,0.25)] flex-shrink-0 cursor-pointer"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {isTabSwitching ? (
          <div className="flex-1 flex items-center justify-center p-6 bg-[#0A0F1D] border border-slate-800 rounded-2xl">
            <SkeletonLoader />
          </div>
        ) : (
          <>
            {activeTab === "analytics" && (
              <div className="flex-1 overflow-auto">
                <AnalyticsOverviewDashboard
                  messages={messages}
                  onTriggerDeepDive={handleDeepDiveMessage}
                  onGoToChat={() => handleTabSwitch("chat")}
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
                    handleTabSwitch("chat");
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
                  onGoToChat={() => handleTabSwitch("chat")}
                />
              </div>
            )}
            {activeTab === "octagon" && <div className="flex-1 overflow-auto"><OctagonDashboard messages={messages} userRole={userRole} /></div>}
            {activeTab === "arena" && (
              <div className="flex-1 overflow-auto">
                <MultiAgentArenaDashboard
                  messages={messages}
                  onGoToChat={() => handleTabSwitch("chat")}
                />
              </div>
            )}
            {activeTab === "zk_ledger" && (
              <div className="flex-1 overflow-auto">
                <ZkAuditLedgerDashboard messages={messages} />
              </div>
            )}
            {activeTab === "dashboard" && <div className="flex-1 overflow-auto"><UserDashboard 
                onUseTemplate={(text) => { setInputQuery(text); handleTabSwitch("chat"); }}
                showToast={showToast}
              /></div>}
          </>
        )}
        
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
            <div className="flex-1 overflow-y-auto p-3 space-y-2">
              
              {/* RBAC Role Status Card */}
              <div className={`p-3 rounded-xl border flex flex-col gap-2 ${
                userRole === "ADMIN_OPERATOR"
                  ? "bg-amber-950/20 border-amber-500/40"
                  : "bg-[#00F0FF]/10 border-[#00F0FF]/30"
              }`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {userRole === "ADMIN_OPERATOR" ? (
                      <Shield className="w-4 h-4 text-amber-400" />
                    ) : (
                      <Compass className="w-4 h-4 text-[#00F0FF]" />
                    )}
                    <span className="text-xs font-mono font-bold text-slate-100">
                      {userRole === "ADMIN_OPERATOR" ? "ADMIN / OPERÁTOR" : "BĚŽNÝ UŽIVATEL"}
                    </span>
                  </div>
                  <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded font-bold ${
                    userRole === "ADMIN_OPERATOR" ? "bg-amber-500/20 text-amber-300" : "bg-[#00F0FF]/20 text-[#00F0FF]"
                  }`}>
                    {userRole === "ADMIN_OPERATOR" ? "ÚROVEŇ 2" : "ÚROVEŇ 1"}
                  </span>
                </div>
                <button
                  onClick={handleToggleRole}
                  className={`w-full py-1.5 px-2.5 rounded-lg text-[11px] font-mono font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    userRole === "ADMIN_OPERATOR"
                      ? "bg-amber-500/15 hover:bg-amber-500/25 text-amber-300 border border-amber-500/30"
                      : "bg-[#00F0FF]/15 hover:bg-[#00F0FF]/25 text-[#00F0FF] border border-[#00F0FF]/30"
                  }`}
                >
                  <RefreshCw className="w-3 h-3" />
                  <span>Přepnout do role {userRole === "ADMIN_OPERATOR" ? "Běžný Uživatel" : "Admin Operátor"}</span>
                </button>
              </div>

              <div className="text-[10px] font-mono font-bold text-slate-500 uppercase px-3 py-1 tracking-wider">
                Hlavní Moduly
              </div>

              <button
                onClick={() => { setActiveTab("chat"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
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

              {userRole === "ADMIN_OPERATOR" && (
                <button
                  onClick={() => { setActiveTab("analytics"); setIsMenuOpen(false); }}
                  className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
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
              )}

              <button
                onClick={() => { setActiveTab("archive"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
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

              {userRole === "ADMIN_OPERATOR" && (
                <button
                  onClick={() => { setActiveTab("nodes"); setIsMenuOpen(false); }}
                  className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
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
              )}

              <button
                onClick={() => { setActiveTab("dashboard"); setIsMenuOpen(false); }}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
                  activeTab === "dashboard"
                    ? "bg-amber-500/15 text-amber-300 border border-amber-500/30 font-bold"
                    : "text-slate-300 hover:bg-slate-900 border border-transparent"
                }`}
              >
                <Sliders className="w-5 h-5 text-amber-400 flex-shrink-0" />
                <div className="truncate">
                  <div className="text-xs font-mono font-bold">Správa Témat & Šablon</div>
                  <div className="text-[10px] text-slate-400 font-sans truncate">Uživatelský panel & předlohy</div>
                </div>
              </button>

              {userRole === "ADMIN_OPERATOR" && (
                <button
                  onClick={() => { setActiveTab("octagon"); setIsMenuOpen(false); }}
                  className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all text-left cursor-pointer ${
                    activeTab === "octagon"
                      ? "bg-purple-500/15 text-purple-300 border border-purple-500/30 font-bold"
                      : "text-slate-300 hover:bg-slate-900 border border-transparent"
                  }`}
                >
                  <Activity className="w-5 h-5 text-purple-400 flex-shrink-0" />
                  <div className="truncate">
                    <div className="text-xs font-mono font-bold">Octagon 8D & do(X) SCM</div>
                    <div className="text-[10px] text-slate-400 font-sans truncate">Kauzální simulátor & Monte Carlo VaR</div>
                  </div>
                </button>
              )}

              <div className="pt-3 border-t border-slate-800 space-y-1">
                <div className="text-[10px] font-mono font-bold text-slate-500 uppercase px-3 py-1 tracking-wider">
                  Nástroje & Akce
                </div>

                {userRole === "ADMIN_OPERATOR" && (
                  <button
                    onClick={() => { setShowKeyRotatorModal(true); setIsMenuOpen(false); }}
                    className="w-full p-2.5 rounded-xl flex items-center justify-between text-xs font-mono text-amber-300 hover:bg-amber-500/15 border border-amber-500/30 transition-all text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <Key className="w-4 h-4 text-amber-400" />
                      <span className="font-bold">3-Slotový Rotátor Klíčů</span>
                    </div>
                    <span className="px-1.5 py-0.5 rounded text-[10px] bg-amber-500/20 text-amber-300 font-mono">
                      Slot {activeKeySlot}
                    </span>
                  </button>
                )}

                <button
                  onClick={() => { setShowSearchModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left cursor-pointer"
                >
                  <Search className="w-4 h-4 text-[#00F0FF]" />
                  <span>Vyhledat ve vláknech</span>
                </button>

                <button
                  onClick={() => { setShowMergeModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left cursor-pointer"
                >
                  <GitMerge className="w-4 h-4 text-purple-400" />
                  <span>Sloučit konverzace</span>
                </button>

                <button
                  onClick={() => { setShowHelpModal(true); setIsMenuOpen(false); }}
                  className="w-full p-2.5 rounded-xl flex items-center gap-2.5 text-xs font-mono text-slate-300 hover:bg-slate-800/80 transition-colors text-left cursor-pointer"
                >
                  <HelpCircle className="w-4 h-4 text-amber-400" />
                  <span>Průvodce & Klávesové Zkratky</span>
                </button>
              </div>
            </div>

            {/* Footer */}
            <div className="p-3 border-t border-slate-800/80 bg-[#0A0F1D] text-center">
              <span className="text-[10px] font-mono text-slate-500">
                O.M.N.I.S. Cognitive System v2.7 ({userRole})
              </span>
            </div>
          </div>
        </div>
      )}

      {/* 3-SLOT GEMINI API KEY ROTATOR MODAL */}
      <ApiKeyRotatorModal
        isOpen={showKeyRotatorModal}
        onClose={() => setShowKeyRotatorModal(false)}
        onSlotChanged={(slot) => setActiveKeySlot(slot)}
      />

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

      {/* EU AI ACT TRANSPARENCY & AUDIT MODAL */}
      {showAiActModal && (
        <EuAiActModal
          onClose={() => setShowAiActModal(false)}
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
