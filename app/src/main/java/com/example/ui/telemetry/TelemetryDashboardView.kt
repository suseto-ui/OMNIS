package com.example.ui.telemetry

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisTelemetry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelemetryDashboardView(
    logs: List<OmnisTelemetry>,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    // Výpočet agregovaných metrik
    val errorCount = remember(logs) { logs.count { it.type == "ERROR" } }
    val warningCount = remember(logs) { logs.count { it.type == "WARNING" } }
    val infoCount = remember(logs) { logs.count { it.type == "INFO" } }

    // Filtrované logy
    val filteredLogs = remember(logs, selectedFilter, searchQuery) {
        logs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "ERROR" -> log.type == "ERROR"
                "WARNING" -> log.type == "WARNING"
                "INFO" -> log.type == "INFO"
                "PERFORMANCE" -> log.type == "PERFORMANCE"
                "COGNITIVE_DRIFT" -> log.type == "COGNITIVE_DRIFT"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.lowercase().trim()
                log.message.lowercase().contains(q) ||
                log.component.lowercase().contains(q) ||
                log.metadata.lowercase().contains(q)
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
            .testTag("telemetry_dashboard_root")
    ) {
        // 1. Hlavička s akcemi
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "SYSTEM TELEMETRY",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Hloubková observabilita a audit kognitivní integrity",
                        color = OmnisTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Export JSON / Markdown tlačítko
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val exported = buildString {
                                appendLine("# O.M.N.I.S. Telemetry Export (${Date()})")
                                appendLine("Total Logs: ${logs.size} | Errors: $errorCount | Warnings: $warningCount\n")
                                logs.forEach { l ->
                                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(l.timestamp))
                                    appendLine("[$time] [${l.type}] [${l.component}]: ${l.message}")
                                    if (l.metadata != "{}") appendLine("  Metadata: ${l.metadata}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(exported))
                            Toast.makeText(context, "Telemetrie (${logs.size} záznamů) zkopírována do schránky", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat logy", tint = OmnisCyan, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClearLogs()
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Smazat logy", tint = Color.Red.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Metrické karty (Summary Stats Bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                label = "CELKEM",
                value = logs.size.toString(),
                color = OmnisCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "CHYBY",
                value = errorCount.toString(),
                color = if (errorCount > 0) Color.Red else OmnisEmerald,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "VAROVÁNÍ",
                value = warningCount.toString(),
                color = if (warningCount > 0) OmnisAmber else OmnisEmerald,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "INFO",
                value = infoCount.toString(),
                color = OmnisCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Vyhledávací pole
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filtrovat zprávy, komponenty, metadata...", color = OmnisTextMuted, fontSize = 11.sp) },
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

        // 4. Filtrační čipy podle závažnosti
        val filters = listOf("ALL", "ERROR", "WARNING", "INFO", "PERFORMANCE", "COGNITIVE_DRIFT")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                val chipColor = when (filter) {
                    "ERROR" -> Color.Red
                    "WARNING" -> OmnisAmber
                    "INFO" -> OmnisCyan
                    "PERFORMANCE" -> OmnisEmerald
                    "COGNITIVE_DRIFT" -> OmnisViolet
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
                        text = filter,
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

        // 5. Seznam logů
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = OmnisPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OmnisBorderDark)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedFilter != "ALL") 
                            "Žádné logy neodpovídají zadanému filtru." 
                        else 
                            "Systémové logy jsou prázdné.",
                        color = OmnisTextMuted,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        TelemetryLogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
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
fun TelemetryLogItem(log: OmnisTelemetry) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val typeColor = when(log.type) {
        "ERROR" -> Color.Red
        "WARNING" -> OmnisAmber
        "INFO" -> OmnisCyan
        "COGNITIVE_DRIFT" -> OmnisViolet
        "PERFORMANCE" -> OmnisEmerald
        else -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = OmnisBgDark,
        border = BorderStroke(0.5.dp, typeColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(typeColor)
                    )
                    Text(
                        text = log.type,
                        color = typeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "| ${log.component.uppercase()}",
                        color = OmnisTextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp)),
                        color = OmnisTextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clipboardManager.setText(AnnotatedString("[${log.type}] ${log.component}: ${log.message} | ${log.metadata}"))
                            Toast.makeText(context, "Log zkopírován do schránky", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat log", tint = OmnisTextMuted, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3
            )

            if (log.metadata != "{}" && log.metadata.isNotBlank()) {
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Metadata: ${log.metadata}",
                                color = OmnisCyan.copy(alpha = 0.75f),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
