package com.example

import com.example.ui.OmnisCorrelationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OmnisCorrelationEngineTest {

    @Test
    fun `test identity correlation returns 100 percent`() {
        val corr = OmnisCorrelationEngine.getCorrelation("Sys", "Sys")
        assertEquals(1.0f, corr.correlation, 0.001f)
    }

    @Test
    fun `test symmetric correlation lookup`() {
        val corrAB = OmnisCorrelationEngine.getCorrelation("Sys", "Sec")
        val corrBA = OmnisCorrelationEngine.getCorrelation("Sec", "Sys")
        assertEquals(corrAB.correlation, corrBA.correlation, 0.001f)
        assertTrue(corrAB.correlation > 0.7f)
    }

    @Test
    fun `test friction detection in Econ and Eco pair`() {
        val corr = OmnisCorrelationEngine.getCorrelation("Econ", "Eco")
        assertTrue("Econ and Eco should exhibit friction/trade-off", corr.correlation < 0f)
        
        val cluster = OmnisCorrelationEngine.evaluateCluster(setOf("Econ", "Eco"))
        assertTrue(cluster.hasFriction)
        assertTrue(cluster.summary.contains("Interference", ignoreCase = true) || cluster.summary.contains("Detekována", ignoreCase = true))
    }

    @Test
    fun `test cluster evaluation with single domain returns neutral baseline`() {
        val cluster = OmnisCorrelationEngine.evaluateCluster(setOf("Sys"))
        assertFalse(cluster.hasFriction)
        assertEquals(1.0f, cluster.averageSynergy, 0.001f)
    }

    @Test
    fun `test multi-domain cluster synergy calculation`() {
        val cluster = OmnisCorrelationEngine.evaluateCluster(setOf("Sys", "Sec", "Law"))
        assertFalse(cluster.hasFriction)
        assertTrue(cluster.averageSynergy > 0.5f)
    }
}
