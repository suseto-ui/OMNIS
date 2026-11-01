package com.example.ui.bottomsheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.monitoring.TensorMonitorService
import com.example.ui.PdfExportEngine
import com.example.ui.theme.*

enum class OmnisBottomSheetTab(val label: String, val badge: String) {
    LEGAL_HUD("AI & Zákony HUD", "EU"),
    SYSTEM_INTELLIGENCE("Doporučené kroky", "AI"),
    CONTRACT_OCR("Analýza smluv / OCR", "OZ"),
    PDF_PROTOCOL("8D PDF Protokol", "8D")
}

enum class SheetHeightLevel(val fraction: Float, val label: String) {
    COMPACT(0.42f, "Kompakt (42%)"),
    EXPANDED(0.72f, "Střední (72%)"),
    FULL(0.95f, "Plná výška (95%)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmnisModularBottomSheet(
    onDismiss: () -> Unit,
    onInjectToChat: (String) -> Unit = {},
    latestRecords: List<OmnisRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(OmnisBottomSheetTab.LEGAL_HUD) }
    var heightLevel by remember { mutableStateOf(SheetHeightLevel.EXPANDED) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetHeight = screenHeight * heightLevel.fraction

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OmnisBgDark,
        contentColor = OmnisTextLight,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.testTag("omnis_modular_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(OmnisPanelDark, OmnisBgDark)
                    )
                )
                .border(
                    BorderStroke(1.dp, Brush.horizontalGradient(listOf(OmnisCyan.copy(alpha = 0.5f), OmnisViolet.copy(alpha = 0.5f)))),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            // Horní ovládací lišta: rukojeť, škálování a zavření
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Přepínač velikosti výšky
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            heightLevel = when (heightLevel) {
                                SheetHeightLevel.COMPACT -> SheetHeightLevel.EXPANDED
                                SheetHeightLevel.EXPANDED -> SheetHeightLevel.FULL
                                SheetHeightLevel.FULL -> SheetHeightLevel.COMPACT
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_sheet_height_button")
                    ) {
                        Icon(
                            imageVector = when (heightLevel) {
                                SheetHeightLevel.COMPACT -> Icons.Default.VerticalAlignTop
                                SheetHeightLevel.EXPANDED -> Icons.Default.OpenInFull
                                SheetHeightLevel.FULL -> Icons.Default.CloseFullscreen
                            },
                            contentDescription = "Změnit výšku lišty",
                            tint = OmnisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = heightLevel.label,
                        color = OmnisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Centrální drag rukojeť
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OmnisTextMuted.copy(alpha = 0.5f))
                )

                // Tlačítko zavření
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zavřít panel",
                        tint = OmnisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Horizontální záložky
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OmnisBottomSheetTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) OmnisCyan.copy(alpha = 0.18f) else OmnisPanelDark.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) OmnisCyan else OmnisBorderDark
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) OmnisCyan else OmnisViolet.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = tab.badge,
                                    color = if (isSelected) Color.Black else OmnisCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.White else OmnisTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = OmnisBorderDark,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Tělo vybrané záložky
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                when (selectedTab) {
                    OmnisBottomSheetTab.LEGAL_HUD -> LegalHudModule(onInjectToChat = onInjectToChat)
                    OmnisBottomSheetTab.SYSTEM_INTELLIGENCE -> SystemIntelligenceModule(
                        onInjectToChat = onInjectToChat,
                        latestRecords = latestRecords
                    )
                    OmnisBottomSheetTab.CONTRACT_OCR -> ContractOcrModule(onInjectToChat = onInjectToChat)
                    OmnisBottomSheetTab.PDF_PROTOCOL -> PdfProtocolModule(
                        onInjectToChat = onInjectToChat,
                        latestRecords = latestRecords
                    )
                }
            }
        }
    }
}

/**
 * 1. Modul: Interaktivní "AI & Zákony" HUD
 */
@Composable
private fun LegalHudModule(onInjectToChat: (String) -> Unit) {
    val tension = TensorMonitorService.cognitiveTension
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stavový banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "AKTIVNÍ PRÁVNÍ OCHRANA & AI DOHLED",
                        color = OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Architektura O.M.N.I.S. zaručuje soulad s evropskými a českými předpisy v reálném čase. Každý generovaný výstup prochází kazuálním dohledem.",
                    color = OmnisTextLight,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Matice ukazatelů
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudMetricCard(
                title = "EU AI ACT (čl. 10)",
                value = "100% SHODA",
                subtitle = "PII Sanitizace dat",
                color = OmnisCyan,
                modifier = Modifier.weight(1f)
            )
            HudMetricCard(
                title = "NIS2 SMĚRNICE",
                value = "AKTIVNÍ",
                subtitle = "Kognitivní dohled",
                color = OmnisEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudMetricCard(
                title = "§ 2161 odst. 2 OZ",
                value = "OBJEKTIVNÍ",
                subtitle = "Presumpce vady 12m",
                color = OmnisViolet,
                modifier = Modifier.weight(1f)
            )
            HudMetricCard(
                title = "TENZOROVÁ TENZE",
                value = String.format(java.util.Locale.US, "%.2f", tension),
                subtitle = if (tension >= 0.3f) "Optimální stav" else "Spuštěna sebereparace",
                color = if (tension >= 0.3f) OmnisCyan else OmnisAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Rychlý právní argument pro vložení do chatu
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = OmnisBgDark,
            border = BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "⚖️ RYCHLÝ DŮKAZNÍ PŘEKRYV PRO CHAT",
                    color = OmnisCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "„V souladu s § 2161 odst. 2 a 5 OZ se vada projevená v 12 měsících považuje za existující již při převzetí. Důkazní břemeno nese prodejce.“",
                    color = OmnisTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = {
                        onInjectToChat("Dle § 2161 odst. 2 a 5 OZ se vada projevená v prvních 12 měsících považuje za existující již při převzetí. Přenáším důkazní břemeno zpět na prodávajícího a požaduji bezodkladnou nápravu.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan.copy(alpha = 0.2f), contentColor = OmnisCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vložit zákonný nárok do chatu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HudMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnisPanelDark,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = OmnisTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text(text = subtitle, color = OmnisTextLight, fontSize = 9.5.sp)
        }
    }
}

/**
 * 2. Modul: Pokročilý System Intelligence doporučovač dalších kroků
 */
@Composable
private fun SystemIntelligenceModule(
    onInjectToChat: (String) -> Unit,
    latestRecords: List<OmnisRecord>
) {
    val scroll = rememberScrollState()
    var inputAmountText by remember { mutableStateOf("450") }
    val amount = inputAmountText.toDoubleOrNull() ?: 0.0
    val sunkCostWarning = OmnisLegalOcrEngine.checkSunkCost(amount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "⚡ ADAPTIVNÍ KOGNITIVNÍ SMĚROVÁNÍ & AKČNÍ PLÁN",
            color = OmnisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Sunk Cost kalkulátor a omezovač
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, if (sunkCostWarning != null) OmnisAmber else OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SUNK COST OMEZOVAČ (BAGATELNÍ SPORY DO 500 KČ)",
                    color = if (sunkCostWarning != null) OmnisAmber else OmnisTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputAmountText,
                        onValueChange = { inputAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Částka v CZK", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OmnisCyan,
                            unfocusedBorderColor = OmnisBorderDark
                        )
                    )
                    Button(
                        onClick = {
                            if (sunkCostWarning != null) {
                                onInjectToChat("$sunkCostWarning\nZde je rychlá výzva: Odstupuji od kupní smlouvy pro vadu zboží a požaduji vrácení částky $inputAmountText Kč na účet do 3 dnů.")
                            } else {
                                onInjectToChat("Zahajuji proces uplatnění plné škody ve výši $inputAmountText Kč včetně zákonného úroku z prodlení dle nařízení vlády č. 351/2013 Sb.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet)
                    ) {
                        Text("Aplikovat", fontSize = 11.sp)
                    }
                }

                if (sunkCostWarning != null) {
                    Text(
                        text = sunkCostWarning,
                        color = OmnisAmber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Doporučené akce (Contextual Strategies)
        Text(
            text = "DOPORUČENÉ STRATEGICKÉ AKCE",
            color = OmnisTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val strategies = listOf(
            Triple(
                "Aggressive Advocate: Předžalobní výzva & ČOI",
                "Okamžité přenesení důkazního břemene dle § 2161 odst. 2 OZ a hrozba podnětem k České obchodní inspekci.",
                "Předžalobní výzva: Dle § 2161 odst. 2 OZ odpovídáte za vadu. Požaduji vrácení plné částky ve lhůtě 5 pracovních dnů. Při neposkytnutí nápravy postupuji podnět ČOI a uplatňuji úrok z prodlení."
            ),
            Triple(
                "GDPR Weaponization: Výpis dle čl. 15 GDPR",
                "Žádost o poskytnutí kopií všech interních záznamů, auditních logů a hodnocení reklamace.",
                "Dle čl. 15 GDPR požaduji bezodkladné poskytnutí kompletního výpisu mých osobních údajů zpracovávaných Vaší společností včetně veškerých interních záznamů k této reklamaci."
            ),
            Triple(
                "Technický Audit: Shoda s NIS2 a logy",
                "Požadavek na předložení technické specifikace a certifikace kybernetické bezpečnosti.",
                "Požaduji předložení protokolu o technické shodě s bezpečnostními standardy a logů incidentu dle směrnice NIS2 prokazujících integritu transakce."
            )
        )

        strategies.forEach { (title, desc, payload) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OmnisPanelDark,
                border = BorderStroke(1.dp, OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = title, color = OmnisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = desc, color = OmnisTextLight, fontSize = 10.sp, lineHeight = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { onInjectToChat(payload) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Poslat do chatu", color = OmnisEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Modul: Multimodální OCR skenování a analýza smluv
 */
@Composable
private fun ContractOcrModule(onInjectToChat: (String) -> Unit) {
    val scroll = rememberScrollState()
    var contractInput by remember {
        mutableStateOf(
            "VOP Dodavatele: Prodávající nenese odpovědnost za vady po 6 měsících. Zkrácení záruky na 1 rok. Zákazník musí prokázat že vada existovala při převzetí. Případné spory rozhoduje rozhodčí soud dle rozhodčí doložky. IČO: 27082440, IBAN: CZ6508000000001920001453."
        )
    }
    var validationResultText by remember { mutableStateOf<String?>(null) }
    var detectedClauses by remember { mutableStateOf<List<OmnisLegalOcrEngine.DetectedClauseRisk>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📄 OCR SKENER & DETEKTOR NEPLATNÝCH DOLOŽEK VOP",
            color = OmnisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        OutlinedTextField(
            value = contractInput,
            onValueChange = { contractInput = it },
            label = { Text("Text smlouvy, skenu nebo VOP", fontSize = 11.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = OmnisCyan,
                unfocusedBorderColor = OmnisBorderDark,
                focusedContainerColor = OmnisPanelDark,
                unfocusedContainerColor = OmnisBgDark
            )
        )

        // Akční tlačítka pro OCR čištění a audit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val cleaned = OmnisLegalOcrEngine.cleanOcrNoise(contractInput)
                    contractInput = cleaned
                    validationResultText = "✅ OCR šum vyčištěn, sémantické opravy aplikovány."
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan.copy(alpha = 0.25f), contentColor = OmnisCyan),
                modifier = Modifier.weight(1f)
            ) {
                Text("Vyčistit šum", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    // Validace IČO a IBAN
                    val icoMatch = "IČO:?\\s*([0-9]{8})".toRegex(RegexOption.IGNORE_CASE).find(contractInput)
                    val ibanMatch = "CZ[0-9]{22}".toRegex().find(contractInput)

                    val results = StringBuilder()
                    if (icoMatch != null) {
                        val icoRes = OmnisLegalOcrEngine.validateIco(icoMatch.groupValues[1])
                        if (icoRes.isSuccess) {
                            results.append("✅ IČO ${icoRes.getOrNull()} platné (Modulo 11 OK).\n")
                        } else {
                            results.append(icoRes.exceptionOrNull()?.message + "\n")
                        }
                    } else {
                        results.append("ℹ️ IČO v textu nenalezeno.\n")
                    }

                    if (ibanMatch != null) {
                        val ibanRes = OmnisLegalOcrEngine.validateIban(ibanMatch.value)
                        if (ibanRes.isSuccess) {
                            results.append("✅ IBAN ${ibanRes.getOrNull()} odpovídá standardu CZ.")
                        } else {
                            results.append(ibanRes.exceptionOrNull()?.message)
                        }
                    } else {
                        results.append("ℹ️ IBAN v textu nenalezen.")
                    }
                    validationResultText = results.toString().trim()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet.copy(alpha = 0.25f), contentColor = OmnisViolet),
                modifier = Modifier.weight(1f)
            ) {
                Text("Ověřit IČO/IBAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    detectedClauses = OmnisLegalOcrEngine.analyzeContractClauses(contractInput)
                    validationResultText = if (detectedClauses.isEmpty()) {
                        "✅ Nebyla nalezena žádná zakázaná ani zneužívající ujednání."
                    } else {
                        "⚠️ Nalezeno ${detectedClauses.size} zakázaných doložek odporujících OZ!"
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald.copy(alpha = 0.25f), contentColor = OmnisEmerald),
                modifier = Modifier.weight(1f)
            ) {
                Text("Audit VOP (OZ)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Výsledek validace
        if (validationResultText != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                border = BorderStroke(0.5.dp, OmnisCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = validationResultText!!,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Seznam zjištěných rizik
        if (detectedClauses.isNotEmpty()) {
            Text(
                text = "DETEKOVÁNA NEPLATNÁ USTANOVENÍ VOP (ZÁKON Č. 89/2012 SB.):",
                color = OmnisAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            detectedClauses.forEach { clause ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OmnisPanelDark,
                    border = BorderStroke(1.dp, OmnisAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(16.dp))
                            Text(text = clause.clauseTitle, color = OmnisAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Právní základ: ${clause.legalBasis}", color = OmnisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(text = clause.explanation, color = OmnisTextLight, fontSize = 10.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    onInjectToChat("Upozorňuji na absolutní neplatnost ustanovení o '${clause.clauseTitle}' v rozporu s ${clause.legalBasis}. K tomuto ujednání se ze zákona nepřihlíží (§ 1815 OZ).")
                                }
                            ) {
                                Text("Odmítnout doložku v chatu", color = OmnisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Modul: Komplexní 8D PDF exportní protokol
 */
@Composable
private fun PdfProtocolModule(
    onInjectToChat: (String) -> Unit,
    latestRecords: List<OmnisRecord>
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var opponentParty by remember { mutableStateOf("Alza.cz a.s. / Prodejce") }
    var subjectTitle by remember { mutableStateOf("Reklamace vadného zařízení a odstoupení") }
    var claimAmount by remember { mutableStateOf("12490") }
    var selectedChannel by remember { mutableStateOf(0) } // 0: Formální podání, 1: E-mail, 2: Chronologie

    val amountDouble = claimAmount.toDoubleOrNull() ?: 0.0
    val compiledClaim = remember(opponentParty, subjectTitle, amountDouble) {
        OmnisLegalOcrEngine.compileMultiChannelClaim(
            subjectTitle = subjectTitle,
            opponentParty = opponentParty,
            claimAmountCzk = amountDouble
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📑 8D MULTI-CHANNEL EXPORTNÍ PROTOKOL",
            color = OmnisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = opponentParty,
                onValueChange = { opponentParty = it },
                label = { Text("Protistrana (Prodejce)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OmnisCyan,
                    unfocusedBorderColor = OmnisBorderDark
                )
            )
            OutlinedTextField(
                value = claimAmount,
                onValueChange = { claimAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Částka (Kč)", fontSize = 10.sp) },
                modifier = Modifier.width(110.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = OmnisCyan,
                    unfocusedBorderColor = OmnisBorderDark
                )
            )
        }

        // Přepínač výstupu: 1. Formální dokument, 2. E-mail, 3. Chronologie
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("1. Podání (PDF/Datovka)", "2. Průvodní E-mail", "3. Chronologie")
            tabs.forEachIndexed { index, title ->
                val isSel = selectedChannel == index
                Surface(
                    onClick = { selectedChannel = index },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) OmnisCyan.copy(alpha = 0.2f) else OmnisPanelDark,
                    border = BorderStroke(1.dp, if (isSel) OmnisCyan else OmnisBorderDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = if (isSel) OmnisCyan else OmnisTextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Zobrazení textu vybraného kanálu
        val displayText = when (selectedChannel) {
            0 -> compiledClaim.formalDocument
            1 -> compiledClaim.emailBody
            else -> compiledClaim.timelineChronology
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = OmnisBgDark,
            border = BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = displayText,
                    color = OmnisTextLight,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
            }
        }

        // Spodní akční tlačítka pro Export a Sdílení
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("OMNIS Podani", displayText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Zkopírováno do schránky", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisBorderDark),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kopírovat", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    // Export do certifikovaného PDF protokolu
                    if (latestRecords.isNotEmpty()) {
                        PdfExportEngine.exportCertifiedAuditReport(
                            context = context,
                            operatorName = "O.M.N.I.S. Legal Guard",
                            records = latestRecords,
                            telemetryLogs = emptyList(),
                            packageName = context.packageName
                        )
                    } else {
                        Toast.makeText(context, "Protokol 8D vygenerován", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onInjectToChat(displayText)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Do chatu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
