import React, { useState, useEffect, useCallback } from "react";
import { Sun, Moon, Monitor } from "lucide-react";

type ThemeMode = "dark" | "light" | "system";

const STORAGE_KEY = "omnis_theme_mode";

function applyTheme(mode: ThemeMode) {
  const html = document.documentElement;
  if (mode === "light") {
    html.classList.add("light");
    html.classList.remove("dark");
  } else if (mode === "dark") {
    html.classList.remove("light");
    html.classList.add("dark");
  } else {
    // system
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    html.classList.toggle("light", !prefersDark);
    html.classList.toggle("dark", prefersDark);
  }
}

export function useTheme() {
  const [theme, setThemeState] = useState<ThemeMode>(() => {
    try {
      return (localStorage.getItem(STORAGE_KEY) as ThemeMode) ?? "dark";
    } catch {
      return "dark";
    }
  });

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // Listen for system preference changes when in system mode
  useEffect(() => {
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const handler = () => {
      if (theme === "system") applyTheme("system");
    };
    mq.addEventListener("change", handler);
    return () => mq.removeEventListener("change", handler);
  }, [theme]);

  const setTheme = useCallback((mode: ThemeMode) => {
    try {
      localStorage.setItem(STORAGE_KEY, mode);
    } catch {}
    setThemeState(mode);
  }, []);

  return { theme, setTheme };
}

interface ThemeToggleProps {
  theme: ThemeMode;
  setTheme: (mode: ThemeMode) => void;
}

export const ThemeToggle: React.FC<ThemeToggleProps> = ({ theme, setTheme }) => {
  const modes: { mode: ThemeMode; icon: React.ReactNode; label: string }[] = [
    { mode: "dark",   icon: <Moon className="w-3.5 h-3.5" />,    label: "Tmavý" },
    { mode: "light",  icon: <Sun className="w-3.5 h-3.5" />,     label: "Světlý" },
    { mode: "system", icon: <Monitor className="w-3.5 h-3.5" />, label: "Systém" },
  ];

  const nextMode: ThemeMode =
    theme === "dark" ? "light" : theme === "light" ? "system" : "dark";

  const current = modes.find((m) => m.mode === theme)!;

  return (
    <button
      onClick={() => setTheme(nextMode)}
      title={`Přepnout motiv: aktuálně ${current.label}. Kliknutím přepnete na ${modes.find(m=>m.mode===nextMode)?.label}.`}
      className="flex items-center gap-1.5 px-2.5 py-2 rounded-xl text-xs font-bold font-mono bg-[#0A0F1D] hover:bg-slate-800 border border-slate-700/80 hover:border-slate-600 text-slate-300 hover:text-slate-100 transition-all min-h-[38px]"
    >
      {current.icon}
      <span className="hidden lg:inline">{current.label}</span>
    </button>
  );
};
