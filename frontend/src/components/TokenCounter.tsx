import React, { useMemo } from "react";
import { Zap } from "lucide-react";

interface TokenCounterProps {
  text: string;
  hasImage?: boolean;
}

/**
 * Lightweight heuristic token estimator.
 * Approximates GPT/Gemini tokenization: ~4 chars per token for Latin text.
 * Images add a fixed ~258 tokens (Gemini vision approximation).
 */
function estimateTokens(text: string, hasImage: boolean): number {
  if (!text && !hasImage) return 0;
  // Rough heuristic: split on word boundaries + punctuation
  const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
  const charCount = text.length;
  // Average: (wordCount * 1.3 + charCount / 6) / 2 ≈ reasonable estimate
  const textTokens = Math.ceil((wordCount * 1.3 + charCount / 6) / 2);
  const imageTokens = hasImage ? 258 : 0;
  return textTokens + imageTokens;
}

const LIMITS = {
  safe:    1000,
  warning: 3000,
  danger:  8000,
};

export const TokenCounter: React.FC<TokenCounterProps> = ({ text, hasImage = false }) => {
  const tokenCount = useMemo(() => estimateTokens(text, hasImage), [text, hasImage]);

  if (!text && !hasImage) return null;

  const pct = Math.min((tokenCount / LIMITS.danger) * 100, 100);

  const colorClass =
    tokenCount < LIMITS.safe
      ? "text-slate-500"
      : tokenCount < LIMITS.warning
      ? "text-amber-400"
      : "text-red-400";

  const barColor =
    tokenCount < LIMITS.safe
      ? "bg-slate-600"
      : tokenCount < LIMITS.warning
      ? "bg-amber-500"
      : "bg-red-500";

  return (
    <div
      className={`flex items-center gap-1.5 px-2 py-1 rounded-lg text-[10px] font-mono transition-all ${colorClass}`}
      title={`Odhadovaný počet tokenů: ~${tokenCount}. Limit Gemini: ~8,000 (vstup).`}
    >
      <Zap className="w-3 h-3 flex-shrink-0" />
      <span>~{tokenCount} tk</span>
      {/* Mini progress bar */}
      <div className="w-12 h-1 rounded-full bg-slate-800 overflow-hidden hidden sm:block">
        <div
          className={`h-full rounded-full transition-all duration-300 ${barColor}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
};
