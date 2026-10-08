package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OmnisTab
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocaleManager
import com.example.ui.localization.OmnisStrings
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
    userExperienceMode: com.example.ui.UserExperienceMode = com.example.ui.UserExperienceMode.STANDARD,
    onToggleUserMode: () -> Unit = {},
    onMenuClick: () -> Unit,
    onDeleteHistoryClick: () -> Unit,
    onNewThreadClick: () -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    onExportMarkdownClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
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
                    contentDescription = "Otevřít nabídku",
                    tint = Color.White
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "O.M.N.I.S.",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Surface(
                    color = OmnisCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, OmnisCyan, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = getUserTabTitle(activeTab),
                        color = OmnisCyan,
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
                            color = OmnisTextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 100.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Přepínač režimu: Standard vs Expert (Zjednodušený vs Expert)
                Surface(
                    onClick = onToggleUserMode,
                    shape = RoundedCornerShape(14.dp),
                    color = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald.copy(alpha = 0.18f) else OmnisViolet.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald else OmnisViolet
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) "🌱 Zač." else "⚡ Exp.",
                            color = if (userExperienceMode == com.example.ui.UserExperienceMode.STANDARD) OmnisEmerald else OmnisViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (activeTab == OmnisTab.CHAT) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onNewThreadClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Nové vlákno konverzace",
                            tint = OmnisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                com.example.ui.localization.LanguageToggleChip(isCompact = true)

                // Overflow Options Dropdown
                var showOverflowMenu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { showOverflowMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Více možností",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(OmnisPanelDark)
                    ) {
                        if (activeTab == OmnisTab.CHAT) {
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Export do PDF", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Export do Markdown (.md)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onExportMarkdownClick()
                                }
                            )
                        }

                        if (activeTab == OmnisTab.CHAT || activeTab == OmnisTab.MEMORY) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Vymazat historii", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onDeleteHistoryClick()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = OmnisCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Interaktivní nápověda", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showOverflowMenu = false
                                onHelpClick()
                            }
                        )
                    }
                }
            }
        }
    )
}

private fun getUserTabTitle(tab: OmnisTab): String {
    return when (tab) {
        OmnisTab.CHAT -> "Chat"
        OmnisTab.NEXUS -> "Nexus"
        OmnisTab.GOALS -> "Cíle"
        OmnisTab.ARTIFACTS -> "Artefakty"
        else -> com.example.ui.localization.OmnisStrings.tabTitle(tab)
    }
}

