package com.example.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.data.adversarialScore
import com.example.ui.OmnisMarkdownText
import com.example.ui.UserExperienceMode
import com.example.ui.theme.OmnisStyleSheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Komponenta pro vykreslení jedné zprávy v konverzaci O.M.N.I.S.
 * Využívá unifikovaný stylový systém OmnisStyleSheet (CSS-like tokeny).
 */
@Composable
fun ChatMessageItem(
    record: OmnisRecord,
    previousRecord: OmnisRecord? = null,
    historyRecords: List<OmnisRecord> = emptyList(),
    onQuickQuery: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onExportPdf: (OmnisRecord) -> Unit,
    selectedDomains: Set<String> = emptySet(),
    onDomainClick: (String) -> Unit = {},
    onDomainLongClick: (String, OmnisRecord) -> Unit = { _, _ -> },
    onAuthorize: () -> Unit = {},
    userExperienceMode: UserExperienceMode = UserExperienceMode.STANDARD
) {
    val isUser = record.role == "user"
    var thoughtsExpanded by remember { mutableStateOf(false) }
    var techDetailsExpanded by remember { mutableStateOf(false) }

    val isBlocked = !isUser && record.defenseTier == "BLOCKED"
    val isWarning = !isUser && record.defenseTier == "WARNING"
    val isApproved = !isUser && record.defenseTier == "APPROVED"

    // Výběr stylů podle CSS-like tokenů OmnisStyleSheet
    val bubbleShape = if (isUser) OmnisStyleSheet.Shapes.userBubble else OmnisStyleSheet.Shapes.assistantBubble
    val bubbleColor = when {
        isUser -> OmnisStyleSheet.Colors.BubbleUser
        isBlocked -> OmnisStyleSheet.Colors.BubbleBlocked
        isWarning -> OmnisStyleSheet.Colors.BubbleWarning
        else -> OmnisStyleSheet.Colors.BubbleAssistant
    }
    val bubbleBorder = when {
        isUser -> OmnisStyleSheet.Borders.userBubble
        isBlocked -> OmnisStyleSheet.Borders.error
        isWarning -> OmnisStyleSheet.Borders.warning
        else -> OmnisStyleSheet.Borders.subtle
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = bubbleBorder,
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 0.96f)
                .testTag("message_card_${record.id}")
        ) {
            Column(modifier = Modifier.padding(OmnisStyleSheet.Spacing.md)) {
                // Záhlaví zprávy (Odesílatel & Akce)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isUser) OmnisStyleSheet.Colors.VioletSynthesis else OmnisStyleSheet.Colors.CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isUser) "OPERÁTOR" else "O.M.N.I.S. CORE",
                            color = if (isUser) OmnisStyleSheet.Colors.VioletSynthesis else OmnisStyleSheet.Colors.CyanAccent,
                            style = OmnisStyleSheet.Typography.badgeText
                        )
                    }

                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
                    if (!isUser) {
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val context = LocalContext.current
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeStr,
                                color = OmnisStyleSheet.Colors.TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(record.content))
                                    Toast.makeText(context, "Výstup zkopírován do schránky", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp).testTag("copy_message_${record.id}")
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Kopírovat text",
                                    tint = OmnisStyleSheet.Colors.CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (record.compositeScore > 0f) {
                                IconButton(onClick = { onSpeak(record.content) }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Přehrát",
                                        tint = OmnisStyleSheet.Colors.CyanAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(onClick = { onExportPdf(record) }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Exportovat PDF",
                                        tint = OmnisStyleSheet.Colors.CyanAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = timeStr,
                            color = OmnisStyleSheet.Colors.TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(OmnisStyleSheet.Spacing.xs))

                // Vizuální indikátor bezpečnostního rizika (Adversarial Score - výhradně v EXPERT režimu)
                val advScore = record.adversarialScore
                if (!isUser && userExperienceMode == UserExperienceMode.EXPERT && advScore >= 0.40f) {
                    val advIndicatorColor = when {
                        advScore >= 0.70f -> OmnisStyleSheet.Colors.ErrorRose
                        else -> OmnisStyleSheet.Colors.WarningAmber
                    }
                    Surface(
                        shape = OmnisStyleSheet.Shapes.small,
                        color = advIndicatorColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, advIndicatorColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("adversarial_risk_indicator_${record.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(advIndicatorColor)
                            )
                            Icon(
                                imageVector = if (advScore >= 0.70f) Icons.Default.Security else Icons.Default.Warning,
                                contentDescription = "Bezpečnostní riziko",
                                tint = advIndicatorColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (advScore >= 0.70f) "KRITICKÉ RIZIKO" else "ZVÝŠENÉ RIZIKO",
                                        color = advIndicatorColor,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${(advScore * 100).toInt()}% ADV SCORE",
                                        color = advIndicatorColor,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                Text(
                                    text = "8D Tenzor Sec: ${(record.valSec * 100).toInt()}% | Bezpečnostní pnutí",
                                    color = OmnisStyleSheet.Colors.TextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Bezpečnostní a schvalovací stav (Multi-Layer Defense)
                if (!isUser) {
                    val currentRole by com.example.auth.OmnisAuthService.currentUserRole.collectAsStateWithLifecycle()
                    val isAdmin = currentRole.canAccessSystemActions()

                    when {
                        isBlocked -> {
                            Surface(
                                shape = OmnisStyleSheet.Shapes.badge,
                                color = OmnisStyleSheet.Colors.ErrorRose.copy(alpha = 0.15f),
                                border = OmnisStyleSheet.Borders.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("defense_blocked_banner")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = OmnisStyleSheet.Colors.ErrorRose, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (isAdmin) "VÝSTUP ZABLOKOVÁN: HUMAN-IN-THE-LOOP" else "VÝSTUP ZABLOKOVÁN: VYŽADUJE AUTORIZACI ADMINA",
                                            color = OmnisStyleSheet.Colors.ErrorRose,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (record.defenseNotes.isNotBlank()) {
                                        Text(
                                            text = record.defenseNotes,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                        )
                                    }
                                    if (isAdmin) {
                                        Button(
                                            onClick = onAuthorize,
                                            colors = ButtonDefaults.buttonColors(containerColor = OmnisStyleSheet.Colors.ErrorRose),
                                            shape = OmnisStyleSheet.Shapes.small,
                                            modifier = Modifier
                                                .height(38.dp)
                                                .testTag("btn_authorize_human_loop")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Autorizovat adminem", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "Tento výstup je zablokován bezpečnostní vrstvou. Pro odblokování přepněte na účet ADMIN.",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        isWarning -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = OmnisStyleSheet.Colors.WarningAmber.copy(alpha = 0.10f),
                                border = BorderStroke(0.5.dp, OmnisStyleSheet.Colors.WarningAmber.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("defense_warning_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = OmnisStyleSheet.Colors.WarningAmber, modifier = Modifier.size(14.dp))
                                    Column {
                                        Text(
                                            text = "Zvýšená pozornost (Spolehlivost: ${(record.compositeScore * 100).toInt()}%)",
                                            color = OmnisStyleSheet.Colors.WarningAmber,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (record.defenseNotes.isNotBlank()) {
                                            Text(
                                                text = record.defenseNotes,
                                                color = Color(0xFFFCD34D),
                                                fontSize = 8.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        isApproved && record.compositeScore > 0f -> {
                            Surface(
                                shape = OmnisStyleSheet.Shapes.small,
                                color = OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .testTag("defense_verified_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OmnisStyleSheet.Colors.CyanAccent, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "MULTI-LAYER VERIFIED (${(record.compositeScore * 100).toInt()}%)",
                                        color = OmnisStyleSheet.Colors.CyanAccent,
                                        style = OmnisStyleSheet.Typography.badgeText
                                    )
                                }
                            }
                        }
                    }
                }

                // Přiložený soubor / artefakt
                record.attachedImagePath?.let { path ->
                    val file = File(path)
                    Surface(
                        shape = OmnisStyleSheet.Shapes.badge,
                        color = OmnisStyleSheet.Colors.CanvasDark,
                        border = OmnisStyleSheet.Borders.subtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("attached_artifact_container")
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = OmnisStyleSheet.Colors.CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ARTEFAKT: ${file.name}",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (file.exists()) {
                                    val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                                    Text(
                                        text = "${sizeKb} KB",
                                        color = OmnisStyleSheet.Colors.TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            if (file.exists()) {
                                coil.compose.AsyncImage(
                                    model = file,
                                    contentDescription = "Připojený obrázek",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(OmnisStyleSheet.Shapes.small)
                                )
                            }
                        }
                    }
                }

                // Introspekce kognitivního procesu (V EXPERT módu)
                if (!isUser && userExperienceMode == UserExperienceMode.EXPERT && record.cognitiveProcess.isNotBlank()) {
                    Surface(
                        shape = OmnisStyleSheet.Shapes.badge,
                        color = OmnisStyleSheet.Colors.CanvasDark,
                        border = OmnisStyleSheet.Borders.subtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { thoughtsExpanded = !thoughtsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kognitivní introspekce",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = if (thoughtsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Rozbalit myšlenky",
                                    tint = OmnisStyleSheet.Colors.CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            AnimatedVisibility(visible = thoughtsExpanded) {
                                Text(
                                    text = record.cognitiveProcess,
                                    color = OmnisStyleSheet.Colors.TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(OmnisStyleSheet.Spacing.sm))
                }

                // Hlavní text zprávy (Markdown nebo běžný text)
                if (isUser) {
                    Text(
                        text = record.content,
                        color = OmnisStyleSheet.Colors.TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                } else {
                    OmnisMarkdownText(
                        text = record.content,
                        textColor = OmnisStyleSheet.Colors.TextPrimary,
                        fontSize = 13
                    )
                }

                // Vypočet a zobrazení spotřeby tokenů (Token Telemetry & Cost Badge - výhradně v EXPERT režimu)
                if (!isUser && userExperienceMode == UserExperienceMode.EXPERT) {
                    val estimatedPromptTokens = if (previousRecord != null) (previousRecord.content.length / 3.8f).toInt().coerceAtLeast(12) else 45
                    val estimatedCompletionTokens = (record.content.length / 3.8f).toInt().coerceAtLeast(15)
                    val totalTokens = estimatedPromptTokens + estimatedCompletionTokens
                    val estimatedCostUsd = (estimatedPromptTokens * 0.00000015f) + (estimatedCompletionTokens * 0.00000060f)
                    val estimatedCostCzk = estimatedCostUsd * 23.5f

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = OmnisStyleSheet.Colors.CanvasDark,
                        border = BorderStroke(0.5.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("token_calculation_badge_${record.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Token Telemetrie",
                                    tint = OmnisStyleSheet.Colors.CyanAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "SPOTŘEBA:",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$totalTokens ($estimatedPromptTokens in / $estimatedCompletionTokens out)",
                                    color = OmnisStyleSheet.Colors.TextMuted,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "~$${String.format(java.util.Locale.US, "%.5f", estimatedCostUsd)} (${String.format(java.util.Locale.US, "%.3f", estimatedCostCzk)} Kč)",
                                color = OmnisStyleSheet.Colors.VioletSynthesis,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // 8D Metriky v EXPERT módu
                if (!isUser && userExperienceMode == UserExperienceMode.EXPERT && record.compositeScore > 0f) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OctagonMetricsGrid(
                        record = record,
                        selectedDomains = selectedDomains,
                        onDomainClick = onDomainClick,
                        onDomainLongClick = onDomainLongClick
                    )
                }

                // Rozbalitelné technické detaily výhradně v EXPERT módu
                if (!isUser && userExperienceMode == UserExperienceMode.EXPERT && (record.compositeScore > 0f || record.cognitiveProcess.isNotBlank())) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = OmnisStyleSheet.Shapes.badge,
                        color = OmnisStyleSheet.Colors.CanvasDark,
                        border = BorderStroke(1.dp, OmnisStyleSheet.Colors.SuccessEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { techDetailsExpanded = !techDetailsExpanded }
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
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = OmnisStyleSheet.Colors.SuccessEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Technické detaily a 8D matice (${(record.compositeScore * 100).toInt()}%)",
                                        color = OmnisStyleSheet.Colors.SuccessEmerald,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = if (techDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Rozbalit technické detaily",
                                    tint = OmnisStyleSheet.Colors.SuccessEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            AnimatedVisibility(visible = techDetailsExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (record.cognitiveProcess.isNotBlank()) {
                                        Text(
                                            text = "Kognitivní proces:\n${record.cognitiveProcess}",
                                            color = OmnisStyleSheet.Colors.TextMuted,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (record.compositeScore > 0f) {
                                        OctagonMetricsGrid(
                                            record = record,
                                            selectedDomains = selectedDomains,
                                            onDomainClick = onDomainClick,
                                            onDomainLongClick = onDomainLongClick
                                        )

                                        // SMT Invariant Verification & ZK-Commitment Hash Badges
                                        val record8D = mapOf(
                                            "Sys" to record.valSys, "Econ" to record.valEcon,
                                            "Psych" to record.valPsych, "Eco" to record.valEco,
                                            "Law" to record.valLaw, "Sec" to record.valSec,
                                            "Phys" to record.valPhys, "Soc" to record.valSoc
                                        )
                                        val smtResult = remember(record.id) {
                                            com.example.ui.OmnisCorrelationEngine.verifyLogicalInvariants(record8D)
                                        }
                                        val zkHash = remember(record.id) {
                                            com.example.ui.OmnisCorrelationEngine.generateZkSafetyCommitment(
                                                query = record.content.take(64),
                                                values = record8D,
                                                defenseTier = "APPROVED",
                                                operatorNonce = record.timestamp
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = OmnisStyleSheet.Colors.CanvasDark,
                                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.BorderMuted),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(
                                                            imageVector = if (smtResult.isFullyVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = if (smtResult.isFullyVerified) OmnisStyleSheet.Colors.SuccessEmerald else OmnisStyleSheet.Colors.WarningAmber,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = if (smtResult.isFullyVerified) "SMT VERIFIKOVÁNO (Invarianty OK)" else "SMT VAROVÁNÍ",
                                                            color = if (smtResult.isFullyVerified) OmnisStyleSheet.Colors.SuccessEmerald else OmnisStyleSheet.Colors.WarningAmber,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "ZK-Proof: ${zkHash.commitmentHash.take(16)}...",
                                                        color = OmnisStyleSheet.Colors.TextMuted,
                                                        fontSize = 8.5.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = "Kryptografický audit",
                                                        color = OmnisStyleSheet.Colors.CyanAccent,
                                                        fontSize = 8.5.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SYSTÉMOVÝ INTERVENČNÍ PANEL PRO VŠECHNY UŽIVATELE (KAUZÁLNÍ SIMULACE DO(X)) ---
                if (!isUser) {
                    var causalInterventionOpen by remember { mutableStateOf(false) }
                    var selectedInterventionDom by remember { mutableStateOf("Sys") }
                    var interventionVal by remember { mutableStateOf(0.95f) }
                    var interventionAnalysis by remember { mutableStateOf<com.example.ui.OmnisCorrelationEngine.CausalDoCalculusAnalysis?>(null) }

                    var promptMutatorOpen by remember { mutableStateOf(false) }
                    var promptMutations by remember { mutableStateOf<List<String>>(emptyList()) }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                causalInterventionOpen = !causalInterventionOpen
                                if (causalInterventionOpen && interventionAnalysis == null) {
                                    val r8D = mapOf(
                                        "Sys" to record.valSys, "Econ" to record.valEcon,
                                        "Psych" to record.valPsych, "Eco" to record.valEco,
                                        "Law" to record.valLaw, "Sec" to record.valSec,
                                        "Phys" to record.valPhys, "Soc" to record.valSoc
                                    )
                                    interventionAnalysis = com.example.ui.OmnisCorrelationEngine.calculateCausalDoIntervention(
                                        observedValues = r8D,
                                        targetDomain = selectedInterventionDom,
                                        targetValue = interventionVal
                                    )
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = OmnisStyleSheet.Colors.CyanAccent, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (causalInterventionOpen) "Skrýt do(X)" else "Simulovat do(X)",
                                color = OmnisStyleSheet.Colors.CyanAccent,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                promptMutatorOpen = !promptMutatorOpen
                                if (promptMutatorOpen && promptMutations.isEmpty()) {
                                    val evolved = com.example.ui.OmnisCorrelationEngine.evolveInterventionPromptGenetic(
                                        targetDomain = record.domain,
                                        targetDelta = 0.20f
                                    )
                                    promptMutations = listOf(
                                        evolved.evolvedPrompt,
                                        "Expanzivní varianta: ${evolved.evolvedPrompt} se zaměřením na multi-disciplinární dopad.",
                                        "Kritická varianta: Analyzuj potenciální zranitelnosti a regulatorní soulad pro ${evolved.targetDomain}."
                                    )
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.BorderMuted),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(28.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OmnisStyleSheet.Colors.TextSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (promptMutatorOpen) "Skrýt mutace" else "Mutace promptu",
                                color = OmnisStyleSheet.Colors.TextSecondary,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // DO(X) DRAWER CONTENT
                    AnimatedVisibility(visible = causalInterventionOpen) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OmnisStyleSheet.Colors.CanvasDark,
                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "⚡ JUDEA PEARL KAUZÁLNÍ ZÁSAH: P(Y | do(X = x))",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Zvolte doménu a nastavte cílovou hodnotu přímé intervence:",
                                    color = OmnisStyleSheet.Colors.TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                // Doménové čipy pro výběr
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Sys", "Econ", "Sec", "Eco").forEach { dom ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (selectedInterventionDom == dom) OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.25f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (selectedInterventionDom == dom) OmnisStyleSheet.Colors.CyanAccent else OmnisStyleSheet.Colors.BorderMuted),
                                            modifier = Modifier.weight(1f).clickable {
                                                selectedInterventionDom = dom
                                                val r8D = mapOf(
                                                    "Sys" to record.valSys, "Econ" to record.valEcon,
                                                    "Psych" to record.valPsych, "Eco" to record.valEco,
                                                    "Law" to record.valLaw, "Sec" to record.valSec,
                                                    "Phys" to record.valPhys, "Soc" to record.valSoc
                                                )
                                                interventionAnalysis = com.example.ui.OmnisCorrelationEngine.calculateCausalDoIntervention(
                                                    observedValues = r8D,
                                                    targetDomain = selectedInterventionDom,
                                                    targetValue = interventionVal
                                                )
                                            }
                                        ) {
                                            Text(
                                                text = dom,
                                                color = if (selectedInterventionDom == dom) Color.White else OmnisStyleSheet.Colors.TextMuted,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp).wrapContentWidth(Alignment.CenterHorizontally)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Nastavená hodnota do($selectedInterventionDom): ${(interventionVal * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Slider(
                                    value = interventionVal,
                                    onValueChange = { newVal ->
                                        interventionVal = newVal
                                        val r8D = mapOf(
                                            "Sys" to record.valSys, "Econ" to record.valEcon,
                                            "Psych" to record.valPsych, "Eco" to record.valEco,
                                            "Law" to record.valLaw, "Sec" to record.valSec,
                                            "Phys" to record.valPhys, "Soc" to record.valSoc
                                        )
                                        interventionAnalysis = com.example.ui.OmnisCorrelationEngine.calculateCausalDoIntervention(
                                            observedValues = r8D,
                                            targetDomain = selectedInterventionDom,
                                            targetValue = interventionVal
                                        )
                                    },
                                    valueRange = 0.10f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = OmnisStyleSheet.Colors.CyanAccent,
                                        activeTrackColor = OmnisStyleSheet.Colors.CyanAccent
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(24.dp)
                                )

                                interventionAnalysis?.let { analysis ->
                                    val gainText = if (analysis.netSystemicGain >= 0) "+${String.format("%.1f", analysis.netSystemicGain * 100)}%" else "${String.format("%.1f", analysis.netSystemicGain * 100)}%"
                                    Text(
                                        text = "Kauzální zisk resilience: $gainText (Původní: ${(analysis.originalResilience * 100).toInt()}%, Po zásahu: ${(analysis.postInterventionResilience * 100).toInt()}%)",
                                        color = if (analysis.netSystemicGain >= 0) OmnisStyleSheet.Colors.SuccessEmerald else OmnisStyleSheet.Colors.WarningAmber,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 13.sp
                                    )
                                    Text(
                                        text = "Ochranný štít proti konfounderům: ${if (analysis.isConfounderShieldActive) "Aktivní (Pearl DAG)" else "Neaktivní"}",
                                        color = OmnisStyleSheet.Colors.TextMuted,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            val synthesisQuery = "Aplikuj Do(X) intervenci do($selectedInterventionDom = ${(interventionVal * 100).toInt()}%): Syntetizuj dopad na systémovou stabilizaci (+${String.format("%.1f", analysis.netSystemicGain * 100)}% zisk resiliencie)."
                                            onQuickQuery(synthesisQuery)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = OmnisStyleSheet.Colors.CyanAccent,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PŘENÉST SYNTÉZU DO CHATU",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // PROMPT MUTATOR DRAWER CONTENT
                    AnimatedVisibility(visible = promptMutatorOpen) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OmnisStyleSheet.Colors.CanvasDark,
                            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.BorderMuted),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🧬 GENETICKY OPTIMALIZOVANÉ VARIANTY PROMPTU:",
                                    color = OmnisStyleSheet.Colors.TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                promptMutations.forEachIndexed { idx, candPrompt ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OmnisStyleSheet.Colors.PanelBackground,
                                        border = BorderStroke(1.dp, OmnisStyleSheet.Colors.BorderMuted),
                                        modifier = Modifier.fillMaxWidth().clickable { onQuickQuery(candPrompt) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}. $candPrompt",
                                                color = OmnisStyleSheet.Colors.CyanAccent,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OmnisStyleSheet.Colors.CyanAccent, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Follow-up otázky / akční čipy
                if (!isUser && record.followUpQuestions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val questions = record.followUpQuestions.split("|").filter { it.isNotBlank() }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        questions.forEach { q ->
                            Surface(
                                shape = OmnisStyleSheet.Shapes.small,
                                color = OmnisStyleSheet.Colors.CanvasDark,
                                border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQuickQuery(q) }
                            ) {
                                Text(
                                    text = "→ $q",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8D Uplift & Systémová harmonie (Rozbalovací karta výhradně v EXPERT módu)
        if (!isUser && userExperienceMode == UserExperienceMode.EXPERT) {
            Response8dUpliftCard(
                record = record,
                previousRecord = previousRecord,
                historyRecords = historyRecords,
                onApplyAction = onQuickQuery,
                modifier = Modifier.fillMaxWidth(0.96f)
            )
        }
    }
}

@Composable
private fun OctagonMetricsGrid(
    record: OmnisRecord,
    selectedDomains: Set<String>,
    onDomainClick: (String) -> Unit,
    onDomainLongClick: (String, OmnisRecord) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Sys", record.valSys, Color(0xFF60A5FA), Icons.Default.Settings, selectedDomains.contains("Sys"), onClick = { onDomainClick("Sys") }, onLongClick = { onDomainLongClick("Sys", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Econ", record.valEcon, Color(0xFFFBBF24), Icons.Default.Paid, selectedDomains.contains("Econ"), onClick = { onDomainClick("Econ") }, onLongClick = { onDomainLongClick("Econ", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Psych", record.valPsych, Color(0xFFC084FC), Icons.Default.Face, selectedDomains.contains("Psych"), onClick = { onDomainClick("Psych") }, onLongClick = { onDomainLongClick("Psych", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Eco", record.valEco, Color(0xFF34D399), Icons.Default.Spa, selectedDomains.contains("Eco"), onClick = { onDomainClick("Eco") }, onLongClick = { onDomainLongClick("Eco", record) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Law", record.valLaw, Color(0xFFFB7185), Icons.Default.Gavel, selectedDomains.contains("Law"), onClick = { onDomainClick("Law") }, onLongClick = { onDomainLongClick("Law", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Sec", record.valSec, Color(0xFFEF4444), Icons.Default.Security, selectedDomains.contains("Sec"), onClick = { onDomainClick("Sec") }, onLongClick = { onDomainLongClick("Sec", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Phys", record.valPhys, Color(0xFFFB923C), Icons.Default.Speed, selectedDomains.contains("Phys"), onClick = { onDomainClick("Phys") }, onLongClick = { onDomainLongClick("Phys", record) })
            }
            Box(modifier = Modifier.weight(1f)) {
                MetricPill("Soc", record.valSoc, Color(0xFFF472B6), Icons.Default.Groups, selectedDomains.contains("Soc"), onClick = { onDomainClick("Soc") }, onLongClick = { onDomainLongClick("Soc", record) })
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MetricPill(
    label: String,
    value: Float,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val percent = (value * 100).toInt()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.28f) else OmnisStyleSheet.Colors.CanvasDark,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) color else color.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("metric_pill_$label")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else color.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else OmnisStyleSheet.Colors.TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "$percent%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StreamingCognitiveMessage(streamState: com.example.ui.OmnisViewModel.StreamState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = OmnisStyleSheet.Shapes.assistantBubble,
            color = OmnisStyleSheet.Colors.BubbleAssistant,
            border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (streamState.stage) {
                                "introspection" -> "O.M.N.I.S. CORE (Introspekce...)"
                                "execution" -> "O.M.N.I.S. CORE (Generování...)"
                                "verification" -> "O.M.N.I.S. CORE (Verifikováno)"
                                else -> "O.M.N.I.S. CORE (Zpracovávám...)"
                            },
                            color = if (streamState.stage == "verification") OmnisStyleSheet.Colors.CyanAccent else OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(OmnisStyleSheet.Spacing.xs))

                Surface(
                    shape = OmnisStyleSheet.Shapes.badge,
                    color = OmnisStyleSheet.Colors.CanvasDark,
                    border = BorderStroke(1.dp, OmnisStyleSheet.Colors.CyanAccent.copy(alpha = 0.6f)),
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
                                if (streamState.stage != "verification") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = OmnisStyleSheet.Colors.CyanAccent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OmnisStyleSheet.Colors.CyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Kognitivní introspekce",
                                    color = OmnisStyleSheet.Colors.CyanAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = if (streamState.stage == "verification") "Syntéza úspěšně dokončena. Čekám na vykreslení výsledku." else streamState.text,
                            color = OmnisStyleSheet.Colors.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
