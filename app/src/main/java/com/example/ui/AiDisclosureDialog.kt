package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.accessibility.OmnisAccessibilityHelper.accessibleButton
import com.example.accessibility.OmnisAccessibilityHelper.accessibleHeading
import com.example.ai.transparency.AiDisclosureManager

/**
 * AiDisclosureDialog:
 * Povinný pop-up dialog informující klienta před okamžikem prvního předání dat,
 * že za komunikačním kanálem sedí algoritmický kognitivní systém (AI bot), nikoliv lidský operátor
 * (plná shoda s čl. 50 odst. 1 a 2 EU AI Act).
 */
@Composable
fun AiDisclosureDialog(
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "AI Transparency Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Upozornění o AI komunikaci",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.accessibleHeading("Upozornění o komunikaci s umělou inteligencí")
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "V souladu s Nařízením Evropského parlamentu a Rady o umělé inteligenci (EU AI Act) Vás informujeme, že v tomto komunikačním rozhraní interagujete s algoritmickým kognitivním systémem (AI Bot), nikoliv s lidským operátorem.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Všechny synteticky generované odpovědi jsou opatřeny neviditelným strojově čitelným vodoznakem dokládajícím umělý původ dat (čl. 50 odst. 2).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AiDisclosureManager.confirmDisclosure(context)
                    onConfirmed()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .testTag("confirm_ai_disclosure_button")
                    .accessibleButton("Rozumím a beru na vědomí komunikaci s AI")
            ) {
                Text("Beru na vědomí a pokračovat", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .testTag("dismiss_ai_disclosure_button")
                    .accessibleButton("Zavřít upozornění")
            ) {
                Text("Zavřít")
            }
        }
    )
}
