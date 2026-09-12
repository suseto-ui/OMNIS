import React from "react";
import { Check, Copy } from "lucide-react";

interface MarkdownRendererProps {
  content: string;
}

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  const [copiedIndex, setCopiedIndex] = React.useState<number | null>(null);

  const copyCode = (code: string, idx: number) => {
    navigator.clipboard.writeText(code);
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  // Helper to render bold, italic, code spans in a single line
  const renderInline = (text: string): React.ReactNode => {
    const parts: React.ReactNode[] = [];
    let remaining = text;
    let key = 0;

    while (remaining.length > 0) {
      // Inline code `...`
      const codeMatch = remaining.match(/^`([^`]+)`/);
      if (codeMatch) {
        parts.push(
          <code
            key={key++}
            className="px-1.5 py-0.5 mx-0.5 rounded bg-slate-800/90 text-[#00F0FF] border border-cyan-900/50 font-mono text-[12px]"
          >
            {codeMatch[1]}
          </code>
        );
        remaining = remaining.slice(codeMatch[0].length);
        continue;
      }

      // Bold **...**
      const boldMatch = remaining.match(/^\*\*([^*]+)\*\*/);
      if (boldMatch) {
        parts.push(
          <strong key={key++} className="font-bold text-white tracking-wide">
            {boldMatch[1]}
          </strong>
        );
        remaining = remaining.slice(boldMatch[0].length);
        continue;
      }

      // Italic *...* or _..._
      const italicMatch = remaining.match(/^\*([^*]+)\*/) || remaining.match(/^_([^_]+)_/);
      if (italicMatch) {
        parts.push(
          <em key={key++} className="italic text-cyan-200">
            {italicMatch[1]}
          </em>
        );
        remaining = remaining.slice(italicMatch[0].length);
        continue;
      }

      // Plain text character
      const nextSpecial = remaining.search(/[`*_]/);
      if (nextSpecial === -1) {
        parts.push(remaining);
        break;
      } else if (nextSpecial === 0) {
        // Stray character not matching a pattern
        parts.push(remaining[0]);
        remaining = remaining.slice(1);
      } else {
        parts.push(remaining.slice(0, nextSpecial));
        remaining = remaining.slice(nextSpecial);
      }
    }

    return parts;
  };

  // Parse lines into blocks
  const lines = content.split("\n");
  const elements: React.ReactNode[] = [];
  let inCodeBlock = false;
  let codeBlockLang = "";
  let codeBlockBuffer: string[] = [];
  let codeBlockCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Code block toggle
    if (line.startsWith("```")) {
      if (inCodeBlock) {
        const fullCode = codeBlockBuffer.join("\n");
        const currentIndex = codeBlockCount++;
        elements.push(
          <div key={`code-${i}`} className="my-3 rounded-xl bg-[#080d1a] border border-slate-700/80 overflow-hidden shadow-lg">
            <div className="flex items-center justify-between px-3 py-1.5 bg-slate-900/90 border-b border-slate-800 text-xs font-mono text-slate-400">
              <span className="uppercase text-[#00F0FF]">{codeBlockLang || "code"}</span>
              <button
                onClick={() => copyCode(fullCode, currentIndex)}
                className="flex items-center gap-1 hover:text-white transition-colors"
              >
                {copiedIndex === currentIndex ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="text-emerald-400">Zkopírováno</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Kopírovat</span>
                  </>
                )}
              </button>
            </div>
            <pre className="p-3 text-xs font-mono text-cyan-100 overflow-x-auto leading-relaxed">
              <code>{fullCode}</code>
            </pre>
          </div>
        );
        inCodeBlock = false;
        codeBlockBuffer = [];
        codeBlockLang = "";
      } else {
        inCodeBlock = true;
        codeBlockLang = line.slice(3).trim();
      }
      continue;
    }

    if (inCodeBlock) {
      codeBlockBuffer.push(line);
      continue;
    }

    // Empty line
    if (!line.trim()) {
      elements.push(<div key={`space-${i}`} className="h-2" />);
      continue;
    }

    // Headings
    if (line.startsWith("# ")) {
      elements.push(
        <h1 key={`h1-${i}`} className="text-lg sm:text-xl font-extrabold text-transparent bg-gradient-to-r from-[#00F0FF] via-purple-300 to-emerald-400 bg-clip-text mt-4 mb-2 tracking-wide">
          {renderInline(line.slice(2))}
        </h1>
      );
    } else if (line.startsWith("## ")) {
      elements.push(
        <h2 key={`h2-${i}`} className="text-base sm:text-lg font-bold text-[#00F0FF] border-b border-slate-800/80 pb-1 mt-3 mb-2 flex items-center gap-2">
          {renderInline(line.slice(3))}
        </h2>
      );
    } else if (line.startsWith("### ")) {
      elements.push(
        <h3 key={`h3-${i}`} className="text-sm sm:text-base font-semibold text-purple-300 mt-2.5 mb-1.5">
          {renderInline(line.slice(4))}
        </h3>
      );
    } else if (line.startsWith("#### ")) {
      elements.push(
        <h4 key={`h4-${i}`} className="text-xs sm:text-sm font-semibold text-emerald-300 mt-2 mb-1">
          {renderInline(line.slice(5))}
        </h4>
      );
    }
    // Bullet list (- or *)
    else if (line.match(/^[\*\-]\s+/)) {
      const contentText = line.replace(/^[\*\-]\s+/, "");
      elements.push(
        <div key={`bullet-${i}`} className="flex items-start gap-2.5 my-1 pl-2">
          <span className="w-1.5 h-1.5 rounded-full bg-[#00F0FF] mt-2 flex-shrink-0 shadow-[0_0_6px_#00F0FF]" />
          <span className="text-slate-200 text-sm leading-relaxed">{renderInline(contentText)}</span>
        </div>
      );
    }
    // Numbered list (1. 2. ...)
    else if (line.match(/^\d+\.\s+/)) {
      const match = line.match(/^(\d+)\.\s+(.*)/);
      if (match) {
        elements.push(
          <div key={`num-${i}`} className="flex items-start gap-2.5 my-1.5 pl-2">
            <span className="px-1.5 py-0.5 rounded-md bg-purple-950/60 border border-purple-800/60 text-[#A855F7] font-mono font-bold text-xs flex-shrink-0">
              {match[1]}
            </span>
            <span className="text-slate-200 text-sm leading-relaxed">{renderInline(match[2])}</span>
          </div>
        );
      }
    }
    // Blockquote
    else if (line.startsWith("> ")) {
      elements.push(
        <div key={`quote-${i}`} className="my-2 border-l-2 border-[#00F0FF] bg-cyan-950/20 pl-3 py-1.5 text-xs sm:text-sm text-cyan-200 italic rounded-r-lg">
          {renderInline(line.slice(2))}
        </div>
      );
    }
    // Regular paragraph
    else {
      elements.push(
        <p key={`p-${i}`} className="text-sm text-slate-200 leading-relaxed my-1 font-sans">
          {renderInline(line)}
        </p>
      );
    }
  }

  return <div className="space-y-0.5">{elements}</div>;
};
