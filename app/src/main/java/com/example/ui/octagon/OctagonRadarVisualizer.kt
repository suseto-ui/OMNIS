package com.example.ui.octagon

import android.graphics.Paint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.ui.OmnisCorrelationEngine
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OctagonRadarVisualizer(
    values: List<Float>,
    modifier: Modifier = Modifier,
    pastValues: List<Float>? = null,
    fixedDomains: Set<String> = emptySet(),
    onToggleFix: ((String) -> Unit)? = null,
    selectedDimension: String? = null,
    onSelectDimension: ((String) -> Unit)? = null
) {
    val labels = remember { listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc") }
    var hoveredDimension by remember { mutableStateOf<String?>(null) }
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
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Exit) {
                        hoveredDimension = null
                        continue
                    }
                    val changes = event.changes
                    val activeChange = changes.firstOrNull()
                    
                    if (activeChange != null && activeChange.pressed) {
                        val position = activeChange.position
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (minOf(size.width, size.height) / 2f) * 0.58f
                        val numPoints = 8
                        val angleStep = (2.0 * Math.PI / numPoints).toFloat()

                        var foundHover: String? = null
                        for (i in 0 until numPoints) {
                            val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                            val v = values.getOrElse(i) { 0.5f }.coerceIn(0.05f, 1f)
                            val nodePt = Offset(center.x + (radius * v) * cos(angle), center.y + (radius * v) * sin(angle))
                            val labelRadius = radius + 22f
                            val labelPt = Offset(center.x + labelRadius * cos(angle), center.y + labelRadius * sin(angle))

                            val distToNode = kotlin.math.hypot(position.x - nodePt.x, position.y - nodePt.y)
                            val distToLabel = kotlin.math.hypot(position.x - labelPt.x, position.y - labelPt.y)

                            if (distToNode <= 45f || distToLabel <= 45f) {
                                foundHover = labels[i]
                                break
                            }
                        }

                        hoveredDimension = foundHover

                        // Handle click/tap triggers
                        val isTap = activeChange.pressed && !activeChange.previousPressed
                        if (isTap && foundHover != null) {
                            if (onSelectDimension != null) {
                                onSelectDimension.invoke(foundHover)
                            } else {
                                onToggleFix?.invoke(foundHover)
                            }
                        }
                    } else if (activeChange != null && !activeChange.pressed) {
                        // Track hover when mouse moves without click
                        val position = activeChange.position
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (minOf(size.width, size.height) / 2f) * 0.58f
                        val numPoints = 8
                        val angleStep = (2.0 * Math.PI / numPoints).toFloat()

                        var foundHover: String? = null
                        for (i in 0 until numPoints) {
                            val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                            val v = values.getOrElse(i) { 0.5f }.coerceIn(0.05f, 1f)
                            val nodePt = Offset(center.x + (radius * v) * cos(angle), center.y + (radius * v) * sin(angle))
                            val labelRadius = radius + 22f
                            val labelPt = Offset(center.x + labelRadius * cos(angle), center.y + labelRadius * sin(angle))

                            val distToNode = kotlin.math.hypot(position.x - nodePt.x, position.y - nodePt.y)
                            val distToLabel = kotlin.math.hypot(position.x - labelPt.x, position.y - labelPt.y)

                            if (distToNode <= 45f || distToLabel <= 45f) {
                                foundHover = labels[i]
                                break
                            }
                        }
                        hoveredDimension = foundHover
                    } else {
                        hoveredDimension = null
                    }
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("octagon_radar_canvas")) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.58f
            val numPoints = 8
            val angleStep = (2.0 * Math.PI / numPoints).toFloat()

            // 1. Concentric web rings
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

                val labelRadius = radius + 22f
                val lx = center.x + labelRadius * cos(angle)
                val ly = center.y + labelRadius * sin(angle) + 10f
                val domainName = labels[i]
                val isFixed = fixedDomains.contains(domainName)
                val isSelected = domainName == selectedDimension
                val isHovered = domainName == hoveredDimension
                val isHighlighted = isSelected || isHovered

                if (isHighlighted) {
                    drawCircle(
                        color = if (isHovered && !isSelected) OmnisEmerald.copy(alpha = 0.28f) else OmnisCyan.copy(alpha = 0.22f),
                        radius = 28f * pulseScale,
                        center = Offset(lx, ly - 8f)
                    )
                }

                textPaint.color = if (isSelected) {
                    android.graphics.Color.CYAN
                } else if (isHovered) {
                    android.graphics.Color.rgb(52, 211, 153)
                } else if (isFixed) {
                    android.graphics.Color.rgb(255, 215, 0)
                } else {
                    val c = dimensionColors[i]
                    android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
                }

                textPaint.isUnderlineText = isHighlighted

                drawContext.canvas.nativeCanvas.drawText(
                    if (isFixed) "$domainName*" else domainName,
                    lx,
                    ly,
                    textPaint
                )
            }

            // 3. Dynamic Tension / Synergy Correlation cross-lines
            val domainKeys = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")
            for (i in domainKeys.indices) {
                for (j in i + 1 until domainKeys.size) {
                    val pairCorr = OmnisCorrelationEngine.getCorrelation(domainKeys[i], domainKeys[j])
                    val corr = pairCorr.correlation
                    if (kotlin.math.abs(corr) >= 0.45f) {
                        val valA = values.getOrElse(i) { 0.5f }.coerceIn(0.1f, 1f)
                        val valB = values.getOrElse(j) { 0.5f }.coerceIn(0.1f, 1f)

                        val angleA = i * angleStep - (Math.PI / 2.0).toFloat()
                        val angleB = j * angleStep - (Math.PI / 2.0).toFloat()

                        val ptA = Offset(center.x + (radius * valA) * cos(angleA), center.y + (radius * valA) * sin(angleA))
                        val ptB = Offset(center.x + (radius * valB) * cos(angleB), center.y + (radius * valB) * sin(angleB))

                        val isFriction = corr < 0f
                        val lineColor = if (isFriction) OmnisAmber.copy(alpha = 0.45f) else OmnisCyan.copy(alpha = 0.35f)
                        val strokeW = if (kotlin.math.abs(corr) >= 0.70f) 1.6f else 1.0f

                        drawLine(
                            color = lineColor,
                            start = ptA,
                            end = ptB,
                            strokeWidth = strokeW
                        )
                    }
                }
            }

            // 4. Past Record Ghost Polygon
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

            drawPath(
                path = polygonPath,
                color = OmnisCyan.copy(alpha = 0.22f)
            )
            drawPath(
                path = polygonPath,
                color = OmnisCyan,
                style = Stroke(width = 2.2f)
            )

            // 6. Point Nodes on vertices
            pointCoords.forEachIndexed { i, pt ->
                val nodeColor = dimensionColors.getOrElse(i) { OmnisCyan }
                val currentVal = values.getOrElse(i) { 0.5f }
                val pastVal = pastValues?.getOrNull(i)
                val isAnomaly = pastVal != null && kotlin.math.abs(currentVal - pastVal) >= 0.35f
                val domainName = labels.getOrElse(i) { "" }
                val isFixed = fixedDomains.contains(domainName)
                val isSelected = domainName == selectedDimension
                val isHovered = domainName == hoveredDimension
                val isHighlighted = isSelected || isHovered

                if (isHighlighted) {
                    drawCircle(
                        color = if (isHovered && !isSelected) OmnisEmerald.copy(alpha = 0.4f) else OmnisCyan.copy(alpha = 0.35f),
                        radius = 12f * pulseScale,
                        center = pt
                    )
                    drawCircle(
                        color = if (isHovered && !isSelected) OmnisEmerald else OmnisCyan,
                        radius = 8f,
                        center = pt,
                        style = Stroke(width = 1.8f)
                    )
                }

                if (isAnomaly) {
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

            // 7. Interactive Floating Tooltip (Recharts-like)
            val activeTooltipDim = hoveredDimension ?: selectedDimension
            if (activeTooltipDim != null) {
                val dimIndex = labels.indexOf(activeTooltipDim)
                if (dimIndex != -1) {
                    val dimColor = dimensionColors.getOrElse(dimIndex) { OmnisCyan }
                    val dimValue = values.getOrElse(dimIndex) { 0.5f }
                    val dimDef = OMNIS_8D_DIMENSIONS.find { it.key.equals(activeTooltipDim, ignoreCase = true) }
                    val dimFullName = dimDef?.name ?: activeTooltipDim
                    val dimDesc = dimDef?.desc ?: "Systémová metrika 8D matice"

                    // Background Card geometry
                    val tooltipWidth = 340f
                    val tooltipHeight = 120f
                    val tx = center.x - tooltipWidth / 2f
                    val ty = size.height - tooltipHeight - 10f

                    // Draw filled background card
                    drawRoundRect(
                        color = OmnisPanelDark.copy(alpha = 0.96f),
                        topLeft = Offset(tx, ty),
                        size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    // Draw outer border matched to domain color
                    drawRoundRect(
                        color = if (activeTooltipDim == hoveredDimension) OmnisEmerald else dimColor.copy(alpha = 0.85f),
                        topLeft = Offset(tx, ty),
                        size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                        style = Stroke(width = 2.5f)
                    )

                    // Text styling paint
                    val tooltipPaint = Paint().apply {
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    }

                    // Line 1: [Sys] Systémové inž.: 75%
                    tooltipPaint.color = android.graphics.Color.WHITE
                    tooltipPaint.textSize = 21f
                    tooltipPaint.textAlign = Paint.Align.LEFT
                    drawContext.canvas.nativeCanvas.drawText(
                        "[$activeTooltipDim] $dimFullName: ${(dimValue * 100).toInt()}%",
                        tx + 16f,
                        ty + 28f,
                        tooltipPaint
                    )

                    // Line 2: Definice
                    tooltipPaint.color = android.graphics.Color.rgb(180, 220, 235)
                    tooltipPaint.textSize = 17f
                    tooltipPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                    drawContext.canvas.nativeCanvas.drawText(
                        "Definice: " + dimDesc.take(30),
                        tx + 16f,
                        ty + 55f,
                        tooltipPaint
                    )

                    // Line 3: Koeficient dopadu
                    val formattedVal = String.format(java.util.Locale.US, "%.2f", dimValue)
                    tooltipPaint.color = if (activeTooltipDim == hoveredDimension) android.graphics.Color.rgb(52, 211, 153) else android.graphics.Color.rgb(96, 165, 250)
                    tooltipPaint.textSize = 17f
                    tooltipPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    drawContext.canvas.nativeCanvas.drawText(
                        "Koeficient dopadu: " + formattedVal + if (activeTooltipDim == hoveredDimension) " [HOVER]" else " [AKTIVNÍ]",
                        tx + 16f,
                        ty + 82f,
                        tooltipPaint
                    )

                    // Line 4: Action hint
                    tooltipPaint.color = android.graphics.Color.rgb(120, 155, 175)
                    tooltipPaint.textSize = 14f
                    tooltipPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                    drawContext.canvas.nativeCanvas.drawText(
                        "• Kliknutím zvolíte / uzamknete doménu",
                        tx + 16f,
                        ty + 107f,
                        tooltipPaint
                    )
                }
            }
        }
    }
}
