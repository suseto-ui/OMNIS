package com.example.ui.audit

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitoring.AuditCheckItem
import com.example.monitoring.ProductionAuditManager
import com.example.monitoring.ProductionAuditReport
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionAuditView(
    onPruneOldData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRunningAudit by remember { mutableStateOf(false) }
    var auditReport by remember { mutableStateOf<ProductionAuditReport?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") }
    var activeSubTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isRunningAudit = true
        auditReport = ProductionAuditManager.runComprehensiveAudit(context)
        isRunningAudit = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
    ) {
        // Tab přepínač mezi 360° auditem a zátěžovým benchmarkem
        PrimaryTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = OmnisPanelDark,
            contentColor = OmnisCyan,
            divider = { HorizontalDivider(color = OmnisBorderDark) }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Text(
                        "360° AUDIT KVALITY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_audit_checklist")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Text(
                        "ZÁTĚŽOVÝ BENCHMARK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_stress_benchmark")
            )
        }

        if (activeSubTab == 1) {
            StressBenchmarkView()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
        // Hero Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OmnisCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OmnisCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Audit",
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PRODUKČNÍ AUDIT & CERTIFIKACE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "360° Verifikace: Bezpečnost, Play Politiky & Škálování",
                                    color = OmnisTextLight,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        auditReport?.let { report ->
                            val scoreColor = if (report.overallReadinessScore >= 90) OmnisEmerald else if (report.overallReadinessScore >= 70) Color(0xFFF59E0B) else Color(0xFFEF4444)
                            Surface(
                                color = scoreColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${report.overallReadinessScore}%",
                                    color = scoreColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    auditReport?.let { report ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SystemStatChip("Testů celkem", "${report.totalChecks}")
                            SystemStatChip("Úspěšných", "${report.passedChecks}", OmnisEmerald)
                            SystemStatChip("Kritických chyb", "${report.failedChecks}", if (report.failedChecks > 0) Color(0xFFEF4444) else OmnisTextLight)
                            SystemStatChip("Paměť RAM", "${report.memoryUsageMb} MB")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isRunningAudit = true
                                    auditReport = ProductionAuditManager.runComprehensiveAudit(context)
                                    isRunningAudit = false
                                }
                            },
                            enabled = !isRunningAudit,
                            colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isRunningAudit) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AUDITUJI...", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PŘEZKOUMAT SYSTÉM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onPruneOldData,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OmnisEmerald),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PROŘEZAT DB", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Vše",
                    "EU_AI_ACT_NIS2" to "🇪🇺 EU AI Act & NIS2",
                    "TECHNOLOGY" to "1. Technologie",
                    "FACTUAL" to "2. Fakta",
                    "LEGAL" to "3. Právo & Play",
                    "FUNCTIONAL" to "4. Funkce",
                    "ARCHITECTURAL" to "5. Architektura"
                ).forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategoryFilter == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = catKey },
                        label = { Text(catLabel, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OmnisCyan.copy(alpha = 0.2f),
                            selectedLabelColor = OmnisCyan
                        )
                    )
                }
            }
        }

        // Checklist Items
        auditReport?.let { report ->
            val filteredItems = report.items.filter {
                selectedCategoryFilter == "ALL" || it.category == selectedCategoryFilter
            }

            items(filteredItems) { check ->
                AuditItemCard(check)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }
}
}

@Composable
private fun SystemStatChip(label: String, value: String, color: Color = Color.White) {
    Column {
        Text(text = label, fontSize = 9.sp, color = OmnisTextLight, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AuditItemCard(check: AuditCheckItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (check.isPassed) OmnisBorderDark else Color(0xFFEF4444).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (check.isPassed) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (check.isPassed) OmnisEmerald else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = check.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = if (check.isPassed) OmnisEmerald.copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (check.isPassed) "PASS" else check.severity,
                        color = if (check.isPassed) OmnisEmerald else Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = check.description,
                color = OmnisTextLight,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            check.recommendation?.let { rec ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Doporučení: $rec",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
