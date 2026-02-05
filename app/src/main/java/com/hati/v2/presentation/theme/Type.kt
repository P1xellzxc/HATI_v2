package com.hati.v2.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hati.v2.R

/**
 * HATI_v2 Manga Typography System.
 * Comic/Manga style headers using Bangers font.
 */

// Bangers font family for manga-style headers
val BangersFamily = FontFamily(
    Font(R.font.bangers, FontWeight.Normal)
)

// Fallback system font for body text
val MangaBodyFamily = FontFamily.Default

object MangaTypography {
    // Display - Large impact headers
    val displayLarge = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = 2.sp
    )
    
    val displayMedium = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.5.sp
    )
    
    val displaySmall = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.sp
    )
    
    // Headlines
    val headlineLarge = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    )
    
    val headlineMedium = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    val headlineSmall = TextStyle(
        fontFamily = BangersFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    
    // Body text - uses system font for readability
    val bodyLarge = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    val bodyMedium = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val bodySmall = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    // Labels
    val labelLarge = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val labelMedium = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    val labelSmall = TextStyle(
        fontFamily = MangaBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
}
