package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Modál pro detekci selhání kognitivního toku a samo-opravu.
 */
@Composable
fun RobustnessGuardModal(
    errorMessage: String,
    onDismiss: () -> Unit,
    onSelfRepair: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444)) },
        title = {
            Text(
                text = "Kognitivní selhání detekováno",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Systém O.M.N.I.S. zaznamenal chybu v toku zpracování:",
                    color = OmnisTextLight,
                    fontSize = 14.sp
                )
                Text(
                    text = errorMessage,
                    color = Color(0xFFFCA5A5),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSelfRepair,
                colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Samo-opravná diagnostika", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = OmnisTextMuted)
            }
        },
        containerColor = OmnisPanelDark,
        tonalElevation = 16.dp
    )
}
