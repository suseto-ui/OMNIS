package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AdminDbHealthCard(
    localCount: Int,
    postgresSyncedCount: Int,
    unSyncedCount: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "LOKÁLNÍ DATABÁZE (ROOM / SQLITE)",
                    color = OmnisEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DbMetricBox(label = "CELKEM", value = localCount.toString(), color = Color.White, modifier = Modifier.weight(1f))
                DbMetricBox(label = "SYNCHRONIZOVÁNO", value = postgresSyncedCount.toString(), color = OmnisEmerald, modifier = Modifier.weight(1f))
                DbMetricBox(label = "ČEKÁ NA SYNC", value = unSyncedCount.toString(), color = OmnisAmber, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lokální perzistence je zajištěna pomocí šifrovaného SQLite (SQLCipher v sandboxu). Replikace do Cloud SQL (PostgreSQL) probíhá asynchronně přes background worker.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
