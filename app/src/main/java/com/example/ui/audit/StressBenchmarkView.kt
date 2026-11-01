package com.example.ui.audit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.monitoring.BigDataBenchmarkManager
import com.example.monitoring.QueryBenchmarkResult
import com.example.ui.theme.*

/**
 * Zátěžový benchmark a simulace velkých dat v O.M.N.I.S.
 * Poskytuje živý graf alokace RAM paměti, dávkový generátor tisíců zpráv,
 * reálné měření propustnosti SQLite Room a bezpečné čištění.
 */
@Composable
fun StressBenchmarkView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val memoryHistory by BigDataBenchmarkManager.memoryHistory.collectAsStateWithLifecycle()
    val currentMemory by BigDataBenchmarkManager.currentMemory.collectAsStateWithLifecycle()
    val stressState by BigDataBenchmarkManager.stressState.collectAsStateWithLifecycle()

    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BigDataBenchmarkManager.refreshDatabaseStats(context)
        BigDataBenchmarkManager.startMemoryMonitoring()
    }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = {
                Text(
                    "Odstranit benchmarková data?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Tato akce bezpečně smaže pouze ${stressState.benchmarkRecordsInDb} syntetických záznamů zátěžového testu. Vaše reálná vlákna a zprávy zůstanou netknuté.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = OmnisTextLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        BigDataBenchmarkManager.purgeBenchmarkData(context)
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_purge_benchmark_button")
                ) {
                    Text("SMAZAT SYNTETICKÁ DATA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("ZRUŠIT", fontFamily = FontFamily.Monospace, color = Color.White)
                }
            },
            containerColor = OmnisPanelDark
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Živý graf alokace paměti
        item {
            LiveMemoryGraph(
                memoryHistory = memoryHistory,
                currentSample = currentMemory
            )
        }

        // 2. Stavová karta a ukazatele velkých dat
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = OmnisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "STAV VELKOOBJEMOVÉHO ÚLOŽIŠTĚ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                BigDataBenchmarkManager.refreshDatabaseStats(context)
                            },
                            modifier = Modifier.size(32.dp).testTag("refresh_stats_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Obnovit statistiky", tint = OmnisTextLight, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BenchmarkMetricChip(
                            label = "Záznamů celkem",
                            value = "${stressState.totalDatabaseRecords}",
                            color = Color.White
                        )
                        BenchmarkMetricChip(
                            label = "Syntetických dat",
                            value = "${stressState.benchmarkRecordsInDb}",
                            color = OmnisCyan
                        )
                        BenchmarkMetricChip(
                            label = "Volná Heap RAM",
                            value = "${(currentMemory.maxMb - currentMemory.usedMb).toInt()} MB",
                            color = OmnisEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stavová zpráva
                    Surface(
                        color = OmnisBgDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (stressState.isGenerating) Icons.Default.Sync else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (stressState.isGenerating) OmnisCyan else OmnisEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stressState.statusMessage,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Aktivní ukazatel průběhu generování (zobrazuje se při generování)
        if (stressState.isGenerating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GENEROVÁNÍ DATOVÉ ZÁTĚŽE...",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = OmnisCyan,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${stressState.currentCount} / ${stressState.targetCount} (${(stressState.progressPercent * 100).toInt()}%)",
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { stressState.progressPercent },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = OmnisCyan,
                            trackColor = OmnisBorderDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rychlost: ${stressState.itemsPerSecond} položek/s",
                                fontFamily = FontFamily.Monospace,
                                color = OmnisEmerald,
                                fontSize = 10.sp
                            )

                            OutlinedButton(
                                onClick = { BigDataBenchmarkManager.cancelGeneration() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("cancel_generation_button")
                            ) {
                                Text("PŘERUŠIT", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Panel s tlačítky pro generování zátěže
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GENERÁTOR SYNTETICKÝCH DAT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Generuje zprávy s 8D vektory, myšlenkovými stopami a různými doménami.",
                        fontFamily = FontFamily.Monospace,
                        color = OmnisTextLight,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                BigDataBenchmarkManager.generateStressRecords(context, 1000)
                            },
                            enabled = !stressState.isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("generate_1k_button")
                        ) {
                            Text("+1 000", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                BigDataBenchmarkManager.generateStressRecords(context, 5000)
                            },
                            enabled = !stressState.isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("generate_5k_button")
                        ) {
                            Text("+5 000", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                BigDataBenchmarkManager.generateStressRecords(context, 10000)
                            },
                            enabled = !stressState.isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("generate_10k_button")
                        ) {
                            Text("+10 000", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                BigDataBenchmarkManager.runQuerySpeedBenchmark(context)
                            },
                            enabled = !stressState.isGenerating && !stressState.isRunningQueryBenchmark,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OmnisCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("run_query_benchmark_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (stressState.isRunningQueryBenchmark) "MĚŘÍM..." else "TEST DOTAZŮ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showPurgeConfirmDialog = true },
                            enabled = !stressState.isGenerating && stressState.benchmarkRecordsInDb > 0,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("purge_benchmark_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VYČISTIT BENCHMARK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. Výsledky testu latence dotazů
        item {
            stressState.lastBenchmarkResult?.let { result ->
                QueryBenchmarkResultCard(result)
            } ?: Card(
                colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = OmnisTextLight,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Žádný nedávný test latence",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klepněte na 'TEST DOTAZŮ' pro exaktní změření latence čtení a propustnosti Room databáze.",
                        fontFamily = FontFamily.Monospace,
                        color = OmnisTextLight,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BenchmarkMetricChip(label: String, value: String, color: Color = Color.White) {
    Column {
        Text(text = label, fontSize = 9.sp, color = OmnisTextLight, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun QueryBenchmarkResultCard(result: QueryBenchmarkResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = OmnisEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "VÝSLEDKY LATENCE ROOM DOTAZŮ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = OmnisEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "~${result.estimatedThroughputOpsPerSec} dotazů/s",
                        color = OmnisEmerald,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LatencyRow("Agregace COUNT(*)", "${result.countLatencyMs} ms", result.countLatencyMs < 10)
                LatencyRow("SELECT 100 záznamů", "${result.select100LatencyMs} ms", result.select100LatencyMs < 15)
                LatencyRow("SELECT 500 záznamů", "${result.select500LatencyMs} ms", result.select500LatencyMs < 30)
                LatencyRow("SELECT 1 000 záznamů", "${result.select1000LatencyMs} ms", result.select1000LatencyMs < 50)
                LatencyRow("Stránkované vyhledávání", "${result.searchFilterLatencyMs} ms", result.searchFilterLatencyMs < 20)
            }
        }
    }
}

@Composable
private fun LatencyRow(label: String, latency: String, isOptimal: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            color = OmnisTextLight,
            fontSize = 11.sp
        )
        Surface(
            color = if (isOptimal) OmnisEmerald.copy(alpha = 0.1f) else Color(0xFFF59E0B).copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = latency,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isOptimal) OmnisEmerald else Color(0xFFF59E0B),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
