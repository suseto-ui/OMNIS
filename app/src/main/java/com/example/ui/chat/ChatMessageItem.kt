package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.ui.OmnisMarkdownText
import com.example.ui.OmnisStyleSheet

@Composable
fun ChatMessageItem(
    record: OmnisRecord,
    onQuickQuery: (String) -> Unit
) {
    val isUser = record.role == "user"
    val bubbleShape = if (isUser) OmnisStyleSheet.Shapes.userBubble else OmnisStyleSheet.Shapes.assistantBubble
    val bubbleColor = if (isUser) OmnisStyleSheet.Colors.BubbleUser else OmnisStyleSheet.Colors.BubbleAssistant
    val bubbleBorder = if (isUser) OmnisStyleSheet.Borders.userBubble else OmnisStyleSheet.Borders.subtle

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
                }

                Spacer(modifier = Modifier.height(OmnisStyleSheet.Spacing.xs))

                OmnisMarkdownText(
                    text = record.content,
                    color = Color.White
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val clarificationsList = record.suggestedClarifications.split("|").filter { it.isNotBlank() }
                    ReliabilityAuditCard(
                        confidenceScore = record.confidenceScore,
                        reliabilityStatus = record.reliabilityStatus,
                        suggestedClarifications = clarificationsList,
                        onQuickQuery = onQuickQuery
                    )
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
