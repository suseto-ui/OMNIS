package com.example.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisGoal
import com.example.data.OmnisTask
import com.example.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Composable
fun GoalTrackerView(
    goals: List<OmnisGoal>,
    onCreateGoal: (String, String) -> Unit,
    onDeleteGoal: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
            .testTag("goal_tracker_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = OmnisCyan)
                Text(
                    "AUTONOMNÍ CÍLE",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(OmnisCyan.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nový cíl", tint = OmnisCyan)
            }
        }
        
        Text(
            "Dekonstrukce a exekuce dlouhodobých operačních vizí.",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Žádné aktivní cíle. Definujte novou vizi.", color = OmnisTextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    GoalCard(goal = goal, onDelete = { onDeleteGoal(goal.id) })
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
fun GoalCard(goal: OmnisGoal, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val tasks = remember(goal.tasksJson) {
        try {
            Json.decodeFromString<List<OmnisTask>>(goal.tasksJson)
        } catch (e: Exception) {
            emptyList<OmnisTask>()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = OmnisCardDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(goal.status, color = OmnisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = OmnisCyan,
                trackColor = OmnisBorderDark
            )
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(goal.description, color = OmnisTextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    
                    Text("SUB-TASKY:", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    tasks.forEach { task ->
                        TaskRow(task)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: OmnisTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (task.status == "DONE") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (task.status == "DONE") OmnisEmerald else OmnisTextMuted,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = Color.White, fontSize = 12.sp)
            Text("${task.assignedAgentId} | ${task.domain}", color = OmnisTextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
fun CreateGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmnisBgDark,
        title = { Text("Definovat novou vizi", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název cíle") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = OmnisPanelDark,
                        unfocusedContainerColor = OmnisPanelDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Popis vize") },
                    modifier = Modifier.height(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = OmnisPanelDark,
                        unfocusedContainerColor = OmnisPanelDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black)
            ) {
                Text("Dekonstruovat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = OmnisTextMuted)
            }
        }
    )
}
