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

package com.google.jetstream.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.imageLoader
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.presentation.AppViewModel
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding

/**
 * Immersive list row that displays a background preview of the focused movie/show
 * Based on Android TV Material3 Immersive List design guidelines
 * https://developer.android.com/design/ui/tv/guides/components/immersive-list
 */
@Composable
fun ImmersiveListRow(
    movieList: MovieList,
    modifier: Modifier = Modifier,
    title: String? = null,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    onMovieSelected: (Movie) -> Unit = {},
    gradientColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    initialFocused: Boolean = false, // Show preview on load
    showTitleWhenFocused: Boolean = false, // Keep title visible when focused
    onMovieFocusedForBackground: ((Movie?, Boolean) -> Unit)? = null, // Callback to update fixed background
) {
    if (movieList.isEmpty()) {
        return
    }

    var isListFocused by remember { mutableStateOf(initialFocused) }
    var hasUserInteracted by remember { mutableStateOf(false) }
    var selectedMovie by remember(movieList) { mutableStateOf(movieList.first()) }
    
    // Show preview only if actually focused, or if initially focused AND user hasn't interacted yet
    // This ensures only one preview is visible at a time after user starts navigating
    val showPreview = if (hasUserInteracted) {
        isListFocused // After interaction, only show when actually focused
    } else {
        isListFocused || initialFocused // On initial load, show if marked as initial
    }

    val sectionTitle = if (showPreview && !showTitleWhenFocused) {
        null // Hide title when preview is shown (unless showTitleWhenFocused is true)
    } else {
        title
    }

    // Update fixed background when preview state or selected movie changes
    LaunchedEffect(showPreview, selectedMovie, isListFocused) {
        onMovieFocusedForBackground?.invoke(
            if (showPreview) selectedMovie else null,
            showPreview
        )
    }
    
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(
                top = if (showPreview) 150.dp else 0.dp // Push cards down significantly to prevent them from being too high when focused
            )
        ) {
            // Movie title overlay when preview is shown (synopsis disabled)
            if (showPreview) {
                Text(
                    text = selectedMovie.name,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = startPadding,
                        bottom = 32.dp
                    )
                )
            }

            // Movie row with horizontal cards
            ImmersiveListMoviesRow(
                movieList = movieList,
                itemDirection = ItemDirection.Horizontal, // Use horizontal cards for backdrops
                title = sectionTitle,
                startPadding = startPadding,
                endPadding = endPadding,
                titleStyle = titleStyle,
                showItemTitle = !showPreview, // Hide titles when preview is shown (description shown instead)
                onMovieSelected = onMovieSelected,
                onMovieFocused = { movie ->
                    selectedMovie = movie
                },
                modifier = Modifier.onFocusChanged { focusState ->
                    val wasFocused = isListFocused
                    isListFocused = focusState.hasFocus
                    // Mark as interacted when focus changes (user is navigating)
                    if (focusState.hasFocus != wasFocused) {
                        hasUserInteracted = true
                    }
                    // Ensure first movie is selected when row gains focus
                    if (focusState.hasFocus && !wasFocused) {
                        selectedMovie = movieList.first()
                    }
                }
            )
        }
    }
}

@Composable
internal fun ImmersiveBackground(
    movie: Movie,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onColorExtracted: ((Color) -> Unit)? = null, // Callback to expose extracted color for home screen background
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val appViewModel: AppViewModel = hiltViewModel()
    val serverUrl = appViewModel.preferences.serverUrl ?: ""
    
    // Extract dominant color from backdrop image for background (per-pixel dynamic color)
    var backgroundColor by remember(movie.id) { 
        mutableStateOf<Color>(Color.Black) // Default to black
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Crossfade(
            targetState = movie,
            animationSpec = tween(400), // Smooth fade transition between images
            label = "backdropCrossfade"
        ) { currentMovie ->
            // Get backdrop URL: construct backdrop URL from server URL and movie ID
            val backdropUri = remember(currentMovie.id, serverUrl, currentMovie.posterUri, currentMovie.videoUri) {
                if (serverUrl.isNotBlank()) {
                    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                    "${normalizedServerUrl}Items/${currentMovie.id}/Images/Backdrop?maxWidth=3840"
                } else {
                    // Fallback: try to extract server URL from existing URIs
                    val baseUrl = currentMovie.posterUri.substringBefore("/Items/")
                        .ifEmpty { currentMovie.videoUri.substringBefore("/Videos/") }
                    if (baseUrl.isNotBlank() && currentMovie.id.isNotBlank()) {
                        "$baseUrl/Items/${currentMovie.id}/Images/Backdrop?maxWidth=3840"
                    } else {
                        currentMovie.posterUri // Fallback to posterUri if we can't construct backdrop URL
                    }
                }
            }
            
            // Extract dominant color from image for per-pixel dynamic background
            LaunchedEffect(currentMovie.id, backdropUri, visible) {
                if (visible) {
                    // Reset to black while loading new color
                    backgroundColor = Color.Black
                    onColorExtracted?.invoke(Color.Black)
                    
                    withContext(Dispatchers.IO) {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(backdropUri)
                                .allowHardware(false) // Required for Palette
                                .build()
                            
                            val result = imageLoader.execute(request)
                            if (result is SuccessResult) {
                                val drawable = result.drawable
                                val bitmap = drawableToBitmap(drawable)
                                val palette = Palette.from(bitmap)
                                    .maximumColorCount(16)
                                    .generate()
                                
                                // Use dominant color, or muted color, or vibrant color as fallback
                                val dominantColor = palette.dominantSwatch?.rgb
                                    ?: palette.mutedSwatch?.rgb
                                    ?: palette.vibrantSwatch?.rgb
                                    ?: 0xFF000000.toInt() // Fallback to black
                                
                                val extractedColor = Color(dominantColor)
                                backgroundColor = extractedColor
                                // Expose extracted color for home screen background
                                onColorExtracted?.invoke(extractedColor)
                            }
                        } catch (e: Exception) {
                            // Keep default black on error
                            backgroundColor = Color.Black
                            onColorExtracted?.invoke(Color.Black)
                        }
                    }
                } else {
                    // Reset to black when background is hidden
                    backgroundColor = Color.Black
                    onColorExtracted?.invoke(Color.Black)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .background(backgroundColor), // Use extracted color as background (per-pixel dynamic color)
                contentAlignment = Alignment.TopEnd // Align content to top-right corner
            ) {
                // Background image aligned to top-right corner per guidelines
                // Scale image larger and clip to show top-right portion
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backdropUri)
                        .crossfade(400) // Smooth crossfade for image loading
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // Crop to maintain aspect ratio
                    alignment = Alignment.TopEnd, // Align to top-right
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Scale image 60% smaller
                            transformOrigin = TransformOrigin(1f, 0f) // Anchor to top-right
                            scaleX = 0.6f // 60% smaller than original (1.5f * 0.4 = 0.6f)
                            scaleY = 0.6f // 60% smaller than original
                        }
                        .cinematicScrim() // Apply cinematic scrim with gradients from left and bottom
                )
                
                // Left edge fade to blend with black background - drawn on top of image
                // Using Box with gradient overlay - positioned after AsyncImage so it draws on top
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .leftEdgeFade()
                )
            }
        }
    }
}

@Composable
private fun MovieDescription(
    movie: Movie,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = movie.name,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.fillMaxWidth(0.5f),
            text = movie.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontWeight = FontWeight.Light,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Left edge fade to blend background image with black background
 * Fades the left side of the image from black to transparent
 */
private fun Modifier.leftEdgeFade(): Modifier =
    drawWithCache {
        // Stronger gradient from black (left) to transparent (right) to blend edges
        val leftFadeGradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Black, // Solid black on left edge
                Color.Black.copy(alpha = 0.95f), // Very strong black
                Color.Black.copy(alpha = 0.85f), // Strong black
                Color.Black.copy(alpha = 0.7f), // Medium-strong fade
                Color.Black.copy(alpha = 0.5f), // Medium fade
                Color.Black.copy(alpha = 0.3f), // Light fade
                Color.Black.copy(alpha = 0.1f), // Very light fade
                Color.Transparent // Fully transparent on right
            ),
            startX = 0f,
            endX = size.width.times(0.6f) // Fade covers left 60% of width for stronger blend
        )

        onDrawWithContent {
            // Draw content first (empty for this Box, but keeps drawing order correct)
            drawContent()
            // Draw gradient overlay on top to fade left edge into background
            drawRect(leftFadeGradient)
        }
    }

/**
 * Helper function to convert Drawable to Bitmap for Palette color extraction
 */
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    
    return bitmap
}

/**
 * Cinematic scrim overlay per Android TV Immersive List guidelines
 * Provides darkening gradient from left and bottom to ensure text readability
 * and create a cinematic experience with the subject in top-right
 * Reference: https://developer.android.com/design/ui/tv/guides/components/immersive-list
 */
private fun Modifier.cinematicScrim(): Modifier =
    drawWithCache {
        // Left-to-right gradient for darkening from left edge (30% stronger)
        val leftGradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.99f), // Strong darkening on left (30% stronger: 0.792 * 1.3 ≈ 0.99)
                Color.Black.copy(alpha = 0.815f), // 30% stronger: 0.627 * 1.3 = 0.815
                Color.Black.copy(alpha = 0.408f), // 30% stronger: 0.314 * 1.3 = 0.408
                Color.Transparent // No darkening on right (where subject is)
            ),
            startX = 0f,
            endX = size.width * 0.5f // Gradient extends 50% across width
        )
        
        // Bottom-to-top gradient for darkening from bottom edge (30% stronger, covers full height)
        val bottomGradient = Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.99f), // Strong darkening on bottom (30% stronger: 0.784 * 1.3 ≈ 0.99)
                Color.Black.copy(alpha = 0.611f), // 30% stronger: 0.470 * 1.3 = 0.611
                Color.Black.copy(alpha = 0.408f), // 30% stronger: 0.314 * 1.3 = 0.408
                Color.Black.copy(alpha = 0.204f), // 30% stronger: 0.157 * 1.3 = 0.204
                Color.Transparent // No darkening on top (where subject is)
            ),
            startY = 0f, // Start gradient from top to cover full height
            endY = size.height // Full height coverage for equal darkening on both halves
        )

        onDrawWithContent {
            drawContent()
            // Apply both gradients to create cinematic scrim effect (30% stronger for both top and bottom)
            // Left-to-right gradient first
            drawRect(leftGradient)
            // Bottom-to-top gradient second (will blend with left gradient)
            drawRect(bottomGradient)
        }
    }

