package com.example.ui.octagon

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OmnisRecord
import com.example.ui.OmnisCorrelationEngine
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data structure holding calculated audit details for a single dimension in the 8D Impact Matrix.
 */
data class DimensionAuditItem(
    val key: String,
    val name: String,
    val canonicalName: String,
    val czechLabel: String,
    val value: Float,
    val pastValue: Float?,
    val delta: Float?,
    val isFixed: Boolean,
    val color: Color,
    val description: String,
    val topSynergies: String,
    val topFrictions: String,
    val statusLabel: String,
    val statusColor: Color
)

/**
 * Interactive Dialog displaying a comprehensive Summary Report of all 8 dimensions
 * in the O.M.N.I.S. Impact Matrix and their correlation with the current project status.
 */
@Composable
fun OctagonSummaryReportDialog(
    simSys: Float,
    simEcon: Float,
    simPsych: Float,
    simEco: Float,
    simLaw: Float,
    simSec: Float,
    simPhys: Float,
    simSoc: Float,
    fixedDomains: Set<String> = emptySet(),
    latestRecord: OmnisRecord?,
    records: List<OmnisRecord>,
    activePriorityProfile: String = "UNIFORM",
    onDismiss: () -> Unit,
    onDirectMitigate: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val composite = (simSys + simEcon + simPsych + simEco + simLaw + simSec + simPhys + simSoc) / 8f

    val currentVectorMap = remember(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc) {
        mapOf(
            "Sys" to simSys,
            "Econ" to simEcon,
            "Psych" to simPsych,
            "Eco" to simEco,
            "Law" to simLaw,
            "Sec" to simSec,
            "Phys" to simPhys,
            "Soc" to simSoc
        )
    }

    val currentWeights = remember(activePriorityProfile) {
        when (activePriorityProfile) {
            "SECURITY_FIRST" -> mapOf("Sec" to 1.0f, "Law" to 1.0f, "Phys" to 0.8f, "Sys" to 0.8f, "Econ" to 0.1f, "Psych" to 0.4f, "Eco" to 0.3f, "Soc" to 0.5f)
            "BUDGET_FIRST" -> mapOf("Econ" to 1.0f, "Sys" to 0.8f, "Sec" to 0.5f, "Law" to 0.6f, "Psych" to 0.1f, "Eco" to 0.3f, "Phys" to 0.5f, "Soc" to 0.4f)
            "GREEN_FIRST" -> mapOf("Eco" to 1.0f, "Soc" to 0.8f, "Psych" to 0.8f, "Sys" to 0.6f, "Econ" to 0.3f, "Law" to 0.7f, "Sec" to 0.5f, "Phys" to 0.4f)
            else -> mapOf("Sys" to 1.0f, "Econ" to 1.0f, "Psych" to 1.0f, "Eco" to 1.0f, "Law" to 1.0f, "Sec" to 1.0f, "Phys" to 1.0f, "Soc" to 1.0f)
        }
    }

    val systemicAnalysis = remember(currentVectorMap, currentWeights) {
        OmnisCorrelationEngine.calculateWeightedSystemicEquilibrium(currentVectorMap, currentWeights)
    }

    // Build audit data for all 8 dimensions
    val dimensionAuditList = remember(simSys, simEcon, simPsych, simEco, simLaw, simSec, simPhys, simSoc, latestRecord, fixedDomains) {
        listOf(
            Triple("Sys", simSys, latestRecord?.valSys),
            Triple("Econ", simEcon, latestRecord?.valEcon),
            Triple("Psych", simPsych, latestRecord?.valPsych),
            Triple("Eco", simEco, latestRecord?.valEco),
            Triple("Law", simLaw, latestRecord?.valLaw),
            Triple("Sec", simSec, latestRecord?.valSec),
            Triple("Phys", simPhys, latestRecord?.valPhys),
            Triple("Soc", simSoc, latestRecord?.valSoc)
        ).map { (key, curVal, pastVal) ->
            val delta = pastVal?.let { curVal - it }
            val (name, color, desc, czechLabel) = when (key) {
                "Sys" -> Tuple4("Systémové inženýrství", Color(0xFF60A5FA), "Modularita, čistota architektury a kybernetická stabilita.", "Systémové inženýrství")
                "Econ" -> Tuple4("Teorie her & Ekonomie", Color(0xFFFBBF24), "Nákladová efektivita, tokenová alokace a rozpočtová udržitelnost.", "Ekonomie & Náklady")
                "Psych" -> Tuple4("Kognitivní vědy & Psychologie", Color(0xFFC084FC), "Etika, transparentnost, ergonomie a snížení mentální zátěže operátora.", "Kognice & Ergonomie")
                "Eco" -> Tuple4("Regenerativní Ekologie", Color(0xFF34D399), "Udržitelnost, energetický otisk a materiálová rovnováha biosféry.", "Ekologie & Biosféra")
                "Law" -> Tuple4("Regulace & Právo", Color(0xFFFB7185), "Regulatorní compliance (EU AI Act, NIS2, GDPR) a auditní jistota.", "Právo & Compliance")
                "Sec" -> Tuple4("Zero-Trust Bezpečnost", Color(0xFFEF4444), "Ochrana perimetru, kryptografie, integrita paměti a mitigace hrozeb.", "Zero-Trust Bezpečnost")
                "Phys" -> Tuple4("Fyzikální termodynamika", Color(0xFFFB923C), "Infrastruktura, edge výpočty, latence a hardwarové mantinely.", "Fyzika & Hardware")
                "Soc" -> Tuple4("Socio-kulturní dynamika", Color(0xFFF472B6), "Sociální koheze, férovost modelů a eliminace digitální propasti.", "Společnost & Tým")
                else -> Tuple4("Neznámá", Color.Gray, "", "")
            }

            val (synergies, frictions) = when (key) {
                "Sys" -> Pair("Sec (+0.78), Phys (+0.62), Soc (+0.55)", "Eco (-0.15)")
                "Econ" -> Pair("Sys (+0.45), Soc (+0.42), Phys (+0.38)", "Eco (-0.54), Sec (-0.35), Law (-0.30)")
                "Psych" -> Pair("Soc (+0.85), Eco (+0.60), Sys (+0.40)", "Sec (-0.48), Econ (-0.25), Phys (-0.20)")
                "Eco" -> Pair("Phys (+0.71), Law (+0.65), Psych (+0.60)", "Econ (-0.54)")
                "Law" -> Pair("Sec (+0.82), Soc (+0.68), Eco (+0.65)", "Econ (-0.30)")
                "Sec" -> Pair("Law (+0.82), Sys (+0.78), Phys (+0.52)", "Psych (-0.48), Econ (-0.35)")
                "Phys" -> Pair("Eco (+0.71), Sys (+0.62), Sec (+0.52)", "Psych (-0.20), Soc (-0.10)")
                "Soc" -> Pair("Psych (+0.85), Law (+0.68), Eco (+0.58)", "Econ (-0.15)")
                else -> Pair("-", "-")
            }

            val (statusText, statusCol) = when {
                curVal < 0.30f -> Pair("Kritické riziko", OmnisRed)
                curVal < 0.55f -> Pair("Degradováno", OmnisAmber)
                curVal < 0.75f -> Pair("Stabilní", OmnisCyan)
                else -> Pair("Excelentní", OmnisEmerald)
            }

            DimensionAuditItem(
                key = key,
                name = name,
                canonicalName = key.uppercase(),
                czechLabel = czechLabel,
                value = curVal,
                pastValue = pastVal,
                delta = delta,
                isFixed = fixedDomains.contains(key),
                color = color,
                description = desc,
                topSynergies = synergies,
                topFrictions = frictions,
                statusLabel = statusText,
                statusColor = statusCol
            )
        }
    }

    // Full Markdown summary report string builder for clipboard / export
    val fullMarkdownReport = remember(dimensionAuditList, systemicAnalysis, records, latestRecord, composite) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val assistantRecords = records.filter { it.role == "assistant" }
        val userRecords = records.filter { it.role == "user" }

        buildString {
            appendLine("# 🌐 SOUHRNNÁ AUDITNÍ ZPRÁVA O 8 DIMENZÍCH MATICE DOPADŮ")
            appendLine("### O.M.N.I.S. Core Intelligence Synthesis & Project Correlation")
            appendLine("---")
            appendLine("- **Datum a čas analýzy:** $timestamp")
            appendLine("- **Integrální index harmonie:** ${(composite * 100).toInt()}%")
            appendLine("- **Systémová odolnost (Resilience):** ${(systemicAnalysis.systemicResilience * 100).toInt()}%")
            appendLine("- **Harmonický průměr (Leontief):** ${(systemicAnalysis.harmonicMean * 100).toInt()}%")
            appendLine("- **Aritmetický průměr:** ${(systemicAnalysis.arithmeticMean * 100).toInt()}%")
            appendLine("- **Úzké hrdlo (Bottleneck):** ${systemicAnalysis.bottleneckDomain} (${(systemicAnalysis.bottleneckValue * 100).toInt()}%)")
            appendLine("- **Bod nejvyšší systémové páky (Leverage Point):** ${systemicAnalysis.leverageDomain}")
            appendLine("- **Aktivní profil priorit:** $activePriorityProfile")
            appendLine()
            appendLine("## 📁 AKTUÁLNÍ STAV PROJEKTU")
            appendLine("- **Celkový počet záznamů v paměti:** ${records.size}")
            appendLine("- **Syntetizované odpovědi asistenta:** ${assistantRecords.size}")
            appendLine("- **Dotazy operátora:** ${userRecords.size}")
            appendLine("- **Referenční záznam:** #${latestRecord?.id ?: "Žádný"} (${latestRecord?.role ?: "N/A"})")
            if (latestRecord != null) {
                appendLine("- **Obsah referenčního záznamu:** \"${latestRecord.content.take(120).replace("\n", " ")}...\"")
                appendLine("- **Historické kompozitní skóre reference:** ${(latestRecord.compositeScore * 100).toInt()}%")
            }
            appendLine()
            appendLine("## 📊 DETAIL VŠECH 8 DIMENZÍ MATICE DOPADŮ")
            appendLine("| Dimenze | Kód | Aktuální Stav | Ref. Δ | Status | Fixace | Klíčové Synergie (+) | Klíčové Frikce (-) |")
            appendLine("|---|---|---|---|---|---|---|---|")
            dimensionAuditList.forEach { item ->
                val deltaStr = item.delta?.let { d -> (if (d > 0) "+" else "") + "${(d * 100).toInt()}%" } ?: "N/A"
                val fixStr = if (item.isFixed) "🔒 Fix" else "🔓 Volné"
                appendLine("| ${item.name} | `${item.key}` | ${(item.value * 100).toInt()}% | $deltaStr | ${item.statusLabel} | $fixStr | ${item.topSynergies} | ${item.topFrictions} |")
            }
            appendLine()
            appendLine("## 🔄 KORELACE S AKTUÁLNÍM STAVEM PROJEKTU A RIZIKA")
            if (systemicAnalysis.isCriticalFailure) {
                appendLine("⚠️ **KRITICKÉ VAROVÁNÍ SYSTÉMU:** Doména **${systemicAnalysis.bottleneckDomain}** je na kritické úrovni (${(systemicAnalysis.bottleneckValue * 100).toInt()}%). Vykazuje nebezpečí dominového efektu a ohrožuje celkovou integritu projektu.")
            } else {
                appendLine("✅ **SYSTÉMOVÁ STABILITA:** Projekt se nachází v provozuschopném koridoru odolnosti (${(systemicAnalysis.systemicResilience * 100).toInt()}%).")
            }
            appendLine()
            appendLine("### Doporučená strategická intervence (Donella Meadows Leverage):")
            appendLine("Investice a posílení v doméně **${systemicAnalysis.leverageDomain}** vytvoří maximální pozitivní kaskádový efekt, který pomůže uvolnit úzké hrdlo (**${systemicAnalysis.bottleneckDomain}**) a zvýší celkový index harmonie projektu.")
            appendLine()
            appendLine("---")
            appendLine("*Generováno automatickým diagnostickým modulem O.M.N.I.S. Octagon Engine.*")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("octagon_summary_report_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = OmnisPanelDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header Row
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
                                .background(Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SOUHRNNÁ ZPRÁVA 8D MATICE",
                                color = OmnisCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Audit 8 dimenzí dopadů & Korelace se stavem projektu",
                                color = OmnisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_close_summary_report")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = OmnisTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Header Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricChip(
                        title = "Harmonie",
                        value = "${(composite * 100).toInt()}%",
                        color = OmnisEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "Odolnost",
                        value = "${(systemicAnalysis.systemicResilience * 100).toInt()}%",
                        color = if (systemicAnalysis.systemicResilience < 0.45f) OmnisRed else OmnisCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "Úzké hrdlo",
                        value = "${systemicAnalysis.bottleneckDomain} ${(systemicAnalysis.bottleneckValue * 100).toInt()}%",
                        color = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisAmber,
                        modifier = Modifier.weight(1.2f)
                    )
                    MetricChip(
                        title = "Pákový bod",
                        value = systemicAnalysis.leverageDomain,
                        color = OmnisViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = OmnisBgDark,
                    contentColor = OmnisCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "1. Stav projektu",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "2. 8D Dimenze",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "3. Korelace & Páka",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ProjectStatusTabContent(
                            records = records,
                            latestRecord = latestRecord,
                            composite = composite,
                            systemicAnalysis = systemicAnalysis,
                            activePriorityProfile = activePriorityProfile,
                            dimensionAuditList = dimensionAuditList
                        )
                        1 -> DimensionsTabContent(
                            dimensionAuditList = dimensionAuditList
                        )
                        2 -> CorrelationsTabContent(
                            systemicAnalysis = systemicAnalysis,
                            composite = composite,
                            dimensionAuditList = dimensionAuditList
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = OmnisBorderDark, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Markdown
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fullMarkdownReport))
                            Toast.makeText(context, "Souhrnná zpráva zkopírována do schránky (Markdown)", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OmnisCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("btn_copy_summary_report")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopírovat MD", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Direct Mitigate in Chat
                    Button(
                        onClick = {
                            val mitigationPrompt = "PROVEĎ SYNTÉZU A STRATEGICKOU INTERVENCI DLE SOUHRNNÉ 8D ZPRÁVY: " +
                                "Integrální index harmonie je ${(composite * 100).toInt()}%, systémová odolnost ${(systemicAnalysis.systemicResilience * 100).toInt()}%. " +
                                "Úzké hrdlo je v doméně ${systemicAnalysis.bottleneckDomain} (${(systemicAnalysis.bottleneckValue * 100).toInt()}%) " +
                                "a optimální bod páky dle Donella Meadows je ${systemicAnalysis.leverageDomain}. " +
                                "Aktuální stav projektu obsahuje ${records.size} záznamů (poslední #${latestRecord?.id ?: "N/A"}). " +
                                "Navrhni a proveď konkrétní kroky pro harmonizaci a posílení stability."

                            if (onDirectMitigate != null) {
                                onDirectMitigate(mitigationPrompt)
                                onDismiss()
                            } else {
                                clipboardManager.setText(AnnotatedString(mitigationPrompt))
                                Toast.makeText(context, "Intervenční prompt zkopírován do schránky", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.3f).testTag("btn_apply_summary_intervention")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Analyzovat v Chatu", color = Color.Black, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisBorderDark),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_close_report_modal")
                    ) {
                        Text("Zavřít", color = Color.White, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = OmnisBgDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = OmnisTextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProjectStatusTabContent(
    records: List<OmnisRecord>,
    latestRecord: OmnisRecord?,
    composite: Float,
    systemicAnalysis: OmnisCorrelationEngine.SystemicHealthAnalysis,
    activePriorityProfile: String,
    dimensionAuditList: List<DimensionAuditItem>
) {
    val assistantCount = records.count { it.role == "assistant" }
    val userCount = records.count { it.role == "user" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Project Overview Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisBgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                            Text("AKTIVNÍ PROJEKTOVÁ TRAJEKTORIE", color = OmnisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OmnisViolet.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PROFIL: $activePriorityProfile",
                                color = OmnisViolet,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Projekt aktuálně eviduje ${records.size} kognitivních entit v lokální Room paměti ($assistantCount AI odpovědí, $userCount uživatelských promptů). " +
                                "Tenzorová rovnováha vykazuje ${(composite * 100).toInt()}% celkovou integritu.",
                        color = OmnisTextLight,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (latestRecord != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = OmnisBorderDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(OmnisViolet, CircleShape))
                            Text("Poslední interakce (#${latestRecord.id} • ${latestRecord.role.uppercase()}):", color = OmnisViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = "\"${latestRecord.content.take(150)}${if (latestRecord.content.length > 150) "..." else ""}\"",
                            color = OmnisTextMuted,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Status Assessment Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (systemicAnalysis.isCriticalFailure) OmnisRed.copy(alpha = 0.12f) else OmnisEmerald.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (systemicAnalysis.isCriticalFailure) OmnisRed.copy(alpha = 0.5f) else OmnisEmerald.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (systemicAnalysis.isCriticalFailure) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (systemicAnalysis.isCriticalFailure) "DETEKOVÁNO SYSTÉMOVÉ RIZIKO PROJEKTU" else "PROJEKT JE V ROVNOVÁŽNÉM STAVU",
                            color = if (systemicAnalysis.isCriticalFailure) OmnisRed else OmnisEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (systemicAnalysis.isCriticalFailure) {
                            "Doména ${systemicAnalysis.bottleneckDomain} dosáhla kritického propadu (${(systemicAnalysis.bottleneckValue * 100).toInt()}%). " +
                                    "Doporučuje se aplikovat intervenci přes pákový bod ${systemicAnalysis.leverageDomain} před pokračováním ve vývoji."
                        } else {
                            "Všechny klíčové dimenze se drží nad bezpečnostními prahy. Úzké hrdlo projektu je ${systemicAnalysis.bottleneckDomain}, avšak nepředstavuje bezprostřední hrozbu dominového selhání."
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 8D Dimension Snapshot Grid
        item {
            Text("RYCHLÝ PŘEHLED 8D SKÓRE", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            dimensionAuditList.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPair.forEach { dim ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = OmnisBgDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, dim.color.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(dim.color, CircleShape))
                                    Text(dim.key, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Text("${(dim.value * 100).toInt()}%", color = dim.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionsTabContent(
    dimensionAuditList: List<DimensionAuditItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dimensionAuditList, key = { it.key }) { dim ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisBgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, dim.color.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Row 1: Header + Value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(8.dp).background(dim.color, CircleShape))
                            Text(
                                text = "${dim.key.uppercase()} • ${dim.name}",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (dim.isFixed) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                                ) {
                                    Text("LOCK", color = Color(0xFFFFD700), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dim.delta?.let { d ->
                                val deltaCol = if (d > 0.02f) OmnisEmerald else if (d < -0.02f) OmnisRed else OmnisTextMuted
                                val sign = if (d > 0) "+" else ""
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = deltaCol.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Δ $sign${(d * 100).toInt()}%",
                                        color = deltaCol,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = dim.statusColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, dim.statusColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${(dim.value * 100).toInt()}%",
                                    color = dim.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dim.description, color = OmnisTextMuted, fontSize = 10.sp, lineHeight = 14.sp)

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { dim.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = dim.color,
                        trackColor = OmnisBorderDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Synergie: ${dim.topSynergies}",
                            color = OmnisEmerald.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Frikce: ${dim.topFrictions}",
                            color = OmnisRed.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrelationsTabContent(
    systemicAnalysis: OmnisCorrelationEngine.SystemicHealthAnalysis,
    composite: Float,
    dimensionAuditList: List<DimensionAuditItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Meadows Leverage Point Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisEmerald.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(16.dp))
                        Text("BOD NEJVYŠŠÍ PÁKY (DONELLA MEADOWS LEVERAGE)", color = OmnisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Algoritmus simuloval kaskádové přelévání tenzorů. Cílený impuls v doméně ${systemicAnalysis.leverageDomain} " +
                                "přinese maximální pozitivní multiplikační efekt pro celý systém a pomůže kompenzovat úzké hrdlo (${systemicAnalysis.bottleneckDomain}).",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Bottleneck Assessment Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisBgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
                        Text("LEONTIEFŮV PRINCIP MINIMA (ÚZKÉ HRDLO)", color = OmnisAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Celková systémová propustnost a stabilita projektu je limitována nejslabším článkem: ${systemicAnalysis.bottleneckDomain} (${(systemicAnalysis.bottleneckValue * 100).toInt()}%). " +
                                "Zatímco aritmetický průměr činí ${(systemicAnalysis.arithmeticMean * 100).toInt()}%, harmonický průměr penalizující slabiny je ${(systemicAnalysis.harmonicMean * 100).toInt()}%.",
                        color = OmnisTextLight,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Top Systemic Synergies & Frictions
        item {
            Text("KLÍČOVÉ KŘÍŽOVÉ VAZBY MEZI DOMÉNAMI", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))

            val keyPairCorrelations = listOf(
                Triple("Law & Sec", "+82%", "Kritická synergie: regulace (GDPR, NIS2, EU AI Act) přímo vynucuje bezpečnostní kontroly."),
                Triple("Psych & Soc", "+85%", "Sociální rezonance: důvěra operátora posiluje stabilitu týmu a adopci systému."),
                Triple("Sys & Sec", "+78%", "Architektonická synergie: čistá modularita umožňuje efektivní Zero-Trust segmentaci."),
                Triple("Eco & Phys", "+71%", "Termodynamika: energetická efektivita přímo snižuje ekologický otisk."),
                Triple("Econ vs Eco", "-54%", "Systémová frikce: krátkodobé snižování nákladů může kolidovat s investicemi do ekologie."),
                Triple("Psych vs Sec", "-48%", "Tenzní pole: extrémně striktní bezpečnostní restrikce zvyšují kognitivní tření uživatele.")
            )

            keyPairCorrelations.forEach { (pair, strength, explanation) ->
                val isPositive = strength.startsWith("+")
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OmnisBgDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPositive) OmnisEmerald.copy(alpha = 0.3f) else OmnisRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                strength,
                                color = if (isPositive) OmnisEmerald else OmnisRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(explanation, color = OmnisTextMuted, fontSize = 9.5.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
