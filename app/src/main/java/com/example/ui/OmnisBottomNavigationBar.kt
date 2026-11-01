package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.UserRole
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocaleManager
import com.example.ui.localization.OmnisStrings
import com.example.ui.theme.*

data class NavigationTabItem(
    val tab: OmnisTab,
    val label: String,
    val icon: ImageVector,
    val activeColor: Color = OmnisCyan
)

@Composable
fun OmnisBottomNavigationBar(
    activeTab: OmnisTab,
    userRole: UserRole,
    userExperienceMode: UserExperienceMode = UserExperienceMode.STANDARD,
    onTabSelected: (OmnisTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()

    val items = if (userRole == UserRole.ADMIN_OPERATOR) {
        listOf(
            NavigationTabItem(OmnisTab.ADMIN, "Admin", Icons.Default.AdminPanelSettings, OmnisAmber),
            NavigationTabItem(OmnisTab.MATRIX, if (currentLang == AppLanguage.EN) "8D Matrix" else "8D", Icons.Default.Analytics, OmnisAmber),
            NavigationTabItem(OmnisTab.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat, OmnisCyan),
            NavigationTabItem(OmnisTab.NEXUS, "Nexus", Icons.Default.Hub, OmnisCyan),
            NavigationTabItem(OmnisTab.SETTINGS, if (currentLang == AppLanguage.EN) "Settings" else "Nastavení", Icons.Default.Settings, OmnisCyan)
        )
    } else {
        // Běžný uživatel: Chat, Kognitivní Nexus, Cíle, Artefakty a Nastavení
        listOf(
            NavigationTabItem(
                tab = OmnisTab.CHAT,
                label = if (currentLang == AppLanguage.EN) "Chat" else "Chat",
                icon = Icons.AutoMirrored.Filled.Chat,
                activeColor = OmnisCyan
            ),
            NavigationTabItem(
                tab = OmnisTab.NEXUS,
                label = if (currentLang == AppLanguage.EN) "Nexus" else "Nexus",
                icon = Icons.Default.Hub,
                activeColor = OmnisCyan
            ),
            NavigationTabItem(
                tab = OmnisTab.GOALS,
                label = if (currentLang == AppLanguage.EN) "Goals" else "Cíle",
                icon = Icons.Default.Flag,
                activeColor = OmnisEmerald
            ),
            NavigationTabItem(
                tab = OmnisTab.SETTINGS,
                label = if (currentLang == AppLanguage.EN) "Settings" else "Nastavení",
                icon = Icons.Default.Settings,
                activeColor = OmnisCyan
            )
        )
    }

    NavigationBar(
        containerColor = OmnisPanelDark,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { item ->
            val isSelected = activeTab == item.tab
            val tint = if (isSelected) item.activeColor else OmnisTextMuted

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = tint,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = item.activeColor.copy(alpha = 0.18f),
                    selectedIconColor = item.activeColor,
                    selectedTextColor = item.activeColor,
                    unselectedIconColor = OmnisTextMuted,
                    unselectedTextColor = OmnisTextMuted
                )
            )
        }
    }
}
