package com.example.ui.octagon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Interaktivní časový graf trendů vývoje 8D dimenzí (Time-Series Multi-Line Chart).
 * Vykresluje hybridně fúzované vektory v čase, umožňuje filtrování dimenzí a zvýrazňuje anomálie.
 */
@Composable
fun OctagonTimeSeriesView(
    rawSeries: List<Omnis8dVector>,
    emaSeries: List<Omnis8dVector>,
    anomalies: List<AnomalyAlert>,
    onSelectRecord: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Stav aktivních/zobrazených dimenzí
    var activeDimensions by remember {
        mutableStateOf(setOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc"))
    }
    var useEmaSmoothing by remember { mutableStateOf(true) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val displaySeries = if (useEmaSmoothing && emaSeries.isNotEmpty()) emaSeries else rawSeries
    val dimensions = OMNIS_8D_DIMENSIONS

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = modifier
            .fillMaxWidth()
            .testTag("octagon_time_series_view")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hlavička & Přepínač vyhlazení EMA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = OmnisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ČASOVÝ VÝVOJ 8D TRENZORŮ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Tlačítko přepínače EMA / Surová data
                FilterChip(
                    selected = useEmaSmoothing,
                    onClick = { useEmaSmoothing = !useEmaSmoothing },
                    label = {
                        Text(
                            text = if (useEmaSmoothing) "EMA VYHLAZENO" else "SUROVÁ DATA",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (useEmaSmoothing) Icons.Default.AutoGraph else Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                        selectedLabelColor = OmnisCyan,
                        selectedLeadingIconColor = OmnisCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = OmnisBorderDark,
                        selectedBorderColor = OmnisCyan,
                        enabled = true,
                        selected = useEmaSmoothing
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontální volič dimenzí pro filtrování křivek
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Přepnout Vše
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeDimensions.size == dimensions.size) OmnisCyan.copy(alpha = 0.2f) else OmnisBgDark,
                    border = BorderStroke(1.dp, if (activeDimensions.size == dimensions.size) OmnisCyan else OmnisBorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            activeDimensions = if (activeDimensions.size == dimensions.size) {
                                setOf("Sys", "Sec") // Ponechat defaultní základ
                            } else {
                                dimensions.map { it.key }.toSet()
                            }
                        }
                ) {
                    Text(
                        text = if (activeDimensions.size == dimensions.size) "VŠE AKTIVNÍ" else "VŠECHNY",
                        color = if (activeDimensions.size == dimensions.size) OmnisCyan else OmnisTextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                // Jednotlivé dimenze s vlastní barvou
                dimensions.forEach { dim ->
                    val isActive = activeDimensions.contains(dim.key)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) dim.color.copy(alpha = 0.18f) else OmnisBgDark,
                        border = BorderStroke(1.dp, if (isActive) dim.color else OmnisBorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                activeDimensions = if (isActive) {
                                    if (activeDimensions.size > 1) activeDimensions - dim.key else activeDimensions
                                } else {
                                    activeDimensions + dim.key
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isActive) dim.color else OmnisTextMuted, CircleShape)
                            )
                            Text(
                                text = dim.key,
                                color = if (isActive) Color.White else OmnisTextMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hlavní grafická plocha Canvas
            if (displaySeries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(OmnisBgDark, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Žádná data pro časový graf. Položte dotaz pro zahájení měření.",
                        color = OmnisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(OmnisBgDark, RoundedCornerShape(12.dp))
                        .border(1.dp, OmnisBorderDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(displaySeries) {
                                detectTapGestures { offset ->
                                    val count = displaySeries.size
                                    if (count > 0) {
                                        val stepX = size.width / (count - 1).coerceAtLeast(1).toFloat()
                                        val clickedIdx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                                        selectedPointIndex = clickedIdx
                                        val recId = displaySeries[clickedIdx].recordId
                                        if (recId > 0) onSelectRecord(recId)
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val count = displaySeries.size

                        // 1. Horizontální mřížka (0.25, 0.50, 0.75, 1.0)
                        val gridSteps = 4
                        for (g in 0..gridSteps) {
                            val ratio = g.toFloat() / gridSteps.toFloat()
                            val y = canvasHeight * (1.0f - ratio)
                            drawLine(
                                color = OmnisBorderDark.copy(alpha = 0.4f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        val stepX = if (count > 1) canvasWidth / (count - 1).toFloat() else canvasWidth

                        // 2. Křivky pro každou aktivní dimenzi
                        dimensions.filter { activeDimensions.contains(it.key) }.forEach { dim ->
                            val path = Path()
                            displaySeries.forEachIndexed { idx, vector ->
                                val x = if (count > 1) idx * stepX else canvasWidth / 2f
                                val yValue = vector.getValue(dim.key).coerceIn(0f, 1f)
                                val y = canvasHeight * (1f - yValue)

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            // Vykreslení čáry dimenze
                            drawPath(
                                path = path,
                                color = dim.color.copy(alpha = 0.85f),
                                style = Stroke(width = 2.5f)
                            )

                            // Bodové markery
                            displaySeries.forEachIndexed { idx, vector ->
                                val x = if (count > 1) idx * stepX else canvasWidth / 2f
                                val yValue = vector.getValue(dim.key).coerceIn(0f, 1f)
                                val y = canvasHeight * (1f - yValue)

                                drawCircle(
                                    color = dim.color,
                                    radius = 3.5f,
                                    center = Offset(x, y)
                                )
                            }
                        }

                        // 3. Zvýrazněná svislá čára pro zvolený bod
                        selectedPointIndex?.let { selIdx ->
                            if (selIdx in displaySeries.indices) {
                                val selX = if (count > 1) selIdx * stepX else canvasWidth / 2f
                                drawLine(
                                    color = OmnisCyan.copy(alpha = 0.8f),
                                    start = Offset(selX, 0f),
                                    end = Offset(selX, canvasHeight),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                                )
                            }
                        }
                    }
                }
            }

            // Detail vybraného bodu (pokud je označen)
            selectedPointIndex?.let { idx ->
                if (idx in displaySeries.indices) {
                    val pt = displaySeries[idx]
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OmnisBgDark,
                        border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VZOREK: ${pt.label.ifBlank { "Měření #${idx + 1}" }}",
                                    color = OmnisCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "KOMPOZIT: ${(pt.composite * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Mřížka hodnot
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                dimensions.take(4).forEach { dim ->
                                    Text(
                                        text = "${dim.key}: ${(pt.getValue(dim.key) * 100).toInt()}%",
                                        color = dim.color,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                dimensions.drop(4).forEach { dim ->
                                    Text(
                                        text = "${dim.key}: ${(pt.getValue(dim.key) * 100).toInt()}%",
                                        color = dim.color,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sekce aktivních anomálií
            if (anomalies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OmnisRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, OmnisRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = OmnisRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DETEKOVÁNY STATISTICKÉ ANOMÁLIE (> 2.0σ)",
                                color = OmnisRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        anomalies.take(2).forEach { alert ->
                            Text(
                                text = "• [${alert.dimensionKey}] Odchylka ${(alert.sigmaDiff * 10).toInt() / 10f}σ (${(alert.currentValue * 100).toInt()}% vs průměr ${(alert.meanValue * 100).toInt()}%)",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "  Náprava: ${alert.recommendation}",
                                color = OmnisTextMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}
