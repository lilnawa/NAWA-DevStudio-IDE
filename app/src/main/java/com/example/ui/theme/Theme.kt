package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NawaDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF041824),
    primaryContainer = Color(0xFF003848),
    onPrimaryContainer = CyanPrimary,
    secondary = NeonPurple,
    onSecondary = Color(0xFF1E0B38),
    secondaryContainer = Color(0xFF381466),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = NeonGreen,
    onTertiary = Color(0xFF022B1B),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = Color(0xFF1E293B),
    error = NeonRed,
    onError = Color.White
)

@Composable
fun NawaDevStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NawaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
