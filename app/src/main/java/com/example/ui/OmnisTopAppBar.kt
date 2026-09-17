package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Optimalizovaná komponenta horní lišty s paměťově stabilizovaným výpočtem entropie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnisTopAppBar(
    records: List<OmnisRecord>,
    onOpenMenu: () -> Unit,
    onClearSessionClick: () -> Unit
) {
    // Optimalizace: Použití derivedStateOf pro přepočet entropie pouze při změně seznamu záznamů
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

    TopAppBar(
        navigationIcon = {
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
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ω", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "O.M.N.I.S.",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OmnisCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "v2.7",
                                color = OmnisCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        // System Integrity & Entropy Visualizer Indicator
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isHighEntropy) Color.Red.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isHighEntropy) Color.Red else OmnisEmerald.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isHighEntropy) Color.Red else OmnisEmerald, CircleShape)
                                )
                                Text(
                                    text = if (isHighEntropy) "ENTROPIE ($scorePercentage%)" else "INTEGRITA ($scorePercentage%)",
                                    color = if (isHighEntropy) Color.Red else OmnisEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .statusBarsPadding()
            .height(48.dp),
        actions = {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OmnisPanelDark.copy(alpha = 0.95f)
        )
    )
}
