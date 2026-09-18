package com.example.ui.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisTelemetry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelemetryDashboardView(
    logs: List<OmnisTelemetry>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmnisBgDark)
            .padding(16.dp)
            .navigationBarsPadding()
            .testTag("telemetry_dashboard_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = OmnisCyan)
                Text(
                    "SYSTEM TELEMETRY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            IconButton(onClick = onClearLogs) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Smazat logy", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
        
        Text(
            "Hloubková observabilita kognitivních procesů a systémové integrity.",
            color = OmnisTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Systémové logy jsou prázdné.", color = OmnisTextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        TelemetryLogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryLogItem(log: OmnisTelemetry) {
    val typeColor = when(log.type) {
        "ERROR" -> Color.Red
        "WARNING" -> OmnisAmber
        "INFO" -> OmnisCyan
        "COGNITIVE_DRIFT" -> OmnisViolet
        "PERFORMANCE" -> OmnisEmerald
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(typeColor, RoundedCornerShape(3.dp))
                )
                Text(
                    text = log.type,
                    color = typeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "| ${log.component.uppercase()}",
                    color = OmnisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp)),
                color = OmnisTextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = log.message,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontFamily = FontFamily.Monospace
        )
        
        if (log.metadata != "{}") {
            Text(
                text = log.metadata,
                color = OmnisCyan.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
