/**
 * IndexedDB Auto-Save Engine for O.M.N.I.S.
 * Provides offline redundant storage for conversation threads alongside localStorage.
 */

import { ChatThread } from "./App";

const DB_NAME = "OmnisStorageDB";
const DB_VERSION = 1;
const STORE_NAME = "threads_backup";

export function openOmnisDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: "id" });
      }
    };

    request.onsuccess = (event) => {
      resolve((event.target as IDBOpenDBRequest).result);
    };

    request.onerror = (event) => {
      console.error("[IndexedDB] Error opening database:", (event.target as IDBOpenDBRequest).error);
      reject((event.target as IDBOpenDBRequest).error);
    };
  });
}

export async function saveThreadsToIndexedDB(threads: ChatThread[]): Promise<boolean> {
  try {
    const db = await openOmnisDB();
    const tx = db.transaction(STORE_NAME, "readwrite");
    const store = tx.objectStore(STORE_NAME);

    // Clear and write fresh copy
    await new Promise<void>((resolve, reject) => {
      const clearReq = store.clear();
      clearReq.onsuccess = () => resolve();
      clearReq.onerror = () => reject(clearReq.error);
    });

    for (const thread of threads) {
      store.put(thread);
    }

    return new Promise((resolve) => {
      tx.oncomplete = () => {
        console.log(`[IndexedDB Auto-Save] Successfully saved ${threads.length} threads at ${new Date().toLocaleTimeString()}`);
        resolve(true);
      };
      tx.onerror = () => {
        console.error("[IndexedDB Auto-Save] Transaction failed:", tx.error);
        resolve(false);
      };
    });
  } catch (e) {
    console.error("[IndexedDB Auto-Save] Failed to save threads:", e);
    return false;
  }
}

export async function loadThreadsFromIndexedDB(): Promise<ChatThread[] | null> {
  try {
    const db = await openOmnisDB();
    const tx = db.transaction(STORE_NAME, "readonly");
    const store = tx.objectStore(STORE_NAME);

    return new Promise((resolve) => {
      const request = store.getAll();
      request.onsuccess = () => {
        const result = request.result as ChatThread[];
        resolve(result && result.length > 0 ? result : null);
      };
      request.onerror = () => {
        resolve(null);
      };
    });
  } catch (e) {
    console.error("[IndexedDB] Failed to load threads:", e);
    return null;
  }
}
