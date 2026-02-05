package com.hati.v2.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * FallingLayout - Antigravity Physics System.
 * 
 * Entrance Animation:
 * - All UI elements enter the screen by "falling" from the top
 * - Physics: spring(dampingRatio = DampingRatioMediumBouncy, stiffness = StiffnessLow)
 * - Elements start at y: -screenHeight and animate to targetY
 */

/**
 * A composable that makes its content "fall" into position with spring physics.
 * 
 * @param delay Delay before starting the animation (in ms), useful for staggered effects
 * @param onLanded Callback when the element has landed (for impact effects)
 */
@Composable
fun FallingLayout(
    modifier: Modifier = Modifier,
    delay: Long = 0L,
    onLanded: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Animation state - start offscreen (negative Y)
    val offsetY = remember { Animatable(-screenHeightPx) }
    var hasLanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Optional delay for staggered animations
        if (delay > 0) {
            kotlinx.coroutines.delay(delay)
        }
        
        // Animate to final position with spring physics
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        
        // Trigger landing callback
        if (!hasLanded) {
            hasLanded = true
            onLanded()
        }
    }
    
    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
    ) {
        content()
    }
}

/**
 * Modifier extension for falling entrance animation.
 * Use this when you want to apply falling physics to any composable.
 */
@Composable
fun Modifier.fallingEntrance(
    delay: Long = 0L,
    onLanded: () -> Unit = {}
): Modifier {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val offsetY = remember { Animatable(-screenHeightPx) }
    var hasLanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (delay > 0) {
            kotlinx.coroutines.delay(delay)
        }
        
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        
        if (!hasLanded) {
            hasLanded = true
            onLanded()
        }
    }
    
    return this.offset { IntOffset(0, offsetY.value.roundToInt()) }
}

/**
 * Staggered falling animation for lists of items.
 * Each item falls with a delay based on its index.
 */
@Composable
fun StaggeredFallingColumn(
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 50L,
    itemCount: Int,
    content: @Composable (index: Int) -> Unit
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        repeat(itemCount) { index ->
            FallingLayout(
                delay = index * staggerDelayMs
            ) {
                content(index)
            }
        }
    }
}
