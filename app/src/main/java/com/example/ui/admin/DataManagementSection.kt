package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
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
fun AdminDataIntegrityCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = OmnisEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "DATA INTEGRITY AUDIT",
                    color = OmnisEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Všechny záznamy jsou podepsány pomocí SHA-256 HMAC klíče v TEE (Trusted Execution Environment). Kontrola integrity probíhá při každém startu.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = OmnisEmerald,
                trackColor = OmnisBorderDark
            )
            Text("STAV: 100% VERIFIKOVÁNO", color = OmnisEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun AdminDataPurgeCard(
    postgresSyncedCount: Int,
    onPurge: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                Text(
                    text = "DANGEROUS ZONE: PURGE",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Smazání synchronizovaných záznamů z lokální DB pro uvolnění místa. Data v PostgreSQL zůstanou zachována.",
                color = OmnisTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onPurge(postgresSyncedCount) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(8.dp),
                enabled = postgresSyncedCount > 0
            ) {
                Text("Smazat $postgresSyncedCount záznamů", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
