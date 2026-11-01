package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitoring.TensorMonitorService
import com.example.ui.theme.*

/**
 * Resizable HUD pro zobrazení právní shody a stavu autonomie systému.
 * Umožňuje přímý proklik na panel Tenzorového dohledu a autoregrese.
 */
@Composable
fun AiLegalHUD(
    readinessScore: Int,
    isCompliant: Boolean,
    onOpenTensorOversight: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val tension = TensorMonitorService.cognitiveTension

    Surface(
        modifier = modifier
            .animateContentSize()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        color = OmnisPanelDark.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, if (isCompliant) OmnisEmerald else Color.Red),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenTensorOversight() }
                ) {
                    Icon(
                        imageVector = Icons.Default.GppGood,
                        contentDescription = null,
                        tint = if (isCompliant) OmnisEmerald else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI/LAW HUD",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Změnit velikost",
                        tint = OmnisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Text(
                    text = "Skóre připravenosti: $readinessScore%",
                    color = OmnisTextLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isCompliant) "STATUS: SHODA S NIS2" else "STATUS: NESHODA",
                    color = if (isCompliant) OmnisEmerald else Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Kognitivní tenze: ${String.format(java.util.Locale.US, "%.2f", tension)}",
                    color = if (tension < 0.3f) OmnisAmber else OmnisCyan,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedButton(
                    onClick = onOpenTensorOversight,
                    border = BorderStroke(0.8.dp, OmnisViolet),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmnisViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = OmnisViolet,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tenzorový dohled",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
