package com.hativ2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Manga Header Style (Impact-like)
val MangaHeaderStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 24.sp,
    letterSpacing = 1.sp
)

// Manga Body Style
val MangaBodyStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp
)

val Typography = Typography(
    displayLarge = MangaHeaderStyle.copy(fontSize = 32.sp),
    displayMedium = MangaHeaderStyle.copy(fontSize = 28.sp),
    displaySmall = MangaHeaderStyle.copy(fontSize = 24.sp),
    headlineLarge = MangaHeaderStyle.copy(fontSize = 22.sp),
    headlineMedium = MangaHeaderStyle.copy(fontSize = 20.sp),
    headlineSmall = MangaHeaderStyle.copy(fontSize = 18.sp),
    titleLarge = MangaHeaderStyle.copy(fontSize = 20.sp),
    titleMedium = MangaHeaderStyle.copy(fontSize = 18.sp),
    titleSmall = MangaHeaderStyle.copy(fontSize = 16.sp),
    bodyLarge = MangaBodyStyle,
    bodyMedium = MangaBodyStyle.copy(fontSize = 14.sp),
    bodySmall = MangaBodyStyle.copy(fontSize = 12.sp)
)
