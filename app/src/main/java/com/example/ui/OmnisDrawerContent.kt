package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserRole
import com.example.ui.theme.*

/**
 * Modální vysouvací navigační panel O.M.N.I.S.
 * Plně responzivní s dynamickým scrollováním, ochranou proti ořezu textu a podporou role operátora.
 */
@Composable
fun OmnisDrawerContent(
    activeTab: OmnisTab,
    isCircuitBreakerTripped: Boolean,
    currentRole: UserRole,
    onTabSelected: (OmnisTab) -> Unit,
    onToggleCircuitBreaker: () -> Unit,
    onSwitchToAdminClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = OmnisBgDark,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier
            .widthIn(min = 320.dp, max = 340.dp)
            .fillMaxWidth(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ω", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Column {
                    Text("O.M.N.I.S.", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Kognitivní Řízení v2.7", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Sekce: Konverzační Prvky
            DrawerSectionHeader("KONVERZAČNÍ PRVKY")

            DrawerItem(
                title = "Kognitivní Chat",
                icon = Icons.AutoMirrored.Filled.Send,
                selected = activeTab == OmnisTab.CHAT,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.CHAT) }
            )

            DrawerItem(
                title = "Neural Nexus",
                icon = Icons.Default.Hub,
                selected = activeTab == OmnisTab.NEXUS,
                tint = OmnisEmerald,
                onClick = { onTabSelected(OmnisTab.NEXUS) }
            )

            HorizontalDivider(color = OmnisBorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

            // Sekce: Informační Moduly & Přehledy
            DrawerSectionHeader("INFORMAČNÍ MODULY & PŘEHLEDY")

            DrawerItem(
                title = "Operační Kokpit",
                icon = Icons.Default.Speed,
                selected = activeTab == OmnisTab.DASHBOARD,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.DASHBOARD) }
            )

            DrawerItem(
                title = "Metodika & Průvodce",
                icon = Icons.Default.MenuBook,
                selected = activeTab == OmnisTab.GUIDE,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.GUIDE) }
            )

            DrawerItem(
                title = "Autonomní Cíle",
                icon = Icons.Default.Flag,
                selected = activeTab == OmnisTab.GOALS,
                tint = OmnisEmerald,
                onClick = { onTabSelected(OmnisTab.GOALS) }
            )

            DrawerItem(
                title = "Scenario Architect",
                icon = Icons.Default.Timeline,
                selected = activeTab == OmnisTab.SCENARIOS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.SCENARIOS) }
            )

            DrawerItem(
                title = "Artefakty & Výstupy",
                icon = Icons.Default.Inventory2,
                selected = activeTab == OmnisTab.ARTIFACTS,
                tint = OmnisViolet,
                onClick = { onTabSelected(OmnisTab.ARTIFACTS) }
            )

            DrawerItem(
                title = "Historická Paměť",
                icon = Icons.Default.Star,
                selected = activeTab == OmnisTab.MEMORY,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.MEMORY) }
            )

            DrawerItem(
                title = "Analytický Přehled (8D)",
                icon = Icons.Default.Analytics,
                selected = activeTab == OmnisTab.ANALYTICS,
                tint = OmnisCyan,
                onClick = { onTabSelected(OmnisTab.ANALYTICS) }
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

            // Odhlášení / Čisté ukončení relace
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = OmnisCardDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OmnisEmerald)
                        )
                        Text(
                            text = "Aktivní Relace",
                            color = OmnisTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Odhlásit se",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                lineHeight = 16.sp
            )
        },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = tint.copy(alpha = 0.15f),
            selectedIconColor = tint,
            selectedTextColor = tint,
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = OmnisTextMuted,
            unselectedTextColor = OmnisTextMuted
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .defaultMinSize(minHeight = 44.dp)
    )
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        color = OmnisCyan.copy(alpha = 0.7f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}
