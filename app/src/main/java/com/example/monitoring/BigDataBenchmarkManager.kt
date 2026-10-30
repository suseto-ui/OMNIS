package com.example.monitoring

import android.content.Context
import com.example.data.OmnisDatabase
import com.example.data.OmnisRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Reprezentace jednoho časového vzorku využití paměti JVM Heap.
 */
data class MemorySample(
    val timestamp: Long = System.currentTimeMillis(),
    val usedMb: Float,
    val totalMb: Float,
    val maxMb: Float
) {
    val usagePercentage: Float
        get() = if (maxMb > 0f) (usedMb / maxMb) * 100f else 0f
}

/**
 * Výsledky exaktního měření latence Room dotazů nad velkými daty.
 */
data class QueryBenchmarkResult(
    val countLatencyMs: Long = 0L,
    val select100LatencyMs: Long = 0L,
    val select500LatencyMs: Long = 0L,
    val select1000LatencyMs: Long = 0L,
    val searchFilterLatencyMs: Long = 0L,
    val totalDbRecords: Int = 0,
    val benchmarkRecordsCount: Int = 0,
    val estimatedThroughputOpsPerSec: Int = 0,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Stav probíhajícího zátěžového testu a generátoru velkých dat.
 */
data class StressTestState(
    val isGenerating: Boolean = false,
    val targetCount: Int = 0,
    val currentCount: Int = 0,
    val progressPercent: Float = 0f,
    val itemsPerSecond: Int = 0,
    val elapsedTimeMs: Long = 0L,
    val isRunningQueryBenchmark: Boolean = false,
    val lastBenchmarkResult: QueryBenchmarkResult? = null,
    val totalDatabaseRecords: Int = 0,
    val benchmarkRecordsInDb: Int = 0,
    val statusMessage: String = "Systém připraven k zátěžovému testu velkých dat"
)

/**
 * O.M.N.I.S. Big Data Stress Benchmark Engine.
 * Řídí syntetické generování tisíců zpráv, živou telemetrii paměti Heap RAM
 * a měření latence Room databáze bez blokování uživatelského rozhraní.
 */
object BigDataBenchmarkManager {

    private val benchmarkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var memoryJob: Job? = null
    private var generationJob: Job? = null

    private val _memoryHistory = MutableStateFlow<List<MemorySample>>(emptyList())
    val memoryHistory: StateFlow<List<MemorySample>> = _memoryHistory.asStateFlow()

    private val _currentMemory = MutableStateFlow(sampleMemory())
    val currentMemory: StateFlow<MemorySample> = _currentMemory.asStateFlow()

    private val _stressState = MutableStateFlow(StressTestState())
    val stressState: StateFlow<StressTestState> = _stressState.asStateFlow()

    private const val MAX_MEMORY_SAMPLES = 60
    private const val BATCH_CHUNK_SIZE = 250

    init {
        startMemoryMonitoring()
    }

    /**
     * Spustí periodický sběr vzorků alokace paměti (každých 600 ms).
     */
    fun startMemoryMonitoring() {
        if (memoryJob?.isActive == true) return

        memoryJob = benchmarkScope.launch {
            while (isActive) {
                val sample = sampleMemory()
                _currentMemory.value = sample
                val currentList = _memoryHistory.value.toMutableList()
                currentList.add(sample)
                if (currentList.size > MAX_MEMORY_SAMPLES) {
                    currentList.removeAt(0)
                }
                _memoryHistory.value = currentList
                delay(600)
            }
        }
    }

    /**
     * Vzorkuje aktuální stav JVM runtime paměti.
     */
    fun sampleMemory(): MemorySample {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory().toFloat() / (1024f * 1024f)
        val totalMemory = runtime.totalMemory().toFloat() / (1024f * 1024f)
        val freeMemory = runtime.freeMemory().toFloat() / (1024f * 1024f)
        val usedMemory = (totalMemory - freeMemory).coerceAtLeast(0f)

        return MemorySample(
            usedMb = String.format(java.util.Locale.US, "%.1f", usedMemory).toFloat(),
            totalMb = String.format(java.util.Locale.US, "%.1f", totalMemory).toFloat(),
            maxMb = String.format(java.util.Locale.US, "%.1f", maxMemory).toFloat()
        )
    }

    /**
     * Načte a aktualizuje počty záznamů v lokální Room databázi.
     */
    fun refreshDatabaseStats(context: Context) {
        benchmarkScope.launch {
            val dao = OmnisDatabase.getDatabase(context).omnisDao()
            val total = dao.getRecordCount()
            val benchmark = dao.getBenchmarkRecordCount()
            _stressState.value = _stressState.value.copy(
                totalDatabaseRecords = total,
                benchmarkRecordsInDb = benchmark
            )
        }
    }

    /**
     * Dávkový generátor syntetických zpráv pro simulaci velkých objemů dat.
     * Generuje záznamy po dávkách s voláním yield() pro zachování 60 FPS v UI.
     */
    fun generateStressRecords(context: Context, totalCount: Int) {
        if (_stressState.value.isGenerating) return

        generationJob = benchmarkScope.launch {
            val dao = OmnisDatabase.getDatabase(context).omnisDao()
            val startTime = System.currentTimeMillis()

            _stressState.value = _stressState.value.copy(
                isGenerating = true,
                targetCount = totalCount,
                currentCount = 0,
                progressPercent = 0f,
                itemsPerSecond = 0,
                statusMessage = "Generuji $totalCount zpráv v dávkách po $BATCH_CHUNK_SIZE..."
            )

            val domains = listOf(
                "SYSTEMS_INTELLIGENCE",
                "ECONOMIC_ANALYSIS",
                "CYBERNETICS",
                "NEURAL_ETHICS",
                "DIALECTIC_REASONING",
                "SECURITY_PROTOCOL",
                "MACRO_LOGIC",
                "QUANTUM_INFORMATION"
            )

            var generatedSoFar = 0

            try {
                while (generatedSoFar < totalCount && isActive) {
                    val batchStart = System.currentTimeMillis()
                    val batchSize = (totalCount - generatedSoFar).coerceAtMost(BATCH_CHUNK_SIZE)
                    val recordsBatch = ArrayList<OmnisRecord>(batchSize)

                    for (i in 0 until batchSize) {
                        val recordIndex = generatedSoFar + i + 1
                        val selectedDomain = domains[Random.nextInt(domains.size)]
                        val sysVal = Random.nextFloat()
                        val secVal = Random.nextFloat()
                        val econVal = Random.nextFloat()
                        val composite = (sysVal + secVal + econVal) / 3f

                        val record = OmnisRecord(
                            role = if (Random.nextBoolean()) "user" else "assistant",
                            content = "[BENCHMARK] Syntetická telemetrická zpráva zátěžového testu O.M.N.I.S. #$recordIndex s doménovým vektorem $selectedDomain. Ověření propustnosti lokálního úložiště SQLite Room.",
                            cognitiveProcess = "Invariantní audit Fáze 1-5: Zátěžový cyklus #$recordIndex, deduktivní syntéza doménových vah.",
                            valSys = sysVal,
                            valEcon = econVal,
                            valPsych = Random.nextFloat(),
                            valEco = Random.nextFloat(),
                            valLaw = Random.nextFloat(),
                            valSec = secVal,
                            valPhys = Random.nextFloat(),
                            valSoc = Random.nextFloat(),
                            compositeScore = composite,
                            domain = selectedDomain,
                            threadId = "thread_benchmark",
                            threadTitle = "Zátěžový test velkých dat",
                            userName = "stress_tester",
                            timestamp = System.currentTimeMillis() - Random.nextLong(0, 1000L * 60 * 60 * 24 * 7),
                            isSyncedToPostgres = false
                        )
                        recordsBatch.add(record)
                    }

                    // Dávkový INSERT do Room
                    dao.insertRecords(recordsBatch)
                    generatedSoFar += batchSize

                    val batchDuration = (System.currentTimeMillis() - batchStart).coerceAtLeast(1)
                    val itemsPerSec = ((batchSize.toDouble() / batchDuration) * 1000).toInt()
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (generatedSoFar.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

                    _stressState.value = _stressState.value.copy(
                        currentCount = generatedSoFar,
                        progressPercent = progress,
                        itemsPerSecond = itemsPerSec,
                        elapsedTimeMs = elapsed,
                        statusMessage = "Uloženo $generatedSoFar z $totalCount (${(progress * 100).toInt()}%) - $itemsPerSec pol./s"
                    )

                    // Prevence ANR a uvolnění CPU pro vykreslování Compose UI
                    yield()
                }

                refreshDatabaseStats(context)
                val totalTime = System.currentTimeMillis() - startTime
                val avgSpeed = if (totalTime > 0) ((totalCount.toDouble() / totalTime) * 1000).toInt() else 0

                _stressState.value = _stressState.value.copy(
                    isGenerating = false,
                    currentCount = totalCount,
                    progressPercent = 1f,
                    itemsPerSecond = avgSpeed,
                    statusMessage = "Úspěšně vloženo $totalCount záznamů za ${totalTime}ms (průměrně $avgSpeed pol./s)"
                )

                // Automaticky po dokončení vygenerování spustíme test rychlosti dotazů
                runQuerySpeedBenchmark(context)

            } catch (e: CancellationException) {
                _stressState.value = _stressState.value.copy(
                    isGenerating = false,
                    statusMessage = "Generování bylo zrušeno operátorem."
                )
            } catch (e: Exception) {
                _stressState.value = _stressState.value.copy(
                    isGenerating = false,
                    statusMessage = "Chyba při generování: ${e.message}"
                )
            }
        }
    }

    /**
     * Zruší probíhající generování syntetických dat.
     */
    fun cancelGeneration() {
        generationJob?.cancel()
    }

    /**
     * Změří reálnou rychlost čtení, agregace a textového vyhledávání v Room databázi.
     */
    fun runQuerySpeedBenchmark(context: Context) {
        if (_stressState.value.isRunningQueryBenchmark) return

        benchmarkScope.launch {
            _stressState.value = _stressState.value.copy(
                isRunningQueryBenchmark = true,
                statusMessage = "Měřím latenci dotazů Room databáze..."
            )

            val dao = OmnisDatabase.getDatabase(context).omnisDao()

            var countLatency = 0L
            var select100Latency = 0L
            var select500Latency = 0L
            var select1000Latency = 0L
            var searchLatency = 0L
            var totalCount = 0
            var benchmarkCount = 0

            withContext(Dispatchers.IO) {
                // 1. COUNT dotaz
                countLatency = measureTimeMillis {
                    totalCount = dao.getRecordCount()
                    benchmarkCount = dao.getBenchmarkRecordCount()
                }

                // 2. SELECT 100
                select100Latency = measureTimeMillis {
                    dao.getRecentRecordsSync(100)
                }

                // 3. SELECT 500
                select500Latency = measureTimeMillis {
                    dao.getRecentRecordsSync(500)
                }

                // 4. SELECT 1000
                select1000Latency = measureTimeMillis {
                    dao.getRecentRecordsSync(1000)
                }

                // 5. Paged search / LIKE dotaz
                searchLatency = measureTimeMillis {
                    dao.getPagedRecordsByThread("thread_benchmark", 50, 0)
                }
            }

            val totalLatency = (countLatency + select100Latency + select500Latency + select1000Latency + searchLatency).coerceAtLeast(1)
            val estimatedThroughput = ((5.0 / totalLatency) * 1000).toInt()

            val result = QueryBenchmarkResult(
                countLatencyMs = countLatency,
                select100LatencyMs = select100Latency,
                select500LatencyMs = select500Latency,
                select1000LatencyMs = select1000Latency,
                searchFilterLatencyMs = searchLatency,
                totalDbRecords = totalCount,
                benchmarkRecordsCount = benchmarkCount,
                estimatedThroughputOpsPerSec = estimatedThroughput,
                evaluatedAt = System.currentTimeMillis()
            )

            _stressState.value = _stressState.value.copy(
                isRunningQueryBenchmark = false,
                lastBenchmarkResult = result,
                totalDatabaseRecords = totalCount,
                benchmarkRecordsInDb = benchmarkCount,
                statusMessage = "Test latence dokončen: SELECT 1k za ${select1000Latency}ms ($estimatedThroughput dotazů/s)"
            )
        }
    }

    /**
     * Bezpečně vyčistí výhradně syntetická benchmarková data bez ovlivnění uživatelské historie.
     */
    fun purgeBenchmarkData(context: Context) {
        benchmarkScope.launch {
            _stressState.value = _stressState.value.copy(
                statusMessage = "Mažu benchmarková data z databáze..."
            )

            val dao = OmnisDatabase.getDatabase(context).omnisDao()
            val deletedCount = withContext(Dispatchers.IO) {
                val count = dao.deleteBenchmarkRecords()
                System.gc() // Hint pro JVM Garbage Collector pro uvolnění RAM
                count
            }

            refreshDatabaseStats(context)

            _stressState.value = _stressState.value.copy(
                statusMessage = "Úspěšně odstraněno $deletedCount syntetických benchmark záznamů."
            )
        }
    }
}
