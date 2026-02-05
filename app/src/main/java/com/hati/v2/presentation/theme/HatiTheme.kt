package com.hati.v2.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * HATI_v2 Custom Theme.
 * No Material3 - pure custom design system.
 */

// Local providers for theme access
val LocalMangaColors = staticCompositionLocalOf { MangaColors }
val LocalMangaTypography = staticCompositionLocalOf { MangaTypography }

@Composable
fun HatiTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMangaColors provides MangaColors,
        LocalMangaTypography provides MangaTypography,
        content = content
    )
}

// Extension properties for easy access
object HatiTheme {
    val colors: MangaColors
        @Composable
        get() = LocalMangaColors.current
    
    val typography: MangaTypography
        @Composable
        get() = LocalMangaTypography.current
}
