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
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OmnisTab {
    CHAT,
    ANALYTICS,
    MEMORY,
    NODES,
    ADMIN,
    DASHBOARD,
    MATRIX,
    TEST_SEMANTIC
}

class OmnisViewModel(
    application: Application,
    customRepository: OmnisRepository? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, null)

    init {
        OmnisAuthService.init(application)
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
    val records: StateFlow<List<OmnisRecord>>

    private val _activeTab = MutableStateFlow(OmnisTab.CHAT)
    val activeTab: StateFlow<OmnisTab> = _activeTab.asStateFlow()

    // System Circuit Breaker (Hlavní Jistič)
    private val _isCircuitBreakerTripped = MutableStateFlow(false)
    val isCircuitBreakerTripped: StateFlow<Boolean> = _isCircuitBreakerTripped.asStateFlow()

    fun toggleCircuitBreaker() {
        val currentRole = OmnisAuthService.currentUserRole.value
        if (!currentRole.canAccessSystemActions()) {
            _errorMessage.value = "Přístup odepřen: Ovládání hlavního jističe vyžaduje roli Admin / Operátor."
            return
        }
        _isCircuitBreakerTripped.value = !_isCircuitBreakerTripped.value
    }

    // Action-Driven Dispatcher State
    private val _lastActionResult = MutableStateFlow<ActionExecutionResult?>(null)
    val lastActionResult: StateFlow<ActionExecutionResult?> = _lastActionResult.asStateFlow()

    private val _isActionExecuting = MutableStateFlow(false)
    val isActionExecuting: StateFlow<Boolean> = _isActionExecuting.asStateFlow()

    fun executeActionPayload(payload: ActionPayload) {
        if (_isActionExecuting.value) return
        _isActionExecuting.value = true

        viewModelScope.launch {
            try {
                // RBAC Kontrola: Pouze operátor může spouštět systémové akce
                val currentRole = OmnisAuthService.currentUserRole.value
                if (!currentRole.canAccessSystemActions()) {
                    _errorMessage.value = "Přístup odepřen: Akční dispečer vyžaduje roli Admin / Operátor."
                    _isActionExecuting.value = false
                    return@launch
                }

                // 1. Záznam volání do chatu jako uživatelský/systémový JSON příkaz
                val userActionRecord = OmnisRecord(
                    role = "user",
                    content = "⚡ [ACTION DISPATCH] Volání intentu: `${payload.intent}` | Akce: `${payload.actionId}`\n\n```json\n${payload.toJsonString()}\n```",
                    domain = _selectedDomain.value
                )
                repository.insert(userActionRecord)

                // 2. Nativní backend dispatcher s ochranou timeoutu (10 sekund)
                val result = kotlinx.coroutines.withTimeoutOrNull(10000L) {
                    OmnisActionDispatcher.executeAction(payload)
                } ?: ActionExecutionResult(
                    actionId = payload.actionId,
                    isSuccess = false,
                    statusCode = 504,
                    logs = listOf("ERROR: Exekuce akce překročila maximální časový limit (10s Timeout Guard)"),
                    summaryReport = "TIMEOUT: Systémová akce nebylo dokončena v limitu 10.000 ms."
                )
                _lastActionResult.value = result

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
                    defenseTier = "DISPATCH_EXECUTED"
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

    private val _selectedDomain = MutableStateFlow("SYSTEMS_INTELLIGENCE")
    val selectedDomain: StateFlow<String> = _selectedDomain.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
        sendQuery(state.extractedText, state.imageLocalPath)
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
                val textWithFile = withContext(Dispatchers.IO) {
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
                    withContext(Dispatchers.IO) {
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
        records = repository.allRecords.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        // Seed initial record if empty
        viewModelScope.launch {
            val list = repository.allRecords.first()
            if (list.isEmpty()) {
                val initial = OmnisRecord(
                    role = "assistant",
                    content = "Vítejte v O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Systém je aktivní v režimu přímé ontologické syntézy s reálným vyhodnocováním č[...]",
                    cognitiveProcess = "1. Inicializace subsystému O.M.N.I.S.\n2. Napojení na ontologický rámec.\n3. Výpočet bazálních tenzorů napříč 8 doménami.",
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
                    domain = "SYSTEMS_INTELLIGENCE"
                )
                repository.insert(initial)
            }
        }
    }

    fun setTab(tab: OmnisTab) {
        _activeTab.value = tab
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

    fun sendQuery(customQuery: String? = null, imagePath: String? = null) {
        val query = (customQuery ?: _inputQuery.value).trim()
        if (query.isBlank() || _isLoading.value) return

        // 1. & 2. FÁZE: Evaluátor & Sémantická brána (Quality Gate)
        val gatewayResult = OmnisPromptGateway.processPromptGateway(query, _selectedDomain.value)
        if (gatewayResult.status == "needs_review") {
            // Zadržet exekuci a předat k Human-in-the-Loop revizi
            pendingGatewayImagePath = imagePath
            _pendingGatewayReview.value = gatewayResult
            _inputQuery.value = ""
            return
        }

        // Pokud je schválen bypass (vysoká specificita), pokračovat přímo do exekuce
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

        viewModelScope.launch {
            try {
                // Save user record
                val userRecord = OmnisRecord(
                    role = "user",
                    content = trimmedQuery,
                    domain = _selectedDomain.value,
                    attachedImagePath = imagePath
                )
                repository.insert(userRecord)

                // Synthesize response via Gemini / cognitive engine
                val result = OmnisGeminiClient.synthesize(trimmedQuery, _selectedDomain.value)

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
                    defenseNotes = result.defenseNotes
                )
                repository.insert(asstRecord)

                // AUTOMATICKÝ ZÁPIS DO POSTGRESQL (Replikace na pozadí)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        OmnisActionDispatcher.executeAction(
                            ActionPayload(
                                intent = "auto_postgres_replication",
                                actionId = "postgres_auto_sync",
                                parameters = mapOf("unsynced_count" to 2, "engine" to "PostgreSQL Cloud SQL")
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("OmnisViewModel", "PostgreSQL auto-replication failed", e)
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
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clear()
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
}
