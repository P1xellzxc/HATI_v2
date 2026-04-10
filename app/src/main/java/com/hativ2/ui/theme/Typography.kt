package com.hativ2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
// HATI² Typography — Material Design 3 Type Scale
// ─────────────────────────────────────────────────────────────
//
// Weight Contrast Rule:
//   Pair Regular (400) with Bold (700). Skip Medium (500) as a
//   differentiator — it is too subtle.
//
// Line Height Rule:
//   Body text: 1.4–1.6× the font size for readability.
//   Headlines: 1.1–1.3× (tighter for visual impact).
//
// Letter Spacing Rule:
//   Labels (ALL CAPS): +0.08em minimum tracking.
//   Body text: default tracking; never track lowercase body text.
//
// Max 3 type sizes on a single screen.
// ─────────────────────────────────────────────────────────────

// Manga Header Style — used for display and headline roles
val MangaHeaderStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,          // Bold (700), not Black (900)
    letterSpacing = 0.sp
)

// Manga Body Style — used for body, title, and label roles
val MangaBodyStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,        // Regular (400), not Medium (500)
)

// MD3 Type Scale (sizes from design reference §4.1)
val Typography = Typography(
    // Display — Hero moments, splash screens, key marketing
    displayLarge = MangaHeaderStyle.copy(
        fontSize = 57.sp,
        lineHeight = 64.sp                 // ~1.12× (tight for large display)
    ),
    displayMedium = MangaHeaderStyle.copy(
        fontSize = 45.sp,
        lineHeight = 52.sp                 // ~1.16×
    ),
    displaySmall = MangaHeaderStyle.copy(
        fontSize = 36.sp,
        lineHeight = 44.sp                 // ~1.22×
    ),

    // Headline — Screen titles, section headers
    headlineLarge = MangaHeaderStyle.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp                 // 1.25×
    ),
    headlineMedium = MangaHeaderStyle.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp                 // ~1.29×
    ),
    headlineSmall = MangaHeaderStyle.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp                 // ~1.33×
    ),

    // Title — Navigation, top bars, card headers
    titleLarge = MangaHeaderStyle.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp                 // ~1.27×
    ),
    titleMedium = MangaBodyStyle.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,               // 1.5×
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.15.sp
    ),
    titleSmall = MangaBodyStyle.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,               // ~1.43×
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp
    ),

    // Body — Primary readable content
    bodyLarge = MangaBodyStyle.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,               // 1.5× — ideal for body readability
        letterSpacing = 0.5.sp
    ),
    bodyMedium = MangaBodyStyle.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,               // ~1.43×
        letterSpacing = 0.25.sp
    ),
    bodySmall = MangaBodyStyle.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,               // ~1.33×
        letterSpacing = 0.4.sp
    ),

    // Label — Buttons, chips, metadata (often ALL CAPS)
    // Letter-spacing +0.08em min for ALL CAPS labels
    labelLarge = MangaBodyStyle.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = MangaBodyStyle.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp            // increased for uppercase label use
    ),
    labelSmall = MangaBodyStyle.copy(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp            // increased for uppercase label use
    )
)
