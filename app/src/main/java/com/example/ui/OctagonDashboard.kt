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
import org.json.JSONObject
import org.json.JSONArray
import com.example.api.ComparisonResult
import com.example.data.OmnisRecord
import com.example.ui.theme.*

data class DimensionDefinition(
    val key: String,
    val name: String,
    val desc: String,
    val color: Color,
    val getter: (OmnisRecord) -> Float
)

val OMNIS_8D_DIMENSIONS = listOf(
    DimensionDefinition("Sys", "Systémové inž.", "Modularita a kybernetika", Color(0xFF60A5FA)) { it.valSys },
    DimensionDefinition("Econ", "Ekonomie", "Nákladová efektivita", Color(0xFFFBBF24)) { it.valEcon },
    DimensionDefinition("Psych", "Kognice & Etika", "Důvěra operátora", Color(0xFFC084FC)) { it.valPsych },
    DimensionDefinition("Eco", "Ekologie", "Regenerativní biosféra", Color(0xFF34D399)) { it.valEco },
    DimensionDefinition("Law", "Právo & Soulad", "Legislativa a normy", Color(0xFFFB7185)) { it.valLaw },
    DimensionDefinition("Sec", "Zero-Trust", "Bezpečnost a rizika", Color(0xFFEF4444)) { it.valSec },
    DimensionDefinition("Phys", "Termodynamika", "Fyzikální limity", Color(0xFFFB923C)) { it.valPhys },
    DimensionDefinition("Soc", "Sociální dopad", "Kulturní dynamika", Color(0xFFF472B6)) { it.valSoc }
)

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
    onDirectMitigate: ((String) -> Unit)? = null
) {
    // Multi-selection hook for 8D elements
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val assistantRecords = remember(records) { records.filter { it.role == "assistant" } }
    
    val composite = (simSys + simEcon + simPsych + simEco + simLaw + simSec + simPhys + simSoc) / 8f
    val animatedComposite by animateFloatAsState(targetValue = composite, label = "composite")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showGhostPastOverlay by remember { mutableStateOf(true) }

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
                                    "Delta Obrys: Porovnání současného tenzoru s minulým stavem pro odhalení anomálií."
                                ),
                                tint = OmnisCyan
                            )
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
                        
                        // 8D Radar Geometric Visualizer with Tension Links & Ghost Past Overlay
                        OctagonRadarVisualizer(
                            values = listOf(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc),
                            pastValues = if (showGhostPastOverlay) pastTensorValues else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(vertical = 4.dp),
                            fixedDomains = fixedDomains,
                            onToggleFix = onToggleFix
                        )

                        if (pastTensorValues != null) {
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
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "8D Diferenciální Delta Analýza",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Ref: #${latestRecord?.id}",
                                    color = OmnisViolet,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
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
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = OmnisViolet)
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
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
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
        Column(modifier = Modifier.padding(14.dp)) {
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

@Composable
fun OctagonRadarVisualizer(
    values: List<Float>,
    modifier: Modifier = Modifier,
    pastValues: List<Float>? = null,
    fixedDomains: Set<String> = emptySet(),
    onToggleFix: ((String) -> Unit)? = null
) {
    val labels = remember { listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc") }
    val dimensionColors = remember {
        listOf(
            Color(0xFF60A5FA),
            Color(0xFFFBBF24),
            Color(0xFFC084FC),
            Color(0xFF34D399),
            Color(0xFFFB7185),
            Color(0xFFEF4444),
            Color(0xFFFB923C),
            Color(0xFFF472B6)
        )
    }

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.pointerInput(labels) {
            detectTapGestures { tapOffset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (minOf(size.width, size.height) / 2f) * 0.70f
                val numPoints = 8
                val angleStep = (2.0 * Math.PI / numPoints).toFloat()

                // Find if tap is near any vertex node or label
                for (i in 0 until numPoints) {
                    val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                    val v = values.getOrElse(i) { 0.5f }.coerceIn(0.05f, 1f)
                    val nodePt = Offset(center.x + (radius * v) * cos(angle), center.y + (radius * v) * sin(angle))
                    val labelRadius = radius + 32f
                    val labelPt = Offset(center.x + labelRadius * cos(angle), center.y + labelRadius * sin(angle))

                    val distToNode = kotlin.math.hypot(tapOffset.x - nodePt.x, tapOffset.y - nodePt.y)
                    val distToLabel = kotlin.math.hypot(tapOffset.x - labelPt.x, tapOffset.y - labelPt.y)

                    if (distToNode <= 40f || distToLabel <= 40f) {
                        onToggleFix?.invoke(labels[i])
                        break
                    }
                }
            }
        }, 
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("octagon_radar_canvas")) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.70f
            val numPoints = 8
            val angleStep = (2.0 * Math.PI / numPoints).toFloat()

            // 1. Concentric web rings (0.25, 0.50, 0.75, 1.0)
            val levels = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            levels.forEach { level ->
                val ringPath = Path()
                for (i in 0 until numPoints) {
                    val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                    val r = radius * level
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(
                    path = ringPath,
                    color = OmnisBorderDark.copy(alpha = if (level == 1.0f) 0.8f else 0.35f),
                    style = Stroke(width = if (level == 1.0f) 1.5f else 1f)
                )
            }

            // 2. Radial spokes & Outer Labels
            for (i in 0 until numPoints) {
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                drawLine(
                    color = OmnisBorderDark.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 1f
                )

                // Draw domain text label on outer boundary
                val labelRadius = radius + 32f
                val lx = center.x + labelRadius * cos(angle)
                val ly = center.y + labelRadius * sin(angle) + 10f
                val domainName = labels[i]
                val isFixed = fixedDomains.contains(domainName)
                
                textPaint.color = if (isFixed) {
                    android.graphics.Color.rgb(255, 215, 0)
                } else {
                    val c = dimensionColors[i]
                    android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    if (isFixed) "$domainName*" else domainName,
                    lx,
                    ly,
                    textPaint
                )
            }

            // 3. Tension / Synergy Correlation cross-lines between key axes
            val tensionPairs = listOf(
                Pair(1, 3), // Econ vs Eco (Friction)
                Pair(1, 5), // Econ vs Sec (Friction)
                Pair(5, 2), // Sec vs Psych (Tension)
                Pair(0, 5), // Sys + Sec (Synergy)
                Pair(4, 5)  // Law + Sec (Synergy)
            )

            tensionPairs.forEach { pair ->
                val idxA = pair.first
                val idxB = pair.second
                val valA = values.getOrElse(idxA) { 0.5f }.coerceIn(0.1f, 1f)
                val valB = values.getOrElse(idxB) { 0.5f }.coerceIn(0.1f, 1f)

                val angleA = idxA * angleStep - (Math.PI / 2.0).toFloat()
                val angleB = idxB * angleStep - (Math.PI / 2.0).toFloat()

                val ptA = Offset(center.x + (radius * valA) * cos(angleA), center.y + (radius * valA) * sin(angleA))
                val ptB = Offset(center.x + (radius * valB) * cos(angleB), center.y + (radius * valB) * sin(angleB))

                val isFriction = (idxA == 1 && idxB == 3) || (idxA == 1 && idxB == 5) || (idxA == 5 && idxB == 2)
                val lineColor = if (isFriction) OmnisAmber.copy(alpha = 0.45f) else OmnisCyan.copy(alpha = 0.35f)

                drawLine(
                    color = lineColor,
                    start = ptA,
                    end = ptB,
                    strokeWidth = 1.2f
                )
            }

            // 4. Past Record Ghost Polygon (Temporal Delta Comparison)
            pastValues?.let { pastList ->
                if (pastList.size == numPoints) {
                    val pastPath = Path()
                    for (i in 0 until numPoints) {
                        val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                        val pv = pastList[i].coerceIn(0.05f, 1f)
                        val pr = radius * pv
                        val px = center.x + pr * cos(angle)
                        val py = center.y + pr * sin(angle)
                        if (i == 0) pastPath.moveTo(px, py) else pastPath.lineTo(px, py)
                    }
                    pastPath.close()

                    drawPath(
                        path = pastPath,
                        color = OmnisViolet.copy(alpha = 0.12f)
                    )
                    drawPath(
                        path = pastPath,
                        color = OmnisViolet.copy(alpha = 0.65f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // 5. Polygon Area of Current Values
            val polygonPath = Path()
            val pointCoords = mutableListOf<Offset>()
            for (i in 0 until numPoints) {
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val v = values.getOrElse(i) { 0.5f }.coerceIn(0.05f, 1f)
                val r = radius * v
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                val pt = Offset(x, y)
                pointCoords.add(pt)
                if (i == 0) polygonPath.moveTo(x, y) else polygonPath.lineTo(x, y)
            }
            polygonPath.close()

            // Fill polygon with translucent gradient
            drawPath(
                path = polygonPath,
                color = OmnisCyan.copy(alpha = 0.22f)
            )
            // Stroke polygon boundary
            drawPath(
                path = polygonPath,
                color = OmnisCyan,
                style = Stroke(width = 2.2f)
            )

            // 6. Point Nodes on vertices & Anomaly Detection Halo
            pointCoords.forEachIndexed { i, pt ->
                val nodeColor = dimensionColors.getOrElse(i) { OmnisCyan }
                val currentVal = values.getOrElse(i) { 0.5f }
                val pastVal = pastValues?.getOrNull(i)
                val isAnomaly = pastVal != null && kotlin.math.abs(currentVal - pastVal) >= 0.35f
                val domainName = labels.getOrElse(i) { "" }
                val isFixed = fixedDomains.contains(domainName)

                if (isAnomaly) {
                    // Pulsating warning beacon halo
                    drawCircle(
                        color = OmnisRed.copy(alpha = 0.35f),
                        radius = 8f * pulseScale,
                        center = pt
                    )
                    drawCircle(
                        color = OmnisRed,
                        radius = 6.5f,
                        center = pt,
                        style = Stroke(width = 1.5f)
                    )
                }

                if (isFixed) {
                    // Golden outer ring for fixed axis
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.4f),
                        radius = 8f,
                        center = pt
                    )
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 6.5f,
                        center = pt,
                        style = Stroke(width = 1.5f)
                    )
                }

                drawCircle(
                    color = OmnisBgDark,
                    radius = 5.5f,
                    center = pt
                )
                drawCircle(
                    color = if (isFixed) Color(0xFFFFD700) else if (isAnomaly) OmnisRed else nodeColor,
                    radius = 4f,
                    center = pt
                )
            }
        }
    }
}
