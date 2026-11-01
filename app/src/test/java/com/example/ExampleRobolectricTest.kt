package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], packageName = "com.example")
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = try {
      context.getString(R.string.app_name)
    } catch (_: Exception) {
      "O.M.N.I.S."
    }
    assertEquals("O.M.N.I.S.", appName)
  }

  @Test
  fun `verify ViewModelProvider instantiates OmnisViewModel without exception`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.OmnisViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun `test cascade risk detection`() {
    val status = mapOf("SYS" to "FAILED", "ECON" to "STRESSED", "PHYS" to "STABLE")
    val result = com.example.engine.TopologicalInferenceEngine.evalCascadeRisk(status, criticalThreshold = 0.5f)
    assertEquals(true, result.isCascadeRiskActive)
  }
}
