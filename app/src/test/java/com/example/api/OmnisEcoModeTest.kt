package com.example.api

import androidx.test.core.app.ApplicationProvider
import com.example.auth.UserRole
import com.example.gateway.RefusalLadderEngine
import com.example.ui.OmnisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class OmnisEcoModeTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        OmnisGeminiClient.ioDispatcher = testDispatcher
        OmnisGeminiClient.customApiService = object : OmnisApiService {
            override suspend fun processHybridIntent(userInput: String): HybridOmnisResponse {
                return HybridOmnisResponse(
                    intent = "intent_test",
                    confidenceScore = 0.95f,
                    executionPlan = emptyList(),
                    immediateResponse = "Test response for $userInput",
                    requiredOutputFormat = "text"
                )
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleEcoMode_in_OmnisViewModel_alternates_state_correctly() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = OmnisViewModel(app)

        assertTrue("Eco Mode should be enabled by default", vm.isEcoModeEnabled.value)

        vm.toggleEcoMode()
        assertFalse("Eco Mode should toggle to false", vm.isEcoModeEnabled.value)

        vm.toggleEcoMode()
        assertTrue("Eco Mode should toggle back to true", vm.isEcoModeEnabled.value)
    }

    @Test
    fun deterministic_synthesis_succeeds_in_eco_mode_with_valid_8D_footprint() = runTest {
        val result = OmnisGeminiClient.synthesize(
            query = "Navrhni škálovatelnou datovou vrstvu",
            domain = "SYS",
            isEcoMode = true
        )

        assertNotNull(result)
        assertTrue(result.answer.isNotEmpty())
        assertTrue(result.valSys >= 0.0f && result.valSys <= 1.0f)
        assertTrue(result.composite >= 0.0f && result.composite <= 1.0f)
        assertEquals("APPROVED", result.defenseTier)
    }

    @Test
    fun RefusalLadderVerdict_formats_properly_for_both_RBAC_roles() {
        val prompt = "Analyzuj dopad regulace na fintech sektor"
        val verdict = RefusalLadderEngine.evaluatePrompt(prompt)

        assertTrue(verdict.isApproved)
        assertEquals(6, verdict.evaluations.size)

        // Verify standard user and admin operator access flags
        assertTrue(UserRole.ADMIN_OPERATOR.canAccessDevDiagnostic())
        assertFalse(UserRole.STANDARD_USER.canAccessDevDiagnostic())
        assertTrue(UserRole.ADMIN_OPERATOR.canModifySystemState())
    }
}
