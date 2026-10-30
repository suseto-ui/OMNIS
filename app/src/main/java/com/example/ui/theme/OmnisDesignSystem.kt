package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ==============================================================================
 * O.M.N.I.S. CENTRAL DESIGN SYSTEM (CSS-STYLE DESIGN TOKENS & RENDER STYLES)
 * ==============================================================================
 * Centralizovaný stylový systém fungující jako obdoba CSS proměnných a tříd.
 * Sjednocuje barvy, rámečky (borders), zaoblení (shapes), typografii a
 * vizuální komponenty do jediného snadno udržovatelného a upravitelného místa.
 */
object OmnisStyleSheet {

    // --------------------------------------------------------------------------
    // 1. BAREVNÁ PALETA (COLORS / CSS VARIABLES)
    // --------------------------------------------------------------------------
    object Colors {
        // Plátna a pozadí
        val CanvasDark = Color(0xFF0B0F19)
        val PanelBackground = Color(0xFF131B2E)
        val CardBackground = Color(0xFF1A2438)
        val InputBackground = Color(0xFF0F172A)
        val DialogBackground = Color(0xFF161F33)

        // Primární & Akcentní barvy
        val CyanAccent = Color(0xFF00F0FF)
        val CyanSubtle = Color(0xFF38BDF8)
        val VioletSynthesis = Color(0xFF818CF8)
        val VioletSubtle = Color(0xFF6366F1)

        // Funkční a stavové barvy
        val SuccessEmerald = Color(0xFF10B981)
        val WarningAmber = Color(0xFFF59E0B)
        val ErrorRose = Color(0xFFEF4444)
        val InfoBlue = Color(0xFF3B82F6)

        // Rámečky (Borders)
        val BorderMuted = Color(0xFF233048)
        val BorderBright = Color(0xFF334566)
        val BorderActive = Color(0xFF00F0FF)
        val BorderWarning = Color(0xFFF59E0B)
        val BorderError = Color(0xFFEF4444)
        val BorderSuccess = Color(0xFF10B981)

        // Texty
        val TextPrimary = Color(0xFFF8FAFC)
        val TextSecondary = Color(0xFFCBD5E1)
        val TextMuted = Color(0xFF64748B)
        val TextAccent = Color(0xFF00F0FF)
        val TextCode = Color(0xFF7DD3FC)

        // Stavové podbarvení bublin a zpráv
        val BubbleUser = Color(0xFF4338CA).copy(alpha = 0.28f)
        val BubbleAssistant = Color(0xFF151E32)
        val BubbleWarning = Color(0xFF36200A)
        val BubbleBlocked = Color(0xFF3B1215)
    }

    // --------------------------------------------------------------------------
    // 2. RÁMEČKY A LINKY (BORDERS / STROKES)
    // --------------------------------------------------------------------------
    object Borders {
        val none = null
        val subtle = BorderStroke(1.dp, Colors.BorderMuted)
        val standard = BorderStroke(1.dp, Colors.BorderBright)
        val active = BorderStroke(1.dp, Colors.BorderActive)
        val activeCyan = BorderStroke(1.dp, Colors.CyanAccent.copy(alpha = 0.6f))
        val warning = BorderStroke(1.dp, Colors.BorderWarning.copy(alpha = 0.7f))
        val error = BorderStroke(1.dp, Colors.BorderError.copy(alpha = 0.7f))
        val success = BorderStroke(1.dp, Colors.BorderSuccess.copy(alpha = 0.7f))
        val userBubble = BorderStroke(1.dp, Colors.VioletSynthesis.copy(alpha = 0.45f))
    }

    // --------------------------------------------------------------------------
    // 3. ZAOBLENÍ (SHAPES & RADII)
    // --------------------------------------------------------------------------
    object Shapes {
        val micro = RoundedCornerShape(4.dp)
        val small = RoundedCornerShape(6.dp)
        val badge = RoundedCornerShape(8.dp)
        val card = RoundedCornerShape(12.dp)
        val panel = RoundedCornerShape(16.dp)
        val input = RoundedCornerShape(20.dp)
        val pill = RoundedCornerShape(999.dp)

        // Asymetrické bubliny zpráv
        val userBubble = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 3.dp
        )
        val assistantBubble = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 3.dp,
            bottomEnd = 16.dp
        )
    }

    // --------------------------------------------------------------------------
    // 4. TYPOGRAFIE A TEXTOVÉ STYLY (TYPOGRAPHY)
    // --------------------------------------------------------------------------
    object Typography {
        val titleScreen = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Colors.TextPrimary,
            letterSpacing = 1.sp
        )

        val headerSection = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Colors.TextPrimary,
            letterSpacing = 0.5.sp
        )

        val bodyRegular = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = Colors.TextSecondary
        )

        val bodyAssistant = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            color = Colors.TextPrimary
        )

        val codeMono = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = Colors.TextCode
        )

        val badgeText = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )

        val caption = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            color = Colors.TextMuted
        )
    }

    // --------------------------------------------------------------------------
    // 5. METRIKY ROZESTUPŮ (SPACING & ELEVATION)
    // --------------------------------------------------------------------------
    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
    }
}
