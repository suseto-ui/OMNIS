package com.example.ui.nexus

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
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
    onRunNexus: (List<String>) -> Unit,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onCreateGoal: ((title: String, description: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null
) {
    val history by NexusOrchestrator.nexusHistory.collectAsState()
    val isProcessing by NexusOrchestrator.isProcessing.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    
    var selectedAgentIds by remember { mutableStateOf(AgentRegistry.agents.filter { it.id != "omnis_core" }.map { it.id }.toSet()) }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(OmnisBgDark).padding(14.dp).navigationBarsPadding()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(22.dp))
                Column {
                    Text("KOGNITIVNÍ NEXUS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text("Multi-agentní kolaborace a sémantický dialog", color = OmnisTextMuted, fontSize = 10.sp)
                }
                com.example.ui.guide.OmnisHelpIconButton(
                    title = "Kognitivní Nexus",
                    description = "Paralelní multi-agentní orchestrátor O.M.N.I.S. pro řešení komplexních úloh zapojením specializovaných AI profilů.",
                    bulletPoints = listOf(
                        "Agenti: System Architect, Security Red Team, Economic Analyst, Cognitive Engineer.",
                        "Synergie: Agenti komunikují v reálném čase a hledají konsensus.",
                        "Paralelizace: Každý agent analyzuje zadání ze svého oborového úhlu.",
                        "Syntéza: O.M.N.I.S.-CORE provede finální integraci všech stanovisek."
                    ),
                    tint = OmnisEmerald
                )
            }

            if (history.isNotEmpty() && !isProcessing) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        NexusOrchestrator.clearHistory()
                        Toast.makeText(context, "Historie Nexusu vymazána", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Vymazat Nexus", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Agent Selection Rail
        Text("ZAPOJENÍ AGENTI V SYSTÉMU:", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AgentRegistry.agents.filter { it.id != "omnis_core" }.forEach { agent ->
                val isSelected = selectedAgentIds.contains(agent.id)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) agent.color.copy(alpha = 0.2f) else OmnisPanelDark,
                    border = BorderStroke(1.dp, if (isSelected) agent.color else OmnisBorderDark),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAgentIds = if (isSelected) selectedAgentIds - agent.id else selectedAgentIds + agent.id
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(18.dp).background(agent.color, CircleShape))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            agent.name.split("-")[0],
                            color = if (isSelected) Color.White else OmnisTextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Akční lišta Nexusu, pokud máme výsledky
        if (history.isNotEmpty() && !isProcessing) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onSaveArtifact != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val report = buildString {
                                appendLine("# O.M.N.I.S. Nexus Deliberace")
                                appendLine("**Dotaz / Problém:** $query\n")
                                appendLine("## Stanoviska specializovaných agentů")
                                history.forEach { msg ->
                                    val a = AgentRegistry.getById(msg.agentId)
                                    appendLine("### ${a?.name ?: msg.agentId} (${a?.role ?: "Agent"})")
                                    appendLine(msg.text)
                                    appendLine()
                                }
                            }
                            onSaveArtifact(
                                "Nexus Deliberace: ${query.take(30)}",
                                "REPORT",
                                "markdown",
                                report
                            )
                            Toast.makeText(context, "Uloženo do Znalostních Artefaktů", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, OmnisEmerald),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Uložit syntézu", color = OmnisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (onCreateGoal != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val coreSynthesis = history.findLast { it.agentId == "omnis_core" }?.text ?: history.lastOrNull()?.text ?: query
                            val goalTitle = "Mise: ${query.take(35)}"
                            onCreateGoal(goalTitle, coreSynthesis)
                            Toast.makeText(context, "Mise vytvořena v Autonomních Cílech", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, OmnisCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vytvořit cíl", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val textToShare = buildString {
                            appendLine("O.M.N.I.S. Nexus: $query\n")
                            history.forEach { msg ->
                                val a = AgentRegistry.getById(msg.agentId)
                                appendLine("[${a?.name ?: msg.agentId}]: ${msg.text}\n")
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TITLE, "O.M.N.I.S. Nexus")
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Sdílet Nexus deliberaci"))
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, OmnisCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sdílet", color = OmnisCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val fullDialogue = history.joinToString("\n\n") { msg ->
                            val a = AgentRegistry.getById(msg.agentId)
                            "${a?.name ?: msg.agentId}:\n${msg.text}"
                        }
                        clipboardManager.setText(AnnotatedString(fullDialogue))
                        Toast.makeText(context, "Celý dialog zkopírován do schránky", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, OmnisBorderDark),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kopírovat", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat / Process Area
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            if (history.isEmpty() && !isProcessing) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nexus je připraven na zahájení deliberace.",
                            color = OmnisTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Zadejte problém níže a spusťte paralelní analýzu.",
                            color = OmnisTextMuted.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history) { msg ->
                        val agent = AgentRegistry.getById(msg.agentId)
                        NexusMessageBubble(
                            agent = agent,
                            text = msg.text,
                            onSendToChat = onSendToChat
                        )
                    }
                    if (isProcessing) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = OmnisEmerald, strokeWidth = 2.dp)
                                Text("Probíhá agentní deliberace a syntéza...", color = OmnisEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Zadejte komplexní problém pro Nexus...", color = OmnisTextMuted, fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OmnisEmerald,
                unfocusedBorderColor = OmnisBorderDark
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRunNexus(selectedAgentIds.toList()) 
                    },
                    enabled = !isProcessing && query.isNotBlank() && selectedAgentIds.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Spustit Nexus",
                        tint = if (!isProcessing && query.isNotBlank() && selectedAgentIds.isNotEmpty()) OmnisEmerald else OmnisBorderDark
                    )
                }
            }
        )
    }
}

@Composable
fun NexusMessageBubble(
    agent: com.example.agent.OmnisAgent?,
    text: String,
    onSendToChat: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isCore = agent?.id == "omnis_core"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(agent?.color ?: Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(agent?.name?.take(1) ?: "?", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    agent?.name ?: "UNKNOWN",
                    color = agent?.color ?: Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onSendToChat != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val prompt = "Konzultace pohledu agenta [${agent?.name ?: "Agent"}]:\n\n$text\n\nJaké jsou praktické implementační kroky?"
                                onSendToChat(prompt)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Konzultovat v Chatu", tint = OmnisViolet, modifier = Modifier.size(12.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Zkopírováno", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = OmnisTextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(0.dp, 10.dp, 10.dp, 10.dp),
                color = if (isCore) OmnisCyan.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, if (isCore) OmnisCyan.copy(alpha = 0.5f) else OmnisBorderDark),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    com.example.ui.OmnisMarkdownText(
                        text = text,
                        textColor = Color.White,
                        fontSize = 12
                    )
                }
            }
        }
    }
}
