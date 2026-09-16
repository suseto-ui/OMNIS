package com.example

import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.app.Application

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OmnisViewModelTest {

    private lateinit var viewModel: OmnisViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = OmnisViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial tab is CHAT`() {
        assertEquals(OmnisTab.CHAT, viewModel.activeTab.value)
    }

    @Test
    fun `test state transitions between all tabs`() {
        // Test CHAT to ANALYTICS
        viewModel.setTab(OmnisTab.ANALYTICS)
        assertEquals(OmnisTab.ANALYTICS, viewModel.activeTab.value)

        // Test ANALYTICS to MEMORY
        viewModel.setTab(OmnisTab.MEMORY)
        assertEquals(OmnisTab.MEMORY, viewModel.activeTab.value)

        // Test MEMORY to NODES
        viewModel.setTab(OmnisTab.NODES)
        assertEquals(OmnisTab.NODES, viewModel.activeTab.value)

        // Test NODES to DASHBOARD
        viewModel.setTab(OmnisTab.DASHBOARD)
        assertEquals(OmnisTab.DASHBOARD, viewModel.activeTab.value)

        // Test DASHBOARD to MATRIX
        viewModel.setTab(OmnisTab.MATRIX)
        assertEquals(OmnisTab.MATRIX, viewModel.activeTab.value)

        // Test MATRIX to DEV
        viewModel.setTab(OmnisTab.DEV)
        assertEquals(OmnisTab.DEV, viewModel.activeTab.value)
        
        // Test DEV to TEST_SEMANTIC
        viewModel.setTab(OmnisTab.TEST_SEMANTIC)
        assertEquals(OmnisTab.TEST_SEMANTIC, viewModel.activeTab.value)

        // Test TEST_SEMANTIC back to CHAT
        viewModel.setTab(OmnisTab.CHAT)
        assertEquals(OmnisTab.CHAT, viewModel.activeTab.value)
    }
}
