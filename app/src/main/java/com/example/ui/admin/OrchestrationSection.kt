package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.action.OrchestrationManager
import com.example.ui.theme.*

@Composable
fun AdminOrchestrationCard(
    isBatchingEnabled: Boolean,
    batchWindow: Long,
    onToggleBatching: (Boolean) -> Unit,
    onWindowChange: (Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, if (isBatchingEnabled) OmnisEmerald else OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "KOGNITIVNÍ ORCHESTRACE",
                        color = OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (isBatchingEnabled) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OmnisEmerald.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "SMART BATCHING: ON",
                            color = OmnisEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Optimalizace kognitivního toku sdružováním frekventovaných požadavků. Snižuje režijní náklady na tokeny (Context Window) a latenci HTTP handshake.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMART BATCHING", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Automatické sdružování úloh", color = OmnisTextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isBatchingEnabled,
                        onCheckedChange = onToggleBatching,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OmnisEmerald,
                            checkedTrackColor = OmnisEmerald.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (isBatchingEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "OKNO SDRUŽOVÁNÍ: ${batchWindow}ms",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = batchWindow.toFloat(),
                    onValueChange = { onWindowChange(it.toLong()) },
                    valueRange = 500f..5000f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = OmnisEmerald,
                        activeTrackColor = OmnisEmerald,
                        inactiveTrackColor = OmnisBorderDark
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("500ms", color = OmnisTextMuted, fontSize = 9.sp)
                    Text("Aggressive", color = OmnisEmerald.copy(alpha = 0.5f), fontSize = 9.sp)
                    Text("5000ms", color = OmnisTextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}
