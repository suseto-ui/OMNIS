package com.example.ui.nexus

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.NexusAgent
import com.example.nexus.NexusTopology
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun NexusVisualizerView(
    activeAgentIds: List<String> = emptyList()
) {
    var selectedAgent by remember { mutableStateOf<NexusAgent?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "nexus_pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("nexus_visualizer_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = OmnisCyan)
                Text(
                    "NEURAL NEXUS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Text(
            "Vizualizace sémantické topologie a agentní spolupráce.",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, OmnisBorderDark, RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw connections
                NexusTopology.agents.forEachIndexed { i, agentA ->
                    NexusTopology.agents.forEachIndexed { j, agentB ->
                        if (i < j) {
                            val start = Offset(agentA.position.x * width, agentA.position.y * height)
                            val end = Offset(agentB.position.x * width, agentB.position.y * height)
                            
                            val isActive = activeAgentIds.contains(agentA.id) && activeAgentIds.contains(agentB.id)
                            
                            drawLine(
                                color = if (isActive) OmnisCyan.copy(alpha = 0.6f) else OmnisBorderDark.copy(alpha = 0.3f),
                                start = start,
                                end = end,
                                strokeWidth = if (isActive) 3f else 1f,
                                pathEffect = if (!isActive) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                            )
                        }
                    }
                }

                // Draw Agents
                NexusTopology.agents.forEach { agent ->
                    val center = Offset(agent.position.x * width, agent.position.y * height)
                    val isRunning = activeAgentIds.contains(agent.id)
                    
                    // Outer glow for active
                    if (isRunning) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(agent.color.copy(alpha = 0.3f), Color.Transparent),
                                center = center,
                                radius = 60f * pulseScale
                            ),
                            center = center,
                            radius = 60f * pulseScale
                        )
                    }

                    // Main node
                    drawCircle(
                        color = if (isRunning) agent.color else agent.color.copy(alpha = 0.4f),
                        center = center,
                        radius = 20f
                    )
                    
                    drawCircle(
                        color = Color.Black,
                        center = center,
                        radius = 16f
                    )

                    drawCircle(
                        color = agent.color,
                        center = center,
                        radius = 8f,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Overlay clickable invisible nodes
            NexusTopology.agents.forEach { agent ->
                BoxWithConstraints {
                    val x = agent.position.x
                    val y = agent.position.y
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (maxWidth * x) - 30.dp,
                                y = (maxHeight * y) - 30.dp
                            )
                            .size(60.dp)
                            .clip(CircleShape)
                            .clickable { selectedAgent = agent }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Agent Details Info
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            color = OmnisCardDark,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedAgent == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                        Text("Vyberte uzel pro detaily agenta.", color = OmnisTextMuted, fontSize = 14.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(selectedAgent!!.color, CircleShape))
                        Text(selectedAgent!!.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(selectedAgent!!.domain, color = selectedAgent!!.color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(selectedAgent!!.description, color = OmnisTextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
