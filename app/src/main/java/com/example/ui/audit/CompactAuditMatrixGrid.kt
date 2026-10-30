package com.example.ui.audit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.*

@Composable
fun CompactAuditMatrixGrid(
    confidenceScore: Float,
    reliabilityStatus: String,
    suggestedClarifications: List<String>,
    onQuickQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLow = reliabilityStatus == "LOW_WARNING" || confidenceScore < 0.70f
    val isMed = reliabilityStatus == "MEDIUM" || (confidenceScore in 0.70f..0.82f)

    val primaryColor = when {
        isLow -> Color(0xFFF59E0B)
        isMed -> Color(0xFF38BDF8)
        else -> OmnisEmerald
    }

    val containerBg = OmnisCardDark

    Card(
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_audit_matrix")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row
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
                        imageVector = if (isLow) Icons.Default.Warning else if (isMed) Icons.Default.Info else Icons.Default.Shield,
                        contentDescription = "Audit Status",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AUDITNÍ MATICE DEFENSIVE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Surface(
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${(confidenceScore * 100).toInt()}% ${reliabilityStatus}",
                        color = primaryColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2x2 Matrix Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cell 1: Index Spolehlivosti
                    MatrixGridCell(
                        modifier = Modifier.weight(1f),
                        title = "INDEX SPOLEHLIVOSTI",
                        value = "${(confidenceScore * 100).toInt()}/100",
                        subtitle = if (isLow) "Riziko spekulace" else "Verifikováno",
                        accentColor = primaryColor,
                        progress = confidenceScore
                    )

                    // Cell 2: Factual Density
                    val factScore = (confidenceScore * 0.9f + 0.1f).coerceIn(0f, 1f)
                    MatrixGridCell(
                        modifier = Modifier.weight(1f),
                        title = "FAKTICKÁ HUSTOTA",
                        value = "${(factScore * 100).toInt()}%",
                        subtitle = "Faktické vzorce",
                        accentColor = OmnisCyan,
                        progress = factScore
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cell 3: 8D AKGE Spread
                    val vectorSpread = (confidenceScore * 0.85f + 0.15f).coerceIn(0f, 1f)
                    MatrixGridCell(
                        modifier = Modifier.weight(1f),
                        title = "8D VEKTOR DOMÉN",
                        value = String.format("%.2f σ", vectorSpread * 2.5f),
                        subtitle = "Rozptyl znalostí",
                        accentColor = Color(0xFFA855F7),
                        progress = vectorSpread
                    )

                    // Cell 4: Speculation Trigger
                    val specRisk = if (isLow) 0.65f else if (isMed) 0.25f else 0.05f
                    MatrixGridCell(
                        modifier = Modifier.weight(1f),
                        title = "SPEKULAČNÍ RIZIKO",
                        value = if (isLow) "VYSOKÉ" else if (isMed) "STŘEDNÍ" else "NÍZKÉ",
                        subtitle = "Trigger detektor",
                        accentColor = if (isLow) Color(0xFFEF4444) else OmnisEmerald,
                        progress = specRisk
                    )
                }
            }

            // Interactive Clarification Chips Section
            if (suggestedClarifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = OmnisBorderDark)
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "DOPORUČENÉ INTERAKTIVNÍ KROKY / UPŘESNĚNÍ:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = OmnisTextLight,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestedClarifications.take(3).forEach { clarification ->
                        OutlinedButton(
                            onClick = { onQuickQuery(clarification) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = OmnisPanelDark,
                                contentColor = primaryColor
                            ),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("matrix_chip_${clarification.take(8)}")
                        ) {
                            Text(
                                text = clarification,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixGridCell(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OmnisPanelDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = OmnisTextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = accentColor,
                trackColor = OmnisBorderDark
            )
        }
    }
}
