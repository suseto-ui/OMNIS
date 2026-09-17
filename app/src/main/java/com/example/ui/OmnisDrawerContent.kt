package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserRole
import com.example.ui.theme.*

/**
 * Extrahovaná komponenta postranního menu (Navigation Drawer) pro O.M.N.I.S.
 */
@Composable
fun OmnisDrawerContent(
    activeTab: OmnisTab,
    currentRole: UserRole,
    isCircuitBreakerTripped: Boolean,
    onTabSelected: (OmnisTab) -> Unit,
    onToggleCircuitBreaker: () -> Unit,
    onSwitchToAdminClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = OmnisBgDark,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier.width(280.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                contentAlignment = Alignment.Center
            ) {
                Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column {
                Text("O.M.N.I.S.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Kognitivní Řízení v2.7", color = OmnisCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 12.dp))
        
        NavigationDrawerItem(
            label = { Text("Kognitivní Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = activeTab == OmnisTab.CHAT,
            onClick = { onTabSelected(OmnisTab.CHAT) },
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                selectedIconColor = OmnisCyan,
                selectedTextColor = OmnisCyan,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = OmnisTextMuted,
                unselectedTextColor = OmnisTextMuted
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(52.dp)
        )

        NavigationDrawerItem(
            label = { Text("Analytický Přehled", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = activeTab == OmnisTab.ANALYTICS,
            onClick = { onTabSelected(OmnisTab.ANALYTICS) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                selectedIconColor = OmnisCyan,
                selectedTextColor = OmnisCyan,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = OmnisTextMuted,
                unselectedTextColor = OmnisTextMuted
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(52.dp)
        )
        
        NavigationDrawerItem(
            label = { Text("Historická Paměť & Archiv", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            selected = activeTab == OmnisTab.MEMORY,
            onClick = { onTabSelected(OmnisTab.MEMORY) },
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                selectedIconColor = OmnisCyan,
                selectedTextColor = OmnisCyan,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = OmnisTextMuted,
                unselectedTextColor = OmnisTextMuted
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(52.dp)
        )

        if (currentRole.canAccessSystemActions()) {
            NavigationDrawerItem(
                label = { Text("Administrátorské Centrum", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selected = activeTab == OmnisTab.ADMIN,
                onClick = { onTabSelected(OmnisTab.ADMIN) },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OmnisAmber) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = OmnisAmber.copy(alpha = 0.15f),
                    selectedIconColor = OmnisAmber,
                    selectedTextColor = OmnisAmber,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = OmnisAmber,
                    unselectedTextColor = OmnisAmber
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(52.dp)
            )

            NavigationDrawerItem(
                label = { Text("Kognitivní Uzly & Invarianty", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selected = activeTab == OmnisTab.NODES,
                onClick = { onTabSelected(OmnisTab.NODES) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                    selectedIconColor = OmnisCyan,
                    selectedTextColor = OmnisCyan,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = OmnisTextMuted,
                    unselectedTextColor = OmnisTextMuted
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(52.dp)
            )

            NavigationDrawerItem(
                label = { Text("Octagon 8D Matice", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selected = activeTab == OmnisTab.MATRIX,
                onClick = { onTabSelected(OmnisTab.MATRIX) },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = OmnisCyan.copy(alpha = 0.15f),
                    selectedIconColor = OmnisCyan,
                    selectedTextColor = OmnisCyan,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = OmnisTextMuted,
                    unselectedTextColor = OmnisTextMuted
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(52.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 8.dp))

        // Admin Circuit Breaker Switch (Hlavní Jistič)
        if (currentRole.canAccessSystemActions()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = OmnisPanelDark,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCircuitBreakerTripped) Color.Red else OmnisAmber)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            modifier = Modifier.size(18.dp)
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
            Spacer(modifier = Modifier.height(4.dp))
        }
        HorizontalDivider(color = OmnisBorderDark, modifier = Modifier.padding(vertical = 8.dp))

        // Switch to Admin Mode Button (pokud je přihlášen běžný uživatel)
        if (currentRole == UserRole.STANDARD_USER) {
            NavigationDrawerItem(
                label = { Text("Přepnout do Admin Režimu", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selected = false,
                onClick = onSwitchToAdminClick,
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OmnisAmber) },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = OmnisAmber.copy(alpha = 0.1f),
                    unselectedIconColor = OmnisAmber,
                    unselectedTextColor = OmnisAmber
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp)
            )
        }

        // Role badge and Logout
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = OmnisCardDark,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Role:", color = OmnisTextMuted, fontSize = 10.sp)
                    Text(
                        text = if (currentRole == UserRole.ADMIN_OPERATOR) "Admin / Operátor" else "Běžný Uživatel",
                        color = if (currentRole == UserRole.ADMIN_OPERATOR) OmnisAmber else OmnisCyan,
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
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Odhlásit se",
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
