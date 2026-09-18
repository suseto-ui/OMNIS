package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitoring.PerformanceMonitor
import com.example.ui.theme.*

@Composable
fun AdminObservabilityCard() {
    val trigger by PerformanceMonitor.metricsUpdateTrigger.collectAsState()
    
    // Odvozené metriky (reagují na trigger)
    val totalRequests = remember(trigger) { PerformanceMonitor.getTotalRequests() }
    val totalBatches = remember(trigger) { PerformanceMonitor.getTotalBatches() }
    val avgLatency = remember(trigger) { PerformanceMonitor.getAverageLatency() }
    val efficiency = remember(trigger) { PerformanceMonitor.getEfficiencyRatio() }
    val savedTokens = remember(trigger) { PerformanceMonitor.getTotalTokensSaved() }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "SYSTÉMOVÁ OBSERVABILITA",
                    color = OmnisEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Latency Gauge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisPanelDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = OmnisTextMuted, modifier = Modifier.size(14.dp))
                            Text("PRŮMĚRNÁ LATENCE API", color = OmnisTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${avgLatency}ms", color = if (avgLatency < 1200) OmnisEmerald else OmnisAmber, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { (avgLatency.toFloat() / 3000f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (avgLatency < 1200) OmnisEmerald else if (avgLatency < 2500) OmnisAmber else Color.Red,
                        trackColor = OmnisBorderDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Efficiency Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisPanelDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("BATCH EFEKTIVITA", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.1f", efficiency)}%", color = OmnisEmerald, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Text("Celkem $totalBatches dávek", color = OmnisTextMuted, fontSize = 8.sp)
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisPanelDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("UŠETŘENO TOKENŮ", color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Token, contentDescription = null, tint = OmnisAmber, modifier = Modifier.size(14.dp))
                            Text("${savedTokens}", color = OmnisAmber, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                        Text("Optimalizace kontextu", color = OmnisTextMuted, fontSize = 8.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Metriky jsou sbírány v reálném čase z Orchestration VRM uzlu. Cílová latence pro APPROVED tier je < 1500ms.",
                color = OmnisTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
