package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.api.ExecutionPlanStep
import com.example.api.HybridOmnisResponse
import com.example.api.OmnisApiService
import com.example.api.OmnisGeminiClient
import com.example.api.ToolCallSpec
import com.example.data.OmnisDao
import com.example.data.OmnisRecord
import com.example.data.OmnisRepository
import com.example.ui.OmnisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

class FakeOmnisDao : OmnisDao {
    private val recordsFlow = MutableStateFlow<List<OmnisRecord>>(emptyList())

    override fun getAllRecords(): Flow<List<OmnisRecord>> = recordsFlow

    override fun getLatestRecord(): Flow<OmnisRecord?> = recordsFlow.map { it.lastOrNull() }

    override suspend fun insertRecord(record: OmnisRecord): Long {
        recordsFlow.value = recordsFlow.value + record
        return recordsFlow.value.size.toLong()
    }

    override suspend fun clearAll() {
        recordsFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OmnisQueryOutputVerificationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeDao: FakeOmnisDao
    private lateinit var repository: OmnisRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        OmnisGeminiClient.ioDispatcher = testDispatcher
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakeOmnisDao()
        repository = OmnisRepository(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        OmnisGeminiClient.ioDispatcher = Dispatchers.IO
        OmnisGeminiClient.customApiService = null
    }

    @Test
    fun `verify successful query submission produces structured 8D output and updates state`() = runTest(testDispatcher) {
        // Arrange mock API returning a deterministic HybridOmnisResponse
        val fakeResponse = HybridOmnisResponse(
            intent = "systems_architecture",
            confidenceScore = 0.94f,
            executionPlan = listOf(
                ExecutionPlanStep(
                    stepNumber = 1,
                    actionDescription = "Provést sémantickou dekonstrukci dotazu a mapování entit",
                    toolCall = ToolCallSpec(
                        toolId = "semantic_analyzer",
                        parameters = mapOf("depth" to "exhaustive")
                    )
                ),
                ExecutionPlanStep(
                    stepNumber = 2,
                    actionDescription = "Vypočítat 8D tenzory a izolovat pákový bod systému",
                    toolCall = ToolCallSpec(
                        toolId = "impact_matrix_engine",
                        parameters = mapOf("dimensions" to 8)
                    )
                )
            ),
            immediateResponse = "Strukturovaný návrh distribuované architektury byl úspěšně vygenerován.",
            requiredOutputFormat = "markdown"
        )

        OmnisGeminiClient.customApiService = object : OmnisApiService {
            override suspend fun processHybridIntent(userInput: String): HybridOmnisResponse {
                return fakeResponse
            }
        }

        val viewModel = OmnisViewModel(application, repository)
        advanceUntilIdle()

        // Act: Enter query and submit
        val testQuery = "Jak optimalizovat distribuovaný výpočetní uzel pro nulovou chybovost?"
        viewModel.onQueryChange(testQuery)
        assertEquals(testQuery, viewModel.inputQuery.value)

        viewModel.sendQuery()
        // Input query should immediately be cleared
        assertEquals("", viewModel.inputQuery.value)

        advanceUntilIdle()

        // Assert: Verify state and output functionality
        assertFalse(viewModel.isLoading.value)
        assertEquals(0.94f, viewModel.simSec.value, 0.02f)
        assertEquals(0.90f, viewModel.simSys.value, 0.02f)

        val records = viewModel.records.first { it.size >= 2 }
        val userRecord = records.find { it.role == "user" }
        val asstRecord = records.find { it.role == "assistant" && it.content.contains("Strukturovaný návrh") }

        assertNotNull("User record must be persisted", userRecord)
        assertEquals(testQuery, userRecord?.content)

        assertNotNull("Assistant record must be persisted", asstRecord)
        assertTrue("Output answer must match synthesized content", asstRecord!!.content.contains("Strukturovaný návrh"))
        assertTrue("Cognitive thoughts must detail intent and plan", asstRecord.cognitiveProcess.contains("systems_architecture"))
        assertTrue("Follow-up questions must be populated", asstRecord.followUpQuestions.isNotBlank())
        assertEquals(0.94f, asstRecord.compositeScore, 0.01f)
        assertEquals(0.9f, asstRecord.valSys, 0.01f)
        assertEquals(0.8f, asstRecord.valEcon, 0.01f)
        assertEquals(1.0f, asstRecord.valLaw, 0.01f)
    }

    @Test
    fun `verify zero-simulation policy when gateway is unreachable`() = runTest(testDispatcher) {
        // Arrange: API throws network failure exception
        OmnisGeminiClient.customApiService = object : OmnisApiService {
            override suspend fun processHybridIntent(userInput: String): HybridOmnisResponse {
                throw IOException("Unable to resolve host: gateway.omnis.cloud")
            }
        }

        val viewModel = OmnisViewModel(application, repository)
        advanceUntilIdle()

        // Act: Submit query
        viewModel.sendQuery("Test výpadku konektivity")
        advanceUntilIdle()

        // Assert: Zero-simulation policy must be strictly honored
        assertFalse(viewModel.isLoading.value)

        // All simulation dimensions must drop to 0.0f
        assertEquals(0.0f, viewModel.simSys.value, 0.001f)
        assertEquals(0.0f, viewModel.simEcon.value, 0.001f)
        assertEquals(0.0f, viewModel.simLaw.value, 0.001f)
        assertEquals(0.0f, viewModel.simSec.value, 0.001f)

        val records = fakeDao.getAllRecords().first { it.size >= 2 }
        val errorRecord = records.find { it.role == "assistant" && it.compositeScore == 0.0f }
        assertNotNull("Failure record must be recorded in Room database", errorRecord)
        assertTrue(
            "Answer must communicate gateway failure without hallucinating data",
            errorRecord!!.content.contains("Kritické selhání při komunikaci s O.M.N.I.S. Gateway")
        )
        assertEquals("Network Failure / Schema Mismatch", errorRecord.cognitiveProcess)
        assertEquals(0.0f, errorRecord.compositeScore, 0.001f)
    }

    @Test
    fun `verify default runtime produces real structured cognitive result and no error message`() = runTest(testDispatcher) {
        // Ensure customApiService is null (default production runtime)
        OmnisGeminiClient.customApiService = null

        val viewModel = OmnisViewModel(application, repository)
        advanceUntilIdle()

        // Act: User enters "znovu"
        val query = "znovu"
        viewModel.onQueryChange(query)
        viewModel.sendQuery()
        advanceUntilIdle()

        // Assert: Result must be a real synthesis, not an error
        assertFalse(viewModel.isLoading.value)
        assertTrue("System dimension must be non-zero", viewModel.simSys.value > 0.5f)
        assertTrue("Security dimension must be non-zero", viewModel.simSec.value > 0.5f)

        val records = fakeDao.getAllRecords().first { it.size >= 2 }
        val asstRecord = records.find { it.role == "assistant" && it.content.contains("rekurzivní") || it.content.contains("O.M.N.I.S.") }
        assertNotNull("Assistant synthesized record must be present", asstRecord)
        assertFalse("Output must not contain error message", asstRecord!!.content.contains("Kritické selhání"))
        assertTrue("Cognitive thoughts must be populated", asstRecord.cognitiveProcess.isNotBlank())
        assertTrue("Follow up questions must be populated", asstRecord.followUpQuestions.isNotBlank())
        assertTrue("Composite score must be positive", asstRecord.compositeScore > 0.5f)
    }
}

