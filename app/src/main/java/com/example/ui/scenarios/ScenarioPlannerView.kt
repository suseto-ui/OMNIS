package com.example.ui.scenarios

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.scenario.ForecastPoint
import com.example.scenario.ForecastingEngine
import com.example.scenario.ScenarioEvent
import com.example.scenario.ScenarioLibrary
import com.example.ui.guide.OmnisHelpIconButton
import com.example.ui.theme.*

// Mapování barev pro všech 8 dimenzí
val dimColors = mapOf(
    "SYS" to OmnisCyan,
    "ECON" to OmnisAmber,
    "PSYCH" to OmnisViolet,
    "ECO" to OmnisEmerald,
    "LAW" to Color(0xFF60A5FA),
    "SEC" to Color(0xFFEF4444),
    "PHYS" to Color(0xFFF97316),
    "SOC" to Color(0xFFEC4899)
)

val dimLabels = mapOf(
    "SYS" to "Systémová architektura",
    "ECON" to "Ekonomická efektivita",
    "PSYCH" to "Psychologická integrita",
    "ECO" to "Ekologická udržitelnost",
    "LAW" to "Právní & regulatorní soulad",
    "SEC" to "Bezpečnostní odolnost",
    "PHYS" to "Fyzická proveditelnost",
    "SOC" to "Společenská akceptace"
)

@Composable
fun ScenarioPlannerView(
    currentRecord: OmnisRecord?,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onCreateGoal: ((title: String, description: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var selectedEvents by remember { mutableStateOf(setOf<ScenarioEvent>()) }
    var customEvents by remember { mutableStateOf(listOf<ScenarioEvent>()) }
    var simulationSteps by remember { mutableIntStateOf(20) }
    var activeDimensionFilter by remember { mutableStateOf("ALL") }
    var showCustomEventDialog by remember { mutableStateOf(false) }

    val allAvailableEvents = remember(customEvents) {
        ScenarioLibrary.presetEvents + customEvents
    }

    val forecast = remember(currentRecord, selectedEvents, simulationSteps) {
        currentRecord?.let { 
            ForecastingEngine.project(it, selectedEvents.toList(), steps = simulationSteps)
        } ?: emptyList()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .testTag("scenario_planner_root")
    ) {
        val isWideScreen = maxWidth >= 640.dp

        if (isWideScreen) {
            // Tablet / Desktop Split
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    HeaderSection(
                        onAddCustomEvent = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCustomEventDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SimulationControls(
                        steps = simulationSteps,
                        onStepsChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            simulationSteps = it 
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "AKTIVNÍ UDÁLOSTI (${selectedEvents.size}/${allAvailableEvents.size})",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(allAvailableEvents) { event ->
                            EventCard(
                                event = event,
                                isSelected = selectedEvents.contains(event),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedEvents = if (selectedEvents.contains(event)) selectedEvents - event else selectedEvents + event
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(start = 12.dp)
                ) {
                    ProjectionSection(
                        forecast = forecast,
                        selectedEvents = selectedEvents,
                        activeFilter = activeDimensionFilter,
                        onFilterChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeDimensionFilter = it 
                        },
                        onSaveArtifact = onSaveArtifact,
                        onCreateGoal = onCreateGoal,
                        onSendToChat = onSendToChat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile Vertical Scroll Layout
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                HeaderSection(
                    onAddCustomEvent = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCustomEventDialog = true
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                SimulationControls(
                    steps = simulationSteps,
                    onStepsChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        simulationSteps = it 
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SCÉNÁŘOVÉ FAKTORY (${selectedEvents.size}/${allAvailableEvents.size})",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (selectedEvents.isNotEmpty()) {
                        Text(
                            "Zrušit výběr",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedEvents = emptySet() 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Event cards
                allAvailableEvents.forEach { event ->
                    EventCard(
                        event = event,
                        isSelected = selectedEvents.contains(event),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedEvents = if (selectedEvents.contains(event)) selectedEvents - event else selectedEvents + event
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ProjectionSection(
                    forecast = forecast,
                    selectedEvents = selectedEvents,
                    activeFilter = activeDimensionFilter,
                    onFilterChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeDimensionFilter = it 
                    },
                    onSaveArtifact = onSaveArtifact,
                    onCreateGoal = onCreateGoal,
                    onSendToChat = onSendToChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showCustomEventDialog) {
        CustomScenarioDialog(
            onDismiss = { showCustomEventDialog = false },
            onConfirm = { customEvent ->
                customEvents = customEvents + customEvent
                selectedEvents = selectedEvents + customEvent
                showCustomEventDialog = false
            }
        )
    }
}

@Composable
private fun HeaderSection(
    onAddCustomEvent: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OmnisCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(20.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "SCENARIO ARCHITECT",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    OmnisHelpIconButton(
                        title = "Scenario Architect",
                        description = "Prediktivní dynamické modelování 8D matice a sémantického driftu stability v čase.",
                        bulletPoints = listOf(
                            "Události: Aktivujte makro-scénáře jako Singularity, Kybernetický útok či ESG tranzice.",
                            "8D Projekce: Přepínejte dimenze nebo sledujte komplexní celosystémovou odezvu.",
                            "Časový Horizont: Simulace 10, 20 nebo 50 výpočetních kroků dopředu.",
                            "Vlastní Událost: Definujte vlastní transdisciplinární vektor dopadů."
                        ),
                        tint = OmnisCyan
                    )
                }
                Text(
                    "Modelování prediktivních scénářů a driftů stability",
                    color = OmnisTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        IconButton(
            onClick = onAddCustomEvent,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = "Vlastní scénář", tint = OmnisCyan, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SimulationControls(
    steps: Int,
    onStepsChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "HORIZONT KROKŮ:",
                color = OmnisTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 20, 50).forEach { stepOption ->
                    val isSelected = steps == stepOption
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) OmnisCyan.copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(0.5.dp, if (isSelected) OmnisCyan else OmnisBorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onStepsChange(stepOption) }
                    ) {
                        Text(
                            text = "${stepOption}x",
                            color = if (isSelected) OmnisCyan else OmnisTextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: ScenarioEvent,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) event.color.copy(alpha = 0.15f) else OmnisCardDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) event.color else OmnisBorderDark
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) event.color else OmnisTextMuted.copy(alpha = 0.5f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    color = if (isSelected) event.color else Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.description,
                    color = OmnisTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Aktivní",
                    tint = event.color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectionSection(
    forecast: List<ForecastPoint>,
    selectedEvents: Set<ScenarioEvent>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onCreateGoal: ((title: String, description: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isSynthesisExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PROJEKCE 8D MATICE V ČASE",
                color = OmnisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (forecast.isNotEmpty() && selectedEvents.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Konzultovat v chatu
                    if (onSendToChat != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val first = forecast.first()
                                val last = forecast.last()
                                val eventNames = selectedEvents.joinToString(", ") { it.name }
                                val prompt = buildString {
                                    appendLine("Konzultace prediktivní simulace scénáře [$eventNames] s horizontem ${forecast.size} kroků:")
                                    appendLine("- SYS: ${(first.valSys * 100).toInt()}% -> ${(last.valSys * 100).toInt()}%")
                                    appendLine("- SEC: ${(first.valSec * 100).toInt()}% -> ${(last.valSec * 100).toInt()}%")
                                    appendLine("- ECON: ${(first.valEcon * 100).toInt()}% -> ${(last.valEcon * 100).toInt()}%")
                                    appendLine("- LAW: ${(first.valLaw * 100).toInt()}% -> ${(last.valLaw * 100).toInt()}%")
                                    appendLine("- PSYCH: ${(first.valPsych * 100).toInt()}% -> ${(last.valPsych * 100).toInt()}%")
                                    appendLine("- ECO: ${(first.valEco * 100).toInt()}% -> ${(last.valEco * 100).toInt()}%")
                                    appendLine("Jaká transdisciplinární opatření doporučuješ provést pro zamezení destabilizace systému?")
                                }
                                onSendToChat(prompt)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Konzultovat v chatu", tint = OmnisViolet, modifier = Modifier.size(15.dp))
                        }
                    }

                    // Uložit jako Znalostní Artefakt
                    if (onSaveArtifact != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val first = forecast.first()
                                val last = forecast.last()
                                val eventNames = selectedEvents.joinToString(", ") { it.name }
                                val artifactContent = buildString {
                                    appendLine("# Prediktivní 8D Projekce O.M.N.I.S.")
                                    appendLine("**Simulované scénáře:** $eventNames")
                                    appendLine("**Horizont simulace:** ${forecast.size} kroků")
                                    appendLine()
                                    appendLine("## Vývoj dimenzí")
                                    appendLine("| Dimenze | Výchozí stav | Predikovaný stav | Delta |")
                                    appendLine("|---|---|---|---|")
                                    fun row(name: String, v1: Float, v2: Float) {
                                        val d = ((v2 - v1) * 100).toInt()
                                        val sign = if (d > 0) "+$d%" else "$d%"
                                        appendLine("| $name | ${(v1 * 100).toInt()}% | ${(v2 * 100).toInt()}% | $sign |")
                                    }
                                    row("SYS (Architektura)", first.valSys, last.valSys)
                                    row("ECON (Ekonomika)", first.valEcon, last.valEcon)
                                    row("PSYCH (Psychologie)", first.valPsych, last.valPsych)
                                    row("ECO (Ekologie)", first.valEco, last.valEco)
                                    row("LAW (Právo)", first.valLaw, last.valLaw)
                                    row("SEC (Bezpečnost)", first.valSec, last.valSec)
                                    row("PHYS (Fyzika)", first.valPhys, last.valPhys)
                                    row("SOC (Společnost)", first.valSoc, last.valSoc)
                                    appendLine()
                                    appendLine("## Doporučené mitigace")
                                    val critical = mutableListOf<String>()
                                    if (last.valSys < 0.5f) critical.add("SYS: Posílit systémovou redundanci a rozpojit kritické závislosti.")
                                    if (last.valSec < 0.5f) critical.add("SEC: Okamžitá aktivace obranných protokolů a jističů.")
                                    if (last.valEcon < 0.5f) critical.add("ECON: Alokace rezervních zdrojů a diverzifikace.")
                                    if (last.valLaw < 0.5f) critical.add("LAW: Provedení compliance auditu a právní validace.")
                                    if (critical.isEmpty()) {
                                        appendLine("Všechny dimenze zůstávají v mezích tolerance.")
                                    } else {
                                        critical.forEach { appendLine("- $it") }
                                    }
                                }
                                onSaveArtifact(
                                    "Predikce: $eventNames",
                                    "REPORT",
                                    "markdown",
                                    artifactContent
                                )
                                Toast.makeText(context, "Projekce uložena do artefaktů", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Uložit jako artefakt", tint = OmnisEmerald, modifier = Modifier.size(15.dp))
                        }
                    }

                    // Vytvořit Autonomní Cíl
                    if (onCreateGoal != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val eventNames = selectedEvents.joinToString(", ") { it.name }
                                val first = forecast.first()
                                val last = forecast.last()
                                val degradedDims = mutableListOf<String>()
                                if (last.valSys < first.valSys) degradedDims.add("SYS")
                                if (last.valSec < first.valSec) degradedDims.add("SEC")
                                if (last.valEcon < first.valEcon) degradedDims.add("ECON")
                                if (last.valLaw < first.valLaw) degradedDims.add("LAW")
                                if (last.valPsych < first.valPsych) degradedDims.add("PSYCH")
                                if (last.valEco < first.valEco) degradedDims.add("ECO")
                                if (last.valPhys < first.valPhys) degradedDims.add("PHYS")
                                if (last.valSoc < first.valSoc) degradedDims.add("SOC")

                                val goalTitle = "Mitigace: $eventNames"
                                val goalDesc = "Autonomní stabilizační plán pro kompenzaci negativních dopadů scénáře v dimenzích: ${if (degradedDims.isEmpty()) "Všechny dimenze stabilní" else degradedDims.joinToString(", ")}."
                                onCreateGoal(goalTitle, goalDesc)
                                Toast.makeText(context, "Vytvořen autonomní mitigační cíl", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Vytvořit cíl", tint = OmnisAmber, modifier = Modifier.size(15.dp))
                        }
                    }

                    // Kopírovat do schránky
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val summary = buildString {
                                appendLine("# O.M.N.I.S. Scenario Projection Report")
                                appendLine("Events: ${selectedEvents.joinToString { it.name }}")
                                val first = forecast.first()
                                val last = forecast.last()
                                appendLine("SYS: ${(first.valSys * 100).toInt()}% -> ${(last.valSys * 100).toInt()}%")
                                appendLine("ECON: ${(first.valEcon * 100).toInt()}% -> ${(last.valEcon * 100).toInt()}%")
                                appendLine("PSYCH: ${(first.valPsych * 100).toInt()}% -> ${(last.valPsych * 100).toInt()}%")
                                appendLine("ECO: ${(first.valEco * 100).toInt()}% -> ${(last.valEco * 100).toInt()}%")
                                appendLine("LAW: ${(first.valLaw * 100).toInt()}% -> ${(last.valLaw * 100).toInt()}%")
                                appendLine("SEC: ${(first.valSec * 100).toInt()}% -> ${(last.valSec * 100).toInt()}%")
                                appendLine("PHYS: ${(first.valPhys * 100).toInt()}% -> ${(last.valPhys * 100).toInt()}%")
                                appendLine("SOC: ${(first.valSoc * 100).toInt()}% -> ${(last.valSoc * 100).toInt()}%")
                            }
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "Prediktivní report zkopírován", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat report", tint = OmnisCyan, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filtrační lišta dimenzí
        val dimensionOptions = listOf("ALL", "SYS", "ECON", "PSYCH", "ECO", "LAW", "SEC", "PHYS", "SOC")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dimensionOptions.forEach { dim ->
                val isSelected = activeFilter == dim
                val tagColor = dimColors[dim] ?: OmnisCyan

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) tagColor.copy(alpha = 0.25f) else OmnisPanelDark,
                    border = BorderStroke(0.5.dp, if (isSelected) tagColor else OmnisBorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFilterChange(dim) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (dim != "ALL") {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(tagColor))
                        }
                        Text(
                            text = dim,
                            color = if (isSelected) tagColor else OmnisTextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = modifier,
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            if (forecast.isNotEmpty()) {
                ForecastChart(points = forecast, activeFilter = activeFilter)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Načítání kognitivních dat...", color = OmnisTextMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Souhrn dopadu
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            color = OmnisPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSynthesisExpanded = !isSynthesisExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(14.dp))
                        Text(
                            "PREDIKTIVNÍ SYNTÉZA 8D",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (isSynthesisExpanded) "Méně" else "Více",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            if (isSynthesisExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = OmnisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedEvents.isEmpty() || forecast.isEmpty()) {
                    Text(
                        "Vyberte události výše pro zahájení prediktivní simulace.",
                        color = OmnisTextMuted,
                        fontSize = 10.5.sp
                    )
                } else {
                    val last = forecast.last()
                    val first = forecast.first()

                    // Klíčové 3 primární dimenze
                    ImpactRow("Stabilita systému (SYS)", first.valSys, last.valSys, OmnisCyan)
                    ImpactRow("Ekonomická efektivita (ECON)", first.valEcon, last.valEcon, OmnisAmber)
                    ImpactRow("Bezpečnostní integrita (SEC)", first.valSec, last.valSec, Color(0xFFEF4444))

                    if (isSynthesisExpanded) {
                        ImpactRow("Psychologická integrita (PSYCH)", first.valPsych, last.valPsych, OmnisViolet)
                        ImpactRow("Ekologická udržitelnost (ECO)", first.valEco, last.valEco, OmnisEmerald)
                        ImpactRow("Právní soulad (LAW)", first.valLaw, last.valLaw, Color(0xFF60A5FA))
                        ImpactRow("Fyzická proveditelnost (PHYS)", first.valPhys, last.valPhys, Color(0xFFF97316))
                        ImpactRow("Společenská akceptace (SOC)", first.valSoc, last.valSoc, Color(0xFFEC4899))
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastChart(points: List<ForecastPoint>, activeFilter: String) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val width = size.width
        val height = size.height
        if (points.size < 2 || width <= 0 || height <= 0) return@Canvas

        val stepX = width / (points.size - 1)

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = height * (i / 4f)
            drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, y), Offset(width, y))
        }

        // Draw curves based on activeFilter
        if (activeFilter == "ALL" || activeFilter == "SYS") {
            drawForecastLine(points, { it.valSys }, OmnisCyan, height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "ECON") {
            drawForecastLine(points, { it.valEcon }, OmnisAmber, height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "SEC") {
            drawForecastLine(points, { it.valSec }, Color(0xFFEF4444), height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "PSYCH") {
            drawForecastLine(points, { it.valPsych }, OmnisViolet, height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "ECO") {
            drawForecastLine(points, { it.valEco }, OmnisEmerald, height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "LAW") {
            drawForecastLine(points, { it.valLaw }, Color(0xFF60A5FA), height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "PHYS") {
            drawForecastLine(points, { it.valPhys }, Color(0xFFF97316), height, stepX)
        }
        if (activeFilter == "ALL" || activeFilter == "SOC") {
            drawForecastLine(points, { it.valSoc }, Color(0xFFEC4899), height, stepX)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForecastLine(
    points: List<ForecastPoint>,
    selector: (ForecastPoint) -> Float,
    color: Color,
    height: Float,
    stepX: Float
) {
    val path = Path()
    points.forEachIndexed { i, point ->
        val x = i * stepX
        val y = height * (1f - selector(point).coerceIn(0f, 1f))
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}

@Composable
fun ImpactRow(label: String, initial: Float, final: Float, indicatorColor: Color = OmnisCyan) {
    val diff = final - initial
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(indicatorColor))
            Text(label, color = OmnisTextMuted, fontSize = 10.5.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${(final * 100).toInt()}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = (if (diff >= 0) "+" else "") + "${(diff * 100).toInt()}%",
                color = if (diff >= 0) OmnisEmerald else Color(0xFFEF4444),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CustomScenarioDialog(
    onDismiss: () -> Unit,
    onConfirm: (ScenarioEvent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sysImpact by remember { mutableFloatStateOf(0.2f) }
    var secImpact by remember { mutableFloatStateOf(-0.2f) }
    var econImpact by remember { mutableFloatStateOf(0.1f) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        titleContentColor = Color.White,
        textContentColor = OmnisTextMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(20.dp))
                Text("Vlastní Scénářová Událost", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název události (např. Solární bouře)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OmnisCyan
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Popis krizového faktoru", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OmnisCyan
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Vliv na Systém (SYS): ${(sysImpact * 100).toInt()}%", color = OmnisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = sysImpact,
                    onValueChange = { sysImpact = it },
                    valueRange = -0.8f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = OmnisCyan, activeTrackColor = OmnisCyan)
                )

                Text("Vliv na Bezpečnost (SEC): ${(secImpact * 100).toInt()}%", color = Color(0xFFEF4444), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = secImpact,
                    onValueChange = { secImpact = it },
                    valueRange = -0.8f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                )

                Text("Vliv na Ekonomiku (ECON): ${(econImpact * 100).toInt()}%", color = OmnisAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = econImpact,
                    onValueChange = { econImpact = it },
                    valueRange = -0.8f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = OmnisAmber, activeTrackColor = OmnisAmber)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Zadejte název scénáře", Toast.LENGTH_SHORT).show()
                    } else {
                        val newEvent = ScenarioEvent(
                            id = "custom_${System.currentTimeMillis()}",
                            name = name.trim(),
                            description = desc.ifBlank { "Uživatelem definovaný scénář" },
                            impactSys = sysImpact,
                            impactEcon = econImpact,
                            impactPsych = 0.0f,
                            impactEco = 0.0f,
                            impactLaw = 0.0f,
                            impactSec = secImpact,
                            impactPhys = 0.0f,
                            impactSoc = 0.0f,
                            color = OmnisCyan
                        )
                        onConfirm(newEvent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Přidat scénář", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = OmnisTextMuted, fontSize = 11.sp)
            }
        }
    )
}
