package com.example.ui.causal

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.causal.CausalEngine
import com.example.causal.CausalNode
import com.example.causal.CausalSimulationState
import com.example.causal.NodeInterventionResult
import com.example.data.OmnisRecord
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Samostatná interaktivní obrazovka pro Judea Pearl Do(X) kauzální simulátor.
 * Umožňuje grafovou chirurgii, řízení intervencí, analýzu kontrafaktuálů
 * a export do artefaktů / cílů / kognitivního chatu.
 */
@Composable
fun CausalSimulatorView(
    baselineRecord: OmnisRecord?,
    onSaveArtifact: (title: String, type: String, language: String, content: String) -> Unit,
    onCreateGoal: (title: String, description: String) -> Unit,
    onSendToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Stav aktivních intervencí: nodeId -> clamped value
    var activeInterventions by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var selectedNodeId by remember { mutableStateOf<String>("SYS") }
    var activePresetName by remember { mutableStateOf<String?>(null) }

    // Výpočet stavu simulace
    val simulationState = remember(baselineRecord, activeInterventions, activePresetName) {
        CausalEngine.simulate(
            baselineRecord = baselineRecord,
            interventions = activeInterventions,
            presetName = activePresetName
        )
    }

    val selectedNode = remember(selectedNodeId) {
        CausalEngine.nodes.find { it.id == selectedNodeId } ?: CausalEngine.nodes.first()
    }

    val isSelectedIntervened = activeInterventions.containsKey(selectedNodeId)
    val currentSelectedValue = activeInterventions[selectedNodeId]
        ?: simulationState.nodeResults.find { it.nodeId == selectedNodeId }?.interventionalValue
        ?: 0.6f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hlavička obrazovky
        CausalSimulatorHeader(
            resilienceScore = simulationState.systemicResilienceScore,
            cascadeRisk = simulationState.cascadeRiskIndex,
            activeInterventionsCount = activeInterventions.size
        )

        // Sekce 1: Presety kauzálních scénářů
        PresetScenariosRow(
            activePresetId = activePresetName,
            onSelectPreset = { preset ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activeInterventions = preset.interventions
                activePresetName = preset.title
                selectedNodeId = preset.interventions.keys.firstOrNull() ?: "SYS"
            },
            onReset = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeInterventions = emptyMap()
                activePresetName = null
            }
        )

        // Sekce 2: Interaktivní DAG & Grafová chirurgie Canvas
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
            border = BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KAUZÁLNÍ DAG (STRUCTURAL CAUSAL MODEL)",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Odříznuto hran: ${simulationState.severedEdges.size}",
                        color = if (simulationState.severedEdges.isNotEmpty()) Color.Red else OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Klikněte na uzel pro nastavení přímé intervence do(X = v). Červené čárkované hrany značí chirurgicky odříznuté vlivy.",
                    color = OmnisTextMuted,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                CausalGraphCanvas(
                    nodes = CausalEngine.nodes,
                    edges = CausalEngine.edges,
                    simulationState = simulationState,
                    selectedNodeId = selectedNodeId,
                    onNodeClick = { node ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedNodeId = node.id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }
        }

        // Sekce 3: Ovládací konzole pro vybraný uzel (Do(X) Slider)
        InterventionControlConsole(
            selectedNode = selectedNode,
            isIntervened = isSelectedIntervened,
            currentValue = currentSelectedValue,
            onToggleIntervention = { enable ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val newMap = activeInterventions.toMutableMap()
                if (enable) {
                    newMap[selectedNodeId] = currentSelectedValue
                } else {
                    newMap.remove(selectedNodeId)
                }
                activeInterventions = newMap
                activePresetName = null
            },
            onValueChange = { newVal ->
                val newMap = activeInterventions.toMutableMap()
                newMap[selectedNodeId] = newVal
                activeInterventions = newMap
                activePresetName = null
            }
        )

        // Sekce 4: Porovnání P(Y | X) vs P(Y | do(X)) — Kontrafaktuální dopad
        CounterfactualAnalyticsCard(
            results = simulationState.nodeResults,
            onSelectNode = { nodeId ->
                selectedNodeId = nodeId
            }
        )

        // Sekce 5: Akční dispečink (Uložit, Vytvořit Cíl, Odeslat do Chatu)
        ActionDispatchPanel(
            simulationState = simulationState,
            onSaveArtifact = {
                val report = generateCausalReportMarkdown(simulationState)
                onSaveArtifact("Kauzální Intervence do(${activeInterventions.keys.joinToString()})", "CAUSAL_ANALYSIS", "markdown", report)
                Toast.makeText(context, "Kauzální zpráva uložena do artefaktů", Toast.LENGTH_SHORT).show()
            },
            onCreateGoal = {
                val title = if (activeInterventions.isNotEmpty()) {
                    "Kauzální mitigace: Stabilizace po do(${activeInterventions.keys.joinToString()})"
                } else {
                    "Optimalizace 8D systémového ekvilibria"
                }
                val desc = "Autonomní cíl odvozený z Do(X) simulace. Cílová odolnost: ${(simulationState.systemicResilienceScore * 100).toInt()} %."
                onCreateGoal(title, desc)
                Toast.makeText(context, "Cíl byl zařazen mezi autonomní priority", Toast.LENGTH_SHORT).show()
            },
            onSendToChat = {
                val prompt = generateCounterfactualChatPrompt(simulationState)
                onSendToChat(prompt)
            }
        )
    }
}

@Composable
private fun CausalSimulatorHeader(
    resilienceScore: Float,
    cascadeRisk: Float,
    activeInterventionsCount: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = OmnisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DO(X) SIMULÁTOR",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Judea Pearl Do-Calculus & Causal Graph Surgery",
                    color = OmnisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Skóre odolnosti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisCyan.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, OmnisCyan)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("ODOLNOST", color = OmnisCyan, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${(resilienceScore * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Riziko kaskády
                val riskColor = if (cascadeRisk > 0.4f) Color.Red else if (cascadeRisk > 0.2f) OmnisAmber else OmnisEmerald
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, riskColor)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("KASKÁDA", color = riskColor, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${(cascadeRisk * 100).toInt()}%",
                            color = riskColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetScenariosRow(
    activePresetId: String?,
    onSelectPreset: (com.example.causal.CausalPreset) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "PŘEDNASTAVENÉ SCÉNÁŘE & ZÁSAHY",
            color = OmnisTextMuted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tlačítko Reset
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OmnisBorderDark),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = OmnisPanelDark),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            CausalEngine.presets.forEach { preset ->
                val isSelected = activePresetId == preset.title
                Button(
                    onClick = { onSelectPreset(preset) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) OmnisCyan.copy(alpha = 0.25f) else OmnisPanelDark,
                        contentColor = if (isSelected) OmnisCyan else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) OmnisCyan else OmnisBorderDark
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = preset.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CausalGraphCanvas(
    nodes: List<CausalNode>,
    edges: List<com.example.causal.CausalEdge>,
    simulationState: CausalSimulationState,
    selectedNodeId: String,
    onNodeClick: (CausalNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(OmnisCardDark, RoundedCornerShape(8.dp))
        ) {
            canvasSize = size
            val width = size.width
            val height = size.height
            val padX = 36.dp.toPx()
            val padY = 32.dp.toPx()

            // Vypočtené absolutní pozice uzlů
            val nodePosMap = nodes.associate { node ->
                val px = padX + node.defaultNormalizedX * (width - 2 * padX)
                val py = padY + node.defaultNormalizedY * (height - 2 * padY)
                node.id to Offset(px, py)
            }

            // 1. Kreslení orientovaných hran (Causal Edges)
            edges.forEach { edge ->
                val start = nodePosMap[edge.sourceId] ?: return@forEach
                val end = nodePosMap[edge.targetId] ?: return@forEach
                val isSevered = simulationState.severedEdges.contains(edge.sourceId to edge.targetId)

                val angle = atan2(end.y - start.y, end.x - start.x)
                val nodeRadius = 18.dp.toPx()
                val adjustedStart = Offset(
                    start.x + cos(angle) * nodeRadius,
                    start.y + sin(angle) * nodeRadius
                )
                val adjustedEnd = Offset(
                    end.x - cos(angle) * (nodeRadius + 4.dp.toPx()),
                    end.y - sin(angle) * (nodeRadius + 4.dp.toPx())
                )

                if (isSevered) {
                    // Odříznutá hrana (severed edge under do-calculus)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color.Red.copy(alpha = 0.7f),
                        start = adjustedStart,
                        end = adjustedEnd,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashedEffect,
                        cap = StrokeCap.Round
                    )
                    // Značka křížku/odříznutí uprostřed
                    val mid = Offset((adjustedStart.x + adjustedEnd.x) / 2f, (adjustedStart.y + adjustedEnd.y) / 2f)
                    drawLine(
                        color = Color.Red,
                        start = Offset(mid.x - 5f, mid.y - 5f),
                        end = Offset(mid.x + 5f, mid.y + 5f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(mid.x + 5f, mid.y - 5f),
                        end = Offset(mid.x - 5f, mid.y + 5f),
                        strokeWidth = 2.5f
                    )
                } else {
                    // Aktivní kauzální tok
                    val strokeColor = if (edge.weight > 0) OmnisCyan.copy(alpha = 0.45f) else OmnisAmber.copy(alpha = 0.45f)
                    drawLine(
                        color = strokeColor,
                        start = adjustedStart,
                        end = adjustedEnd,
                        strokeWidth = (1.5f + (kotlin.math.abs(edge.weight) * 2f)).dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Kreslení šipky
                    val arrowLength = 9.dp.toPx()
                    val arrowAngle = Math.PI / 6
                    val arrowP1 = Offset(
                        adjustedEnd.x - arrowLength * cos(angle - arrowAngle).toFloat(),
                        adjustedEnd.y - arrowLength * sin(angle - arrowAngle).toFloat()
                    )
                    val arrowP2 = Offset(
                        adjustedEnd.x - arrowLength * cos(angle + arrowAngle).toFloat(),
                        adjustedEnd.y - arrowLength * sin(angle + arrowAngle).toFloat()
                    )
                    val path = Path().apply {
                        moveTo(adjustedEnd.x, adjustedEnd.y)
                        lineTo(arrowP1.x, arrowP1.y)
                        lineTo(arrowP2.x, arrowP2.y)
                        close()
                    }
                    drawPath(path = path, color = strokeColor)
                }
            }

            // 2. Kreslení uzlů na Canvasu
            nodes.forEach { node ->
                val center = nodePosMap[node.id] ?: return@forEach
                val isSelected = node.id == selectedNodeId
                val isIntervened = simulationState.activeInterventions.containsKey(node.id)
                val radius = if (isSelected) 20.dp.toPx() else 17.dp.toPx()

                // Záře pro intervenovaný uzel
                if (isIntervened) {
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.25f),
                        radius = radius + 8.dp.toPx(),
                        center = center
                    )
                } else if (isSelected) {
                    drawCircle(
                        color = node.color.copy(alpha = 0.25f),
                        radius = radius + 6.dp.toPx(),
                        center = center
                    )
                }

                // Tělo uzlu
                drawCircle(
                    color = OmnisPanelDark,
                    radius = radius,
                    center = center
                )

                // Obvod uzlu
                drawCircle(
                    color = if (isIntervened) Color.Red else if (isSelected) Color.White else node.color,
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (isSelected || isIntervened) 2.5.dp.toPx() else 1.5.dp.toPx())
                )
            }
        }

        // Overlay průhledných klikatelných elementů přes uzly pro spolehlivou interakci
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            val height = maxHeight
            val padX = 36.dp
            val padY = 32.dp

            nodes.forEach { node ->
                val isIntervened = simulationState.activeInterventions.containsKey(node.id)
                val isSelected = node.id == selectedNodeId
                val posX = padX + (width - padX * 2) * node.defaultNormalizedX
                val posY = padY + (height - padY * 2) * node.defaultNormalizedY

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = posX - 24.dp, y = posY - 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onNodeClick(node) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = node.id,
                            color = if (isIntervened) Color.Red else if (isSelected) Color.White else node.color,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        val res = simulationState.nodeResults.find { it.nodeId == node.id }
                        val pct = ((res?.interventionalValue ?: 0.5f) * 100).toInt()
                        Text(
                            text = "$pct%",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterventionControlConsole(
    selectedNode: CausalNode,
    isIntervened: Boolean,
    currentValue: Float,
    onToggleIntervention: (Boolean) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, if (isIntervened) Color.Red.copy(alpha = 0.8f) else OmnisCyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(selectedNode.color)
                    )
                    Text(
                        text = "INTERVENCE: do(${selectedNode.id} = x)",
                        color = if (isIntervened) Color.Red else OmnisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isIntervened) "AKTIVNÍ DO()" else "POUZE POZOROVÁNO",
                        color = if (isIntervened) Color.Red else OmnisTextMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = isIntervened,
                        onCheckedChange = onToggleIntervention,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.Red,
                            uncheckedThumbColor = OmnisTextMuted,
                            uncheckedTrackColor = OmnisBorderDark
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            Text(
                text = "${selectedNode.name} — ${selectedNode.description}",
                color = OmnisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fixovaná hodnota zásahu:",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${(currentValue * 100).toInt()} %",
                    color = if (isIntervened) Color.Red else selectedNode.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = currentValue,
                onValueChange = onValueChange,
                enabled = isIntervened,
                valueRange = 0.05f..0.99f,
                colors = SliderDefaults.colors(
                    thumbColor = if (isIntervened) Color.Red else OmnisTextMuted,
                    activeTrackColor = if (isIntervened) Color.Red else OmnisTextMuted,
                    inactiveTrackColor = OmnisBorderDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CounterfactualAnalyticsCard(
    results: List<NodeInterventionResult>,
    onSelectNode: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KONTRAFAKTUÁLNÍ DELTA DOPADŮ",
                    color = OmnisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "P(Y|X) → P(Y|do(X))",
                    color = OmnisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            results.forEach { res ->
                val deltaPct = (res.delta * 100).toInt()
                val sign = if (deltaPct > 0) "+$deltaPct%" else "$deltaPct%"
                val deltaColor = when {
                    res.isDirectlyIntervened -> Color.Red
                    deltaPct > 5 -> OmnisEmerald
                    deltaPct < -5 -> Color(0xFFEF4444)
                    else -> OmnisTextMuted
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisCardDark.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, if (res.isDirectlyIntervened) Color.Red.copy(alpha = 0.4f) else OmnisBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNode(res.nodeId) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(res.color)
                            )
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = res.nodeId,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "(${res.nodeName})",
                                        color = OmnisTextMuted,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = res.impactClassification,
                                    color = deltaColor,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${(res.observedValue * 100).toInt()}% → ${(res.interventionalValue * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = deltaColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, deltaColor)
                            ) {
                                Text(
                                    text = sign,
                                    color = deltaColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionDispatchPanel(
    simulationState: CausalSimulationState,
    onSaveArtifact: () -> Unit,
    onCreateGoal: () -> Unit,
    onSendToChat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AKČNÍ EXPORT & KOGNITIVNÍ DISPEČINK",
                color = OmnisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Uložit do Artefaktů
                Button(
                    onClick = onSaveArtifact,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnisCyan.copy(alpha = 0.2f),
                        contentColor = OmnisCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OmnisCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Artefakt", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                // Vytvořit Cíl
                Button(
                    onClick = onCreateGoal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnisAmber.copy(alpha = 0.2f),
                        contentColor = OmnisAmber
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OmnisAmber),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tvořit Cíl", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                // Odeslat do Chatu
                Button(
                    onClick = onSendToChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnisViolet.copy(alpha = 0.25f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OmnisViolet),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat Rozbor", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateCausalReportMarkdown(state: CausalSimulationState): String {
    val interventionsStr = state.activeInterventions.entries.joinToString(", ") { "do(${it.key} = ${(it.value * 100).toInt()}%)" }
    return buildString {
        appendLine("# Zpráva z Kauzální Simulace O.M.N.I.S.")
        appendLine("**Aktivní intervence (Judea Pearl Do-Calculus):** ${if (interventionsStr.isBlank()) "Žádné (baseline)" else interventionsStr}")
        appendLine("**Počet odříznutých kauzálních hran:** ${state.severedEdges.size}")
        appendLine("**Index systémové odolnosti:** ${(state.systemicResilienceScore * 100).toInt()}%")
        appendLine("**Index kaskádového rizika:** ${(state.cascadeRiskIndex * 100).toInt()}%")
        appendLine()
        appendLine("## Výsledky po intervenci (Kontrafaktuální Delta)")
        appendLine("| Uzel | Název | P(Y|X) Pozorováno | P(Y|do(X)) Zásah | Delta | Klasifikace |")
        appendLine("| :--- | :--- | :--- | :--- | :--- | :--- |")
        state.nodeResults.forEach { r ->
            val deltaPct = (r.delta * 100).toInt()
            val sign = if (deltaPct > 0) "+$deltaPct%" else "$deltaPct%"
            appendLine("| ${r.nodeId} | ${r.nodeName} | ${(r.observedValue * 100).toInt()}% | ${(r.interventionalValue * 100).toInt()}% | $sign | ${r.impactClassification} |")
        }
        appendLine()
        appendLine("Vygenerováno autonomním Do(X) SCM kognitivním simulátorem platformy O.M.N.I.S.")
    }
}

private fun generateCounterfactualChatPrompt(state: CausalSimulationState): String {
    val interventions = state.activeInterventions.entries.joinToString(", ") { "do(${it.key} = ${(it.value * 100).toInt()}%)" }
    val deltas = state.nodeResults.filter { kotlin.math.abs(it.delta) > 0.04f }
        .joinToString(", ") { "${it.nodeId}: ${(it.delta * 100).toInt()}%" }
    return "Proveď rigorózní kognitivní rozbor kauzální intervence Judea Pearl: [$interventions]. Výsledné delty v 8D matici: [$deltas]. Odhadni dopady 2. a 3. řádu na celkovou stabilitu systému a navrhni mitigace."
}
