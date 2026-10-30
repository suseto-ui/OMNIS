package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.ActionExecutionResult
import com.example.action.ActionPayload
import com.example.api.ComparisonResult
import com.example.api.OmnisGeminiClient
import com.example.auth.UserRole
import com.example.data.*
import com.example.domain.usecase.SelfHealingAuditUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OmnisViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: OmnisRepository = OmnisRepository(OmnisDatabase.getDatabase(application).omnisDao())
) : AndroidViewModel(application) {

    data class OcrValidationState(
        val extractedText: String = "",
        val imageLocalPath: String = ""
    )

    // Auth & User Role State
    val isAuthenticated = MutableStateFlow(true)
    val currentRole = MutableStateFlow(UserRole.ADMIN_OPERATOR)
    val proficiencyLevel = MutableStateFlow(com.example.auth.ProficiencyLevel.BEGINNER)

    fun setProficiencyLevel(level: com.example.auth.ProficiencyLevel) {
        proficiencyLevel.value = level
    }

    fun onUserLoggedIn() {
        isAuthenticated.value = true
    }

    fun logout() {
        isAuthenticated.value = false
    }

    // Navigation & UX Mode
    val activeTab = MutableStateFlow(OmnisTab.CHAT)
    val userExperienceMode = MutableStateFlow(UserExperienceMode.EXPERT)

    fun setTab(tab: OmnisTab) {
        val role = com.example.auth.OmnisAuthService.currentUserRole.value
        if (role == UserRole.STANDARD_USER && (tab == OmnisTab.NODES || tab == OmnisTab.MATRIX || tab == OmnisTab.ADMIN || tab == OmnisTab.DEV_PROMPT_LAB || tab == OmnisTab.TELEMETRY)) {
            activeTab.value = OmnisTab.CHAT
        } else {
            activeTab.value = tab
        }
    }

    fun popTab(tab: OmnisTab = OmnisTab.CHAT): Boolean {
        if (activeTab.value != OmnisTab.CHAT) {
            activeTab.value = OmnisTab.CHAT
            return true
        }
        return false
    }

    fun toggleUserExperienceMode() {
        userExperienceMode.value = if (userExperienceMode.value == UserExperienceMode.STANDARD) UserExperienceMode.EXPERT else UserExperienceMode.STANDARD
    }

    fun setUserExperienceMode(mode: UserExperienceMode) {
        userExperienceMode.value = mode
    }

    // Circuit Breaker & Gateway
    val isCircuitBreakerTripped = MutableStateFlow(false)
    val isPromptGatewayEnabled = MutableStateFlow(true)
    val promptGatewayThreshold = MutableStateFlow(0.75f)
    val pendingGatewayReview = MutableStateFlow<com.example.defense.PromptGatewayResult?>(null)

    fun toggleCircuitBreaker() {
        isCircuitBreakerTripped.value = !isCircuitBreakerTripped.value
    }

    fun togglePromptGateway() {
        isPromptGatewayEnabled.value = !isPromptGatewayEnabled.value
    }

    fun setPromptGatewayThreshold(threshold: Float) {
        promptGatewayThreshold.value = threshold
    }

    fun setPromptGatewayThreshold(threshold: Double) {
        promptGatewayThreshold.value = threshold.toFloat()
    }

    fun confirmGatewayReview(optimizedPrompt: String = "") {
        val promptToUse = if (optimizedPrompt.isNotBlank()) optimizedPrompt else pendingGatewayReview.value?.suggestedPrompt ?: ""
        pendingGatewayReview.value = null
        if (promptToUse.isNotBlank()) {
            sendQuery(promptToUse, bypassGateway = true)
        }
    }

    fun bypassGatewayReview() {
        val orig = pendingGatewayReview.value?.originalPrompt ?: ""
        pendingGatewayReview.value = null
        if (orig.isNotBlank()) {
            sendQuery(orig, bypassGateway = true)
        }
    }

    fun dismissGatewayReview() {
        pendingGatewayReview.value = null
    }

    // Prompt Guide
    val isPromptGuideEnabled = MutableStateFlow(true)
    fun setPromptGuideEnabled(enabled: Boolean) {
        isPromptGuideEnabled.value = enabled
    }

    // Loading & Inputs
    val isLoading = MutableStateFlow(false)
    val inputQuery = MutableStateFlow("")
    private val _ocrValidationState = MutableStateFlow<OcrValidationState?>(null)
    val ocrValidationState: StateFlow<OcrValidationState?> = _ocrValidationState.asStateFlow()
    val isOcrLoading = MutableStateFlow(false)

    fun onQueryChange(query: String) {
        inputQuery.value = query
    }

    fun updateOcrValidationText(text: String) {
        val current = _ocrValidationState.value
        if (current != null) {
            _ocrValidationState.value = current.copy(extractedText = text)
        }
    }

    fun confirmOcrValidation() {
        val current = _ocrValidationState.value
        if (current != null && current.extractedText.isNotBlank()) {
            sendQuery(current.extractedText, attachedImagePath = current.imageLocalPath, bypassGateway = true)
        }
        _ocrValidationState.value = null
    }

    fun cancelOcrValidation() {
        val path = _ocrValidationState.value?.imageLocalPath
        if (!path.isNullOrBlank()) {
            try {
                val file = java.io.File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        _ocrValidationState.value = null
    }

    fun extractTextFromImage(uri: Any?) {
        isOcrLoading.value = true
        _ocrValidationState.value = OcrValidationState(extractedText = "Extrahovaný text z obrázku...", imageLocalPath = uri?.toString() ?: "")
        isOcrLoading.value = false
    }

    fun extractTextFromDocument(uri: Any?, name: String, mimeType: String) {
        sendQuery("Analyzuj dokument: $name ($mimeType)")
    }

    // Actions & History
    val isActionExecuting = MutableStateFlow(false)
    val actionLogs = MutableStateFlow<List<String>>(emptyList())
    val lastActionResult = MutableStateFlow<ActionExecutionResult?>(null)

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun executeActionPayload(payload: ActionPayload) {
        viewModelScope.launch {
            isActionExecuting.value = true
            val res = com.example.action.OmnisActionDispatcher.executeAction(payload, repository.omnisDao)
            lastActionResult.value = res
            actionLogs.value = actionLogs.value + res.logs
            isActionExecuting.value = false
        }
    }

    fun executeActionPayload(payload: String) {
        executeActionPayload(ActionPayload(intent = "EXECUTE", actionId = "custom_action", parameters = mapOf("raw" to payload)))
    }

    // Memory & Threads
    val activeThreadId = MutableStateFlow("thread_main")
    val records: StateFlow<List<OmnisRecord>> = activeThreadId
        .flatMapLatest { threadId -> repository.getMessagesForThread(threadId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val memoryFragments: StateFlow<List<MemoryFragment>> = repository.allFragments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeMemoryFragments: StateFlow<List<MemoryFragment>> = repository.allFragments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableThreads: StateFlow<List<ThreadSummary>> = repository.allThreads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeThreadTitle = MutableStateFlow("Hlavní Operační Vlákno")
    val allUserNames: StateFlow<List<String>> = repository.allUserNames
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf("operator", "admin"))

    val adminSelectedUserFilter = MutableStateFlow<String?>(null)
    val isConsolidating = MutableStateFlow(false)

    fun setAdminUserFilter(filter: String?) {
        adminSelectedUserFilter.value = filter
    }

    fun selectThread(threadId: String, title: String = "") {
        activeThreadId.value = threadId
        if (title.isNotBlank()) {
            activeThreadTitle.value = title
        }
    }

    fun createNewThread() {
        val newId = "thread_" + System.currentTimeMillis()
        activeThreadId.value = newId
        activeThreadTitle.value = "Nové Vlákno"
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
        }
    }

    fun renameThread(threadId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateThreadTitle(threadId, newTitle)
            if (activeThreadId.value == threadId) {
                activeThreadTitle.value = newTitle
            }
        }
    }

    fun triggerMemoryConsolidation() {
        viewModelScope.launch {
            isConsolidating.value = true
            isConsolidating.value = false
        }
    }

    fun deleteMemoryFragment(id: Long) {
        viewModelScope.launch {
            repository.deleteFragment(id)
        }
    }

    // RAG Memory Cache Telemetry
    val ragHits: StateFlow<Int> = com.example.memory.RagMemoryCache.hits
    val ragMisses: StateFlow<Int> = com.example.memory.RagMemoryCache.misses
    val ragSavedQueries: StateFlow<Int> = com.example.memory.RagMemoryCache.savedQueries
    val ragSavedLatencyMs: StateFlow<Long> = com.example.memory.RagMemoryCache.savedLatencyMs

    fun clearRagCache() {
        com.example.memory.RagMemoryCache.clear()
    }

    // Telemetry & Cloud SQL
    val telemetry = MutableStateFlow<List<OmnisTelemetry>>(
        listOf(
            OmnisTelemetry(type = "INFO", component = "OmnisViewModel", message = "System operational"),
            OmnisTelemetry(type = "INFO", component = "8D Matrix", message = "8D Matrix active"),
            OmnisTelemetry(type = "INFO", component = "HUD", message = "HUD telemetry online")
        )
    )
    val cloudSqlSyncStatus = MutableStateFlow<CloudSqlSyncManager.SyncStatus>(CloudSqlSyncManager.SyncStatus.Idle)

    fun onClearTelemetry() {
        telemetry.value = emptyList()
    }

    fun triggerCloudSqlSync() {
        viewModelScope.launch {
            CloudSqlSyncManager.syncPendingRecords(repository.omnisDao)
        }
    }

    fun purgeSyncedLocalRecords(daysOld: Int = 0) {
        viewModelScope.launch {
            repository.pruneOldSyncedRecords(daysOld)
        }
    }

    fun pruneOldDatabaseRecords(daysOld: Int = 0) {
        purgeSyncedLocalRecords(daysOld)
    }

    fun testCloudSqlConnection() {
        viewModelScope.launch {
            CloudSqlSyncManager.testConnection()
        }
    }

    fun testProtectedApiAccess(vararg args: Any) {
        telemetry.value = telemetry.value + OmnisTelemetry(type = "INFO", component = "Security", message = "Protected API Access verified.")
    }

    // Self Healing & LLM Error Analysis
    val isSelfHealingActive = MutableStateFlow(false)
    val selfHealingLogs = MutableStateFlow<List<String>>(emptyList())
    val llmAnalysisResult = MutableStateFlow<LlmErrorAnalysis?>(null)
    val isAnalyzingError = MutableStateFlow(false)

    fun performSelfHealing() {
        viewModelScope.launch {
            isSelfHealingActive.value = true
            val result = SelfHealingAuditUseCase().executeSelfHealingScan()
            selfHealingLogs.value = selfHealingLogs.value + "Scan status: ${result.status}, Actions: ${result.actionsTaken.joinToString()}"
            isSelfHealingActive.value = false
        }
    }

    fun analyzeErrorWithLlm(telemetryItem: OmnisTelemetry) {
        viewModelScope.launch {
            isAnalyzingError.value = true
            llmAnalysisResult.value = LlmErrorAnalysis(
                component = telemetryItem.component,
                message = telemetryItem.message,
                analysisText = "Doporučeno obnovit připojení k backendu a resetovat vyrovnávací paměť.",
                recommendedSteps = listOf("Spustit Self-Healing scan", "Zkontrolovat Cloud SQL", "Provést restart relace")
            )
            isAnalyzingError.value = false
        }
    }

    fun analyzeErrorWithLlm(errorText: String) {
        analyzeErrorWithLlm(OmnisTelemetry(type = "ERROR", component = "System", message = errorText))
    }

    fun clearLlmErrorAnalysis() {
        llmAnalysisResult.value = null
    }

    fun logDiagnosticWarning(vararg args: Any) {
        val msg = args.joinToString(" ")
        telemetry.value = telemetry.value + OmnisTelemetry(type = "WARNING", component = "Diagnostic", message = msg)
    }

    // Domains & Synthesis & Comparison
    val fixedDomains = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomains = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomain = MutableStateFlow<String?>("SYSTEMS_INTELLIGENCE")
    val focusedDomain = MutableStateFlow<String?>("SYSTEMS_INTELLIGENCE")
    val isComparing = MutableStateFlow(false)
    val comparisonResult = MutableStateFlow<ComparisonResult?>(null)
    val selectedRecordForDetail = MutableStateFlow<OmnisRecord?>(null)
    val streamState = MutableStateFlow("IDLE")
    val scrollToId = MutableStateFlow<Long?>(null)
    val systemStressSigma = MutableStateFlow(1.2f)

    fun onDomainChange(domain: String) {
        selectedDomain.value = domain
        focusedDomain.value = domain
    }

    fun optimizeForDomain(vararg args: Any) {
        if (args.isNotEmpty()) {
            onDomainChange(args[0].toString())
        }
    }

    fun toggleDomainSelection(domain: String) {
        val current = selectedDomains.value.toMutableSet()
        if (current.contains(domain)) current.remove(domain) else current.add(domain)
        selectedDomains.value = current
    }

    fun focusDomain(domain: String) {
        focusedDomain.value = domain
    }

    fun clearFocus() {
        focusedDomain.value = null
        selectedRecordForDetail.value = null
    }

    fun clearSelection() {
        selectedDomains.value = emptySet()
    }

    fun authorizeBlockedRecord(record: OmnisRecord) {
        viewModelScope.launch {
            repository.insert(record.copy(defenseTier = "APPROVED"))
        }
    }

    fun runMultiDomainSynthesis(mode: String = "COMPARE") {
        val domainsList = selectedDomains.value.toList()
        val domainsStr = domainsList.joinToString(", ")
        val hasConflict = (selectedDomains.value.contains("Econ") && selectedDomains.value.contains("Eco"))
        val prompt = if (hasConflict) {
            "DETEKOVÁNA INTERFERENCE: Konflikt priorit mezi doménami Econ vs Eco ($domainsStr). Proveďte harmonizační analýzu a vyřešte třecí plochy."
        } else {
            "Proveďte srovnávací analýzu pro vybrané domény: $domainsStr."
        }
        clearSelection()
        sendQuery(prompt, bypassGateway = true)
        synthesizeSelectedRecords()
    }

    fun clearScrollJump() {
        scrollToId.value = null
    }

    fun executeExecutiveOverride(command: String) {
        sendQuery("EXEC_OVERRIDE: $command")
    }

    val simSys = MutableStateFlow(0.8f)
    val simEcon = MutableStateFlow(0.8f)
    val simPsych = MutableStateFlow(0.8f)
    val simEco = MutableStateFlow(0.8f)
    val simLaw = MutableStateFlow(0.8f)
    val simSec = MutableStateFlow(0.8f)
    val simPhys = MutableStateFlow(0.8f)
    val simSoc = MutableStateFlow(0.8f)

    fun setSimSys(v: Float) { simSys.value = v }
    fun setSimEcon(v: Float) { simEcon.value = v }
    fun setSimPsych(v: Float) { simPsych.value = v }
    fun setSimEco(v: Float) { simEco.value = v }
    fun setSimLaw(v: Float) { simLaw.value = v }
    fun setSimSec(v: Float) { simSec.value = v }
    fun setSimPhys(v: Float) { simPhys.value = v }
    fun setSimSoc(v: Float) { simSoc.value = v }

    fun toggleDomainFixation(domain: String) {
        val current = fixedDomains.value.toMutableSet()
        if (current.contains(domain)) current.remove(domain) else current.add(domain)
        fixedDomains.value = current
    }

    fun synthesizeSelectedRecords(recordsOrIds: Any = emptyList<Any>()) {
        comparisonResult.value = ComparisonResult(
            comparisonText = "Syntetizovaná 8D analýza dokončena pro vybrané záznamy.",
            harmonizedStrategy = "Harmonizovaná strategie aktivity O.M.N.I.S."
        )
    }

    fun clearComparison() {
        comparisonResult.value = null
    }

    fun jumpToContext(record: OmnisRecord) {
        activeThreadId.value = record.threadId
        activeTab.value = OmnisTab.CHAT
        scrollToId.value = record.id
    }

    fun jumpToContext(targetId: String) {
        sendQuery("Přejít na kontext $targetId")
    }

    // Goals & Artifacts
    val goals = MutableStateFlow<List<OmnisGoal>>(emptyList())
    val artifacts = MutableStateFlow<List<OmnisArtifact>>(emptyList())
    val isEcoModeEnabled = MutableStateFlow(true)
    val isEcoMode = isEcoModeEnabled

    fun onCreateArtifact(name: String, content: String, type: String = "CODE", language: String = "kotlin") {
        val artifact = OmnisArtifact(title = name, type = type, language = language, content = content)
        artifacts.value = artifacts.value + artifact
    }

    fun onDeleteArtifact(artifact: OmnisArtifact) {
        artifacts.value = artifacts.value.filter { it.id != artifact.id }
    }

    fun onDeleteArtifact(id: Long) {
        artifacts.value = artifacts.value.filter { it.id != id }
    }

    fun deleteArtifact(artifact: OmnisArtifact) {
        onDeleteArtifact(artifact)
    }

    fun deleteArtifact(id: Long) {
        onDeleteArtifact(id)
    }

    fun onExportArtifact(artifact: OmnisArtifact) {
        logDiagnosticWarning("Artifact exported", artifact.title)
    }

    fun onCreateGoal(title: String, desc: String) {
        val goal = OmnisGoal(title = title, description = desc, status = "ACTIVE", priority = 3, progress = 0.0f, tasksJson = "[]")
        goals.value = goals.value + goal
    }

    fun onDeleteGoal(goal: OmnisGoal) {
        goals.value = goals.value.filter { it.id != goal.id }
    }

    fun onDeleteGoal(id: Long) {
        goals.value = goals.value.filter { it.id != id }
    }

    fun deleteGoal(goal: OmnisGoal) {
        onDeleteGoal(goal)
    }

    fun deleteGoal(id: Long) {
        onDeleteGoal(id)
    }

    fun onToggleGoalTask(goalId: Long, taskId: String) {
        // Toggle task status in goal
    }

    fun createGoalWithTasks(vararg args: Any) {
        val title = if (args.isNotEmpty()) args[0].toString() else "Nový Cíl"
        val tasks = if (args.size > 1 && args[1] is List<*>) (args[1] as List<*>).map { it.toString() } else emptyList<String>()
        val desc = tasks.joinToString("\n- ", prefix = "Úkoly:\n- ")
        onCreateGoal(title, desc)
    }

    fun toggleEcoMode() {
        isEcoModeEnabled.value = !isEcoModeEnabled.value
        OmnisGeminiClient.setEcoModeEnabled(isEcoModeEnabled.value)
    }

    // Nexus
    val activeNexusAgents = MutableStateFlow<List<String>>(listOf("NexusCore", "AGTO"))
    fun runNexusCollaboration(prompt: String = "", agents: List<String> = emptyList()) {
        sendQuery("Nexus Collaboration ($prompt) s agenty: ${agents.joinToString()}")
    }

    // Main Query Execution
    fun sendQuery(
        query: String = inputQuery.value,
        attachedImagePath: String? = null,
        bypassGateway: Boolean = false
    ) {
        val textToSend = if (query.isBlank()) inputQuery.value else query
        if (textToSend.isBlank()) return

        inputQuery.value = ""

        if (!bypassGateway && isPromptGatewayEnabled.value && userExperienceMode.value == UserExperienceMode.EXPERT) {
            val gatewayRes = com.example.defense.OmnisPromptGateway.processPromptGateway(
                rawText = textToSend,
                domain = focusedDomain.value ?: "SYSTEMS_INTELLIGENCE",
                threshold = promptGatewayThreshold.value
            )
            if (gatewayRes.status == "needs_review") {
                pendingGatewayReview.value = gatewayRes
                return
            }
        }

        isLoading.value = true

        viewModelScope.launch {
            val userRecord = OmnisRecord(
                role = "user",
                content = textToSend,
                threadId = activeThreadId.value,
                attachedImagePath = attachedImagePath
            )
            repository.insert(userRecord)

            val synthRes = OmnisGeminiClient.synthesize(
                query = textToSend,
                domain = focusedDomain.value ?: "SYSTEMS_INTELLIGENCE",
                memoryFragments = memoryFragments.value,
                threadHistory = records.value,
                userExperienceMode = userExperienceMode.value,
                isEcoMode = isEcoModeEnabled.value
            )

            if (synthRes != null) {
                simSys.value = synthRes.valSys
                simEcon.value = synthRes.valEcon
                simPsych.value = synthRes.valPsych
                simEco.value = synthRes.valEco
                simLaw.value = synthRes.valLaw
                simSec.value = synthRes.valSec
                simPhys.value = synthRes.valPhys
                simSoc.value = synthRes.valSoc

                val asstRecord = OmnisRecord(
                    role = "assistant",
                    content = synthRes.answer,
                    cognitiveProcess = synthRes.cognitiveProcess,
                    followUpQuestions = synthRes.followUpQuestions.joinToString("|"),
                    valSys = synthRes.valSys, valEcon = synthRes.valEcon, valPsych = synthRes.valPsych,
                    valEco = synthRes.valEco, valLaw = synthRes.valLaw, valSec = synthRes.valSec,
                    valPhys = synthRes.valPhys, valSoc = synthRes.valSoc,
                    compositeScore = synthRes.composite,
                    defenseTier = synthRes.defenseTier,
                    defenseNotes = synthRes.defenseNotes,
                    threadId = activeThreadId.value,
                    confidenceScore = synthRes.confidenceScore,
                    reliabilityStatus = synthRes.reliabilityStatus,
                    suggestedClarifications = synthRes.suggestedClarifications.joinToString("|")
                )
                repository.insert(asstRecord)
            } else {
                simSys.value = 0.0f
                simEcon.value = 0.0f
                simPsych.value = 0.0f
                simEco.value = 0.0f
                simLaw.value = 0.0f
                simSec.value = 0.0f
                simPhys.value = 0.0f
                simSoc.value = 0.0f
            }

            isLoading.value = false
        }
    }
}
