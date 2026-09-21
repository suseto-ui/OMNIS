package com.example.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.action.ActionPayload
import com.example.action.OrchestrationManager
import com.example.action.ResilienceManager
import com.example.data.OmnisRecord
import com.example.ui.admin.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHubView(
    records: List<OmnisRecord>,
    onExecuteAction: (ActionPayload) -> Unit,
    isRunningAction: Boolean,
    actionLogs: List<String>,
    fragments: List<com.example.data.MemoryFragment>,
    isConsolidating: Boolean,
    onConsolidateMemory: () -> Unit,
    onDeleteMemoryFragment: (Long) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onPurgeSyncedRecords: (Int) -> Unit
) {
    var selectedSection by remember { mutableStateOf("DB_HEALTH") } // DB_HEALTH, DISPATCHER, STABILITY, INTEGRITY, PURGE
    
    val circuitState by ResilienceManager.circuitState.collectAsState()
    val isChaosMode by ResilienceManager.isChaosModeEnabled.collectAsState()
    
    val isBatchingEnabled by OrchestrationManager.isSmartBatchingEnabled.collectAsState()
    val batchWindow by OrchestrationManager.batchingWindowMs.collectAsState()

    val localCount = records.size
    val postgresSyncedCount = remember(localCount) { if (localCount > 2) localCount - 2 else localCount }
    val unSyncedCount = localCount - postgresSyncedCount

    Scaffold(
        containerColor = OmnisBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ADMINISTRATORSKÉ CENTRUM", fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.White)
                        Text("O.M.N.I.S. Core Diagnostics (v2.1.0-RC)", fontSize = 10.sp, color = OmnisTextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OmnisBgDark, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Horizontal Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminNavChip(title = "DB Health", icon = Icons.Default.Storage, isSelected = selectedSection == "DB_HEALTH", onClick = { selectedSection = "DB_HEALTH" })
                AdminNavChip(title = "Paměť", icon = Icons.Default.Memory, isSelected = selectedSection == "MEMORY", onClick = { selectedSection = "MEMORY" })
                AdminNavChip(title = "Orchestrace", icon = Icons.Default.SettingsSuggest, isSelected = selectedSection == "ORCHESTRATION", onClick = { selectedSection = "ORCHESTRATION" })
                AdminNavChip(title = "Monitor", icon = Icons.Default.Analytics, isSelected = selectedSection == "OBSERVABILITY", onClick = { selectedSection = "OBSERVABILITY" })
                AdminNavChip(title = "Stabilita", icon = Icons.Default.Security, isSelected = selectedSection == "STABILITY", onClick = { selectedSection = "STABILITY" })
                AdminNavChip(title = "Dispečer", icon = Icons.Default.Terminal, isSelected = selectedSection == "DISPATCHER", onClick = { selectedSection = "DISPATCHER" })
                AdminNavChip(title = "Integrita", icon = Icons.Default.Shield, isSelected = selectedSection == "INTEGRITY", onClick = { selectedSection = "INTEGRITY" })
                AdminNavChip(title = "Správa Dat", icon = Icons.Default.Delete, isSelected = selectedSection == "PURGE", onClick = { selectedSection = "PURGE" })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (selectedSection) {
                    "DB_HEALTH" -> {
                        item {
                            AdminDbHealthCard(localCount, postgresSyncedCount, unSyncedCount)
                        }
                        item {
                            AdminLearningLabsCard()
                        }
                    }
                    "MEMORY" -> {
                        item {
                            AdminMemoryCard(
                                fragments = fragments,
                                isConsolidating = isConsolidating,
                                onConsolidate = onConsolidateMemory,
                                onDeleteFragment = onDeleteMemoryFragment
                            )
                        }
                    }
                    "ORCHESTRATION" -> {
                        item {
                            AdminOrchestrationCard(
                                isBatchingEnabled = isBatchingEnabled,
                                batchWindow = batchWindow,
                                onToggleBatching = { OrchestrationManager.toggleSmartBatching(it) },
                                onWindowChange = { OrchestrationManager.setBatchingWindow(it) }
                            )
                        }
                    }
                    "OBSERVABILITY" -> {
                        item {
                            AdminObservabilityCard()
                        }
                    }
                    "STABILITY" -> {
                        item {
                            ResilienceStabilityCard(
                                circuitState = circuitState,
                                isChaosMode = isChaosMode,
                                onToggleChaos = { enabled ->
                                    onExecuteAction(
                                        ActionPayload(
                                            intent = "manual_resilience_override",
                                            actionId = "resilience_circuit_breaker_audit",
                                            parameters = mapOf("chaos_simulation" to enabled)
                                        )
                                    )
                                }
                            )
                        }
                    }
                    "DISPATCHER" -> {
                        item {
                            ActionDrivenInteractivePanel(
                                onExecuteAction = onExecuteAction,
                                isRunning = isRunningAction,
                                logs = actionLogs
                            )
                        }
                    }
                    "INTEGRITY" -> {
                        item {
                            AdminDataIntegrityCard()
                        }
                    }
                    "PURGE" -> {
                        item {
                            AdminDataPurgeCard(postgresSyncedCount, onPurgeSyncedRecords)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLearningLabsCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Science, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(18.dp))
                Text(
                    text = "COGNITIVE LEARNING LABS",
                    color = OmnisAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Experimentální prostředí pro ladění LLM parametrů a trénování lokálních tensorových modelů.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
