package com.example.ui.cockpit

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dialectics.*
import com.example.ui.OmnisTab
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CockpitAgentArenaCard(
    lowestDim: Triple<String, Float, String>?,
    onSendToChat: (String) -> Unit,
    onTabSwitch: (OmnisTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val selectedAgentA by remember { mutableStateOf(AgentArenaLibrary.presetArchetypes[0]) } // Conservative Analyst
    val selectedAgentB by remember { mutableStateOf(AgentArenaLibrary.presetArchetypes[1]) } // Radical Innovator
    
    var isDebating by remember { mutableStateOf(false) }
    var currentTurnTitle by remember { mutableStateOf("Připraveno ke kognitivnímu duelu") }
    var debateResult by remember { mutableStateOf<ArenaDebateResult?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    fun runQuickDebate() {
        if (isDebating) return
        isDebating = true
        debateResult = null
        currentTurnTitle = "Kolo 1: Spuštění tezí..."

        val problem = if (lowestDim != null) {
            "Optimalizace a mitigace rizik v dimenzi ${lowestDim.first}: ${lowestDim.third} (aktuální skóre: ${(lowestDim.second * 100).toInt()}%)"
        } else {
            "Autonomní rebalancování toků a topologická stabilita O.M.N.I.S."
        }

        coroutineScope.launch {
            val result = AgentArenaEngine.executeArenaDebate(
                problemStatement = problem,
                agentA = selectedAgentA,
                agentB = selectedAgentB,
                onProgressUpdate = { turn ->
                    currentTurnTitle = "${turn.roundName} (${turn.agentName})"
                }
            )
            debateResult = result
            isDebating = false
            currentTurnTitle = "Konsenzus dosažen (Skóre: ${(result.consensusIndex * 100).toInt()}%)"

            // Propagovat syntetizované 8D hodnoty do AKGE-8D Tenzorového jádra
            result.synthesized8DScores.forEach { (domain, score) ->
                com.example.engine.AkgeTensorKernel.updateDomainWeight(domain, score)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("cockpit_multiagent_arena_card"),
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OmnisViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            tint = OmnisViolet,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DIALEKTICKÁ MULTI-AGENTNÍ ARÉNA",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = currentTurnTitle,
                            color = if (isDebating) OmnisCyan else OmnisTextMuted,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpanded = !isExpanded
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Rozbalit",
                            tint = OmnisTextMuted
                        )
                    }
                }
            }

            // Quick Agents Display & Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Agents badge row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(selectedAgentA.colorHex).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(selectedAgentA.colorHex))
                    ) {
                        Text(
                            text = selectedAgentA.name,
                            color = Color(selectedAgentA.colorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text("VS", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(selectedAgentB.colorHex).copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color(selectedAgentB.colorHex))
                    ) {
                        Text(
                            text = selectedAgentB.name,
                            color = Color(selectedAgentB.colorHex),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Run Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        runQuickDebate()
                    },
                    enabled = !isDebating,
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp).testTag("cockpit_arena_start_button")
                ) {
                    if (isDebating) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isDebating) "PROBÍHÁ DUEL..." else "SPUSTIT DUEL",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Expanded / Result View
            AnimatedVisibility(visible = isExpanded || debateResult != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = OmnisBorderDark, thickness = 0.5.dp)

                    if (debateResult != null) {
                        val res = debateResult!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "VÝSLEDEK KONSENZU",
                                    color = OmnisTextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Index konsenzus: ${(res.consensusIndex * 100).toInt()}% (${if (res.consensusIndex >= 0.7f) "ROBUSTNÍ" else "DIVERGENTNÍ"})",
                                    color = if (res.consensusIndex >= 0.7f) OmnisEmerald else OmnisAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = OmnisCyan.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, OmnisCyan)
                            ) {
                                Text(
                                    text = "ZK: ${res.zkCommitmentHash}",
                                    color = OmnisCyan,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        // Argumentation Log Panel
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LOG ARGUMENTACE (FÁZE):",
                                color = OmnisTextMuted,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            res.turns.forEach { turn ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(OmnisPanelDark.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${turn.roundName} | ${turn.agentName}",
                                        color = OmnisViolet,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = turn.argumentText,
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = res.synthesisSummary,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val prompt = "Aplikuj syntézu z Dialektické Arény (${res.problemStatement}):\n${res.synthesisSummary}\nDoporučení:\n${res.actionableGuidelines.joinToString("\n") { "- $it" }}"
                                    onSendToChat(prompt)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, OmnisCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp).weight(1f)
                            ) {
                                Text("Odeslat syntézu do Chatu", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSwitch(OmnisTab.DIALECTICS)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, OmnisViolet),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Plná Aréna →", color = OmnisViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Aréna je připravena provést dialektický střet mezi zvolenými archetypy s využitím 8D tiskového modelu a derivace kognitivních metrik.",
                            color = OmnisTextMuted,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTabSwitch(OmnisTab.DIALECTICS)
                                }
                            ) {
                                Text("Otevřít pokročilou záložku Arény v plném zobrazení →", color = OmnisCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
