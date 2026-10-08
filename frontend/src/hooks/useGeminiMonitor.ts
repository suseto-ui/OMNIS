import { useState, useEffect, useCallback } from "react";
import { omnisEngine } from "../omnisEngine";
import { ToastType } from "../types";

export interface KeyPoolEntry {
  name: string;
  preview: string;
  status: string;
  is_active: boolean;
  error?: string;
}

export function useGeminiMonitor(showToast: (msg: string, type?: ToastType) => void) {
  const [endpoint, setEndpoint] = useState<string>(omnisEngine.apiEndpoint);
  const [lastError, setLastError] = useState<string | null>(null);
  const [keyPool, setKeyPool] = useState<KeyPoolEntry[]>([]);
  const [activeKey, setActiveKey] = useState<string | null>(null);
  const [isCheckingPool, setIsCheckingPool] = useState<boolean>(false);

  const requestNotificationPermission = useCallback(() => {
    if (typeof window !== "undefined" && "Notification" in window) {
      if (Notification.permission === "default") {
        Notification.requestPermission();
      }
    }
  }, []);

  useEffect(() => {
    requestNotificationPermission();
  }, [requestNotificationPermission]);

  const checkKeyPool = useCallback(async (silent: boolean = true) => {
    setIsCheckingPool(true);
    try {
      const res = await fetch("/api/system/gemini-check");
      if (res.ok) {
        const data = await res.json();
        if (data.key_pool && Array.isArray(data.key_pool)) {
          setKeyPool(data.key_pool);
        }
        if (data.active_key) {
          setActiveKey(data.active_key);
        }
        if (!silent) {
          if (data.status === "ACTIVE_HEALTHY") {
            showToast(`🟢 Klíč ${data.active_key} je aktivní a v pořádku.`, "success");
          } else if (data.status === "DEPLETED_PREPAYMENT_CREDITS") {
            showToast(`⚠️ ${data.active_key} vyčerpal kredit. Aktivován automatický failover.`, "warning");
          } else {
            showToast(`ℹ️ Stav klíče: ${data.status}`, "info");
          }
        }
      }
    } catch (e) {
      console.warn("[useGeminiMonitor] Nelze ověřit stav key-pool:", e);
    } finally {
      setIsCheckingPool(false);
    }
  }, [showToast]);

  useEffect(() => {
    checkKeyPool(true);
    // Periodické ověření každé 2 minuty
    const interval = setInterval(() => checkKeyPool(true), 120000);
    return () => clearInterval(interval);
  }, [checkKeyPool]);

  const triggerFallback = useCallback((errorDescription: string) => {
    const backupInstance = "/api/process";
    if (omnisEngine.apiEndpoint !== backupInstance) {
      omnisEngine.apiEndpoint = backupInstance;
      setEndpoint(backupInstance);
      setLastError(errorDescription);

      showToast(`⚠️ Detekována chyba Gemini API. Automaticky přepínám na záložní instanci (Lokální offline syntéza).`, "error");

      if (typeof window !== "undefined" && "Notification" in window) {
        if (Notification.permission === "granted") {
          try {
            new Notification("O.M.N.I.S. Kognitivní Přesměrování", {
              body: `Detekována chyba Gemini API. Automaticky přepínám na záložní instanci (Lokální offline syntéza). Popis: ${errorDescription}`,
              tag: "omnis-api-fallback",
              requireInteraction: true,
            });
          } catch (e) {
            console.error("System Notification failed", e);
          }
        }
      }
    }
  }, [showToast]);

  const resetToPrimary = useCallback(() => {
    const primaryInstance = "/api/query";
    omnisEngine.apiEndpoint = primaryInstance;
    setEndpoint(primaryInstance);
    setLastError(null);
    showToast("Přepnuto zpět na primární instanci (Online Gemini API).", "success");
    checkKeyPool(false);
  }, [showToast, checkKeyPool]);

  return {
    endpoint,
    lastError,
    triggerFallback,
    resetToPrimary,
    keyPool,
    activeKey,
    isCheckingPool,
    checkKeyPool
  };
}
