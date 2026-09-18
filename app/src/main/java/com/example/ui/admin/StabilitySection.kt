package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.action.ResilienceManager
import com.example.ui.theme.*

@Composable
fun ResilienceStabilityCard(
    circuitState: ResilienceManager.CircuitState,
    isChaosMode: Boolean,
    onToggleChaos: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, if (isChaosMode) Color.Red else OmnisEmerald),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = if (isChaosMode) Color.Red else OmnisEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SYSTÉMOVÁ REZILIENCE & JISTIČE",
                        color = if (isChaosMode) Color.Red else OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when(circuitState) {
                        ResilienceManager.CircuitState.CLOSED -> OmnisEmerald.copy(alpha = 0.2f)
                        ResilienceManager.CircuitState.OPEN -> Color.Red.copy(alpha = 0.2f)
                        ResilienceManager.CircuitState.HALF_OPEN -> OmnisAmber.copy(alpha = 0.2f)
                        else -> OmnisBorderDark
                    }
                ) {
                    Text(
                        text = "STATUS: ${circuitState.name}",
                        color = when(circuitState) {
                            ResilienceManager.CircuitState.CLOSED -> OmnisEmerald
                            ResilienceManager.CircuitState.OPEN -> Color.Red
                            ResilienceManager.CircuitState.HALF_OPEN -> OmnisAmber
                            else -> OmnisTextMuted
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mechanismus Circuit Breaker chrání kognitivní engine před kaskádovým selháním při detekci nestability externích API nebo databázových uzlů.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                border = BorderStroke(1.dp, if (isChaosMode) Color.Red.copy(alpha = 0.5f) else OmnisBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SIMULACE CHAOSU (CHAOS MODE)",
                            color = if (isChaosMode) Color.Red else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Umělá injektáž 30% chybovosti pro testování automatického odpojení jističů.",
                            color = OmnisTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    
                    Switch(
                        checked = isChaosMode,
                        onCheckedChange = onToggleChaos,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Red,
                            checkedTrackColor = Color.Red.copy(alpha = 0.3f),
                            uncheckedThumbColor = OmnisTextMuted,
                            uncheckedTrackColor = OmnisBorderDark
                        )
                    )
                }
            }
            
            if (isChaosMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Text(
                            text = "VAROVÁNÍ: Režim chaosu může dočasně znemožnit kognitivní dotazy v Chatu (Circuit Breaker se aktivuje autonomně).",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
