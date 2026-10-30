package com.example.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow

enum class UserExperienceMode {
    STANDARD,
    EXPERT;

    companion object {
        val isEcoMode = MutableStateFlow(true)
    }
}

object OmnisStyleSheet {
    object Colors {
        val VioletSynthesis = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
        val CyanAccent = androidx.compose.ui.graphics.Color(0xFF06B6D4)
        val BubbleUser = androidx.compose.ui.graphics.Color(0xFF1E1B4B)
        val BubbleAssistant = androidx.compose.ui.graphics.Color(0xFF0F172A)
        val BubbleBlocked = androidx.compose.ui.graphics.Color(0xFF451A03)
        val BubbleWarning = androidx.compose.ui.graphics.Color(0xFF3B0764)
        val TextMuted = androidx.compose.ui.graphics.Color(0xFF94A3B8)
        val ErrorRose = androidx.compose.ui.graphics.Color(0xFFF43F5E)
        val WarningAmber = androidx.compose.ui.graphics.Color(0xFFF59E0B)
    }

    object Shapes {
        val userBubble = androidx.compose.foundation.shape.RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp)
        val assistantBubble = androidx.compose.foundation.shape.RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp)
        val small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
        val badge = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    }

    object Borders {
        val userBubble = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF6366F1))
        val error = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFF43F5E))
        val warning = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFF59E0B))
        val subtle = androidx.compose.foundation.BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0xFF334155))
    }

    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
    }

    object Typography {
        val badgeText = androidx.compose.ui.text.TextStyle(
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
