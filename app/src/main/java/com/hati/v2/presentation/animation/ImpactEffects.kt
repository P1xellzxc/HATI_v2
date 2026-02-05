package com.hati.v2.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * ImpactEffects - Screen shake and squash effects on landing.
 * 
 * Impact Effect:
 * - When an element lands, apply a "Screen Shake" or "Squash" effect
 * - Creates dynamic, manga-style impact feel
 */

/**
 * Screen shake effect state holder.
 */
class ShakeState {
    private val offsetX = Animatable(0f)
    private val offsetY = Animatable(0f)
    
    val x: Float get() = offsetX.value
    val y: Float get() = offsetY.value
    
    suspend fun shake(
        intensity: Float = 10f,
        durationMs: Int = 300
    ) {
        val iterations = 6
        val delayPerIteration = durationMs / iterations
        
        repeat(iterations) { i ->
            val dampening = 1f - (i.toFloat() / iterations)
            val randomX = (Random.nextFloat() - 0.5f) * intensity * dampening
            val randomY = (Random.nextFloat() - 0.5f) * intensity * dampening
            
            kotlinx.coroutines.launch { 
                offsetX.animateTo(randomX, tween(delayPerIteration / 2))
            }
            offsetY.animateTo(randomY, tween(delayPerIteration / 2))
        }
        
        // Return to origin
        kotlinx.coroutines.coroutineScope {
            kotlinx.coroutines.launch { offsetX.animateTo(0f, spring()) }
            offsetY.animateTo(0f, spring())
        }
    }
}

@Composable
fun rememberShakeState(): ShakeState {
    return remember { ShakeState() }
}

/**
 * Container that shakes on impact.
 */
@Composable
fun ShakeableContainer(
    modifier: Modifier = Modifier,
    shakeState: ShakeState = rememberShakeState(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .offset { IntOffset(shakeState.x.roundToInt(), shakeState.y.roundToInt()) }
    ) {
        content()
    }
}

/**
 * Squash effect - compresses vertically and expands horizontally on impact.
 */
@Composable
fun SquashOnImpact(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            // Squash: compress Y, expand X
            kotlinx.coroutines.launch {
                scaleX.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                scaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            
            scaleY.animateTo(
                targetValue = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleY.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scaleX.value
                this.scaleY = scaleY.value
                // Keep bottom anchored
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            }
    ) {
        content()
    }
}

/**
 * Combined falling + squash effect for complete "antigravity" landing.
 */
@Composable
fun AntigravityEntrance(
    modifier: Modifier = Modifier,
    delay: Long = 0L,
    content: @Composable BoxScope.() -> Unit
) {
    var hasLanded by remember { mutableStateOf(false) }
    
    SquashOnImpact(
        modifier = modifier,
        trigger = hasLanded
    ) {
        FallingLayout(
            delay = delay,
            onLanded = { hasLanded = true }
        ) {
            content()
        }
    }
}
