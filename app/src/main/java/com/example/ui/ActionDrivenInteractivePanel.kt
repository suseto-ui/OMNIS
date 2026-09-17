package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.action.ActionPayload
import com.example.action.ActionExecutionResult
import com.example.ui.theme.*

/**
 * Action-Driven Interactive Panel:
 * Umožňuje odesílat strukturované JSON payloady pro nativní Python/Backend akce:
 * 1. Infra Diagnostika (DB test, TLS validace, Connection pool)
 * 2. Nízkoúrovňová Optimalizace (eBPF/XDP offloading, distributed tracing)
 * 3. Resilience & Stabilita (Circuit Breaker, Bulkhead, Chaos Engineering)
 * 4. AI Governance (Zero-Trust pravidla, deterministický stav export)
 */
@Composable
fun ActionDrivenInteractivePanel(
    onExecuteActionPayload: (ActionPayload) -> Unit,
    lastActionResult: ActionExecutionResult? = null,
    isActionExecuting: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedActionId by remember { mutableStateOf<String?>("db_connectivity_test") }

    // Dynamic Parameter State
    var targetEngine by remember { mutableStateOf("PostgreSQL") }
    var networkInterface by remember { mutableStateOf("eth0") }
    var chaosSimulationEnabled by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("action_driven_panel"),
        shape = RoundedCornerShape(14.dp),
        color = OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OmnisCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "AKČNÍ DISPEČER (SYSTEM ACTIONS)",
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "JSON Payload",
                            color = OmnisCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                        tint = OmnisCyan
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Vyberte systémovou akci pro spuštění bez účasti LLM (nativní backend dispatcher):",
                        color = OmnisTextMuted,
                        fontSize = 11.sp
                    )

                    // Action Selector Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActionChip(
                            label = "Infra DB Diagnostika",
                            icon = Icons.Default.Storage,
                            isSelected = selectedActionId == "db_connectivity_test",
                            onClick = { selectedActionId = "db_connectivity_test" }
                        )
                        ActionChip(
                            label = "eBPF/XDP Offload",
                            icon = Icons.Default.Memory,
                            isSelected = selectedActionId == "ebpf_xdp_offload",
                            onClick = { selectedActionId = "ebpf_xdp_offload" }
                        )
                        ActionChip(
                            label = "Circuit Breaker Audit",
                            icon = Icons.Default.Security,
                            isSelected = selectedActionId == "resilience_circuit_breaker_audit",
                            onClick = { selectedActionId = "resilience_circuit_breaker_audit" }
                        )
                        ActionChip(
                            label = "Zero-Trust Export",
                            icon = Icons.Default.Shield,
                            isSelected = selectedActionId == "ai_governance_export",
                            onClick = { selectedActionId = "ai_governance_export" }
                        )
                    }

                    // Dynamic Action Parameter Controls
                    when (selectedActionId) {
                        "db_connectivity_test" -> {
                            OutlinedTextField(
                                value = targetEngine,
                                onValueChange = { targetEngine = it },
                                label = { Text("DB Engine / Target", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OmnisCyan, unfocusedBorderColor = OmnisBorderDark)
                            )
                        }
                        "ebpf_xdp_offload" -> {
                            OutlinedTextField(
                                value = networkInterface,
                                onValueChange = { networkInterface = it },
                                label = { Text("Network Interface", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OmnisCyan, unfocusedBorderColor = OmnisBorderDark)
                            )
                        }
                    }

                    // Structured Payload Preview
                    val payload = when (selectedActionId) {
                        "db_connectivity_test" -> ActionPayload(
                            intent = "execute_diagnostic",
                            actionId = "db_connectivity_test",
                            parameters = mapOf(
                                "engine" to targetEngine,
                                "include_tls_validation" to true
                            )
                        )
                        "ebpf_xdp_offload" -> ActionPayload(
                            intent = "optimize_network_layer",
                            actionId = "ebpf_xdp_offload",
                            parameters = mapOf(
                                "interface" to networkInterface,
                                "sync_type" to "CRDT_EVENT_DRIVEN"
                            )
                        )
                        "resilience_circuit_breaker_audit" -> ActionPayload(
                            intent = "test_resilience_boundary",
                            actionId = "resilience_circuit_breaker_audit",
                            parameters = mapOf(
                                "domain_boundary" to "ALL_DOMAINS",
                                "chaos_simulation" to chaosSimulationEnabled
                            )
                        )
                        "ai_governance_export" -> ActionPayload(
                            intent = "enforce_zero_trust",
                            actionId = "ai_governance_export",
                            parameters = mapOf(
                                "export_deterministic_state" to true,
                                "audit_cycle" to "NEXT_ITERATION"
                            )
                        )
                        else -> ActionPayload(intent = "custom", actionId = "default")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OmnisBgDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "JSON Payload Contract:",
                                    color = OmnisCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Endpoint: /api/v1/dispatch",
                                    color = OmnisTextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = payload.toJsonString(),
                                color = Color(0xFFA5D6A7),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Execute Button
                    Button(
                        onClick = {
                            onExecuteActionPayload(payload)
                        },
                        enabled = !isActionExecuting,
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("action_execute_button")
                    ) {
                        if (isActionExecuting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROVÁDÍ SE DISPEČINK...", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ODESLAT A SPUSTIT NATIVNÍ AKCI", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) OmnisCyan.copy(alpha = 0.2f) else OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) OmnisCyan else OmnisBorderDark),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OmnisCyan else OmnisTextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else OmnisTextMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
