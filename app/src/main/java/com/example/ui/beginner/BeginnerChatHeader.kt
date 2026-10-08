package com.example.ui.beginner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLocaleManager
import com.example.ui.theme.*

/**
 * Kompaktní a prostorově úsporná lišta pro režim Začátečník.
 * Výchozí stav je sbalený na elegantní 1-řádkový pruh s rychlými čipy,
 * takže nezabírá drahocenné místo pro zprávy konverzace.
 */
@Composable
fun BeginnerChatHeader(
    onQuickPromptSelected: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onSwitchToExpert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by AppLocaleManager.currentLanguage.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    val prompts = if (currentLang == AppLanguage.EN) {
        listOf(
            "💼 Compare job offers" to "I have two job offers. Help me compare their pros, cons, and financial security in simple points.",
            "💰 Monthly budget" to "How should I structure a realistic monthly family budget without unnecessary stress?",
            "📄 Lease contract tips" to "What are the most important things to check in an apartment lease agreement?",
            "🎯 Plan a goal" to "I want to start a side business. Help me break this goal into small achievable weekly steps."
        )
    } else {
        listOf(
            "💼 Porovnat práce" to "Mám dvě nabídky práce. Pomoz mi v jednoduchých bodech porovnat jejich výhody, rizika a finanční jistotu.",
            "💰 Rodinný rozpočet" to "Jak mám jednoduše a bez stresu rozvrhnout měsíční rodinný rozpočet?",
            "📄 Nájemní smlouva" to "Jaké jsou nejdůležitější věci a háčky, na které si dát pozor při podpisu nájemní smlouvy?",
            "🎯 Naplánovat cíl" to "Chci si otevřít malou živnost. Rozděl mi tento cíl na postupné jednoduché kroky na každý týden."
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnisPanelDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, OmnisEmerald.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("beginner_chat_header")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Kompaktní 1-řádková hlavička
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(OmnisEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌱", fontSize = 11.sp)
                    }
                    Text(
                        text = if (currentLang == AppLanguage.EN) "BEGINNER" else "ZAČÁTEČNÍK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = OmnisEmerald,
                        fontSize = 10.sp
                    )

                    // Horizontální čipy přímo v 1. řádku pro okamžitý přístup
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        prompts.forEach { (label, queryText) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OmnisBgDark,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, OmnisBorderDark),
                                modifier = Modifier.clickable { onQuickPromptSelected(queryText) }
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Průvodce",
                            tint = OmnisCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                            tint = OmnisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(color = OmnisBorderDark, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentLang == AppLanguage.EN) "Plain answers in everyday Czech without complex formulas." else "Srozumitelné odpovědi v běžné řeči bez složitých matic a vzorců.",
                        color = OmnisTextLight,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.EN) "Need deep math matrices?" else "Chcete matematické matice?",
                            color = OmnisTextMuted,
                            fontSize = 9.sp
                        )
                        Text(
                            text = if (currentLang == AppLanguage.EN) "Switch to Expert ⚡" else "Přepnout na Expert ⚡",
                            color = OmnisViolet,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { onSwitchToExpert() }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
