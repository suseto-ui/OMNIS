package com.example.ui.nexus

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AgentRegistry
import com.example.agent.NexusOrchestrator
import com.example.ui.theme.*

@Composable
fun NexusView(
    query: String,
    onQueryChange: (String) -> Unit,
    onRunNexus: (List<String>) -> Unit
) {
    val history by NexusOrchestrator.nexusHistory.collectAsState()
    val isProcessing by NexusOrchestrator.isProcessing.collectAsState()
    val listState = rememberLazyListState()
    
    var selectedAgentIds by remember { mutableStateOf(AgentRegistry.agents.map { it.id }.toSet()) }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OmnisBgDark).padding(16.dp).navigationBarsPadding()) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Hub, contentDescription = null, tint = OmnisEmerald)
            Text("KOGNITIVNÍ NEXUS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
        Text("Multi-agentní kolaborace a sémantický dialog.", color = OmnisTextMuted, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(20.dp))

        // Agent Selection Rail
        Text("AKTIVNÍ AGENTI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentRegistry.agents.forEach { agent ->
                val isSelected = selectedAgentIds.contains(agent.id)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) agent.color.copy(alpha = 0.2f) else OmnisPanelDark,
                    border = BorderStroke(1.dp, if (isSelected) agent.color else OmnisBorderDark),
                    modifier = Modifier.weight(1f).clickable {
                        selectedAgentIds = if (isSelected) selectedAgentIds - agent.id else selectedAgentIds + agent.id
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(24.dp).background(agent.color, CircleShape))
                        Text(agent.name.split("-")[0], color = if (isSelected) Color.White else OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat / Process Area
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            if (history.isEmpty() && !isProcessing) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(48.dp))
                        Text("Nexus je připraven na zahájení dialogu.", color = OmnisTextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { msg ->
                        val agent = AgentRegistry.getById(msg.agentId)
                        NexusMessageBubble(agent, msg.text)
                    }
                    if (isProcessing) {
                        item {
                            Text("Čekám na vyjádření dalších agentů...", color = OmnisEmerald, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Zadejte komplexní problém pro Nexus...", color = OmnisTextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OmnisEmerald,
                unfocusedBorderColor = OmnisBorderDark
            ),
            trailingIcon = {
                IconButton(
                    onClick = { onRunNexus(selectedAgentIds.toList()) },
                    enabled = !isProcessing && query.isNotBlank() && selectedAgentIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OmnisEmerald)
                }
            }
        )
    }
}

@Composable
fun NexusMessageBubble(agent: com.example.agent.OmnisAgent?, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(agent?.color ?: Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(agent?.name?.take(1) ?: "?", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(agent?.name ?: "UNKNOWN", color = agent?.color ?: Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, OmnisBorderDark)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
