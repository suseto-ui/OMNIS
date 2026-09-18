package com.example.ui.scenarios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.scenario.ForecastPoint
import com.example.scenario.ForecastingEngine
import com.example.scenario.ScenarioEvent
import com.example.scenario.ScenarioLibrary
import com.example.ui.theme.*

@Composable
fun ScenarioPlannerView(
    currentRecord: OmnisRecord?
) {
    var selectedEvents by remember { mutableStateOf(setOf<ScenarioEvent>()) }
    val forecast = remember(currentRecord, selectedEvents) {
        currentRecord?.let { 
            ForecastingEngine.project(it, selectedEvents.toList(), steps = 20)
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("scenario_planner_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Timeline, contentDescription = null, tint = OmnisCyan)
            Text(
                "SCENARIO ARCHITECT",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Text(
            "Modelování prediktivních scénářů a sémantických driftů v čase.",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // Levý panel: Výběr událostí
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            ) {
                Text("AKTIVNÍ UDÁLOSTI", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScenarioLibrary.presetEvents) { event ->
                        val isSelected = selectedEvents.contains(event)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedEvents = if (isSelected) selectedEvents - event else selectedEvents + event
                                },
                            color = if (isSelected) event.color.copy(alpha = 0.2f) else OmnisCardDark,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) event.color else OmnisBorderDark)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(event.name, color = if (isSelected) event.color else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(event.description, color = OmnisTextMuted, fontSize = 9.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                }
            }

            // Pravý panel: Vizualizace projekce
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .padding(start = 8.dp)
            ) {
                Text("PROJEKCE 8D MATICE", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    if (forecast.isNotEmpty()) {
                        ForecastChart(forecast)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Načítání kognitivních dat...", color = OmnisTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Souhrn dopadu
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    color = OmnisPanelDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(14.dp))
                            Text("PREDIKTIVNÍ SYNTÉZA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (selectedEvents.isEmpty()) {
                            Text("Vyberte události pro zahájení simulace driftu.", color = OmnisTextMuted, fontSize = 10.sp)
                        } else {
                            val last = forecast.last()
                            val first = forecast.first()
                            ImpactRow("Stabilita systému", first.valSys, last.valSys)
                            ImpactRow("Ekonomická efektivita", first.valEcon, last.valEcon)
                            ImpactRow("Bezpečnostní integrita", first.valSec, last.valSec)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastChart(points: List<ForecastPoint>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        // Draw grid
        for (i in 0..4) {
            val y = height * (i / 4f)
            drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, y), Offset(width, y))
        }

        // Draw lines for key dimensions
        drawForecastLine(points, { it.valSys }, OmnisCyan, width, height, stepX)
        drawForecastLine(points, { it.valEcon }, OmnisAmber, width, height, stepX)
        drawForecastLine(points, { it.valSec }, Color.Red, width, height, stepX)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForecastLine(
    points: List<ForecastPoint>,
    selector: (ForecastPoint) -> Float,
    color: Color,
    width: Float,
    height: Float,
    stepX: Float
) {
    val path = Path()
    points.forEachIndexed { i, point ->
        val x = i * stepX
        val y = height * (1f - selector(point))
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}

@Composable
fun ImpactRow(label: String, initial: Float, final: Float) {
    val diff = final - initial
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OmnisTextMuted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${(final * 100).toInt()}%",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = (if (diff >= 0) "+" else "") + "${(diff * 100).toInt()}%",
                color = if (diff >= 0) OmnisEmerald else Color.Red,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
