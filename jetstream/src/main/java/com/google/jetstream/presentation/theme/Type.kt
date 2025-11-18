/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jetstream.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

// Use default Android TV font (Roboto) for all typography
// FontFamily.Default will use the system default sans-serif font
val DefaultTvFontFamily = FontFamily.Default

// Keep LexendExa for backwards compatibility but use default font
val LexendExa = FontFamily.Default

// Set of Material typography styles to start with
// All sizes reduced by 30% (multiplied by 0.7)
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 39.9.sp, // 30% smaller
        lineHeight = 44.8.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.175).sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    displayMedium = TextStyle(
        fontSize = 31.5.sp, // 30% smaller
        lineHeight = 36.4.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    displaySmall = TextStyle(
        fontSize = 25.2.sp, // 30% smaller
        lineHeight = 30.8.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineLarge = TextStyle(
        fontSize = 22.4.sp, // 30% smaller
        lineHeight = 28.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineMedium = TextStyle(
        fontSize = 19.6.sp, // 30% smaller
        lineHeight = 25.2.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineSmall = TextStyle(
        fontSize = 16.8.sp, // 30% smaller
        lineHeight = 22.4.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleLarge = TextStyle(
        fontSize = 18.2.sp, // 30% smaller
        lineHeight = 22.4.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleMedium = TextStyle(
        fontSize = 14.sp, // 30% smaller
        lineHeight = 19.6.sp, // 30% smaller
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.105.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleSmall = TextStyle(
        fontSize = 12.6.sp, // 30% smaller
        lineHeight = 16.8.sp, // 30% smaller
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.07.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelLarge = TextStyle(
        fontSize = 9.8.sp, // 30% smaller
        lineHeight = 14.sp, // 30% smaller
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.07.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelMedium = TextStyle(
        fontSize = 8.4.sp, // 30% smaller
        lineHeight = 11.2.sp, // 30% smaller
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.175.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelSmall = TextStyle(
        fontSize = 7.7.sp, // 30% smaller
        lineHeight = 11.2.sp, // 30% smaller
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.07.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp, // 30% smaller
        lineHeight = 19.6.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.35.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodyMedium = TextStyle(
        fontSize = 12.6.sp, // 30% smaller
        lineHeight = 16.8.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.175.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodySmall = TextStyle(
        fontSize = 11.2.sp, // 30% smaller
        lineHeight = 15.4.sp, // 30% smaller
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.14.sp, // 30% smaller
        fontFamily = DefaultTvFontFamily,
        textMotion = TextMotion.Animated
    )
)
