package com.example.ui.chat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.OmnisRecord
import com.example.ui.OmnisMarkdownText
import com.example.ui.OmnisStyleSheet
import com.example.ui.theme.*

/**
 * Harmonická konverzační karta zprávy v čistém cyber-glass stylu.
 * Ve výchozím stavu poskytuje nerušený zážitek ze čtení a volitelně
 * zpřístupňuje myšlenkovou stopu, auditní matici a interaktivní kroky.
 */
@Composable
fun ChatMessageItem(
    record: OmnisRecord,
    onQuickQuery: (String) -> Unit,
    onSpeak: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUser = record.role == "user"

    val showReasoningPref by ChatDisplayPreferences.showReasoningTrace.collectAsStateWithLifecycle()
    val showAuditMatrixPref by ChatDisplayPreferences.showAuditMatrixGrid.collectAsStateWithLifecycle()
    val showConfidencePillsPref by ChatDisplayPreferences.showConfidencePills.collectAsStateWithLifecycle()
    val showNextStepsPref by ChatDisplayPreferences.showInteractiveNextSteps.collectAsStateWithLifecycle()
    val cyberGlassEnabled by ChatDisplayPreferences.cyberGlassEffectEnabled.collectAsStateWithLifecycle()

    var isReasoningExpanded by remember { mutableStateOf(false) }

    val bubbleShape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    val bubbleBg = if (isUser) {
        if (cyberGlassEnabled) OmnisViolet.copy(alpha = 0.18f) else OmnisPanelDark
    } else {
        if (cyberGlassEnabled) OmnisPanelDark.copy(alpha = 0.85f) else OmnisPanelDark
    }
    val bubbleBorder = if (isUser) {
        BorderStroke(1.dp, OmnisViolet.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, OmnisBorderDark.copy(alpha = 0.8f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            border = bubbleBorder,
            tonalElevation = if (cyberGlassEnabled) 4.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 0.96f)
                .animateContentSize()
                .testTag("message_card_${record.id}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Záhlaví zprávy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isUser) {
                            Image(
                                painter = painterResource(id = R.drawable.img_omnis_avatar),
                                contentDescription = "Operator Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(OmnisCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = OmnisCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isUser) "OPERÁTOR" else "O.M.N.I.S. INTELLIGENCE",
                            color = if (isUser) OmnisViolet else OmnisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Volitelný čip spolehlivosti
                    if (!isUser && showConfidencePillsPref && record.confidenceScore > 0f) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OmnisEmerald.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, OmnisEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "KONFIDENCE ${(record.confidenceScore * 100).toInt()}%",
                                color = OmnisEmerald,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Samotný formátovaný text zprávy
                OmnisMarkdownText(
                    text = record.content,
                    color = Color.White
                )

                // Volitelná myšlenková stopa (Reasoning Trace)
                if (!isUser && !record.cognitiveProcess.isNullOrBlank()) {
                    val isTraceAlwaysVisible = showReasoningPref
                    if (isTraceAlwaysVisible || isReasoningExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = OmnisBgDark.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, OmnisBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = OmnisViolet,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "KOGNITIVNÍ MYŠLENKOVÁ STOPA:",
                                        color = OmnisViolet,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = record.cognitiveProcess ?: "",
                                    color = OmnisTextLight,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Volitelná 8D auditní matice
                if (!isUser && showAuditMatrixPref) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val clarificationsList = record.suggestedClarifications.split("|").filter { it.isNotBlank() }
                    ReliabilityAuditCard(
                        confidenceScore = record.confidenceScore,
                        reliabilityStatus = record.reliabilityStatus,
                        suggestedClarifications = clarificationsList,
                        onQuickQuery = onQuickQuery
                    )
                }

                // Volitelné interaktivní navazující kroky (Next Steps)
                if (!isUser && showNextStepsPref) {
                    val questions = record.followUpQuestions.split("|").filter { it.isNotBlank() }
                    if (questions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "DOPORUČENÉ INTERAKTIVNÍ KROKY:",
                                color = OmnisCyan.copy(alpha = 0.8f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                questions.forEach { query ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = OmnisBgDark,
                                        border = BorderStroke(0.6.dp, OmnisCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { onQuickQuery(query) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowOutward,
                                                contentDescription = null,
                                                tint = OmnisCyan,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = query,
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Panel rychlých akcí zprávy (Kopírovat, Přečíst hlasem, Rozbalit myšlenku)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tlačítko pro rozbalení myšlenkové stopy (pokud není trvale zapnutá v nastavení)
                        if (!showReasoningPref && !record.cognitiveProcess.isNullOrBlank()) {
                            IconButton(
                                onClick = { isReasoningExpanded = !isReasoningExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReasoningExpanded) Icons.Default.VisibilityOff else Icons.Default.Psychology,
                                    contentDescription = "Myšlenková stopa",
                                    tint = if (isReasoningExpanded) OmnisViolet else OmnisTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Tlačítko přečtení hlasem
                        IconButton(
                            onClick = { onSpeak(record.content) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Přečíst hlasem",
                                tint = OmnisTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Tlačítko kopírování
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(record.content))
                                Toast.makeText(context, "Zpráva zkopírována do schránky", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopírovat text",
                                tint = OmnisTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReliabilityAuditCard(
    confidenceScore: Float,
    reliabilityStatus: String,
    suggestedClarifications: List<String>,
    onQuickQuery: (String) -> Unit
) {
    com.example.ui.audit.CompactAuditMatrixGrid(
        confidenceScore = confidenceScore,
        reliabilityStatus = reliabilityStatus,
        suggestedClarifications = suggestedClarifications,
        onQuickQuery = onQuickQuery,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
