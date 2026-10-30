package com.example.ui.octagon

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TopologicalInferenceEngine
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

data class DimensionDetail(
    val code: String,
    val nameCs: String,
    val nameEn: String,
    val value: Float,
    val color: Color,
    val plainSummaryCs: String,
    val plainSummaryEn: String,
    val expertVector: String
)

/**
 * Adaptivní Oktagon Dashboard (8D matice s postupným rozbalováním dimenzí).
 * Umožňuje běžným uživatelům sledovat celkový stav v čistém 8-úhelníku,
 * a na jedno klepnutí si postupně rozbalovat jednotlivé dimenze a telemetrii.
 */
@Composable
fun AdaptiveOctagonDashboard(
    values: Map<String, Float>,
    systemStressSigma: Float = 0.18f,
    isCompact: Boolean = false,
    onDimensionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedDimension by remember { mutableStateOf<String?>(null) }
    var expandAll by remember { mutableStateOf(false) }

    val dimensions = remember(values) {
        listOf(
            DimensionDetail(
                code = "SYS",
                nameCs = "Systémová architektura",
                nameEn = "System Architecture",
                value = (values["SYS"] ?: 0.75f).coerceIn(0f, 1f),
                color = Color(0xFF60A5FA),
                plainSummaryCs = "Stabilita mikroslužeb, nízká latence a spolehlivost běhu.",
                plainSummaryEn = "Microservice stability, low latency, runtime reliability.",
                expertVector = "T_sys = [v_core: 0.88, mem_res: 0.92, io_bound: 0.12]"
            ),
            DimensionDetail(
                code = "ECON",
                nameCs = "Ekonomika a náklady",
                nameEn = "Economic & Cost",
                value = (values["ECON"] ?: 0.68f).coerceIn(0f, 1f),
                color = Color(0xFFFBBF24),
                plainSummaryCs = "Efektivní využití tokenů a optimalizace výpočetního rozpočtu.",
                plainSummaryEn = "Token efficiency and computation budget optimization.",
                expertVector = "T_econ = [token_burn: 0.24, cost_eff: 0.86, roi_index: 0.79]"
            ),
            DimensionDetail(
                code = "PSYCH",
                nameCs = "Kognice a ergonomie",
                nameEn = "Cognitive Ergonomics",
                value = (values["PSYCH"] ?: 0.82f).coerceIn(0f, 1f),
                color = Color(0xFFC084FC),
                plainSummaryCs = "Srozumitelnost odpovědí, přívětivost a minimální mentální zátěž.",
                plainSummaryEn = "Response clarity, friendliness, minimal mental friction.",
                expertVector = "T_psych = [clarity: 0.94, cognitive_load: 0.18, empathy: 0.85]"
            ),
            DimensionDetail(
                code = "ECO",
                nameCs = "Udržitelnost a efektivita",
                nameEn = "Ecological Efficiency",
                value = (values["ECO"] ?: 0.70f).coerceIn(0f, 1f),
                color = Color(0xFF34D399),
                plainSummaryCs = "Energetická šetrnost a úspornost inferenčních procesů.",
                plainSummaryEn = "Green inference efficiency and low carbon footprint.",
                expertVector = "T_eco = [energy_eff: 0.78, pflops_watt: 0.82, cache_hit: 0.91]"
            ),
            DimensionDetail(
                code = "LAW",
                nameCs = "Právní a normativní soulad",
                nameEn = "Legal & Compliance",
                value = (values["LAW"] ?: 0.88f).coerceIn(0f, 1f),
                color = Color(0xFFFB7185),
                plainSummaryCs = "Soulad s EU AI Act, NIS2, ISO/IEC a regulatorními rámci.",
                plainSummaryEn = "Compliance with EU AI Act, NIS2, ISO/IEC standards.",
                expertVector = "T_law = [eu_ai_act: 0.98, nis2_audit: 0.95, gdpr_safe: 1.00]"
            ),
            DimensionDetail(
                code = "SEC",
                nameCs = "Kybernetická bezpečnost",
                nameEn = "Cyber Security",
                value = (values["SEC"] ?: 0.91f).coerceIn(0f, 1f),
                color = Color(0xFFEF4444),
                plainSummaryCs = "Ochrana před útoky, prompt injection a neautorizovaným přístupem.",
                plainSummaryEn = "Defense against prompt injection and unauthorized exfiltration.",
                expertVector = "T_sec = [injection_resist: 0.99, zero_trust: 0.94, zk_hash: 1.00]"
            ),
            DimensionDetail(
                code = "PHYS",
                nameCs = "Fyzikální a HW stabilita",
                nameEn = "Physical Hardware",
                value = (values["PHYS"] ?: 0.65f).coerceIn(0f, 1f),
                color = Color(0xFFFB923C),
                plainSummaryCs = "Teplotní a hardwarová zátěž procesorů a paměťových řadičů.",
                plainSummaryEn = "Thermal, memory controller and silicon hardware health.",
                expertVector = "T_phys = [thermal_margin: 0.72, bus_latency: 0.68, clock_stab: 0.89]"
            ),
            DimensionDetail(
                code = "SOC",
                nameCs = "Společenský kontext",
                nameEn = "Social & Alignment",
                value = (values["SOC"] ?: 0.77f).coerceIn(0f, 1f),
                color = Color(0xFFF472B6),
                plainSummaryCs = "Etické ukotvení výstupů a bezpečnost pro lidské uživatele.",
                plainSummaryEn = "Ethical alignment and holistic societal safety.",
                expertVector = "T_soc = [bias_mitigation: 0.91, safety_guard: 0.96, trust: 0.84]"
            )
        )
    }

    // Výpočet perkolačního stavu a tokenového rozpočtu
    val domainsStatus = remember(dimensions) {
        dimensions.associate { dim ->
            val status = when {
                dim.value < 0.40f -> "FAILED"
                dim.value < 0.65f -> "STRESSED"
                else -> "OPTIMAL"
            }
            dim.code to status
        }
    }

    val percolation = remember(domainsStatus) {
        TopologicalInferenceEngine.evalCascadeRisk(domainsStatus)
    }

    val tokenBudget = remember(systemStressSigma) {
        TopologicalInferenceEngine.calculateTokenBudget(systemStressSigma)
    }

    val avgIntegrity = remember(dimensions) {
        dimensions.map { it.value }.average().toFloat()
    }

    Surface(
        color = OmnisPanelDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (percolation.isCascadeRiskActive) Color.Red else OmnisBorderDark),
        modifier = modifier.fillMaxWidth().testTag("adaptive_octagon_dashboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hlavička s celkovým stavem a přepínačem rozbalení
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OmnisCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "8D OKTAGON DASHBOARD",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Celková systémová integrita: ${String.format("%.1f", avgIntegrity * 100)}%",
                            color = if (avgIntegrity >= 0.70f) OmnisEmerald else OmnisAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                TextButton(
                    onClick = { expandAll = !expandAll },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (expandAll) "Sbalit vše" else "Rozbalit vše",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Středový interaktivní Oktagon Canvas (Radar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 170.dp else 220.dp),
                contentAlignment = Alignment.Center
            ) {
                OctagonCanvas(
                    dimensions = dimensions,
                    modifier = Modifier.fillMaxSize(),
                    onSelectCode = { code ->
                        expandedDimension = if (expandedDimension == code) null else code
                        onDimensionClick?.invoke(code)
                    }
                )

                // Středový štítek integrity
                Surface(
                    color = OmnisBgDark.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.size(if (isCompact) 48.dp else 60.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${(avgIntegrity * 100).toInt()}%",
                            color = OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 12.sp else 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "8D",
                            color = OmnisTextMuted,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Živý telemetrický pás (Stres σ, Perkolace pc, Komprese tokenů)
            Surface(
                color = OmnisBgDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stres σ
                    Column {
                        Text(
                            text = "STRES (σ)",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format("%.2f", systemStressSigma),
                            color = if (systemStressSigma < 0.2f) OmnisEmerald else if (systemStressSigma < 0.6f) OmnisAmber else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Perkolace
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PERKOLACE (pc=38%)",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (percolation.isCascadeRiskActive) "KASKÁDA !" else "STABILNÍ",
                            color = if (percolation.isCascadeRiskActive) Color.Red else OmnisEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Token komprese
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOKEN TOK",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (tokenBudget.compressionRate > 0) "${(tokenBudget.compressionRate * 100).toInt()}% Komprese" else "FULL DETAIL",
                            color = OmnisCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Rychlé tlačítka pro rozbalení jednotlivých 8 dimenzí
            Text(
                text = "Klepnutím na dimenzi rozbalíte detailní rozbor:",
                color = OmnisTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dimensions.forEach { dim ->
                    val isOpened = expandAll || expandedDimension == dim.code
                    Surface(
                        onClick = {
                            expandedDimension = if (expandedDimension == dim.code) null else dim.code
                            onDimensionClick?.invoke(dim.code)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isOpened) dim.color.copy(alpha = 0.25f) else OmnisBgDark,
                        border = BorderStroke(1.dp, if (isOpened) dim.color else OmnisBorderDark),
                        modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dim.code,
                                color = if (isOpened) dim.color else Color.White,
                                fontWeight = if (isOpened) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Postupně rozbalené detaily vybraných dimenzí
            dimensions.forEach { dim ->
                val isVisible = expandAll || expandedDimension == dim.code
                AnimatedVisibility(
                    visible = isVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DimensionDetailCard(dimension = dim)
                }
            }
        }
    }
}

@Composable
private fun DimensionDetailCard(dimension: DimensionDetail) {
    Surface(
        color = OmnisBgDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, dimension.color.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dimension.color)
                    )
                    Text(
                        text = "${dimension.code} • ${dimension.nameCs}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = dimension.color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${(dimension.value * 100).toInt()}%",
                        color = dimension.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Vizuální progress bar
            LinearProgressIndicator(
                progress = { dimension.value },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = dimension.color,
                trackColor = OmnisBorderDark
            )

            // Lidské vysvětlení pro běžné uživatele
            Text(
                text = dimension.plainSummaryCs,
                color = OmnisTextLight,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            // Tenzorový rozpad pro experty
            Text(
                text = dimension.expertVector,
                color = OmnisTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun OctagonCanvas(
    dimensions: List<DimensionDetail>,
    modifier: Modifier = Modifier,
    onSelectCode: (String) -> Unit
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY) * 0.82f

        val sides = 8
        val angleStep = (2 * Math.PI) / sides

        // Soustředné polygonové mřížky (0.25, 0.50, 0.75, 1.0)
        listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { scale ->
            val gridPath = Path()
            for (i in 0 until sides) {
                val angle = i * angleStep - Math.PI / 2
                val x = centerX + (radius * scale * cos(angle)).toFloat()
                val y = centerY + (radius * scale * sin(angle)).toFloat()
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()
            drawPath(
                path = gridPath,
                color = OmnisBorderDark.copy(alpha = if (scale == 1.0f) 0.6f else 0.3f),
                style = Stroke(width = 1f)
            )
        }

        // Paprsky z centra k vrcholům
        for (i in 0 until sides) {
            val angle = i * angleStep - Math.PI / 2
            val endX = centerX + (radius * cos(angle)).toFloat()
            val endY = centerY + (radius * sin(angle)).toFloat()
            drawLine(
                color = OmnisBorderDark.copy(alpha = 0.4f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        // Datový polygon aktuálních hodnot
        val dataPath = Path()
        dimensions.forEachIndexed { i, dim ->
            val angle = i * angleStep - Math.PI / 2
            val valRadius = radius * dim.value.coerceIn(0.1f, 1.0f)
            val x = centerX + (valRadius * cos(angle)).toFloat()
            val y = centerY + (valRadius * sin(angle)).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        // Výplň polygonu s poloprůhledným gradientem
        drawPath(
            path = dataPath,
            brush = Brush.radialGradient(
                colors = listOf(OmnisCyan.copy(alpha = 0.35f), OmnisAmber.copy(alpha = 0.15f)),
                center = Offset(centerX, centerY),
                radius = radius
            )
        )

        // Obrys datového polygonu
        drawPath(
            path = dataPath,
            color = OmnisCyan,
            style = Stroke(width = 2.dp.toPx())
        )

        // Vrcholy a body
        dimensions.forEachIndexed { i, dim ->
            val angle = i * angleStep - Math.PI / 2
            val valRadius = radius * dim.value.coerceIn(0.1f, 1.0f)
            val vx = centerX + (valRadius * cos(angle)).toFloat()
            val vy = centerY + (valRadius * sin(angle)).toFloat()

            // Bod na hodnotě
            drawCircle(
                color = dim.color,
                radius = 4.dp.toPx(),
                center = Offset(vx, vy)
            )
        }
    }
}
