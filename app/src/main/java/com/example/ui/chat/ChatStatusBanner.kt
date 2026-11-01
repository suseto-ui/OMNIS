package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.OmnisGeminiClient
import com.example.ui.theme.OmnisStyleSheet

/**
 * Stavová lišta kognitivní brány Gemini a modelu.
 * Zobrazuje aktuálně běžící model v kaskádě (např. gemini-2.5-flash)
 * a log přepnutí při vyčerpání limitu či chybě.
 */
@Composable
fun ChatStatusBanner(
    userRole: com.example.auth.UserRole = com.example.auth.UserRole.STANDARD_USER,
    userExperienceMode: com.example.ui.UserExperienceMode = com.example.ui.UserExperienceMode.STANDARD,
    modifier: Modifier = Modifier
) {
    val activeModel by OmnisGeminiClient.activeModel.collectAsStateWithLifecycle()
    val modelLogs by OmnisGeminiClient.modelSwitchLog.collectAsStateWithLifecycle()
    val lastOnlineError by OmnisGeminiClient.lastOnlineError.collectAsStateWithLifecycle()

    var showHelpDetail by remember { mutableStateOf(false) }
    var showCascadeDialog by remember { mutableStateOf(false) }

    val isTechnicalMode = userRole == com.example.auth.UserRole.ADMIN_OPERATOR || userExperienceMode == com.example.ui.UserExperienceMode.EXPERT

    if (showCascadeDialog && isTechnicalMode) {
        AlertDialog(
            onDismissRequest = { showCascadeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = OmnisStyleSheet.Colors.CyanAccent
                    )
                    Text(
                        text = "Stav kaskády modelů Gemini",
                        style = OmnisStyleSheet.Typography.headerSection
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aktivní model: $activeModel",
                        color = OmnisStyleSheet.Colors.SuccessEmerald,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(color = OmnisStyleSheet.Colors.BorderMuted)
                    Text(
                        text = "Historie přepnutí kaskády:",
                        color = OmnisStyleSheet.Colors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (modelLogs.isEmpty()) {
                        Text(
                            text = "Dosud nedošlo k žádnému vynucenému fallbacku.",
                            color = OmnisStyleSheet.Colors.TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        modelLogs.takeLast(5).forEach { log ->
                            Text(
                                text = "• $log",
                                color = OmnisStyleSheet.Colors.TextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCascadeDialog = false }) {
                    Text("Zavřít", color = OmnisStyleSheet.Colors.CyanAccent)
                }
            },
            containerColor = OmnisStyleSheet.Colors.DialogBackground
        )
    }

    if (lastOnlineError != null) {
        if (!isTechnicalMode) {
            // Čistý, přívětivý indikátor pro běžného uživatele bez technických detailů a kódů
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .testTag("gemini_status_standard"),
                color = Color.Transparent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OmnisStyleSheet.Colors.SuccessEmerald)
                    )
                    Text(
                        text = "Kognitivní systém O.M.N.I.S. (Záložní režim aktivní)",
                        color = OmnisStyleSheet.Colors.SuccessEmerald.copy(alpha = 0.9f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("gemini_fallback_banner"),
                shape = OmnisStyleSheet.Shapes.card,
                color = OmnisStyleSheet.Colors.PanelBackground,
                border = OmnisStyleSheet.Borders.warning
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = OmnisStyleSheet.Colors.WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Deterministický Fallback aktivní",
                                color = OmnisStyleSheet.Colors.WarningAmber,
                                style = OmnisStyleSheet.Typography.badgeText
                            )
                        }
                        TextButton(
                            onClick = { showHelpDetail = !showHelpDetail },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showHelpDetail) "Skrýt SOP" else "Jak aktivovat?",
                                color = OmnisStyleSheet.Colors.CyanAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = formatErrorMessage(lastOnlineError ?: ""),
                        color = OmnisStyleSheet.Colors.TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp),
                        lineHeight = 15.sp
                    )

                    AnimatedVisibility(visible = showHelpDetail) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(color = OmnisStyleSheet.Colors.BorderMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 SOP - POSTUP AKTIVACE ONLINE GEMINI REŽIMU:",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Získejte svůj bezplatný klíč na https://aistudio.google.com/ \n" +
                                        "2. Otevřete panel 'Secrets' v rozhraní AI Studio.\n" +
                                        "3. Přidejte klíč s přesným názvem: GEMINI_API_KEY\n" +
                                        "4. Vložte hodnotu klíče (začíná na AIzaSy...).\n" +
                                        "5. Aplikace automaticky aktivuje online kaskádu modelů.",
                                color = OmnisStyleSheet.Colors.TextMuted,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = OmnisStyleSheet.Colors.BorderMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "DETAILECH CHYBY (LOG SERVERU):",
                                color = OmnisStyleSheet.Colors.WarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastOnlineError ?: "",
                                color = OmnisStyleSheet.Colors.TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Čistý indikátor online stavu s aktuálním aktivním modelem
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .clickable { if (isTechnicalMode) showCascadeDialog = true },
            color = Color.Transparent
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OmnisStyleSheet.Colors.SuccessEmerald)
                    )
                    Text(
                        text = if (isTechnicalMode) "KOGNITIVNÍ BRÁNA ONLINE ($activeModel)" else "Kognitivní systém O.M.N.I.S. (Online)",
                        color = OmnisStyleSheet.Colors.SuccessEmerald.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
                if (isTechnicalMode) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detaily kaskády",
                        tint = OmnisStyleSheet.Colors.TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private fun formatErrorMessage(rawError: String): String {
    return when {
        rawError.contains("402") || rawError.contains("prepayment credits are depleted") || rawError.contains("billing") -> {
            "CHYBA: Nedostatek kreditů v AI Studio (HTTP 402).\n" +
            "U vašeho Google AI Studio účtu došlo k vyčerpání předplaceného kreditu nebo limitu.\n" +
            "Aplikace běží bezpečně v lokálním offline režimu."
        }
        rawError.contains("429") || rawError.contains("quota") || rawError.contains("rate limit") -> {
            "CHYBA: Překročen limit požadavků (HTTP 429).\n" +
            "Byl dočasně překročen bezplatný limit API požadavků.\n" +
            "Aplikace běží v lokálním offline režimu."
        }
        rawError.contains("400") || rawError.contains("invalid key") || rawError.contains("API key not valid") -> {
            "CHYBA: Neplatný API klíč (HTTP 400/403).\n" +
            "Zkontrolujte prosím správnost vložení klíče GEMINI_API_KEY v panelu 'Secrets'.\n" +
            "Aplikace běží v lokálním offline režimu."
        }
        else -> {
            rawError
        }
    }
}
