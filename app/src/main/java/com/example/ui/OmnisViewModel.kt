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
    private val _simEconomic = MutableStateFlow(0.85f)
    val simEconomic: StateFlow<Float> = _simEconomic.asStateFlow()

    private val _simEcoSocial = MutableStateFlow(0.92f)
    val simEcoSocial: StateFlow<Float> = _simEcoSocial.asStateFlow()

    private val _simTech = MutableStateFlow(0.96f)
    val simTech: StateFlow<Float> = _simTech.asStateFlow()

    private val _simPsych = MutableStateFlow(0.89f)
    val simPsych: StateFlow<Float> = _simPsych.asStateFlow()

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
                        cognitiveProcess = "1. Inicializace subsystému O.M.N.I.S.\n2. Napojení na ontologický rámec.\n3. Výpočet bazálních tenzorů: Eko=92%, Ekon=85%, Tech=96%, Psych=89%.",
                        followUpQuestions = "Jak provázat ekonomické pobídky s ekologickou regenerací?|Jak navrhnout distribuovanou architekturu s nulovou energetickou stopou?",
                        economicViability = 0.85f,
                        ecoSocialRegeneration = 0.92f,
                        technologicalElegance = 0.96f,
                        psychologicalAcceptability = 0.89f,
                        compositeScore = 0.905f,
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

    fun setSimEconomic(v: Float) { _simEconomic.value = v }
    fun setSimEcoSocial(v: Float) { _simEcoSocial.value = v }
    fun setSimTech(v: Float) { _simTech.value = v }
    fun setSimPsych(v: Float) { _simPsych.value = v }

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
                    economicViability = result.economic,
                    ecoSocialRegeneration = result.ecoSocial,
                    technologicalElegance = result.technological,
                    psychologicalAcceptability = result.psychological,
                    compositeScore = result.composite,
                    domain = _selectedDomain.value
                )
                repository.insert(asstRecord)

                // Update simulation sliders to match latest result
                _simEconomic.value = result.economic
                _simEcoSocial.value = result.ecoSocial
                _simTech.value = result.technological
                _simPsych.value = result.psychological
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
