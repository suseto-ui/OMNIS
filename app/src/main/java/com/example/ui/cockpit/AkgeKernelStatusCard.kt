package com.example.ui.cockpit

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.AkgeTensorKernel
import kotlinx.coroutines.launch

private val AkgeCyan = Color(0xFF00E5FF)
private val AkgeGreen = Color(0xFF10B981)
private val AkgeAmber = Color(0xFFF59E0B)
private val AkgeRed = Color(0xFFEF4444)
private val AkgeBgDark = Color(0xFF0B111E)
private val AkgeCardBg = Color(0xFF131D2E)
private val AkgeBorderDark = Color(0xFF1E2D42)

@Composable
fun AkgeKernelStatusCard(
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val kernelSnapshot by AkgeTensorKernel.kernelState.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (kernelSnapshot.percolationStatus.isCascadeRiskActive) AkgeRed else AkgeCyan.copy(alpha = 0.5f)
                ),
                RoundedCornerShape(12.dp)
            )
            .testTag("akge_kernel_status_card"),
        color = AkgeCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        isExpanded = !isExpanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (kernelSnapshot.percolationStatus.isCascadeRiskActive) AkgeRed else AkgeGreen
                            )
                    )
                    Column {
                        Text(
                            text = "AKGE-8D TENSOR KERNEL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (kernelSnapshot.percolationStatus.isCascadeRiskActive) "KASKÁDOVÉ RIZIKO (pc >= 0.38)" else "INTEGRITA ZACHOVÁNA",
                            fontSize = 9.sp,
                            color = if (kernelSnapshot.percolationStatus.isCascadeRiskActive) AkgeRed else AkgeCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AkgeBgDark,
                        border = BorderStroke(0.5.dp, AkgeCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = kernelSnapshot.stateCommitmentHash.take(8),
                            color = AkgeCyan,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Rozbalit panel AKGE jádra",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Summary Metrics Bar
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "Euler χ",
                    value = "${kernelSnapshot.topologicalMetrics.eulerCharacteristic}",
                    isAlert = !kernelSnapshot.topologicalMetrics.isValid
                )
                MetricPill(
                    label = "Betti β1",
                    value = "${kernelSnapshot.topologicalMetrics.betti1}",
                    isAlert = kernelSnapshot.topologicalMetrics.betti1 > 7
                )
                MetricPill(
                    label = "Entropie S",
                    value = String.format("%.2f", kernelSnapshot.topologicalMetrics.shannonEntropy),
                    isAlert = kernelSnapshot.topologicalMetrics.shannonEntropy >= 2.07f
                )
                MetricPill(
                    label = "Pnutí σ",
                    value = String.format("%.2f", kernelSnapshot.systemStressSigma),
                    isAlert = kernelSnapshot.systemStressSigma > 0.6f
                )
            }

            // Expanded Interactive Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = AkgeBorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "8D Tenzorové rozložení vah (CSR Sparse Matrix):",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 8-Domain mini bars
                    val weights = kernelSnapshot.domainWeights
                    val chunked = weights.entries.chunked(4)
                    chunked.forEach { rowEntries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowEntries.forEach { entry ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = AkgeBgDark,
                                    border = BorderStroke(0.5.dp, AkgeBorderDark)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            entry.key,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entry.value < 0.5f) AkgeAmber else AkgeCyan
                                        )
                                        Text(
                                            String.format("%.2f", entry.value),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Autonomous Stabilizer Status Panel
                    val stabilizerState by com.example.engine.AkgeAutonomousStabilizer.stabilizerState.collectAsStateWithLifecycle()
                    val coroutineScope = rememberCoroutineScope()

                    LaunchedEffect(kernelSnapshot.percolationStatus.isCascadeRiskActive, kernelSnapshot.systemStressSigma) {
                        if (kernelSnapshot.percolationStatus.isCascadeRiskActive || kernelSnapshot.systemStressSigma >= 0.38f) {
                            com.example.engine.AkgeAutonomousStabilizer.checkAndTriggerIfNeeded(coroutineScope)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                BorderStroke(
                                    0.5.dp,
                                    if (stabilizerState.status == com.example.engine.StabilizationStatus.STABILIZING) AkgeAmber
                                    else AkgeGreen.copy(alpha = 0.5f)
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        color = AkgeBgDark
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (stabilizerState.status == com.example.engine.StabilizationStatus.STABILIZING) AkgeAmber else AkgeGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "AUTONOMNÍ STABILIZÁTOR AGENTŮ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (stabilizerState.isAutonomousTriggerEnabled) AkgeGreen.copy(alpha = 0.2f) else AkgeBorderDark
                                ) {
                                    Text(
                                        text = if (stabilizerState.isAutonomousTriggerEnabled) "AUTO-ON" else "AUTO-OFF",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stabilizerState.isAutonomousTriggerEnabled) AkgeGreen else Color.Gray,
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                com.example.engine.AkgeAutonomousStabilizer.setAutonomousTriggerEnabled(!stabilizerState.isAutonomousTriggerEnabled)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (stabilizerState.status == com.example.engine.StabilizationStatus.STABILIZING) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = AkgeAmber)
                                    Text(
                                        text = "Probíhá dialektická debata pro eliminaci napětí...",
                                        fontSize = 8.5.sp,
                                        color = AkgeAmber,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else if (stabilizerState.lastEvent != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val ev = stabilizerState.lastEvent!!
                                Text(
                                    text = "Poslední intervence: ZK #${ev.zkCommitmentHash} | Konsensus ${(ev.consensusIndex * 100).toInt()}% (σ: ${String.format("%.2f", ev.initialStressSigma)} -> ${String.format("%.2f", ev.resolvedStressSigma)})",
                                    fontSize = 8.sp,
                                    color = AkgeCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                AkgeTensorKernel.injectCascadeStress(listOf("SYS", "SEC", "LAW"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("akge_simulate_stress_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AkgeAmber),
                            border = BorderStroke(1.dp, AkgeAmber.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulovat stres", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    com.example.engine.AkgeAutonomousStabilizer.executeStabilization(
                                        reason = "Manuální požadavek operátora na stabilizaci",
                                        fastMode = false
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("akge_stabilize_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AkgeGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stabilizovat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                AkgeTensorKernel.resetToEquilibrium()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("akge_reset_equilibrium_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AkgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rovnováha", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    isAlert: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AkgeBgDark,
        border = BorderStroke(0.5.dp, if (isAlert) AkgeRed else AkgeBorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 8.sp,
                color = if (isAlert) AkgeRed else Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) AkgeRed else Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
