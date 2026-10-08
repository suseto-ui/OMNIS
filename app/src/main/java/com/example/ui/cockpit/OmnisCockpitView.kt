package com.example.ui.cockpit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisArtifact
import com.example.data.OmnisGoal
import com.example.data.OmnisRecord
import com.example.ui.OmnisTab
import com.example.ui.artifacts.getArtifactIcon
import com.example.ui.artifacts.getArtifactTypeColor
import com.example.ui.goals.CreateGoalDialog
import com.example.ui.theme.*

@Composable
fun OmnisCockpitView(
    latestRecord: OmnisRecord?,
    activeGoals: List<OmnisGoal>,
    recentArtifacts: List<OmnisArtifact>,
    onTabSwitch: (OmnisTab) -> Unit,
    onSendToChat: ((String) -> Unit)? = null,
    onCreateGoal: ((String, String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val healthScore = latestRecord?.compositeScore ?: 0.78f

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var quickPromptText by remember { mutableStateOf("") }

    if (showCreateGoalDialog && onCreateGoal != null) {
        CreateGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onConfirm = { title, desc ->
                onCreateGoal(title, desc)
                showCreateGoalDialog = false
            }
        )
    }

    val lowestDim = remember(latestRecord) {
        if (latestRecord == null) null
        else {
            val list = listOf(
                Triple("SYS", latestRecord.valSys, "Systémová architektura a stabilita"),
                Triple("ECON", latestRecord.valEcon, "Ekonomická a nákladová efektivita"),
                Triple("PSYCH", latestRecord.valPsych, "Kognitivní a psychologická ergonomie"),
                Triple("ECO", latestRecord.valEco, "Udržitelnost a environmentální stopa"),
                Triple("LAW", latestRecord.valLaw, "Právní a regulatorní soulad"),
                Triple("SEC", latestRecord.valSec, "Kybernetická a aplikační bezpečnost"),
                Triple("PHYS", latestRecord.valPhys, "Fyzikální a HW optimalizace"),
                Triple("SOC", latestRecord.valSoc, "Sociální a etická harmonizace")
            )
            list.minByOrNull { it.second }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .verticalScroll(scrollState)
            .padding(14.dp)
            .navigationBarsPadding()
            .testTag("cockpit_root")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "O.M.N.I.S. COCKPIT",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "AUTONOMOUS MISSION CONTROL",
                        color = OmnisCyan,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(OmnisEmerald))
                }
            }
            
            // System Health Indicator
            SystemHealthGauge(healthScore)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Action Command Strip
        QuickActionStrip(onTabSwitch = onTabSwitch)

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Cognitive Command Bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(13.dp))
                        Text(
                            "RYCHLÝ KOGNITIVNÍ VSTUP",
                            color = OmnisCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (onCreateGoal != null) {
                        Text(
                            "+ NOVÁ MISE",
                            color = OmnisAmber,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCreateGoalDialog = true
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = quickPromptText,
                        onValueChange = { quickPromptText = it },
                        placeholder = { Text("Zadejte dotaz, cíl nebo kognitivní prompt...", color = OmnisTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            focusedContainerColor = OmnisBgDark,
                            unfocusedContainerColor = OmnisBgDark
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (quickPromptText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val text = quickPromptText
                                quickPromptText = ""
                                onSendToChat?.invoke(text)
                            }
                        },
                        enabled = quickPromptText.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (quickPromptText.isNotBlank()) OmnisCyan else OmnisBorderDark)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                // Quick suggestion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val templates = listOf(
                        "Audit integrity systému",
                        "Optimalizovat 8D matici",
                        "Syntéza priorit cílů",
                        "Bezpečnostní prověrka"
                    )
                    templates.forEach { tpl ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OmnisBgDark,
                            border = BorderStroke(0.5.dp, OmnisBorderDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    quickPromptText = tpl
                                }
                        ) {
                            Text(
                                tpl,
                                color = OmnisTextMuted,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Cognitive Insight Recommendation Banner
        if (lowestDim != null && lowestDim.second < 0.85f) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisViolet.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(OmnisViolet.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = OmnisViolet, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "KOGNITIVNÍ RADAR O.M.N.I.S.",
                            color = OmnisViolet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Dimenze ${lowestDim.first} (${(lowestDim.second * 100).toInt()}%): ${lowestDim.third}.",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val prompt = "Proveď transdisciplinární analýzu a navrhni optimalizační plán pro dimenzi ${lowestDim.first} (${lowestDim.third}), která aktuálně dosahuje ${(lowestDim.second * 100).toInt()}%."
                            onSendToChat?.invoke(prompt)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, OmnisViolet),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Vyřešit", color = OmnisViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 8D Matrix State Bar
        SectionHeader("8D TRANSDISCIPLINÁRNÍ MATICE", Icons.Default.Grid4x4) { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTabSwitch(OmnisTab.MATRIX) 
        }
        Spacer(modifier = Modifier.height(8.dp))
        Matrix8DOverviewCard(latestRecord = latestRecord, onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTabSwitch(OmnisTab.MATRIX)
        })

        Spacer(modifier = Modifier.height(18.dp))

        // Grid of Quick Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "AKTIVNÍ CÍLE",
                value = "${activeGoals.size} V BĚHU",
                icon = Icons.Default.Flag,
                color = OmnisAmber,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSwitch(OmnisTab.GOALS) 
                }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "ARTEFAKTY",
                value = "${recentArtifacts.size} ULOŽENO",
                icon = Icons.Default.Inventory2,
                color = OmnisCyan,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSwitch(OmnisTab.ARTIFACTS) 
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active Goals Section
        SectionHeader("AKTIVNÍ MISE & CÍLE", Icons.Default.Flag) { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTabSwitch(OmnisTab.GOALS) 
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (activeGoals.isEmpty()) {
            EmptyStateBox("Žádné aktivní cíle. Zadejte novou vizi v sekci Cíle.")
        } else {
            activeGoals.take(3).forEach { goal ->
                GoalSummaryItem(goal = goal, onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSwitch(OmnisTab.GOALS)
                })
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recent Artifacts
        SectionHeader("KNOWLEDGE ARTIFACTS", Icons.Default.Code) { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTabSwitch(OmnisTab.ARTIFACTS) 
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recentArtifacts.take(6), key = { it.id }) { artifact ->
                ArtifactMiniCard(artifact = artifact, onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSwitch(OmnisTab.ARTIFACTS)
                })
            }
            if (recentArtifacts.isEmpty()) {
                item { EmptyStateBox("Žádné vygenerované artefakty", modifier = Modifier.width(240.dp)) }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Telemetry Preview
        SectionHeader("SYSTEM TELEMETRY", Icons.Default.Dns) { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTabSwitch(OmnisTab.TELEMETRY) 
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSwitch(OmnisTab.TELEMETRY)
                },
            color = OmnisCardDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STATUS", color = OmnisTextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("NOMINAL", color = OmnisEmerald, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(OmnisBorderDark))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LATENCE", color = OmnisTextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("28 ms", color = OmnisCyan, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(OmnisBorderDark))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PAMĚŤ KROKŮ", color = OmnisTextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("BM25 HYBRID", color = OmnisViolet, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(90.dp)) // Nav bar padding
    }
}

@Composable
fun QuickActionStrip(onTabSwitch: (OmnisTab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val actions = listOf(
        Triple("Chat", Icons.AutoMirrored.Filled.Chat, OmnisTab.CHAT),
        Triple("Matice", Icons.Default.Grid4x4, OmnisTab.MATRIX),
        Triple("Scénáře", Icons.Default.Timeline, OmnisTab.SCENARIOS),
        Triple("Cíle", Icons.Default.Flag, OmnisTab.GOALS),
        Triple("Nexus", Icons.Default.Hub, OmnisTab.NEXUS),
        Triple("Artefakty", Icons.Default.Code, OmnisTab.ARTIFACTS),
        Triple("Telemetrie", Icons.Default.Dns, OmnisTab.TELEMETRY)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, icon, tab) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                border = BorderStroke(0.8.dp, OmnisBorderDark),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSwitch(tab)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(14.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun Matrix8DOverviewCard(latestRecord: OmnisRecord?, onClick: () -> Unit) {
    val dimensions = listOf(
        Triple("SYS", latestRecord?.valSys ?: 0.85f, OmnisCyan),
        Triple("ECON", latestRecord?.valEcon ?: 0.72f, OmnisAmber),
        Triple("PSYCH", latestRecord?.valPsych ?: 0.68f, OmnisViolet),
        Triple("ECO", latestRecord?.valEco ?: 0.80f, OmnisEmerald),
        Triple("LAW", latestRecord?.valLaw ?: 0.75f, Color(0xFF60A5FA)),
        Triple("SEC", latestRecord?.valSec ?: 0.90f, Color(0xFFEF4444)),
        Triple("PHYS", latestRecord?.valPhys ?: 0.70f, Color(0xFFF97316)),
        Triple("SOC", latestRecord?.valSoc ?: 0.77f, Color(0xFFEC4899))
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("STAV KOGNITIVNÍCH DIMENZÍ", color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("DETAIL MATICE →", color = OmnisCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dimensions.forEach { (dim, score, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(score * 100).toInt()}%",
                            color = color,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(OmnisPanelDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(score.coerceIn(0.1f, 1.0f))
                                    .align(Alignment.BottomCenter)
                                    .background(color)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dim,
                            color = OmnisTextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemHealthGauge(score: Float) {
    val color = when {
        score > 0.8f -> OmnisEmerald
        score > 0.5f -> OmnisCyan
        else -> Color.Red
    }
    
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { score },
            modifier = Modifier.size(52.dp),
            color = color,
            strokeWidth = 3.5.dp,
            trackColor = OmnisBorderDark
        )
        Text(
            "${(score * 100).toInt()}%",
            color = color,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = OmnisCardDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = OmnisTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, onMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(15.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            "DETAIL",
            color = OmnisCyan,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onMore() }
        )
    }
}

@Composable
fun GoalSummaryItem(goal: OmnisGoal, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = OmnisPanelDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, OmnisBorderDark)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(OmnisCyan.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${(goal.progress * 100).toInt()}%", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(goal.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
                Text(goal.status, color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ArtifactMiniCard(artifact: OmnisArtifact, onClick: () -> Unit) {
    val typeColor = getArtifactTypeColor(artifact.type)
    Surface(
        modifier = Modifier
            .width(170.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = OmnisPanelDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(getArtifactIcon(artifact.type), contentDescription = null, tint = typeColor, modifier = Modifier.size(14.dp))
                Text(artifact.type, color = typeColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Text(artifact.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = FontFamily.Monospace)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(OmnisCyan))
                Text(artifact.language.uppercase(), color = OmnisCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun EmptyStateBox(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier = modifier
            .height(54.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .border(1.dp, OmnisBorderDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = OmnisTextMuted, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
    }
}
