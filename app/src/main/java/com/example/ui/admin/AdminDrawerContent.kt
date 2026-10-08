package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.ThreadSummary
import com.example.ui.OmnisTab
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Striktně administrátorský navigační panel (Admin Drawer).
 * Obsahuje systémové ovládací prvky, správu vláken všech uživatelů, jističe a administraci.
 * Fyzicky odděleno od uživatelského rozhraní.
 */
@Composable
fun AdminDrawerContent(
    activeTab: OmnisTab,
    isCircuitBreakerTripped: Boolean,
    isPromptGatewayEnabled: Boolean = true,
    availableThreads: List<ThreadSummary> = emptyList(),
    activeThreadId: String = "",
    allUserNames: List<String> = emptyList(),
    adminSelectedUserFilter: String? = null,
    onTabSelected: (OmnisTab) -> Unit,
    onSelectUserFilter: (String?) -> Unit = {},
    onSelectThread: (threadId: String, title: String) -> Unit = { _, _ -> },
    onCreateNewThread: () -> Unit = {},
    onDeleteThread: (threadId: String) -> Unit = {},
    onRenameThread: (threadId: String, newTitle: String) -> Unit = { _, _ -> },
    onToggleCircuitBreaker: () -> Unit,
    onTogglePromptGateway: () -> Unit = {},
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
                    text = "Přejmenovat vlákno (Admin)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nový název pro vlákno uživatele ${threadToRename?.userName}:",
                        color = OmnisTextLight,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisAmber,
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
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisAmber, contentColor = Color.Black)
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
            .width(280.dp)
            .fillMaxHeight(),
        drawerContainerColor = OmnisBgDark,
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hlavička Admin
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = OmnisAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisAmber)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = OmnisAmber,
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
                        text = "ADMINISTRACE & SYSTÉM",
                        color = OmnisAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 6.dp))

            // Sekce: Správa Vláken a Historie (Admin Thread Management)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionHeader("SPRÁVA VLÁKEN (${availableThreads.size})")
                
                Button(
                    onClick = {
                        onCreateNewThread()
                        onTabSelected(OmnisTab.CHAT)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnisAmber.copy(alpha = 0.2f),
                        contentColor = OmnisAmber
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, OmnisAmber.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nové vlákno", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ NOVÉ", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Filtr podle uživatelů (Admin Filter Selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Všechna vlákna chip
                val isAllSelected = adminSelectedUserFilter == null
                Surface(
                    modifier = Modifier.clickable { onSelectUserFilter(null) },
                    color = if (isAllSelected) OmnisAmber.copy(alpha = 0.25f) else OmnisPanelDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isAllSelected) OmnisAmber else OmnisBorderDark
                    )
                ) {
                    Text(
                        text = "🌐 Všechna (Nejnovější)",
                        color = if (isAllSelected) OmnisAmber else OmnisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Jednotliví uživatelé
                allUserNames.forEach { uName ->
                    val isSelected = adminSelectedUserFilter == uName
                    Surface(
                        modifier = Modifier.clickable { onSelectUserFilter(uName) },
                        color = if (isSelected) OmnisCyan.copy(alpha = 0.25f) else OmnisPanelDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) OmnisCyan else OmnisBorderDark
                        )
                    ) {
                        Text(
                            text = "👤 $uName",
                            color = if (isSelected) OmnisCyan else OmnisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Seznam vláken pro Admina
            if (availableThreads.isEmpty()) {
                Text(
                    text = "Žádná vlákna neodpovídají filtru",
                    color = OmnisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
                        color = if (isActive) OmnisAmber.copy(alpha = 0.15f) else OmnisPanelDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, OmnisAmber)
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
                                    tint = if (isActive) OmnisAmber else OmnisTextMuted,
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = OmnisCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(3.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "👤 ${thread.userName}",
                                                color = OmnisCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .widthIn(max = 70.dp)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            text = formatTimestamp(thread.lastTimestamp),
                                            color = OmnisTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "• ${thread.messageCount} zpr.",
                                            color = if (isActive) OmnisAmber.copy(alpha = 0.8f) else OmnisTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
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

            HorizontalDivider(color = OmnisBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

            // Sekce: Administrace
            AdminSectionHeader("SYSTÉMOVÁ ADMINISTRACE")

            AdminDrawerItem(
                title = "Administrátorské Centrum",
                icon = Icons.Default.AdminPanelSettings,
                selected = activeTab == OmnisTab.ADMIN,
                tint = OmnisAmber,
                onClick = { onTabSelected(OmnisTab.ADMIN) }
            )

            AdminDrawerItem(
                title = "Kognitivní Uzly & Invarianty",
                icon = Icons.Default.AccountTree,
                selected = activeTab == OmnisTab.NODES,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.NODES) }
            )

            AdminDrawerItem(
                title = "Octagon 8D Matice",
                icon = Icons.Default.Grid4x4,
                selected = activeTab == OmnisTab.MATRIX,
                tint = OmnisViolet,
                onClick = { onTabSelected(OmnisTab.MATRIX) }
            )

            AdminDrawerItem(
                title = "Systémová Telemetrie",
                icon = Icons.Default.Dns,
                selected = activeTab == OmnisTab.TELEMETRY,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.TELEMETRY) }
            )

            AdminDrawerItem(
                title = "Systémová Dokumentace",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                selected = activeTab == OmnisTab.GUIDE,
                tint = OmnisAmber,
                onClick = { onTabSelected(OmnisTab.GUIDE) }
            )

            AdminDrawerItem(
                title = "Sémantický Testovací Chat",
                icon = Icons.Default.Science,
                selected = activeTab == OmnisTab.TEST_SEMANTIC,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.TEST_SEMANTIC) }
            )

            AdminDrawerItem(
                title = "Dev Prompt Lab",
                icon = Icons.Default.Terminal,
                selected = activeTab == OmnisTab.DEV_PROMPT_LAB,
                tint = OmnisViolet,
                onClick = { onTabSelected(OmnisTab.DEV_PROMPT_LAB) }
            )

            AdminDrawerItem(
                title = "Produkční Audit & Benchmark",
                icon = Icons.Default.VerifiedUser,
                selected = activeTab == OmnisTab.PRODUCTION_AUDIT,
                tint = OmnisEmerald,
                onClick = { onTabSelected(OmnisTab.PRODUCTION_AUDIT) }
            )

            HorizontalDivider(color = OmnisBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

            // Sekce: Navigace & Kokpit
            AdminSectionHeader("NAVIGACE & KOKPIT")

            AdminDrawerItem(
                title = "Operační Kokpit",
                icon = Icons.Default.Speed,
                selected = activeTab == OmnisTab.DASHBOARD,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.DASHBOARD) }
            )

            AdminDrawerItem(
                title = "Kognitivní Chat",
                icon = Icons.AutoMirrored.Filled.Chat,
                selected = activeTab == OmnisTab.CHAT,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.CHAT) }
            )

            AdminDrawerItem(
                title = "Neural Nexus",
                icon = Icons.Default.Hub,
                selected = activeTab == OmnisTab.NEXUS,
                tint = OmnisEmerald,
                onClick = { onTabSelected(OmnisTab.NEXUS) }
            )

            AdminDrawerItem(
                title = "Scenario Architect",
                icon = Icons.Default.Timeline,
                selected = activeTab == OmnisTab.SCENARIOS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.SCENARIOS) }
            )

            AdminDrawerItem(
                title = "Do(X) Kauzální Simulátor",
                icon = Icons.Default.AccountTree,
                selected = activeTab == OmnisTab.CAUSAL_SIMULATOR,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.CAUSAL_SIMULATOR) }
            )

            AdminDrawerItem(
                title = "Autonomní Cíle",
                icon = Icons.Default.Flag,
                selected = activeTab == OmnisTab.GOALS,
                tint = OmnisAmber,
                onClick = { onTabSelected(OmnisTab.GOALS) }
            )

            AdminDrawerItem(
                title = "Artefakty & Výstupy",
                icon = Icons.Default.Inventory2,
                selected = activeTab == OmnisTab.ARTIFACTS,
                tint = OmnisViolet,
                onClick = { onTabSelected(OmnisTab.ARTIFACTS) }
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 6.dp))

            // Admin Semantic Gateway Switch (Sémantická Brána & Quality Gate)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = OmnisPanelDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPromptGatewayEnabled) Icons.Default.Shield else Icons.Default.GppMaybe,
                            contentDescription = null,
                            tint = if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "SÉMANTICKÁ BRÁNA",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isPromptGatewayEnabled) "ZAPNUTO (Filtrování & Elevace)" else "VYPNUTO (Bypass / Direct Pass)",
                                color = if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Switch(
                        checked = isPromptGatewayEnabled,
                        onCheckedChange = { onTogglePromptGateway() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OmnisCyan,
                            uncheckedThumbColor = OmnisAmber,
                            checkedTrackColor = OmnisCyan.copy(alpha = 0.3f),
                            uncheckedTrackColor = OmnisAmber.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Admin Circuit Breaker Switch (Hlavní Jistič)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = OmnisPanelDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCircuitBreakerTripped) Color.Red else OmnisAmber)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isCircuitBreakerTripped) Color.Red else OmnisAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("HLAVNÍ JISTIČ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (isCircuitBreakerTripped) "VYPNUTO (Blokáda)" else "ZAPNUTO (Aktivní)",
                                color = if (isCircuitBreakerTripped) Color.Red else OmnisEmerald,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Switch(
                        checked = !isCircuitBreakerTripped,
                        onCheckedChange = { onToggleCircuitBreaker() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OmnisEmerald,
                            uncheckedThumbColor = Color.Red,
                            checkedTrackColor = OmnisEmerald.copy(alpha = 0.3f),
                            uncheckedTrackColor = Color.Red.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Odhlášení Admina
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                                .background(OmnisAmber)
                        ) { }
                        Text(
                            text = "Admin / Operátor",
                            color = OmnisAmber,
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

            // Jazykový přepínač v patičce draweru
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JAZYK / LANGUAGE",
                    color = OmnisTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                com.example.ui.localization.LanguageToggleChip()
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
private fun AdminSectionHeader(text: String) {
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
private fun AdminDrawerItem(
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
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

