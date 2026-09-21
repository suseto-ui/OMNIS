package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserRole
import com.example.data.ThreadSummary
import com.example.ui.OmnisTab
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Striktně uživatelský navigační panel (Drawer).
 * Obsahuje automaticky načtená vlákna přihlášeného uživatele a konverzační moduly.
 */
@Composable
fun UserDrawerContent(
    activeTab: OmnisTab,
    availableThreads: List<ThreadSummary> = emptyList(),
    activeThreadId: String = "",
    onTabSelected: (OmnisTab) -> Unit,
    onSelectThread: (threadId: String, title: String) -> Unit = { _, _ -> },
    onCreateNewThread: () -> Unit = {},
    onDeleteThread: (threadId: String) -> Unit = {},
    onRenameThread: (threadId: String, newTitle: String) -> Unit = { _, _ -> },
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var threadToRename by remember { mutableStateOf<ThreadSummary?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    if (threadToRename != null) {
        AlertDialog(
            onDismissRequest = { threadToRename = null },
            title = {
                Text(
                    text = "Přejmenovat vlákno",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zadejte nový název vlákna:",
                        color = OmnisTextLight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        threadToRename?.let { thread ->
                            if (renameInputText.isNotBlank()) {
                                onRenameThread(thread.threadId, renameInputText.trim())
                            }
                        }
                        threadToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black)
                ) {
                    Text("Uložit", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToRename = null }) {
                    Text("Zrušit", color = OmnisTextMuted, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = OmnisPanelDark
        )
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = OmnisBgDark,
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hlavička Uživatel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = OmnisCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "O.M.N.I.S.",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "KOGNITIVNÍ PORTÁL",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 6.dp))

            // Sekce: Vlákna konverzace (Thread History)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserSectionHeader("VLÁKNA KONVERZACE (${availableThreads.size})")
                
                Button(
                    onClick = {
                        onCreateNewThread()
                        onTabSelected(OmnisTab.CHAT)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnisCyan.copy(alpha = 0.2f),
                        contentColor = OmnisCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nové vlákno", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ NOVÉ", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Seznam vláken
            if (availableThreads.isEmpty()) {
                Text(
                    text = "Žádná předchozí vlákna",
                    color = OmnisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                availableThreads.take(8).forEach { thread ->
                    val isActive = thread.threadId == activeThreadId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                onSelectThread(thread.threadId, thread.threadTitle)
                                onTabSelected(OmnisTab.CHAT)
                            },
                        color = if (isActive) OmnisCyan.copy(alpha = 0.15f) else OmnisPanelDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan)
                                 else androidx.compose.foundation.BorderStroke(0.5.dp, OmnisBorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isActive) OmnisCyan else OmnisTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = thread.threadTitle,
                                        color = if (isActive) Color.White else OmnisTextLight,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatTimestamp(thread.lastTimestamp),
                                            color = OmnisTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "• ${thread.messageCount} zpráv",
                                            color = if (isActive) OmnisCyan.copy(alpha = 0.8f) else OmnisTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        renameInputText = thread.threadTitle
                                        threadToRename = thread
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Přejmenovat vlákno",
                                        tint = OmnisTextMuted.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                if (availableThreads.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteThread(thread.threadId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Smazat vlákno",
                                            tint = OmnisTextMuted.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = OmnisBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

            // Sekce: Konverzační Prvky & Moduly
            UserSectionHeader("NAVIGACE & MODULY")

            UserDrawerItem(
                title = "Kognitivní Chat",
                icon = Icons.Default.Chat,
                selected = activeTab == OmnisTab.CHAT,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.CHAT) }
            )

            UserDrawerItem(
                title = "Neural Nexus",
                icon = Icons.Default.Hub,
                selected = activeTab == OmnisTab.NEXUS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.NEXUS) }
            )

            UserDrawerItem(
                title = "Operační Kokpit",
                icon = Icons.Default.Speed,
                selected = activeTab == OmnisTab.DASHBOARD,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.DASHBOARD) }
            )

            UserDrawerItem(
                title = "Metodika & Průvodce",
                icon = Icons.Default.MenuBook,
                selected = activeTab == OmnisTab.GUIDE,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.GUIDE) }
            )

            UserDrawerItem(
                title = "Autonomní Cíle",
                icon = Icons.Default.Flag,
                selected = activeTab == OmnisTab.GOALS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.GOALS) }
            )

            UserDrawerItem(
                title = "Scenario Architect",
                icon = Icons.Default.Timeline,
                selected = activeTab == OmnisTab.SCENARIOS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.SCENARIOS) }
            )

            UserDrawerItem(
                title = "Artefakty & Výstupy",
                icon = Icons.Default.Inventory2,
                selected = activeTab == OmnisTab.ARTIFACTS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.ARTIFACTS) }
            )

            UserDrawerItem(
                title = "Historická Paměť",
                icon = Icons.Default.Psychology,
                selected = activeTab == OmnisTab.MEMORY,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.MEMORY) }
            )

            UserDrawerItem(
                title = "Analytický Přehled",
                icon = Icons.Default.Analytics,
                selected = activeTab == OmnisTab.ANALYTICS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.ANALYTICS) }
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 6.dp))

            // Odhlášení / Informace o uživatelské relaci
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = OmnisCardDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OmnisEmerald)
                        ) { }
                        Text(
                            text = "Aktivní operátor",
                            color = OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Odhlásit se",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm dd.MM.", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun UserSectionHeader(text: String) {
    Text(
        text = text,
        color = OmnisTextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun UserDrawerItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        color = if (selected) tint.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) tint else OmnisTextMuted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = if (selected) Color.White else OmnisTextMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

