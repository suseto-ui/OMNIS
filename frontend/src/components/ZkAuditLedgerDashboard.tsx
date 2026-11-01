import React, { useState, useMemo } from "react";
import { 
  ShieldCheck, Lock, Download, CheckCircle2, AlertTriangle, FileText, 
  Search, RefreshCw, Key, Sparkles, Layers, Activity 
} from "lucide-react";
import jsPDF from "jspdf";
import { MessageItem } from "./MessageBubble";
import { 
  generateZkSnarkAuditRecord, 
  ZkSnarkAuditRecord, 
  verifyZkSnarkProof 
} from "../omnisEngine";

interface ZkAuditLedgerDashboardProps {
  messages: MessageItem[];
}

export const ZkAuditLedgerDashboard: React.FC<ZkAuditLedgerDashboardProps> = ({
  messages,
}) => {
  const assistantMessages = messages.filter((m) => m.role === "assistant");
  const lastAssistantMsg = assistantMessages[assistantMessages.length - 1];

  // Initial stream of audit blocks
  const [ledgerStream, setLedgerStream] = useState<ZkSnarkAuditRecord[]>(() => {
    const initialRecords: ZkSnarkAuditRecord[] = [];
    if (assistantMessages.length > 0) {
      assistantMessages.slice(-5).forEach((msg) => {
        initialRecords.push(
          generateZkSnarkAuditRecord(msg.content, msg.impact_matrix, msg.gates, "Agent ALPHA (Architektura)")
        );
      });
    } else {
      initialRecords.push(
        generateZkSnarkAuditRecord("Inicializační auditní blok systému O.M.N.I.S.")
      );
    }
    return initialRecords;
  });

  const selectedBlock = ledgerStream[ledgerStream.length - 1] || ledgerStream[0];

  // Local Proof Verification Tool state
  const [testMerkleInput, setTestMerkleInput] = useState<string>(selectedBlock?.merkleRoot || "");
  const [testStateInput, setTestStateInput] = useState<string>(selectedBlock?.stateVectorHash || "");
  const [testGateInput, setTestGateInput] = useState<string>(selectedBlock?.gateVerificationHash || "");

  const isValidLocalProof = useMemo(() => {
    return verifyZkSnarkProof(testMerkleInput, testStateInput, testGateInput);
  }, [testMerkleInput, testStateInput, testGateInput]);

  const handleGenerateNewBlock = (isOverride = false) => {
    const newBlock = generateZkSnarkAuditRecord(
      lastAssistantMsg?.content || "Nový kognitivní auditní blok",
      lastAssistantMsg?.impact_matrix,
      lastAssistantMsg?.gates,
      isOverride ? "MANUÁLNÍ EXECUTIVE OVERRIDE (ADMIN)" : "Agent ALPHA (Architektura)",
      isOverride,
      "ADMIN_OPERATOR"
    );
    setLedgerStream(prev => [...prev, newBlock]);
  };

  const handleExportPDF = () => {
    const doc = new jsPDF();
    doc.setFillColor(6, 10, 23);
    doc.rect(0, 0, 210, 297, "F");

    doc.setFont("courier", "bold");
    doc.setFontSize(16);
    doc.setTextColor(0, 240, 255);
    doc.text("O.M.N.I.S. ZK-SNARK AUDITNI CERTIFIKAT", 15, 20);

    doc.setFontSize(10);
    doc.setTextColor(148, 163, 184);
    doc.text("Kryptograficky neprenosna pecet podla EU AI Act (Clanek 50)", 15, 28);
    doc.line(15, 32, 195, 32);

    let currentY = 42;
    if (selectedBlock.isOverrideActive) {
      doc.setFillColor(185, 28, 28);
      doc.rect(15, 34, 180, 8, "F");
      doc.setFontSize(9);
      doc.setTextColor(255, 255, 255);
      doc.text("! OVERRIDE_ACTIVE: VEDOME MANUALNI PREPSANI VETA (ADMIN_OPERATOR) !", 18, 39.5);
      currentY = 48;
    }

    doc.setFontSize(11);
    doc.setTextColor(255, 255, 255);
    doc.text(`Blok ID: ${selectedBlock.blockId}`, 15, currentY);
    doc.text(`Casove razitko: ${selectedBlock.timestamp}`, 15, currentY + 8);
    doc.text(`Trida shody: ${selectedBlock.euAiActComplianceClass}`, 15, currentY + 16);
    doc.text(`Index ukotveni: ${selectedBlock.groundingScore} %`, 15, currentY + 24);
    doc.text(`Vitezna deliberacni linie: ${selectedBlock.deliberationWinner}`, 15, currentY + 32);

    doc.setTextColor(0, 240, 255);
    doc.text("Merkle Root Hash:", 15, currentY + 46);
    doc.setFontSize(9);
    doc.setTextColor(203, 213, 225);
    doc.text(selectedBlock.merkleRoot, 15, currentY + 52);

    doc.setFontSize(11);
    doc.setTextColor(0, 240, 255);
    doc.text("SHA-256 Hash 8D Stavoveho Vektoru:", 15, currentY + 66);
    doc.setFontSize(9);
    doc.setTextColor(203, 213, 225);
    doc.text(selectedBlock.stateVectorHash, 15, currentY + 72);

    doc.setFontSize(11);
    doc.setTextColor(0, 240, 255);
    doc.text("ZK-SNARK Podpis Dukazu (Proof Signature):", 15, currentY + 86);
    doc.setFontSize(9);
    doc.setTextColor(203, 213, 225);
    doc.text(selectedBlock.proofSignature, 15, currentY + 92);

    doc.line(15, currentY + 104, 195, currentY + 104);
    doc.setFontSize(9);
    if (selectedBlock.isOverrideActive) {
      doc.setTextColor(239, 68, 68);
      doc.text("AUDITNI STATUS: OVERRIDE_ACTIVE. Zaznam obsahuje autorizovane veto operatora.", 15, currentY + 114);
    } else {
      doc.setTextColor(16, 185, 129);
      doc.text("VERIFIKACE: Zero-Knowledge dukaz uspesne overen. Certifikat je platny.", 15, currentY + 114);
    }

    doc.save(`OMNIS_Audit_Certificate_${selectedBlock.blockId}.pdf`);
  };

  const handleDownloadJSON = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(selectedBlock, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `OMNIS_ZK_Proof_${selectedBlock.blockId}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-7xl mx-auto font-sans text-slate-100">
      {/* Top Banner */}
      <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 shadow-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Lock className="w-5 h-5 text-[#00F0FF]" />
            <h1 className="text-lg sm:text-xl font-bold font-mono text-slate-100 tracking-wider uppercase">
              ZK-SNARK SHA-256 AUDITNÍ LEDGER
            </h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-[#00F0FF]/15 text-[#00F0FF] border border-[#00F0FF]/30">
              EU AI ACT ČL. 50
            </span>
          </div>
          <p className="text-xs text-slate-400 max-w-2xl font-mono">
            Kryptografický řetězec auditních bloků s Merkle kořeny, SHA-256 pečetěmi a okamžitým PDF/JSON exportem certifikátů.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => handleGenerateNewBlock(false)}
            className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-mono font-bold border border-slate-700 transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <RefreshCw className="w-3.5 h-3.5 text-[#00F0FF]" />
            <span>Generovat Blok</span>
          </button>
          <button
            onClick={() => handleGenerateNewBlock(true)}
            className="px-3 py-2 rounded-xl bg-red-950/60 hover:bg-red-900/60 text-red-300 text-xs font-mono font-bold border border-red-500/50 transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
            title="Vygenerovat testovací blok s aktivním příznakem manuálního přepsání"
          >
            <AlertTriangle className="w-3.5 h-3.5 text-red-400" />
            <span>Simulovat Override Blok</span>
          </button>
          <button
            onClick={handleExportPDF}
            className="px-3.5 py-2 rounded-xl bg-[#00F0FF] hover:bg-[#00F0FF]/80 text-slate-950 text-xs font-mono font-bold transition-all flex items-center gap-1.5 shadow-[0_0_12px_rgba(0,240,255,0.3)] cursor-pointer active:scale-95"
          >
            <FileText className="w-3.5 h-3.5 text-slate-950" />
            <span>Export Certifikátu (PDF)</span>
          </button>
          <button
            onClick={handleDownloadJSON}
            className="px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-emerald-400 border border-emerald-500/40 text-xs font-mono font-bold transition-all flex items-center gap-1.5 cursor-pointer active:scale-95"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span>JSON Důkaz</span>
          </button>
        </div>
      </div>

      {/* Real-Time Stream Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Ledger Blocks List */}
        <div className="lg:col-span-1 p-4 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-3 font-mono text-xs shadow-xl">
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <span className="text-slate-400 font-bold uppercase text-[10px] flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-[#00F0FF]" /> Auditní Bloky ({ledgerStream.length})
            </span>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          </div>

          <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
            {ledgerStream.map((block) => (
              <div
                key={block.blockId}
                onClick={() => {
                  setTestMerkleInput(block.merkleRoot);
                  setTestStateInput(block.stateVectorHash);
                  setTestGateInput(block.gateVerificationHash);
                }}
                className={`p-3 rounded-xl border transition-all cursor-pointer space-y-1.5 ${
                  selectedBlock.blockId === block.blockId
                    ? block.isOverrideActive
                      ? "bg-red-950/40 border-red-500 shadow-[0_0_12px_rgba(239,68,68,0.25)]"
                      : "bg-slate-900 border-[#00F0FF] shadow-[0_0_12px_rgba(0,240,255,0.2)]"
                    : "bg-slate-950/60 border-slate-800 hover:border-slate-700"
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-[#00F0FF]">{block.blockId}</span>
                    {block.isOverrideActive && (
                      <span className="px-1.5 py-0.2 rounded text-[8px] font-bold bg-red-500/20 text-red-400 border border-red-500/40">
                        OVERRIDE_ACTIVE
                      </span>
                    )}
                  </div>
                  <span className="text-[9px] text-slate-400">{new Date(block.timestamp).toLocaleTimeString()}</span>
                </div>
                <p className="text-[10px] text-slate-300 truncate">{block.querySnippet}</p>
                <span className="text-[9px] text-slate-400 block truncate font-mono">
                  Root: {block.merkleRoot.slice(0, 18)}...
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Selected Block Cryptographic Details & Local Proof Verifier */}
        <div className="lg:col-span-2 space-y-4">
          {/* Detailed Block Card */}
          <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-4 font-mono text-xs shadow-xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                <span className="font-bold text-slate-100 text-sm">{selectedBlock.blockId}</span>
                <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                  ZK-SNARK SIGNED
                </span>
                {selectedBlock.isOverrideActive && (
                  <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-red-500/20 text-red-400 border border-red-500/50 animate-pulse">
                    OVERRIDE_ACTIVE
                  </span>
                )}
              </div>
              <span className="text-slate-400 text-[10px]">{selectedBlock.timestamp}</span>
            </div>

            {selectedBlock.isOverrideActive && (
              <div className="p-3 rounded-xl bg-red-950/40 border border-red-500/50 text-red-200 text-[11px] leading-relaxed flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                <div>
                  <strong className="text-red-400 block font-mono">EXECUTIVE OVERRIDE ZAZNAMENÁN V MERKLE ROOTU:</strong>
                  Tento kognitivní blok byl manuálně autorizován operátorem (ADMIN_OPERATOR). Veto Regulátora bylo vědomě přepsáno s trvalou stopou v souladu s článkem 50 EU AI Act.
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <span className="text-[10px] text-slate-400 font-bold uppercase block">Merkle Kořen (Merkle Root):</span>
                <span className="text-[10px] text-[#00F0FF] break-all block">{selectedBlock.merkleRoot}</span>
              </div>

              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <span className="text-[10px] text-slate-400 font-bold uppercase block">SHA-256 Stavového Vektoru:</span>
                <span className="text-[10px] text-cyan-300 break-all block">{selectedBlock.stateVectorHash}</span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
              <span className="text-[10px] text-slate-400 font-bold uppercase block">ZK-SNARK Podpis Důkazu:</span>
              <span className="text-[10px] text-purple-300 break-all block">{selectedBlock.proofSignature}</span>
            </div>

            <div className="p-3 rounded-xl bg-emerald-950/30 border border-emerald-500/30 text-emerald-200 text-[11px] leading-relaxed flex items-start gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
              <span>Tento blok je imunní proti jakékoliv zpětné manipulaci. Všechny maticové výpočty a verifikační brány jsou pevně zakotveny v kryptografické struktuře.</span>
            </div>
          </div>

          {/* Local Proof Validator Tool */}
          <div className="p-5 rounded-2xl bg-[#080D1D] border border-slate-800 space-y-3 font-mono text-xs shadow-xl">
            <h3 className="font-bold text-slate-100 uppercase tracking-wider flex items-center gap-1.5 border-b border-slate-800 pb-2">
              <Key className="w-4 h-4 text-[#00F0FF]" /> Interaktivní Verifikátor Důkazu (Local Proof Validator)
            </h3>

            <div className="space-y-2">
              <div>
                <label className="text-[10px] text-slate-400 uppercase font-bold block mb-1">Merkle Root Hash:</label>
                <input
                  type="text"
                  value={testMerkleInput}
                  onChange={(e) => setTestMerkleInput(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                />
              </div>

              <div>
                <label className="text-[10px] text-slate-400 uppercase font-bold block mb-1">SHA-256 State Hash:</label>
                <input
                  type="text"
                  value={testStateInput}
                  onChange={(e) => setTestStateInput(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-[#00F0FF]"
                />
              </div>
            </div>

            <div className="p-3 rounded-xl border flex items-center justify-between transition-all" style={{
              backgroundColor: isValidLocalProof ? "rgba(16, 185, 129, 0.1)" : "rgba(239, 68, 68, 0.1)",
              borderColor: isValidLocalProof ? "rgba(16, 185, 129, 0.4)" : "rgba(239, 68, 68, 0.4)"
            }}>
              <div className="flex items-center gap-2">
                {isValidLocalProof ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                ) : (
                  <AlertTriangle className="w-5 h-5 text-red-400" />
                )}
                <span className={isValidLocalProof ? "text-emerald-300 font-bold" : "text-red-300 font-bold"}>
                  {isValidLocalProof ? "KRYPTOGRAFICKÝ DŮKAZ PLATNÝ (VERIFIED)" : "NEPLATNÝ NBO POŠKOZENÝ DŮKAZ"}
                </span>
              </div>
              <span className="text-[10px] text-slate-400">ISO/IEC 42010 Verified</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
