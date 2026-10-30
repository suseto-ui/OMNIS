package com.example.ui.telemetry

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.example.security.nis2.Nis2Pillar
import com.example.security.nis2.Nis2ZkAuditCertificate
import com.example.security.nis2.Nis2ZkAuditEngine
import com.example.ui.theme.*

/**
 * Interaktivní panel ZK-SNARK auditora pro směrnici NIS2 a nový zákon o kybernetické bezpečnosti (ZoKB).
 */
@Composable
fun Nis2AuditPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var isSimulationMfaActive by remember { mutableStateOf(true) }
    var isSimulationOidcActive by remember { mutableStateOf(true) }
    var simulationVulnCount by remember { mutableIntStateOf(0) }

    // Stav 10 pilířů
    var pillars by remember {
        mutableStateOf(Nis2ZkAuditEngine.getDefaultPillars())
    }

    // Aktualizace pilířů při změně simulovaných parametrů
    LaunchedEffect(isSimulationMfaActive, isSimulationOidcActive, simulationVulnCount) {
        pillars = Nis2ZkAuditEngine.getDefaultPillars(
            mfaActive = isSimulationMfaActive,
            oidcActive = isSimulationOidcActive,
            unresolvedVulnerabilities = simulationVulnCount
        )
    }

    // Aktuálně vygenerovaný ZK certifikát
    var certificate by remember {
        mutableStateOf<Nis2ZkAuditCertificate?>(
            Nis2ZkAuditEngine.generateZkAuditCertificate(pillars = pillars)
        )
    }

    var expandedPillarId by remember { mutableStateOf<String?>(null) }
    var showFullReportModal by remember { mutableStateOf(false) }

    val (currentCompliance, currentRisk) = remember(pillars) {
        Nis2ZkAuditEngine.evaluateCompliance(pillars)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(14.dp)
            .testTag("nis2_audit_panel_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hlavička panelu a Statutární stav
        Surface(
            color = OmnisPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (currentRisk > 0.40f) Color.Red else OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentRisk > 0.40f) Color.Red.copy(alpha = 0.2f) else OmnisCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentRisk > 0.40f) Icons.Default.GppBad else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (currentRisk > 0.40f) Color.Red else OmnisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "NIS2 & ZoKB ZK-AUDITOR",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ochrana statutárních orgánů dle čl. 20 NIS2",
                                color = OmnisTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (currentRisk > 0.40f) Color.Red.copy(alpha = 0.2f) else OmnisEmerald.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, if (currentRisk > 0.40f) Color.Red else OmnisEmerald)
                    ) {
                        Text(
                            text = if (currentRisk > 0.40f) "NON-COMPLIANT" else "COMPLIANT",
                            color = if (currentRisk > 0.40f) Color.Red else OmnisEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Metriky shody a sankčního rizika
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Shoda
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OmnisBgDark,
                        border = BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("INDEX SHODY (NIS2)", color = OmnisTextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${String.format("%.1f", currentCompliance * 100)} %",
                                color = if (currentCompliance >= 0.80f) OmnisEmerald else OmnisAmber,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Sankční riziko
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OmnisBgDark,
                        border = BorderStroke(1.dp, if (currentRisk > 0.40f) Color.Red.copy(alpha = 0.6f) else OmnisBorderDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("SANKČNÍ RIZIKO", color = OmnisTextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${String.format("%.1f", currentRisk * 100)} %",
                                color = if (currentRisk <= 0.10f) OmnisEmerald else if (currentRisk <= 0.40f) OmnisAmber else Color.Red,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Statutární doložka
                Text(
                    text = if (currentRisk <= 0.40f)
                        "Statutární doložka: Osobní odpovědnost členů vedení je chráněna. Všechny procesy splňují požadavky NÚKIB a EU 2022/2555."
                    else
                        "VAROVÁNÍ STATUTÁRNÍM ORGÁNŮM: Nedostatečné zabezpečení (MFA/zranitelnosti) vystavuje vedení riziku zákazu výkonu funkce a pokutám.",
                    color = if (currentRisk <= 0.40f) OmnisEmerald else Color(0xFFFCA5A5),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }

        // 2. Tlačítková akční lišta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    certificate = Nis2ZkAuditEngine.generateZkAuditCertificate(pillars = pillars)
                    Toast.makeText(context, "ZK-SNARK certifikát vygenerován", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp).testTag("btn_generate_zk_cert")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generovat ZK Důkaz", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    certificate?.let { cert ->
                        clipboardManager.setText(AnnotatedString(cert.fullAuditReport))
                        Toast.makeText(context, "Auditní zpráva zkopírována do schránky", Toast.LENGTH_SHORT).show()
                    }
                },
                border = BorderStroke(1.dp, OmnisViolet),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp).testTag("btn_copy_audit_report")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = OmnisViolet, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kopírovat Zprávu", color = OmnisViolet, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3. Simulátor podmínek shody (Stress & What-If audit)
        Surface(
            color = OmnisPanelDark.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SIMULACE STAVU INFRASTRUKTURY (WHAT-IF AUDIT)",
                    color = OmnisTextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isSimulationMfaActive,
                        onClick = { isSimulationMfaActive = !isSimulationMfaActive },
                        label = { Text("MFA Enforced", fontSize = 9.sp) },
                        leadingIcon = {
                            Icon(if (isSimulationMfaActive) Icons.Default.Check else Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    )
                    FilterChip(
                        selected = isSimulationOidcActive,
                        onClick = { isSimulationOidcActive = !isSimulationOidcActive },
                        label = { Text("OIDC / RBAC", fontSize = 9.sp) },
                        leadingIcon = {
                            Icon(if (isSimulationOidcActive) Icons.Default.Check else Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    )
                    FilterChip(
                        selected = simulationVulnCount == 0,
                        onClick = { simulationVulnCount = if (simulationVulnCount == 0) 3 else 0 },
                        label = { Text(if (simulationVulnCount == 0) "Zero Vulns" else "3x CVE Vuln", fontSize = 9.sp) },
                        leadingIcon = {
                            Icon(if (simulationVulnCount == 0) Icons.Default.Shield else Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    )
                }
            }
        }

        // 4. Karta ZK-SNARK certifikátu
        certificate?.let { cert ->
            Surface(
                color = OmnisBgDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OmnisAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ZK-SNARK COMMITMENT HASH:",
                            color = OmnisAmber,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(cert.commitmentHash))
                                Toast.makeText(context, "Hash zkopírován", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat hash", tint = OmnisAmber, modifier = Modifier.size(12.dp))
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = cert.commitmentHash,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = cert.proofOfNecessity,
                        color = OmnisTextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }
        }

        // 5. Seznam 10 pilířů NIS2
        Text(
            text = "10 ZÁKONNÝCH PILÍŘŮ BEZPEČNOSTNÍCH OPATŘENÍ (ČL. 21 ODST. 2)",
            color = OmnisCyan,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(pillars, key = { it.id }) { pillar ->
                val isExpanded = expandedPillarId == pillar.id
                Surface(
                    color = OmnisPanelDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, if (pillar.score < 0.6f) Color.Red.copy(alpha = 0.4f) else OmnisBorderDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPillarId = if (isExpanded) null else pillar.id }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pillar.articleRef,
                                    color = OmnisCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pillar.titleCs,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${(pillar.score * 100).toInt()} %",
                                color = if (pillar.score >= 0.8f) OmnisEmerald else if (pillar.score >= 0.6f) OmnisAmber else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Rozbalený detail
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = pillar.descriptionCs,
                                    color = OmnisTextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    text = "Doporučení pro statutární orgán: ${pillar.mitigationRecommendationCs}",
                                    color = OmnisAmber,
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
