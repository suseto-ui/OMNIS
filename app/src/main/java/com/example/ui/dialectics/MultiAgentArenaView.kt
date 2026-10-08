package com.example.ui.dialectics

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.dialectics.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MultiAgentArenaView(
    onSendToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedAgentA by remember { mutableStateOf(AgentArenaLibrary.presetArchetypes[0]) } // Conservative Analyst
    var selectedAgentB by remember { mutableStateOf(AgentArenaLibrary.presetArchetypes[1]) } // Radical Innovator
    var problemText by remember { mutableStateOf(AgentArenaLibrary.presetDebateTopics.first()) }

    var isDebating by remember { mutableStateOf(false) }
    var debateResult by remember { mutableStateOf<ArenaDebateResult?>(null) }
    var liveTurns by remember { mutableStateOf<List<ArenaDebateTurn>>(emptyList()) }

    var showAgentASelector by remember { mutableStateOf(false) }
    var showAgentBSelector by remember { mutableStateOf(false) }

    val historyList by AgentArenaEngine.debateHistoryState.collectAsState()
    var comparePair by remember { mutableStateOf<Pair<ArenaDebateResult, ArenaDebateResult>?>(null) }

    // Derive real-time active domain and dynamic scores from live turns or debate result
    val latestTurn = liveTurns.lastOrNull()
    val activeDomain = latestTurn?.highlightedDomains?.firstOrNull()

    val liveScoresA = remember(liveTurns, debateResult) {
        if (debateResult != null) {
            debateResult!!.agentA8DScores
        } else if (liveTurns.isNotEmpty()) {
            val scores = mutableMapOf<String, Float>()
            AgentArenaEngine.all8DDomains.forEach { domain ->
                val turnsA = liveTurns.filter { it.agentId == selectedAgentA.id }
                val lastScore = turnsA.lastOrNull()?.domainScores?.get(domain)
                scores[domain] = lastScore ?: 0f
            }
            scores
        } else {
            emptyMap()
        }
    }

    val liveScoresB = remember(liveTurns, debateResult) {
        if (debateResult != null) {
            debateResult!!.agentB8DScores
        } else if (liveTurns.isNotEmpty()) {
            val scores = mutableMapOf<String, Float>()
            AgentArenaEngine.all8DDomains.forEach { domain ->
                val turnsB = liveTurns.filter { it.agentId == selectedAgentB.id }
                val lastScore = turnsB.lastOrNull()?.domainScores?.get(domain)
                scores[domain] = lastScore ?: 0f
            }
            scores
        } else {
            emptyMap()
        }
    }

    fun startArenaDebate() {
        if (isDebating) return
        isDebating = true
        liveTurns = emptyList()
        debateResult = null

        coroutineScope.launch {
            val result = AgentArenaEngine.executeArenaDebate(
                problemStatement = problemText,
                agentA = selectedAgentA,
                agentB = selectedAgentB,
                onProgressUpdate = { newTurn ->
                    liveTurns = liveTurns + newTurn
                }
            )
            debateResult = result
            isDebating = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Title Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OmnisCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Multi-Agent Arena",
                                tint = OmnisCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MULTI-AGENTNÍ ARÉNA (DUEL 8D)",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Živá dialektika dvou agentů • Srovnávací 8D matice",
                                color = OmnisTextLight,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Agent Selection Configurator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. VÝBĚR PROTICHŮDNÝCH AGENTŮ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OmnisCyan,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Agent A Box
                        Box(modifier = Modifier.weight(1f)) {
                            AgentCardButton(
                                label = "AGENT A (TEZE)",
                                agent = selectedAgentA,
                                onClick = { showAgentASelector = true }
                            )
                        }

                        Text(
                            text = "VS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = Color.Red,
                            fontSize = 16.sp
                        )

                        // Agent B Box
                        Box(modifier = Modifier.weight(1f)) {
                            AgentCardButton(
                                label = "AGENT B (ANTITEZE)",
                                agent = selectedAgentB,
                                onClick = { showAgentBSelector = true }
                            )
                        }
                    }

                    // Problem Input Box
                    Text(
                        text = "2. ZADÁNÍ PROBLÉMU NEBO HYPOTÉZY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OmnisCyan,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = problemText,
                        onValueChange = { problemText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedContainerColor = OmnisBgDark,
                            unfocusedContainerColor = OmnisBgDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )

                    // Topic Presets Chips
                    Text(
                        text = "Přednastavená témata soubojů:",
                        color = OmnisTextLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AgentArenaLibrary.presetDebateTopics) { topic ->
                            FilterChip(
                                selected = (problemText == topic),
                                onClick = { problemText = topic },
                                label = {
                                    Text(
                                        text = topic.take(28) + "...",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = OmnisCyan,
                                    containerColor = OmnisBgDark,
                                    labelColor = OmnisTextLight
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = OmnisBorderDark,
                                    selectedBorderColor = OmnisCyan,
                                    enabled = true,
                                    selected = problemText == topic
                                )
                            )
                        }
                    }

                    // Action Button
                    Button(
                        onClick = { startArenaDebate() },
                        enabled = !isDebating && problemText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmnisCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isDebating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PROBÍHÁ ŽIVÁ 8D DEBATA...",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SPUSTIT 8D DUEL AGENTŮ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Dynamic 8D Influence Spiderweb Graph
        if (liveTurns.isNotEmpty() || debateResult != null) {
            item {
                Dynamic8DInfluenceSpiderGraph(
                    agentA = selectedAgentA,
                    agentB = selectedAgentB,
                    agentAScores = liveScoresA,
                    agentBScores = liveScoresB,
                    synthesizedScores = debateResult?.synthesized8DScores,
                    activeDomain = activeDomain,
                    isLive = isDebating
                )
            }
        }

        // Live Argument Stream Feed
        if (liveTurns.isNotEmpty()) {
            item {
                Text(
                    text = "💬 ŽIVÝ PROUD ARGUMENTŮ A NÁMITEK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = OmnisCyan,
                    fontSize = 13.sp
                )
            }

            items(liveTurns) { turn ->
                val isAgentA = turn.agentId == selectedAgentA.id
                val agentColor = Color(if (isAgentA) selectedAgentA.colorHex else selectedAgentB.colorHex)

                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, agentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(agentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAgentA) "A" else "B",
                                        fontWeight = FontWeight.Bold,
                                        color = agentColor,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = turn.agentName,
                                    fontWeight = FontWeight.Bold,
                                    color = agentColor,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = turn.roundName,
                                color = OmnisTextLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = turn.argumentText,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            turn.highlightedDomains.forEach { domain ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(agentColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "8D: $domain",
                                        color = agentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Comparative 8D Matrix Chart
        debateResult?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📊 SROVNÁVACÍ 8D MATICE DOPADŮ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = OmnisCyan,
                                fontSize = 13.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OmnisCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "DIVERGENCE: ${(result.divergenceIndex * 100).toInt()}%",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Chart Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LegendItem(label = result.agentA.name, color = Color(result.agentA.colorHex))
                            LegendItem(label = result.agentB.name, color = Color(result.agentB.colorHex))
                            LegendItem(label = "Syntéza", color = OmnisCyan)
                        }

                        Divider(color = OmnisBorderDark)

                        // 8D Domains Comparative Bars
                        AgentArenaEngine.all8DDomains.forEach { domain ->
                            val scoreA = result.agentA8DScores[domain] ?: 0f
                            val scoreB = result.agentB8DScores[domain] ?: 0f
                            val scoreSynth = result.synthesized8DScores[domain] ?: 0f

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = domain,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "A: $scoreA | B: $scoreB | Syntéza: $scoreSynth",
                                        color = OmnisTextLight,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Triple Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OmnisBgDark),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val colorA = Color(result.agentA.colorHex)
                                    val colorB = Color(result.agentB.colorHex)

                                    // Bar A
                                    Box(
                                        modifier = Modifier
                                            .weight((scoreA + 1f).coerceAtLeast(0.1f))
                                            .fillMaxHeight()
                                            .background(colorA)
                                    )
                                    // Bar B
                                    Box(
                                        modifier = Modifier
                                            .weight((scoreB + 1f).coerceAtLeast(0.1f))
                                            .fillMaxHeight()
                                            .background(colorB)
                                    )
                                    // Bar Synth
                                    Box(
                                        modifier = Modifier
                                            .weight((scoreSynth + 1f).coerceAtLeast(0.1f))
                                            .fillMaxHeight()
                                            .background(OmnisCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Epistemic Synthesis Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = OmnisEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "EPISTEMICKÁ KONSENZUÁLNÍ SYNTÉZA",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = OmnisEmerald,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = result.synthesisSummary,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Divider(color = OmnisBorderDark)

                        Text(
                            text = "AKČNÍ SMĚRNICE:",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = OmnisTextLight,
                            fontSize = 11.sp
                        )

                        result.actionableGuidelines.forEach { guideline ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "•", color = OmnisCyan, fontSize = 12.sp)
                                Text(
                                    text = guideline,
                                    color = OmnisTextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // ZK SNARK Footprint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OmnisBgDark)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ZK-SNARK OTISK:",
                                color = OmnisTextLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = result.zkCommitmentHash,
                                color = OmnisCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Send to Chat Button
                        OutlinedButton(
                            onClick = {
                                onSendToChat("Analýza Multi-Agentní Arény u téma: '${result.problemStatement}'\nSyntéza: ${result.synthesisSummary}\nZK-SNARK: ${result.zkCommitmentHash}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = OmnisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ODESLAT SYNTÉZU DO HLAVNÍHO CHATU O.M.N.I.S.",
                                color = OmnisCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // History Panel
        item {
            ArenaHistoryPanel(
                historyList = historyList,
                onLoadDebate = { debate ->
                    selectedAgentA = debate.agentA
                    selectedAgentB = debate.agentB
                    problemText = debate.problemStatement
                    liveTurns = debate.turns
                    debateResult = debate
                },
                onCompareDebates = { debate1, debate2 ->
                    comparePair = Pair(debate1, debate2)
                }
            )
        }
    }

    // Comparison Modal Dialog
    comparePair?.let { pair ->
        Arena8DComparisonModal(
            debateA = pair.first,
            debateB = pair.second,
            onDismiss = { comparePair = null }
        )
    }

    // Agent Selector Dialogs
    if (showAgentASelector) {
        AgentSelectionDialog(
            title = "ZVOLTE AGENTA A (TEZE)",
            selectedAgent = selectedAgentA,
            onAgentSelected = {
                selectedAgentA = it
                showAgentASelector = false
            },
            onDismiss = { showAgentASelector = false }
        )
    }

    if (showAgentBSelector) {
        AgentSelectionDialog(
            title = "ZVOLTE AGENTA B (ANTITEZE)",
            selectedAgent = selectedAgentB,
            onAgentSelected = {
                selectedAgentB = it
                showAgentBSelector = false
            },
            onDismiss = { showAgentBSelector = false }
        )
    }
}

@Composable
private fun AgentCardButton(
    label: String,
    agent: AgentArchetype,
    onClick: () -> Unit
) {
    val agentColor = Color(agent.colorHex)

    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisBgDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, agentColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = OmnisTextLight,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = agent.name,
                color = agentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = agent.roleTitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label.take(15),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AgentSelectionDialog(
    title: String,
    selectedAgent: AgentArchetype,
    onAgentSelected: (AgentArchetype) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisCardDark,
        title = {
            Text(
                text = title,
                color = OmnisCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AgentArenaLibrary.presetArchetypes.forEach { archetype ->
                    val color = Color(archetype.colorHex)
                    val isSelected = archetype.id == selectedAgent.id

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.2f) else OmnisBgDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) color else OmnisBorderDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAgentSelected(archetype) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = archetype.name,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = archetype.roleTitle,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = archetype.description,
                                color = OmnisTextLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ZRUŠIT", color = OmnisTextLight, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
