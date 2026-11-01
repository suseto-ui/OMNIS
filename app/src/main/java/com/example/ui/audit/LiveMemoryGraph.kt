package com.example.ui.audit

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitoring.MemorySample
import com.example.ui.theme.*

/**
 * Živý Canvas graf telemetrie paměti JVM Heap RAM.
 * Vykresluje dynamickou křivku v reálném čase, hladinu 75% varování a gradientní výplň.
 */
@Composable
fun LiveMemoryGraph(
    memoryHistory: List<MemorySample>,
    currentSample: MemorySample,
    modifier: Modifier = Modifier
) {
    // Pulzující neonová animace pro aktuální vzorek
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Živý graf paměti JVM Heap. Využito ${currentSample.usedMb} MB z ${currentSample.maxMb} MB (${currentSample.usagePercentage.toInt()} procent)."
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hlavička grafu s metrikami
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
                            .background(OmnisCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "JVM HEAP TELEMETRIE (ŽIVĚ)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Vzorkování 600 ms • Posledních 60 vzorků",
                            fontFamily = FontFamily.Monospace,
                            color = OmnisTextLight,
                            fontSize = 10.sp
                        )
                    }
                }

                // Aktuální vytížení v procentech s dynamickou barvou
                val isCritical = currentSample.usagePercentage >= 75f
                val badgeColor = if (isCritical) Color(0xFFEF4444) else if (currentSample.usagePercentage >= 50f) Color(0xFFF59E0B) else OmnisEmerald

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${currentSample.usedMb} MB / ${currentSample.maxMb} MB (${currentSample.usagePercentage.toInt()}%)",
                        color = badgeColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Vykreslení Křivky
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OmnisBgDark.copy(alpha = 0.7f))
                    .border(1.dp, OmnisBorderDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                    val width = size.width
                    val height = size.height

                    if (width <= 0 || height <= 0) return@Canvas

                    val effectiveMax = if (currentSample.maxMb > 0f) currentSample.maxMb else 256f

                    // 1. Mřížka a vodorovné linie (25%, 50%, 75% výšky)
                    val gridSteps = listOf(0.25f, 0.5f, 0.75f)
                    for (step in gridSteps) {
                        val y = height * (1f - step)
                        drawLine(
                            color = if (step == 0.75f) Color(0xFFEF4444).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = if (step == 0.75f) 1.5f else 1f,
                            pathEffect = if (step == 0.75f) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    }

                    // 2. Křivka paměti
                    if (memoryHistory.isNotEmpty()) {
                        val points = memoryHistory.mapIndexed { index, sample ->
                            val x = if (memoryHistory.size > 1) {
                                (index.toFloat() / (memoryHistory.size - 1)) * width
                            } else {
                                width / 2f
                            }
                            val normalizedY = (sample.usedMb / effectiveMax).coerceIn(0f, 1f)
                            val y = height * (1f - normalizedY)
                            Offset(x, y)
                        }

                        // Path pro gradientní výplň
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    OmnisCyan.copy(alpha = 0.35f),
                                    OmnisEmerald.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Path pro neonovou čáru
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val controlPointX = (p0.x + p1.x) / 2f
                                cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(OmnisCyan.copy(alpha = 0.6f), OmnisCyan, OmnisEmerald)
                            ),
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Poslední aktivní bod s pulzujícím neonovým efektem
                        val lastPoint = points.last()
                        drawCircle(
                            color = OmnisCyan.copy(alpha = pulseAlpha * 0.4f),
                            radius = 9f,
                            center = lastPoint
                        )
                        drawCircle(
                            color = OmnisCyan,
                            radius = 4f,
                            center = lastPoint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legenda a limity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFEF4444), shape = RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Varovná hranice (75% Heap)",
                        color = OmnisTextLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(OmnisCyan, shape = RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Alokováno: ${currentSample.totalMb} MB",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
