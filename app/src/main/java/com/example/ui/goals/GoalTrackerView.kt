package com.example.ui.goals

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisGoal
import com.example.data.OmnisTask
import com.example.ui.guide.OmnisHelpIconButton
import com.example.ui.theme.*
import kotlinx.serialization.json.Json

@Composable
fun GoalTrackerView(
    goals: List<OmnisGoal>,
    onCreateGoal: (String, String) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onToggleTask: (Long, String) -> Unit = { _, _ -> },
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val activeCount = remember(goals) { goals.count { it.status == "ACTIVE" } }
    val completedCount = remember(goals) { goals.count { it.status == "COMPLETED" } }
    val avgProgress = remember(goals) {
        if (goals.isEmpty()) 0
        else (goals.map { it.progress }.average() * 100).toInt()
    }

    val filteredGoals = remember(goals, selectedFilter, searchQuery) {
        goals.filter { goal ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "ACTIVE" -> goal.status == "ACTIVE"
                "COMPLETED" -> goal.status == "COMPLETED"
                "HIGH_PRIO" -> goal.priority >= 4
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase().trim()
                goal.title.lowercase().contains(q) ||
                goal.description.lowercase().contains(q) ||
                goal.tasksJson.lowercase().contains(q)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(14.dp)
            .navigationBarsPadding()
            .testTag("goal_tracker_root")
    ) {
        // Hlavička
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "AUTONOMNÍ CÍLE",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Dekompozice misí a sledování exekuce (${goals.size})",
                        color = OmnisTextMuted,
                        fontSize = 10.sp
                    )
                }
                OmnisHelpIconButton(
                    title = "Autonomní Cíle",
                    description = "Agentní systém O.M.N.I.S. pro dekompozici, řízení a automatizované sledování komplexních projektů a misí.",
                    bulletPoints = listOf(
                        "Dekompozice: Každý cíl obsahuje strukturované podúkoly.",
                        "Interaktivita: Klepnutím na podúkol změníte jeho stav (DONE / PENDING).",
                        "Progrese: Výpočet % splnění v reálném čase podle dokončených kroků.",
                        "Priority: Automatické označení priorit (CRITICAL, HIGH, MEDIUM)."
                    ),
                    tint = OmnisCyan
                )
            }

            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreateDialog = true 
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(OmnisCyan.copy(alpha = 0.15f))
                    .border(1.dp, OmnisCyan.copy(alpha = 0.4f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nový cíl", tint = OmnisCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Souhrnné metriky
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalMetricCard("CELKEM", goals.size.toString(), OmnisCyan, Modifier.weight(1f))
            GoalMetricCard("AKTIVNÍ", activeCount.toString(), OmnisAmber, Modifier.weight(1f))
            GoalMetricCard("HOTOVO", completedCount.toString(), OmnisEmerald, Modifier.weight(1f))
            GoalMetricCard("PRŮMĚR", "$avgProgress%", if (avgProgress > 75) OmnisEmerald else OmnisCyan, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vyhledávání
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Hledat v cílech a podúkolech...", color = OmnisTextMuted, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(15.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Vymazat", tint = OmnisTextMuted, modifier = Modifier.size(13.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OmnisCyan,
                unfocusedBorderColor = OmnisBorderDark,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = OmnisCyan
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filtrační čipy
        val filters = listOf("ALL", "ACTIVE", "COMPLETED", "HIGH_PRIO")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                val chipColor = when (filter) {
                    "ACTIVE" -> OmnisAmber
                    "COMPLETED" -> OmnisEmerald
                    "HIGH_PRIO" -> Color.Red
                    else -> OmnisCyan
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.2f) else OmnisPanelDark,
                    border = BorderStroke(1.dp, if (isSelected) chipColor else OmnisBorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = filter
                        }
                ) {
                    Text(
                        text = when (filter) {
                            "ALL" -> "VŠECHNY"
                            "ACTIVE" -> "AKTIVNÍ"
                            "COMPLETED" -> "DOKONČENÉ"
                            "HIGH_PRIO" -> "VYSOKÁ PRIORITA"
                            else -> filter
                        },
                        color = if (isSelected) chipColor else OmnisTextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredGoals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlagCircle, contentDescription = null, tint = OmnisBorderDark, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") 
                            "Žádné cíle neodpovídají filtru." 
                        else 
                            "Žádné aktivní cíle. Zadejte novou vizi tlačítkem +.",
                        color = OmnisTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredGoals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onDelete = { onDeleteGoal(goal.id) },
                        onToggleTask = { taskId -> onToggleTask(goal.id, taskId) },
                        onSaveArtifact = onSaveArtifact,
                        onSendToChat = onSendToChat
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc ->
                onCreateGoal(title, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GoalMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                color = OmnisTextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: OmnisGoal,
    onDelete: () -> Unit,
    onToggleTask: (String) -> Unit,
    onSaveArtifact: ((title: String, type: String, language: String, content: String) -> Unit)? = null,
    onSendToChat: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tasks = remember(goal.tasksJson) {
        try {
            Json.decodeFromString<List<OmnisTask>>(goal.tasksJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val isCompleted = goal.status == "COMPLETED" || goal.progress >= 1.0f
    val progressColor = if (isCompleted) OmnisEmerald else OmnisCyan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = !expanded 
            }
            .animateContentSize(),
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCompleted) OmnisEmerald.copy(alpha = 0.4f) else OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = goal.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (goal.priority >= 4) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, Color.Red),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "PRIO ${goal.priority}",
                                    color = Color.Red,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "STATUS: DOKONČENO" else "STATUS: ${goal.status}",
                            color = progressColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${(goal.progress * 100).toInt()}% splněno",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat cíl", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = OmnisBorderDark
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            color = OmnisTextMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Text(
                        text = "DEKOMPONOVANÉ ÚKOLY (${tasks.count { it.status == "DONE" }}/${tasks.size}):",
                        color = OmnisCyan,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    tasks.forEach { task ->
                        InteractiveTaskRow(
                            task = task,
                            onToggle = { onToggleTask(task.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Akční tlačítka pro kognitivní operace s cílem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Konzultace v chatu
                        if (onSendToChat != null) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val pendingTasks = tasks.filter { it.status != "DONE" }
                                    val prompt = buildString {
                                        appendLine("Autonomní asistence pro misi: **${goal.title}**")
                                        if (goal.description.isNotBlank()) appendLine("Popis: ${goal.description}")
                                        appendLine("Aktuální stav: ${(goal.progress * 100).toInt()}% splněno.")
                                        if (pendingTasks.isNotEmpty()) {
                                            appendLine("Zbývající úkoly:")
                                            pendingTasks.forEach { t ->
                                                appendLine("- [${t.assignedAgentId}] ${t.title} (${t.domain})")
                                            }
                                        }
                                        appendLine("Navrhni přesné exekuční kroky, kód nebo automatizované řešení pro nejbližší prioritní úkol.")
                                    }
                                    onSendToChat(prompt)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, OmnisViolet),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = OmnisViolet, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Konzultovat", color = OmnisViolet, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Uložení plánu mise do Znalostních Artefaktů
                        if (onSaveArtifact != null) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val artifactContent = buildString {
                                        appendLine("# O.M.N.I.S. Plán mise: ${goal.title}")
                                        if (goal.description.isNotBlank()) {
                                            appendLine("## Popis vize")
                                            appendLine(goal.description)
                                            appendLine()
                                        }
                                        appendLine("## Metriky mise")
                                        appendLine("- **Priorita:** ${goal.priority}")
                                        appendLine("- **Status:** ${goal.status}")
                                        appendLine("- **Progrese:** ${(goal.progress * 100).toInt()}%")
                                        appendLine()
                                        appendLine("## Dekomponované úkoly")
                                        tasks.forEach { t ->
                                            val check = if (t.status == "DONE") "[x]" else "[ ]"
                                            appendLine("- $check **${t.title}** (Agent: `${t.assignedAgentId}`, Doména: `${t.domain}`)")
                                        }
                                    }
                                    onSaveArtifact(
                                        "Mise: ${goal.title}",
                                        "REPORT",
                                        "markdown",
                                        artifactContent
                                    )
                                    Toast.makeText(context, "Mise uložena do artefaktů", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, OmnisEmerald),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uložit do artefaktů", color = OmnisEmerald, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sdílení plánu mise
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val shareText = buildString {
                                    appendLine("O.M.N.I.S. Mise: ${goal.title}")
                                    appendLine("Progrese: ${(goal.progress * 100).toInt()}%")
                                    tasks.forEach { t ->
                                        val mark = if (t.status == "DONE") "✓" else "○"
                                        appendLine("$mark ${t.title} [${t.assignedAgentId}]")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, "Mise: ${goal.title}")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Sdílet misi: ${goal.title}"))
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, OmnisCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sdílet", color = OmnisCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Kopírování do schránky
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val copyText = buildString {
                                    appendLine("Mise: ${goal.title}")
                                    if (goal.description.isNotBlank()) appendLine("${goal.description}\n")
                                    tasks.forEach { t ->
                                        val mark = if (t.status == "DONE") "[X]" else "[ ]"
                                        appendLine("$mark ${t.title} (${t.assignedAgentId})")
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(copyText))
                                Toast.makeText(context, "Plán mise zkopírován", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, OmnisBorderDark),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopírovat", color = OmnisTextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTaskRow(
    task: OmnisTask,
    onToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDone = task.status == "DONE"

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isDone) OmnisPanelDark.copy(alpha = 0.4f) else OmnisPanelDark,
        border = BorderStroke(0.5.dp, if (isDone) OmnisEmerald.copy(alpha = 0.3f) else OmnisBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isDone) "Hotovo" else "Ke splnění",
                tint = if (isDone) OmnisEmerald else OmnisCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (isDone) OmnisTextMuted else Color.White,
                    fontSize = 11.5.sp,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "AGENT: ${task.assignedAgentId}",
                        color = OmnisCyan.copy(alpha = 0.7f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• DOMÉNA: ${task.domain}",
                        color = OmnisTextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = if (isDone) OmnisEmerald.copy(alpha = 0.15f) else OmnisAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = task.status,
                    color = if (isDone) OmnisEmerald else OmnisAmber,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        titleContentColor = Color.White,
        textContentColor = OmnisTextMuted,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(20.dp))
                Text("Definovat novou vizi", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "O.M.N.I.S. automaticky dekonstruuje zadání na strukturované úkoly.",
                    color = OmnisTextMuted,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název cíle (např. Audit bezpečnosti)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OmnisCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Detailní specifikace vize", fontSize = 11.sp) },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = OmnisCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Zadejte název cíle", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(title.trim(), desc.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dekonstruovat", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = OmnisTextMuted, fontSize = 11.sp)
            }
        }
    )
}
