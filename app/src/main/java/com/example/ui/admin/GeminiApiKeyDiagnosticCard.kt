package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * GeminiApiKeyDiagnosticCard:
 * Vizuální diagnostický a konfigurační monitor pro 3 rotující Gemini API klíče.
 * Umožňuje sledovat stav injekce ze Secrets panelu (Slot 1, 2, 3), Round-Robin rotaci,
 * testovat ping a zadat/aktualizovat klíče přímo v mobilní aplikaci.
 */
@Composable
fun GeminiApiKeyDiagnosticCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRunningPing by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var showFullLogs by remember { mutableStateOf(true) }
    var showManualKeyEntry by remember { mutableStateOf(false) }

    // Init custom keys from preferences if available
    LaunchedEffect(Unit) {
        OmnisGeminiClient.KeyPool.initFromContext(context)
    }

    val activeKeyName by OmnisGeminiClient.activeKeyName.collectAsStateWithLifecycle()
    val activeModel by OmnisGeminiClient.activeModel.collectAsStateWithLifecycle()
    val rotationStats by OmnisGeminiClient.rotationStats.collectAsStateWithLifecycle()
    val keyPoolLog by OmnisGeminiClient.keyPoolLog.collectAsStateWithLifecycle()
    val lastOnlineError by OmnisGeminiClient.lastOnlineError.collectAsStateWithLifecycle()

    val keyValidations = remember(activeKeyName) { GeminiDiagnosticService.validateAllKeysFormat() }
    val primaryValidation = keyValidations.firstOrNull() ?: remember { GeminiDiagnosticService.validateBuildConfigKeyFormat() }

    val report by GeminiDiagnosticService.latestReport.collectAsStateWithLifecycle()
    val logs by GeminiDiagnosticService.pingLogs.collectAsStateWithLifecycle()

    var inputKey1 by remember { mutableStateOf("") }
    var inputKey2 by remember { mutableStateOf("") }
    var inputKey3 by remember { mutableStateOf("") }
    var saveFeedbackMsg by remember { mutableStateOf<String?>(null) }

    // Automatický start síťové diagnostiky
    LaunchedEffect(Unit) {
        if (report == null) {
            isRunningPing = true
            GeminiDiagnosticService.diagnoseApiKey()
            isRunningPing = false
        }
    }

    val warningColor by animateColorAsState(
        targetValue = when (primaryValidation.warningLevel) {
            WarningLevel.OK -> OmnisEmerald
            WarningLevel.WARNING -> OmnisAmber
            WarningLevel.CRITICAL -> if (OmnisGeminiClient.KeyPool.hasAnyKey()) OmnisEmerald else OmnisRed
        },
        label = "warningColor"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, warningColor.copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_api_key_diagnostic_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. HLAVIČKA MODULU & CELKOVÝ STAV
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = warningColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "3-KLÍČOVÝ ROTUJÍCÍ FOND GEMINI API",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Aktivní Round-Robin & Failover pro telefon",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = warningColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, warningColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (OmnisGeminiClient.KeyPool.hasAnyKey()) "3-KEY POOL: AKTIVNÍ" else "CHYBÍ KLÍČE",
                        color = warningColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. STATUS PŘEHLED KLÍČŮ (SLOT 1, SLOT 2, SLOT 3)
            Text(
                text = "STATUS ROTUJÍCÍCH KLÍČŮ VE FONDU:",
                color = OmnisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            keyValidations.forEach { valItem ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OmnisBgDark,
                    border = BorderStroke(
                        1.dp,
                        if (valItem.isPresent && !valItem.isPlaceholder) OmnisEmerald.copy(alpha = 0.5f) else OmnisRed.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (valItem.isPresent && !valItem.isPlaceholder) OmnisEmerald else OmnisRed,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = valItem.slotName,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = if (valItem.isPresent && !valItem.isPlaceholder) valItem.maskedKey else "[NENÍ NASTAVEN]",
                            color = if (valItem.isPresent && !valItem.isPlaceholder) OmnisCyan else OmnisTextMuted,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AKTIVNÍ KLÍČ & MODEL
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = OmnisViolet.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "⚡ Aktuálně aktivní klíč: $activeKeyName",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "🤖 Aktivní model: $activeModel | Režim: Round-Robin (Rotace při každém dotazu)",
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (lastOnlineError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Poslední chybový log: $lastOnlineError",
                            color = OmnisAmber,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TLAČÍTKO PRO RUČNÍ ZADÁNÍ / EDITACI KLÍČŮ V TELEFONU
            OutlinedButton(
                onClick = { showManualKeyEntry = !showManualKeyEntry },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (showManualKeyEntry) Icons.Default.ExpandLess else Icons.Default.Key,
                    contentDescription = null,
                    tint = OmnisCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showManualKeyEntry) "SKRÝT RUČNÍ ZADÁNÍ KLÍČŮ" else "ZADAT / UPRAVIT 3 KLÍČE V TELEFONU",
                    color = OmnisCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            AnimatedVisibility(visible = showManualKeyEntry) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Zde můžete vložit své 3 Gemini API klíče (uloží se trvale do zařízení):",
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputKey1,
                        onValueChange = { inputKey1 = it },
                        placeholder = { Text("Klíč 1 (např. AIzaSy...)", fontSize = 10.sp, color = OmnisTextMuted) },
                        label = { Text("Slot 1 (GEMINI_API_KEY)", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputKey2,
                        onValueChange = { inputKey2 = it },
                        placeholder = { Text("Klíč 2 (např. AIzaSy...)", fontSize = 10.sp, color = OmnisTextMuted) },
                        label = { Text("Slot 2 (GEMINI_API_KEY_2)", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputKey3,
                        onValueChange = { inputKey3 = it },
                        placeholder = { Text("Klíč 3 (např. AIzaSy...)", fontSize = 10.sp, color = OmnisTextMuted) },
                        label = { Text("Slot 3 (GEMINI_API_KEY_3)", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            OmnisGeminiClient.KeyPool.saveCustomKeys(
                                context,
                                inputKey1.ifBlank { null },
                                inputKey2.ifBlank { null },
                                inputKey3.ifBlank { null }
                            )
                            saveFeedbackMsg = "✅ Všechny 3 klíče byly úspěšně uloženy a rotace byla aktualizována!"
                            coroutineScope.launch {
                                GeminiDiagnosticService.diagnoseApiKey()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmnisEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ULOŽIT A AKTIVOVAT ROTACI V TELEFONU",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (saveFeedbackMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = saveFeedbackMsg!!,
                            color = OmnisEmerald,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. JEDNOKLIKOVÝ PING TEST
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isRunningPing = true
                            GeminiDiagnosticService.pingPrimaryModel(selectedModel)
                            GeminiDiagnosticService.diagnoseApiKey()
                            isRunningPing = false
                        }
                    },
                    enabled = !isRunningPing,
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisViolet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_ping_gemini_primary")
                ) {
                    if (isRunningPing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TESTUJI ROTACI...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OTESTOVAT PING A ROTACI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        selectedModel = when (selectedModel) {
                            "gemini-3.5-flash" -> "gemini-flash-latest"
                            "gemini-flash-latest" -> "gemini-3.1-flash-lite-preview"
                            "gemini-3.1-flash-lite-preview" -> "gemini-3.1-pro-preview"
                            else -> "gemini-3.5-flash"
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = selectedModel.removePrefix("gemini-"),
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. DIAGNOSTICKÝ LOG PŘIPOJENÍ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = OmnisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "DIAGNOSTICKÝ LOG ROTACE (${logs.size})",
                        color = OmnisCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { GeminiDiagnosticService.clearLogs() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Smazat logy",
                                tint = OmnisTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showFullLogs = !showFullLogs },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (showFullLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Zobrazit/skrýt log",
                            tint = OmnisTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedVisibility(visible = showFullLogs) {
                if (logs.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OmnisBgDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Žádné záznamy. Klikněte na 'OTESTOVAT PING A ROTACI'.",
                            color = OmnisTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        logs.take(5).forEach { logEntry ->
                            DiagnosticLogItem(logEntry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticLogItem(entry: GeminiPingLogEntry) {
    val entryColor = when (entry.status) {
        GeminiKeyStatus.VALID_ACTIVE -> OmnisEmerald
        GeminiKeyStatus.CREDITS_DEPLETED_402 -> OmnisAmber
        GeminiKeyStatus.RATE_LIMITED_429 -> OmnisAmber
        GeminiKeyStatus.INVALID_KEY_400_403 -> OmnisRed
        GeminiKeyStatus.SERVER_ERROR_5XX -> OmnisRed
        GeminiKeyStatus.NETWORK_ERROR -> OmnisCyan
        GeminiKeyStatus.NOT_CONFIGURED -> OmnisTextMuted
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, entryColor.copy(alpha = 0.3f)),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.timestamp,
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = entry.targetEndpoint,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = entryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = entry.statusBadge,
                        color = entryColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = entry.details,
                color = OmnisTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )

            if (!entry.payloadSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Payload: ${entry.payloadSnippet}",
                    color = OmnisCyan.copy(alpha = 0.8f),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }
        }
    }
}
