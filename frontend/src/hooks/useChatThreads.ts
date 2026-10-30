import { useState, useEffect, useMemo, useCallback } from "react";
import { ChatThread, MessageItem } from "../types";
import { saveThreadsToIndexedDB, loadThreadsFromIndexedDB } from "../indexedDbStorage";

export function useChatThreads() {
  const [threads, setThreads] = useState<ChatThread[]>(() => {
    try {
      const saved = localStorage.getItem("omnis_chat_threads");
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch (e) {
      console.error("Failed to parse threads from localStorage", e);
    }
    return [
      {
        id: "thread-default",
        title: "Vlákno #1 - Kognitivní analýza",
        messages: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
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
  const [lastIndexedDbSave, setLastIndexedDbSave] = useState<string | null>(null);

  // Fallback load from IndexedDB or Backend API on initial mount
  useEffect(() => {
    const initializeThreadsState = async () => {
      try {
        const savedLocalStorage = localStorage.getItem("omnis_chat_threads");
        if (!savedLocalStorage) {
          const idbThreads = await loadThreadsFromIndexedDB();
          if (idbThreads && idbThreads.length > 0) {
            setThreads(idbThreads);
            console.log("[IndexedDB Recovery] Restored threads state from IndexedDB backup.");
            return;
          }
        }

        // Attempt synchronization with backend API
        const res = await fetch("/api/threads");
        if (res.ok) {
          const remoteThreads = await res.json();
          if (Array.isArray(remoteThreads) && remoteThreads.length > 0) {
            setThreads((prev) => {
              if (prev.length <= 1 && prev[0]?.id === "thread-default" && prev[0]?.messages.length === 0) {
                return remoteThreads.map((rt: any) => ({
                  id: String(rt.id),
                  title: rt.title || "Vlákno O.M.N.I.S.",
                  messages: (rt.messages || []).map((m: any) => ({
                    id: String(m.id),
                    role: m.role as "user" | "assistant",
                    content: m.content,
                    cognitive_process: m.cognitive_thoughts,
                    follow_up_questions: m.follow_up_questions,
                    created_at: m.created_at || new Date().toISOString(),
                  })),
                  createdAt: rt.created_at || new Date().toISOString(),
                  updatedAt: rt.updated_at || new Date().toISOString(),
                }));
              }
              return prev;
            });
          }
        }
      } catch (e) {
        console.warn("[Thread Sync] Backend sync skipped or unavailable; offline storage active:", e);
      }
    };
    initializeThreadsState();
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
          const timeStr = new Date().toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
          });
          setLastIndexedDbSave(timeStr);
        }
      }
    };

    const initialTimer = setTimeout(autoSaveToIndexedDb, 4000);
    const interval = setInterval(autoSaveToIndexedDb, 60000);

    return () => {
      clearTimeout(initialTimer);
      clearInterval(interval);
    };
  }, [threads]);

  // Derived current active thread & messages
  const activeThread = useMemo(() => {
    return (
      threads.find((t) => t.id === activeThreadId) ||
      threads[0] || {
        id: "thread-default",
        title: "Vlákno #1",
        messages: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    );
  }, [threads, activeThreadId]);

  const messages = activeThread.messages;

  // Helper to update active thread's messages
  const updateActiveThreadMessages = useCallback(
    (updater: (prevMessages: MessageItem[]) => MessageItem[]) => {
      setThreads((prevThreads) => {
        return prevThreads.map((thread) => {
          if (thread.id === activeThreadId) {
            const updatedMsgs = updater(thread.messages);
            let title = thread.title;
            if ((title.startsWith("Vlákno #") || title === "Nové vlákno") && updatedMsgs.length > 0) {
              const firstUserMsg = updatedMsgs.find((m) => m.role === "user");
              if (firstUserMsg) {
                title =
                  firstUserMsg.content.length > 28
                    ? firstUserMsg.content.substring(0, 28) + "..."
                    : firstUserMsg.content;
              }
            }
            return {
              ...thread,
              title,
              messages: updatedMsgs,
              updatedAt: new Date().toISOString(),
            };
          }
          return thread;
        });
      });
    },
    [activeThreadId]
  );

  const createNewThread = useCallback(() => {
    const newId = `thread-${Date.now()}`;
    const newTitle = `Vlákno #${threads.length + 1}`;
    const newThread: ChatThread = {
      id: newId,
      title: newTitle,
      messages: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setThreads((prev) => [newThread, ...prev]);
    setActiveThreadId(newId);

    // Asynchronous backend persistence
    fetch("/api/threads", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: newTitle }),
    }).catch((e) => console.warn("[Thread Sync] Failed to persist thread to backend:", e));

    return newId;
  }, [threads.length]);

  const deleteThread = useCallback(
    (id: string) => {
      setThreads((prev) => {
        const filtered = prev.filter((t) => t.id !== id);
        if (filtered.length === 0) {
          const defaultThread: ChatThread = {
            id: "thread-default",
            title: "Vlákno #1",
            messages: [],
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          };
          setActiveThreadId(defaultThread.id);
          return [defaultThread];
        }
        if (id === activeThreadId) {
          setActiveThreadId(filtered[0].id);
        }
        return filtered;
      });

      // Asynchronous backend deletion
      fetch(`/api/threads/${id}`, {
        method: "DELETE",
      }).catch((e) => console.warn("[Thread Sync] Failed to delete thread from backend:", e));
    },
    [activeThreadId]
  );

  const startRenameThread = useCallback((id: string, currentTitle: string) => {
    setEditingThreadId(id);
    setEditingTitleText(currentTitle);
  }, []);

  const saveRenameThread = useCallback((id: string) => {
    const trimmed = editingTitleText.trim();
    if (!trimmed) {
      setEditingThreadId(null);
      return;
    }
    setThreads((prev) =>
      prev.map((t) => (t.id === id ? { ...t, title: trimmed, updatedAt: new Date().toISOString() } : t))
    );
    setEditingThreadId(null);

    // Asynchronous backend update
    fetch(`/api/threads/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title: trimmed }),
    }).catch((e) => console.warn("[Thread Sync] Failed to update thread title on backend:", e));
  }, [editingTitleText]);

  const mergeThreads = useCallback((sourceId: string, targetId: string) => {
    setThreads((prev) => {
      const source = prev.find((t) => t.id === sourceId);
      const target = prev.find((t) => t.id === targetId);
      if (!source || !target) return prev;

      const mergedMessages = [...target.messages, ...source.messages].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );

      const updatedTarget = {
        ...target,
        messages: mergedMessages,
        updatedAt: new Date().toISOString(),
      };

      return prev.filter((t) => t.id !== sourceId).map((t) => (t.id === targetId ? updatedTarget : t));
    });
  }, []);

  return {
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
    createNewThread,
    deleteThread,
    startRenameThread,
    saveRenameThread,
    mergeThreads,
  };
}
