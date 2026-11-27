package com.lumacraft.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Elegant neutral system with a single refined indigo accent.
val LumaIndigo = Color(0xFF5B7CFA)
val LumaIndigoSoft = Color(0xFFA9BCFF)
val LumaMint = Color(0xFF7BD8C2)
val LumaAmber = Color(0xFFF5B65A)
val LumaRose = Color(0xFFFF7A9E)

private val DarkColors = darkColorScheme(
    primary = LumaIndigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF24334F),
    onPrimaryContainer = Color(0xFFD6E1FF),
    secondary = Color(0xFF9AA6C4),
    onSecondary = Color(0xFF10141C),
    tertiary = LumaMint,
    onTertiary = Color(0xFF0A1512),
    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFE9EBEF),
    surface = Color(0xFF101216),
    onSurface = Color(0xFFE9EBEF),
    surfaceVariant = Color(0xFF171A21),
    onSurfaceVariant = Color(0xFFA8B0BF),
    outline = Color(0xFF2A2E38),
    error = Color(0xFFF27B7B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5FE8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE6FF),
    onPrimaryContainer = Color(0xFF121F45),
    secondary = Color(0xFF4A5568),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1F8F79),
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF191C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C22),
    surfaceVariant = Color(0xFFE9EBF1),
    onSurfaceVariant = Color(0xFF5A616E),
    outline = Color(0xFFC9CFDA),
    error = Color(0xFFB3261E)
)

@Composable
fun LumaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
