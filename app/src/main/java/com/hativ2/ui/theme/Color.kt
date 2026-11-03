package com.hativ2.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// HATI² Color Palette — Manga × Notion Design System
// ─────────────────────────────────────────────────────────────
//
// 60-30-10 COLOR RULE (whole-app breakdown):
//
//   60 % DOMINANT  — NotionWhite / DarkSurface (backgrounds, surfaces)
//                    These occupy the vast majority of screen real estate
//                    and provide visual breathing room.
//
//   30 % SECONDARY — MangaBlack / NotionWhite (text, borders, shadows)
//                    + NotionMuted (secondary text, placeholders)
//                    + NotionDivider (separators, disabled surfaces)
//                    High-contrast manga outlines reinforce the 30 % layer.
//
//   10 % ACCENT    — NotionYellow (primary accent / CTA highlights)
//                    + Semantic pastels (Green, Red, Blue) for status
//                    + Category pastels (Orange, Purple, Pink) for charts
//                    Accent colors should never dominate a section.
//
// Each colour is annotated with its closest Pantone Coated match
// so the palette can be reproduced in print or cross-platform tooling.
//
// DESIGN REFERENCE: Think in HSB, export in hex. All dark-mode
// variants reduce saturation and increase lightness per the rule:
//   Light: hsl(H, S%, L%)  →  Dark: hsl(H, S-20%, L+20%)
// ─────────────────────────────────────────────────────────────

// ── Notion Pastels (10 % accent layer) ──────────────────────
val NotionRed    = Color(0xFFFECACA) // Pantone 7422 C  — bg-red-200
val NotionBlue   = Color(0xFFBFDBFE) // Pantone 2707 C  — bg-blue-200
val PastelYellow = Color(0xFFFEF08A) // Pantone 600 C   — bg-yellow-200 (fixed, for named volume colors)
val NotionGreen  = Color(0xFFBBF7D0) // Pantone 344 C   — bg-green-200
val NotionPurple = Color(0xFFE9D5FF) // Pantone 2635 C  — bg-purple-200
val NotionPink   = Color(0xFFFBCFE8) // Pantone 7430 C  — bg-pink-200
val NotionOrange = Color(0xFFFED7AA) // Pantone 7507 C  — bg-orange-200
val NotionGray   = Color(0xFFE5E7EB) // Pantone Cool Gray 2 C — bg-gray-200

// ── Themed roles ────────────────────────────────────────────
// These resolve from the user's chosen HatiPalette (see HatiPalette.kt).
// Under the Classic palette they keep their original values.
val NotionYellow: Color get() = ActivePalette.current.accent   // primary accent / CTA
val NotionWhite:  Color get() = ActivePalette.current.paper    // paper surface

// ── Semantic grays (30 % secondary layer) ───────────────────
// These replace raw Color.Gray / Color.LightGray throughout the app
// to keep every tint inside the design system.
val NotionMuted:    Color get() = ActivePalette.current.muted    // secondary text, labels
val NotionDivider:  Color get() = ActivePalette.current.divider  // dividers, light separators
val NotionDisabled: Color get() = ActivePalette.current.disabled // disabled states, placeholders

// ── Manga High Contrast (60 % + 30 % structural layer) ─────
val MangaBlack: Color get() = ActivePalette.current.ink // text, borders, shadows
val MangaSuccess = Color(0xFF22C55E) // Pantone 7481 C   — positive / success
val MangaDarkGray = Color(0xFF555555) // Pantone Cool Gray 10 C — heavy-disabled

// ── Dark Mode Surface System ────────────────────────────────
// Rule: Dark mode surface is NOT #000000; pure black causes eye
// strain. Use #121212 as base, then elevate surfaces with +5%
// lightness per elevation level.
val DarkSurface          = Color(0xFF121212) // Level 0 — base dark background
val DarkSurfaceElevated1 = Color(0xFF1E1E1E) // Level 1 — cards, raised surfaces (+5%)
val DarkSurfaceElevated2 = Color(0xFF232323) // Level 2 — menus, dialogs (+10%)
val DarkSurfaceElevated3 = Color(0xFF2C2C2C) // Level 3 — modal overlays (+15%)
val DarkOnSurface        = Color(0xFFE0E0E0) // Slightly off-white for readability

// ── Dark Mode Accent Variants (desaturated + lighter) ───────
// Reduce saturation in dark mode to avoid visual strain.
val DarkNotionYellow: Color get() = ActivePalette.current.darkAccent // desaturated accent
val DarkNotionBlue   = Color(0xFFCCE5FF) // hsl(210, 60%, 90%) — desaturated blue
val DarkNotionGreen  = Color(0xFFB3EACA) // hsl(145, 45%, 82%) — desaturated green
val DarkNotionRed    = Color(0xFFFFD4D4) // hsl(0, 60%, 91%)  — desaturated red

// ── Error System ────────────────────────────────────────────
// Rule: Never use Error red for anything that is not an error.
// Red means broken. Treat it like a fire alarm.
val ErrorRed            = Color(0xFFB3261E) // MD3 error baseline
val ErrorRedContainer   = Color(0xFFF9DEDC) // Light-mode error container
val DarkErrorRed        = Color(0xFFF2B8B5) // Dark-mode on-error
val DarkErrorContainer  = Color(0xFF8C1D18) // Dark-mode error container

// ── Hex Constants for Data Layer ────────────────────────────
const val HEX_NOTION_ORANGE = "#FED7AA"

// ─────────────────────────────────────────────────────────────
// CINEMATOGRAPHY SEMANTIC TOKENS
// ─────────────────────────────────────────────────────────────
//
// Each screen picks ≤3 hues from this layer (the "film LUT" rule):
// a single warm focal pill + one cool accent for status + the
// neutral surface. Do not freelance with raw NotionX colors in
// new code — pick a semantic token that documents the intent.
//
// Warm/cool contrast: warm (cream/peach) focal pills sit on the
// cool neutral surface to feel "lit" rather than flat — the same
// trick used in film color grading.
// ─────────────────────────────────────────────────────────────

// Focal cream pill — the single hero amount on each screen.
// Used by: Dashboard balance, Charts total spending, AddExpense
// amount input. Never repeated more than once per screen.
val SurfaceHero: Color     get() = ActivePalette.current.hero     // warm focal pill
val DarkSurfaceHero: Color get() = ActivePalette.current.darkHero // desaturated dark equivalent

// Status accents. Always paired warm/cool (positive=cool-green,
// debit=warm-pink) so glance-readability is preserved.
val AccentPositive     = NotionGreen
val AccentDebit        = NotionPink
val DarkAccentPositive = DarkNotionGreen
val DarkAccentDebit    = Color(0xFFE8B8C8)

// Selected-category fills for the chip grid. The peach for Food &
// Drinks is the warm signature in the AddExpense screen; the rest
// stay outlined to keep ≤3 hues active at once.
val AccentCategoryFood: Color     get() = ActivePalette.current.categoryFood // selected chip
val DarkAccentCategoryFood: Color get() = ActivePalette.current.darkCategoryFood

// ─────────────────────────────────────────────────────────────
// GLASSMORPHISM LAYER SYSTEM
// ─────────────────────────────────────────────────────────────
// Glass surfaces use translucent fills + blur (via Modifier.blur
// on API 31+). Three opacity levels for layering depth.
// Light: white-tinted glass. Dark: white at lower opacity (dark bg
// already provides depth). Border is always very subtle.
// ─────────────────────────────────────────────────────────────

// Light mode glass layers (white-tinted)
val GlassSurfaceLight    = Color(0x0DFFFFFF) // 5% white
val GlassSurfaceMidLight = Color(0x1AFFFFFF) // 10% white
val GlassBorderLight     = Color(0x26000000) // 15% black border

// Dark mode glass layers (white on dark bg)
val GlassSurfaceDark     = Color(0x0EFFFFFF) // 5.5% white
val GlassSurfaceMidDark  = Color(0x1CFFFFFF) // 11% white
val GlassBorderDark      = Color(0x24FFFFFF) // 14% white border

// Dark gradient bg for glass screens
val GlassBgStart         = Color(0xFF08091C)
val GlassBgEnd           = Color(0xFF0F0B2C)
