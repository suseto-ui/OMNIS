package com.example.ui.octagon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * FÁZE XI (11.2): 3D Canvas Digitálního Dvojčete O.M.N.I.S.
 * Poskytuje interaktivní prostorové 3D vykreslení 8D Oktagonu s projekcí perspektivy,
 * podporou rotace tažením prstu (Pitch & Yaw) a vizualizací tenzních linií ve 3D.
 */
@Composable
fun DigitalTwin3dCanvas(
    vectorMap: Map<String, Float>,
    resilienceScore: Float,
    bottleneckDomain: String,
    modifier: Modifier = Modifier
) {
    var rotX by remember { mutableFloatStateOf(25f) } // Pitch
    var rotY by remember { mutableFloatStateOf(45f) } // Yaw

    val domains = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF090D16), RoundedCornerShape(12.dp))
            .border(1.dp, OmnisCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "3D PROSTOROVÉ DIGITÁLNÍ DVOJČE",
                color = OmnisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Tažením prstu rotujte prostorovou projekci 8D Oktagonu",
                color = Color.Gray,
                fontSize = 9.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        rotY += dragAmount.x * 0.5f
                        rotX = (rotX - dragAmount.y * 0.5f).coerceIn(-75f, 75f)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) * 0.38f

            val radX = Math.toRadians(rotX.toDouble())
            val radY = Math.toRadians(rotY.toDouble())

            // Funkce pro 3D rotaci a perspektivní projekci
            fun project3D(x: Float, y: Float, z: Float): Offset {
                // Rotace kolem osy Y (Yaw)
                val x1 = (x * cos(radY) + z * sin(radY)).toFloat()
                val z1 = (-x * sin(radY) + z * cos(radY)).toFloat()

                // Rotace kolem osy X (Pitch)
                val y2 = (y * cos(radX) - z1 * sin(radX)).toFloat()
                val z2 = (y * sin(radX) + z1 * cos(radX)).toFloat()

                // Perspektivní faktor
                val distance = 400f
                val scale = distance / (distance + z2 + 200f)

                return Offset(
                    x = center.x + x1 * scale,
                    y = center.y + y2 * scale
                )
            }

            // 1. Kreslení 3D drátěného referenčního oktagonu
            val refPoints = (0 until 8).map { i ->
                val angle = i * (Math.PI * 2.0 / 8.0)
                val x = (radius * cos(angle)).toFloat()
                val z = (radius * sin(angle)).toFloat()
                project3D(x, 0f, z)
            }

            for (i in 0 until 8) {
                val next = (i + 1) % 8
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = refPoints[i],
                    end = refPoints[next],
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.25f),
                    start = center,
                    end = refPoints[i],
                    strokeWidth = 1f
                )
            }

            // 2. Kreslení aktivního 3D tenzorového polygonu
            val active3DPoints = domains.mapIndexed { i, dom ->
                val angle = i * (Math.PI * 2.0 / 8.0)
                val score = vectorMap[dom] ?: 0.5f
                val r = radius * score
                // Výška Y závisí na rozdílu od resilience skóre (prostorový reliéf)
                val y = (score - resilienceScore) * 60f
                val x = (r * cos(angle)).toFloat()
                val z = (r * sin(angle)).toFloat()
                dom to project3D(x, y, z)
            }

            val polyPath = Path().apply {
                if (active3DPoints.isNotEmpty()) {
                    moveTo(active3DPoints[0].second.x, active3DPoints[0].second.y)
                    for (i in 1 until active3DPoints.size) {
                        lineTo(active3DPoints[i].second.x, active3DPoints[i].second.y)
                    }
                    close()
                }
            }

            // Vyplnění plochy poloprůhlednou barvou
            drawPath(
                path = polyPath,
                color = OmnisCyan.copy(alpha = 0.20f)
            )

            // Obrysová linie 3D polygonu
            drawPath(
                path = polyPath,
                color = OmnisCyan,
                style = Stroke(width = 2.5f)
            )

            // 3. Vykreslení prostorových uzlů (Nodes) a popisků
            active3DPoints.forEach { (domain, pt) ->
                val isBottleneck = domain.equals(bottleneckDomain, ignoreCase = true)
                val nodeColor = if (isBottleneck) OmnisRed else OmnisEmerald

                drawCircle(
                    color = nodeColor,
                    radius = if (isBottleneck) 6f else 4f,
                    center = pt
                )
            }
        }

        // Spodní stavový řádek
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resilience: ${(resilienceScore * 100).toInt()}%",
                color = OmnisCyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bottleneck: $bottleneckDomain",
                color = OmnisRed,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
