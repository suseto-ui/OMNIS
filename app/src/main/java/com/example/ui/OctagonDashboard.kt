package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject
import com.example.api.ComparisonResult
import com.example.data.OmnisRecord
import com.example.telemetry.OmnisPhysicalTelemetryBridge
import com.example.ui.theme.*
import com.example.ui.octagon.*

@Composable
fun OctagonDashboard(
    records: List<OmnisRecord>,
    latestRecord: OmnisRecord?,
    simSys: Float,
    simEcon: Float,
    simPsych: Float,
    simEco: Float,
    simLaw: Float,
    simSec: Float,
    simPhys: Float,
    simSoc: Float,
    fixedDomains: Set<String> = emptySet(),
    onToggleFix: (String) -> Unit = {},
    onSimChange: (Float, Float, Float, Float, Float, Float, Float, Float) -> Unit,
    isComparing: Boolean,
    comparisonResult: ComparisonResult?,
    onSynthesize: (Set<Long>) -> Unit,
    onClearComparison: () -> Unit,
    onDirectMitigate: ((String) -> Unit)? = null,
    onCreateCompensatoryGoal: ((title: String, description: String, tasks: List<String>) -> Unit)? = null
) {
    // Multi-selection hook for 8D elements
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val assistantRecords = remember(records) { records.filter { it.role == "assistant" } }
    
    val composite = (simSys + simEcon + simPsych + simEco + simLaw + simSec + simPhys + simSoc) / 8f
    val animatedComposite by animateFloatAsState(targetValue = composite, label = "composite")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var showSummaryReportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showGhostPastOverlay by remember { mutableStateOf(true) }
    var selectedDimension by remember { mutableStateOf("Sys") }

    // Hybridní Fúze & Časové zpracování (Phase XII)
    var activeViewMode by remember { mutableStateOf("RADAR") } // "RADAR", "TIME_SERIES", "FRICTION"
    val telemetrySnapshot by OmnisPhysicalTelemetryBridge.telemetryState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        OmnisPhysicalTelemetryBridge.refreshTelemetry(context)
    }

    val fusionSeries = remember(records, telemetrySnapshot) {
        Omnis8dFusionEngine.createFusionSeries(records, telemetrySnapshot)
    }
    val emaSeries = remember(fusionSeries) {
        Omnis8dFusionEngine.computeEmaSeries(fusionSeries)
    }
    val currentFusedVector = remember(fusionSeries, simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc) {
        if (fusionSeries.isNotEmpty()) {
            fusionSeries.last()
        } else {
            Omnis8dVector(
                sys = simSys, econ = simEcon, psych = simPsych, eco = simEco,
                law = simLaw, sec = simSec, phys = simPhys, soc = simSoc,
                isFused = false, label = "Manuální tenzor"
            )
        }
    }
    val statisticalAnomalies = remember(fusionSeries, currentFusedVector) {
        Omnis8dFusionEngine.detectAnomalies(fusionSeries, currentFusedVector)
    }
    val domainFrictions = remember(currentFusedVector) {
        Omnis8dFusionEngine.evaluateFriction(currentFusedVector)
    }

    // Custom Profile Preset Slots (Slot 1 & Slot 2)
    val sharedPrefs = remember { context.getSharedPreferences("omnis_tensor_presets", Context.MODE_PRIVATE) }
    var preset1Saved by remember { mutableStateOf(sharedPrefs.contains("preset_1_sys")) }
    var preset2Saved by remember { mutableStateOf(sharedPrefs.contains("preset_2_sys")) }

    // Historical Time-Travel Playback Engine
    val coroutineScope = rememberCoroutineScope()
    var isPlayingHistory by remember { mutableStateOf(false) }
    var playbackIndex by remember { mutableIntStateOf(0) }
    var playbackSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }

    val pastTensorValues = remember(latestRecord) {
        latestRecord?.let {
            listOf(it.valSys, it.valEcon, it.valPsych, it.valEco, it.valLaw, it.valSec, it.valPhys, it.valSoc)
        }
    }

    val currentVectorMap = remember(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc) {
        mapOf(
            "Sys" to simSys,
            "Econ" to simEcon,
            "Psych" to simPsych,
            "Eco" to simEco,
            "Law" to simLaw,
            "Sec" to simSec,
            "Phys" to simPhys,
            "Soc" to simSoc
        )
    }

    var activePriorityProfile by remember { mutableStateOf("UNIFORM") }
    
    val currentWeights = remember(activePriorityProfile) {
        when (activePriorityProfile) {
            "SECURITY_FIRST" -> mapOf("Sec" to 1.0f, "Law" to 1.0f, "Phys" to 0.8f, "Sys" to 0.8f, "Econ" to 0.1f, "Psych" to 0.4f, "Eco" to 0.3f, "Soc" to 0.5f)
            "BUDGET_FIRST" -> mapOf("Econ" to 1.0f, "Sys" to 0.8f, "Sec" to 0.5f, "Law" to 0.6f, "Psych" to 0.1f, "Eco" to 0.3f, "Phys" to 0.5f, "Soc" to 0.4f)
            "GREEN_FIRST" -> mapOf("Eco" to 1.0f, "Soc" to 0.8f, "Psych" to 0.8f, "Sys" to 0.6f, "Econ" to 0.3f, "Law" to 0.7f, "Sec" to 0.5f, "Phys" to 0.4f)
            else -> mapOf("Sys" to 1.0f, "Econ" to 1.0f, "Psych" to 1.0f, "Eco" to 1.0f, "Law" to 1.0f, "Sec" to 1.0f, "Phys" to 1.0f, "Soc" to 1.0f)
        }
    }

    val systemicAnalysis = remember(currentVectorMap, currentWeights) {
        OmnisCorrelationEngine.calculateWeightedSystemicEquilibrium(currentVectorMap, currentWeights)
    }

    Box(modifier = Modifier.fillMaxSize().testTag("octagon_dashboard")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Composite Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matrix_hero_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Hexagon, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "OCTAGON 8D ENGINE DASHBOARD",
                                color = OmnisCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            com.example.ui.guide.OmnisHelpIconButton(
                                title = "Octagon 8D Matice",
                                description = "8D Transdisciplinární tenzor pro hodnocení systémové rovnováhy a vzájemných vazeb napříč všemi doménami.",
                                bulletPoints = listOf(
                                    "Domény: Systém, Ekonomie, Kognice, Ekologie, Právo, Bezpečnost, Fyzika, Společnost.",
                                    "Integrální Index: Vážený průměr vyjadřující celkovou synergii ekosystému.",
                                    "Delta Obrys: Porovnání současného tenzoru s minulým stavem pro odhalení anomálií.",
                                    "Hybridní Fúze: Sloučení sémantického LLM skóre s telemetrií RAM, CPU a sítě."
                                ),
                                tint = OmnisCyan
                            )
                        }

                        // Stavová lišta hybridní fúze a tlačítko PDF exportu
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OmnisEmerald.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(OmnisEmerald, CircleShape))
                                    Text(
                                        text = "FÚZE: TELEMETRIE HW (${(telemetrySnapshot.batteryLevel * 100).toInt()}% BAT, ${(telemetrySnapshot.memoryPressure * 100).toInt()}% RAM)",
                                        color = OmnisEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val operator = com.example.auth.OmnisAuthService.currentSession.value?.username ?: "Operátor"
                                    com.example.ui.PdfExportEngine.export8dMatrixReport(
                                        context = context,
                                        operatorName = operator,
                                        currentVector = currentFusedVector,
                                        history = fusionSeries,
                                        anomalies = statisticalAnomalies,
                                        frictions = domainFrictions,
                                        telemetry = telemetrySnapshot,
                                        packageName = context.packageName
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("btn_export_8d_pdf")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("8D PDF Report", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${(animatedComposite * 100).toInt()} %",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "INTEGRÁLNÍ INDEX HARMONIE",
                            color = OmnisEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Systémová rovnováha napříč 8 transdisciplinárními doménami O.M.N.I.S.",
                            color = OmnisTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Přepínač zobrazení: Radar vs. Časové trendy vs. Křížové tření
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeViewMode == "RADAR",
                                onClick = { activeViewMode = "RADAR" },
                                label = { Text("RADAROVÝ POLYGON", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = OmnisCyan,
                                    selectedLeadingIconColor = OmnisCyan
                                )
                            )
                            FilterChip(
                                selected = activeViewMode == "TIME_SERIES",
                                onClick = { activeViewMode = "TIME_SERIES" },
                                label = { Text("ČASOVÝ VÝVOJ & EMA", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = OmnisCyan,
                                    selectedLeadingIconColor = OmnisCyan
                                )
                            )
                            FilterChip(
                                selected = activeViewMode == "FRICTION",
                                onClick = { activeViewMode = "FRICTION" },
                                label = { Text("KŘÍŽOVÉ TŘENÍ", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OmnisAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = OmnisAmber,
                                    selectedLeadingIconColor = OmnisAmber
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tělo vybraného zobrazení
                        when (activeViewMode) {
                            "RADAR" -> {
                                OctagonRadarVisualizer(
                                    values = listOf(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc),
                                    pastValues = if (showGhostPastOverlay) pastTensorValues else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                        .padding(vertical = 4.dp),
                                    fixedDomains = fixedDomains,
                                    onToggleFix = onToggleFix,
                                    selectedDimension = selectedDimension,
                                    onSelectDimension = { selectedDimension = it }
                                )
                            }
                            "TIME_SERIES" -> {
                                OctagonTimeSeriesView(
                                    rawSeries = fusionSeries,
                                    emaSeries = emaSeries,
                                    anomalies = statisticalAnomalies,
                                    onSelectRecord = { /* focus */ }
                                )
                            }
                            "FRICTION" -> {
                                OctagonFrictionView(
                                    frictions = domainFrictions,
                                    onDirectMitigate = onDirectMitigate,
                                    onCreateCompensatoryGoal = onCreateCompensatoryGoal
                                )
                            }
                        }

                        if (activeViewMode == "RADAR" && pastTensorValues != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(OmnisViolet, CircleShape))
                                    Text("Předchozí stav (#${latestRecord?.id})", color = OmnisViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                TextButton(
                                    onClick = { showGhostPastOverlay = !showGhostPastOverlay },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        if (showGhostPastOverlay) "Skrýt delta obrys" else "Zobrazit delta obrys",
                                        color = OmnisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Anomaly Alert Banner
                            val currentSimList = listOf(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc)
                            val domainNames = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")
                            val anomalies = domainNames.filterIndexed { idx, _ ->
                                val cur = currentSimList[idx]
                                val prev = pastTensorValues[idx]
                                kotlin.math.abs(cur - prev) >= 0.35f
                            }

                            if (anomalies.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OmnisRed.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisRed.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = OmnisRed, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "Detekována anomálie tenzoru (Δ ≥ 0.35): ${anomalies.joinToString(", ")}",
                                                color = OmnisRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val mitigationPrompt = "PROVEĎ AUTO-SYNTÉZU A MITIGACI TENZORU: Byly detekovány kritické anomálie v dimenzích [${anomalies.joinToString(", ")}]. " +
                                                    "Aktuální 8D parametry jsou: Sys=$simSys, Econ=$simEcon, Psych=$simPsych, Eco=$simEco, Law=$simLaw, Sec=$simSec, Phys=$simPhys, Soc=$simSoc. " +
                                                    "Navrhni okamžitá nápravná opatření a stabilizační architekturu pro vyrovnání napětí v systému."
                                                if (onDirectMitigate != null) {
                                                    onDirectMitigate(mitigationPrompt)
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(mitigationPrompt))
                                                    Toast.makeText(context, "Prompt pro auto-mitigaci byl zkopírován do schránky", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OmnisRed),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mitigovat", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Nelineární systémová rovnováha & Bod páky (Donella Meadows Leverage)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OmnisBgDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (systemicAnalysis.isCriticalFailure) OmnisRed.copy(alpha = 0.6f) else OmnisCyan.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = if (systemicAnalysis.isCriticalFailure) Icons.Default.Warning else Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "SYSTÉMOVÁ ROVNOVÁHA & LEONTIEF MATRIX",
                                            color = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "${(systemicAnalysis.systemicResilience * 100).toInt()}% Odolnost",
                                        color = if (systemicAnalysis.systemicResilience < 0.4f) OmnisRed else if (systemicAnalysis.systemicResilience < 0.7f) OmnisAmber else OmnisEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "PROFIL PRIORIT / ZAMĚŘENÍ SYSTÉMU (VÁHY):",
                                    color = OmnisCyan,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val profiles = listOf(
                                        "UNIFORM" to "Standardní",
                                        "SECURITY_FIRST" to "Bezpečnost & Spolehlivost",
                                        "BUDGET_FIRST" to "Ekonomie & Rozpočet",
                                        "GREEN_FIRST" to "Ekologie & Společnost"
                                    )
                                    profiles.forEach { (profileId, label) ->
                                        val selected = activePriorityProfile == profileId
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selected) OmnisCyan.copy(alpha = 0.25f) else OmnisBorderDark.copy(alpha = 0.5f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selected) OmnisCyan else OmnisBorderDark
                                            ),
                                            modifier = Modifier
                                                .clickable { activePriorityProfile = profileId }
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) Color.White else OmnisTextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val shortBottleneck = when (systemicAnalysis.bottleneckDomain.uppercase()) {
                                    "PHYSICAL_SYSTEMS", "PHYSICAL" -> "Phys"
                                    "SECURITY_DEFENSE", "SECURITY" -> "Sec"
                                    "LEGAL_FRAMEWORKS", "LEGAL" -> "Law"
                                    "ECOLOGY" -> "Eco"
                                    "PSYCHOLOGY" -> "Psych"
                                    "ECONOMICS" -> "Econ"
                                    "SYSTEMS_INTELLIGENCE", "SYSTEMS" -> "Sys"
                                    "SOCIOLOGY" -> "Soc"
                                    else -> systemicAnalysis.bottleneckDomain.take(6)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Harmonický průměr", color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${(systemicAnalysis.harmonicMean * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Aritmetický průměr", color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${(systemicAnalysis.arithmeticMean * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                        Text("Úzké hrdlo (Min)", color = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "$shortBottleneck (${(systemicAnalysis.bottleneckValue * 100).toInt()}%)",
                                            color = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisAmber,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (systemicAnalysis.isCriticalFailure) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚠️ KRITICKÉ RIZIKO: Doména ${systemicAnalysis.bottleneckDomain} je pod prahem 20% stability a degraduje integritu celého systému.",
                                        color = OmnisRed,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = OmnisBorderDark, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Leverage point recommendation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("PÁKOVÝ BOD (LEVERAGE POINT):", color = OmnisEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text("Zesílení domény ${systemicAnalysis.leverageDomain} přinese maximální pozitivní kaskádový zisk napříč 8D maticí.", color = OmnisTextMuted, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val prompt = "APLIKOVAT SYSTÉMOVÝ IMPULS NA PÁKOVÝ BOD [${systemicAnalysis.leverageDomain}]: " +
                                                "Analyzuj systém s parametry [Sys=$simSys, Econ=$simEcon, Psych=$simPsych, Eco=$simEco, Law=$simLaw, Sec=$simSec, Phys=$simPhys, Soc=$simSoc]. " +
                                                "Navrhni cílenou intervenci v doméně ${systemicAnalysis.leverageDomain} pro odblokování úzkého hrdla (${systemicAnalysis.bottleneckDomain}) a maximalizaci celkové harmonie."
                                            if (onDirectMitigate != null) {
                                                onDirectMitigate(prompt)
                                            } else {
                                                clipboardManager.setText(AnnotatedString(prompt))
                                                Toast.makeText(context, "Prompt pro intervenci zkopírován do schránky", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Intervenovat", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        val selectedDomainDetails = remember(selectedDimension) {
                            when (selectedDimension) {
                                "Sys" -> Triple("Systémové inženýrství & Kybernetika", Color(0xFF60A5FA), "Modularita, robustnost, čistota architektury a kybernetická stabilita systému. Vysoká hodnota znamená nízkou náchylnost k poruchám a čistou kognitivní modularitu.")
                                "Econ" -> Triple("Teorie her & Ekonomie", Color(0xFFFBBF24), "Efektivita nákladů, návratnost investice (ROI), alokace zdrojů a transakční frikce. Hodnotí rozpočtovou udržitelnost a efektivitu nasazených výpočetních tokenů.")
                                "Psych" -> Triple("Kognitivní vědy & Psychologie", Color(0xFFC084FC), "Etika, transparentnost a důvěra lidského operátora. Řeší kognitivní zátěž, srozumitelnost algoritmických výstupů (explainability) a redukci lidské chybovosti.")
                                "Eco" -> Triple("Regenerativní Ekologie", Color(0xFF34D399), "Udržitelnost, dopad na životní prostředí a materiálové cykly. Hodnotí celkovou ekologickou rovnováhu a energetickou udržitelnost biosférického ekosystému.")
                                "Law" -> Triple("Regulace & Právo", Color(0xFFFB7185), "Soulad s legislativními rámci, normami a regulacemi (např. EU AI Act, NIS2, GDPR). Zajišťuje právní jistotu, auditovatelnost a ochranu osobních dat.")
                                "Sec" -> Triple("Zero-Trust Bezpečnost", Color(0xFFEF4444), "Ochrana perimetru, kryptografické audity, šifrování a mitigace hrozeb. Řídí kybernetickou obranu a detekci kaskádových rizik.")
                                "Phys" -> Triple("Fyzikální termodynamika", Color(0xFFFB923C), "Energetická entropie, fyzikální mantinely a hardwarové zdroje. Analyzuje energetickou spotřebu, limitace procesorů a fyzické zatížení infrastruktury.")
                                "Soc" -> Triple("Socio-kulturní dynamika", Color(0xFFF472B6), "Dopad na kulturní, sociální a společenské struktury. Zajišťuje inkluzi, férovost algoritmů a minimalizuje negativní společenskou polarizaci.")
                                else -> Triple("Neznámá doména", Color.Gray, "Chybí podrobnější technická data o vybrané doméně.")
                            }
                        }

                        val selectedValue = when (selectedDimension) {
                            "Sys" -> simSys
                            "Econ" -> simEcon
                            "Psych" -> simPsych
                            "Eco" -> simEco
                            "Law" -> simLaw
                            "Sec" -> simSec
                            "Phys" -> simPhys
                            "Soc" -> simSoc
                            else -> 0.5f
                        }

                        val selectedSynergies = when (selectedDimension) {
                            "Sys" -> "Sec (+0.78), Law (+0.32), Phys (+0.40)"
                            "Econ" -> "Sys (+0.45), Law (+0.10)"
                            "Psych" -> "Soc (+0.85), Law (+0.30)"
                            "Eco" -> "Phys (+0.68), Soc (+0.50)"
                            "Law" -> "Sec (+0.60), Psych (+0.30)"
                            "Sec" -> "Sys (+0.78), Law (+0.60)"
                            "Phys" -> "Eco (+0.68), Sys (+0.40)"
                            "Soc" -> "Psych (+0.85), Eco (+0.50)"
                            else -> "Nenalezeny"
                        }

                        val selectedFrictions = when (selectedDimension) {
                            "Sys" -> "Eco (-0.15)"
                            "Econ" -> "Eco (-0.54), Sec (-0.35)"
                            "Psych" -> "Sec (-0.48)"
                            "Eco" -> "Econ (-0.54)"
                            "Law" -> "Econ (-0.20)"
                            "Sec" -> "Psych (-0.48), Econ (-0.35)"
                            "Phys" -> "Soc (-0.10)"
                            "Soc" -> "Econ (-0.15)"
                            else -> "Nenalezeny"
                        }

                        // 8D Dimension Semantic Detail Box (INTERACTIVE RADAR DETAIL CARD)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OmnisPanelDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, selectedDomainDetails.second.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(selectedDomainDetails.second, CircleShape)
                                        )
                                        Text(
                                            text = selectedDomainDetails.first.uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = selectedDomainDetails.second.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, selectedDomainDetails.second.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = selectedDimension.uppercase(),
                                                color = selectedDomainDetails.second,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Lock Status Toggle
                                    val isFixed = fixedDomains.contains(selectedDimension)
                                    IconButton(
                                        onClick = { onToggleFix(selectedDimension) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFixed) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isFixed) Color(0xFFFFD700) else OmnisTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedDomainDetails.third,
                                    color = OmnisTextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Progress and Sliders row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Úroveň: ${(selectedValue * 100).toInt()}%",
                                        color = selectedDomainDetails.second,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    LinearProgressIndicator(
                                        progress = { selectedValue },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = selectedDomainDetails.second,
                                        trackColor = OmnisBorderDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = OmnisBorderDark, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Relations Row (Synergies vs Frictions)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Synergie (+):", color = OmnisEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text(selectedSynergies, color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("Frikce (-):", color = OmnisRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text(selectedFrictions, color = OmnisTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Button
                                Button(
                                    onClick = {
                                        val promptPrompt = "ANALÝZA INTEGRITY PRO DIMENZI [${selectedDimension} - ${selectedDomainDetails.first}]: " +
                                            "Úroveň stability je ${(selectedValue * 100).toInt()}%. Vazby: Synergie=[$selectedSynergies], Frikce=[$selectedFrictions]. " +
                                            "Navrhni specifické technické a organizační kroky pro posílení stability této domény bez vyvolání negativních zpětných kaskád v ostatních oblastech."
                                        if (onDirectMitigate != null) {
                                            onDirectMitigate(promptPrompt)
                                        } else {
                                            clipboardManager.setText(AnnotatedString(promptPrompt))
                                            Toast.makeText(context, "Prompt pro doménovou analýzu byl zkopírován do schránky", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = selectedDomainDetails.second),
                                    modifier = Modifier.fillMaxWidth().height(28.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyzovat doménu ${selectedDimension}", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = OmnisBorderDark, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Multi-Layer Defensive Engine Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(13.dp))
                                Text("DEFENSE ARCHITECTURE:", color = OmnisCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text("MULTI-LAYER AKTIVNÍ", color = OmnisEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jistič: V provozu (CLOSED)", color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("Jury: Flash 1.5 Triangulace", color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Souhrnná zpráva o 8 dimenzích matice dopadů a korelaci s projektem
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_report_hero_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(OmnisCyan.copy(alpha = 0.25f), OmnisViolet.copy(alpha = 0.25f))))
                                    .border(1.dp, OmnisCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SOUHRNNÁ ZPRÁVA 8D MATICE",
                                    color = OmnisCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Vygenerovat souhrnnou zprávu o všech 8 dimenzích matice dopadů a jejich korelaci s aktuálním stavem projektu.",
                                    color = OmnisTextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { showSummaryReportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_generate_8d_summary_report")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generovat",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Interactive What-If Sliders & Domain Pinning
            item {
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
                            text = "8D CO-KDYŽ SIMULACE & FIXACE DOMÉN",
                            color = OmnisTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (fixedDomains.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${fixedDomains.size} FIX",
                                    color = Color(0xFFFFD700),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (fixedDomains.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                fixedDomains.forEach { onToggleFix(it) }
                                Toast.makeText(context, "Všechny fixace uvolněny", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = "Uvolnit vše",
                                color = OmnisCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Ontological Simulation Presets
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { showSummaryReportDialog = true },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisCyan) },
                        label = { Text("Souhrnná 8D Zpráva", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisCyan.copy(alpha = 0.2f),
                            labelColor = OmnisCyan
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisCyan
                        ),
                        modifier = Modifier.testTag("chip_generate_summary_report")
                    )
                    SuggestionChip(
                        onClick = { onSimChange(0.95f, 0.20f, 0.45f, 0.40f, 0.90f, 0.98f, 0.50f, 0.35f) },
                        label = { Text("Max Sec / Nízký rozpočet", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisRed.copy(alpha = 0.15f),
                            labelColor = OmnisRed
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisRed.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = { onSimChange(0.85f, 0.65f, 0.80f, 0.95f, 0.70f, 0.60f, 0.75f, 0.85f) },
                        label = { Text("Regenerativní Škálování", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisEmerald.copy(alpha = 0.15f),
                            labelColor = OmnisEmerald
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisEmerald.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = { onSimChange(0.90f, 0.92f, 0.55f, 0.30f, 0.65f, 0.70f, 0.80f, 0.40f) },
                        label = { Text("Hyper-Econ / Průmysl", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisAmber.copy(alpha = 0.15f),
                            labelColor = OmnisAmber
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisAmber.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            // Target equilibrium mean based on fixed vs free axes
                            val freeDomains = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc").filter { !fixedDomains.contains(it) }
                            if (freeDomains.isEmpty()) {
                                Toast.makeText(context, "Všechny osy jsou uzamčeny", Toast.LENGTH_SHORT).show()
                            } else {
                                // Calculate mean of fixed domains or fallback to optimal 0.78 equilibrium
                                val fixedMean = if (fixedDomains.isNotEmpty()) {
                                    val fixedVals = mutableListOf<Float>()
                                    if (fixedDomains.contains("Sys")) fixedVals.add(simSys)
                                    if (fixedDomains.contains("Econ")) fixedVals.add(simEcon)
                                    if (fixedDomains.contains("Psych")) fixedVals.add(simPsych)
                                    if (fixedDomains.contains("Eco")) fixedVals.add(simEco)
                                    if (fixedDomains.contains("Law")) fixedVals.add(simLaw)
                                    if (fixedDomains.contains("Sec")) fixedVals.add(simSec)
                                    if (fixedDomains.contains("Phys")) fixedVals.add(simPhys)
                                    if (fixedDomains.contains("Soc")) fixedVals.add(simSoc)
                                    fixedVals.average().toFloat()
                                } else {
                                    0.78f
                                }

                                val targetVal = fixedMean.coerceIn(0.60f, 0.85f)
                                onSimChange(
                                    if (fixedDomains.contains("Sys")) simSys else targetVal,
                                    if (fixedDomains.contains("Econ")) simEcon else targetVal,
                                    if (fixedDomains.contains("Psych")) simPsych else targetVal,
                                    if (fixedDomains.contains("Eco")) simEco else targetVal,
                                    if (fixedDomains.contains("Law")) simLaw else targetVal,
                                    if (fixedDomains.contains("Sec")) simSec else targetVal,
                                    if (fixedDomains.contains("Phys")) simPhys else targetVal,
                                    if (fixedDomains.contains("Soc")) simSoc else targetVal
                                )
                                Toast.makeText(context, "Auto-harmonizace provedena (cílová rovnováha: ${(targetVal * 100).toInt()}%)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisCyan) },
                        label = { Text("Auto-Harmonizace", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisCyan.copy(alpha = 0.15f),
                            labelColor = OmnisCyan
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisCyan.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            // Stress Scenario: Kybernetický Útok (High Sec & Law, Low Sys & Econ)
                            onSimChange(
                                if (fixedDomains.contains("Sys")) simSys else 0.30f,
                                if (fixedDomains.contains("Econ")) simEcon else 0.40f,
                                if (fixedDomains.contains("Psych")) simPsych else 0.50f,
                                if (fixedDomains.contains("Eco")) simEco else 0.70f,
                                if (fixedDomains.contains("Law")) simLaw else 0.85f,
                                if (fixedDomains.contains("Sec")) simSec else 0.98f,
                                if (fixedDomains.contains("Phys")) simPhys else 0.60f,
                                if (fixedDomains.contains("Soc")) simSoc else 0.45f
                            )
                            Toast.makeText(context, "Simulace: Kybernetický Útok / Kritická Infrastruktura", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisRed) },
                        label = { Text("Zátěž: Kybernetický Útok", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisRed.copy(alpha = 0.15f),
                            labelColor = OmnisRed
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisRed.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            // Stress Scenario: Ekologická / Energetická Krize (High Eco & Phys, Low Econ)
                            onSimChange(
                                if (fixedDomains.contains("Sys")) simSys else 0.65f,
                                if (fixedDomains.contains("Econ")) simEcon else 0.25f,
                                if (fixedDomains.contains("Psych")) simPsych else 0.60f,
                                if (fixedDomains.contains("Eco")) simEco else 0.95f,
                                if (fixedDomains.contains("Law")) simLaw else 0.80f,
                                if (fixedDomains.contains("Sec")) simSec else 0.55f,
                                if (fixedDomains.contains("Phys")) simPhys else 0.90f,
                                if (fixedDomains.contains("Soc")) simSoc else 0.75f
                            )
                            Toast.makeText(context, "Simulace: Ekologicko-Energetická Krize", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisAmber) },
                        label = { Text("Zátěž: Eko-Energetická Krize", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisAmber.copy(alpha = 0.15f),
                            labelColor = OmnisAmber
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisAmber.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = { onSimChange(0.70f, 0.70f, 0.70f, 0.70f, 0.70f, 0.70f, 0.70f, 0.70f) },
                        label = { Text("Vyvážený Reset (0.70)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisBorderDark,
                            labelColor = OmnisCyan
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisBorderDark
                        )
                    )
                    // Custom Preset Slot 1 (Load / Save on Long Press via icon action)
                    SuggestionChip(
                        onClick = {
                            if (preset1Saved) {
                                val sSys = sharedPrefs.getFloat("preset_1_sys", 0.7f)
                                val sEcon = sharedPrefs.getFloat("preset_1_econ", 0.7f)
                                val sPsych = sharedPrefs.getFloat("preset_1_psych", 0.7f)
                                val sEco = sharedPrefs.getFloat("preset_1_eco", 0.7f)
                                val sLaw = sharedPrefs.getFloat("preset_1_law", 0.7f)
                                val sSec = sharedPrefs.getFloat("preset_1_sec", 0.7f)
                                val sPhys = sharedPrefs.getFloat("preset_1_phys", 0.7f)
                                val sSoc = sharedPrefs.getFloat("preset_1_soc", 0.7f)
                                onSimChange(
                                    if (fixedDomains.contains("Sys")) simSys else sSys,
                                    if (fixedDomains.contains("Econ")) simEcon else sEcon,
                                    if (fixedDomains.contains("Psych")) simPsych else sPsych,
                                    if (fixedDomains.contains("Eco")) simEco else sEco,
                                    if (fixedDomains.contains("Law")) simLaw else sLaw,
                                    if (fixedDomains.contains("Sec")) simSec else sSec,
                                    if (fixedDomains.contains("Phys")) simPhys else sPhys,
                                    if (fixedDomains.contains("Soc")) simSoc else sSoc
                                )
                                Toast.makeText(context, "Preset Alpha načten", Toast.LENGTH_SHORT).show()
                            } else {
                                sharedPrefs.edit()
                                    .putFloat("preset_1_sys", simSys)
                                    .putFloat("preset_1_econ", simEcon)
                                    .putFloat("preset_1_psych", simPsych)
                                    .putFloat("preset_1_eco", simEco)
                                    .putFloat("preset_1_law", simLaw)
                                    .putFloat("preset_1_sec", simSec)
                                    .putFloat("preset_1_phys", simPhys)
                                    .putFloat("preset_1_soc", simSoc)
                                    .apply()
                                preset1Saved = true
                                Toast.makeText(context, "Preset Alpha uložen z aktuálního stavu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = {
                            Icon(
                                if (preset1Saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = OmnisViolet
                            )
                        },
                        label = { Text(if (preset1Saved) "Slot α (Načíst)" else "Slot α (Uložit)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisViolet.copy(alpha = 0.15f),
                            labelColor = OmnisViolet
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisViolet.copy(alpha = 0.5f)
                        )
                    )
                    // Custom Preset Slot 2
                    SuggestionChip(
                        onClick = {
                            if (preset2Saved) {
                                val sSys = sharedPrefs.getFloat("preset_2_sys", 0.7f)
                                val sEcon = sharedPrefs.getFloat("preset_2_econ", 0.7f)
                                val sPsych = sharedPrefs.getFloat("preset_2_psych", 0.7f)
                                val sEco = sharedPrefs.getFloat("preset_2_eco", 0.7f)
                                val sLaw = sharedPrefs.getFloat("preset_2_law", 0.7f)
                                val sSec = sharedPrefs.getFloat("preset_2_sec", 0.7f)
                                val sPhys = sharedPrefs.getFloat("preset_2_phys", 0.7f)
                                val sSoc = sharedPrefs.getFloat("preset_2_soc", 0.7f)
                                onSimChange(
                                    if (fixedDomains.contains("Sys")) simSys else sSys,
                                    if (fixedDomains.contains("Econ")) simEcon else sEcon,
                                    if (fixedDomains.contains("Psych")) simPsych else sPsych,
                                    if (fixedDomains.contains("Eco")) simEco else sEco,
                                    if (fixedDomains.contains("Law")) simLaw else sLaw,
                                    if (fixedDomains.contains("Sec")) simSec else sSec,
                                    if (fixedDomains.contains("Phys")) simPhys else sPhys,
                                    if (fixedDomains.contains("Soc")) simSoc else sSoc
                                )
                                Toast.makeText(context, "Preset Beta načten", Toast.LENGTH_SHORT).show()
                            } else {
                                sharedPrefs.edit()
                                    .putFloat("preset_2_sys", simSys)
                                    .putFloat("preset_2_econ", simEcon)
                                    .putFloat("preset_2_psych", simPsych)
                                    .putFloat("preset_2_eco", simEco)
                                    .putFloat("preset_2_law", simLaw)
                                    .putFloat("preset_2_sec", simSec)
                                    .putFloat("preset_2_phys", simPhys)
                                    .putFloat("preset_2_soc", simSoc)
                                    .apply()
                                preset2Saved = true
                                Toast.makeText(context, "Preset Beta uložen z aktuálního stavu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = {
                            Icon(
                                if (preset2Saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = OmnisCyan
                            )
                        },
                        label = { Text(if (preset2Saved) "Slot β (Načíst)" else "Slot β (Uložit)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisCyan.copy(alpha = 0.15f),
                            labelColor = OmnisCyan
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisCyan.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            val jsonState = """
                                {
                                  "tensor_version": "8D-OMNIS-V1",
                                  "sys": $simSys,
                                  "econ": $simEcon,
                                  "psych": $simPsych,
                                  "eco": $simEco,
                                  "law": $simLaw,
                                  "sec": $simSec,
                                  "phys": $simPhys,
                                  "soc": $simSoc,
                                  "composite": $composite,
                                  "fixed_domains": ${fixedDomains.joinToString(prefix = "[\"", separator = "\", \"", postfix = "\"]")}
                                }
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(jsonState))
                            Toast.makeText(context, "8D Tenzor zkopírován do schránky (JSON)", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisViolet) },
                        label = { Text("Export JSON", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisViolet.copy(alpha = 0.15f),
                            labelColor = OmnisViolet
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisViolet.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            val report = StringBuilder().apply {
                                appendLine("# 🌐 OMNIS 8D ARCHITECTURAL AUDIT REPORT")
                                appendLine("---")
                                appendLine("- **Generováno:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                                appendLine("- **Kompozitní Index:** ${(composite * 100).toInt()}%")
                                appendLine("- **Referenční záznam:** #${latestRecord?.id ?: "N/A"}")
                                appendLine()
                                appendLine("### 📊 8D Tenzorový Rozpad")
                                appendLine("| Dimenze | Kód | Aktuální Váha | Fixace | Popis |")
                                appendLine("|---|---|---|---|---|")
                                appendLine("| Systémové Inženýrství | `Sys` | ${(simSys * 100).toInt()}% | ${if (fixedDomains.contains("Sys")) "🔒 Uzamčeno" else "🔓 Volné"} | Modularita & Kybernetika |")
                                appendLine("| Ekonomie | `Econ` | ${(simEcon * 100).toInt()}% | ${if (fixedDomains.contains("Econ")) "🔒 Uzamčeno" else "🔓 Volné"} | Nákladová efektivita |")
                                appendLine("| Kognice & Etika | `Psych` | ${(simPsych * 100).toInt()}% | ${if (fixedDomains.contains("Psych")) "🔒 Uzamčeno" else "🔓 Volné"} | Důvěra operátora |")
                                appendLine("| Ekologie | `Eco` | ${(simEco * 100).toInt()}% | ${if (fixedDomains.contains("Eco")) "🔒 Uzamčeno" else "🔓 Volné"} | Regenerativní biosféra |")
                                appendLine("| Právo & Soulad | `Law` | ${(simLaw * 100).toInt()}% | ${if (fixedDomains.contains("Law")) "🔒 Uzamčeno" else "🔓 Volné"} | Legislativa a normy |")
                                appendLine("| Zero-Trust Bezpečnost | `Sec` | ${(simSec * 100).toInt()}% | ${if (fixedDomains.contains("Sec")) "🔒 Uzamčeno" else "🔓 Volné"} | Bezpečnost a rizika |")
                                appendLine("| Termodynamika | `Phys` | ${(simPhys * 100).toInt()}% | ${if (fixedDomains.contains("Phys")) "🔒 Uzamčeno" else "🔓 Volné"} | Fyzikální limity |")
                                appendLine("| Sociální Dopad | `Soc` | ${(simSoc * 100).toInt()}% | ${if (fixedDomains.contains("Soc")) "🔒 Uzamčeno" else "🔓 Volné"} | Kulturní dynamika |")
                                appendLine()
                                appendLine("### 🛡️ Bezpečnostní status: Zero-Trust Strict Sandbox Active")
                            }.toString()

                            clipboardManager.setText(AnnotatedString(report))
                            Toast.makeText(context, "Auditní zpráva zkopírována do schránky (Markdown)", Toast.LENGTH_LONG).show()
                        },
                        icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisEmerald) },
                        label = { Text("Export Audit Report", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisEmerald.copy(alpha = 0.15f),
                            labelColor = OmnisEmerald
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisEmerald.copy(alpha = 0.5f)
                        )
                    )
                    SuggestionChip(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text ?: ""
                            importJsonText = clipText
                            showImportDialog = true
                        },
                        icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisCyan) },
                        label = { Text("Import JSON", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = OmnisCyan.copy(alpha = 0.15f),
                            labelColor = OmnisCyan
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = OmnisCyan.copy(alpha = 0.5f)
                        )
                    )
                    // Historical Playback / Step-Through
                    if (assistantRecords.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {
                                if (isPlayingHistory) {
                                    isPlayingHistory = false
                                } else {
                                    isPlayingHistory = true
                                    coroutineScope.launch {
                                        val recordsList = assistantRecords.reversed() // oldest to newest
                                        for (i in recordsList.indices) {
                                            if (!isPlayingHistory) break
                                            playbackIndex = i
                                            val r = recordsList[i]
                                            onSimChange(
                                                if (fixedDomains.contains("Sys")) simSys else r.valSys,
                                                if (fixedDomains.contains("Econ")) simEcon else r.valEcon,
                                                if (fixedDomains.contains("Psych")) simPsych else r.valPsych,
                                                if (fixedDomains.contains("Eco")) simEco else r.valEco,
                                                if (fixedDomains.contains("Law")) simLaw else r.valLaw,
                                                if (fixedDomains.contains("Sec")) simSec else r.valSec,
                                                if (fixedDomains.contains("Phys")) simPhys else r.valPhys,
                                                if (fixedDomains.contains("Soc")) simSoc else r.valSoc
                                            )
                                            delay((1000L / playbackSpeedMultiplier).toLong())
                                        }
                                        isPlayingHistory = false
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    if (isPlayingHistory) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = OmnisAmber
                                )
                            },
                            label = { Text(if (isPlayingHistory) "Přehrávání (${playbackIndex + 1}/${assistantRecords.size})" else "Přehrát Historii", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = OmnisAmber.copy(alpha = 0.15f),
                                labelColor = OmnisAmber
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = OmnisAmber.copy(alpha = 0.5f)
                            )
                        )
                        SuggestionChip(
                            onClick = {
                                playbackSpeedMultiplier = when (playbackSpeedMultiplier) {
                                    0.5f -> 1.0f
                                    1.0f -> 2.0f
                                    2.0f -> 4.0f
                                    else -> 0.5f
                                }
                            },
                            icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = OmnisAmber) },
                            label = { Text("${playbackSpeedMultiplier}x Rychlost", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = OmnisAmber.copy(alpha = 0.15f),
                                labelColor = OmnisAmber
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = OmnisAmber.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Systémové inženýrství & Kybernetika",
                    desc = "Modularita, robustnost a čistota architektury",
                    value = simSys,
                    color = Color(0xFF60A5FA),
                    icon = Icons.Default.Settings,
                    testTag = "slider_sys",
                    isFixed = fixedDomains.contains("Sys"),
                    onToggleFix = { onToggleFix("Sys") },
                    onValueChange = { if (!fixedDomains.contains("Sys")) onSimChange(it, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Teorie her & Ekonomie",
                    desc = "Efektivita nákladů a návratnost investice",
                    value = simEcon,
                    color = Color(0xFFFBBF24),
                    icon = Icons.Default.Paid,
                    testTag = "slider_econ",
                    isFixed = fixedDomains.contains("Econ"),
                    onToggleFix = { onToggleFix("Econ") },
                    onValueChange = { if (!fixedDomains.contains("Econ")) onSimChange(simSys, it, simPsych, simEco, simLaw, simSec, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Kognitivní vědy & Psychologie",
                    desc = "Etika, transparentnost a důvěra operátora",
                    value = simPsych,
                    color = Color(0xFFC084FC),
                    icon = Icons.Default.Face,
                    testTag = "slider_psych",
                    isFixed = fixedDomains.contains("Psych"),
                    onToggleFix = { onToggleFix("Psych") },
                    onValueChange = { if (!fixedDomains.contains("Psych")) onSimChange(simSys, simEcon, it, simEco, simLaw, simSec, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Regenerativní Ekologie",
                    desc = "Udržitelnost a regenerativní potenciál biosféry",
                    value = simEco,
                    color = Color(0xFF34D399),
                    icon = Icons.Default.Spa,
                    testTag = "slider_eco",
                    isFixed = fixedDomains.contains("Eco"),
                    onToggleFix = { onToggleFix("Eco") },
                    onValueChange = { if (!fixedDomains.contains("Eco")) onSimChange(simSys, simEcon, simPsych, it, simLaw, simSec, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Regulace & Právo",
                    desc = "Soulad s legislativou a normami",
                    value = simLaw,
                    color = Color(0xFFFB7185),
                    icon = Icons.Default.Gavel,
                    testTag = "slider_law",
                    isFixed = fixedDomains.contains("Law"),
                    onToggleFix = { onToggleFix("Law") },
                    onValueChange = { if (!fixedDomains.contains("Law")) onSimChange(simSys, simEcon, simPsych, simEco, it, simSec, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Zero-Trust Bezpečnost",
                    desc = "Ochrana perimetru a mitigace rizik",
                    value = simSec,
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.Security,
                    testTag = "slider_sec",
                    isFixed = fixedDomains.contains("Sec"),
                    onToggleFix = { onToggleFix("Sec") },
                    onValueChange = { if (!fixedDomains.contains("Sec")) onSimChange(simSys, simEcon, simPsych, simEco, simLaw, it, simPhys, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Fyzikální termodynamika",
                    desc = "Energetická entropie a fyzikální mantinely",
                    value = simPhys,
                    color = Color(0xFFFB923C),
                    icon = Icons.Default.Speed,
                    testTag = "slider_phys",
                    isFixed = fixedDomains.contains("Phys"),
                    onToggleFix = { onToggleFix("Phys") },
                    onValueChange = { if (!fixedDomains.contains("Phys")) onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, it, simSoc) }
                )
            }

            item {
                OctagonDimensionSliderCard(
                    title = "Socio-kulturní dynamika",
                    desc = "Dopad na kulturní a sociální struktury",
                    value = simSoc,
                    color = Color(0xFFF472B6),
                    icon = Icons.Default.Groups,
                    testTag = "slider_soc",
                    isFixed = fixedDomains.contains("Soc"),
                    onToggleFix = { onToggleFix("Soc") },
                    onValueChange = { if (!fixedDomains.contains("Soc")) onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, it) }
                )
            }

            // 8D Tensor Differential Delta Analyzer (Simulated vs Last Stored)
            if (pastTensorValues != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("tensor_delta_analyzer_card"),
                        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "8D Diferenciální Delta Analýza",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Ref: #${latestRecord?.id}",
                                    color = OmnisViolet,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val currentDims = listOf(
                                Triple("Sys", simSys, pastTensorValues[0]),
                                Triple("Econ", simEcon, pastTensorValues[1]),
                                Triple("Psych", simPsych, pastTensorValues[2]),
                                Triple("Eco", simEco, pastTensorValues[3]),
                                Triple("Law", simLaw, pastTensorValues[4]),
                                Triple("Sec", simSec, pastTensorValues[5]),
                                Triple("Phys", simPhys, pastTensorValues[6]),
                                Triple("Soc", simSoc, pastTensorValues[7])
                            )

                            currentDims.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPair.forEach { (name, cur, prev) ->
                                        val delta = cur - prev
                                        val deltaColor = when {
                                            delta > 0.05f -> OmnisEmerald
                                            delta < -0.05f -> OmnisRed
                                            else -> OmnisTextMuted
                                        }
                                        val deltaSign = if (delta > 0) "+" else ""

                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(6.dp),
                                            color = OmnisBgDark.copy(alpha = 0.6f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${(cur * 100).toInt()}%",
                                                        color = OmnisTextMuted,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = deltaColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "$deltaSign${(delta * 100).toInt()}%",
                                                            color = deltaColor,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
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
            }

            // 8D Comparison Hook Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("octagon_comparison_section")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = OmnisViolet)
                            Text(
                                text = "8D SROVNÁVACÍ PIPELINE & SYNTÉZA",
                                color = OmnisViolet,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Zaškrtněte alespoň 2 prvky pro side-by-side matici a vytvoření harmonizované strategie.",
                            color = OmnisTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // List of Assistant Records for Selection
            items(assistantRecords.reversed(), key = { it.id }) { record ->
                val isSelected = selectedIds.contains(record.id)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) OmnisViolet.copy(alpha = 0.15f) else OmnisPanelDark,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) OmnisViolet else OmnisBorderDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("element_selection_${record.id}")
                        .clickable {
                            selectedIds = if (isSelected) {
                                selectedIds - record.id
                            } else {
                                selectedIds + record.id
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedIds = if (isSelected) selectedIds - record.id else selectedIds + record.id
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Vybrat prvek",
                                tint = if (isSelected) OmnisViolet else OmnisTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "8D Prvek #${record.id}",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Skóre: ${(record.compositeScore * 100).toInt()}%",
                                    color = OmnisEmerald,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = record.content.take(90) + if (record.content.length > 90) "..." else "",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            // Mini 8D indicators
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OMNIS_8D_DIMENSIONS.take(4).forEach { dim ->
                                    val v = dim.getter(record)
                                    Surface(
                                        color = dim.color.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, dim.color.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "${dim.key}:${(v * 10).toInt()}",
                                            color = dim.color,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // Floating Synthesis Trigger Button
        AnimatedVisibility(
            visible = selectedIds.size >= 2,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { onSynthesize(selectedIds) },
                containerColor = OmnisViolet,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = {
                    Text(
                        text = "Syntetizovat 8D prvky (${selectedIds.size})",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier.testTag("btn_synthesize_selected")
            )
        }
    }

    // Comparison Modal
    if (isComparing || comparisonResult != null) {
        val selectedRecords = remember(selectedIds, records) {
            records.filter { selectedIds.contains(it.id) }
        }
        ComparisonModal(
            isComparing = isComparing,
            result = comparisonResult,
            selectedRecords = selectedRecords,
            onDismiss = onClearComparison
        )
    }

    // Souhrnná zpráva o 8 dimenzích matice dopadů a korelaci s projektem
    if (showSummaryReportDialog) {
        OctagonSummaryReportDialog(
            simSys = simSys,
            simEcon = simEcon,
            simPsych = simPsych,
            simEco = simEco,
            simLaw = simLaw,
            simSec = simSec,
            simPhys = simPhys,
            simSoc = simSoc,
            fixedDomains = fixedDomains,
            latestRecord = latestRecord,
            records = records,
            activePriorityProfile = activePriorityProfile,
            onDismiss = { showSummaryReportDialog = false },
            onDirectMitigate = onDirectMitigate
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = OmnisCyan)
                    Text("Import 8D Tenzoru", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Vložte JSON definici 8D parametrů (nebo načtěte ze schránky):",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("import_json_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        placeholder = {
                            Text("{\"sys\": 0.8, \"econ\": 0.6, ...}", color = OmnisTextMuted.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark,
                            cursorColor = OmnisCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val obj = JSONObject(importJsonText)
                            val newSys = (obj.optDouble("sys", simSys.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newEcon = (obj.optDouble("econ", simEcon.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newPsych = (obj.optDouble("psych", simPsych.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newEco = (obj.optDouble("eco", simEco.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newLaw = (obj.optDouble("law", simLaw.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newSec = (obj.optDouble("sec", simSec.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newPhys = (obj.optDouble("phys", simPhys.toDouble())).toFloat().coerceIn(0f, 1f)
                            val newSoc = (obj.optDouble("soc", simSoc.toDouble())).toFloat().coerceIn(0f, 1f)
                            
                            onSimChange(newSys, newEcon, newPsych, newEco, newLaw, newSec, newPhys, newSoc)
                            showImportDialog = false
                            Toast.makeText(context, "8D Tenzor úspěšně importován", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Chyba formátu JSON: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan)
                ) {
                    Text("Aplikovat tenzor", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Zrušit", color = OmnisTextMuted)
                }
            },
            containerColor = OmnisPanelDark
        )
    }
}

@Composable
fun ComparisonModal(
    isComparing: Boolean,
    result: ComparisonResult?,
    selectedRecords: List<OmnisRecord>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("comparison_modal"),
            shape = RoundedCornerShape(16.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisViolet)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(OmnisViolet, OmnisCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "8D COMPARISON & SYNTHESIS",
                                color = OmnisViolet,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Porovnání ${selectedRecords.size} prvků & Harmonizace",
                                color = OmnisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_comparison_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = OmnisTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isComparing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OmnisViolet)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Zpracovávám 8D kognitivní syntézu...",
                                color = OmnisCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Harmonizace dimenzí a výpočet systémové koherence",
                                color = OmnisTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Side-by-Side 8D Impact Matrix
                        item {
                            Text(
                                text = "1. SIDE-BY-SIDE 8D IMPACT MATRIX",
                                color = OmnisCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SideBySideMatrixView(selectedRecords = selectedRecords)
                        }

                        // Section 2: AI Comparative Analysis
                        if (result != null && result.comparisonText.isNotBlank()) {
                            item {
                                Text(
                                    text = "2. STRATEGICKÉ ZHODNOCENÍ ODCHYLEK",
                                    color = OmnisAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OmnisBgDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = result.comparisonText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        // Section 3: Harmonized Strategy Synthesis
                        if (result != null && result.harmonizedStrategy.isNotBlank()) {
                            item {
                                Text(
                                    text = "3. HARMONIZOVANÁ SYNTÉZA & POSTUP",
                                    color = OmnisEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OmnisEmerald.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "SJEDNOCENÁ SYSTÉMOVÁ DIREKTIVA",
                                                color = OmnisEmerald,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = result.harmonizedStrategy,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet)
                    ) {
                        Text("Zavřít syntézu", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SideBySideMatrixView(selectedRecords: List<OmnisRecord>) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        val horizontalScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(12.dp)
        ) {
            // Header with element tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOMÉNA",
                    color = OmnisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(110.dp)
                )
                selectedRecords.forEach { rec ->
                    Surface(
                        color = OmnisPanelDark,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text(
                            text = "#${rec.id}",
                            color = OmnisCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(6.dp),
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = "DELTA",
                    color = OmnisEmerald,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = OmnisBorderDark)
            Spacer(modifier = Modifier.height(8.dp))

            // Rows for each 8D dimension
            OMNIS_8D_DIMENSIONS.forEach { dim ->
                val values = selectedRecords.map { dim.getter(it) }
                val minVal = values.minOrNull() ?: 0f
                val maxVal = values.maxOrNull() ?: 0f
                val delta = maxVal - minVal

                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.width(110.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dim.color)
                        )
                        Text(
                            text = dim.key,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    selectedRecords.forEach { rec ->
                        val v = dim.getter(rec)
                        Text(
                            text = String.format("%.2f", v),
                            color = dim.color,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(80.dp)
                        )
                    }

                    Text(
                        text = if (delta > 0.001f) "±" + String.format("%.2f", delta) else "0.00",
                        color = if (delta > 0.25f) OmnisAmber else OmnisEmerald,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OctagonDimensionSliderCard(
    title: String,
    desc: String,
    value: Float,
    color: Color,
    icon: ImageVector,
    testTag: String,
    isFixed: Boolean = false,
    onToggleFix: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isFixed) OmnisPanelDark.copy(alpha = 0.95f) else OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isFixed) color else OmnisBorderDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onToggleFix, 
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isFixed) Icons.Default.Lock else icon, 
                            contentDescription = if (isFixed) "Odemknout dimenzi $title" else "Uzamknout dimenzi $title", 
                            tint = if (isFixed) Color(0xFFFFD700) else color, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isFixed) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "LOCK",
                                        color = Color(0xFFFFD700),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = desc,
                            color = OmnisTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${(value * 100).toInt()}%",
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = !isFixed,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = if (isFixed) OmnisTextMuted else color,
                    activeTrackColor = if (isFixed) OmnisTextMuted.copy(alpha = 0.5f) else color,
                    inactiveTrackColor = OmnisBorderDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Komponenta pro křížovou analýzu tření a synergií mezi 8D doménami.
 */
@Composable
fun OctagonFrictionView(
    frictions: List<DomainFrictionPair>,
    onDirectMitigate: ((String) -> Unit)? = null,
    onCreateCompensatoryGoal: ((title: String, description: String, tasks: List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
            Text(
                text = "KŘÍŽOVÉ TŘENÍ A SYNERGIE MEZI DOMÉNAMI",
                color = OmnisAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        frictions.forEach { pair ->
            val isFriction = pair.frictionScore > 0.35f
            val cardBorder = if (isFriction) OmnisAmber else OmnisEmerald
            val cardBg = if (isFriction) OmnisAmber.copy(alpha = 0.08f) else OmnisEmerald.copy(alpha = 0.05f)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OmnisPanelDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pair.dim1Name} ↔ ${pair.dim2Name}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = cardBorder.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = pair.status,
                                color = cardBorder,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pair.description,
                        color = OmnisTextMuted,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Napětí: ${(pair.frictionScore * 100).toInt()}%",
                            color = cardBorder,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isFriction && onCreateCompensatoryGoal != null) {
                                Button(
                                    onClick = {
                                        val title = "Kompenzace 8D: ${pair.dim1Name} ↔ ${pair.dim2Name}"
                                        val desc = "Plán pro eliminaci systémového tření: ${pair.description}. Doporučení: ${pair.recommendation}"
                                        val tasks = listOf(
                                            "1. Analyzovat zdroj pnutí v doméně ${pair.dim1Name}",
                                            "2. Aplikovat kompenzační limity pro ${pair.dim2Name}",
                                            "3. Re-evaluovat 8D tenzor v matici O.M.N.I.S."
                                        )
                                        onCreateCompensatoryGoal(title, desc, tasks)
                                        Toast.makeText(context, "Cíl byl úspěšně převzat do Autonomních Cílů!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Převzít do cílů", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (isFriction && onDirectMitigate != null) {
                                Button(
                                    onClick = {
                                        val prompt = "PROVEĎ HARMONIZACI TŘENÍ: Domény [${pair.dim1Name}] a [${pair.dim2Name}] vykazují tření ${(pair.frictionScore * 100).toInt()}%. " +
                                            "Doporučení k nápravě: ${pair.recommendation}. Navrhni kompromisní systémové řešení."
                                        onDirectMitigate(prompt)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OmnisAmber),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Harmonizovat", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

