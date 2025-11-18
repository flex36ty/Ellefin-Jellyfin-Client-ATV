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

package com.google.jetstream.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.presentation.AppViewModel
import com.google.jetstream.presentation.common.ImmersiveListRow
import com.google.jetstream.presentation.common.ImmersiveBackground
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.common.Error
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MoviesRow
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.data.util.JellyfinPreferences

@Composable
fun HomeScreen(
    onMovieClick: (movie: Movie) -> Unit,
    goToVideoPlayer: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    homeScreeViewModel: HomeScreeViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val uiState by homeScreeViewModel.uiState.collectAsStateWithLifecycle()
    val carouselEnabled = appViewModel.preferences.carouselEnabled
    val immersiveListEnabled = appViewModel.preferences.immersiveListEnabled
    val dynamicBackgroundColorEnabled = appViewModel.preferences.dynamicBackgroundColorEnabled
    val blackBackgroundEnabled = appViewModel.preferences.blackBackgroundEnabled

    when (val s = uiState) {
        is HomeScreenUiState.Ready -> {
            Catalog(
                featuredMovies = if (carouselEnabled) s.featuredMovies else emptyList(),
                continueWatchingList = s.continueWatchingList,
                libraryContentRows = s.libraryContentRows,
                onMovieClick = onMovieClick,
                onScroll = onScroll,
                goToVideoPlayer = goToVideoPlayer,
                isTopBarVisible = isTopBarVisible,
                immersiveListEnabled = immersiveListEnabled,
                dynamicBackgroundColorEnabled = dynamicBackgroundColorEnabled,
                blackBackgroundEnabled = blackBackgroundEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is HomeScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is HomeScreenUiState.Error -> Error(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Catalog(
    featuredMovies: MovieList,
    continueWatchingList: MovieList,
    libraryContentRows: List<com.google.jetstream.data.entities.LibraryContentRow>,
    onMovieClick: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    goToVideoPlayer: (movie: Movie) -> Unit,
    immersiveListEnabled: Boolean = false,
    dynamicBackgroundColorEnabled: Boolean = false,
    blackBackgroundEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    isTopBarVisible: Boolean = true,
) {

    val lazyListState = rememberLazyListState()
    val childPadding = rememberChildPadding()
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val appViewModel: AppViewModel = hiltViewModel()
    val serverUrl = appViewModel.preferences.serverUrl ?: ""

    // Track focused movie for fixed background preview (per Android TV Immersive List guidelines)
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var showBackground by remember { mutableStateOf(false) }
    
    // Track extracted color for dynamic home screen background (per-pixel color extraction)
    var extractedBackgroundColor by remember { mutableStateOf<Color>(Color.Black) }
    
    // Helper function to convert Drawable to Bitmap for Palette color extraction
    fun drawableToBitmap(drawable: Drawable): Bitmap {
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
    
    // Helper function to darken a color by reducing its brightness
    fun darkenColor(color: Color, factor: Float = 0.6f): Color {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) * factor
        val g = ((argb shr 8) and 0xFF) * factor
        val b = (argb and 0xFF) * factor
        return Color((argb and 0xFF000000.toInt()) or ((r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()))
    }
    
    // Extract color from focused movie when immersive preview is disabled AND dynamic background is enabled
    val movieToExtractColorFrom = if (dynamicBackgroundColorEnabled && !immersiveListEnabled && focusedMovie != null) {
        focusedMovie
    } else {
        null
    }
    
    LaunchedEffect(movieToExtractColorFrom?.id, dynamicBackgroundColorEnabled) {
        // Only extract colors when dynamic background is enabled
        if (!dynamicBackgroundColorEnabled) {
            return@LaunchedEffect
        }
        
        val movie = movieToExtractColorFrom ?: return@LaunchedEffect
        
        val backdropUri = if (serverUrl.isNotBlank()) {
            val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            "${normalizedServerUrl}Items/${movie.id}/Images/Backdrop?maxWidth=3840"
        } else {
            val baseUrl = movie.posterUri.substringBefore("/Items/")
                .ifEmpty { movie.videoUri.substringBefore("/Videos/") }
            if (baseUrl.isNotBlank() && movie.id.isNotBlank()) {
                "$baseUrl/Items/${movie.id}/Images/Backdrop?maxWidth=3840"
            } else {
                movie.posterUri
            }
        }
        
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
                    
                    // Darken the extracted color before applying it
                    val extractedColor = Color(dominantColor)
                    extractedBackgroundColor = darkenColor(extractedColor, 0.6f)
                }
            } catch (e: Exception) {
                // Keep default black on error
                extractedBackgroundColor = Color.Black
            }
        }
    }

    // Always show top bar - make it persistent
    LaunchedEffect(Unit) {
        onScroll(true)
    }

    // Fixed background overlay that doesn't scroll - per Android TV Immersive List guidelines
    // Use dynamic extracted color when enabled, or black background when enabled, or theme default
    // When immersive preview is enabled, it's handled by ImmersiveBackground
    // When immersive preview is disabled, extract color from focused movie in MoviesRow
    // Animate color transitions with fade in/out effect
    val targetBackgroundColor = when {
        dynamicBackgroundColorEnabled -> extractedBackgroundColor
        blackBackgroundEnabled -> Color.Black
        else -> MaterialTheme.colorScheme.surface
    }
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(
            durationMillis = 800, // Slow fade in/out transition
            easing = FastOutSlowInEasing
        ),
        label = "backgroundColorAnimation"
    )
    
    Box(modifier = modifier.fillMaxSize().background(animatedBackgroundColor)) {
        // Fixed background preview (doesn't scroll with content)
        if (immersiveListEnabled && focusedMovie != null && showBackground) {
            ImmersiveBackground(
                movie = focusedMovie!!,
                visible = showBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.TopStart), // Fixed position at top
                onColorExtracted = { color ->
                    // Update home screen background with extracted color (darkened) only if dynamic background is enabled
                    if (dynamicBackgroundColorEnabled) {
                        extractedBackgroundColor = darkenColor(color, 0.6f)
                    }
                }
            )
        }

        // Scrollable content
        // Adjust top padding based on whether carousel or immersive preview is enabled
        // When both are disabled, use minimal padding (just enough for nav bar)
        // When either is enabled, use larger padding to accommodate the preview/carousel
        val topPadding = if (featuredMovies.isNotEmpty() || immersiveListEnabled) {
            320.dp // Push all rows down when carousel or immersive preview is active
        } else {
            48.dp // Minimal padding when neither is enabled (just nav bar height + spacing)
        }
        
        @OptIn(ExperimentalComposeUiApi::class)
        LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = topPadding,
            bottom = 75.6.dp // 30% smaller
        ),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup(),
    ) {
            // Immersive carousel with featured movies
            if (featuredMovies.isNotEmpty()) {
                item(contentType = "FeaturedCarousel", key = "featured_carousel") {
                    FeaturedMoviesCarousel(
                        movies = featuredMovies,
                        padding = childPadding,
                        goToVideoPlayer = goToVideoPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(384.dp) // Immersive carousel height (20% shorter)
                            .padding(bottom = 24.dp) // Space between carousel and row below
                            .focusProperties {
                                // Allow down navigation to continue watching or first library row
                                down = if (continueWatchingList.isNotEmpty() || libraryContentRows.isNotEmpty()) {
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Cancel
                                }
                            }
                    )
                }
            }
            
            // Continue Watching row with horizontal backdrop cards
            if (continueWatchingList.isNotEmpty()) {
            item(contentType = "ContinueWatching", key = "continue_watching") {
                if (immersiveListEnabled) {
                    // Use immersive list when enabled
                    ImmersiveListRow(
                        movieList = continueWatchingList,
                        title = "Continue Watching",
                        modifier = Modifier
                            .padding(
                                top = if (featuredMovies.isNotEmpty()) 16.8.dp else 16.8.dp,
                                bottom = if (libraryContentRows.isNotEmpty()) 32.dp else 0.dp // Increased spacing per immersive list guidelines
                            )
                            .focusProperties {
                                // Allow up navigation to carousel if it exists
                                up = if (featuredMovies.isNotEmpty()) {
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Default
                                }
                                // Allow down navigation to first library row
                                down = if (libraryContentRows.isNotEmpty()) {
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Cancel
                                }
                            },
                        onMovieSelected = onMovieClick,
                        onMovieFocusedForBackground = { movie, isVisible ->
                            focusedMovie = movie
                            showBackground = isVisible
                        }
                    )
                } else {
                    // Use regular row when disabled
                    com.google.jetstream.presentation.common.MoviesRow(
                        movieList = continueWatchingList,
                        title = "Continue Watching",
                        showItemTitle = true, // Show movie names below cards
                        itemDirection = com.google.jetstream.presentation.common.ItemDirection.Horizontal, // Use horizontal cards for backdrops
                        cardWidthScale = 1.44f, // 20% bigger cards (1.2 * 1.2 = 1.44)
                    modifier = Modifier
                            .padding(
                                top = if (featuredMovies.isNotEmpty()) 16.8.dp else 16.8.dp, // Spacing after carousel
                                bottom = if (libraryContentRows.isNotEmpty()) 1.dp else 0.dp // Tightest spacing possible
                            )
                            .focusProperties {
                                // Allow up navigation to carousel if it exists
                                up = if (featuredMovies.isNotEmpty()) {
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Default // Navigate up if no carousel
                                }
                                // Allow down navigation to first library row
                                down = if (libraryContentRows.isNotEmpty()) {
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Cancel
                                }
                            },
                    onMovieSelected = onMovieClick,
                    onMovieFocused = { movie ->
                        // Track focused movie for color extraction when immersive preview is disabled
                        if (!immersiveListEnabled) {
                            focusedMovie = movie
                        }
                    }
                )
                }
            }
            }
            
        // Add a row for each library's recently added content (filter out empty rows)
            val nonEmptyRows = libraryContentRows.filter { it.movies.isNotEmpty() }
        android.util.Log.d("HomeScreen", "Catalog: Rendering ${nonEmptyRows.size} library content rows (filtered from ${libraryContentRows.size} total)")
        
        // Find the first "Recently Released Movies" row to show preview on load
        val firstReleasedMoviesRowIndex = nonEmptyRows.indexOfFirst { 
            it.title.contains("Recently Released Movies", ignoreCase = true) 
        }
        
        itemsIndexed(
            nonEmptyRows,
            key = { index, libraryRow -> "library_${libraryRow.libraryId}_${libraryRow.title.replace(" ", "_")}" }
        ) { rowIndex, libraryRow ->
            android.util.Log.d("HomeScreen", "Catalog: Rendering row $rowIndex: ${libraryRow.title} with ${libraryRow.movies.size} movies")
            
            val isRecentlyReleasedRow = rowIndex == firstReleasedMoviesRowIndex && firstReleasedMoviesRowIndex >= 0
            
            if (immersiveListEnabled) {
                // Use immersive list when enabled
                ImmersiveListRow(
                    movieList = libraryRow.movies,
                    title = libraryRow.title,
                    modifier = Modifier
                        .padding(top = if (rowIndex == 0 && continueWatchingList.isEmpty()) 0.dp else 32.dp) // Increased spacing per immersive list guidelines
                        .focusProperties {
                            // Allow up navigation to continue watching (if first row) or previous row
                            up = if (rowIndex == 0 && continueWatchingList.isEmpty()) {
                                FocusRequester.Default
                            } else {
                                FocusRequester.Default
                            }
                            // Allow down navigation to next row
                            down = if (rowIndex < nonEmptyRows.size - 1) {
                                FocusRequester.Default
                            } else {
                                FocusRequester.Cancel
                            }
                        },
                    onMovieSelected = onMovieClick,
                    initialFocused = isRecentlyReleasedRow, // Show preview on load for first Recently Released Movies row
                    showTitleWhenFocused = isRecentlyReleasedRow, // Keep title visible for Recently Released Movies row
                    onMovieFocusedForBackground = { movie, isVisible ->
                        focusedMovie = movie
                        showBackground = isVisible
                    }
                )
            } else {
                // Use regular row when disabled
                // Check if this is a "Recently Added Episodes" row to show horizontal cards with backdrop photos
                val isEpisodesRow = libraryRow.title.contains("Recently Added Episodes", ignoreCase = true)
                
                MoviesRow(
                    modifier = Modifier
                        .padding(top = if (rowIndex == 0 && continueWatchingList.isEmpty()) 0.dp else 1.dp) // Tightest spacing possible
                        .focusProperties {
                            // Allow up navigation to continue watching (if first row) or previous row
                            up = if (rowIndex == 0 && continueWatchingList.isEmpty()) {
                                FocusRequester.Default // Navigate up if no continue watching
                            } else {
                                FocusRequester.Default // Navigate to previous row
                            }
                            // Allow down navigation to next row
                            down = if (rowIndex < nonEmptyRows.size - 1) {
                                FocusRequester.Default
                            } else {
                                FocusRequester.Cancel // Last row, no down navigation
                            }
                        },
                    movieList = libraryRow.movies,
                    title = libraryRow.title,
                    titleStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp // Keep unchanged as requested - recently added text
                    ),
                    showItemTitle = true, // Show movie/show names below cards
                    itemDirection = if (isEpisodesRow) {
                        com.google.jetstream.presentation.common.ItemDirection.Horizontal // Horizontal cards with backdrop photos for episodes
                    } else {
                        com.google.jetstream.presentation.common.ItemDirection.Vertical // Default vertical cards for other rows
                    },
                    cardWidthScale = if (isEpisodesRow) {
                        1.44f // Bigger cards for horizontal backdrop photos (same as continue watching)
                    } else {
                        0.756f // Another 20% bigger (0.63 * 1.2 = 0.756)
                    },
                    cardSpacing = 8.4.dp, // 30% smaller spacing between cards
                    onMovieSelected = onMovieClick,
                    onMovieFocused = { movie ->
                        // Track focused movie for color extraction when immersive preview is disabled
                        if (!immersiveListEnabled) {
                            focusedMovie = movie
                        }
                    }
                )
            }
        }
        }
    }
}
