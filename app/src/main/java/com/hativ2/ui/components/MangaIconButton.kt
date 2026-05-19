package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Square bordered icon button used in top bars (back, download, help),
 * volume card corners (pencil, trash), and any place we need a small
 * tap target with the manga outline treatment. Pairs the existing
 * MangaCard/MangaButton style — 2dp border + 2dp shadow + spring press.
 */
@Composable
fun MangaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val offset by animateDpAsState(
        targetValue = if (isPressed) MangaShadowOffsetSmall else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconBtnPress"
    )
    val shadow by animateDpAsState(
        targetValue = if (isPressed) 0.dp else MangaShadowOffsetSmall,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconBtnShadow"
    )

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadow, y = shadow)
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = offset, y = offset)
                .background(backgroundColor, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(MangaCornerRadius))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
