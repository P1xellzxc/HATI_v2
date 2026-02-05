package com.hati.v2.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * AGSL (Android Graphics Shading Language) Halftone Dot Shader.
 * Creates a classic manga/newspaper halftone pattern effect.
 * 
 * Requires Android 13+ (API 33) for AGSL support.
 */

private const val HALFTONE_SHADER = """
    uniform float2 resolution;
    uniform float dotSize;
    uniform float spacing;
    
    half4 main(float2 fragCoord) {
        // Calculate grid position
        float2 gridPos = mod(fragCoord, spacing);
        float2 center = float2(spacing / 2.0);
        
        // Distance from center of grid cell
        float dist = distance(gridPos, center);
        
        // Create dot based on distance
        float dotRadius = dotSize / 2.0;
        
        if (dist < dotRadius) {
            // Black dot
            return half4(0.0, 0.0, 0.0, 0.3);
        } else {
            // Transparent
            return half4(0.0, 0.0, 0.0, 0.0);
        }
    }
"""

/**
 * HalftoneBackground composable that applies halftone dot texture.
 * Falls back to plain background on older Android versions.
 */
@Composable
fun HalftoneBackground(
    modifier: Modifier = Modifier,
    dotSize: Float = 4f,
    spacing: Float = 12f,
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        HalftoneBackgroundImpl(
            modifier = modifier,
            dotSize = dotSize,
            spacing = spacing,
            content = content
        )
    } else {
        // Fallback for older devices
        Box(modifier = modifier) {
            content()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun HalftoneBackgroundImpl(
    modifier: Modifier = Modifier,
    dotSize: Float = 4f,
    spacing: Float = 12f,
    content: @Composable BoxScope.() -> Unit
) {
    val shader = remember {
        RuntimeShader(HALFTONE_SHADER).apply {
            setFloatUniform("dotSize", dotSize)
            setFloatUniform("spacing", spacing)
        }
    }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "contents")
                    .asComposeRenderEffect()
            }
    ) {
        content()
    }
}

/**
 * Alternative simpler halftone implementation using Canvas drawing.
 * Works on all Android versions.
 */
@Composable
fun HalftoneOverlay(
    modifier: Modifier = Modifier,
    dotSize: Float = 3f,
    spacing: Float = 10f
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val dotRadius = dotSize / 2f
        val rows = (size.height / spacing).toInt() + 1
        val cols = (size.width / spacing).toInt() + 1
        
        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = col * spacing + (spacing / 2)
                val y = row * spacing + (spacing / 2)
                
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.15f),
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
    }
}
