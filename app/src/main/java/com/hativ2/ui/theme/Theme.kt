package com.hativ2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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

private val DarkColorScheme = darkColorScheme(
    primary = DarkOnSurface,
    secondary = DarkNotionYellow,
    tertiary = DarkNotionBlue,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated1,
    onPrimary = DarkSurface,
    onSecondary = DarkSurface,
    onTertiary = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurface.copy(alpha = 0.8f),
    outline = NotionMuted,
    outlineVariant = Color(0xFF444444),
    error = DarkErrorRed,
    errorContainer = DarkErrorContainer,
    onError = DarkSurface,
    onErrorContainer = DarkErrorRed,
    inverseSurface = NotionWhite,
    inverseOnSurface = DarkSurface,
    surfaceContainerHighest = DarkSurfaceElevated3,
    surfaceContainerHigh = DarkSurfaceElevated2,
    surfaceContainer = DarkSurfaceElevated1,
    surfaceContainerLow = DarkSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = MangaBlack,
    secondary = NotionYellow,
    tertiary = NotionBlue,
    background = NotionWhite,
    surface = NotionWhite,
    surfaceVariant = NotionGray,
    onPrimary = NotionWhite,
    onSecondary = MangaBlack,
    onTertiary = MangaBlack,
    onBackground = MangaBlack,
    onSurface = MangaBlack,
    onSurfaceVariant = NotionMuted,
    outline = NotionMuted,
    outlineVariant = NotionDivider,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onError = NotionWhite,
    onErrorContainer = ErrorRed,
    inverseSurface = MangaBlack,
    inverseOnSurface = NotionWhite,
    surfaceContainerHighest = NotionGray,
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerLow = NotionWhite,
)

@Composable
fun HatiV2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We prefer our custom colors over dynamic colors for the specific aesthetic
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //     val context = LocalContext.current
        //     if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}