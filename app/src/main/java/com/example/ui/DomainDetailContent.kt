package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OmnisRecord
import com.example.ui.theme.*

/**
 * Obsah spodního modálního panelu (Bottom Sheet) pro detail domény v kontextu vybrané zprávy.
 */
@Composable
fun DomainDetailContent(
    domain: String,
    record: OmnisRecord,
    onOptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OmnisBgDark,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("domain_detail_content")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DETAIL DOMÉNY: ${domain.uppercase()}",
                        color = OmnisCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ID záznamu: #${record.id} | Kognitivní proces: ${record.cognitiveProcess}",
                        color = OmnisTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = OmnisBorderDark)

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = OmnisPanelDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmnisBorderDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Obsah kognitivního záznamu:",
                        color = OmnisEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.content.take(300) + if (record.content.length > 300) "..." else "",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Button(
                onClick = onOptimize,
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_optimize_domain")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OmnisBgDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Optimalizovat pro doménu $domain",
                    color = OmnisBgDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
