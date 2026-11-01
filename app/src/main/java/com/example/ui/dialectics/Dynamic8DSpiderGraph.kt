package com.example.ui.dialectics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dialectics.AgentArchetype
import com.example.dialectics.AgentArenaEngine
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamický Graf Vlivu (8D Spiderweb / Radar Chart)
 * Zobrazuje v reálném čase váhy argumentů a dominanci jednotlivých agentů v 8D matici O.M.N.I.S.
 */
@Composable
fun Dynamic8DInfluenceSpiderGraph(
    agentA: AgentArchetype,
    agentB: AgentArchetype,
    agentAScores: Map<String, Float>,
    agentBScores: Map<String, Float>,
    synthesizedScores: Map<String, Float>? = null,
    activeDomain: String? = null,
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedDomainDetail by remember { mutableStateOf<String?>(null) }

    val colorA = Color(agentA.colorHex)
    val colorB = Color(agentB.colorHex)

    // Calculate dynamic dominance ratio based on total score sums across 8D domains
    val totalScoreA = agentAScores.values.sumOf { (it + 1f).toDouble() }.toFloat().coerceAtLeast(0.1f)
    val totalScoreB = agentBScores.values.sumOf { (it + 1f).toDouble() }.toFloat().coerceAtLeast(0.1f)
    val sum = totalScoreA + totalScoreB
    val dominanceRatioA = (totalScoreA / sum).coerceIn(0.05f, 0.95f)
    val dominanceRatioB = 1.0f - dominanceRatioA

    val dominantAgentName = if (dominanceRatioA >= 0.5f) agentA.name else agentB.name
    val dominantColor = if (dominanceRatioA >= 0.5f) colorA else colorB

    // Animated dominance ratio for real-time smooth bar shift
    val animatedDominanceA by animateFloatAsState(
        targetValue = dominanceRatioA,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DominanceA"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Dynamic Influence Graph",
                        tint = OmnisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DYNAMICKÝ GRAF VLIVU (8D PAVUČINA)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OmnisCyan,
                        fontSize = 13.sp
                    )
                }

                if (isLive) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                    ) {
                        Text(
                            text = "🔴 LIVE UPDATE",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Real-Time Dominance Bar & Indicator
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorA)
                        )
                        Text(
                            text = "${agentA.name}: ${(animatedDominanceA * 100).toInt()}%",
                            color = colorA,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "DOMINUJE: $dominantAgentName",
                        color = dominantColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${agentB.name}: ${((1f - animatedDominanceA) * 100).toInt()}%",
                            color = colorB,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorB)
                        )
                    }
                }

                // Dual Color Progress Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(OmnisBgDark),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(animatedDominanceA.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .background(colorA)
                    )
                    Box(
                        modifier = Modifier
                            .weight((1f - animatedDominanceA).coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .background(colorB)
                    )
                }
            }

            // Spiderweb Canvas Radar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                SpiderwebRadarCanvas(
                    agentA = agentA,
                    agentB = agentB,
                    agentAScores = agentAScores,
                    agentBScores = agentBScores,
                    synthesizedScores = synthesizedScores,
                    activeDomain = activeDomain,
                    onDomainClicked = { domain ->
                        selectedDomainDetail = if (selectedDomainDetail == domain) null else domain
                    }
                )
            }

            // Domain Selector Chips for Deep Dive
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Aktivní domény 8D matice (klikněte pro detail):",
                    color = OmnisTextLight,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(AgentArenaEngine.all8DDomains) { domain ->
                        val isSelected = selectedDomainDetail == domain
                        val isActive = activeDomain == domain
                        val scoreA = agentAScores[domain] ?: 0f
                        val scoreB = agentBScores[domain] ?: 0f

                        val chipBorder = when {
                            isActive -> OmnisCyan
                            isSelected -> Color.White
                            else -> OmnisBorderDark
                        }

                        val chipBg = when {
                            isActive -> OmnisCyan.copy(alpha = 0.25f)
                            isSelected -> OmnisCardDark
                            else -> OmnisBgDark
                        }

                        Surface(
                            onClick = {
                                selectedDomainDetail = if (selectedDomainDetail == domain) null else domain
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = chipBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = domain,
                                    color = if (isActive) OmnisCyan else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "A:${scoreA} B:${scoreB}",
                                    color = OmnisTextLight,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Selected Domain Detail Breakdown
            selectedDomainDetail?.let { domain ->
                val scoreA = agentAScores[domain] ?: 0f
                val scoreB = agentBScores[domain] ?: 0f
                val synthScore = synthesizedScores?.get(domain)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OmnisBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔍 DETAIL DOMÉNY: $domain",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = OmnisCyan,
                                fontSize = 12.sp
                            )
                            IconButton(
                                onClick = { selectedDomainDetail = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("✕", color = OmnisTextLight, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = getDomainDescription(domain),
                            color = OmnisTextLight,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Vliv [${agentA.name}]: $scoreA",
                                color = colorA,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Vliv [${agentB.name}]: $scoreB",
                                color = colorB,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        synthScore?.let { synth ->
                            Text(
                                text = "Epistemická syntéza domény: $synth",
                                color = OmnisEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiderwebRadarCanvas(
    agentA: AgentArchetype,
    agentB: AgentArchetype,
    agentAScores: Map<String, Float>,
    agentBScores: Map<String, Float>,
    synthesizedScores: Map<String, Float>?,
    activeDomain: String?,
    onDomainClicked: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val domains = AgentArenaEngine.all8DDomains
    val numDomains = domains.size

    val colorA = Color(agentA.colorHex)
    val colorB = Color(agentB.colorHex)
    val colorSynth = OmnisCyan

    // Pulsating animation for active domain
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Animated values for smooth vertex morphing
    val animatedScoresA = domains.map { domain ->
        val score = agentAScores[domain] ?: 0f
        animateFloatAsState(
            targetValue = score,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "ScoreA_$domain"
        ).value
    }

    val animatedScoresB = domains.map { domain ->
        val score = agentBScores[domain] ?: 0f
        animateFloatAsState(
            targetValue = score,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "ScoreB_$domain"
        ).value
    }

    val animatedScoresSynth = domains.map { domain ->
        val score = synthesizedScores?.get(domain) ?: 0f
        animateFloatAsState(
            targetValue = score,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "ScoreSynth_$domain"
        ).value
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                // Click interaction handled by chips below for precise tap target
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (minOf(size.width, size.height) / 2f) - 36.dp.toPx()

        val angles = List(numDomains) { i ->
            (i * (2 * Math.PI / numDomains) - Math.PI / 2).toFloat()
        }

        // 1. Draw Concentric 8-sided Grid Polygons (25%, 50%, 75%, 100%)
        val gridRatios = listOf(0.25f, 0.50f, 0.75f, 1.0f)
        gridRatios.forEach { ratio ->
            val gridPath = Path()
            val gridRadius = radius * ratio
            angles.forEachIndexed { i, angle ->
                val x = center.x + gridRadius * cos(angle)
                val y = center.y + gridRadius * sin(angle)
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()

            drawPath(
                path = gridPath,
                color = OmnisBorderDark.copy(alpha = if (ratio == 1.0f) 0.8f else 0.4f),
                style = Stroke(
                    width = if (ratio == 1.0f) 1.5.dp.toPx() else 1.dp.toPx(),
                    pathEffect = if (ratio < 1.0f) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                )
            )
        }

        // 2. Draw Axis Rays from Center & Domain Labels
        angles.forEachIndexed { i, angle ->
            val domain = domains[i]
            val endX = center.x + radius * cos(angle)
            val endY = center.y + radius * sin(angle)

            // Ray Line
            drawLine(
                color = OmnisBorderDark.copy(alpha = 0.5f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )

            // Active Domain Pulse Highlight
            if (activeDomain == domain) {
                val pulseRadius = 12.dp.toPx() * pulseScale
                drawCircle(
                    color = OmnisCyan.copy(alpha = 0.3f),
                    radius = pulseRadius,
                    center = Offset(endX, endY)
                )
                drawCircle(
                    color = OmnisCyan,
                    radius = 4.dp.toPx(),
                    center = Offset(endX, endY)
                )
            }

            // Domain Labels around perimeter
            val labelRadius = radius + 22.dp.toPx()
            val labelX = center.x + labelRadius * cos(angle)
            val labelY = center.y + labelRadius * sin(angle)

            val textLayoutResult = textMeasurer.measure(
                text = domain,
                style = TextStyle(
                    color = if (activeDomain == domain) OmnisCyan else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    labelX - textLayoutResult.size.width / 2f,
                    labelY - textLayoutResult.size.height / 2f
                )
            )
        }

        // Helper to convert score (-1.0 to 1.0) to radius
        fun scoreToRadius(score: Float): Float {
            val normalized = ((score + 1.0f) / 2.0f).coerceIn(0.08f, 1.0f)
            return radius * normalized
        }

        // 3. Draw Agent A Polygon
        val pathA = Path()
        val pointsA = mutableListOf<Offset>()
        angles.forEachIndexed { i, angle ->
            val r = scoreToRadius(animatedScoresA[i])
            val pt = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
            pointsA.add(pt)
            if (i == 0) pathA.moveTo(pt.x, pt.y) else pathA.lineTo(pt.x, pt.y)
        }
        pathA.close()

        drawPath(path = pathA, color = colorA.copy(alpha = 0.25f))
        drawPath(path = pathA, color = colorA, style = Stroke(width = 2.5.dp.toPx()))
        pointsA.forEach { pt ->
            drawCircle(color = colorA, radius = 4.dp.toPx(), center = pt)
        }

        // 4. Draw Agent B Polygon
        val pathB = Path()
        val pointsB = mutableListOf<Offset>()
        angles.forEachIndexed { i, angle ->
            val r = scoreToRadius(animatedScoresB[i])
            val pt = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
            pointsB.add(pt)
            if (i == 0) pathB.moveTo(pt.x, pt.y) else pathB.lineTo(pt.x, pt.y)
        }
        pathB.close()

        drawPath(path = pathB, color = colorB.copy(alpha = 0.25f))
        drawPath(path = pathB, color = colorB, style = Stroke(width = 2.5.dp.toPx()))
        pointsB.forEach { pt ->
            drawCircle(color = colorB, radius = 4.dp.toPx(), center = pt)
        }

        // 5. Draw Synthesis Polygon (if available)
        if (synthesizedScores != null) {
            val pathSynth = Path()
            val pointsSynth = mutableListOf<Offset>()
            angles.forEachIndexed { i, angle ->
                val r = scoreToRadius(animatedScoresSynth[i])
                val pt = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
                pointsSynth.add(pt)
                if (i == 0) pathSynth.moveTo(pt.x, pt.y) else pathSynth.lineTo(pt.x, pt.y)
            }
            pathSynth.close()

            drawPath(
                path = pathSynth,
                color = colorSynth,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
            pointsSynth.forEach { pt ->
                drawCircle(color = colorSynth, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}

private fun getDomainDescription(domain: String): String {
    return when (domain) {
        "SYS" -> "Systémová architektura, tenzorová propustnost a exekuční stabilita."
        "ECON" -> "Ekonomická efektivita, nákladová alokace a výpočetní ROI."
        "PSYCH" -> "Kognitivní zátěž, uživatelská důvěra a lidský faktor."
        "ECO" -> "Udržitelnost zdrojů, energetická náročnost a ekologická stopa."
        "LAW" -> "Normativní shoda, regulatorní brány (EU AI Act, ISO/IEC) a právní certifikace."
        "SEC" -> "Kybernetická bezpečnost, odolnost proti útokům a NIS2 standardy."
        "PHYS" -> "Fyzická výpočetní infrastruktura, paměťový výkon CSR matice a latence."
        "SOC" -> "Společenský dopad, etická integrita a distribuovaný konsenzus."
        else -> "Vlivová dimenze 8D matice O.M.N.I.S."
    }
}
