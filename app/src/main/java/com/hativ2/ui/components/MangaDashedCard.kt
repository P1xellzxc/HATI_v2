package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Card with a dashed black border instead of the usual solid one.
 * Used for empty-state slots ("+ New Volume", "Add Photo") to
 * signal an additive action without competing visually with the
 * primary content cards.
 *
 * Has no hard shadow — dashed cards are the "Background" depth
 * layer (LayerDepth.Background) and should sit flush.
 */
@Composable
fun MangaDashedCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    backgroundColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.onSurface
    val strokeWidth = with(androidx.compose.ui.platform.LocalDensity.current) { MangaBorderWidth.toPx() }
    val cornerRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { MangaCornerRadius.toPx() }
    val dashPx = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MangaCornerRadius))
            .background(backgroundColor, RoundedCornerShape(MangaCornerRadius))
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
