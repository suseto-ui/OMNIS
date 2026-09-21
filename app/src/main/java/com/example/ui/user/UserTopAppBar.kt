package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OmnisTab
import com.example.ui.theme.*

/**
 * Uživatelská lišta aplikace (TopAppBar).
 * Čistě orientovaná na uživatelskou práci, bez admin odznaků nebo systémových stavů.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTopAppBar(
    activeTab: OmnisTab,
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
                    contentDescription = "Otevřít nabídku",
                    tint = Color.White
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
                    color = OmnisCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, OmnisCyan, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = getUserTabTitle(activeTab),
                        color = OmnisCyan,
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
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
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
            if (activeTab == OmnisTab.CHAT) {
                IconButton(onClick = onNewThreadClick) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Nové vlákno konverzace",
                        tint = OmnisCyan
                    )
                }

                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportovat vlákno",
                            tint = OmnisTextLight
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
                                        tint = OmnisCyan,
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

            if (activeTab == OmnisTab.CHAT || activeTab == OmnisTab.MEMORY) {
                IconButton(onClick = onDeleteHistoryClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Smazat historii",
                        tint = OmnisTextMuted
                    )
                }
            }

            IconButton(onClick = onHelpClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Interaktivní nápověda",
                    tint = OmnisCyan
                )
            }
        }
    )
}

private fun getUserTabTitle(tab: OmnisTab): String {
    return when (tab) {
        OmnisTab.DASHBOARD -> "KOKPIT"
        OmnisTab.CHAT -> "CHAT"
        OmnisTab.NEXUS -> "NEXUS"
        OmnisTab.GUIDE -> "PRŮVODCE"
        OmnisTab.GOALS -> "CÍLE"
        OmnisTab.SCENARIOS -> "SCÉNÁŘE"
        OmnisTab.ARTIFACTS -> "ARTEFAKTY"
        OmnisTab.MEMORY -> "PAMĚŤ"
        OmnisTab.ANALYTICS -> "ANALYTIKA"
        else -> "UŽIVATEL"
    }
}

