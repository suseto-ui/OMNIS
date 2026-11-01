package com.example.ui.localization

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

/**
 * Kompaktní a vizuálně čistý přepínač jazyka (CZ / EN).
 * Vhodný pro umístění v TopAppBar i v navigačním draweru.
 */
@Composable
fun LanguageToggleChip(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val currentLang by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()

    val description = if (currentLang == AppLanguage.CS) {
        "Aktivní jazyk Čeština. Klepnutím přepnete na English."
    } else {
        "Active language English. Tap to switch to Czech."
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .testTag("language_toggle_chip")
            .clip(RoundedCornerShape(8.dp))
            .background(OmnisPanelDark)
            .border(1.dp, OmnisBorderDark, RoundedCornerShape(8.dp))
            .clickable {
                AppLocaleManager.toggleLanguage(context)
            }
            .padding(horizontal = 6.dp, vertical = if (isCompact) 4.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LanguageBadge(
                code = "CZ",
                isActive = currentLang == AppLanguage.CS,
                isCompact = isCompact
            )
            Text(
                text = "|",
                color = OmnisBorderDark,
                fontSize = if (isCompact) 10.sp else 12.sp,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                maxLines = 1
            )
            LanguageBadge(
                code = "EN",
                isActive = currentLang == AppLanguage.EN,
                isCompact = isCompact
            )
        }
    }
}

@Composable
private fun LanguageBadge(
    code: String,
    isActive: Boolean,
    isCompact: Boolean
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else OmnisTextLight,
        animationSpec = tween(200),
        label = "textColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) OmnisCyan else Color.Transparent,
        animationSpec = tween(200),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = if (isCompact) 4.dp else 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = textColor,
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            softWrap = false,
            maxLines = 1
        )
    }
}
