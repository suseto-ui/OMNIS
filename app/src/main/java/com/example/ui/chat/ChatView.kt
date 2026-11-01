package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.bottomsheet.OmnisModularBottomSheet
import com.example.ui.components.RobustnessGuardModal
import com.example.ui.theme.*

@Composable
fun ChatView(
    viewModel: OmnisViewModel,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val records by viewModel.records.collectAsState()
    val cognitiveError by viewModel.cognitiveError.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showModularSheet by remember { mutableStateOf(false) }
    val showLegalBar by ChatDisplayPreferences.showLegalStatusBarAboveInput.collectAsStateWithLifecycle()
    val cyberGlassEnabled by ChatDisplayPreferences.cyberGlassEffectEnabled.collectAsStateWithLifecycle()
    
    if (cognitiveError != null) {
        RobustnessGuardModal(
            errorMessage = cognitiveError!!,
            onDismiss = { viewModel.dismissError() },
            onSelfRepair = { viewModel.triggerSelfRepair() }
        )
    }

    if (showModularSheet) {
        OmnisModularBottomSheet(
            onDismiss = { showModularSheet = false },
            onInjectToChat = { injected ->
                inputText = injected
                showModularSheet = false
            },
            latestRecords = records
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OmnisBgDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (records.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(OmnisCyan.copy(alpha = 0.3f), OmnisViolet.copy(alpha = 0.3f))
                                    )
                                )
                                .border(1.5.dp, OmnisCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ω", color = OmnisCyan, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "O.M.N.I.S. KOGNITIVNÍ PLATFORMA",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OmnisEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Systém aktivní • Prahová hodnota 0.75 • AI Gemini / Offline Fallback",
                                    color = OmnisEmerald,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Readiness Status Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = OmnisPanelDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "💡 RYCHLÉ SPUŠTĚNÍ & DOTAZY",
                                    color = OmnisCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                val samplePrompts = listOf(
                                    "Zahájit celkovou analýzu stavu O.M.N.I.S. infrastruktury",
                                    "Jaké jsou mé aktivní cíle a stav 8D matice?",
                                    "Srovnávací vyhodnocení ekonomických a bezpečnostních rizik",
                                    "Test autonomních jističů a bezpečnostních bariér"
                                )

                                samplePrompts.forEach { prompt ->
                                    Surface(
                                        onClick = {
                                            viewModel.sendQuery(prompt)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = OmnisBgDark,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = OmnisCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = prompt,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // E-waste Super-Prompt Trigger
                                val context = LocalContext.current
                                Surface(
                                    onClick = {
                                        val eWastePrompt = "Anatomie perfektního super-promptu. Definice role a expertizy: Modelu nastavíte identitu špičkového hutního inženýra, chemického prospektora a analytika recyklačního průmyslu e-waste. Kategorizace vstupního materiálu: Zamezíte vynechání jakékoliv kategorie – prompt musí striktně požadovat pokrytí od spotřební elektroniky přes průmyslová zařízení až po telekomunikace. Specifikace sledovaných kovů: Explicitně vyjmenujete drahé i vzácné kovy (Au, Ag, Pd, Pt, Cu, Ta, Nd, In), aby model nezůstal jen u zlata. Požadovaná datová struktura: Nařídíte přesný formát výstupu (metrické jednotky, konverze g/t, g/ks, procentuální podíly a tržní ocenění). Metodika získávání: Vynutíte detailní přehled separačních a rafinačních technik (pyrometalurgie, hydrometalurgie, biohydrometalurgie, elektrochemie) včetně chemických činidel a bezpečnosti. Bariéry a negativní pravidla (Negative Constraints): Zákaz zkratek jako \"a tak dále\", \"další běžné komponenty\", povinnost rozepsat vše do nejmenších detailů."
                                        com.example.monitoring.UserActivityLogger.getInstance(context).logInteraction("user", "ChatView", "Spuštění E-Waste Super-Promptu")
                                        viewModel.sendQuery(eWastePrompt)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = OmnisBgDark,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, OmnisViolet),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            tint = OmnisViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "SPUSTIT E-WASTE SUPER-PROMPT",
                                            color = OmnisViolet,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(records) { record ->
                    ChatMessageItem(
                        record = record,
                        onQuickQuery = { query ->
                            viewModel.sendQuery(query)
                        },
                        onSpeak = onSpeak
                    )
                }
            }
        }

        // Minimalistický prolínající se právní stav nad polem vstupu
        if (showLegalBar) {
            ChatLegalStatusBar(
                onOpenAudit = { showModularSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // Plynulé plovoucí Cyber-Glass pole vstupu
        Surface(
            color = if (cyberGlassEnabled) OmnisPanelDark.copy(alpha = 0.90f) else OmnisPanelDark,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (cyberGlassEnabled) OmnisCyan.copy(alpha = 0.45f) else OmnisBorderDark,
                        OmnisBorderDark
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Rychlé tlačítko pro otevření modulárního BottomSheetu (HUD, OCR, Doporučení, 8D)
                IconButton(
                    onClick = { showModularSheet = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OmnisBgDark.copy(alpha = 0.7f))
                        .border(1.dp, OmnisCyan.copy(alpha = 0.5f), CircleShape)
                        .testTag("open_modular_bottom_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Otevřít kognitivní a právní moduly",
                        tint = OmnisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Zadejte dotaz pro O.M.N.I.S...",
                            color = OmnisTextMuted,
                            fontSize = 13.5.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OmnisCyan,
                        unfocusedBorderColor = OmnisBorderDark,
                        focusedContainerColor = OmnisBgDark.copy(alpha = 0.65f),
                        unfocusedContainerColor = OmnisBgDark.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendQuery(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) {
                                Brush.linearGradient(listOf(OmnisCyan, OmnisViolet))
                            } else {
                                Brush.linearGradient(listOf(OmnisBgDark, OmnisBorderDark))
                            }
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat",
                        tint = if (inputText.isNotBlank()) Color.Black else OmnisTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
