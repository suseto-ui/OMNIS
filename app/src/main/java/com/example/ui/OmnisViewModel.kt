package com.example.ui

import kotlinx.coroutines.Dispatchers
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
    GUIDE
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
    }

    val isAuthenticated: StateFlow<Boolean> = OmnisAuthService.isAuthenticated
    val currentRole: StateFlow<UserRole> = OmnisAuthService.currentUserRole

    fun logout() {
        OmnisAuthService.logout(getApplication())
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

    private val _activeNexusAgents = MutableStateFlow<List<String>>(emptyList())
    val activeNexusAgents: StateFlow<List<String>> = _activeNexusAgents.asStateFlow()

    private val _activeTab = MutableStateFlow(OmnisTab.DASHBOARD)
    val activeTab: StateFlow<OmnisTab> = _activeTab.asStateFlow()

    // System Circuit Breaker (Hlavní Jistič)
    private val _isCircuitBreakerTripped = MutableStateFlow(false)
    val isCircuitBreakerTripped: StateFlow<Boolean> = _isCircuitBreakerTripped.asStateFlow()

    // Sémantická brána (Semantic Prompt Gateway)
    private val _isPromptGatewayEnabled = MutableStateFlow(true)
    val isPromptGatewayEnabled: StateFlow<Boolean> = _isPromptGatewayEnabled.asStateFlow()

    private val _promptGatewayThreshold = MutableStateFlow(0.85f)
    val promptGatewayThreshold: StateFlow<Float> = _promptGatewayThreshold.asStateFlow()

    fun setPromptGatewayThreshold(threshold: Float) {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Nastavení citlivosti brány vyžaduje roli Admin."
            return
        }
        val clamped = threshold.coerceIn(0.50f, 0.95f)
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

    fun toggleCircuitBreaker() {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Ovládání hlavního jističe vyžaduje roli Admin."
            return
        }
        val newState = !_isCircuitBreakerTripped.value
        _isCircuitBreakerTripped.value = newState
        com.example.action.ResilienceManager.setCircuitState(
            if (newState) com.example.action.ResilienceManager.CircuitState.OPEN 
            else com.example.action.ResilienceManager.CircuitState.CLOSED
        )
        if (!newState) {
            com.example.api.OmnisGeminiClient.circuitBreaker.reset()
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
                    
                    val initial = OmnisRecord(
                        role = "assistant",
                        content = "Vítejte v O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Systém je aktivní v režimu přímé ontologické syntézy s reálným vyhodnocováním napříč 8 dimenzemi.",
                        cognitiveProcess = "1. Inicializace vlákna pro uživatele $username.\n2. Napojení na ontologický rámec.\n3. Výpočet bazálních tenzorů napříč 8 doménami.",
                        followUpQuestions = "Jak provázat ekonomické pobídky s ekologickou regenerací?|Jak navrhnout distribuovanou architekturu s nulovou energetickou stopou?",
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

    private val _tabBackStack = MutableStateFlow<List<OmnisTab>>(emptyList())
    val canNavigateBack: Boolean
        get() = _activeTab.value != OmnisTab.DASHBOARD || _tabBackStack.value.isNotEmpty()

    fun setTab(tab: OmnisTab) {
        if (_activeTab.value != tab) {
            if (tab == OmnisTab.DASHBOARD) {
                _tabBackStack.value = emptyList()
            } else {
                val currentStack = _tabBackStack.value
                if (currentStack.lastOrNull() != _activeTab.value) {
                    _tabBackStack.value = currentStack + _activeTab.value
                }
            }
            _activeTab.value = tab
        }
    }

    fun popTab(): Boolean {
        val currentStack = _tabBackStack.value
        if (currentStack.isNotEmpty()) {
            val prev = currentStack.last()
            _tabBackStack.value = currentStack.dropLast(1)
            _activeTab.value = prev
            return true
        } else if (_activeTab.value != OmnisTab.DASHBOARD) {
            _activeTab.value = OmnisTab.DASHBOARD
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
        if (_isPromptGatewayEnabled.value) {
            val currentRole = com.example.auth.OmnisAuthService.currentUserRole.value
            val gatewayResult = OmnisPromptGateway.processPromptGateway(
                rawText = query, 
                domain = _selectedDomain.value, 
                threshold = _promptGatewayThreshold.value
            )
            if (gatewayResult.status == "needs_review" && currentRole.canAccessSystemActions()) {
                // Zadržet exekuci a předat k Human-in-the-Loop revizi (POUZE PRO ADMIN)
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

        if (_isCircuitBreakerTripped.value) {
            _errorMessage.value = "⚠️ HLAVNÍ JISTIČ VYPNUT: Kognitivní exekuce byla pozastavena operátorem. Zapněte jistič v navigačním menu."
            return
        }

        _inputQuery.value = ""
        _isLoading.value = true
        _errorMessage.value = null
        _streamState.value = StreamState("introspection", "Analýza struktury dotazu a kontrola bezpečnostních mantinelů...")

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

                kotlinx.coroutines.delay(400)
                _streamState.value = StreamState("introspection", "Aktivován modul: omnis-core-synthesizer. Načítání kontextu z pgvector...")
                
                // PREDIKTIVNÍ RETRIEVAL: Vyhledání relevantních fragmentů dlouhodobé paměti s hybridním skórováním
                val relevantFragments = com.example.memory.MemoryRetrievalEngine.findRelevantFragments(
                    query = trimmedQuery,
                    dao = repository.omnisDao,
                    domain = _selectedDomain.value
                )
                _activeMemoryFragments.value = relevantFragments
                
                kotlinx.coroutines.delay(400)
                _streamState.value = StreamState("execution", "Generuji strukturovaný payload pro Akční dispečer (Gemini)...")

                // Synthesize response via Gemini / cognitive engine with memory and thread history injection
                val result = OmnisGeminiClient.synthesize(
                    query = trimmedQuery,
                    domain = _selectedDomain.value,
                    memoryFragments = relevantFragments,
                    threadHistory = records.value
                )
                
                // ULOŽENÍ ARTEFAKTU (pokud byl vygenerován)
                result.artifact?.let { artifact ->
                    val artifactId = repository.omnisDao.insertArtifact(artifact.copy(sourceRecordId = System.currentTimeMillis()))
                    Log.i("OmnisViewModel", "Autonomně uložen artefakt ID: $artifactId")
                }

                _streamState.value = StreamState("verification", "MULTI-LAYER VERIFIED", result.composite, "MULTI-LAYER VERIFIED")
                kotlinx.coroutines.delay(500)

                val asstRecord = OmnisRecord(
                    role = "assistant",
                    content = result.answer,
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

                // Update simulation sliders to match latest result
                _simSys.value = result.valSys
                _simEcon.value = result.valEcon
                _simPsych.value = result.valPsych
                _simEco.value = result.valEco
                _simLaw.value = result.valLaw
                _simSec.value = result.valSec
                _simPhys.value = result.valPhys
                _simSoc.value = result.valSoc
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
        // Export logic stub
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

    fun onClearTelemetry() {
        viewModelScope.launch {
            repository.omnisDao.clearTelemetry()
            com.example.telemetry.TelemetryEngine.log("INFO", "Admin", "Telemetrická data byla smazána.")
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
}
