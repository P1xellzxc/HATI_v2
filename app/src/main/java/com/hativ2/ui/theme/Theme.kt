package com.hativ2.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────
// 60-30-10 mapping per theme mode
//
//  DARK   60 % DarkSurface/#121212 (bg/surface) — NOT pure black
//         30 % DarkOnSurface (text) + NotionMuted (labels)
//         10 % DarkNotionYellow (accent) + DarkNotionBlue (tertiary)
//
//  LIGHT  60 % NotionWhite (bg/surface)
//         30 % MangaBlack (text/borders) + NotionMuted (labels)
//         10 % NotionYellow (accent) + NotionBlue (tertiary)
//
//  Dark mode rules from design reference:
//  - Surface uses #121212, not #000000 (pure black causes eye strain)
//  - Accent colors are desaturated + lighter for dark mode
//  - Elevation is communicated via lightness, not shadow
//  - Each elevation level adds ~5% lightness to the surface
// ─────────────────────────────────────────────────────────────

private fun darkSchemeFor(p: HatiPalette) = darkColorScheme(
    primary = DarkOnSurface,
    secondary = p.darkAccent,
    tertiary = p.darkTertiary,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated1,
    onPrimary = DarkSurface,
    onSecondary = DarkSurface,
    onTertiary = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurface.copy(alpha = 0.8f),
    outline = p.muted,
    outlineVariant = Color(0xFF444444),
    error = DarkErrorRed,
    errorContainer = DarkErrorContainer,
    onError = DarkSurface,
    onErrorContainer = DarkErrorRed,
    inverseSurface = p.paper,
    inverseOnSurface = DarkSurface,
    surfaceContainerHighest = DarkSurfaceElevated3,
    surfaceContainerHigh = DarkSurfaceElevated2,
    surfaceContainer = DarkSurfaceElevated1,
    surfaceContainerLow = DarkSurface,
)

private fun lightSchemeFor(p: HatiPalette) = lightColorScheme(
    primary = p.ink,
    secondary = p.accent,
    tertiary = p.tertiary,
    background = p.paper,
    surface = p.paper,
    surfaceVariant = p.surfaceVariant,
    onPrimary = p.paper,
    onSecondary = p.ink,
    onTertiary = p.ink,
    onBackground = p.ink,
    onSurface = p.ink,
    onSurfaceVariant = p.muted,
    outline = p.muted,
    outlineVariant = p.divider,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onError = Color.White,
    onErrorContainer = ErrorRed,
    inverseSurface = p.ink,
    inverseOnSurface = p.paper,
    surfaceContainerHighest = p.surfaceVariant,
    surfaceContainerHigh = lerp(p.paper, p.surfaceVariant, 0.6f),
    surfaceContainer = lerp(p.paper, p.surfaceVariant, 0.35f),
    surfaceContainerLow = p.paper,
)

@Composable
fun HatiV2Theme(
    themeMode: ThemeMode = ThemeMode.System,
    palette: HatiPalette = HatiPalettes.Classic,
    densityScale: Float = 1f,
    textScale: Float = 1f,
    // Back-compat: callers passing darkTheme directly still work.
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light  -> false
        ThemeMode.Dark   -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette, darkTheme) {
        if (darkTheme) darkSchemeFor(palette) else lightSchemeFor(palette)
    }
    SideEffect { ActivePalette.current = palette }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val baseDensity = LocalDensity.current
    // Multiply only density — leave fontScale untouched so the Android
    // system Settings → Display → Font size still works independently
    // of the in-app text-size preference.
    val scaledDensity = Density(
        density = baseDensity.density * densityScale,
        fontScale = baseDensity.fontScale
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalDensityScale provides densityScale,
        LocalTextScale provides textScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.scaledBy(textScale),
            shapes = Shapes,
            content = content
        )
    }
}
