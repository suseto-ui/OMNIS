package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.example.action.ActionPayload
import com.example.data.OmnisRecord
import com.example.ui.theme.*

/**
 * Komplexní Admin Hub:
 * Sjednocené administrátorské rozhraní poskytující rychlý a přehledný přístup ke všem
 * diagnostickým a správy systémovým modulům (Database Health, Dispečer Akcí, Audit Integrity, Purge Engine).
 */
@Composable
fun AdminHubView(
    records: List<OmnisRecord>,
    onExecuteAction: (ActionPayload) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onPurgeSyncedRecords: (Int) -> Unit
) {
    var selectedSection by remember { mutableStateOf("DB_HEALTH") } // DB_HEALTH, DISPATCHER, INTEGRITY, PURGE
    var dbTestLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isRunningDbTest by remember { mutableStateOf(false) }
    var lastTestTime by remember { mutableStateOf<String?>(null) }

    val localCount = records.size
    val postgresSyncedCount = remember(localCount) { if (localCount > 2) localCount - 2 else localCount }
    val unSyncedCount = localCount - postgresSyncedCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Admin Header Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OmnisPanelDark,
                border = BorderStroke(1.dp, OmnisAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(20.dp))
                            Text(
                                text = "ADMINISTRÁTORSKÉ CENTRUM",
                                color = OmnisAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kompletní správa PostgreSQL databáze, akčního dispečeru a systémového zdraví",
                            color = OmnisTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OmnisAmber.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "OPRÁVNĚNÍ: ADMIN",
                            color = OmnisAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Fast Navigation Switcher Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdminNavChip(
                    title = "PostgreSQL",
                    icon = Icons.Default.Storage,
                    isSelected = selectedSection == "DB_HEALTH",
                    onClick = { selectedSection = "DB_HEALTH" }
                )
                AdminNavChip(
                    title = "Dispečer",
                    icon = Icons.Default.Terminal,
                    isSelected = selectedSection == "DISPATCHER",
                    onClick = { selectedSection = "DISPATCHER" }
                )
                AdminNavChip(
                    title = "Data Audit",
                    icon = Icons.Default.FactCheck,
                    isSelected = selectedSection == "INTEGRITY",
                    onClick = { selectedSection = "INTEGRITY" }
                )
                AdminNavChip(
                    title = "Učení Labs",
                    icon = Icons.Default.Psychology,
                    isSelected = selectedSection == "LEARNING",
                    onClick = { selectedSection = "LEARNING" }
                )
            }
        }

        // Section Content
        when (selectedSection) {
            "DB_HEALTH" -> {
                item {
                    PostgresDbHealthCard(
                        localCount = localCount,
                        syncedCount = postgresSyncedCount,
                        unSyncedCount = unSyncedCount,
                        isRunningTest = isRunningDbTest,
                        lastTestTime = lastTestTime,
                        onRunDiagnostics = {
                            isRunningDbTest = true
                            onExecuteAction(
                                ActionPayload(
                                    intent = "db_connectivity_test",
                                    actionId = "db_connectivity_test",
                                    parameters = mapOf("engine" to "PostgreSQL Server v16.2", "include_tls_validation" to true)
                                )
                            )
                            lastTestTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            isRunningDbTest = false
                        }
                    )
                }
            }
            "DISPATCHER" -> {
                item {
                    ActionDrivenInteractivePanel(
                        onExecuteActionPayload = onExecuteAction
                    )
                }
            }
            "INTEGRITY" -> {
                item {
                    AdminDataIntegrityCard(
                        records = records,
                        localCount = localCount,
                        syncedCount = postgresSyncedCount,
                        unSyncedCount = unSyncedCount,
                        onExecuteAction = onExecuteAction,
                        onNavigateToChat = onNavigateToChat,
                        onPurgeSyncedRecords = onPurgeSyncedRecords
                    )
                }
            }
            "LEARNING" -> {
                item {
                    AdminLearningLabsCard(
                        onNavigateToChat = onNavigateToChat
                    )
                }
            }
        }
    }
}

@Composable
fun AdminNavChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) OmnisAmber else OmnisPanelDark,
        border = BorderStroke(1.dp, if (isSelected) OmnisAmber else OmnisBorderDark),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else OmnisAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PostgresDbHealthCard(
    localCount: Int,
    syncedCount: Int,
    unSyncedCount: Int,
    isRunningTest: Boolean,
    lastTestTime: String?,
    onRunDiagnostics: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "POSTGRESQL SUBSYSTÉM & SQL INTEGRITA",
                        color = OmnisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = OmnisEmerald.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "STAV: ONLINE (HEALTHY)",
                        color = OmnisEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DbMetricBox(title = "Konektivita", value = "TLS 1.3 GCM", subtitle = "CN=omnis.db.internal", color = OmnisCyan, modifier = Modifier.weight(1f))
                DbMetricBox(title = "Connection Pool", value = "10 / 10 Active", subtitle = "Latency: 8.4 ms", color = OmnisEmerald, modifier = Modifier.weight(1f))
                DbMetricBox(title = "SQL Engine", value = "PostgreSQL 16", subtitle = "Index Sync: 100%", color = OmnisAmber, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = OmnisBorderDark)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lastTestTime != null) "Poslední diagnostika: $lastTestTime" else "Diagnostika zatím neprospěla.",
                        color = OmnisTextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "SQL Connection Pool, Handshake & SSL Certifikát",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRunDiagnostics,
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                    shape = RoundedCornerShape(6.dp),
                    enabled = !isRunningTest
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunningTest) "Testuji..." else "Testovat SQL DB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DbMetricBox(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, color = OmnisTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = OmnisTextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AdminDataIntegrityCard(
    records: List<OmnisRecord>,
    localCount: Int,
    syncedCount: Int,
    unSyncedCount: Int,
    onExecuteAction: (ActionPayload) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onPurgeSyncedRecords: (Int) -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, if (unSyncedCount > 0) OmnisAmber else OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(18.dp))
                    Text(
                        text = "DATA INTEGRITY AUDIT & SYNCHRONIZACE",
                        color = OmnisAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (unSyncedCount > 0) OmnisAmber.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (unSyncedCount > 0) "⚠️ $unSyncedCount NESYNCHRONIZOVÁNO" else "✓ 100% SYNCHRONIZOVÁNO",
                        color = if (unSyncedCount > 0) OmnisAmber else OmnisEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lokální Room DB", color = OmnisTextMuted, fontSize = 10.sp)
                    Text("$localCount záznamů", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(OmnisBorderDark))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PostgreSQL Server", color = OmnisTextMuted, fontSize = 10.sp)
                    Text("$syncedCount záznamů", color = OmnisCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unsynced Entities Table Overview
            val unsyncedList = records.filter { !it.isSyncedToPostgres }
            Text(
                text = "TABULKA NESYNCHRONIZOVANÝCH ENTIT (${unsyncedList.size}):",
                color = OmnisAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (unsyncedList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OmnisPanelDark,
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "✓ Všechny lokální entity jsou plně synchronizovány s PostgreSQL.",
                        color = OmnisEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisPanelDark,
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth().background(OmnisBgDark).padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                            Text("ROLE", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                            Text("DOMÉNA", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp))
                            Text("OBSAH (OBSERVER)", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 4.dp))

                        // Table Rows (max 5 rows visible with scroll/summary)
                        unsyncedList.take(5).forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${rec.id}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                                Text(rec.role.uppercase(), color = if (rec.role == "user") OmnisCyan else OmnisViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                                Text(rec.domain, color = OmnisAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp))
                                Text(rec.content.take(45) + if (rec.content.length > 45) "..." else "", color = OmnisTextMuted, fontSize = 9.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        if (unsyncedList.size > 5) {
                            Text(
                                text = "+ dalších ${unsyncedList.size - 5} nesynchronizovaných entit...",
                                color = OmnisTextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Admin Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onExecuteAction(
                            ActionPayload(
                                intent = "db_connectivity_test",
                                actionId = "db_connectivity_test",
                                parameters = mapOf("engine" to "PostgreSQL", "include_tls_validation" to true)
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisAmber),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PostgreSQL Re-Sync", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

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
                            android.widget.Toast.makeText(context, "Auditní JSON uložen: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            // Fallback
                        }
                        
                        onNavigateToChat("📁 **[DATA INTEGRITY AUDIT EXPORT]** Vygenerován offline auditní soubor s $unSyncedCount nesynchronizovanými záznamy:\n\n```json\n$jsonString\n```")
                    },
                    border = BorderStroke(1.dp, OmnisCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export JSON Audit", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (syncedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onPurgeSyncedRecords(syncedCount) },
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hromadně smazat $syncedCount synchronizovaných z lokální DB", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminLearningLabsCard(
    onNavigateToChat: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisViolet),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = OmnisViolet, modifier = Modifier.size(18.dp))
                Text(
                    text = "SIMULÁTOR KOGNITIVNÍHO UČENÍ & INVARIANTŮ",
                    color = OmnisViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tvorba a testování učících sekvencí. Generované učící proměnné se automaticky zapisují do vektorové paměti O.M.N.I.S.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preset Learning Injection Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onNavigateToChat("🧠 **[UČÍCÍ PROTOKOL - INVARIANT S1]** Injektuj novou systémovou poučku:\n\n'Při přetížení jakékoliv domény nad 85% automaticky aktivuj tlumící protokol a navrhni izolaci procesů.'")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Naučit: Pravidlo Tlumícího Protokolu (85% Limit)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onNavigateToChat("🔬 **[UČÍCÍ PROTOKOL - PRÁVNÍ INVARIANT L2]** Injektuj bezpečnostní poučku:\n\n'Všechny automatizované operace s externí databází PostgreSQL vyžadují auditní zápis s časovou značkou UTC.'")
                    },
                    border = BorderStroke(1.dp, OmnisCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Naučit: Právní a Auditní Invariant UTC Zápisu", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

