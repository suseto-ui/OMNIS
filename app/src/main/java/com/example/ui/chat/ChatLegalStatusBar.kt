package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitoring.TensorMonitorService
import com.example.ui.theme.*

/**
 * Minimalistický prolínající se prvek právního stavu a NIS2/EU AI Act souladu.
 * Trvale a diskrétně umístěn v okně chatu přímo nad textovým polem vstupu.
 */
@Composable
fun ChatLegalStatusBar(
    onOpenAudit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val tension = TensorMonitorService.cognitiveTension
    val isTensionNormal = tension >= 0.3f

    Surface(
        color = OmnisPanelDark.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        border = BorderStroke(
            0.5.dp,
            Brush.horizontalGradient(
                listOf(
                    OmnisEmerald.copy(alpha = 0.3f),
                    OmnisCyan.copy(alpha = 0.4f),
                    OmnisViolet.copy(alpha = 0.3f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("chat_legal_status_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isTensionNormal) OmnisEmerald else OmnisAmber)
                    )
                    Text(
                        text = "🇪🇺 NIS2 & EU AI ACT COMPLIANT",
                        color = OmnisEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "•",
                        color = OmnisTextMuted,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "PII Sanitizace aktivní",
                        color = OmnisTextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisCyan.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "TENZE: ${String.format(java.util.Locale.US, "%.2f", tension)}",
                            color = if (isTensionNormal) OmnisCyan else OmnisAmber,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenAudit,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("open_modular_sheet_from_status_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Otevřít kognitivní a právní panel",
                            tint = OmnisCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Rozbalit detail souladu",
                        tint = OmnisTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Systém O.M.N.I.S. automaticky maskuje citlivé osobní údaje (čl. 10 AI Act) a aplikuje kazuální dohled nad generovaným textem dle směrnice NIS2.",
                        color = OmnisTextLight,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onOpenAudit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = "Podrobný audit shody",
                                color = OmnisCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OmnisCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
