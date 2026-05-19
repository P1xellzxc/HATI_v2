package com.hativ2.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// HATI² Adaptive Sizing — Density × Text orthogonal axes
// ─────────────────────────────────────────────────────────────
//
// Two independent multipliers live alongside the Material theme:
//
//   LocalDensityScale → multiplies Density.density only. Scales
//   icons, padding, card sizes. Does NOT touch fontScale, so the
//   Android system accessibility font setting keeps working.
//
//   LocalTextScale    → applied via Typography.scaledBy() and
//   surfaces here for any composable that needs to multiply a
//   raw .sp value (rare — prefer the typography styles).
//
// Either can be 0.875 / 1.0 / 1.125 (density) or
// 0.9 / 1.0 / 1.15 (text). Defaults to 1.0 so untouched code
// renders the same as before.
//
// LayerDepth encodes the cinematography "three z-layers" rule:
// Background (flush), Surface (4dp shadow — default card),
// Floating (8dp shadow — FAB / sticky bar / modal).
// ─────────────────────────────────────────────────────────────

val LocalDensityScale = compositionLocalOf { 1f }
val LocalTextScale    = compositionLocalOf { 1f }

enum class LayerDepth(val shadow: Dp) {
    Background(0.dp),
    Surface(4.dp),
    Floating(8.dp)
}

enum class DensityPreset(val factor: Float) {
    Compact(0.875f),
    Standard(1.0f),
    Comfortable(1.125f)
}

enum class TextSizePreset(val factor: Float) {
    Small(0.9f),
    Medium(1.0f),
    Large(1.15f)
}

enum class ThemeMode { System, Light, Dark }
