package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AdminSelfHealingCard(
    isHealing: Boolean,
    logs: List<String>,
    onTriggerHealing: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, if (isHealing) OmnisAmber else OmnisEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = if (isHealing) OmnisAmber else OmnisEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AUTONOMNÍ SELF-HEALING UI & STAVU",
                        color = if (isHealing) OmnisAmber else OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isHealing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = OmnisAmber,
                        strokeWidth = 2.dp
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "STANDBY / OPTIMAL",
                            color = OmnisEmerald,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Detekuje a automaticky opravuje anomálie, zaseknuté asynchronní zámky, nestabilní prahové hodnoty sémantické brány a nekonzistentní stav lokální databáze v reálném čase.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTriggerHealing,
                enabled = !isHealing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmnisEmerald,
                    contentColor = Color.Black,
                    disabledContainerColor = OmnisBorderDark,
                    disabledContentColor = OmnisTextMuted
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isHealing) Icons.Default.Loop else Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isHealing) "PROBÍHÁ AUTODIAGNOSTIKA..." else "SPUSTIT SYSTÉMOVOU SANACI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisPanelDark,
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = OmnisTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LOG DIAGNOSTICKÉHO JÁDRA",
                                color = OmnisTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Render logs
                        logs.takeLast(8).forEach { logLine ->
                            val isError = logLine.contains("ERROR", true)
                            val isSuccess = logLine.contains("OK", true) || logLine.contains("DOKONČEN", true) || logLine.contains("Úspěšně", true)
                            val isRepair = logLine.contains("OPRAVA", true) || logLine.contains("SITUACE", true)
                            
                            val textColor = when {
                                isError -> Color.Red
                                isSuccess -> OmnisEmerald
                                isRepair -> OmnisAmber
                                else -> Color.White
                            }

                            Text(
                                text = logLine,
                                color = textColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
