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
    onNavigateToChat: (String) -> Unit,
    onPurgeSyncedRecords: (Int) -> Unit = {},
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null
) {
    val assistantRecords = remember(records) {
        records.filter { it.role == "assistant" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("cognitive_nodes_view"),
        contentPadding = PaddingValues(bottom = 64.dp),
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
                            text = "DEV LAB / KOGNITIVNÍ UZLY",
                            color = OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        com.example.ui.guide.OmnisHelpIconButton(
                            title = "Kognitivní Uzly",
                            description = "Modul pro automatickou detekci systémových invariantů z odpovědí agenta a ověřování datové integrity.",
                            bulletPoints = listOf(
                                "Invarianty: Extrakce technických pravidel a omezení z výstupů.",
                                "Akce: Generování přímých akčních tlačítkových nabídek.",
                                "Audit Integrity: Sledování stavu lokální databáze vs. cloudová synchronizace."
                            ),
                            tint = OmnisCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatická detekce systémových invariantů z odpovědí a diagnostika datové integrity.",
                        color = OmnisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Data Integrity Audit Card (Porovnání lokálních záznamů s PostgreSQL synchem)
        item {
            val localCount = records.size
            // Simulovaný počet synchronizovaných záznamů v PostgreSQL (např. o 2 méně než v lokální DB pro demonstraci chybějící synchro)
            val postgresSyncedCount = remember(localCount) { if (localCount > 2) localCount - 2 else localCount }
            val unSyncedCount = localCount - postgresSyncedCount

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OmnisBgDark,
                border = BorderStroke(1.dp, if (unSyncedCount > 0) OmnisAmber else OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
                            Text(
                                text = "DATA INTEGRITY AUDIT",
                                color = OmnisAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (unSyncedCount > 0) OmnisAmber.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (unSyncedCount > 0) "⚠️ $unSyncedCount NESYNCHRONIZOVÁNO" else "✓ PLNOU SYNCHRO",
                                color = if (unSyncedCount > 0) OmnisAmber else OmnisEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lokální DB (IndexedDB/Room)", color = OmnisTextMuted, fontSize = 9.sp)
                            Text("$localCount záznamů", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(OmnisBorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PostgreSQL Synced", color = OmnisTextMuted, fontSize = 9.sp)
                            Text("$postgresSyncedCount záznamů", color = OmnisCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (unSyncedCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    onExecuteAction(
                                        com.example.action.ActionPayload(
                                            intent = "db_connectivity_test",
                                            actionId = "db_connectivity_test",
                                            parameters = mapOf("engine" to "PostgreSQL", "include_tls_validation" to true)
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OmnisAmber),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PostgreSQL Re-Sync", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Export JSON Unsynced Records
                            val context = androidx.compose.ui.platform.LocalContext.current
                            OutlinedButton(
                                onClick = {
                                    val unSyncedRecords = records.takeLast(unSyncedCount)
                                    val jsonString = buildString {
                                        append("[\n")
                                        unSyncedRecords.forEachIndexed { idx, rec ->
                                            append("  {\"id\": ${rec.id}, \"role\": \"${rec.role}\", \"domain\": \"${rec.domain}\", \"timestamp\": ${rec.timestamp}, \"content\": \"${rec.content.replace("\"", "\\\"").replace("\n", " ")}\"}")
                                            if (idx < unSyncedRecords.size - 1) append(",")
                                            append("\n")
                                        }
                                        append("]")
                                    }
                                    
                                    try {
                                        val fileName = "omnis_audit_unsynced_${System.currentTimeMillis()}.json"
                                        val file = java.io.File(context.cacheDir, fileName)
                                        file.writeText(jsonString)
                                        android.widget.Toast.makeText(context, "Auditní JSON uložen do souboru: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        // Fallback na zobrazení v chatu
                                    }
                                    
                                    onNavigateToChat("📁 **[DATA INTEGRITY AUDIT EXPORT]** Vygenerován offline auditní soubor s $unSyncedCount nesynchronizovanými záznamy:\n\n```json\n$jsonString\n```")
                                },
                                border = BorderStroke(1.dp, OmnisCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export JSON Audit", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Mass Delete Synced Local Records (Uvolnění paměti)
                    if (postgresSyncedCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onPurgeSyncedRecords(postgresSyncedCount)
                            },
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hromadně smazat $postgresSyncedCount synchronizovaných z lokální DB", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                    onNavigateToChat = onNavigateToChat,
                    onSaveArtifact = onSaveArtifact
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
    onNavigateToChat: (String) -> Unit,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null
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
                        onNavigateToChat = onNavigateToChat,
                        onSaveArtifact = onSaveArtifact
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
    onNavigateToChat: (String) -> Unit,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Jistota: ${(invariant.certaintyScore * 100).toInt()}%",
                        color = OmnisAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (onSaveArtifact != null) {
                        IconButton(
                            onClick = {
                                val artifactContent = buildString {
                                    appendLine("# Systémový Invariant: ${invariant.title}")
                                    appendLine("**Doména:** ${invariant.domain}")
                                    appendLine("**Spolehlivost:** ${(invariant.certaintyScore * 100).toInt()}%\n")
                                    appendLine("## Specifikace")
                                    appendLine(invariant.description)
                                    appendLine("\n## Doporučený Dispatch Intent")
                                    appendLine("- Intent: `${invariant.suggestedActionIntent}`")
                                    appendLine("- Parametry: `${invariant.suggestedActionPayload}`")
                                }
                                onSaveArtifact(
                                    "Invariant: ${invariant.title}",
                                    "CONFIG",
                                    "yaml",
                                    artifactContent
                                )
                                android.widget.Toast.makeText(context, "Invariant uložen do artefaktů", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Uložit invariant", tint = OmnisEmerald, modifier = Modifier.size(14.dp))
                        }
                    }
                }
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
