package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserRole
import com.example.ui.theme.*

/**
 * Plovoucí Quick-Action HUD Bar pro okamžitý přehled a ovládání klíčových systémových parametrů:
 * 1. Stav Hlavního jističe (Circuit Breaker)
 * 2. Stav Sémantické brány (Prompt Gateway)
 * 3. Aktivní kognitivní doména (rychlý přepínač mezi 8 doménami)
 * Plná integrace haptické odezvy (WCAG / moderní haptika).
 */
@Composable
fun OmnisQuickActionHud(
    isCircuitBreakerTripped: Boolean,
    onToggleCircuitBreaker: () -> Unit,
    isPromptGatewayEnabled: Boolean,
    onTogglePromptGateway: () -> Unit,
    currentDomain: String,
    onDomainChange: (String) -> Unit,
    userRole: UserRole,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDomainPicker by remember { mutableStateOf(false) }
    val isAdmin = userRole.canAccessSystemActions()

    val domainMap = listOf(
        "Sys" to "SYSTEMS_INTELLIGENCE",
        "Econ" to "ECONOMICS",
        "Psych" to "PSYCHOLOGY",
        "Eco" to "ECOLOGY",
        "Law" to "LEGAL_FRAMEWORKS",
        "Sec" to "SECURITY_DEFENSE",
        "Phys" to "PHYSICAL_SYSTEMS",
        "Soc" to "SOCIOLOGY"
    )

    val currentShortName = domainMap.find { it.second == currentDomain || it.first == currentDomain }?.first ?: "Sys"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OmnisPanelDark.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, OmnisBorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Indikátor a přepínač JISTIČE (Circuit Breaker)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCircuitBreakerTripped) Color.Red.copy(alpha = 0.18f) else OmnisEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, if (isCircuitBreakerTripped) Color.Red else OmnisEmerald),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isAdmin) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleCircuitBreaker()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCircuitBreakerTripped) Color.Red else OmnisEmerald)
                        )
                        Text(
                            text = if (isCircuitBreakerTripped) "BREAKER: OFF" else "SYS: OK",
                            color = if (isCircuitBreakerTripped) Color.Red else OmnisEmerald,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. Indikátor a přepínač SÉMANTICKÉ BRÁNY (Gateway)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPromptGatewayEnabled) OmnisCyan.copy(alpha = 0.15f) else OmnisAmber.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isAdmin) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePromptGateway()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber)
                        )
                        Text(
                            text = if (isPromptGatewayEnabled) "GATEWAY: ON" else "GATEWAY: BYPASS",
                            color = if (isPromptGatewayEnabled) OmnisCyan else OmnisAmber,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 3. Rychlý volič aktivní domény
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OmnisBgDark,
                    border = BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showDomainPicker = !showDomainPicker
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = OmnisCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "DOM: $currentShortName",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = if (showDomainPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = OmnisTextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        // Výsuvný pruh rychlého výběru domény
        AnimatedVisibility(
            visible = showDomainPicker,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OmnisBgDark,
                border = BorderStroke(0.5.dp, OmnisCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    domainMap.forEach { (shortLabel, fullDomain) ->
                        val isSelected = currentShortName == shortLabel
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) OmnisCyan.copy(alpha = 0.25f) else Color.Transparent,
                            border = BorderStroke(0.5.dp, if (isSelected) OmnisCyan else OmnisBorderDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDomainChange(fullDomain)
                                    showDomainPicker = false
                                }
                        ) {
                            Text(
                                text = shortLabel,
                                color = if (isSelected) OmnisCyan else OmnisTextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
