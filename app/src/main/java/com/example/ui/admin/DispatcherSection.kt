package com.example.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.action.ActionPayload
import com.example.ui.theme.*

@Composable
fun ActionDrivenInteractivePanel(
    onExecuteAction: (ActionPayload) -> Unit,
    isRunning: Boolean,
    logs: List<String>
) {
    val logState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logState.animateScrollToItem(logs.size - 1)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "BACKEND ACTION DISPATCHER",
                    color = OmnisEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DispatcherActionRow(
                    title = "Infrastructure Audit",
                    desc = "Test DB connectivity & TLS",
                    actionId = "db_connectivity_test",
                    params = mapOf("engine" to "PostgreSQL/SQLite", "include_tls_validation" to true),
                    isRunning = isRunning,
                    onExecute = onExecuteAction
                )
                DispatcherActionRow(
                    title = "eBPF/XDP Offloading",
                    desc = "Kernel packet bypass optimization",
                    actionId = "ebpf_xdp_offload",
                    params = mapOf("interface" to "eth0", "sync_type" to "CRDT_EVENT_DRIVEN"),
                    isRunning = isRunning,
                    onExecute = onExecuteAction
                )
                DispatcherActionRow(
                    title = "AI Governance Audit",
                    desc = "Zero-Trust tensor verification",
                    actionId = "ai_governance_export",
                    params = emptyMap(),
                    isRunning = isRunning,
                    onExecute = onExecuteAction
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Log Panel
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Žádné záznamy o exekuci.", color = OmnisTextMuted, fontSize = 10.sp)
                    }
                } else {
                    LazyColumn(
                        state = logState,
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                log,
                                color = if (log.contains("ERROR")) Color.Red else OmnisEmerald,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatcherActionRow(
    title: String,
    desc: String,
    actionId: String,
    params: Map<String, Any>,
    isRunning: Boolean,
    onExecute: (ActionPayload) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = OmnisTextMuted, fontSize = 9.sp)
            }
            
            IconButton(
                onClick = {
                    onExecute(ActionPayload(intent = "MANUAL_DISPATCH", actionId = actionId, parameters = params))
                },
                enabled = !isRunning,
                modifier = Modifier.size(32.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = OmnisEmerald)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OmnisEmerald)
                }
            }
        }
    }
}
