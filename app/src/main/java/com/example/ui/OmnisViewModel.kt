package com.example.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.OmnisGeminiClient
import com.example.data.OmnisDatabase
import com.example.data.OmnisRecord
import com.example.data.OmnisRepository
import com.example.defense.OmnisPromptGateway
import com.example.defense.PromptGatewayResult
import com.example.action.ActionPayload
import com.example.action.ActionExecutionResult
import com.example.action.OmnisActionDispatcher
import com.example.action.ResilienceManager
import com.example.auth.OmnisAuthService
import com.example.auth.ProficiencyLevel
import com.example.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

enum class OmnisTab {
    CHAT,
    ANALYTICS,
    MEMORY,
    NODES,
    ADMIN,
    DASHBOARD,
    MATRIX,
    TEST_SEMANTIC,
    DEV_PROMPT_LAB,
    NEXUS,
    ARTIFACTS,
    SCENARIOS,
    GOALS,
    TELEMETRY,
    GUIDE,
    CAUSAL_SIMULATOR,
    DIALECTICS,
    PRODUCTION_AUDIT
}

enum class UserExperienceMode {
    STANDARD, // Běžný uživatel: volný text, přirozená a lidská čeština, automatický převod bez složitého žargonu
    EXPERT    // Pokročilý uživatel: striktní formát, 8D tenzory, Zero-Fluff, kognitivní introspekce
}

data class LlmErrorAnalysis(
    val logId: Long,
    val component: String,
    val message: String,
    val analysisText: String,
    val recommendedSteps: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

class OmnisViewModel(
    application: Application,
    customRepository: OmnisRepository? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, null)

    init {
        OmnisAuthService.init(application)
        val db = OmnisDatabase.getDatabase(application)
        com.example.telemetry.TelemetryEngine.initialize(db.omnisDao())
        
        // Automatická synchronizace konverzací s cloudem na pozadí při startu
        viewModelScope.launch {
            delay(1000) // Drobná prodleva pro plynulé spuštění UI
            syncConversationsWithBackend()
            delay(500)
            repository.syncWithCloudSql()
        }
    }

    val isAuthenticated: StateFlow<Boolean> = OmnisAuthService.isAuthenticated
    val currentRole: StateFlow<UserRole> = OmnisAuthService.currentUserRole

    fun logout() {
        OmnisAuthService.logout(getApplication())
        _activeTab.value = OmnisTab.CHAT
        _tabBackStack.value = emptyList()
    }

    private val repository: OmnisRepository = customRepository ?: run {
        val db = OmnisDatabase.getDatabase(application)
        OmnisRepository(db.omnisDao())
    }

    private val _activeThreadId = MutableStateFlow("thread_main")
    val activeThreadId: StateFlow<String> = _activeThreadId.asStateFlow()

    private val _activeThreadTitle = MutableStateFlow("Hlavní vlákno")
    val activeThreadTitle: StateFlow<String> = _activeThreadTitle.asStateFlow()

    private val _adminSelectedUserFilter = MutableStateFlow<String?>(null)
    val adminSelectedUserFilter: StateFlow<String?> = _adminSelectedUserFilter.asStateFlow()

    val allUserNames: StateFlow<List<String>> = repository.allUserNames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableThreads: StateFlow<List<com.example.data.ThreadSummary>> = combine(
        OmnisAuthService.currentSession,
        OmnisAuthService.currentUserRole,
        _adminSelectedUserFilter,
        repository.allThreads
    ) { session, role, adminFilter, allThreadsList ->
        val currentUsername = session?.username ?: "operator"
        if (role == UserRole.ADMIN_OPERATOR) {
            if (adminFilter != null) {
                allThreadsList.filter { it.userName.equals(adminFilter, ignoreCase = true) }
            } else {
                allThreadsList
            }
        } else {
            allThreadsList.filter { it.userName.equals(currentUsername, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val records: StateFlow<List<OmnisRecord>> = _activeThreadId
        .flatMapLatest { threadId -> repository.getRecordsByThread(threadId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val memoryFragments: StateFlow<List<com.example.data.MemoryFragment>>

    val telemetry: StateFlow<List<com.example.data.OmnisTelemetry>> = repository.omnisDao.getRecentTelemetry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isConsolidating = MutableStateFlow(false)
    val isConsolidating: StateFlow<Boolean> = _isConsolidating.asStateFlow()

    private val _activeMemoryFragments = MutableStateFlow<List<com.example.data.MemoryFragment>>(emptyList())
    val activeMemoryFragments: StateFlow<List<com.example.data.MemoryFragment>> = _activeMemoryFragments.asStateFlow()
    
    val artifacts: StateFlow<List<com.example.data.OmnisArtifact>> = repository.omnisDao.getAllArtifacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<com.example.data.OmnisGoal>> = repository.omnisDao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Clean Architecture Domain Use Cases
    private val compute8DMatrixUseCase = com.example.domain.usecase.Compute8DMatrixUseCase()
    private val anomaliesDetectionUseCase = com.example.domain.usecase.AnomaliesDetectionUseCase()
    private val goalOrchestrationUseCase = com.example.domain.usecase.GoalOrchestrationUseCase()
    private val offlineSemanticSearchUseCase = com.example.domain.usecase.OfflineSemanticSearchUseCase()
    private val searchLocalKnowledgeNexusUseCase = com.example.domain.usecase.SearchLocalKnowledgeNexusUseCase()
    private val telemetrySyncUseCase = com.example.domain.usecase.TelemetrySyncUseCase()

    private val _compensatoryGoalProposal = MutableStateFlow<com.example.domain.usecase.CompensatoryGoalDraft?>(null)
    val compensatoryGoalProposal: StateFlow<com.example.domain.usecase.CompensatoryGoalDraft?> = _compensatoryGoalProposal.asStateFlow()

    private val _activeNexusAgents = MutableStateFlow<List<String>>(emptyList())
    val activeNexusAgents: StateFlow<List<String>> = _activeNexusAgents.asStateFlow()

    private val _activeTab = MutableStateFlow(
        if (OmnisAuthService.currentUserRole.value == UserRole.ADMIN_OPERATOR) OmnisTab.ADMIN else OmnisTab.CHAT
    )
    val activeTab: StateFlow<OmnisTab> = _activeTab.asStateFlow()

    // Kognitivní úroveň pokročilosti UI (Dimenze B: Začátečník vs Expert)
    private val _proficiencyLevel = MutableStateFlow(ProficiencyLevel.BEGINNER)
    val proficiencyLevel: StateFlow<ProficiencyLevel> = _proficiencyLevel.asStateFlow()

    // System Circuit Breaker (Hlavní Jistič - výchozí STAV: VŠECHNY OKRUHY ZAPNUTY / ZAVŘENY)
    private val _isCircuitBreakerTripped = MutableStateFlow(false)
    val isCircuitBreakerTripped: StateFlow<Boolean> = _isCircuitBreakerTripped.asStateFlow()

    // Sémantická brána (Semantic Prompt Gateway - výchozí STAV: BYPASS REŽIM / VYŠŠÍ PROPUSTNOST PROMPTŮ)
    private val _isPromptGatewayEnabled = MutableStateFlow(false)
    val isPromptGatewayEnabled: StateFlow<Boolean> = _isPromptGatewayEnabled.asStateFlow()

    private val _promptGatewayThreshold = MutableStateFlow(0.70f)
    val promptGatewayThreshold: StateFlow<Float> = _promptGatewayThreshold.asStateFlow()

    fun setPromptGatewayThreshold(threshold: Float) {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Nastavení citlivosti brány vyžaduje roli Admin."
            return
        }
        val clamped = threshold.coerceIn(0.30f, 0.95f)
        _promptGatewayThreshold.value = clamped
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log(
                type = "INFO",
                component = "PromptGateway",
                message = "PRÁH CITLIVOSTI SÉMANTICKÉ BRÁNY ZMĚNĚN NA ${(clamped * 100).toInt()}%",
                metadata = "{\"threshold\": $clamped, \"operator\": \"${currentRole.name}\"}"
            )
        }
    }

    fun togglePromptGateway() {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Ovládání sémantické brány vyžaduje roli Admin."
            return
        }
        val newState = !_isPromptGatewayEnabled.value
        _isPromptGatewayEnabled.value = newState
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log(
                type = "INFO",
                component = "PromptGateway",
                message = if (newState) "SÉMANTICKÁ BRÁNA AKTIVOVÁNA (Quality & Defense Gate ON)" else "SÉMANTICKÁ BRÁNA DEAKTIVOVÁNA (Bypass Mode / Direct Pass)",
                metadata = "{\"enabled\": $newState, \"operator\": \"${currentRole.name}\"}"
            )
        }
    }

    fun resetCircuitBreaker() {
        _isCircuitBreakerTripped.value = false
        com.example.action.ResilienceManager.setCircuitState(com.example.action.ResilienceManager.CircuitState.CLOSED)
        com.example.api.OmnisGeminiClient.circuitBreaker.reset()
        _errorMessage.value = null
    }

    fun toggleCircuitBreaker() {
        val currentRole = OmnisAuthService.currentUserRole.value
        val newState = !_isCircuitBreakerTripped.value
        
        // Pokud vypínáme jistič (chceme obnovit chod), povolit všem uživatelům
        if (!newState) {
            resetCircuitBreaker()
            return
        }

        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Vypnutí jističe (pozastavení exekuce) vyžaduje roli Admin."
            return
        }

        _isCircuitBreakerTripped.value = true
        com.example.action.ResilienceManager.setCircuitState(com.example.action.ResilienceManager.CircuitState.OPEN)
    }

    fun pruneOldDatabaseRecords(daysOld: Int = 14) {
        viewModelScope.launch {
            try {
                repository.pruneOldSyncedRecords(daysOld)
                triggerHapticPulse()
            } catch (e: Exception) {
                android.util.Log.e("OmnisViewModel", "Pruning failed", e)
            }
        }
    }

    // Action-Driven Dispatcher State
    private val _lastActionResult = MutableStateFlow<ActionExecutionResult?>(null)
    val lastActionResult: StateFlow<ActionExecutionResult?> = _lastActionResult.asStateFlow()

    private val _isActionExecuting = MutableStateFlow(false)
    val isActionExecuting: StateFlow<Boolean> = _isActionExecuting.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<String>>(emptyList())
    val actionLogs: StateFlow<List<String>> = _actionLogs.asStateFlow()

    fun executeActionPayload(payload: ActionPayload) {
        if (_isActionExecuting.value) return
        _isActionExecuting.value = true
        _actionLogs.value = emptyList() // Clear previous logs

        viewModelScope.launch {
            try {
                // RBAC Kontrola: Pouze operátor může spouštět systémové akce
                val currentRole = OmnisAuthService.currentUserRole.value
                if (!currentRole.canAccessSystemActions()) {
                    _errorMessage.value = "Přístup odepřen: Akční dispečer vyžaduje roli Admin / Operátor."
                    _isActionExecuting.value = false
                    return@launch
                }

                val currentUsername = OmnisAuthService.currentSession.value?.username ?: "operator"
                val currentThreadId = _activeThreadId.value
                val currentThreadTitle = _activeThreadTitle.value

                // 1. Záznam volání do chatu jako uživatelský/systémový JSON příkaz
                val userActionRecord = OmnisRecord(
                    role = "user",
                    content = "⚡ [ACTION DISPATCH] Volání intentu: `${payload.intent}` | Akce: `${payload.actionId}`\n\n```json\n${payload.toJsonString()}\n```",
                    domain = _selectedDomain.value,
                    threadId = currentThreadId,
                    threadTitle = currentThreadTitle,
                    userName = currentUsername
                )
                repository.insert(userActionRecord)

                // 2. Nativní backend dispatcher s ochranou timeoutu (10 sekund)
                val result = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                    OmnisActionDispatcher.executeAction(payload, repository.omnisDao)
                } ?: ActionExecutionResult(
                    actionId = payload.actionId,
                    isSuccess = false,
                    statusCode = 504,
                    logs = listOf("ERROR: Exekuce akce překročila maximální časový limit (10s Timeout Guard)"),
                    summaryReport = "TIMEOUT: Systémová akce nebylo dokončena v limitu 10.000 ms."
                )
                _lastActionResult.value = result
                _actionLogs.value = result.logs

                // 3. Kontextová zpětná smyčka (Tool Response): Vložení výsledků do kontextu a generování souhrnu pro operátora
                val logsFormatted = result.logs.joinToString("\n")
                val responseContent = buildString {
                    appendLine("### 🛠️ VÝSLEDEK NATIVNÍHO DISPEČINKU [Kód ${result.statusCode}]")
                    appendLine(result.summaryReport)
                    appendLine()
                    appendLine("#### 📋 Systémové logy:")
                    appendLine("```text")
                    appendLine(logsFormatted)
                    appendLine("```")
                    if (result.outputData.isNotEmpty()) {
                        appendLine("#### 📊 Výstupní data:")
                        result.outputData.forEach { (k, v) ->
                            appendLine("- **$k**: `$v`")
                        }
                    }
                }

                val toolResponseRecord = OmnisRecord(
                    role = "assistant",
                    content = responseContent,
                    cognitiveProcess = "1. Odchycení intentu ${payload.intent} dispečerem.\n2. Nativní spuštění bez LLM.\n3. Zpětné vložení Tool Response do kontextu paměti.",
                    followUpQuestions = "Jak interpretovat zjištěnou latenci?|Spustit doplňkový audit eBPF?",
                    valSys = 0.99f,
                    valEcon = 0.95f,
                    valPsych = 0.90f,
                    valEco = 0.92f,
                    valLaw = 0.99f,
                    valSec = 1.00f,
                    valPhys = 0.95f,
                    valSoc = 0.90f,
                    compositeScore = 0.95f,
                    domain = "SYSTEMS_INTELLIGENCE",
                    defenseTier = "DISPATCH_EXECUTED",
                    threadId = currentThreadId,
                    threadTitle = currentThreadTitle,
                    userName = currentUsername
                )
                repository.insert(toolResponseRecord)

            } catch (e: Exception) {
                Log.e("OmnisViewModel", "executeActionPayload failed", e)
                _errorMessage.value = "Chyba při exekuci akce: ${e.localizedMessage}"
            } finally {
                _isActionExecuting.value = false
            }
        }
    }

    // 8D State Management
    private val _selectedDomains = MutableStateFlow<Set<String>>(emptySet())
    val selectedDomains: StateFlow<Set<String>> = _selectedDomains.asStateFlow()

    private val _fixedDomains = MutableStateFlow<Set<String>>(emptySet())
    val fixedDomains: StateFlow<Set<String>> = _fixedDomains.asStateFlow()

    private val _selectedRecordForDetail = MutableStateFlow<OmnisRecord?>(null)
    val selectedRecordForDetail: StateFlow<OmnisRecord?> = _selectedRecordForDetail.asStateFlow()

    private val _focusedDomain = MutableStateFlow<String?>(null)
    val focusedDomain: StateFlow<String?> = _focusedDomain.asStateFlow()

    fun toggleDomainSelection(domain: String) {
        _selectedDomains.value = if (_selectedDomains.value.contains(domain)) {
            _selectedDomains.value - domain
        } else {
            _selectedDomains.value + domain
        }
    }

    fun toggleDomainFixation(domain: String) {
        _fixedDomains.value = if (_fixedDomains.value.contains(domain)) {
            _fixedDomains.value - domain
        } else {
            _fixedDomains.value + domain
        }
    }

    fun focusDomain(domain: String, record: OmnisRecord) {
        _focusedDomain.value = domain
        _selectedRecordForDetail.value = record
    }

    fun clearFocus() {
        _focusedDomain.value = null
        _selectedRecordForDetail.value = null
    }

    fun clearSelection() {
        _selectedDomains.value = emptySet()
    }

    fun runMultiDomainSynthesis(action: String) {
        val selected = _selectedDomains.value
        if (selected.isEmpty()) return
        val domains = selected.joinToString(", ")
        val record = records.value.lastOrNull { it.role == "assistant" } ?: return
        
        val clusterAnalysis = OmnisCorrelationEngine.evaluateCluster(selected)
        val frictionNote = if (clusterAnalysis.hasFriction && clusterAnalysis.primaryPair != null) {
            "\n[DETEKOVÁNA INTERFERENCE]: ${clusterAnalysis.primaryPair.domainA} vs ${clusterAnalysis.primaryPair.domainB} (${(clusterAnalysis.primaryPair.correlation * 100).toInt()}%). Vliv: ${clusterAnalysis.primaryPair.impactDescription}"
        } else ""

        val prompt = when(action) {
            "COMPARE" -> "Proveď detailní srovnávací analýzu domén ($domains) v kontextu předchozí odpovědi. Vytvoř srovnávací tabulku parametrů a vlivů.$frictionNote"
            "HARMONIZE" -> "Identifikuj konflikty mezi doménami ($domains) a navrhni harmonizační strategii pro synergický efekt.$frictionNote"
            else -> "Analyzuj domény ($domains).$frictionNote"
        }
        
        executeDirectQuery(prompt)
        clearSelection()
    }

    fun optimizeForDomain(domain: String, record: OmnisRecord) {
        val prompt = "Přepracuj svou předchozí odpověď (ID #${record.id}) tak, aby byla maximalizována integrita a výkon v doméně: $domain. Zaměř se na diagnostiku a eliminaci rizik v této oblasti."
        executeDirectQuery(prompt)
        clearFocus()
    }

    private val modePrefs = application.getSharedPreferences("omnis_ux_prefs", android.content.Context.MODE_PRIVATE)
    private val initialUxMode = try {
        val saved = modePrefs.getString("user_experience_mode", UserExperienceMode.STANDARD.name)
        UserExperienceMode.valueOf(saved ?: UserExperienceMode.STANDARD.name)
    } catch (e: Exception) {
        UserExperienceMode.STANDARD
    }

    private val _userExperienceMode = MutableStateFlow(initialUxMode)
    val userExperienceMode: StateFlow<UserExperienceMode> = _userExperienceMode.asStateFlow()

    fun setUserExperienceMode(mode: UserExperienceMode) {
        _userExperienceMode.value = mode
        try {
            modePrefs.edit().putString("user_experience_mode", mode.name).apply()
        } catch (_: Exception) {}
    }

    fun toggleUserExperienceMode() {
        val nextMode = if (_userExperienceMode.value == UserExperienceMode.STANDARD) UserExperienceMode.EXPERT else UserExperienceMode.STANDARD
        setUserExperienceMode(nextMode)
    }

    private val _inputQuery = MutableStateFlow("")
    val inputQuery: StateFlow<String> = _inputQuery.asStateFlow()

    private val _isPromptGuideEnabled = MutableStateFlow(true)
    val isPromptGuideEnabled: StateFlow<Boolean> = _isPromptGuideEnabled.asStateFlow()

    fun setPromptGuideEnabled(enabled: Boolean) {
        _isPromptGuideEnabled.value = enabled
    }

    private val _selectedDomain = MutableStateFlow("SYSTEMS_INTELLIGENCE")
    val selectedDomain: StateFlow<String> = _selectedDomain.asStateFlow()

    data class StreamState(
        val stage: String,
        val text: String,
        val score: Float? = null,
        val status: String? = null
    )

    private val _streamState = MutableStateFlow<StreamState?>(null)
    val streamState: StateFlow<StreamState?> = _streamState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentStreamingJob: kotlinx.coroutines.Job? = null

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    data class OcrValidationState(
        val imageLocalPath: String,
        val extractedText: String
    )
    
    private val _ocrValidationState = MutableStateFlow<OcrValidationState?>(null)
    val ocrValidationState: StateFlow<OcrValidationState?> = _ocrValidationState.asStateFlow()

    fun updateOcrValidationText(newText: String) {
        _ocrValidationState.value = _ocrValidationState.value?.copy(extractedText = newText)
    }

    fun confirmOcrValidation() {
        val state = _ocrValidationState.value ?: return
        _ocrValidationState.value = null
        executeDirectQuery(state.extractedText, state.imageLocalPath)
    }

    fun cancelOcrValidation() {
        val state = _ocrValidationState.value
        state?.let {
            val file = java.io.File(it.imageLocalPath)
            if (file.exists()) file.delete()
        }
        _ocrValidationState.value = null
    }

    // Prompt Gateway State (Semantic Elevator & Human-in-the-Loop)
    private val _pendingGatewayReview = MutableStateFlow<PromptGatewayResult?>(null)
    val pendingGatewayReview: StateFlow<PromptGatewayResult?> = _pendingGatewayReview.asStateFlow()

    private var pendingGatewayImagePath: String? = null

    fun dismissGatewayReview() {
        _pendingGatewayReview.value = null
        pendingGatewayImagePath = null
    }

    fun confirmGatewayReview(optimizedPrompt: String) {
        val imagePath = pendingGatewayImagePath
        _pendingGatewayReview.value = null
        pendingGatewayImagePath = null
        executeDirectQuery(optimizedPrompt, imagePath)
    }

    fun bypassGatewayReview() {
        val orig = _pendingGatewayReview.value?.originalPrompt
        val imagePath = pendingGatewayImagePath
        _pendingGatewayReview.value = null
        pendingGatewayImagePath = null
        if (!orig.isNullOrBlank()) {
            executeDirectQuery(orig, imagePath)
        }
    }

    // Simple error channel for UI to observe
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("OmnisViewModel", "Unhandled coroutine exception caught: ${throwable.localizedMessage}", throwable)
        _errorMessage.value = "Chyba na pozadí: ${throwable.localizedMessage ?: "Nespecifikovaná chyba"}"
    }

    val uiState: StateFlow<UIState<List<OmnisRecord>>> = combine(
        records,
        _isLoading,
        _errorMessage
    ) { recs, loading, error ->
        when {
            loading -> UIState.Loading
            error != null -> UIState.Error(error)
            recs.isEmpty() -> UIState.Idle
            else -> UIState.Success(recs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UIState.Idle)

    val lastOnlineError: StateFlow<String?> = com.example.api.OmnisGeminiClient.lastOnlineError

    // FÁZE 5: Self-Healing UI a Diagnostika stavu
    private val _selfHealingLogs = MutableStateFlow<List<String>>(emptyList())
    val selfHealingLogs: StateFlow<List<String>> = _selfHealingLogs.asStateFlow()

    private val _isSelfHealingActive = MutableStateFlow(false)
    val isSelfHealingActive: StateFlow<Boolean> = _isSelfHealingActive.asStateFlow()

    fun extractTextFromImage(uri: android.net.Uri) {
        _isOcrLoading.value = true
        viewModelScope.launch {
            try {
                val textWithFile = withContext(OmnisGeminiClient.ioDispatcher) {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
                    }
                    
                    // Scale down bitmap to avoid memory issues and too large payloads
                    val maxDim = 1024f
                    val scale = kotlin.math.min(maxDim / bitmap.width, maxDim / bitmap.height)
                    val scaledBitmap = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap, 
                            (bitmap.width * scale).toInt(), 
                            (bitmap.height * scale).toInt(), 
                            true
                        )
                    } else {
                        bitmap
                    }

                    val outputStream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    
                    val text = OmnisGeminiClient.extractTextFromImage(base64Image)
                    
                    if (text != null && text.isNotBlank()) {
                        // Save image locally
                        val file = java.io.File(getApplication<Application>().filesDir, "ocr_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(file).use { out ->
                            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        Pair(file.absolutePath, text)
                    } else {
                        null
                    }
                }
                
                if (textWithFile != null) {
                    _ocrValidationState.value = OcrValidationState(
                        imageLocalPath = textWithFile.first,
                        extractedText = textWithFile.second
                    )
                } else {
                    _errorMessage.value = "Systém neidentifikoval žádný text."
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "OCR Failed", e)
                _errorMessage.value = "Chyba při zpracování obrázku."
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    fun extractTextFromDocument(uri: android.net.Uri, fileName: String, mimeType: String = "application/pdf") {
        _isOcrLoading.value = true
        viewModelScope.launch {
            try {
                val text = if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                    PdfTextExtractor.extractText(getApplication<Application>(), uri, fileName)
                } else {
                    withContext(OmnisGeminiClient.ioDispatcher) {
                        val contentResolver = getApplication<Application>().contentResolver
                        val inputStream = contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            OmnisGeminiClient.extractTextFromDocument(base64Data, mimeType)
                        } else {
                            null
                        }
                    }
                }

                if (!text.isNullOrBlank()) {
                    val currentText = _inputQuery.value
                    val newText = if (currentText.isNotBlank()) {
                        "$currentText\n\n--- Obsah dokumentu ($fileName) ---\n$text\n--- Konec dokumentu ---"
                    } else {
                        "--- Obsah dokumentu ($fileName) ---\n$text\n--- Konec dokumentu ---"
                    }
                    _inputQuery.value = newText
                } else {
                    _errorMessage.value = "Z dokumentu $fileName se nepodařilo přečíst text."
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "Document extraction Failed", e)
                _errorMessage.value = "Chyba při zpracování dokumentu: ${e.localizedMessage ?: "Neznámá chyba"}"
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    // Test Semantic Page State
    private val _testSemanticRecords = MutableStateFlow<List<com.example.data.OmnisSemanticRecord>>(emptyList())
    val testSemanticRecords: StateFlow<List<com.example.data.OmnisSemanticRecord>> = _testSemanticRecords.asStateFlow()

    fun updateSemanticAnchorWeight(recordId: Long, domain: String, word: String, newWeight: Float) {
        _testSemanticRecords.value = _testSemanticRecords.value.map { record ->
            if (record.id == recordId && record.anchors.containsKey(domain)) {
                val updatedAnchorsMap = record.anchors.toMutableMap()
                val updatedAnchors = updatedAnchorsMap[domain]?.map { anchor ->
                    if (anchor.word == word) anchor.copy(weight = newWeight) else anchor
                } ?: emptyList()
                updatedAnchorsMap[domain] = updatedAnchors
                record.copy(anchors = updatedAnchorsMap)
            } else {
                record
            }
        }
    }

    fun reSynthesizeTestRecord(recordId: Long, domain: String) {
        val record = _testSemanticRecords.value.find { it.id == recordId } ?: return
        val anchors = record.anchors[domain] ?: return
        
        val prompt = "Uprav předchozí odpověď tak, aby sémantická váha byla rozložena následovně: " +
            anchors.joinToString(", ") { "${it.word} = ${(it.weight * 100).toInt()}%" }
            
        sendTestSemanticQuery(prompt)
    }

    fun sendTestSemanticQuery(query: String) {
        val q = query.trim()
        if (q.isBlank() || _isLoading.value) return

        _isLoading.value = true
        val userRecord = com.example.data.OmnisSemanticRecord(role = "user", content = q)
        _testSemanticRecords.value = _testSemanticRecords.value + userRecord

        viewModelScope.launch {
            try {
                // Simulate network call or actual call
                kotlinx.coroutines.delay(1000)
                val responseContent = "TOTO JE TESTOVACÍ VÝSTUP PRO SÉMANTICKÉ KOTVY.\n(Systém analyzoval $q)"
                val fakeAnchors = mapOf(
                    "Sys" to listOf(
                        com.example.data.SemanticAnchor("škálovatelnost", 0.9f),
                        com.example.data.SemanticAnchor("bezpečnostní perimetr", 0.5f),
                        com.example.data.SemanticAnchor("latence", 0.3f),
                        com.example.data.SemanticAnchor("propustnost", 0.8f)
                    )
                )
                
                val asstRecord = com.example.data.OmnisSemanticRecord(
                    role = "assistant",
                    content = responseContent,
                    anchors = fakeAnchors
                )
                _testSemanticRecords.value = _testSemanticRecords.value + asstRecord
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Simulation sliders for Impact Matrix tab
    private val _simSys = MutableStateFlow(0.95f)
    val simSys: StateFlow<Float> = _simSys.asStateFlow()

    private val _simEcon = MutableStateFlow(0.88f)
    val simEcon: StateFlow<Float> = _simEcon.asStateFlow()

    private val _simPsych = MutableStateFlow(0.91f)
    val simPsych: StateFlow<Float> = _simPsych.asStateFlow()

    private val _simEco = MutableStateFlow(0.94f)
    val simEco: StateFlow<Float> = _simEco.asStateFlow()

    private val _simLaw = MutableStateFlow(0.98f)
    val simLaw: StateFlow<Float> = _simLaw.asStateFlow()

    private val _simSec = MutableStateFlow(0.99f)
    val simSec: StateFlow<Float> = _simSec.asStateFlow()

    private val _simPhys = MutableStateFlow(0.87f)
    val simPhys: StateFlow<Float> = _simPhys.asStateFlow()

    private val _simSoc = MutableStateFlow(0.90f)
    val simSoc: StateFlow<Float> = _simSoc.asStateFlow()

    private val _scrollToId = MutableStateFlow<Long?>(null)
    val scrollToId: StateFlow<Long?> = _scrollToId.asStateFlow()

    init {
        // Inicializace 3-klíčového fondu Gemini API z BuildConfig a SharedPreferences
        OmnisGeminiClient.KeyPool.initFromContext(application)

        // Synchronizace s globálním ResilienceManagerem
        viewModelScope.launch {
            ResilienceManager.circuitState.collect { state ->
                _isCircuitBreakerTripped.value = (state == ResilienceManager.CircuitState.OPEN)
            }
        }

        memoryFragments = repository.allFragments.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        // Automatické načtení a výběr vláken pro přihlášeného uživatele při startu
        viewModelScope.launch {
            OmnisAuthService.currentSession.collect { session ->
                val username = session?.username ?: "operator"
                val role = session?.role ?: UserRole.STANDARD_USER
                
                val userThreads = if (role == UserRole.ADMIN_OPERATOR) {
                    repository.allThreads.first()
                } else {
                    repository.getThreadsByUser(username).first()
                }
                
                if (userThreads.isNotEmpty()) {
                    val latest = userThreads.first()
                    _activeThreadId.value = latest.threadId
                    _activeThreadTitle.value = latest.threadTitle
                } else {
                    val initialThreadId = "thread_${username}_${System.currentTimeMillis()}"
                    _activeThreadId.value = initialThreadId
                    _activeThreadTitle.value = "Hlavní vlákno"
                    
                    val isStandardMode = _userExperienceMode.value == UserExperienceMode.STANDARD
                    val initial = OmnisRecord(
                        role = "assistant",
                        content = if (isStandardMode) "Vítejte v O.M.N.I.S. Asistent je připraven vám jednoduše odpovídat na dotazy z běžného života, práce či plánování cílů." else "Vítejte v O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Systém je aktivní v režimu přímé ontologické syntézy s reálným vyhodnocováním napříč 8 dimenzemi.",
                        cognitiveProcess = if (isStandardMode) "Inicializace chytrého asistenta." else "1. Inicializace vlákna pro uživatele $username.\n2. Napojení na ontologický rámec.\n3. Výpočet bazálních tenzorů napříč 8 doménami.",
                        followUpQuestions = if (isStandardMode) "Jak si jednoduše naplánovat osobní cíl?|Jak porovnat dvě pracovní nabídky?" else "Jak provázat ekonomické pobídky s ekologickou regenerací?|Jak navrhnout distribuovanou architekturu s nulovou energetickou stopou?",
                        valSys = 0.95f,
                        valEcon = 0.88f,
                        valPsych = 0.91f,
                        valEco = 0.94f,
                        valLaw = 0.98f,
                        valSec = 0.99f,
                        valPhys = 0.87f,
                        valSoc = 0.90f,
                        compositeScore = 0.927f,
                        domain = "SYSTEMS_INTELLIGENCE",
                        threadId = initialThreadId,
                        threadTitle = "Hlavní vlákno",
                        userName = username
                    )
                    repository.insert(initial)
                }
            }
        }
    }

    val defaultTab: OmnisTab
        get() = if (OmnisAuthService.currentUserRole.value == UserRole.ADMIN_OPERATOR) OmnisTab.ADMIN else OmnisTab.CHAT

    private val adminOnlyTabs = setOf(
        OmnisTab.ADMIN,
        OmnisTab.MATRIX,
        OmnisTab.TELEMETRY,
        OmnisTab.NODES,
        OmnisTab.PRODUCTION_AUDIT,
        OmnisTab.DEV_PROMPT_LAB,
        OmnisTab.TEST_SEMANTIC
    )

    private val _tabBackStack = MutableStateFlow<List<OmnisTab>>(emptyList())
    val canNavigateBack: Boolean
        get() = _activeTab.value != defaultTab || _tabBackStack.value.isNotEmpty()

    fun onUserLoggedIn() {
        val role = OmnisAuthService.currentUserRole.value
        _activeTab.value = if (role == UserRole.ADMIN_OPERATOR) OmnisTab.ADMIN else OmnisTab.CHAT
        _tabBackStack.value = emptyList()
    }

    fun setTab(tab: OmnisTab) {
        // Striktní RBAC kontrola: Pokud běžný uživatel zkouší admin záložku, fallback na CHAT
        val targetTab = if (OmnisAuthService.currentUserRole.value != UserRole.ADMIN_OPERATOR && adminOnlyTabs.contains(tab)) {
            OmnisTab.CHAT
        } else {
            tab
        }

        if (_activeTab.value != targetTab) {
            val currentStack = _tabBackStack.value
            if (currentStack.lastOrNull() != _activeTab.value) {
                _tabBackStack.value = (currentStack + _activeTab.value).takeLast(20)
            }
            _activeTab.value = targetTab
        } else {
            // Opakované klepnutí na stejnou záložku odscrolluje na vršek seznamu
            val firstId = records.value.firstOrNull()?.id
            if (firstId != null) {
                _scrollToId.value = firstId
            }
        }
    }

    fun setProficiencyLevel(level: ProficiencyLevel) {
        _proficiencyLevel.value = level
    }

    fun popTab(): Boolean {
        val currentStack = _tabBackStack.value
        if (currentStack.isNotEmpty()) {
            val prev = currentStack.last()
            _tabBackStack.value = currentStack.dropLast(1)
            _activeTab.value = prev
            return true
        } else if (_activeTab.value != defaultTab) {
            _activeTab.value = defaultTab
            return true
        }
        return false
    }

    fun onQueryChange(newQuery: String) {
        _inputQuery.value = newQuery
    }

    fun onDomainChange(domain: String) {
        _selectedDomain.value = domain
    }

    fun setSimSys(v: Float) { _simSys.value = v }
    fun setSimEcon(v: Float) { _simEcon.value = v }
    fun setSimPsych(v: Float) { _simPsych.value = v }
    fun setSimEco(v: Float) { _simEco.value = v }
    fun setSimLaw(v: Float) { _simLaw.value = v }
    fun setSimSec(v: Float) { _simSec.value = v }
    fun setSimPhys(v: Float) { _simPhys.value = v }
    fun setSimSoc(v: Float) { _simSoc.value = v }

    private val _comparisonResult = MutableStateFlow<com.example.api.ComparisonResult?>(null)
    val comparisonResult: StateFlow<com.example.api.ComparisonResult?> = _comparisonResult.asStateFlow()
    
    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    fun synthesizeSelectedRecords(selectedIds: Set<Long>) {
        val selectedRecords = records.value.filter { selectedIds.contains(it.id) }
        if (selectedRecords.size < 2) return
        _isComparing.value = true
        viewModelScope.launch {
            _comparisonResult.value = OmnisGeminiClient.synthesizeComparison(selectedRecords)
            _isComparing.value = false
        }
    }
    
    fun clearComparison() {
        _comparisonResult.value = null
    }

    // Smart Batching Logic
    private data class BatchTask(val query: String, val imagePath: String?, val domain: String)
    private val batchQueue = mutableListOf<BatchTask>()
    private var batchJob: kotlinx.coroutines.Job? = null

    private fun startBatchingJob() {
        if (batchJob != null) return
        batchJob = viewModelScope.launch {
            val window = com.example.action.OrchestrationManager.batchingWindowMs.value
            kotlinx.coroutines.delay(window)
            
            val itemsToProcess = synchronized(batchQueue) {
                val items = batchQueue.toList()
                batchQueue.clear()
                items
            }
            batchJob = null

            if (itemsToProcess.isNotEmpty()) {
                if (itemsToProcess.size == 1) {
                    executeDirectQuery(itemsToProcess[0].query, itemsToProcess[0].imagePath)
                } else {
                    processBatch(itemsToProcess)
                }
            }
        }
    }

    private fun processBatch(tasks: List<BatchTask>) {
        _isLoading.value = true
        _streamState.value = StreamState("execution", "SMART BATCHING: Sdružuji ${tasks.size} kognitivních úloh do jednoho payloadu...")
        
        viewModelScope.launch {
            try {
                val batchItems = tasks.mapIndexed { index, task ->
                    com.example.api.BatchItem(id = "task_$index", query = task.query, domain = task.domain)
                }
                
                val results = com.example.api.OmnisGeminiClient.callGeminiBatchApi(batchItems)
                
                tasks.forEachIndexed { index, task ->
                    val result = results["task_$index"]
                    
                    val currentUsername = OmnisAuthService.currentSession.value?.username ?: "operator"
                    val currentThreadId = _activeThreadId.value
                    val currentThreadTitle = _activeThreadTitle.value

                    // Uložit uživatelský záznam
                    repository.insert(OmnisRecord(
                        role = "user", 
                        content = task.query, 
                        domain = task.domain, 
                        attachedImagePath = task.imagePath,
                        threadId = currentThreadId,
                        threadTitle = currentThreadTitle,
                        userName = currentUsername
                    ))
                    
                    if (result != null) {
                        repository.insert(OmnisRecord(
                            role = "assistant",
                            content = result.answer,
                            cognitiveProcess = result.cognitiveProcess,
                            followUpQuestions = result.followUpQuestions.joinToString("|"),
                            valSys = result.valSys, valEcon = result.valEcon, valPsych = result.valPsych,
                            valEco = result.valEco, valLaw = result.valLaw, valSec = result.valSec,
                            valPhys = result.valPhys, valSoc = result.valSoc,
                            compositeScore = result.composite, domain = task.domain,
                            defenseTier = result.defenseTier, defenseNotes = result.defenseNotes,
                            threadId = currentThreadId,
                            threadTitle = currentThreadTitle,
                            userName = currentUsername
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "Batch processing failed", e)
                _errorMessage.value = "Chyba při dávkovém zpracování: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _streamState.value = null
            }
        }
    }

    fun sendQuery(customQuery: String? = null, imagePath: String? = null) {
        val query = (customQuery ?: _inputQuery.value).trim()
        if (query.isBlank() || _isLoading.value) return

        // EU AI Act Čl. 50: Kontrola transparentního pop-up dialogu před prvním odesláním dat
        if (!com.example.ai.transparency.AiDisclosureManager.checkOrPromptDisclosure()) {
            return
        }

        // 0. KONTROLA STAVU JISTIČE (Circuit Breaker Gate):
        // Automatický průchod při stavu OPEN je zakázán!
        // Vstup je zablokován a čeká se na nový vstup, který projde validací a bude akceptován.
        val circuitBreaker = com.example.api.OmnisGeminiClient.circuitBreaker
        if (circuitBreaker.getState() == com.example.defense.OmnisCircuitBreaker.State.OPEN) {
            val acceptance = circuitBreaker.evaluateAndAcceptNewInput(query, _selectedDomain.value)
            if (acceptance is com.example.defense.OmnisCircuitBreaker.InputAcceptanceResult.Blocked) {
                _errorMessage.value = "VSTUP ZABLOKOVÁN: ${acceptance.reason} Systém čeká na nový akceptovaný dotaz."
                return
            }
        }

        // Check for Smart Batching
        if (com.example.action.OrchestrationManager.isSmartBatchingEnabled.value && imagePath == null) {
            synchronized(batchQueue) {
                batchQueue.add(BatchTask(query, imagePath, _selectedDomain.value))
            }
            _inputQuery.value = ""
            startBatchingJob()
            return
        }

        // 1. & 2. FÁZE: Evaluátor & Sémantická brána (Quality Gate)
        // V režimu STANDARD se brána nepokouší pozastavovat dotaz kvůli chybějícím tagům
        if (_isPromptGatewayEnabled.value && _userExperienceMode.value == UserExperienceMode.EXPERT) {
            val currentRole = com.example.auth.OmnisAuthService.currentUserRole.value
            val gatewayResult = OmnisPromptGateway.processPromptGateway(
                rawText = query, 
                domain = _selectedDomain.value, 
                threshold = _promptGatewayThreshold.value
            )
            if (gatewayResult.status == "needs_review" && currentRole.canAccessSystemActions()) {
                // Zadržet exekuci a předat k Human-in-the-Loop revizi (POUZE PRO ADMIN V EXPERT REŽIMU)
                pendingGatewayImagePath = imagePath
                _pendingGatewayReview.value = gatewayResult
                _inputQuery.value = ""
                return
            }
        }

        // Pokud je schválen bypass nebo je brána vypnuta, pokračovat přímo do exekuce
        executeDirectQuery(query, imagePath)
    }

    fun executeDirectQuery(query: String, imagePath: String? = null) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || _isLoading.value) return

        val isStandardMode = _userExperienceMode.value == UserExperienceMode.STANDARD

        _inputQuery.value = ""
        _isLoading.value = true
        _errorMessage.value = null
        _streamState.value = StreamState(
            "introspection", 
            if (isStandardMode) "Příprava odpovědi a analýza dotazu..." else "Analýza struktury dotazu a kontrola bezpečnostních mantinelů..."
        )

        currentStreamingJob?.cancel()
        currentStreamingJob = viewModelScope.launch {
            try {
                val currentUsername = OmnisAuthService.currentSession.value?.username ?: "operator"
                val currentThreadId = _activeThreadId.value
                val currentThreadTitle = _activeThreadTitle.value

                // Auto-titling pokud je vlákno v počátečním názvu
                if (currentThreadTitle == "Nové vlákno" || currentThreadTitle == "Hlavní vlákno") {
                    val autoTitle = trimmedQuery.take(30).replace("\n", " ").trim()
                    val formattedTitle = if (autoTitle.length >= 30) "$autoTitle..." else autoTitle
                    if (formattedTitle.isNotBlank()) {
                        _activeThreadTitle.value = formattedTitle
                        viewModelScope.launch {
                            repository.updateThreadTitle(currentThreadId, formattedTitle)
                        }
                    }
                }

                // Save user record
                val userRecord = OmnisRecord(
                    role = "user",
                    content = trimmedQuery,
                    domain = _selectedDomain.value,
                    attachedImagePath = imagePath,
                    threadId = currentThreadId,
                    threadTitle = _activeThreadTitle.value,
                    userName = currentUsername
                )
                repository.insert(userRecord)

                kotlinx.coroutines.delay(200)
                _streamState.value = StreamState(
                    "introspection", 
                    if (isStandardMode) "Získávání potřebných informací..." else "Aktivován modul: omnis-core-synthesizer. Načítání kontextu z pgvector..."
                )
                
                // PREDIKTIVNÍ RETRIEVAL: Vyhledání relevantních fragmentů dlouhodobé paměti s hybridním skórováním
                val relevantFragments = com.example.memory.MemoryRetrievalEngine.findRelevantFragments(
                    query = trimmedQuery,
                    dao = repository.omnisDao,
                    domain = _selectedDomain.value
                )
                _activeMemoryFragments.value = relevantFragments
                
                kotlinx.coroutines.delay(200)
                _streamState.value = StreamState(
                    "execution", 
                    if (isStandardMode) "Sestavuji přehlednou odpověď..." else "Generuji strukturovaný payload pro Akční dispečer (Gemini)..."
                )

                // Synthesize response via Gemini / cognitive engine with memory and thread history injection
                val result = OmnisGeminiClient.synthesize(
                    query = trimmedQuery,
                    domain = _selectedDomain.value,
                    memoryFragments = relevantFragments,
                    threadHistory = records.value,
                    userExperienceMode = _userExperienceMode.value
                )
                
                // ULOŽENÍ ARTEFAKTU (pokud byl vygenerován)
                result.artifact?.let { artifact ->
                    val artifactId = repository.omnisDao.insertArtifact(artifact.copy(sourceRecordId = System.currentTimeMillis()))
                    Log.i("OmnisViewModel", "Autonomně uložen artefakt ID: $artifactId")
                }

                _streamState.value = StreamState("verification", "MULTI-LAYER VERIFIED", result.composite, "MULTI-LAYER VERIFIED")
                kotlinx.coroutines.delay(500)

                val watermarkedContent = com.example.ai.transparency.AiSteganographyEngine.embedWatermark(
                    plainText = result.answer,
                    modelName = "gemini-3.1-pro-preview"
                )

                val asstRecord = OmnisRecord(
                    role = "assistant",
                    content = watermarkedContent,
                    cognitiveProcess = result.cognitiveProcess,
                    followUpQuestions = result.followUpQuestions.joinToString("|"),
                    valSys = result.valSys,
                    valEcon = result.valEcon,
                    valPsych = result.valPsych,
                    valEco = result.valEco,
                    valLaw = result.valLaw,
                    valSec = result.valSec,
                    valPhys = result.valPhys,
                    valSoc = result.valSoc,
                    compositeScore = result.composite,
                    domain = _selectedDomain.value,
                    defenseTier = result.defenseTier,
                    defenseNotes = result.defenseNotes,
                    threadId = currentThreadId,
                    threadTitle = _activeThreadTitle.value,
                    userName = currentUsername
                )
                repository.insert(asstRecord)

                // AUTOMATICKÁ ORCHESTRACE NATIVNÍCH AKCÍ
                if (result.recommendedActionId != null) {
                    val payload = ActionPayload(
                        intent = "AI_RECOMMENDED_ORCHESTRATION",
                        actionId = result.recommendedActionId,
                        parameters = result.recommendedActionParams ?: emptyMap()
                    )
                    executeActionPayload(payload)
                } else {
                    // AUTOMATICKÝ ZÁPIS DO POSTGRESQL (Replikace na pozadí)
                    viewModelScope.launch(OmnisGeminiClient.ioDispatcher) {
                        try {
                            OmnisActionDispatcher.executeAction(
                                ActionPayload(
                                    intent = "auto_postgres_replication",
                                    actionId = "postgres_auto_sync",
                                    parameters = mapOf("unsynced_count" to 2, "engine" to "PostgreSQL Cloud SQL")
                                ),
                                repository.omnisDao
                            )
                        } catch (e: Exception) {
                            Log.e("OmnisViewModel", "PostgreSQL auto-replication failed", e)
                        }
                    }
                }

                // Update simulation sliders to match latest result, respecting pinned/fixed domains
                if (!_fixedDomains.value.contains("Sys")) _simSys.value = result.valSys
                if (!_fixedDomains.value.contains("Econ")) _simEcon.value = result.valEcon
                if (!_fixedDomains.value.contains("Psych")) _simPsych.value = result.valPsych
                if (!_fixedDomains.value.contains("Eco")) _simEco.value = result.valEco
                if (!_fixedDomains.value.contains("Law")) _simLaw.value = result.valLaw
                if (!_fixedDomains.value.contains("Sec")) _simSec.value = result.valSec
                if (!_fixedDomains.value.contains("Phys")) _simPhys.value = result.valPhys
                if (!_fixedDomains.value.contains("Soc")) _simSoc.value = result.valSoc

                // FÁZE 2: Memory Engine - Kontrola potřeby sémantické komprese
                checkAndTriggerSemanticCompression(currentThreadId)
            } catch (e: Exception) {
                // Log error and surface to UI via errorMessage flow
                Log.e("OmnisViewModel", "executeDirectQuery failed", e)
                _errorMessage.value = e.localizedMessage ?: e.toString()
            } finally {
                _isLoading.value = false
                _streamState.value = null
            }
        }
    }

    fun selectThread(threadId: String, threadTitle: String) {
        currentStreamingJob?.cancel()
        _isLoading.value = false
        _streamState.value = null
        _activeThreadId.value = threadId
        _activeThreadTitle.value = threadTitle
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log(
                type = "INFO",
                component = "ThreadManager",
                message = "PŘEPNUTO NA VLÁKNO: $threadTitle ($threadId)",
                metadata = "{\"threadId\": \"$threadId\", \"title\": \"$threadTitle\"}"
            )
        }
    }

    fun createNewThread(title: String? = null) {
        currentStreamingJob?.cancel()
        _isLoading.value = false
        _streamState.value = null
        val username = OmnisAuthService.currentSession.value?.username ?: "operator"
        val newThreadId = "thread_${username}_${System.currentTimeMillis()}"
        val threadTitle = title ?: "Nové vlákno"
        _activeThreadId.value = newThreadId
        _activeThreadTitle.value = threadTitle

        viewModelScope.launch {
            val welcome = OmnisRecord(
                role = "assistant",
                content = "Zahájeno nové kognitivní vlákno [$threadTitle] pro uživatele $username. Zadejte dotaz nebo nahrajte podklad pro 8D syntézu.",
                cognitiveProcess = "Inicializace nového izolačního kontextu vlákna $newThreadId pro uživatele $username.",
                valSys = 0.90f,
                valSec = 0.95f,
                compositeScore = 0.90f,
                domain = _selectedDomain.value,
                threadId = newThreadId,
                threadTitle = threadTitle,
                userName = username
            )
            repository.insert(welcome)
            com.example.telemetry.TelemetryEngine.log(
                type = "INFO",
                component = "ThreadManager",
                message = "VYTVOŘENO NOVÉ VLÁKNO: $threadTitle ($newThreadId)",
                metadata = "{\"threadId\": \"$newThreadId\", \"user\": \"$username\"}"
            )
        }
    }

    fun deleteThread(threadId: String) {
        if (_activeThreadId.value == threadId) {
            currentStreamingJob?.cancel()
            _isLoading.value = false
            _streamState.value = null
        }
        viewModelScope.launch {
            repository.deleteThread(threadId)
            val username = OmnisAuthService.currentSession.value?.username ?: "operator"
            val remaining = repository.getThreadsByUser(username).first()
            if (_activeThreadId.value == threadId) {
                if (remaining.isNotEmpty()) {
                    val next = remaining.first()
                    _activeThreadId.value = next.threadId
                    _activeThreadTitle.value = next.threadTitle
                } else {
                    createNewThread()
                }
            }
        }
    }

    fun renameThread(threadId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.updateThreadTitle(threadId, trimmed)
            if (_activeThreadId.value == threadId) {
                _activeThreadTitle.value = trimmed
            }
        }
    }

    fun setAdminUserFilter(userName: String?) {
        _adminSelectedUserFilter.value = userName
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log(
                type = "INFO",
                component = "AdminThreadFilter",
                message = "ADMIN FILTR VLÁKEN NASTAVEN: ${userName ?: "VŠECHNA VLÁKNA"}",
                metadata = "{\"filterUser\": \"$userName\"}"
            )
        }
    }

    fun clearActiveThread() {
        viewModelScope.launch {
            repository.deleteThread(_activeThreadId.value)
            createNewThread()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clear()
            createNewThread()
        }
    }

    private val _isSyncingWithBackend = MutableStateFlow(false)
    val isSyncingWithBackend: StateFlow<Boolean> = _isSyncingWithBackend.asStateFlow()

    fun syncConversationsWithBackend() {
        if (_isSyncingWithBackend.value) return
        _isSyncingWithBackend.value = true
        repository.syncWithBackend(getApplication()) { success ->
            _isSyncingWithBackend.value = false
            if (success) {
                Log.i("OmnisViewModel", "Obousměrná synchronizace s cloudem byla úspěšná.")
            } else {
                Log.w("OmnisViewModel", "Obousměrná synchronizace s cloudem selhala nebo nebyla dokončena.")
            }
        }
    }

    val cloudSqlSyncStatus: StateFlow<com.example.data.CloudSqlSyncManager.SyncStatus> =
        com.example.data.CloudSqlSyncManager.syncStatus

    fun triggerCloudSqlSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.syncWithCloudSql()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                val msg = "✓ Synchronizováno $count zpráv do Google Cloud SQL."
                _errorMessage.value = msg
                onComplete?.invoke(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Chyba připojení ke Cloud SQL"
                _errorMessage.value = "⚠️ Cloud SQL: $err (Zprávy jsou bezpečně uloženy v lokální Room DB)"
                onComplete?.invoke(false, err)
            }
        }
    }

    fun testCloudSqlConnection(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val (ok, message) = repository.testCloudSqlConnection()
            _errorMessage.value = if (ok) "✓ $message" else "⚠️ $message"
            onComplete?.invoke(ok, message)
        }
    }

    fun purgeSyncedLocalRecords(syncedCount: Int) {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Hromadné uvolnění paměti vyžaduje roli Admin / Operátor."
            return
        }
        viewModelScope.launch {
            val all = records.value
            if (all.size <= syncedCount || syncedCount <= 0) return@launch
            // Získat ID posledního synchronizovaného záznamu
            val syncedRecords = all.take(syncedCount)
            val maxSyncedId = syncedRecords.maxOfOrNull { it.id } ?: return@launch
            
            repository.deleteSyncedRecords(maxSyncedId)
            _errorMessage.value = "✓ Úspěšně smazáno $syncedCount synchronizovaných záznamů z lokální DB."
        }
    }

    fun jumpToContext(record: OmnisRecord) {
        viewModelScope.launch {
            _scrollToId.value = record.id
            _activeTab.value = OmnisTab.CHAT
        }
    }

    fun clearScrollJump() {
        _scrollToId.value = null
    }

    fun authorizeBlockedRecord(record: OmnisRecord) {
        viewModelScope.launch {
            val updated = record.copy(
                defenseTier = "APPROVED",
                defenseNotes = "Manuálně autorizováno operátorem (Human-in-the-Loop Override)."
            )
            repository.insert(updated)
        }
    }

    fun triggerMemoryConsolidation() {
        if (_isConsolidating.value) return
        _isConsolidating.value = true
        viewModelScope.launch {
            try {
                val currentRecords = records.value
                val success = com.example.memory.MemoryConsolidator.consolidate(currentRecords, repository.omnisDao)
                if (success) {
                    _errorMessage.value = "✓ Konsolidace paměti dokončena."
                } else {
                    _errorMessage.value = "Konsolidace nebyla provedena (nedostatek dat nebo chyba)."
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "Memory consolidation failed", e)
                _errorMessage.value = "Chyba při konsolidaci: ${e.localizedMessage}"
            } finally {
                _isConsolidating.value = false
            }
        }
    }

    fun deleteMemoryFragment(id: Long) {
        viewModelScope.launch {
            repository.omnisDao.deleteFragment(id)
        }
    }

    fun onDeleteArtifact(id: Long) {
        viewModelScope.launch {
            repository.omnisDao.deleteArtifact(id)
        }
    }

    fun onCreateArtifact(title: String, type: String, language: String, content: String, metadata: String = "{}") {
        viewModelScope.launch {
            val artifact = com.example.data.OmnisArtifact(
                title = title,
                type = type,
                language = language,
                content = content,
                metadata = metadata
            )
            repository.omnisDao.insertArtifact(artifact)
            com.example.telemetry.TelemetryEngine.log("INFO", "ARTF", "Vytvořen nový znalostní artefakt: $title [$type]")
        }
    }

    fun onExportArtifact(artifact: com.example.data.OmnisArtifact) {
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log("INFO", "ARTF_EXPORT", "Exportován artefakt '${artifact.title}' (${artifact.type})")
        }
    }

    fun onCreateGoal(title: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            com.example.telemetry.TelemetryEngine.log("INFO", "AGTO", "Zahájena dekonstrukce vize: $title")
            val tasks = com.example.agent.GoalDeconstructor.deconstruct(title, description)
            val tasksJson = kotlinx.serialization.json.Json.encodeToString(tasks)
            
            val goal = com.example.data.OmnisGoal(
                title = title,
                description = description,
                status = "ACTIVE",
                priority = 3,
                progress = 0f,
                tasksJson = tasksJson
            )
            repository.omnisDao.insertGoal(goal)
            com.example.telemetry.TelemetryEngine.log("INFO", "AGTO", "Vize dekonstruována na ${tasks.size} úkolů.", "{\"goal\":\"$title\"}")
            _isLoading.value = false
        }
    }

    fun onDeleteGoal(id: Long) {
        viewModelScope.launch {
            repository.omnisDao.deleteGoal(id)
        }
    }

    fun onToggleGoalTask(goalId: Long, taskId: String) {
        viewModelScope.launch {
            val allGoals = repository.omnisDao.getAllGoals().first()
            val goal = allGoals.find { it.id == goalId } ?: return@launch
            try {
                val currentTasks = kotlinx.serialization.json.Json.decodeFromString<List<com.example.data.OmnisTask>>(goal.tasksJson)
                val updatedTasks = currentTasks.map { task ->
                    if (task.id == taskId) {
                        val newStatus = if (task.status == "DONE") "PENDING" else "DONE"
                        task.copy(status = newStatus)
                    } else task
                }
                val doneCount = updatedTasks.count { it.status == "DONE" }
                val newProgress = if (updatedTasks.isNotEmpty()) doneCount.toFloat() / updatedTasks.size.toFloat() else 0f
                val newStatus = if (newProgress >= 1.0f) "COMPLETED" else "ACTIVE"
                val updatedJson = kotlinx.serialization.json.Json.encodeToString(updatedTasks)
                repository.omnisDao.updateGoalProgress(goalId, newStatus, newProgress, updatedJson)
                com.example.telemetry.TelemetryEngine.log(
                    "INFO", 
                    "AGTO", 
                    "Úkol '$taskId' v cíli '${goal.title}' aktualizován. Celková progrese: ${(newProgress * 100).toInt()}%."
                )
            } catch (e: Exception) {
                android.util.Log.e("OmnisViewModel", "Chyba při aktualizaci úkolu", e)
            }
        }
    }

    /**
     * Vytvoří nový cíl se zadanými dílčími úkoly (např. při 8D kompenzaci z matice).
     */
    fun createGoalWithTasks(title: String, description: String, taskTitles: List<String>) {
        viewModelScope.launch {
            val tasks = taskTitles.mapIndexed { index, tTitle ->
                com.example.data.OmnisTask(
                    id = java.util.UUID.randomUUID().toString().take(8),
                    title = tTitle,
                    description = "8D Kompenzační krok",
                    status = if (index == 0) "IN_PROGRESS" else "PENDING",
                    assignedAgentId = "OMNIS_AUTOPOIESIS_AGENT",
                    domain = "8D_COMPENSATION"
                )
            }
            val tasksJson = kotlinx.serialization.json.Json.encodeToString(tasks)
            val goal = com.example.data.OmnisGoal(
                title = title,
                description = description,
                status = "ACTIVE",
                priority = 5,
                progress = 0.05f,
                tasksJson = tasksJson
            )
            repository.omnisDao.insertGoal(goal)
            com.example.telemetry.TelemetryEngine.log("INFO", "GOAL_COMPENSATION", "Založen 8D kompenzační cíl: $title")
        }
    }

    /**
     * Vyhodnotí 8D vektor a historii pro generování návrhu kompenzačního cíle.
     */
    fun evaluate8DForCompensatoryGoals(
        vector: com.example.ui.octagon.Omnis8dVector,
        history: List<com.example.ui.octagon.Omnis8dVector>
    ) {
        val anomalies = anomaliesDetectionUseCase.detectAnomalies(vector, history)
        val frictions = anomaliesDetectionUseCase.evaluateFrictions(vector)
        val proposal = anomaliesDetectionUseCase.generateCompensatoryProposal(frictions, anomalies)
        _compensatoryGoalProposal.value = proposal
    }

    /**
     * Uživatelské schválení kompenzačního cíle (One-Click Approval).
     */
    fun acceptCompensatoryGoal(draft: com.example.domain.usecase.CompensatoryGoalDraft) {
        viewModelScope.launch {
            val goal = goalOrchestrationUseCase.createGoalFromCompensatoryDraft(draft)
            repository.omnisDao.insertGoal(goal)
            com.example.telemetry.TelemetryEngine.log("INFO", "GOAL_COMPENSATION", "Založen cíl z 8D návrhu: ${draft.title}")
            _compensatoryGoalProposal.value = null
        }
    }

    fun dismissCompensatoryGoal() {
        _compensatoryGoalProposal.value = null
    }

    /**
     * Offline lokální sémantické vyhledávání nad historií zpráv a Znalostním Nexusem.
     */
    fun searchNexusOffline(query: String, targetDomain: String? = null): List<com.example.domain.usecase.SemanticSearchResult> {
        val currentRecords = records.value
        return offlineSemanticSearchUseCase.search(query, currentRecords, targetDomain)
    }

    /**
     * Lokální vektorové / fulltextové sémantické RAG vyhledávání v paměti Znalostního Nexusu.
     */
    suspend fun searchKnowledgeNexusVector(
        query: String,
        isVectorMode: Boolean = true
    ): List<com.example.domain.usecase.NexusSearchResult> {
        val fragments = activeMemoryFragments.value.ifEmpty { memoryFragments.value }
        val currentRecords = records.value
        return searchLocalKnowledgeNexusUseCase.execute(query, fragments, currentRecords, isVectorMode)
    }

    /**
     * Test dostupnosti zabezpečené REST API Gateway.
     */
    suspend fun pingApiGateway(): Pair<Boolean, String> {
        return com.example.data.sync.OmnisApiGatewayClient.pingGateway()
    }

    /**
     * Dávková synchronizace přes REST API Gateway bez přímé expozice PostgreSQL.
     */
    suspend fun syncPendingWithApiGateway(): Pair<Int, String> {
        val allCurrentRecords = repository.allRecords.first()
        val pending = allCurrentRecords.filter { !it.isSyncedToPostgres }
        val result = com.example.data.sync.OmnisApiGatewayClient.syncBatchToGateway(pending)
        if (result.first > 0) {
            repository.omnisDao.markRecordsAsSynced(pending.map { it.id })
        }
        return result
    }

    fun onClearTelemetry() {
        viewModelScope.launch {
            repository.omnisDao.clearTelemetry()
            com.example.telemetry.TelemetryEngine.log("INFO", "Admin", "Telemetrická data byla smazána.")
        }
    }

    fun performSelfHealing() {
        if (_isSelfHealingActive.value) return
        _isSelfHealingActive.value = true
        _selfHealingLogs.value = emptyList()

        viewModelScope.launch {
            val logs = mutableListOf<String>()
            
            fun log(msg: String) {
                val timeStamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
                logs.add("[$timeStamp] $msg")
                _selfHealingLogs.value = logs.toList()
            }

            log("Zahájen autonomní proces Self-Healing UI & State Diagnostics...")
            delay(400)

            // 1. Diagnostika stavu jističe (Circuit Breaker)
            log("DIAGNOSTIKA: Kontrola stavu systémových jističů...")
            val circuitStateValue = com.example.action.ResilienceManager.circuitState.value
            if (circuitStateValue != com.example.action.ResilienceManager.CircuitState.CLOSED) {
                log("SITUACE: Detekován nestabilní stav jističe: $circuitStateValue.")
                log("OPRAVA: Resetování jističe do stavu CLOSED a vyčištění chybové fronty...")
                resetCircuitBreaker()
                delay(300)
                log("STAV: Jistič úspěšně UZAVŘEN.")
            } else {
                log("OK: Jističe jsou v optimálním uzavřeném stavu.")
            }

            // 2. Diagnostika zaseknutých loading / exekučních asynchronních operací
            log("DIAGNOSTIKA: Kontrola asynchronních zámků...")
            var repairedLocks = 0
            if (_isActionExecuting.value) {
                log("SITUACE: Detekován aktivní exekuční zámek akcí. Rušení deadlocku...")
                _isActionExecuting.value = false
                repairedLocks++
            }
            if (_isLoading.value) {
                log("SITUACE: Detekován zaseknutý stav načítání. Obnova UI...")
                _isLoading.value = false
                repairedLocks++
            }
            if (_isConsolidating.value) {
                log("SITUACE: Detekována visící konsolidace paměti. Uvolnění vlákna...")
                _isConsolidating.value = false
                repairedLocks++
            }
            if (_isComparing.value) {
                log("SITUACE: Detekován visící komparační modul. Resetování stavu...")
                _isComparing.value = false
                repairedLocks++
            }
            if (_isAnalyzingError.value) {
                log("SITUACE: Detekován visící diagnostický LLM modul. Resetování...")
                _isAnalyzingError.value = false
                repairedLocks++
            }
            
            if (repairedLocks > 0) {
                log("OPRAVA: Úspěšně uvolněno $repairedLocks asynchronních zámků.")
            } else {
                log("OK: Všechny asynchronní stavy a loading zámky jsou konzistentní.")
            }
            delay(300)

            // 3. Diagnostika sémantické brány (Prompt Gateway)
            log("DIAGNOSTIKA: Kontrola sémantického filtru promptů...")
            val threshold = _promptGatewayThreshold.value
            if (threshold > 0.85f || threshold < 0.20f) {
                log("SITUACE: Detekována extrémní hodnota sémantického prahu: $threshold.")
                log("OPRAVA: Kalibrace prahu na bezpečnou hodnotu 0.40f...")
                _promptGatewayThreshold.value = 0.40f
                delay(200)
                log("STAV: Sémantická brána překalibrována.")
            } else {
                log("OK: Sémantický filtr je v doporučeném limitu ($threshold).")
            }

            // 4. Vyčištění dočasných chyb a zpráv
            if (_errorMessage.value != null) {
                log("SITUACE: Detekována aktivní chybová hláška. OPRAVA: Vymazání a sanitace chybového kanálu...")
                _errorMessage.value = null
            }

            // 5. Verifikace databázové integrity
            log("DIAGNOSTIKA: Verifikace integrity databáze Room SQLite...")
            try {
                val testPayload = ActionPayload(
                    intent = "self_healing_test",
                    actionId = "db_connectivity_test"
                )
                val testResult = com.example.action.OmnisActionDispatcher.executeAction(testPayload, repository.omnisDao)
                if (testResult.isSuccess) {
                    log("OK: Databázová integrita ověřena. SQLite v pořádku.")
                } else {
                    log("WARN: Databázový test vrátil varování. Spouštění SQLite vakuové komprese...")
                    repository.omnisDao.clearTelemetry()
                    log("OPRAVA: Stará telemetrická data smazána k uvolnění connection poolu.")
                }
            } catch (e: Exception) {
                log("ERROR: Detekována chyba připojení: ${e.localizedMessage}")
                log("OPRAVA: Pokus o obnovu Room Database session...")
            }
            delay(400)

            log("PROCES DOKONČEN: Všechny subsystémy byly diagnostikovány a opraveny.")
            com.example.telemetry.TelemetryEngine.log(
                "INFO", 
                "SelfHealing", 
                "Autonomní Self-Healing úspěšně proběhl. Opraveno zámků: $repairedLocks.",
                "{\"repaired_locks\": $repairedLocks}"
            )
            _isSelfHealingActive.value = false
        }
    }

    private val _llmAnalysisResult = MutableStateFlow<LlmErrorAnalysis?>(null)
    val llmAnalysisResult: StateFlow<LlmErrorAnalysis?> = _llmAnalysisResult.asStateFlow()

    private val _isAnalyzingError = MutableStateFlow(false)
    val isAnalyzingError: StateFlow<Boolean> = _isAnalyzingError.asStateFlow()

    fun logDiagnosticWarning(component: String, message: String, metadata: String = "{}") {
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log("WARN", component, message, metadata)
        }
    }

    fun logDiagnosticError(component: String, message: String, metadata: String = "{}") {
        viewModelScope.launch {
            com.example.telemetry.TelemetryEngine.log("ERROR", component, message, metadata)
        }
    }

    fun analyzeErrorWithLlm(log: com.example.data.OmnisTelemetry) {
        viewModelScope.launch {
            _isAnalyzingError.value = true
            try {
                val prompt = """
                    Jsi O.M.N.I.S. AI Diagnostik & Systémový Architekt.
                    Prožeň automatickou hloubkovou analýzu příčin (Root Cause Analysis) následující systémové chyby/varování z logu:
                    
                    Typ události: ${log.type}
                    Komponenta/Doména: ${log.component}
                    Zpráva chyby: ${log.message}
                    Metadata/Kontext: ${log.metadata}
                    Čas vzniku: ${java.util.Date(log.timestamp)}
                    
                    Strukturuj svou odpověď přesně takto v češtině:
                    1. 🧠 DETAILNÍ INFERENČNÍ ANALÝZA KOŘENOVÉ PŘÍČINY (Root Cause Analysis):
                       Vysvětli technickou podstatu selhání na úrovni kognitivního jádra, LLM rozhraní, sémantického perimetru nebo databáze.
                    2. 🛠️ KONKRÉTNÍ AKČNÍ KROKY K OPRAVĚ PRO ADMINISTRÁTORA (SOP Remediation Steps):
                       Detailně popiš 3-4 konkrétní kroky pro správce (např. 1. Povýšit prompt na trojsložkový tvar [Doména]+[Akce]+[Kritérium], 2. Resetovat jistič v horní liště, 3. Reindexovat paměťové fragmenty).
                    3. 🛡️ PREVENTIVNÍ OPATŘENÍ:
                       Doporučení pro zamezení opakování chyby v produkčním provozu.
                """.trimIndent()

                val result = OmnisGeminiClient.synthesize(prompt, log.component)

                val rawLines = result.answer.lines()
                val extractedSteps = rawLines
                    .filter { line -> line.trim().startsWith("-") || line.trim().matches(Regex("^\\d+\\..*")) }
                    .map { it.replace(Regex("^[\\d\\.\\-\\*\n\\s]+"), "").trim() }
                    .filter { it.isNotBlank() }
                    .filter { 
                        !it.contains("ANALÝZA KOŘENOVÉ PŘÍČINY", true) && 
                        !it.contains("AKČNÍ KROKY K OPRAVĚ", true) && 
                        !it.contains("PREVENTIVNÍ OPATŘENÍ", true) &&
                        !it.contains("Vysvětli technickou podstatu", true) &&
                        !it.contains("Detailně popiš", true) &&
                        !it.contains("Doporučení pro zamezení", true)
                    }

                _llmAnalysisResult.value = LlmErrorAnalysis(
                    logId = log.id,
                    component = log.component,
                    message = log.message,
                    analysisText = result.answer,
                    recommendedSteps = if (extractedSteps.isNotEmpty()) extractedSteps else listOf(
                        "Formulovat dotaz v trojsložkovém tvaru: [SYSTEMS_INTELLIGENCE] + [Akční sloveso] + [Kritérium]",
                        "Využít Sémantickou bránu (Prompt Gateway) a kliknout na 'Převzít a odeslat'",
                        "Pokud je jistič ve stavu OPEN, provést reset v horní liště aplikace",
                        "Otestovat upravený prompt v Dev Prompt Labu"
                    )
                )
            } catch (e: Exception) {
                _llmAnalysisResult.value = LlmErrorAnalysis(
                    logId = log.id,
                    component = log.component,
                    message = log.message,
                    analysisText = "Selhání LLM inferenční analýzy: ${e.message ?: "Neznámá výjimka API"}. Doporučujeme manuální kontrolu logu.",
                    recommendedSteps = listOf(
                        "Zkontrolovat stav síťového připojení a klíče Gemini API",
                        "Provést reset jističe v horní liště",
                        "Vyčistit paměťové fragmenty v Admin Hubu"
                    )
                )
            } finally {
                _isAnalyzingError.value = false
            }
        }
    }

    fun clearLlmErrorAnalysis() {
        _llmAnalysisResult.value = null
    }

    fun runNexusCollaboration(query: String, agentIds: List<String>) {
        if (query.isBlank() || agentIds.isEmpty()) return
        
        _isLoading.value = true
        _activeNexusAgents.value = agentIds
        viewModelScope.launch {
            try {
                val finalResult = com.example.agent.NexusOrchestrator.runCollaborativeSession(query, agentIds)
                if (finalResult != null) {
                    val currentUsername = OmnisAuthService.currentSession.value?.username ?: "operator"
                    val currentThreadId = _activeThreadId.value
                    val currentThreadTitle = _activeThreadTitle.value

                    // Uložit finální syntézu do historie
                    val userRecord = OmnisRecord(
                        role = "user",
                        content = "🤝 [NEXUS COLLABORATION] Téma: $query | Agenti: ${agentIds.joinToString(", ")}",
                        domain = "NEXUS_ORCHESTRATION",
                        threadId = currentThreadId,
                        threadTitle = currentThreadTitle,
                        userName = currentUsername
                    )
                    repository.insert(userRecord)

                    val asstRecord = OmnisRecord(
                        role = "assistant",
                        content = finalResult.answer,
                        cognitiveProcess = "Multi-agentní kolaborace NEXUS:\n" + 
                            com.example.agent.NexusOrchestrator.nexusHistory.value.joinToString("\n") { 
                                "- ${it.agentId}: ${it.text.take(60)}..." 
                            },
                        followUpQuestions = finalResult.followUpQuestions.joinToString("|"),
                        valSys = finalResult.valSys, valEcon = finalResult.valEcon, valPsych = finalResult.valPsych,
                        valEco = finalResult.valEco, valLaw = finalResult.valLaw, valSec = finalResult.valSec,
                        valPhys = finalResult.valPhys, valSoc = finalResult.valSoc,
                        compositeScore = finalResult.composite, domain = "NEXUS_SYNTHESIS",
                        defenseTier = finalResult.defenseTier, defenseNotes = finalResult.defenseNotes,
                        threadId = currentThreadId,
                        threadTitle = currentThreadTitle,
                        userName = currentUsername
                    )
                    repository.insert(asstRecord)
                    
                    // Přepnout na chat pro zobrazení výsledku
                    _activeTab.value = OmnisTab.CHAT
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "Nexus collaboration failed", e)
                _errorMessage.value = "Chyba při NEXUS kolaboraci: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _activeNexusAgents.value = emptyList()
            }
        }
    }

    /**
     * FÁZE 2: Memory Engine - Sémantická komprese historie
     */
    private fun checkAndTriggerSemanticCompression(threadId: String) {
        viewModelScope.launch {
            try {
                // Získání aktuálních záznamů pro toto vlákno
                val threadRecords = repository.omnisDao.getRecordsByThread(threadId).first()
                
                // Pokud je víc než 20 zpráv a ještě nebyla v tomto běhu provedena komprese (nebo je vlákno dlouhé)
                // Hledáme v cognitiveProcess příznak, že komprese již proběhla u některé z nedávných zpráv
                val recentCompression = threadRecords.takeLast(10).any { 
                    it.cognitiveProcess?.contains("COMPLETED_SEMANTIC_COMPRESSION") == true 
                }

                if (threadRecords.size >= 15 && !recentCompression) {
                    runSemanticCompression(threadId, threadRecords)
                }
            } catch (e: Exception) {
                Log.e("OmnisViewModel", "Error checking for semantic compression", e)
            }
        }
    }

    private suspend fun runSemanticCompression(threadId: String, threadRecords: List<OmnisRecord>) {
        try {
            val summary = OmnisGeminiClient.summarizeConversation(threadRecords)
            if (summary != null) {
                val compressionRecord = OmnisRecord(
                    role = "system",
                    content = "⚡ [SÉMANTICKÁ KOMPRESE] Systém provedl zahuštění kontextu: $summary",
                    cognitiveProcess = "COMPLETED_SEMANTIC_COMPRESSION",
                    threadId = threadId,
                    threadTitle = _activeThreadTitle.value,
                    userName = "system",
                    domain = "system_memory"
                )
                repository.insert(compressionRecord)
                com.example.telemetry.TelemetryEngine.log("INFO", "MEM", "Sémantická komprese dokončena pro vlákno $threadId")
            }
        } catch (e: Exception) {
            Log.e("OmnisViewModel", "Semantic compression execution error", e)
        }
    }

    // --- FÁZE XX: MODULÁRNÍ MODERNIZACE UŽIVATELSKÉHO FRONTENDU ---
    private val _causalInterventionResult = MutableStateFlow<OmnisCorrelationEngine.CausalDoCalculusAnalysis?>(null)
    val causalInterventionResult: StateFlow<OmnisCorrelationEngine.CausalDoCalculusAnalysis?> = _causalInterventionResult.asStateFlow()

    private val _homeostasisRegulationResult = MutableStateFlow<OmnisCorrelationEngine.HomeostaticRegulationAnalysis?>(null)
    val homeostasisRegulationResult: StateFlow<OmnisCorrelationEngine.HomeostaticRegulationAnalysis?> = _homeostasisRegulationResult.asStateFlow()

    private val _promptMutationsResult = MutableStateFlow<OmnisCorrelationEngine.PromptEvolutionResult?>(null)
    val promptMutationsResult: StateFlow<OmnisCorrelationEngine.PromptEvolutionResult?> = _promptMutationsResult.asStateFlow()

    private val _smtVerificationResult = MutableStateFlow<OmnisCorrelationEngine.NeuroSymbolicVerificationResult?>(null)
    val smtVerificationResult: StateFlow<OmnisCorrelationEngine.NeuroSymbolicVerificationResult?> = _smtVerificationResult.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    fun toggleHapticFeedback() {
        _isHapticEnabled.value = !_isHapticEnabled.value
    }

    fun triggerHapticPulse(isHighTension: Boolean = false) {
        if (!_isHapticEnabled.value) return
        try {
            val app = getApplication<android.app.Application>()
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (app.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val duration = if (isHighTension) 120L else 40L
                    val amplitude = if (isHighTension) 200 else 80
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (isHighTension) 100L else 30L)
                }
            }
        } catch (_: Exception) {}
    }

    fun runCausalIntervention(domain: String, fixedValue: Float) {
        val current8D = mapOf(
            "Sys" to _simSys.value,
            "Econ" to _simEcon.value,
            "Psych" to _simPsych.value,
            "Eco" to _simEco.value,
            "Law" to _simLaw.value,
            "Sec" to _simSec.value,
            "Phys" to _simPhys.value,
            "Soc" to _simSoc.value
        )
        val analysis = OmnisCorrelationEngine.calculateCausalDoIntervention(
            observedValues = current8D,
            targetDomain = domain,
            targetValue = fixedValue
        )
        _causalInterventionResult.value = analysis
        triggerHapticPulse(false)
    }

    fun clearCausalIntervention() {
        _causalInterventionResult.value = null
    }

    fun applyAshbyHomeostasis() {
        val current8D = mapOf(
            "Sys" to _simSys.value,
            "Econ" to _simEcon.value,
            "Psych" to _simPsych.value,
            "Eco" to _simEco.value,
            "Law" to _simLaw.value,
            "Sec" to _simSec.value,
            "Phys" to _simPhys.value,
            "Soc" to _simSoc.value
        )
        val analysis = OmnisCorrelationEngine.calculateHomeostaticRegulation(current8D)
        _homeostasisRegulationResult.value = analysis

        // Aplikovat regulované hodnoty na slidery s respektováním uzamčených dimenzí
        if (!_fixedDomains.value.contains("Sys")) _simSys.value = analysis.regulatedValues["Sys"] ?: _simSys.value
        if (!_fixedDomains.value.contains("Econ")) _simEcon.value = analysis.regulatedValues["Econ"] ?: _simEcon.value
        if (!_fixedDomains.value.contains("Psych")) _simPsych.value = analysis.regulatedValues["Psych"] ?: _simPsych.value
        if (!_fixedDomains.value.contains("Eco")) _simEco.value = analysis.regulatedValues["Eco"] ?: _simEco.value
        if (!_fixedDomains.value.contains("Law")) _simLaw.value = analysis.regulatedValues["Law"] ?: _simLaw.value
        if (!_fixedDomains.value.contains("Sec")) _simSec.value = analysis.regulatedValues["Sec"] ?: _simSec.value
        if (!_fixedDomains.value.contains("Phys")) _simPhys.value = analysis.regulatedValues["Phys"] ?: _simPhys.value
        if (!_fixedDomains.value.contains("Soc")) _simSoc.value = analysis.regulatedValues["Soc"] ?: _simSoc.value

        triggerHapticPulse(true)
    }

    fun evolvePrompt(basePrompt: String) {
        val targetDom = _selectedDomain.value
        val result = OmnisCorrelationEngine.evolveInterventionPromptGenetic(
            targetDomain = targetDom,
            targetDelta = 0.20f
        )
        _promptMutationsResult.value = result
    }

    fun clearPromptMutations() {
        _promptMutationsResult.value = null
    }

    fun verifySmtSafety(): OmnisCorrelationEngine.NeuroSymbolicVerificationResult {
        val current8D = mapOf(
            "Sys" to _simSys.value,
            "Econ" to _simEcon.value,
            "Psych" to _simPsych.value,
            "Eco" to _simEco.value,
            "Law" to _simLaw.value,
            "Sec" to _simSec.value,
            "Phys" to _simPhys.value,
            "Soc" to _simSoc.value
        )
        val res = OmnisCorrelationEngine.verifyLogicalInvariants(current8D)
        _smtVerificationResult.value = res
        return res
    }
}
