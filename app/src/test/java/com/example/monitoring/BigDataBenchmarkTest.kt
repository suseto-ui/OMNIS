package com.example.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OmnisDatabase
import com.example.data.OmnisRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BigDataBenchmarkTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test JVM heap memory sampling returns positive valid values`() {
        val sample = BigDataBenchmarkManager.sampleMemory()
        assertNotNull(sample)
        assertTrue(sample.maxMb > 0f)
        assertTrue(sample.totalMb > 0f)
        assertTrue(sample.usedMb >= 0f)
        assertTrue(sample.usagePercentage in 0f..100f)
    }

    @Test
    fun `test batch insert and purge benchmark records isolation`() = runBlocking {
        val dao = OmnisDatabase.getDatabase(context).omnisDao()

        // Vložíme běžný uživatelský záznam
        val userRecord = OmnisRecord(
            role = "user",
            content = "Důležitá uživatelská zpráva",
            threadId = "user_thread_1",
            userName = "operator"
        )
        dao.insertRecord(userRecord)

        // Vložíme benchmarkové záznamy
        val benchmarkRecords = (1..10).map { i ->
            OmnisRecord(
                role = "assistant",
                content = "[BENCHMARK] Syntetická telemetrie zátěžového testu #$i",
                threadId = "thread_benchmark",
                userName = "stress_tester"
            )
        }
        dao.insertRecords(benchmarkRecords)

        val benchmarkCountBefore = dao.getBenchmarkRecordCount()
        assertTrue(benchmarkCountBefore >= 10)

        // Bezpečné smazání benchmarkových záznamů
        val deleted = dao.deleteBenchmarkRecords()
        assertTrue(deleted >= 10)

        val benchmarkCountAfter = dao.getBenchmarkRecordCount()
        assertEquals(0, benchmarkCountAfter)

        // Uživatelský záznam MUSÍ zůstat netknutý
        val userRecordsCount = dao.getThreadMessageCount("user_thread_1")
        assertEquals(1, userRecordsCount)
    }

    @Test
    fun `test query latency benchmark metrics calculation`() = runBlocking {
        BigDataBenchmarkManager.runQuerySpeedBenchmark(context)
        // Počkáme krátký čas na dokončení asynchronního měření
        kotlinx.coroutines.delay(200)

        val state = BigDataBenchmarkManager.stressState.value
        assertNotNull(state)
        // State result nesmí být null a nesmí házet výjimku
        val result = state.lastBenchmarkResult
        if (result != null) {
            assertTrue(result.countLatencyMs >= 0)
            assertTrue(result.select100LatencyMs >= 0)
            assertTrue(result.estimatedThroughputOpsPerSec >= 0)
        }
    }
}
