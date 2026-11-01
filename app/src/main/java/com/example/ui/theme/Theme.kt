package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OmnisDarkColorScheme = darkColorScheme(
    primary = OmnisCyan,
    secondary = OmnisViolet,
    tertiary = OmnisEmerald,
    background = OmnisBgDark,
    surface = OmnisPanelDark,
    surfaceVariant = OmnisBorderDark,
    onPrimary = OmnisBgDark,
    onSecondary = OmnisTextLight,
    onBackground = OmnisTextLight,
    onSurface = OmnisTextLight,
    onSurfaceVariant = OmnisTextMuted,
)

@Composable
fun OmnisTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = OmnisDarkColorScheme,
        typography = Typography,
        content = content,
    )
}

