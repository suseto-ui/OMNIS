import React from "react";

interface SkeletonMessageProps {
  isUser?: boolean;
}

export const SkeletonMessage: React.FC<SkeletonMessageProps> = ({ isUser = false }) => {
  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"} w-full animate-fade-in`}>
      <div className={`max-w-[75%] space-y-2 ${isUser ? "items-end" : "items-start"} flex flex-col`}>
        {/* Avatar + name row */}
        <div className={`flex items-center gap-2 ${isUser ? "flex-row-reverse" : "flex-row"}`}>
          <div className="skeleton w-7 h-7 rounded-lg flex-shrink-0" />
          <div className="skeleton skeleton-line w-20 h-3" />
        </div>
        {/* Message bubble */}
        <div className={`w-full p-4 rounded-2xl border border-slate-800/60 ${isUser ? "bg-slate-900/40" : "bg-[#0A0F1D]/60"} space-y-2`}>
          <div className="skeleton skeleton-line-full skeleton-line" />
          <div className="skeleton skeleton-line-medium skeleton-line" />
          {!isUser && (
            <>
              <div className="skeleton skeleton-line-full skeleton-line" />
              <div className="skeleton skeleton-line-short skeleton-line" style={{ marginTop: "16px" }} />
            </>
          )}
        </div>
        {/* Actions row for assistant */}
        {!isUser && (
          <div className="flex gap-2">
            <div className="skeleton w-16 h-6 rounded-lg" />
            <div className="skeleton w-16 h-6 rounded-lg" />
            <div className="skeleton w-20 h-6 rounded-lg" />
          </div>
        )}
      </div>
    </div>
  );
};

export const SkeletonLoader: React.FC = () => {
  return (
    <div className="space-y-6 p-4 sm:p-6 w-full max-w-3xl mx-auto">
      {/* Typing indicator banner */}
      <div className="flex items-center justify-center gap-2 py-3">
        <div className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-[#0A0F1D] border border-[#00F0FF]/20 shadow-[0_0_15px_rgba(0,240,255,0.1)]">
          <span className="text-[11px] font-mono text-[#00F0FF] uppercase tracking-widest">O.M.N.I.S. zpracovává</span>
          <div className="flex items-end gap-1 ml-1">
            <span className="typing-dot" />
            <span className="typing-dot" />
            <span className="typing-dot" />
          </div>
        </div>
      </div>

      {/* Phase progress indicators */}
      <div className="grid grid-cols-5 gap-1.5 max-w-md mx-auto">
        {["I. Sémantika", "II. Syntéza", "III. Akce", "IV. Exekuce", "V. Matice"].map((phase, i) => (
          <div key={i} className="flex flex-col items-center gap-1">
            <div
              className="skeleton h-1.5 w-full rounded-full"
              style={{ animationDelay: `${i * 0.15}s` }}
            />
            <span className="text-[8px] font-mono text-slate-600 text-center leading-tight hidden sm:block">
              {phase}
            </span>
          </div>
        ))}
      </div>

      {/* Skeleton message */}
      <SkeletonMessage isUser={false} />
    </div>
  );
};
