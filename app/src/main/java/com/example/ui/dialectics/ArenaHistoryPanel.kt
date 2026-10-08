package com.example.ui.dialectics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dialectics.AgentArenaEngine
import com.example.dialectics.ArenaDebateResult
import com.example.ui.theme.*

/**
 * Panel historie pro Multi-Agentní Arénu O.M.N.I.S.
 * Umožňuje procházet předchozí souboje, načítat je zpět do arény a porovnávat jejich 8D grafy.
 */
@Composable
fun ArenaHistoryPanel(
    historyList: List<ArenaDebateResult>,
    onLoadDebate: (ArenaDebateResult) -> Unit,
    onCompareDebates: (ArenaDebateResult, ArenaDebateResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedForComparisonA by remember { mutableStateOf<ArenaDebateResult?>(null) }

    val filteredHistory = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.problemStatement.contains(searchQuery, ignoreCase = true) ||
                it.agentA.name.contains(searchQuery, ignoreCase = true) ||
                it.agentB.name.contains(searchQuery, ignoreCase = true) ||
                it.zkCommitmentHash.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History Panel",
                        tint = OmnisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "HISTORIE DEBAT & POROVNÁNÍ 8D GRAFŮ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OmnisCyan,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = OmnisBgDark,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    Text(
                        text = "${historyList.size} ZÁZNAMŮ",
                        color = OmnisTextLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Comparison Selection Alert Banner if one debate is selected
            selectedForComparisonA?.let { debateA ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OmnisCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRO SROVNÁNÍ VYBRÁNA 1. DEBATA:",
                                color = OmnisCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = debateA.problemStatement.take(45) + "...",
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Nyní klikněte 'POROVNAT GRAF' u druhé debaty níže",
                                color = OmnisTextLight,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = { selectedForComparisonA = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                    }
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Hledat v historii debat (téma, agent, ZK-hash)...",
                        color = OmnisTextLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OmnisCyan,
                    unfocusedBorderColor = OmnisBorderDark,
                    focusedContainerColor = OmnisBgDark,
                    unfocusedContainerColor = OmnisBgDark
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = OmnisTextLight, modifier = Modifier.size(18.dp))
                }
            )

            // History List
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Žádné předchozí debaty neodpovídají zadanému filtru.",
                        color = OmnisTextLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredHistory.forEach { debate ->
                        val isFirstSelected = selectedForComparisonA?.debateId == debate.debateId

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFirstSelected) OmnisCyan.copy(alpha = 0.1f) else OmnisBgDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isFirstSelected) OmnisCyan else OmnisBorderDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Agents & Consensus Metrics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = debate.agentA.name,
                                            color = Color(debate.agentA.colorHex),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(text = "VS", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                        Text(
                                            text = debate.agentB.name,
                                            color = Color(debate.agentB.colorHex),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Surface(
                                        color = OmnisEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald)
                                    ) {
                                        Text(
                                            text = "KONSENZUS ${(debate.consensusIndex * 100).toInt()}%",
                                            color = OmnisEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Topic Title
                                Text(
                                    text = debate.problemStatement,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp
                                )

                                // ZK Hash & Divergence Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ZK-SNARK: ${debate.zkCommitmentHash}",
                                        color = OmnisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Divergence: ${(debate.divergenceIndex * 100).toInt()}%",
                                        color = OmnisTextLight,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Divider(color = OmnisBorderDark.copy(alpha = 0.5f))

                                // Action Buttons: Load Debate vs Compare
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onLoadDebate(debate) },
                                        colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "NAČÍST DO ARÉNY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (selectedForComparisonA == null) {
                                                selectedForComparisonA = debate
                                            } else if (selectedForComparisonA?.debateId != debate.debateId) {
                                                onCompareDebates(selectedForComparisonA!!, debate)
                                                selectedForComparisonA = null
                                            }
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isFirstSelected) OmnisEmerald else OmnisBorderDark
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = null,
                                            tint = if (isFirstSelected) OmnisEmerald else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isFirstSelected) "VYBRÁNO JAKO 1." else "POROVNAT GRAF",
                                            color = if (isFirstSelected) OmnisEmerald else Color.White,
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
        }
    }
}
