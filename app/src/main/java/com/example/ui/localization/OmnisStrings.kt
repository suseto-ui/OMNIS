package com.example.ui.localization

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OmnisTab

/**
 * Centrální lokalizační slovník aplikace O.M.N.I.S.
 * Poskytuje reaktivní překlady pro všechny komponenty, navigaci, dialogy a stavy.
 */
object OmnisStrings {

    val currentLang: AppLanguage
        get() = AppLocaleManager.currentLanguage.value

    // ==========================================
    // NAVIGACE A NÁZVY ZÁLOŽEK
    // ==========================================
    fun tabTitle(tab: OmnisTab, lang: AppLanguage = currentLang): String = when (lang) {
        AppLanguage.CS -> when (tab) {
            OmnisTab.CHAT -> "Kognitivní Chat"
            OmnisTab.NEXUS -> "Neural Nexus"
            OmnisTab.DASHBOARD -> "Operační Kokpit"
            OmnisTab.GUIDE -> "Metodika & Průvodce"
            OmnisTab.GOALS -> "Autonomní Cíle"
            OmnisTab.SCENARIOS -> "Scenario Architect"
            OmnisTab.ARTIFACTS -> "Artefakty & Výstupy"
            OmnisTab.MEMORY -> "Historická Paměť"
            OmnisTab.ANALYTICS -> "Analytický Přehled (8D)"
            OmnisTab.ADMIN -> "Systémová Správa"
            OmnisTab.NODES -> "Kognitivní Uzly"
            OmnisTab.MATRIX -> "8D Matice & Octagon"
            OmnisTab.TELEMETRY -> "Systémová Telemetrie"
            OmnisTab.TEST_SEMANTIC -> "Sémantické Testování"
            OmnisTab.DEV_PROMPT_LAB -> "Dev Prompt Lab"
            OmnisTab.CAUSAL_SIMULATOR -> "Causal Simulator"
            OmnisTab.DIALECTICS -> "Dialektický Motor"
            OmnisTab.PRODUCTION_AUDIT -> "Produkční Audit & Benchmark"
        }
        AppLanguage.EN -> when (tab) {
            OmnisTab.CHAT -> "Cognitive Chat"
            OmnisTab.NEXUS -> "Neural Nexus"
            OmnisTab.DASHBOARD -> "Operations Cockpit"
            OmnisTab.GUIDE -> "Methodology & Guide"
            OmnisTab.GOALS -> "Autonomous Goals"
            OmnisTab.SCENARIOS -> "Scenario Architect"
            OmnisTab.ARTIFACTS -> "Artifacts & Outputs"
            OmnisTab.MEMORY -> "Historical Memory"
            OmnisTab.ANALYTICS -> "8D Analytical Overview"
            OmnisTab.ADMIN -> "System Administration"
            OmnisTab.NODES -> "Cognitive Nodes"
            OmnisTab.MATRIX -> "8D Matrix & Octagon"
            OmnisTab.TELEMETRY -> "System Telemetry"
            OmnisTab.TEST_SEMANTIC -> "Semantic Testing"
            OmnisTab.DEV_PROMPT_LAB -> "Dev Prompt Lab"
            OmnisTab.CAUSAL_SIMULATOR -> "Causal Simulator"
            OmnisTab.DIALECTICS -> "Dialectics Engine"
            OmnisTab.PRODUCTION_AUDIT -> "Production Audit & Benchmark"
        }
    }

    // ==========================================
    // SPODNÍ NAVIGAČNÍ LIŠTA (BOTTOM BAR)
    // ==========================================
    fun bottomBarChat(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Chat" else "Chat"
    fun bottomBarNexus(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Nexus" else "Nexus"
    fun bottomBarCockpit(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Cockpit" else "Kokpit"
    fun bottomBarGoals(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Goals" else "Cíle"
    fun bottomBarAdmin(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Admin" else "Admin"
    fun bottomBarNodes(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Nodes" else "Uzly"
    fun bottomBarMatrix(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Matrix" else "Matice"
    fun bottomBarAudit(lang: AppLanguage = currentLang) = if (lang == AppLanguage.EN) "Audit" else "Audit"

    // ==========================================
    // BOČNÍ MENU (DRAWER)
    // ==========================================
    fun drawerHeaderSubtitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Cognitive Governance v2.7" else "Kognitivní Řízení v2.7"
    fun drawerSectionConversation(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "CONVERSATION MODULES" else "KONVERZAČNÍ PRVKY"
    fun drawerSectionInfo(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "INFORMATION MODULES & OVERVIEW" else "INFORMAČNÍ MODULY & PŘEHLEDY"
    fun drawerSectionPlanning(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "STRATEGIC PLANNING & ANALYSIS" else "STRATEGICKÉ PLÁNOVÁNÍ"
    fun drawerSectionDiagnostics(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "DIAGNOSTICS & SYSTEM CONTROL" else "DIAGNOSTIKA & SYSTÉMOVÉ ŘÍZENÍ"
    fun drawerActiveSession(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Active Session" else "Aktivní Relace"
    fun drawerOperatorMode(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Operator Mode" else "Režim Operátora"
    fun drawerStandardUser(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Standard User" else "Běžný Uživatel"
    fun drawerLogout(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Logout" else "Odhlásit se"

    // ==========================================
    // HORNÍ LIŠTA (TOP APP BAR)
    // ==========================================
    fun topBarNewThread(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "New Thread" else "Nové Vlákno"
    fun topBarDeleteHistory(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Clear History" else "Smazat Historii"
    fun topBarCloudSync(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Cloud Sync" else "Cloud Sync"
    fun topBarExportPdf(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Export to PDF" else "Exportovat do PDF"
    fun topBarExportMarkdown(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Export to Markdown" else "Exportovat do Markdown"
    fun topBarCertifiedAudit(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Certified Audit" else "Certifikovaný Audit"
    fun topBarHelp(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Help & Guide" else "Nápověda & Metodika"
    fun topBarUserMode(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "User Mode" else "Uživatelský Mód"
    fun topBarAdminMode(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Admin Mode" else "Admin Mód"
    fun modeBeginner(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "🌱 Beginner" else "🌱 Začátečník"
    fun modeExpert(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "⚡ Expert" else "⚡ Expert"

    // ==========================================
    // REŽIM ZAČÁTEČNÍK (BEGINNER MODE)
    // ==========================================
    fun beginnerAdvisorTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Smart Advisor" else "Chytrý rádce"
    fun beginnerGoalsTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "My Goals" else "Moje cíle"
    fun beginnerOverviewTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Quick Overview" else "Jednoduchý přehled"
    fun beginnerGuideTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Beginner Guide" else "Průvodce začátečníka"
    fun beginnerSwitchToExpert(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Switch to Expert ⚡" else "Přepnout na Expert ⚡"
    fun beginnerSwitchToBeginner(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Switch to Beginner 🌱" else "Přepnout na Začátečník 🌱"

    // ==========================================
    // KOGNITIVNÍ CHAT
    // ==========================================
    fun chatInputPlaceholder(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Enter cognitive query, thesis or problem..." else "Zadejte kognitivní dotaz, tezi či problém..."
    fun chatSend(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Send" else "Odeslat"
    fun chatReasoningTrace(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "COGNITIVE REASONING TRACE" else "MYŠLENKOVÁ STOPA RÁDCE"
    fun chatFollowUp(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "RECOMMENDED FOLLOW-UP QUESTIONS" else "DOPORUČENÉ NAVAZUJÍCÍ OTÁZKY"
    fun chatEmptyGreetingTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "O.M.N.I.S. Cognitive System" else "Kognitivní Systém O.M.N.I.S."
    fun chatEmptyGreetingSubtitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "8D Transdisciplinary Multi-Agent Cognitive Core" else "8D Transdisciplinární Multi-Agentní Kognitivní Jádro"
    fun chatAttachImage(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Attach image / OCR" else "Připojit obrázek / OCR"
    fun chatAttachDoc(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Attach document" else "Připojit dokument"
    fun chatVoiceInput(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Voice input" else "Hlasový vstup"

    // ==========================================
    // PRODUKČNÍ AUDIT & ZÁTĚŽOVÝ BENCHMARK
    // ==========================================
    fun auditTabQuality(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "360° QUALITY AUDIT" else "360° AUDIT KVALITY"
    fun auditTabStress(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "STRESS BENCHMARK" else "ZÁTĚŽOVÝ BENCHMARK"
    fun auditReviewSystem(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "REVIEW SYSTEM" else "PŘEZKOUMAT SYSTÉM"
    fun auditPruneDb(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "PRUNE DB" else "PROŘEZAT DB"
    fun auditTotalChecks(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Total Checks" else "Testů celkem"
    fun auditPassed(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Passed" else "Úspěšných"
    fun auditFailed(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Failed / Notice" else "Kritických chyb"
    fun auditRamMemory(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "RAM Memory" else "Paměť RAM"

    // ==========================================
    // ZÁTĚŽOVÝ BENCHMARK
    // ==========================================
    fun benchStorageStatus(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "BIG DATA STORAGE STATUS" else "STAV VELKOOBJEMOVÉHO ÚLOŽIŠTĚ"
    fun benchTotalRecords(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Total Records" else "Záznamů celkem"
    fun benchSyntheticData(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Synthetic Benchmark" else "Syntetických dat"
    fun benchFreeHeap(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Free Heap RAM" else "Volná Heap RAM"
    fun benchGeneratorTitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "SYNTHETIC DATA GENERATOR" else "GENERÁTOR SYNTETICKÝCH DAT"
    fun benchGeneratorSubtitle(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Generates messages with 8D vectors, cognitive traces, and realistic domains." else "Generuje zprávy s 8D vektory, myšlenkovými stopami a různými doménami."
    fun benchTestQueries(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "QUERY BENCHMARK" else "TEST DOTAZŮ"
    fun benchPurge(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "PURGE BENCHMARK" else "VYČISTIT BENCHMARK"
    fun benchGenerating(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "GENERATING DATA LOAD..." else "GENEROVÁNÍ DATOVÉ ZÁTĚŽE..."
    fun benchSpeed(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Speed" else "Rychlost"
    fun benchCancel(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "CANCEL" else "PŘERUŠIT"

    // ==========================================
    // KOKPIT & CÍLE
    // ==========================================
    fun cockpitActiveGoals(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Active Autonomous Goals" else "Aktivní Autonomní Cíle"
    fun cockpitRecentArtifacts(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Recent Knowledge Artifacts" else "Nedávné Znalostní Artefakty"
    fun cockpitSystemHealth(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "System Integrity & Health" else "Integrita a Stav Systému"
    fun goalsCreateNew(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Create New Goal" else "Vytvořit Nový Cíl"

    // ==========================================
    // JAZYKOVÝ PŘEPÍNAČ
    // ==========================================
    fun languageLabel(lang: AppLanguage = currentLang) =
        if (lang == AppLanguage.EN) "Language" else "Jazyk"
}
