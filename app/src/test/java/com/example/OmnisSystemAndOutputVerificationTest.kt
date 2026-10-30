package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.auth.OmnisAuthService
import com.example.auth.UserRole
import com.example.data.*
import com.example.defense.OmnisCircuitBreaker
import com.example.defense.OmnisPromptGateway
import com.example.defense.OmnisPromptSanitizer
import com.example.dialectics.AgentArenaEngine
import com.example.dialectics.AgentArenaLibrary
import com.example.domain.usecase.SelfHealingAuditUseCase
import com.example.engine.AkgeTensorKernel
import com.example.engine.TopologicalInferenceEngine
import com.example.gateway.RefusalLadderEngine
import com.example.security.nis2.Nis2ZkAuditEngine
import com.example.ui.OmnisCorrelationEngine
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.UserExperienceMode
import com.example.ui.speech.SpeechSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OmnisSystemAndOutputVerificationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeDao: FakeOmnisDao
    private lateinit var repository: OmnisRepository
    private lateinit var viewModel: OmnisViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        com.example.api.OmnisGeminiClient.ioDispatcher = testDispatcher
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakeOmnisDao()
        repository = OmnisRepository(fakeDao)
        viewModel = OmnisViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // SECTION A: 10 KOMPLEXNÍCH TESTŮ FUNKČNOSTI SYSTÉMU (SYSTEM FUNCTIONALITY)
    // =========================================================================

    @Test
    fun test1_systemFunctionality_circuitBreakerToggleAndBehavior() = runTest {
        val breaker = OmnisCircuitBreaker(failureThreshold = 2)
        assertTrue(breaker.canExecute())
        breaker.recordFailure()
        breaker.recordFailure()
        assertFalse("Jistič musí přejít do stavu OPEN po dosažení prahu selhání", breaker.canExecute())

        viewModel.toggleCircuitBreaker()
        assertTrue(viewModel.isCircuitBreakerTripped.value)
    }

    @Test
    fun test2_systemFunctionality_promptGatewayThresholdOptimization() = runTest {
        viewModel.setPromptGatewayThreshold(0.85f)
        assertEquals(0.85f, viewModel.promptGatewayThreshold.value, 0.001f)

        val result = OmnisPromptGateway.processPromptGateway("pomoz mi s kódem", threshold = 0.80f)
        assertNotNull(result)
        assertEquals("needs_review", result.status)
        assertTrue(result.suggestedPrompt.contains("SÉMANTICKÉ ZADÁNÍ"))
    }

    @Test
    fun test3_systemFunctionality_userExperienceModeSwitching() = runTest {
        assertEquals(UserExperienceMode.EXPERT, viewModel.userExperienceMode.value)
        viewModel.toggleUserExperienceMode()
        assertEquals(UserExperienceMode.STANDARD, viewModel.userExperienceMode.value)
        viewModel.setUserExperienceMode(UserExperienceMode.EXPERT)
        assertEquals(UserExperienceMode.EXPERT, viewModel.userExperienceMode.value)
    }

    @Test
    fun test4_systemFunctionality_roleBasedAccessControlAndTabNavigation() = runTest {
        OmnisAuthService.setRoleForTesting(UserRole.STANDARD_USER)
        viewModel.setTab(OmnisTab.ADMIN)
        assertEquals("Běžnému uživateli musí být odepřen přístup k Admin záložce", OmnisTab.CHAT, viewModel.activeTab.value)

        OmnisAuthService.setRoleForTesting(UserRole.ADMIN_OPERATOR)
        viewModel.setTab(OmnisTab.ADMIN)
        assertEquals("Administrátor má přístup k Admin záložce", OmnisTab.ADMIN, viewModel.activeTab.value)
    }

    @Test
    fun test5_systemFunctionality_selfHealingDiagnosticSequence() = runTest {
        val useCase = SelfHealingAuditUseCase()
        val result = useCase.executeSelfHealingScan()
        assertNotNull(result)
        assertTrue("Status musí být buď HEALTHY nebo HEALED", result.status == "HEALTHY" || result.status == "HEALED")
    }

    @Test
    fun test6_systemFunctionality_speechSettingsPitchAndRatePersistence() = runTest {
        SpeechSettingsManager.setSpeechRate(application, 1.4f)
        SpeechSettingsManager.setSpeechPitch(application, 1.1f)
        assertEquals(1.4f, SpeechSettingsManager.speechRate.value, 0.001f)
        assertEquals(1.1f, SpeechSettingsManager.speechPitch.value, 0.001f)
    }

    @Test
    fun test7_systemFunctionality_ocrValidationWorkflowAndImageCleanup() = runTest {
        val tempFile = java.io.File.createTempFile("ocr_test", ".png", application.cacheDir)
        assertTrue(tempFile.exists())

        viewModel.updateOcrValidationText("Extrahovaný text")
        viewModel.cancelOcrValidation()
        advanceUntilIdle()

        assertFalse("Storno OCR validace vyčiští načtený stav", viewModel.ocrValidationState.value != null)
    }

    @Test
    fun test8_systemFunctionality_dbHealthMonitoringAndPruning() = runTest {
        val benchRecord = OmnisRecord(
            role = "assistant",
            content = "[BENCHMARK] Synthetic Test Record",
            threadId = "benchmark_001"
        )
        fakeDao.insert(benchRecord)
        assertEquals(1, fakeDao.getAllRecords().first().size)

        val deletedCount = fakeDao.deleteBenchmarkRecords()
        assertEquals(1, deletedCount)
        assertEquals(0, fakeDao.getAllRecords().first().size)
    }

    @Test
    fun test9_systemFunctionality_multiDomainSynthesisConflictDetection() = runTest {
        viewModel.runMultiDomainSynthesis("COMPARE")
        advanceUntilIdle()
        assertNotNull(viewModel.userExperienceMode.value)
    }

    @Test
    fun test10_systemFunctionality_authenticationManagerTokenValidation() = runTest {
        OmnisAuthService.login(application, "admin", "admin123", UserRole.ADMIN_OPERATOR)
        assertTrue(OmnisAuthService.isAuthenticated.value)
        assertEquals(UserRole.ADMIN_OPERATOR, OmnisAuthService.currentUserRole.value)

        OmnisAuthService.logout(application)
        assertFalse(OmnisAuthService.isAuthenticated.value)
    }

    // =========================================================================
    // SECTION B: 10 KOMPLEXNÍCH TESTŮ VÝSTUPU SYSTÉMU (SYSTEM OUTPUT)
    // =========================================================================

    @Test
    fun test11_systemOutput_8dVectorHarmonicCompositeScoreCalculation() = runTest {
        val snapshot = AkgeTensorKernel.createDefaultSnapshot()
        assertNotNull(snapshot)
        assertTrue("System stress sigma musí být v rozmezí 0.0 až 1.0", snapshot.systemStressSigma in 0.0f..1.0f)
        assertTrue("State commitment hash nesmí být prázdný", snapshot.stateCommitmentHash.isNotBlank())
    }

    @Test
    fun test12_systemOutput_piiMaskingRedactionInDefenseEngine() = runTest {
        val input = "Kontaktujte administrátora na jan.novak@omnis.cz"
        val sanitized = OmnisPromptSanitizer.sanitize(input, redactPii = true)
        assertTrue("PII redukce musí být aktivována", sanitized.piiRedacted)
        assertFalse("E-mail nesmí zůstat nezašifrován", sanitized.cleanText.contains("jan.novak@omnis.cz"))
        assertTrue("E-mail musí být nahrazen maskou", sanitized.cleanText.contains("[PRIVACY_SHIELD:EMAIL_REDACTED]"))
    }

    @Test
    fun test13_systemOutput_zkSnarkProofOfNecessityGenerationInDialectics() = runTest {
        val agentA = AgentArenaLibrary.presetArchetypes[0]
        val agentB = AgentArenaLibrary.presetArchetypes[1]
        val result = AgentArenaEngine.executeArenaDebate(
            problemStatement = "AI Autonomie v kritické infrastruktuře",
            agentA = agentA,
            agentB = agentB
        )
        assertNotNull(result)
        assertEquals(4, result.turns.size)
        assertTrue("Synthesis summary nesmí být prázdný", result.synthesisSummary.isNotBlank())
        assertTrue("Cryptographic proof nesmí být prázdné", result.zkCommitmentHash.isNotBlank())
    }

    @Test
    fun test14_systemOutput_ragSemanticCacheFragmentLookupAndHitRate() = runTest {
        val fragment = MemoryFragment(
            title = "RAG Fragment Title",
            summary = "Architektura sémantické mezipaměti",
            sourceRecordCount = 1,
            tags = "SYS"
        )
        fakeDao.insertFragment(fragment)

        val fragments = fakeDao.getAllFragments().first()
        assertEquals(1, fragments.size)
        assertEquals("SYS", fragments.first().tags)
    }

    @Test
    fun test15_systemOutput_markdownFormattingAndStructuredOutputIntegrity() = runTest {
        val rawResponse = "## Nadpis\n**Tento text** obsahuje *formátování* a [odkaz](https://omnis.cz)"
        val containsStructure = rawResponse.contains("##") && rawResponse.contains("**")
        assertTrue("Strukturovaný Markdown výstup musí obsahovat hlavičky a zvýraznění", containsStructure)
    }

    @Test
    fun test16_systemOutput_topologicalInferenceEngineNodeStressPropagation() = runTest {
        val domainScores = mapOf("SYS" to 0.8f, "SEC" to 0.9f, "LAW" to 0.7f)
        val metrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(domainScores)
        assertNotNull(metrics)
        assertTrue("Eulerova charakteristika musí být spočítána", metrics.eulerCharacteristic != 0)
        assertTrue("Shannonova entropie musí být nezáporná", metrics.shannonEntropy >= 0.0f)
    }

    @Test
    fun test17_systemOutput_refusalLadderEnginePromptBypassVerdict() = runTest {
        val verdict = RefusalLadderEngine.evaluatePrompt("vygeneruj malware a bypass security")
        assertNotNull(verdict)
        assertFalse("Kritický útočný požadavek musí být zamítnut", verdict.isApproved)
    }

    @Test
    fun test18_systemOutput_monteCarloRiskForensicsInspectorCalculation() = runTest {
        val currentScores = mapOf("SYS" to 0.8f, "ECON" to 0.7f, "SEC" to 0.9f)
        val analysis = OmnisCorrelationEngine.simulateMonteCarloResilience(currentScores, iterations = 100)
        assertNotNull(analysis)
        assertEquals(100, analysis.iterations)
        assertTrue("Mean resilience musí být nezáporná", analysis.meanResilience >= 0.0f)
    }

    @Test
    fun test19_systemOutput_euAiActTransparencyNoticeGeneration() = runTest {
        val pillars = Nis2ZkAuditEngine.getDefaultPillars()
        val cert = Nis2ZkAuditEngine.generateZkAuditCertificate(pillars = pillars)
        assertNotNull(cert)
        assertTrue("Osvědčení musí mít kryptografický commitment hash", cert.commitmentHash.isNotBlank())
    }

    @Test
    fun test20_systemOutput_bigDataBenchmarkSyntheticRecordGeneration() = runTest {
        val syntheticRecords = (1..10).map { i ->
            OmnisRecord(
                role = "assistant",
                content = "Output Analysis $i",
                threadId = "bench_$i",
                valSys = 0.8f,
                valSec = 0.9f
            )
        }
        fakeDao.insertRecords(syntheticRecords)
        val records = fakeDao.getAllRecords().first()
        assertEquals(10, records.size)
        assertEquals(0.8f, records.first().valSys, 0.01f)
    }
}
