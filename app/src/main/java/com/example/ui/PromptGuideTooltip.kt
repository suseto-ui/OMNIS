package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
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
import com.example.ui.theme.*

/**
 * PromptGuideTooltip: Malá dynamická komponenta zobrazující nápovědu pro formátování promptu nad vstupním polem.
 * Reaguje na zadání prvních znaků dotazu. Umožňuje dočasné zavření křížkem nebo trvalé vypnutí v nastavení.
 */
@Composable
fun PromptGuideTooltip(
    inputText: String,
    isEnabledInSettings: Boolean,
    onDisableInSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDismissedBySession by remember { mutableStateOf(false) }

    // Nápověda se zobrazí pokud je povolená v nastavení, nebyla v relaci zavřena a text má 2 až 45 znaků
    val shouldShow = isEnabledInSettings && !isDismissedBySession && inputText.length in 2..45

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("prompt_guide_tooltip")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = OmnisAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "NÁPOVĚDA FORMÁTOVÁNÍ PROMPTU",
                            color = OmnisAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Vypnout v nastavení",
                            color = OmnisTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { onDisableInSettings() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("btn_disable_prompt_guide_settings")
                        )

                        IconButton(
                            onClick = { isDismissedBySession = true },
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("btn_close_prompt_guide_tooltip")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zavřít nápovědu",
                                tint = OmnisTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Pro 100% verifikovanou exekuci bez zásahu sémantické brány formulujte dotaz:",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = OmnisBgDark,
                    border = BorderStroke(1.dp, OmnisBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = " [Kognitivní doména] + [Akce] + [Kritérium]\n Např: \"[Auth Engine] Proveď analýzu Zero-Trust pro zamezení úniku tokenů\"",
                        color = OmnisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
