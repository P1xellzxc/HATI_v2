package com.hati.v2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hati.v2.presentation.theme.MangaColors

/**
 * MangaCard - Core component of the HATI_v2 Manga Finance design system.
 * 
 * Features:
 * - 4dp solid black border on all containers
 * - Hard-edge drop shadow (4dp X, 4dp Y offset, no blur)
 * - Optional halftone texture background
 */
@Composable
fun MangaCard(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 4.dp,
    shadowOffset: Dp = 4.dp,
    backgroundColor: Color = MangaColors.White,
    borderColor: Color = MangaColors.Black,
    shadowColor: Color = MangaColors.Black,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .mangaDropShadow(
                offset = shadowOffset,
                color = shadowColor
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RectangleShape
            )
            .background(backgroundColor)
            .padding(borderWidth) // Inner padding to prevent content touching border
    ) {
        content()
    }
}

/**
 * Custom modifier for hard-edge drop shadow (no blur).
 * Creates the classic manga/comic book shadow effect.
 */
fun Modifier.mangaDropShadow(
    offset: Dp = 4.dp,
    color: Color = MangaColors.Black
): Modifier = this.drawBehind {
    val shadowOffsetPx = offset.toPx()
    
    // Draw hard-edge shadow rectangle behind the content
    drawRect(
        color = color,
        topLeft = Offset(shadowOffsetPx, shadowOffsetPx),
        size = Size(size.width, size.height)
    )
}

/**
 * Elevated variant with larger shadow for interactive elements.
 */
@Composable
fun MangaCardElevated(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    MangaCard(
        modifier = modifier,
        shadowOffset = 6.dp,
        content = content
    )
}

/**
 * Pressed variant with reduced shadow for pressed state.
 */
@Composable
fun MangaCardPressed(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    MangaCard(
        modifier = modifier
            .offset(x = 2.dp, y = 2.dp), // Move down-right when pressed
        shadowOffset = 2.dp,
        content = content
    )
}
