package com.hativ2.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import com.hativ2.ui.theme.LayerDepth

/**
 * Dark circular floating action button — the "Floating" depth layer
 * in the cinematography model. Appears on top-level screens that
 * need a global add action (Dashboard, Charts).
 */
@Composable
fun MangaFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = "Add",
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface,
    contentColor: Color = MaterialTheme.colorScheme.surface,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) LayerDepth.Floating.shadow else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabPress"
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else LayerDepth.Floating.shadow,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabShadow"
    )

    Box(modifier = modifier.size(56.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = pressOffset, y = pressOffset)
                .background(backgroundColor, CircleShape)
                .border(MangaBorderWidth, MaterialTheme.colorScheme.onSurface, CircleShape)
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
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
