package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import com.example.data.OmnisRecord
import com.example.ui.theme.*

/**
 * Responzivní horní navigační lišta O.M.N.I.S.
 * Přizpůsobuje se kontextu (Kokpit vs Podsekce), eliminuje přeplnění a ořez prvků.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnisTopAppBar(
    records: List<OmnisRecord>,
    activeTab: OmnisTab = OmnisTab.DASHBOARD,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onOpenMenu: () -> Unit,
    onClearSessionClick: () -> Unit
) {
    val isHighEntropyPair by remember(records) {
        derivedStateOf {
            val last10Scores = records.takeLast(10).map { it.compositeScore }
            val avgScore = if (last10Scores.isNotEmpty()) last10Scores.average().toFloat() else 0.85f
            val highEntropy = avgScore < 0.60f
            Pair(highEntropy, (avgScore * 100).toInt())
        }
    }

    val isHighEntropy = isHighEntropyPair.first
    val scorePercentage = isHighEntropyPair.second
    val isSubScreen = activeTab != OmnisTab.DASHBOARD

    val tabTitle = when (activeTab) {
        OmnisTab.DASHBOARD -> "Kokpit"
        OmnisTab.CHAT -> "Chat"
        OmnisTab.NEXUS -> "Nexus"
        OmnisTab.ANALYTICS -> "Analýza"
        OmnisTab.MEMORY -> "Paměť"
        OmnisTab.NODES -> "Invarianty"
        OmnisTab.ADMIN -> "Admin"
        OmnisTab.MATRIX -> "8D Matice"
        OmnisTab.TEST_SEMANTIC -> "Test"
        OmnisTab.ARTIFACTS -> "Artefakty"
        OmnisTab.SCENARIOS -> "Scénáře"
        OmnisTab.GOALS -> "Cíle"
        OmnisTab.TELEMETRY -> "Telemetrie"
        OmnisTab.GUIDE -> "Metodika"
        OmnisTab.DEV_PROMPT_LAB -> "Dev Lab"
    }

    TopAppBar(
        navigationIcon = {
            if (isSubScreen) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .testTag("nav_back_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zpět",
                        tint = OmnisCyan
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .testTag("hamburger_menu_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Otevřít menu",
                        tint = OmnisCyan
                    )
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ω", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                
                Text(
                    text = "O.M.N.I.S.",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )

                if (isSubScreen) {
                    // Podsekce badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = tabTitle.uppercase(),
                            color = OmnisCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    // Integrita / Entropie badge na domovské obrazovce
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isHighEntropy) Color.Red.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHighEntropy) Color.Red else OmnisEmerald.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(if (isHighEntropy) Color.Red else OmnisEmerald, CircleShape)
                            )
                            Text(
                                text = "$scorePercentage%",
                                color = if (isHighEntropy) Color.Red else OmnisEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.statusBarsPadding(),
        actions = {
            if (isSubScreen) {
                // V podsekcích umožníme rychlý přístup k bočnímu menu zprava
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .testTag("action_drawer_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Otevřít menu",
                        tint = OmnisTextMuted
                    )
                }
            } else {
                // Na domovské obrazovce tlačítko vymazání relace
                IconButton(
                    onClick = onClearSessionClick,
                    modifier = Modifier
                        .testTag("clear_history_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Vymazat relaci",
                        tint = OmnisTextMuted
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OmnisPanelDark.copy(alpha = 0.95f)
        )
    )
}
