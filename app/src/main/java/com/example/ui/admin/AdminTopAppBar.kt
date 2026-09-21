package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OmnisTab
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
    onMenuClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onNewThreadClick: () -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    onExportMarkdownClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "O.M.N.I.S.",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = OmnisAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, OmnisAmber, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = getAdminTabTitle(activeTab),
                        color = OmnisAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            color = OmnisAmber.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 110.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        actions = {
            if (activeTab == OmnisTab.CHAT) {
                IconButton(onClick = onNewThreadClick) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Nové vlákno",
                        tint = OmnisAmber
                    )
                }

                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportovat vlákno",
                            tint = OmnisAmber.copy(alpha = 0.9f)
                        )
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.background(OmnisPanelDark)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = OmnisAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Export do PDF", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showExportMenu = false
                                onExportPdfClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = OmnisEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Export do Markdown (.md)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showExportMenu = false
                                onExportMarkdownClick()
                            }
                        )
                    }
                }
            }

            // Indikátor stavu Sémantické Brány (Gateway)
            Surface(
                color = if (isPromptGatewayEnabled) OmnisCyan.copy(alpha = 0.15f) else OmnisAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber)
                    )
                    Text(
                        text = if (isPromptGatewayEnabled) "GATEWAY ON" else "BYPASS",
                        color = if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Indikátor stavu jističe
            Surface(
                color = if (isCircuitBreakerTripped) Color.Red.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isCircuitBreakerTripped) Color.Red else OmnisEmerald),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isCircuitBreakerTripped) Color.Red else OmnisEmerald)
                    )
                    Text(
                        text = if (isCircuitBreakerTripped) "TRIPPED" else "SYS OK",
                        color = if (isCircuitBreakerTripped) Color.Red else OmnisEmerald,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onDeleteHistoryClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Vymazat historii paměti",
                    tint = OmnisTextMuted
                )
            }

            IconButton(onClick = onHelpClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Interaktivní nápověda",
                    tint = OmnisAmber
                )
            }
        }
    )
}


private fun getAdminTabTitle(tab: OmnisTab): String {
    return when (tab) {
        OmnisTab.ADMIN -> "ADMIN HUB"
        OmnisTab.NODES -> "UZLY & INVARIANTY"
        OmnisTab.MATRIX -> "8D MATICE"
        OmnisTab.TELEMETRY -> "TELEMETRIE"
        OmnisTab.TEST_SEMANTIC -> "TEST CHAT"
        OmnisTab.DASHBOARD -> "KOKPIT"
        OmnisTab.CHAT -> "CHAT"
        else -> "ADMIN"
    }
}
