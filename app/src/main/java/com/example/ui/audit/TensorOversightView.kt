package com.example.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.monitoring.TensorMonitorService
import com.example.monitoring.TensorOversightLog
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vizuální logovací panel pro Autoregresivní kontrolní cyklus (Tenzorový dohled).
 * Poskytuje detailní přehled o samo-opravných zásazích, doplněných datech
 * a vývoji konfidenčního skóre v čase.
 */
@Composable
fun TensorOversightView(
    viewModel: OmnisViewModel,
    onSendToChat: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val logs by viewModel.tensorOversightLogs.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("ALL") }
    val tension = TensorMonitorService.cognitiveTension
    val confidence = TensorMonitorService.confidenceScore.value

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> logs
            "DATA_SUPPLEMENT" -> logs.filter { it.correctionType.contains("Doplnění", ignoreCase = true) }
            "TECH_REFINE" -> logs.filter { it.correctionType.contains("zpřesnění", ignoreCase = true) || it.correctionType.contains("Technické", ignoreCase = true) }
            "LEGAL" -> logs.filter { it.correctionType.contains("právní", ignoreCase = true) || it.correctionType.contains("citace", ignoreCase = true) }
            else -> logs
        }
    }

    val avgConfidenceGain = remember(logs) {
        if (logs.isEmpty()) 0f
        else logs.map { it.confidenceAfter - it.confidenceBefore }.average().toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("tensor_oversight_view_root"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Hlavička panelu & Tenzorový stav
        Surface(
            color = OmnisPanelDark,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(OmnisViolet.copy(alpha = 0.3f), OmnisCyan.copy(alpha = 0.3f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = OmnisViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TENZOROVÝ DOHLED & AUTOREGRESE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Autonomní kontrolní cyklus při poklesu kognitivní tenze < 0.3",
                                color = OmnisTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OmnisEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, OmnisEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(OmnisEmerald)
                            )
                            Text(
                                text = "CYKLUS AKTIVNÍ",
                                color = OmnisEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Statistické dlaždice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "CELKEM ZÁSAHŮ",
                        value = "${logs.size}",
                        accentColor = OmnisCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "PRŮMĚRNÝ NÁRŮST",
                        value = "+${(avgConfidenceGain * 100).toInt()}%",
                        accentColor = OmnisEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "KOGNITIVNÍ TENZE",
                        value = String.format(Locale.US, "%.2f", tension),
                        accentColor = if (tension < 0.3f) OmnisAmber else OmnisCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "AKTUÁLNÍ JISTOTA",
                        value = "${(confidence * 100).toInt()}%",
                        accentColor = OmnisViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Filtrovací lišta & Akční tlačítka
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Všechny (${logs.size})", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                        selectedLabelColor = OmnisCyan
                    )
                )
                FilterChip(
                    selected = selectedFilter == "DATA_SUPPLEMENT",
                    onClick = { selectedFilter = "DATA_SUPPLEMENT" },
                    label = { Text("Doplnění dat", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OmnisEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = OmnisEmerald
                    )
                )
                FilterChip(
                    selected = selectedFilter == "TECH_REFINE",
                    onClick = { selectedFilter = "TECH_REFINE" },
                    label = { Text("Technické zpřesnění", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OmnisViolet.copy(alpha = 0.2f),
                        selectedLabelColor = OmnisViolet
                    )
                )
                FilterChip(
                    selected = selectedFilter == "LEGAL",
                    onClick = { selectedFilter = "LEGAL" },
                    label = { Text("Právní citace", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OmnisAmber.copy(alpha = 0.2f),
                        selectedLabelColor = OmnisAmber
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.clearTensorOversightLogs() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Vymazat historii dohledů",
                    tint = OmnisTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 3. Seznam událostí Tenzorového dohledu
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = OmnisEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Žádné záznamy o zásazích pro vybraný filtr",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Kognitivní tenze je stabilní nebo nebyla spuštěna autoregrese.",
                        color = OmnisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    TensorOversightCard(
                        log = log,
                        onSendToChat = {
                            viewModel.setTab(OmnisTab.CHAT)
                            onSendToChat(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OmnisBgDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = OmnisTextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TensorOversightCard(
    log: TensorOversightLog,
    onSendToChat: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • dd.MM.yyyy", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }
    val confidenceGain = log.confidenceAfter - log.confidenceBefore

    Surface(
        color = OmnisPanelDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hlavička záznamu: Typ + Čas + Latence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = OmnisViolet.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, OmnisViolet)
                ) {
                    Text(
                        text = log.correctionType.uppercase(),
                        color = OmnisViolet,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${log.executionLatencyMs} ms",
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = formattedTime,
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Dotaz
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = OmnisCyan,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = log.query,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            // Důvod spuštění cyklu (Trigger)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = OmnisAmber,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Důvod zásahu: ${log.triggerReason}",
                    color = OmnisAmber,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Vývoj konfidence (Před vs Po)
            Surface(
                color = OmnisBgDark,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Konfidence:",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(log.confidenceBefore * 100).toInt()}%",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = OmnisTextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${(log.confidenceAfter * 100).toInt()}%",
                            color = OmnisEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, OmnisEmerald)
                    ) {
                        Text(
                            text = "+${(confidenceGain * 100).toInt()}% ZPŘESNĚNO",
                            color = OmnisEmerald,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Rozbalitelný srovnávací blok (Původní vs. Doplněný obsah)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Skrýt srovnání obsahu" else "Zobrazit srovnání původní vs. doplněný obsah",
                    color = OmnisCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OmnisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Původní neúplný výstup
                    Surface(
                        color = Color(0x22EF4444),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "PŮVODNÍ VÝSTUP (POVRCHNÍ / NÍZKÁ TENZE):",
                                    color = Color(0xFFEF4444),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = log.originalContent,
                                color = OmnisTextLight,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Autonomně doplněný výstup
                    Surface(
                        color = Color(0x2210B981),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, OmnisEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = OmnisEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "AUTONOMNĚ DOPLNĚNÝ OBSAH (AUTOREGRESE):",
                                    color = OmnisEmerald,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = log.supplementedContent,
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Tlačítko pro navázání v chatu
                    OutlinedButton(
                        onClick = { onSendToChat(log.query) },
                        border = BorderStroke(1.dp, OmnisCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Otevřít a pokračovat v chatu",
                            color = OmnisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
