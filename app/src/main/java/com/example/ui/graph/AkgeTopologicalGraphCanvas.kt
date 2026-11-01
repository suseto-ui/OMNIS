package com.example.ui.graph

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AkgeTensorKernel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val GraphCyan = Color(0xFF00E5FF)
private val GraphGreen = Color(0xFF10B981)
private val GraphAmber = Color(0xFFF59E0B)
private val GraphRed = Color(0xFFEF4444)
private val GraphBg = Color(0xFF080D1A)
private val GraphCardBg = Color(0xFF111927)
private val GraphBorder = Color(0xFF1F293D)

data class DomainNodeInfo(
    val code: String,
    val name: String,
    val description: String,
    val color: Color
)

val DOMAIN_DEFINITIONS = listOf(
    DomainNodeInfo("SYS", "Systém", "Architektura a kognitivní stabilita", Color(0xFF38BDF8)),
    DomainNodeInfo("ECON", "Ekonomie", "Nákladová efektivita a tokenomika", Color(0xFF34D399)),
    DomainNodeInfo("PSYCH", "Psychologie", "Ergonomie a kognitivní zátěž", Color(0xFFA78BFA)),
    DomainNodeInfo("ECO", "Ekologie", "Udržitelnost a environmentální stopa", Color(0xFF4ADE80)),
    DomainNodeInfo("LAW", "Právo", "Normativní a regulatorní soulad NIS2", Color(0xFFFBBF24)),
    DomainNodeInfo("SEC", "Bezpečnost", "Kybernetická odolnost a ZK-SNARK", Color(0xFFF87171)),
    DomainNodeInfo("PHYS", "Fyzika", "Výpočetní latence a HW optimalizace", Color(0xFFFB923C)),
    DomainNodeInfo("SOC", "Společnost", "Sociální dopad a transdisciplinární etika", Color(0xFFE879F9))
)

@Composable
fun AkgeTopologicalGraphCanvas(
    modifier: Modifier = Modifier,
    onNavigateToDialectics: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val kernelSnapshot by AkgeTensorKernel.kernelState.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    var selectedDomainCode by remember { mutableStateOf<String?>(null) }

    val domainMap = remember { DOMAIN_DEFINITIONS.associateBy { it.code } }
    val nodeRadiusPx = with(density) { 22.dp.toPx() }
    val touchHitRadiusPx = with(density) { 36.dp.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, if (kernelSnapshot.percolationStatus.isCascadeRiskActive) GraphRed else GraphCyan.copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
            .testTag("akge_topological_graph_canvas"),
        color = GraphCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
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
                            .background(if (kernelSnapshot.percolationStatus.isCascadeRiskActive) GraphRed else GraphCyan)
                    )
                    Column {
                        Text(
                            text = "AKGE-8D TOPOLOGICKÝ GRAF",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Řídká matice vazeb (CSR) & detekce cyklů",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GraphBg,
                    border = BorderStroke(0.5.dp, GraphCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "V:8 E:${kernelSnapshot.csrMatrix.values.size / 2} β1:${kernelSnapshot.topologicalMetrics.betti1}",
                        fontSize = 9.5.sp,
                        color = GraphCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Interactive Canvas
            val csr = kernelSnapshot.csrMatrix
            val domainWeights = kernelSnapshot.domainWeights
            val domainKeys = AkgeTensorKernel.DOMAIN_KEYS

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GraphBg)
                    .pointerInput(domainKeys) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = (minOf(size.width, size.height) / 2f) * 0.74f

                            var tappedDomain: String? = null
                            for (i in domainKeys.indices) {
                                val angle = (i * 2.0 * PI / domainKeys.size) - (PI / 2.0)
                                val nx = cx + radius * cos(angle).toFloat()
                                val ny = cy + radius * sin(angle).toFloat()
                                val dist = sqrt((offset.x - nx) * (offset.x - nx) + (offset.y - ny) * (offset.y - ny))
                                if (dist <= touchHitRadiusPx) {
                                    tappedDomain = domainKeys[i]
                                    break
                                }
                            }

                            if (tappedDomain != null) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                selectedDomainCode = if (selectedDomainCode == tappedDomain) null else tappedDomain
                            } else {
                                selectedDomainCode = null
                            }
                        }
                    }
                    .testTag("interactive_graph_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = (minOf(size.width, size.height) / 2f) * 0.74f

                    // Calculate node positions
                    val nodePositions = domainKeys.indices.map { i ->
                        val angle = (i * 2.0 * PI / domainKeys.size) - (PI / 2.0)
                        Offset(
                            x = cx + radius * cos(angle).toFloat(),
                            y = cy + radius * sin(angle).toFloat()
                        )
                    }

                    val selectedIdx = selectedDomainCode?.let { domainKeys.indexOf(it) }?.takeIf { it >= 0 }

                    // 1. Draw Edges from CSR Matrix
                    for (i in 0 until csr.dimension) {
                        val start = csr.rowPointers[i]
                        val end = csr.rowPointers[i + 1]
                        val p1 = nodePositions[i]

                        for (k in start until end) {
                            val j = csr.columnIndices[k]
                            // Only draw each undirected edge once
                            if (i < j) {
                                val p2 = nodePositions[j]
                                val weight = csr.values[k]

                                val isConnectedToSelected = selectedIdx != null && (i == selectedIdx || j == selectedIdx)
                                val isDimmed = selectedIdx != null && !isConnectedToSelected

                                val edgeColor = when {
                                    isConnectedToSelected -> GraphCyan
                                    isDimmed -> GraphBorder.copy(alpha = 0.25f)
                                    else -> GraphCyan.copy(alpha = (weight * 0.5f).coerceIn(0.15f, 0.65f))
                                }

                                val strokeWidth = when {
                                    isConnectedToSelected -> 3.5.dp.toPx()
                                    isDimmed -> 1.dp.toPx()
                                    else -> (1.2f + weight * 1.8f).dp.toPx()
                                }

                                drawLine(
                                    color = edgeColor,
                                    start = p1,
                                    end = p2,
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }

                    // 2. Draw Nodes
                    nodePositions.forEachIndexed { i, pos ->
                        val code = domainKeys[i]
                        val weight = domainWeights[code] ?: 0.5f
                        val isSelected = selectedIdx == i
                        val isStressed = weight < 0.5f

                        val nodeColor = when {
                            isStressed -> GraphRed
                            weight >= 0.75f -> GraphGreen
                            else -> GraphAmber
                        }

                        // Outer glowing halo
                        if (isSelected || isStressed) {
                            drawCircle(
                                color = if (isSelected) GraphCyan.copy(alpha = 0.35f) else GraphRed.copy(alpha = 0.25f),
                                radius = nodeRadiusPx + if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                                center = pos
                            )
                        }

                        // Node base circle
                        drawCircle(
                            color = GraphCardBg,
                            radius = nodeRadiusPx,
                            center = pos
                        )

                        // Node outline ring
                        drawCircle(
                            color = if (isSelected) GraphCyan else nodeColor,
                            radius = nodeRadiusPx,
                            center = pos,
                            style = Stroke(width = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
                        )

                        // Native canvas text rendering for domain label
                        val paint = android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                        val weightPaint = android.graphics.Paint().apply {
                            color = if (isStressed) android.graphics.Color.RED else android.graphics.Color.CYAN
                            textSize = 8.5.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.MONOSPACE
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            code,
                            pos.x,
                            pos.y - 2.dp.toPx(),
                            paint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.2f", weight),
                            pos.x,
                            pos.y + 11.dp.toPx(),
                            weightPaint
                        )
                    }
                }

                // Interactive Hint Overlay
                if (selectedDomainCode == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GraphCardBg.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Klepnutím na uzel zobrazíte detaily vazeb",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Selected Node Inspector Card
            AnimatedVisibility(visible = selectedDomainCode != null) {
                selectedDomainCode?.let { code ->
                    val info = domainMap[code]
                    val weight = domainWeights[code] ?: 0.5f

                    // Get connected edges
                    val domainIdx = domainKeys.indexOf(code)
                    val connections = mutableListOf<Pair<String, Float>>()
                    if (domainIdx >= 0) {
                        val start = csr.rowPointers[domainIdx]
                        val end = csr.rowPointers[domainIdx + 1]
                        for (k in start until end) {
                            val targetIdx = csr.columnIndices[k]
                            connections.add(Pair(domainKeys[targetIdx], csr.values[k]))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphBg)
                            .border(BorderStroke(1.dp, GraphCyan.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Adjust, contentDescription = null, tint = GraphCyan, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = "${info?.name ?: code} ($code)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = info?.description ?: "",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (weight < 0.5f) GraphRed.copy(alpha = 0.2f) else GraphGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, if (weight < 0.5f) GraphRed else GraphGreen)
                            ) {
                                Text(
                                    text = "Váha: ${String.format("%.2f", weight)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (weight < 0.5f) GraphRed else GraphGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Aktivní vazby v CSR grafu (${connections.size}):",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            connections.forEach { conn ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GraphCardBg,
                                    border = BorderStroke(0.5.dp, GraphBorder)
                                ) {
                                    Text(
                                        text = "${conn.first} (${String.format("%.2f", conn.second)})",
                                        fontSize = 8.5.sp,
                                        color = GraphCyan,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions for selected domain
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    AkgeTensorKernel.updateDomainWeight(code, (weight + 0.10f).coerceAtMost(1.0f))
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GraphGreen),
                                border = BorderStroke(0.5.dp, GraphGreen.copy(alpha = 0.7f))
                            ) {
                                Text("+0.10 Zesílit", fontSize = 9.5.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    AkgeTensorKernel.updateDomainWeight(code, (weight - 0.10f).coerceAtLeast(0.05f))
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GraphAmber),
                                border = BorderStroke(0.5.dp, GraphAmber.copy(alpha = 0.7f))
                            ) {
                                Text("-0.10 Oslabit", fontSize = 9.5.sp)
                            }

                            if (onNavigateToDialectics != null) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onNavigateToDialectics("Dialektická analýza a harmonizace domény $code")
                                    },
                                    modifier = Modifier.weight(1.3f).height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GraphCyan, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Dialektika", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
