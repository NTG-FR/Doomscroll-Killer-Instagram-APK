package io.github.ntgfr.doomscrollkiller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand palette: a bold red on calm neutrals.
private val Red = Color(0xFFE5484D)
private val RedLight = Color(0xFFFF6B70)

private val DarkColors = darkColorScheme(
    primary = RedLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1517),
    onPrimaryContainer = Color(0xFFFFD9DA),
    // Dialog icons, tonal buttons and selected chips use the secondary colors: keep them on brand.
    secondary = RedLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1517),
    onSecondaryContainer = Color(0xFFFFD9DA),
    background = Color(0xFF0E0E11),
    onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF0E0E11),
    onSurface = Color(0xFFEDEDF0),
    surfaceVariant = Color(0xFF1C1C21),
    onSurfaceVariant = Color(0xFFA0A0AB),
    surfaceContainer = Color(0xFF17171B),
    surfaceContainerHigh = Color(0xFF1F1F25),
    outline = Color(0xFF3A3A44),
    outlineVariant = Color(0xFF2A2A31),
    error = Color(0xFFFF6B70),
)

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3E4),
    onPrimaryContainer = Color(0xFF5C0E11),
    secondary = Red,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3E4),
    onSecondaryContainer = Color(0xFF5C0E11),
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF17171B),
    surface = Color(0xFFF7F7F9),
    onSurface = Color(0xFF17171B),
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF5E5E6A),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F1F4),
    outline = Color(0xFFD4D4DB),
    outlineVariant = Color(0xFFE6E6EB),
    error = Red,
)

/** Colors for status indicators, which Material's scheme does not cover. */
@Immutable
data class StatusColors(val good: Color, val warning: Color, val bad: Color)

private val DarkStatus = StatusColors(good = Color(0xFF3DD68C), warning = Color(0xFFF5B83D), bad = RedLight)
private val LightStatus = StatusColors(good = Color(0xFF16A34A), warning = Color(0xFFD97706), bad = Red)

val LocalStatusColors = staticCompositionLocalOf { DarkStatus }

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    )
}

@Composable
fun DoomscrollTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    androidx.compose.runtime.CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
