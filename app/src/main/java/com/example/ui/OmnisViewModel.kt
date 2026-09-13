package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.OmnisGeminiClient
import com.example.data.OmnisDatabase
import com.example.data.OmnisRecord
import com.example.data.OmnisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OmnisTab {
    CHAT,
    MATRIX,
    MEMORY
}

class OmnisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OmnisRepository
    val records: StateFlow<List<OmnisRecord>>

    private val _activeTab = MutableStateFlow(OmnisTab.CHAT)
    val activeTab: StateFlow<OmnisTab> = _activeTab.asStateFlow()

    private val _inputQuery = MutableStateFlow("")
    val inputQuery: StateFlow<String> = _inputQuery.asStateFlow()

    private val _selectedDomain = MutableStateFlow("SYSTEMS_INTELLIGENCE")
    val selectedDomain: StateFlow<String> = _selectedDomain.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    init {
        val db = OmnisDatabase.getDatabase(application)
        repository = OmnisRepository(db.omnisDao())
        records = repository.allRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Seed initial record if empty
        viewModelScope.launch {
            repository.allRecords.collect { list ->
                if (list.isEmpty()) {
                    val initial = OmnisRecord(
                        role = "assistant",
                        content = "Vítejte v O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Systém je aktivní v režimu přímé ontologické syntézy s reálným vyhodnocováním čtyřdimenzionální Matice dopadů.",
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

    fun sendQuery(customQuery: String? = null) {
        val query = (customQuery ?: _inputQuery.value).trim()
        if (query.isBlank() || _isLoading.value) return

        _inputQuery.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Save user record
                val userRecord = OmnisRecord(
                    role = "user",
                    content = query,
                    domain = _selectedDomain.value
                )
                repository.insert(userRecord)

                // Synthesize response via Gemini / cognitive engine
                val result = OmnisGeminiClient.synthesize(query, _selectedDomain.value)

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
                    domain = _selectedDomain.value
                )
                repository.insert(asstRecord)

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
                // Error handled gracefully
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
}
