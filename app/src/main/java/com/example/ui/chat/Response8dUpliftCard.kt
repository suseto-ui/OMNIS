package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.ui.OmnisCorrelationEngine
import com.example.ui.theme.OmnisStyleSheet

/**
 * Dedikovaná rozbalovací karta 8D Uplift pro asistentské zprávy.
 * Analyzuje aktuální tenzor záznamu, identifikuje úzká hrdla
 * a nabízí operátorovi okamžité akční impulsy ke zvýšení harmonie a kvality.
 */
@Composable
fun Response8dUpliftCard(
    record: OmnisRecord,
    previousRecord: OmnisRecord? = null,
    historyRecords: List<OmnisRecord> = emptyList(),
    onApplyAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val record8D = remember(record) {
        mapOf(
            "Sys" to record.valSys,
            "Econ" to record.valEcon,
            "Psych" to record.valPsych,
            "Eco" to record.valEco,
            "Law" to record.valLaw,
            "Sec" to record.valSec,
            "Phys" to record.valPhys,
            "Soc" to record.valSoc
        )
    }

    val previousRecord8D = remember(previousRecord) {
        previousRecord?.let {
            mapOf(
                "Sys" to it.valSys,
                "Econ" to it.valEcon,
                "Psych" to it.valPsych,
                "Eco" to it.valEco,
                "Law" to it.valLaw,
                "Sec" to it.valSec,
                "Phys" to it.valPhys,
                "Soc" to it.valSoc
            )
        }
    }

    val history8DVectors = remember(historyRecords) {
        historyRecords.map { r ->
            mapOf(
                "Sys" to r.valSys,
                "Econ" to r.valEcon,
                "Psych" to r.valPsych,
                "Eco" to r.valEco,
                "Law" to r.valLaw,
                "Sec" to r.valSec,
                "Phys" to r.valPhys,
                "Soc" to r.valSoc
            )
        }
    }

    val recommendations = remember(record8D) {
        OmnisCorrelationEngine.generateUpliftRecommendations(record8D)
    }

    val anomalies = remember(record8D, previousRecord8D, history8DVectors) {
        if (history8DVectors.isNotEmpty()) {
            OmnisCorrelationEngine.detectSlidingWindowAnomalies(record8D, history8DVectors, windowSize = 5)
        } else {
            OmnisCorrelationEngine.detectTensorAnomalies(record8D, previousRecord8D)
        }
    }

    val lowestEntry = remember(record8D) {
        record8D.minByOrNull { it.value }
    }

    val harmonyPercent = (record.compositeScore * 100).toInt()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnisStyleSheet.Colors.CanvasDark.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (expanded) OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.6f)
            else OmnisStyleSheet.Colors.BorderMuted
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("response_8d_uplift_card_${record.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Sbalený / rozbalovací proužek
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = OmnisStyleSheet.Colors.CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "8D UPLIFT & SYSTÉMOVÁ HARMONIE",
                                color = OmnisStyleSheet.Colors.CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.3.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = OmnisStyleSheet.Colors.SuccessEmerald.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "$harmonyPercent%",
                                    color = OmnisStyleSheet.Colors.SuccessEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        lowestEntry?.let { (lowKey, lowVal) ->
                            Text(
                                text = "Úzké hrdlo: $lowKey (${(lowVal * 100).toInt()}%) • ${if (expanded) "Skrýt kroky k posílení" else "Klikněte pro doporučení k posílení"}",
                                color = OmnisStyleSheet.Colors.TextMuted,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Sbalit" else "Rozbalit",
                        tint = OmnisStyleSheet.Colors.CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Rozbalený obsah (Detailní doporučení a akční prompty)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = OmnisStyleSheet.Colors.BorderMuted, thickness = 0.8.dp)

                    val hasCriticalAnomaly = anomalies.any { it.isCritical }

                    if (anomalies.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasCriticalAnomaly) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFF78350F).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (hasCriticalAnomaly) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth().testTag("anomaly_alert_card_${record.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (hasCriticalAnomaly) Icons.Default.Dangerous else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (hasCriticalAnomaly) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = if (hasCriticalAnomaly) "KRITICKÝ PROPAD STABILITY (>30 %)" else "DETEKOVÁNY TENZOROVÉ ANOMÁLIE (${anomalies.size})",
                                        color = if (hasCriticalAnomaly) Color(0xFFFCA5A5) else Color(0xFFFDE68A),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                anomalies.forEach { anomaly ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OmnisStyleSheet.Colors.PanelBackground.copy(alpha = 0.65f),
                                        border = BorderStroke(0.8.dp, if (anomaly.isCritical) Color(0xFFEF4444).copy(alpha = 0.6f) else OmnisStyleSheet.Colors.BorderMuted),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${anomaly.domainName} (${anomaly.domainKey})",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (anomaly.isCritical) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "${(anomaly.delta * 100).toInt()}%",
                                                        color = if (anomaly.isCritical) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = anomaly.warningMessage,
                                                color = OmnisStyleSheet.Colors.TextSecondary,
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp
                                            )

                                            if (anomaly.recoveryPrompt.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Button(
                                                    onClick = { onApplyAction(anomaly.recoveryPrompt) },
                                                    shape = RoundedCornerShape(5.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (anomaly.isCritical) Color(0xFFDC2626) else OmnisStyleSheet.Colors.CyanAccent
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                                    modifier = Modifier.height(26.dp).align(Alignment.End)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = null,
                                                        tint = if (anomaly.isCritical) Color.White else Color.Black,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (anomaly.isCritical) "Aktivovat stabilizační zásah" else "Stabilizovat",
                                                        color = if (anomaly.isCritical) Color.White else Color.Black,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "Konkrétní kroky ke zvýšení kvality výstupu a vyrovnání matice dopadů:",
                        color = OmnisStyleSheet.Colors.TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    recommendations.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OmnisStyleSheet.Colors.PanelBackground,
                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.BorderMuted.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = OmnisStyleSheet.Colors.VioletSynthesis.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.VioletSynthesis.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "+${rec.projectedGainPercent}% ${rec.domainKey}",
                                                color = OmnisStyleSheet.Colors.VioletSynthesis,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = rec.actionTitle,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = { onApplyAction(rec.actionPrompt) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OmnisStyleSheet.Colors.CyanAccent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Aplikovat",
                                            color = Color.Black,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = rec.rationale,
                                    color = OmnisStyleSheet.Colors.TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
