package com.example.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OmnisTab
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocaleManager
import com.example.ui.localization.OmnisStrings
import com.example.ui.theme.*

/**
 * Administrátorská horní lišta (TopAppBar).
 * Obsahuje systémové indikátory, stav jističe, název vlákna a bezpečnostní odznak Admin / Operátor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopAppBar(
    activeTab: OmnisTab,
    isCircuitBreakerTripped: Boolean,
    isPromptGatewayEnabled: Boolean = true,
    activeThreadTitle: String = "",
    userExperienceMode: com.example.ui.UserExperienceMode = com.example.ui.UserExperienceMode.STANDARD,
    onToggleUserMode: () -> Unit = {},
    onMenuClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onNewThreadClick: () -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    onExportMarkdownClick: () -> Unit = {},
    onExportCertifiedAuditClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    syncStatus: com.example.data.CloudSqlSyncManager.SyncStatus = com.example.data.CloudSqlSyncManager.SyncStatus.Idle,
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentLang by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()
    var showExportMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OmnisBgDark,
            titleContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Otevřít administrátorskou nabídku",
                    tint = OmnisAmber
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "O.M.N.I.S.",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Surface(
                    color = OmnisAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, OmnisAmber, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = getAdminTabTitle(activeTab),
                        color = OmnisAmber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (activeTab == OmnisTab.CHAT && activeThreadTitle.isNotBlank()) {
                    Surface(
                        color = OmnisPanelDark,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, OmnisBorderDark, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "🧵 $activeThreadTitle",
                            color = OmnisAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Přepínač režimu: Standard vs Expert
                Surface(
                    onClick = onToggleUserMode,
                    shape = RoundedCornerShape(14.dp),
                    color = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald.copy(alpha = 0.18f) else OmnisViolet.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald else OmnisViolet
                    )
                ) {
                    Text(
                        text = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) "🌱 Zač." else "⚡ Exp.",
                        color = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald else OmnisViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Indikátor stavu Cloud SQL synchronizace
                val isSpinning = syncStatus is com.example.data.CloudSqlSyncManager.SyncStatus.Syncing ||
                        syncStatus is com.example.data.CloudSqlSyncManager.SyncStatus.Connecting
                val syncRotation by rememberInfiniteTransition(label = "adm_sync_rot").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "adm_sync_spin"
                )
                val (syncTint, syncIcon) = when (syncStatus) {
                    is com.example.data.CloudSqlSyncManager.SyncStatus.Syncing,
                    is com.example.data.CloudSqlSyncManager.SyncStatus.Connecting -> Pair(OmnisCyan, Icons.Default.Sync)
                    is com.example.data.CloudSqlSyncManager.SyncStatus.Offline -> Pair(OmnisAmber, Icons.Default.CloudOff)
                    is com.example.data.CloudSqlSyncManager.SyncStatus.Success -> Pair(OmnisEmerald, Icons.Default.CloudDone)
                    com.example.data.CloudSqlSyncManager.SyncStatus.Idle -> Pair(OmnisTextMuted, Icons.Default.Sync)
                }

                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .testTag("admin_topbar_cloudsql_sync_button")
                        .size(30.dp)
                ) {
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = "Cloud SQL Synchronizace",
                        tint = syncTint,
                        modifier = Modifier
                            .size(17.dp)
                            .then(if (isSpinning) Modifier.rotate(syncRotation) else Modifier)
                    )
                }

                if (activeTab == OmnisTab.CHAT) {
                    IconButton(
                        onClick = onNewThreadClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Nové vlákno",
                            tint = OmnisAmber,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                com.example.ui.localization.LanguageToggleChip(isCompact = true)

                // Více možností (Overflow Menu)
                var showAdminOverflowMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showAdminOverflowMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Více možností",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAdminOverflowMenu,
                        onDismissRequest = { showAdminOverflowMenu = false },
                        modifier = Modifier.background(OmnisPanelDark)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
                                    Text("Interaktivní nápověda", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showAdminOverflowMenu = false
                                onHelpClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Text("Vymazat historii paměti", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showAdminOverflowMenu = false
                                onDeleteHistoryClick()
                            }
                        )

                        if (activeTab == OmnisTab.CHAT) {
                            HorizontalDivider(color = OmnisBorderDark)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
                                        Text("Export do PDF", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showAdminOverflowMenu = false
                                    onExportPdfClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(16.dp))
                                        Text("Export do Markdown (.md)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showAdminOverflowMenu = false
                                    onExportMarkdownClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                        Text("Certifikovaný Auditní Protokol", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showAdminOverflowMenu = false
                                    onExportCertifiedAuditClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun getAdminTabTitle(tab: OmnisTab): String {
    return when (tab) {
        OmnisTab.ADMIN -> "Admin"
        OmnisTab.MATRIX -> "Matrix"
        OmnisTab.TELEMETRY -> "Metriky"
        OmnisTab.CHAT -> "Chat"
        OmnisTab.NEXUS -> "Nexus"
        OmnisTab.GOALS -> "Cíle"
        OmnisTab.ARTIFACTS -> "Artefakty"
        else -> com.example.ui.localization.OmnisStrings.tabTitle(tab)
    }
}
