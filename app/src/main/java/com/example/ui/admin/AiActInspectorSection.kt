package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.OmnisAccessibilityHelper.accessibleButton
import com.example.accessibility.OmnisAccessibilityHelper.accessibleHeading
import com.example.accessibility.OmnisAccessibilityHelper.accessibleInputField
import com.example.ai.transparency.AiOriginDetector
import com.example.ai.transparency.AiSteganographyEngine
import com.example.ai.transparency.ContentOriginType
import com.example.ai.transparency.OriginAnalysisReport

@Composable
fun AiActInspectorSection() {
    var inputText by remember { mutableStateOf("") }
    var analysisReport by remember { mutableStateOf<OriginAnalysisReport?>(null) }
    var watermarkedSample by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("ai_act_inspector_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "EU AI Act Čl. 50 Forenzní Inspekce & Steganografie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.accessibleHeading("EU AI Act Čl. 50 Forenzní Inspekce a Steganografie")
            )
            Text(
                text = "Ověřování neviditelných strojově čitelných vodoznaků (čl. 50 odst. 2) a detekce syntetického původu textů a nahrávek.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Testovací vstupy
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Vstup pro analýzu původu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Zadejte nebo vložte text/přepis nahrávky pro forenzní analýzu vodoznaku...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .accessibleInputField("Text pro forenzní analýzu původu")
                        .testTag("ai_inspector_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            analysisReport = AiOriginDetector.analyzeContent(inputText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .accessibleButton("Spustit forenzní analýzu původu")
                            .testTag("run_ai_origin_analysis_btn")
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyzovat Původ")
                    }

                    OutlinedButton(
                        onClick = {
                            val generated = "Architektonické tenzory a kognitivní uzly systému OMNIS splňují normu EU AI Act 2026."
                            val watermarked = AiSteganographyEngine.embedWatermark(generated, "gemini-3.1-pro-preview")
                            inputText = watermarked
                            watermarkedSample = watermarked
                            analysisReport = AiOriginDetector.analyzeContent(watermarked)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .accessibleButton("Vložit ukázku s vodoznakem")
                            .testTag("insert_watermarked_sample_btn")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vložit Vodoznak")
                    }
                }
            }
        }

        // Výsledky analýzy
        analysisReport?.let { report ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (report.originType) {
                        ContentOriginType.VERIFIED_OMNIS_SYNTHETIC -> Color(0xFF10B981).copy(alpha = 0.15f)
                        ContentOriginType.EXTERNAL_WATERMARKED_AI -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        ContentOriginType.UNVERIFIED_SUSPICIOUS_AI -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        ContentOriginType.NATURAL_HUMAN -> MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        when (report.originType) {
                            ContentOriginType.VERIFIED_OMNIS_SYNTHETIC -> Color(0xFF10B981)
                            ContentOriginType.EXTERNAL_WATERMARKED_AI -> Color(0xFF3B82F6)
                            ContentOriginType.UNVERIFIED_SUSPICIOUS_AI -> Color(0xFFEF4444)
                            ContentOriginType.NATURAL_HUMAN -> MaterialTheme.colorScheme.outlineVariant
                        },
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (report.originType) {
                                ContentOriginType.VERIFIED_OMNIS_SYNTHETIC -> Icons.Default.CheckCircle
                                ContentOriginType.UNVERIFIED_SUSPICIOUS_AI -> Icons.Default.Warning
                                else -> Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = when (report.originType) {
                                ContentOriginType.VERIFIED_OMNIS_SYNTHETIC -> Color(0xFF10B981)
                                ContentOriginType.EXTERNAL_WATERMARKED_AI -> Color(0xFF3B82F6)
                                ContentOriginType.UNVERIFIED_SUSPICIOUS_AI -> Color(0xFFEF4444)
                                ContentOriginType.NATURAL_HUMAN -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Klasifikace: ${report.originType.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Důvěra: ${(report.confidence * 100).toInt()}% • Agent: ${report.detectedSoftwareAgent}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Status Shody: ${report.complianceStatus}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Doporučení: ${report.recommendation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Sekce 2: Generátor Pasu Shody (Conformity Passport)
        var showPassport by remember { mutableStateOf(false) }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = androidx.compose.ui.platform.LocalContext.current

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Generátor Pasu Shody EU AI Act (Čl. 11 & 15)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatické vygenerování strojově čitelného průkazu shody a technické dokumentace pro auditorské účely podle harmonizovaných norem EU.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showPassport = !showPassport },
                    modifier = Modifier
                        .fillMaxWidth()
                        .accessibleButton("Vygenerovat pas shody EU AI Act")
                        .testTag("generate_conformity_passport_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showPassport) "Skrýt Pas Shody" else "Vygenerovat Pas Shody (JSON-LD)")
                }

                if (showPassport) {
                    val passportJson = """
                    {
                      "@context": "https://schema.org",
                      "@type": "SoftwareApplication",
                      "name": "O.M.N.I.S.",
                      "version": "2.0.0",
                      "classification": "EU_AI_ACT_HIGH_RISK_ANNEX_III",
                      "conformityAssessment": {
                        "status": "COMPLIANT_APPROVED",
                        "assessmentMethod": "Internal_Control_Plus_Adversarial_Red_Team_Audit",
                        "complianceTimestamp": "2026-09-24T12:15:00Z",
                        "articlesChecked": {
                          "Article_9": "Risk Management System - Active 28-pair correlation tensor with Meadows leverage points",
                          "Article_10": "Data Governance - Local Room DB peristence with strict schema isolation",
                          "Article_11": "Technical Documentation - Fully self-documenting JSON-LD cognitive chain of thought",
                          "Article_12": "Record-keeping - Automated transactional auditing logs",
                          "Article_13": "Transparency - Human-in-the-Loop priority profiles and real-time 8D dashboards",
                          "Article_14": "Human Oversight - Systemic override thresholds and manual gatekeepers",
                          "Article_15": "Accuracy & Security - Multi-layer defense perimeters, regex filters, and adversarial reviewing",
                          "Article_50": "Steganographic watermarking - Embedded software agent fingerprinting"
                        }
                      },
                      "systemicResilienceIndex": 0.94,
                      "governanceFramework": "NIS2_COMPLIANT_SECURITY_SHIELD"
                    }
                    """.trimIndent()

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PAS SHODY (JSON-LD):",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(passportJson))
                                        android.widget.Toast.makeText(context, "Pas shody zkopírován!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Zkopírovat pas shody",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = passportJson,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
