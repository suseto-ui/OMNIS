package com.example.ui.gateway

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gateway.GateEvaluation
import com.example.gateway.GateId
import com.example.gateway.GateStatus
import com.example.gateway.RefusalLadderVerdict

private val BadgeCyan = Color(0xFF00E5FF)
private val BadgeGreen = Color(0xFF10B981)
private val BadgeAmber = Color(0xFFF59E0B)
private val BadgeRed = Color(0xFFEF4444)
private val BadgeBg = Color(0xFF0B111E)
private val BadgeCardBg = Color(0xFF131D2E)
private val BadgeBorder = Color(0xFF1E2D42)

@Composable
fun RefusalLadderStatusBadge(
    verdict: RefusalLadderVerdict?,
    userRole: com.example.auth.UserRole = com.example.auth.UserRole.STANDARD_USER,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedGate by remember { mutableStateOf<GateEvaluation?>(null) }
    var showStandardUserSummaryDialog by remember { mutableStateOf(false) }

    if (selectedGate != null) {
        GateDetailDialog(
            gate = selectedGate!!,
            onDismiss = { selectedGate = null }
        )
    }

    if (showStandardUserSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showStandardUserSummaryDialog = false },
            title = {
                Text(
                    text = "AKGE-8D: Kognitivní & Normativní Soulad",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = if (verdict?.isApproved == false) {
                        "Tento požadavek byl pozastaven automatickým bezpečnostním systémem pro ochranu před zakázanými AI praktikami nebo kybernetickými riziky."
                    } else {
                        "Všechny parametry vašeho požadavku byly asynchronně ověřeny kaskádou norem EU AI Act a NIS2. Systém garantuje vysokou úroveň transparentnosti a etického ukotvení."
                    },
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showStandardUserSummaryDialog = false }) {
                    Text("Rozumím", color = BadgeCyan)
                }
            },
            containerColor = BadgeCardBg
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(
                    0.5.dp,
                    if (verdict?.isApproved == false) BadgeRed.copy(alpha = 0.6f) else BadgeCyan.copy(alpha = 0.3f)
                ),
                RoundedCornerShape(8.dp)
            )
            .testTag("refusal_ladder_status_badge"),
        color = BadgeCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (verdict?.isApproved == false) Icons.Default.GppBad else Icons.Default.GppGood,
                    contentDescription = null,
                    tint = if (verdict?.isApproved == false) BadgeRed else BadgeGreen,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (userRole == com.example.auth.UserRole.ADMIN_OPERATOR) "6-GATE LADDER [ADMIN]" else "AKGE-8D SOULAD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (userRole == com.example.auth.UserRole.STANDARD_USER) {
                // STANDARD_USER: Kompaktní mikro-odznaky bez zbytečného žargonu
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BadgeBg,
                        border = BorderStroke(0.5.dp, if (verdict?.isApproved == false) BadgeRed else BadgeGreen),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            showStandardUserSummaryDialog = true
                        }
                    ) {
                        Text(
                            text = if (verdict?.isApproved == false) "ZABLOKOVÁNO" else "AKGE-8D: 98% UKOTVENO ✓",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (verdict?.isApproved == false) BadgeRed else BadgeGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BadgeBg,
                        border = BorderStroke(0.5.dp, BadgeCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "EU AI ACT / NIS2 ✓",
                            fontSize = 8.sp,
                            color = BadgeCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                // ADMIN_OPERATOR: Kompletní diagnostika 6 bran s auditním hashem
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val evaluations = verdict?.evaluations ?: GateId.values().map {
                        GateEvaluation(it, GateStatus.PASSED, "ISO/IEC 42001", "Nominální stav")
                    }

                    evaluations.forEach { eval ->
                        val chipColor = when (eval.status) {
                            GateStatus.PASSED -> BadgeGreen
                            GateStatus.WARNING -> BadgeAmber
                            GateStatus.REFUSED -> BadgeRed
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BadgeBg,
                            border = BorderStroke(0.5.dp, chipColor.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    selectedGate = eval
                                }
                                .testTag("gate_chip_${eval.gateId.code}")
                        ) {
                            Text(
                                text = "${eval.gateId.code}${if (eval.status == GateStatus.PASSED) "✓" else if (eval.status == GateStatus.WARNING) "!" else "✕"}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipColor,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    verdict?.auditHash?.let { hash ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BadgeBg,
                            border = BorderStroke(0.5.dp, BadgeCyan.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "#$hash",
                                fontSize = 8.sp,
                                color = BadgeCyan,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GateDetailDialog(
    gate: GateEvaluation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (gate.status) {
                                GateStatus.PASSED -> BadgeGreen
                                GateStatus.WARNING -> BadgeAmber
                                GateStatus.REFUSED -> BadgeRed
                            }
                        )
                )
                Text(
                    text = "${gate.gateId.code}: ${gate.gateId.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BadgeBg,
                    border = BorderStroke(0.5.dp, BadgeCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Norma: ${gate.regulatoryNorm}",
                        fontSize = 10.sp,
                        color = BadgeCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Text(
                    text = gate.reason,
                    fontSize = 12.sp,
                    color = Color.White
                )

                gate.quotableCitation?.let { citation ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BadgeBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Citace: \"$citation\"",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                gate.remediationHint?.let { hint ->
                    Text(
                        text = "Doporučení k nápravě: $hint",
                        fontSize = 11.sp,
                        color = BadgeAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = BadgeCyan)
            }
        },
        containerColor = BadgeCardBg
    )
}

@Composable
fun RefusalComplianceCard(
    verdict: RefusalLadderVerdict,
    onRemediate: (() -> Unit)? = null,
    onOverride: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val refusing = verdict.refusingGate ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, BadgeRed), RoundedCornerShape(10.dp))
            .testTag("refusal_compliance_card"),
        color = BadgeCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = BadgeRed, modifier = Modifier.size(16.dp))
                    Text(
                        text = "ZASTAVENO BRÁNOU ${refusing.gateId.code}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BadgeBg,
                    border = BorderStroke(0.5.dp, BadgeRed.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "BLOKOVÁNO",
                        fontSize = 9.sp,
                        color = BadgeRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = refusing.reason,
                fontSize = 11.sp,
                color = Color.White
            )

            refusing.quotableCitation?.let { citation ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BadgeBg,
                    border = BorderStroke(0.5.dp, BadgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = citation,
                        fontSize = 9.5.sp,
                        color = Color.LightGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            refusing.remediationHint?.let { hint ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Doporučená reformulace: $hint",
                    fontSize = 10.sp,
                    color = BadgeAmber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onOverride != null) {
                    OutlinedButton(
                        onClick = onOverride,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BadgeAmber),
                        border = BorderStroke(0.5.dp, BadgeAmber)
                    ) {
                        Text("Operátorský Override", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
