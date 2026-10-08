package com.example.ui.dialectics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dialectics.AgentArenaEngine
import com.example.dialectics.ArenaDebateResult
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * Dialog pro vizuální a numerické porovnání dvou historických 8D debat.
 */
@Composable
fun Arena8DComparisonModal(
    debateA: ArenaDebateResult,
    debateB: ArenaDebateResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisCardDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Compare",
                        tint = OmnisCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "SROVNÁNÍ DVOI 8D DEBAT",
                        color = OmnisCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = OmnisTextLight)
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Titles Comparison
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Debate A Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = OmnisBgDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "DEBATA 1",
                                    color = OmnisCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = debateA.problemStatement,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${debateA.agentA.name} vs ${debateA.agentB.name}",
                                    color = OmnisTextLight,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        // Debate B Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = OmnisBgDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "DEBATA 2",
                                    color = OmnisEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = debateB.problemStatement,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${debateB.agentA.name} vs ${debateB.agentB.name}",
                                    color = OmnisTextLight,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Superimposed 8D Spiderweb Canvas Comparison
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OmnisBgDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🕷️ POROVNÁNÍ SYNTEZOVANÝCH 8D POLYGONŮ",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                Dynamic8DInfluenceSpiderGraph(
                                    agentA = debateA.agentA,
                                    agentB = debateB.agentA,
                                    agentAScores = debateA.synthesized8DScores,
                                    agentBScores = debateB.synthesized8DScores,
                                    synthesizedScores = null,
                                    isLive = false
                                )
                            }
                        }
                    }
                }

                // Delta Comparison Table
                item {
                    Text(
                        text = "📊 DELTA (Δ) ODCHYLKY DVOI DEBAT V 8D DOMÉNÁCH:",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AgentArenaEngine.all8DDomains.forEach { domain ->
                            val scoreA = debateA.synthesized8DScores[domain] ?: 0f
                            val scoreB = debateB.synthesized8DScores[domain] ?: 0f
                            val delta = ((scoreA - scoreB) * 100f).toInt() / 100f

                            Surface(
                                color = OmnisBgDark,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = domain,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(text = "D1: $scoreA", color = OmnisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = "D2: $scoreB", color = OmnisEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = "Δ: ${if (delta > 0) "+$delta" else "$delta"}",
                                            color = if (abs(delta) > 0.3f) Color.Yellow else OmnisTextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ZAVŘÍT SROVNÁNÍ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    )
}
