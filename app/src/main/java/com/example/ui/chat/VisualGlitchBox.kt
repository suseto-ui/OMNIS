package com.example.ui.chat

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TopologicalVerificationResult
import com.example.ui.theme.*

/**
 * Komponenta vizuálního glitche pro dvoufázové topologické ověřování.
 * Pokud topologická verifikace detekuje narušení invariantu (Euler χ, Betti β1, Shannon S nebo perkolaci),
 * text je animovaně rozmazán s chromatickou aberací a zobrazen ochranný Gatekeeper panel.
 */
@Composable
fun VisualGlitchBox(
    isGlitchActive: Boolean,
    verificationResult: TopologicalVerificationResult?,
    onExecuteOverride: (() -> Unit)? = null,
    isAdminOperator: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!isGlitchActive) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "GlitchEffect")

    // Horizontální záchvěv (chromatická aberace)
    val glitchOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlitchOffset"
    )

    // Pulzující červeno-oranžová aura
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_glitch_container")
    ) {
        // 1. Zastíněný a rozmazaný obsah s chromatickým glitchem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color.Red.copy(alpha = glowAlpha),
                                OmnisAmber.copy(alpha = glowAlpha),
                                Color.Red.copy(alpha = glowAlpha)
                            )
                        )
                    ),
                    RoundedCornerShape(10.dp)
                )
                .background(OmnisPanelDark)
        ) {
            // Skutečný obsah s rozostřením (Blur)
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(6.dp)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(blurModifier)
                    .offset(x = glitchOffset.dp)
            ) {
                content()
            }

            // Poloprůhledná scanline clona a glitch šum
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 6f
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.28f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f
                    )
                    y += step
                }
            }

            // Bezpečnostní vodoznak přes glitchovaný text
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Red.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppBad,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TOPOLOGICKÝ INVARIANT NARUŠEN (OCHRANNÁ CLONA)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Diagnostický Gatekeeper panel s vysvětlením
        Surface(
            color = OmnisPanelDark,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("gatekeeper_diagnostic_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OmnisAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Zero-Hallucination Gatekeeper (Topologický Audit)",
                        color = OmnisAmber,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = verificationResult?.diagnosticMessage
                        ?: "Text byl rozostřen pro ochranu integrity kognitivní sítě. Byla detekována informační bifurkace nebo neplatná kauzální cesta.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (verificationResult != null) {
                    val m = verificationResult.metrics
                    val p = verificationResult.percolation
                    Surface(
                        color = OmnisBgDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "METRIKY: Euler χ = ${m.eulerCharacteristic} | Betti β1 = ${m.betti1} | Entropie S = ${String.format("%.3f", m.shannonEntropy)}",
                                color = OmnisCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PERKOLACE: Zasaženo ${String.format("%.1f", p.failingRatio * 100)}% domén (Kritický práh pc = 38.0%)",
                                color = if (p.isCascadeRiskActive) Color.Red else OmnisEmerald,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Tlačítko pro Executive Override (pouze pro oprávněné role)
                if (isAdminOperator && onExecuteOverride != null) {
                    Button(
                        onClick = onExecuteOverride,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmnisAmber.copy(alpha = 0.2f),
                            contentColor = OmnisAmber
                        ),
                        border = BorderStroke(1.dp, OmnisAmber),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("executive_override_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aktivovat Executive Override (Proof of Necessity)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
