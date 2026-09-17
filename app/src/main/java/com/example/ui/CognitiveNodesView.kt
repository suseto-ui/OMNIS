package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.ui.theme.*

/**
 * Reprezentuje automaticky extrahovaný systémový invariant z kognitivní zprávy.
 */
data class SystemInvariant(
    val title: String,
    val domain: String,
    val certaintyScore: Float,
    val description: String,
    val suggestedActionIntent: String,
    val suggestedActionPayload: Map<String, Any>
)

/**
 * Modul 'Cognitive Nodes': Analýza systémových invariantů a generování
 * automatických následných akčních kroků (Action Step Generators).
 */
@Composable
fun CognitiveNodesView(
    records: List<OmnisRecord>,
    onExecuteAction: (com.example.action.ActionPayload) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val assistantRecords = remember(records) {
        records.filter { it.role == "assistant" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("cognitive_nodes_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OmnisPanelDark,
                border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(OmnisCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = OmnisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "KOGNITIVNÍ UZLY (COGNITIVE NODES)",
                            color = OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatická detekce systémových invariantů z odpovědí a generování reaktivních následných akčních kroků.",
                        color = OmnisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (assistantRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Žádné kognitivní uzly nebyly dosud vytvořeny. Spusťte dotaz v chatu.",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(assistantRecords.reversed(), key = { it.id }) { record ->
                val invariants = remember(record) { extractInvariantsFromRecord(record) }
                CognitiveNodeCard(
                    record = record,
                    invariants = invariants,
                    onExecuteAction = onExecuteAction,
                    onNavigateToChat = onNavigateToChat
                )
            }
        }
    }
}

@Composable
fun CognitiveNodeCard(
    record: OmnisRecord,
    invariants: List<SystemInvariant>,
    onExecuteAction: (com.example.action.ActionPayload) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = null,
                        tint = OmnisViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "UZEL #${record.id} [${record.domain}]",
                        color = OmnisViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = OmnisEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Integrita ${(record.compositeScore * 100).toInt()}%",
                        color = OmnisEmerald,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.content.take(150) + if (record.content.length > 150) "..." else "",
                color = OmnisTextLight,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = OmnisBorderDark)
            Spacer(modifier = Modifier.height(8.dp))

            // Invariants Section
            Text(
                text = "IDENTIFIKOVANÉ SYSTÉMOVÉ INVARIANITY & AKČNÍ KROKY:",
                color = OmnisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                invariants.forEach { invariant ->
                    InvariantActionTile(
                        invariant = invariant,
                        onExecuteAction = onExecuteAction,
                        onNavigateToChat = onNavigateToChat
                    )
                }
            }
        }
    }
}

@Composable
fun InvariantActionTile(
    invariant: SystemInvariant,
    onExecuteAction: (com.example.action.ActionPayload) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INVARIANT: ${invariant.title}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Konfidence: ${(invariant.certaintyScore * 100).toInt()}%",
                    color = OmnisAmber,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = invariant.description,
                color = OmnisTextMuted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Suggested Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val payload = com.example.action.ActionPayload(
                            intent = invariant.suggestedActionIntent,
                            actionId = invariant.suggestedActionIntent,
                            parameters = invariant.suggestedActionPayload
                        )
                        onExecuteAction(payload)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spustit Dispečink", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onNavigateToChat("Navrhni podrobný implementační plán pro systémový invariant: ${invariant.title}")
                    },
                    border = BorderStroke(1.dp, OmnisViolet),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = OmnisViolet, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prohloubit v Chatu", color = OmnisViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Extrakce systémových invariantů z kognitivního obsahu záznamu.
 */
private fun extractInvariantsFromRecord(record: OmnisRecord): List<SystemInvariant> {
    val list = mutableListOf<SystemInvariant>()
    val contentUpper = record.content.uppercase()

    if (contentUpper.contains("DATABÁZE") || contentUpper.contains("DB") || contentUpper.contains("SQL") || contentUpper.contains("POSTGRES")) {
        list.add(
            SystemInvariant(
                title = "Integrita Databázové Vrstvy",
                domain = record.domain,
                certaintyScore = 0.94f,
                description = "Detekována zmínka databázových operací. Doporučena kontrola konektivity a connection poolu.",
                suggestedActionIntent = "db_connectivity_test",
                suggestedActionPayload = mapOf("engine" to "PostgreSQL", "include_tls_validation" to true)
            )
        )
    }

    if (contentUpper.contains("SÍŤ") || contentUpper.contains("NETWORK") || contentUpper.contains("EBPF") || contentUpper.contains("XDP") || contentUpper.contains("LATENCE")) {
        list.add(
            SystemInvariant(
                title = "Optimalizace Síťové Latence",
                domain = record.domain,
                certaintyScore = 0.89f,
                description = "Detekován požadavek na síťovou propustnost. Doporučen eBPF/XDP offloading.",
                suggestedActionIntent = "ebpf_xdp_offload",
                suggestedActionPayload = mapOf("interface" to "eth0", "sync_type" to "CRDT_EVENT_DRIVEN")
            )
        )
    }

    if (list.isEmpty()) {
        list.add(
            SystemInvariant(
                title = "Invariant Bezpečnostního Perimetru",
                domain = record.domain,
                certaintyScore = 0.92f,
                description = "Standardní invariant stability a Zero-Trust pravidel pro kognitivní uzel.",
                suggestedActionIntent = "ai_governance_export",
                suggestedActionPayload = mapOf("export_deterministic_state" to true)
            )
        )
    }

    return list
}
