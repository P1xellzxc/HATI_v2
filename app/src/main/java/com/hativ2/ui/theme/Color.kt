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
val NotionYellow = Color(0xFFFEF08A) // Pantone 600 C   — bg-yellow-200
val NotionGreen  = Color(0xFFBBF7D0) // Pantone 344 C   — bg-green-200
val NotionPurple = Color(0xFFE9D5FF) // Pantone 2635 C  — bg-purple-200
val NotionPink   = Color(0xFFFBCFE8) // Pantone 7430 C  — bg-pink-200
val NotionOrange = Color(0xFFFED7AA) // Pantone 7507 C  — bg-orange-200
val NotionGray   = Color(0xFFE5E7EB) // Pantone Cool Gray 2 C — bg-gray-200
val NotionWhite  = Color(0xFFFFFFFF) // Pantone 11-0601 TCX (Bright White)

// ── Semantic grays (30 % secondary layer) ───────────────────
// These replace raw Color.Gray / Color.LightGray throughout the app
// to keep every tint inside the design system.
val NotionMuted    = Color(0xFF6B7280) // Pantone Cool Gray 8 C — secondary text, labels
val NotionDivider  = Color(0xFFD1D5DB) // Pantone Cool Gray 3 C — dividers, light separators
val NotionDisabled = Color(0xFF9CA3AF) // Pantone Cool Gray 5 C — disabled states, placeholders

// ── Manga High Contrast (60 % + 30 % structural layer) ─────
val MangaBlack   = Color(0xFF000000) // Pantone Black C  — text, borders, shadows
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
val DarkNotionYellow = Color(0xFFFFF3B0) // hsl(50, 60%, 84%) — desaturated yellow
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
