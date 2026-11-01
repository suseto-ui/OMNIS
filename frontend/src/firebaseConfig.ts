import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyACPYG9kNF8Tii8vVqFN-Ns9Lcv6_OQaJY",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "omnis-508519.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "omnis-508519",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "omnis-508519.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "1098834450200",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:1098834450200:web:d32hucnf0ve068o1calg0e"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
