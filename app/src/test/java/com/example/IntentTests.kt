package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.OmnisRepository
import com.example.ui.OmnisViewModel
import com.example.ui.UserExperienceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IntentTests {

    private lateinit var viewModel: OmnisViewModel
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeOmnisDao
    private lateinit var repository: OmnisRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        com.example.api.OmnisGeminiClient.ioDispatcher = testDispatcher
        com.example.api.OmnisGeminiClient.customApiService = object : com.example.api.OmnisApiService {
            override suspend fun processHybridIntent(userInput: String): com.example.api.HybridOmnisResponse {
                return com.example.api.HybridOmnisResponse(
                    intent = "intent_test",
                    confidenceScore = 0.95f,
                    executionPlan = emptyList(),
                    immediateResponse = "Test response for $userInput",
                    requiredOutputFormat = "text"
                )
            }
        }
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeDao = FakeOmnisDao()
        repository = OmnisRepository(fakeDao)
        viewModel = OmnisViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        com.example.api.OmnisGeminiClient.ioDispatcher = Dispatchers.IO
        com.example.api.OmnisGeminiClient.customApiService = null
    }

    @Test
    fun `test OCR validation state update and cancel cleanup`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dummyFile = File(app.filesDir, "test_ocr_temp.jpg")
        dummyFile.writeText("sample image binary")

        // Directly set OCR validation state via simulated initial data
        viewModel.updateOcrValidationText("Původní extrahovaný text")
        assertNull(viewModel.ocrValidationState.value) // Should be null before any state is initialized

        // Create initial state
        val dummyState = OmnisViewModel.OcrValidationState(
            imageLocalPath = dummyFile.absolutePath,
            extractedText = "OCR Detekovaný řetězec"
        )
        // Set via reflection or simulate flow
        val field = OmnisViewModel::class.java.getDeclaredField("_ocrValidationState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OmnisViewModel.OcrValidationState?>
        stateFlow.value = dummyState

        assertEquals("OCR Detekovaný řetězec", viewModel.ocrValidationState.value?.extractedText)

        // Test editing extracted OCR text
        viewModel.updateOcrValidationText("Opravený operátorem text")
        assertEquals("Opravený operátorem text", viewModel.ocrValidationState.value?.extractedText)
        assertEquals(dummyFile.absolutePath, viewModel.ocrValidationState.value?.imageLocalPath)

        // Test Cancel OCR cleans up file and clears state
        viewModel.cancelOcrValidation()
        assertNull(viewModel.ocrValidationState.value)
        assertEquals(false, dummyFile.exists())
    }

    @Test
    fun `test OCR confirmation dispatches query and clears validation state`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dummyFile = File(app.filesDir, "test_ocr_confirm.jpg")
        dummyFile.writeText("sample image binary")

        val field = OmnisViewModel::class.java.getDeclaredField("_ocrValidationState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<OmnisViewModel.OcrValidationState?>
        stateFlow.value = OmnisViewModel.OcrValidationState(
            imageLocalPath = dummyFile.absolutePath,
            extractedText = "Validovaný dotaz z dokumentu"
        )

        viewModel.confirmOcrValidation()
        advanceUntilIdle()

        // Validation dialog should be dismissed
        assertNull(viewModel.ocrValidationState.value)

        // Records should now contain the confirmed user query
        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content == "Validovaný dotaz z dokumentu" } }
        val userRecord = records.find { it.role == "user" && it.content == "Validovaný dotaz z dokumentu" }
        assertNotNull(userRecord)
        assertEquals(dummyFile.absolutePath, userRecord?.attachedImagePath)

        if (dummyFile.exists()) dummyFile.delete()
    }

    @Test
    fun `test domain selection toggle and multi-domain synthesis trigger`() = runTest(testDispatcher) {
        assertEquals(emptySet<String>(), viewModel.selectedDomains.value)

        // Toggle selections
        viewModel.toggleDomainSelection("Sys")
        viewModel.toggleDomainSelection("Econ")
        assertEquals(setOf("Sys", "Econ"), viewModel.selectedDomains.value)

        // Toggle remove "Sys"
        viewModel.toggleDomainSelection("Sys")
        assertEquals(setOf("Econ"), viewModel.selectedDomains.value)

        // Add back and add another
        viewModel.toggleDomainSelection("Sec")
        assertEquals(setOf("Econ", "Sec"), viewModel.selectedDomains.value)

        // Clear selection
        viewModel.clearSelection()
        assertEquals(emptySet<String>(), viewModel.selectedDomains.value)
    }

    @Test
    fun `test runMultiDomainSynthesis generates formatted prompt and dispatches query`() = runTest(testDispatcher) {
        // Pre-insert an assistant record so synthesis can target it
        val assistantRecord = com.example.data.OmnisRecord(
            id = 42L,
            role = "assistant",
            content = "Strategický rozbor infrastruktury",
            valSys = 0.85f,
            valEcon = 0.70f,
            compositeScore = 0.77f
        )
        fakeDao.insertRecord(assistantRecord)
        advanceUntilIdle()

        viewModel.toggleDomainSelection("Sys")
        viewModel.toggleDomainSelection("Econ")

        viewModel.runMultiDomainSynthesis("COMPARE")
        advanceUntilIdle()

        // Selection should be cleared after dispatch
        assertEquals(emptySet<String>(), viewModel.selectedDomains.value)

        // Records should have recorded the user query with domain comparison prompt
        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content.contains("Sys, Econ") } }
        val userQuery = records.find { it.role == "user" && it.content.contains("Sys, Econ") }
        assertNotNull(userQuery)
        assertEquals(true, userQuery?.content?.contains("srovnávací analýzu"))
    }

    @Test
    fun `test runMultiDomainSynthesis injects friction diagnosis when conflicting domains selected`() = runTest(testDispatcher) {
        val assistantRecord = com.example.data.OmnisRecord(
            id = 100L,
            role = "assistant",
            content = "Výchozí analýza investičních priorit",
            valEcon = 0.90f,
            valEco = 0.40f,
            compositeScore = 0.65f
        )
        fakeDao.insertRecord(assistantRecord)
        advanceUntilIdle()

        viewModel.toggleDomainSelection("Econ")
        viewModel.toggleDomainSelection("Eco")

        viewModel.runMultiDomainSynthesis("HARMONIZE")
        advanceUntilIdle()

        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content.contains("Econ, Eco") } }
        val userQuery = records.find { it.role == "user" && it.content.contains("Econ, Eco") }
        assertNotNull(userQuery)
        assertTrue(userQuery!!.content.contains("DETEKOVÁNA INTERFERENCE"))
        assertTrue(userQuery.content.contains("Econ vs Eco"))
    }

    @Test
    fun `test Prompt Gateway intercepts ambiguous query and elevates semantics`() = runTest(testDispatcher) {
        viewModel.setUserExperienceMode(UserExperienceMode.EXPERT)
        com.example.auth.OmnisAuthService.setRoleForTesting(com.example.auth.UserRole.ADMIN_OPERATOR)
        if (!viewModel.isPromptGatewayEnabled.value) {
            viewModel.togglePromptGateway()
        }

        // Vágní, krátký dotaz bez kontextu a parametrů
        val vagueQuery = "jak to udělat"
        viewModel.sendQuery(vagueQuery)
        advanceUntilIdle()

        // Nesmí být odesláno přímo do databáze (zadrženo pro HITL revizi)
        val reviewState = viewModel.pendingGatewayReview.value
        assertNotNull(reviewState)
        assertEquals("needs_review", reviewState?.status)
        assertEquals(vagueQuery, reviewState?.originalPrompt)
        assertTrue(reviewState!!.suggestedPrompt.contains("### [SÉMANTICKÉ ZADÁNÍ PRO O.M.N.I.S. CORE]"))
        assertTrue(reviewState.suggestedPrompt.contains("[DOPLŇTE"))

        // Potvrzení optimalizovaného promptu operátorem
        val customConfirmedPrompt = "Navrhni a implementuj bezpečný token bucket v Kotlinu"
        viewModel.confirmGatewayReview(customConfirmedPrompt)
        advanceUntilIdle()

        // Brána uvolněna a dotaz propsán do záznamů
        assertNull(viewModel.pendingGatewayReview.value)
        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content == customConfirmedPrompt } }
        assertNotNull(records)
    }

    @Test
    fun `test Prompt Gateway bypass with original prompt upon operator demand`() = runTest(testDispatcher) {
        viewModel.setUserExperienceMode(UserExperienceMode.EXPERT)
        com.example.auth.OmnisAuthService.setRoleForTesting(com.example.auth.UserRole.ADMIN_OPERATOR)
        if (!viewModel.isPromptGatewayEnabled.value) {
            viewModel.togglePromptGateway()
        }

        val vagueQuery = "pomoz"
        viewModel.sendQuery(vagueQuery)
        advanceUntilIdle()

        assertNotNull(viewModel.pendingGatewayReview.value)
        viewModel.bypassGatewayReview()
        advanceUntilIdle()

        assertNull(viewModel.pendingGatewayReview.value)
        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content == vagueQuery } }
        assertNotNull(records)
    }

    @Test
    fun `test Prompt Gateway allows mature high quality query to bypass gate directly`() = runTest(testDispatcher) {
        viewModel.setUserExperienceMode(UserExperienceMode.EXPERT)
        com.example.auth.OmnisAuthService.setRoleForTesting(com.example.auth.UserRole.ADMIN_OPERATOR)
        if (!viewModel.isPromptGatewayEnabled.value) {
            viewModel.togglePromptGateway()
        }

        val richQuery = "Navrhni a implementuj distribuovanou databázovou architekturu s nízkou latencí v Kotlinu"
        viewModel.sendQuery(richQuery)
        advanceUntilIdle()

        // Žádný HITL dialog se nezobrazí, dotaz jde přímo na exekuci
        assertNull(viewModel.pendingGatewayReview.value)
        val records = viewModel.records.first { list -> list.any { it.role == "user" && it.content == richQuery } }
        assertNotNull(records)
    }
}
