import React from "react";
import { Check, Copy, Terminal } from "lucide-react";

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

const MarkdownRendererComponent: React.FC<MarkdownRendererProps> = ({ content, className = "" }) => {
  const [copiedIndex, setCopiedIndex] = React.useState<number | null>(null);

  const copyCode = (code: string, idx: number) => {
    navigator.clipboard.writeText(code).catch(() => {
      const ta = document.createElement("textarea");
      ta.value = code;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      document.body.removeChild(ta);
    });
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2200);
  };

  // ── Inline renderer: bold, italic, code, links ──────────────────────────
  const renderInline = (text: string): React.ReactNode[] => {
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
            className="px-1.5 py-0.5 mx-0.5 rounded bg-slate-800/90 text-[#00F0FF] border border-cyan-900/50 font-mono text-[12px] leading-tight"
          >
            {codeMatch[1]}
          </code>
        );
        remaining = remaining.slice(codeMatch[0].length);
        continue;
      }
      // Bold+italic ***...***
      const boldItalicMatch = remaining.match(/^\*\*\*([^*]+)\*\*\*/);
      if (boldItalicMatch) {
        parts.push(<strong key={key++} className="font-extrabold italic text-white">{boldItalicMatch[1]}</strong>);
        remaining = remaining.slice(boldItalicMatch[0].length);
        continue;
      }
      // Bold **...**
      const boldMatch = remaining.match(/^\*\*([^*]+)\*\*/);
      if (boldMatch) {
        parts.push(<strong key={key++} className="font-bold text-white tracking-wide">{boldMatch[1]}</strong>);
        remaining = remaining.slice(boldMatch[0].length);
        continue;
      }
      // Italic *...* or _..._
      const italicMatch = remaining.match(/^\*([^*]+)\*/) || remaining.match(/^_([^_]+)_/);
      if (italicMatch) {
        parts.push(<em key={key++} className="italic text-cyan-200">{italicMatch[1]}</em>);
        remaining = remaining.slice(italicMatch[0].length);
        continue;
      }
      // Markdown link [text](url)
      const linkMatch = remaining.match(/^\[([^\]]+)\]\(([^)]+)\)/);
      if (linkMatch) {
        parts.push(
          <a
            key={key++}
            href={linkMatch[2]}
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#00F0FF] underline underline-offset-2 hover:text-cyan-300 transition-colors"
          >
            {linkMatch[1]}
          </a>
        );
        remaining = remaining.slice(linkMatch[0].length);
        continue;
      }
      // Plain text
      const nextSpecial = remaining.search(/[`*_\[]/);
      if (nextSpecial === -1) { parts.push(remaining); break; }
      if (nextSpecial === 0) { parts.push(remaining[0]); remaining = remaining.slice(1); }
      else { parts.push(remaining.slice(0, nextSpecial)); remaining = remaining.slice(nextSpecial); }
    }
    return parts;
  };

  // ── Line-by-line block parser ────────────────────────────────────────────
  const lines = content.split("\n");
  const elements: React.ReactNode[] = [];
  let inCodeBlock = false;
  let codeBlockLang = "";
  let codeBlockBuffer: string[] = [];
  let codeBlockCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // ── Code fence ````...```` ──
    if (line.trimStart().startsWith("```")) {
      if (inCodeBlock) {
        const fullCode = codeBlockBuffer.join("\n");
        const currentIndex = codeBlockCount++;
        const langLabel = codeBlockLang || "code";
        elements.push(
          <div key={`code-${i}`} className="code-block-wrapper my-4 shadow-xl animate-fade-in">
            <div className="code-block-header">
              <div className="flex items-center gap-2">
                <Terminal className="w-3.5 h-3.5 text-slate-500" />
                <span className="text-[#00F0FF] uppercase tracking-widest font-bold">{langLabel}</span>
              </div>
              <button
                onClick={() => copyCode(fullCode, currentIndex)}
                className="code-copy-btn flex items-center gap-1.5"
              >
                {copiedIndex === currentIndex ? (
                  <>
                    <Check className="w-3 h-3 text-emerald-400" />
                    <span className="text-emerald-400">Zkopírováno!</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3 h-3" />
                    <span>Kopírovat</span>
                  </>
                )}
              </button>
            </div>
            <pre className="p-4 text-[13px] font-mono text-cyan-100/90 overflow-x-auto leading-[1.7] scrollbar-thin">
              <code>{fullCode}</code>
            </pre>
          </div>
        );
        inCodeBlock = false;
        codeBlockBuffer = [];
        codeBlockLang = "";
      } else {
        inCodeBlock = true;
        codeBlockLang = line.trimStart().slice(3).trim();
      }
      continue;
    }

    if (inCodeBlock) { codeBlockBuffer.push(line); continue; }

    // ── Horizontal rule ────
    if (line.match(/^---+$/) || line.match(/^\*\*\*+$/)) {
      elements.push(
        <hr key={`hr-${i}`} className="my-4 border-0 h-px bg-gradient-to-r from-transparent via-slate-700 to-transparent" />
      );
      continue;
    }

    // ── Empty line ────
    if (!line.trim()) {
      elements.push(<div key={`sp-${i}`} className="h-1.5" />);
      continue;
    }

    // ── Headings ────
    if (line.startsWith("# ")) {
      elements.push(
        <h1 key={`h1-${i}`} className="text-lg sm:text-xl font-extrabold gradient-text-omnis mt-5 mb-2 tracking-wide">
          {renderInline(line.slice(2))}
        </h1>
      );
    } else if (line.startsWith("## ")) {
      elements.push(
        <h2 key={`h2-${i}`} className="text-base sm:text-lg font-bold text-[#00F0FF] border-b border-slate-800/80 pb-1 mt-4 mb-2">
          {renderInline(line.slice(3))}
        </h2>
      );
    } else if (line.startsWith("### ")) {
      elements.push(
        <h3 key={`h3-${i}`} className="text-sm sm:text-base font-semibold text-purple-300 mt-3 mb-1.5">
          {renderInline(line.slice(4))}
        </h3>
      );
    } else if (line.startsWith("#### ")) {
      elements.push(
        <h4 key={`h4-${i}`} className="text-xs sm:text-sm font-semibold text-emerald-300 mt-2.5 mb-1">
          {renderInline(line.slice(5))}
        </h4>
      );
    }
    // ── Table (basic: | col | col |) ────
    else if (line.startsWith("|") && line.endsWith("|")) {
      // Collect all table rows
      const tableLines: string[] = [line];
      let j = i + 1;
      while (j < lines.length && lines[j].startsWith("|")) {
        tableLines.push(lines[j]);
        j++;
      }
      const isHeaderSep = (r: string) => /^[\|\s\-:]+$/.test(r);
      const headerRow = tableLines[0];
      const headerCols = headerRow.split("|").filter((_, idx) => idx > 0 && idx < headerRow.split("|").length - 1);
      const bodyRows = tableLines.slice(2).filter(r => !isHeaderSep(r));
      elements.push(
        <div key={`tbl-${i}`} className="my-4 overflow-x-auto rounded-xl border border-slate-800/80 scrollbar-thin">
          <table className="w-full text-xs font-mono text-left">
            <thead>
              <tr className="bg-slate-900/80 border-b border-slate-800">
                {headerCols.map((col, ci) => (
                  <th key={ci} className="px-3 py-2 text-[#00F0FF] font-bold uppercase tracking-wide whitespace-nowrap">{col.trim()}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {bodyRows.map((row, ri) => {
                const cols = row.split("|").filter((_, idx) => idx > 0 && idx < row.split("|").length - 1);
                return (
                  <tr key={ri} className="border-b border-slate-800/50 hover:bg-slate-900/40 transition-colors">
                    {cols.map((col, ci) => (
                      <td key={ci} className="px-3 py-2 text-slate-300 whitespace-nowrap">{renderInline(col.trim())}</td>
                    ))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      );
      i = j - 1;
    }
    // ── Bullet list (- or *) ────
    else if (line.match(/^[\*\-]\s+/)) {
      const contentText = line.replace(/^[\*\-]\s+/, "");
      elements.push(
        <div key={`bullet-${i}`} className="flex items-start gap-2.5 my-1.5 pl-2">
          <span className="w-1.5 h-1.5 rounded-full bg-[#00F0FF] mt-2 flex-shrink-0 shadow-[0_0_6px_rgba(0,240,255,0.6)]" />
          <span className="text-slate-200 text-sm leading-relaxed">{renderInline(contentText)}</span>
        </div>
      );
    }
    // ── Numbered list ────
    else if (line.match(/^\d+\.\s+/)) {
      const match = line.match(/^(\d+)\.\s+(.*)/);
      if (match) {
        elements.push(
          <div key={`num-${i}`} className="flex items-start gap-2.5 my-1.5 pl-2">
            <span className="px-1.5 py-0.5 rounded-md bg-purple-950/60 border border-purple-800/60 text-[#A855F7] font-mono font-bold text-xs flex-shrink-0 min-w-[20px] text-center">
              {match[1]}
            </span>
            <span className="text-slate-200 text-sm leading-relaxed">{renderInline(match[2])}</span>
          </div>
        );
      }
    }
    // ── Blockquote > ────
    else if (line.startsWith("> ")) {
      elements.push(
        <div key={`quote-${i}`} className="my-2.5 border-l-2 border-[#00F0FF] bg-cyan-950/20 pl-4 py-2 text-xs sm:text-sm text-cyan-200 italic rounded-r-xl">
          {renderInline(line.slice(2))}
        </div>
      );
    }
    // ── Paragraph ────
    else {
      elements.push(
        <p key={`p-${i}`} className="text-sm text-slate-200 leading-relaxed my-1">
          {renderInline(line)}
        </p>
      );
    }
  }

  return <div className={`space-y-0.5 ${className}`}>{elements}</div>;
};

export const MarkdownRenderer = React.memo(MarkdownRendererComponent);
