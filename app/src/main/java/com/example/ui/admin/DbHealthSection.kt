package com.example.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSqlSyncManager
import com.example.data.DatabaseConfig
import com.example.ui.theme.*

@Composable
fun AdminDbHealthCard(
    localCount: Int,
    postgresSyncedCount: Int,
    unSyncedCount: Int,
    syncStatus: CloudSqlSyncManager.SyncStatus = CloudSqlSyncManager.SyncStatus.Idle,
    onSyncNow: () -> Unit = {},
    onTestConnection: () -> Unit = {}
) {
    val isSyncing = syncStatus is CloudSqlSyncManager.SyncStatus.Syncing || syncStatus is CloudSqlSyncManager.SyncStatus.Connecting
    val rotation by rememberInfiniteTransition(label = "sync_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_spin_anim"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisCardDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "HYBRIDNÍ DATABÁZE (ROOM + CLOUD SQL)",
                        color = OmnisEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Live Sync Status Chip
                val (chipColor, chipText, chipIcon) = when (syncStatus) {
                    is CloudSqlSyncManager.SyncStatus.Syncing -> Triple(OmnisCyan, "SYNCING...", Icons.Default.CloudSync)
                    is CloudSqlSyncManager.SyncStatus.Connecting -> Triple(OmnisAmber, "CONNECTING...", Icons.Default.CloudSync)
                    is CloudSqlSyncManager.SyncStatus.Success -> Triple(OmnisEmerald, "ONLINE", Icons.Default.CloudDone)
                    is CloudSqlSyncManager.SyncStatus.Offline -> Triple(Color(0xFFF87171), "OFFLINE (CACHED)", Icons.Default.CloudOff)
                    CloudSqlSyncManager.SyncStatus.Idle -> Triple(OmnisTextMuted, "STANDBY", Icons.Default.Storage)
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = chipColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(chipColor, CircleShape)
                        )
                        Text(
                            text = chipText,
                            color = chipColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row: Total, Synced, Pending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DbMetricBox(label = "CELKEM (ROOM)", value = localCount.toString(), color = Color.White, modifier = Modifier.weight(1f))
                DbMetricBox(label = "CLOUD SQL SYNCHRO", value = postgresSyncedCount.toString(), color = OmnisEmerald, modifier = Modifier.weight(1f))
                DbMetricBox(label = "ČEKÁ NA SYNC", value = unSyncedCount.toString(), color = if (unSyncedCount > 0) OmnisAmber else OmnisTextMuted, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Connection Info Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cloud SQL Instance:", color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("${DatabaseConfig.host}:${DatabaseConfig.port}", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cílová databáze:", color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(DatabaseConfig.dbName, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Uživatel / Režim:", color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("${DatabaseConfig.user} (Hybrid Zero-Latency)", color = OmnisEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Sync Status Details
            if (syncStatus is CloudSqlSyncManager.SyncStatus.Offline) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Stav spojení: ${syncStatus.reason}",
                    color = OmnisAmber,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            } else if (syncStatus is CloudSqlSyncManager.SyncStatus.Success && syncStatus.syncedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Poslední dávka: synchronizováno ${syncStatus.syncedCount} zpráv do Cloud SQL.",
                    color = OmnisEmerald,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Actions: Live Sync Now + Test Connection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .testTag("btn_live_cloudsql_sync"),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSyncing) "SYNCHRONIZUJI..." else "SPUSTIT LIVE SYNC",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isSyncing,
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_test_cloudsql_conn"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = OmnisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TEST SPOJENÍ",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Lokální Room SQLite slouží jako primární vrstva s nulovou latencí. Změny se asynchronně replikují do Cloud SQL přes zabezpečené TLS spojení.",
                color = OmnisTextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}
