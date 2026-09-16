package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.OmnisRepository
import com.example.ui.OmnisViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val application = ApplicationProvider.getApplicationContext<Application>()
        fakeDao = FakeOmnisDao()
        repository = OmnisRepository(fakeDao)
        viewModel = OmnisViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
}
