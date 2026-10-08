package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AdminNavChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(title, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = OmnisEmerald.copy(alpha = 0.2f),
            selectedLabelColor = OmnisEmerald,
            selectedLeadingIconColor = OmnisEmerald,
            containerColor = OmnisPanelDark,
            labelColor = OmnisTextMuted,
            iconColor = OmnisTextMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) OmnisEmerald else OmnisBorderDark,
            selectedBorderColor = OmnisEmerald,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
fun DbMetricBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = OmnisPanelDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, color = OmnisTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
