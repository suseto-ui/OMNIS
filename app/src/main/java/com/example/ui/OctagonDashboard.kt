package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onClearComparison: () -> Unit
) {
    // Multi-selection hook for 8D elements
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val assistantRecords = remember(records) { records.filter { it.role == "assistant" } }
    
    val composite = (simSys + simEcon + simPsych + simEco + simLaw + simSec + simPhys + simSoc) / 8f
    val animatedComposite by animateFloatAsState(targetValue = composite, label = "composite")

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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    }
                }
            }

            // Interactive What-If Sliders
            item {
                Text(
                    text = "8D CO-KDYŽ SIMULACE & FIXACE DOMÉN",
                    color = OmnisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
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
                    onValueChange = { onSimChange(simSys, simEcon, it, simEco, simLaw, simSec, simPhys, simSoc) }
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
                    onValueChange = { onSimChange(simSys, simEcon, simPsych, it, simLaw, simSec, simPhys, simSoc) }
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
                    onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, it, simSec, simPhys, simSoc) }
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
                    onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, it, simPhys, simSoc) }
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
                    onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, it, simSoc) }
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
                    onValueChange = { onSimChange(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, it) }
                )
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
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with element tags
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOMÉNA",
                    color = OmnisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(100.dp)
                )
                selectedRecords.forEach { rec ->
                    Surface(
                        color = OmnisPanelDark,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.width(70.dp)
                    ) {
                        Text(
                            text = "#${rec.id}",
                            color = OmnisCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(4.dp),
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
                    modifier = Modifier.width(55.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.width(100.dp),
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
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    selectedRecords.forEach { rec ->
                        val v = dim.getter(rec)
                        Text(
                            text = String.format("%.2f", v),
                            color = dim.color,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(70.dp)
                        )
                    }

                    Text(
                        text = if (delta > 0.001f) "±" + String.format("%.2f", delta) else "0.00",
                        color = if (delta > 0.25f) OmnisAmber else OmnisEmerald,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(55.dp)
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
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFixed) color else OmnisBorderDark),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onToggleFix, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isFixed) Icons.Default.Lock else icon, 
                            contentDescription = null, 
                            tint = color, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = desc,
                color = OmnisTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = OmnisBorderDark
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
