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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding

enum class ItemDirection(val aspectRatio: Float) {
    Vertical(10.5f / 16f),
    Horizontal(16f / 9f);
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MoviesRow(
    movieList: MovieList,
    modifier: Modifier = Modifier,
    itemDirection: ItemDirection = ItemDirection.Vertical,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 16.8.sp // 30% smaller titles
    ),
    showItemTitle: Boolean = true,
    showIndexOverImage: Boolean = false,
    cardWidthScale: Float = 1f, // Scale factor for card width (1f = normal, <1f = smaller)
    cardSpacing: Dp = 19.6.dp, // 30% smaller spacing between cards
    onMovieSelected: (movie: Movie) -> Unit = {},
    onMovieFocused: (movie: Movie) -> Unit = {}
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(
        modifier = modifier.focusGroup()
    ) {
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                modifier = Modifier
                    .alpha(1f)
                    .padding(start = startPadding, top = 2.8.dp, bottom = 5.6.dp) // 30% smaller padding
            )
        }
        AnimatedContent(
            targetState = movieList,
            label = "",
        ) { movieState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding)
                    .clipToBounds()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(
                        end = endPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                    modifier = Modifier
                        .focusRequester(lazyRow)
                        .focusRestorer {
                            firstItem
                        }
                ) {
                itemsIndexed(movieState, key = { _, movie -> movie.id }) { index, movie ->
                    val itemModifier = if (index == 0) {
                        Modifier.focusRequester(firstItem)
                    } else {
                        Modifier
                    }
                    // Use fixed width for cards instead of weight to prevent expansion from long titles
                    // Calculate width based on aspect ratio: for vertical cards (10.5/16), use ~200dp height = ~132dp width
                    // For better visibility, use larger cards: ~280dp height = ~184dp width
                    // 30% smaller: 184 * 0.7 = 128.8, 320 * 0.7 = 224
                    val baseCardWidth = if (itemDirection == ItemDirection.Vertical) {
                        128.8.dp // 30% smaller width for vertical cards (aspect ratio ~0.656)
                    } else {
                        224.dp // 30% smaller for horizontal cards (16:9 ratio)
                    }
                    val cardWidth = baseCardWidth * cardWidthScale
                    MoviesRowItem(
                        modifier = itemModifier.width(cardWidth),
                        index = index,
                        itemDirection = itemDirection,
                        onMovieSelected = {
                            lazyRow.saveFocusedChild()
                            onMovieSelected(it)
                        },
                        onMovieFocused = onMovieFocused,
                        movie = movie,
                        showItemTitle = showItemTitle,
                        showIndexOverImage = showIndexOverImage
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImmersiveListMoviesRow(
    movieList: MovieList,
    modifier: Modifier = Modifier,
    itemDirection: ItemDirection = ItemDirection.Vertical,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp // 30% smaller (15 * 0.7)
    ),
    showItemTitle: Boolean = true,
    showIndexOverImage: Boolean = false,
    onMovieSelected: (Movie) -> Unit = {},
    onMovieFocused: (Movie) -> Unit = {}
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(
        modifier = modifier.focusGroup()
    ) {
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                modifier = Modifier
                    .alpha(1f)
                    .padding(start = startPadding)
                    .padding(vertical = 11.2.dp)
            )
        }
        AnimatedContent(
            targetState = movieList,
            label = "",
        ) { movieState ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding)
                    .clipToBounds()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(end = endPadding),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), // 30% smaller
                    modifier = Modifier
                        .focusRequester(lazyRow)
                        .focusRestorer {
                            firstItem
                        }
                ) {
                itemsIndexed(
                    movieState,
                    key = { _, movie ->
                        movie.id
                    }
                ) { index, movie ->
                    val itemModifier = if (index == 0) {
                        Modifier.focusRequester(firstItem)
                    } else {
                        Modifier
                    }
                    // Use fixed width for cards instead of weight to prevent expansion from long titles
                    // 30% smaller: 184 * 0.7 = 128.8, 320 * 0.7 = 224
                    val cardWidth = if (itemDirection == ItemDirection.Vertical) {
                        128.8.dp // 30% smaller width for vertical cards
                    } else {
                        224.dp // 30% smaller for horizontal cards
                    }
                    MoviesRowItem(
                        modifier = itemModifier.width(cardWidth),
                        index = index,
                        itemDirection = itemDirection,
                        onMovieSelected = {
                            lazyRow.saveFocusedChild()
                            onMovieSelected(it)
                        },
                        onMovieFocused = onMovieFocused,
                        movie = movie,
                        showItemTitle = showItemTitle,
                        showIndexOverImage = showIndexOverImage
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MoviesRowItem(
    index: Int,
    movie: Movie,
    onMovieSelected: (Movie) -> Unit,
    showItemTitle: Boolean,
    showIndexOverImage: Boolean,
    modifier: Modifier = Modifier,
    itemDirection: ItemDirection = ItemDirection.Vertical,
    onMovieFocused: (Movie) -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasTriggeredCallback by remember { mutableStateOf(false) }

    MovieCard(
        onClick = { onMovieSelected(movie) },
        title = {
            MoviesRowItemText(
                showItemTitle = showItemTitle,
                isItemFocused = isFocused,
                movie = movie
            )
        },
        modifier = Modifier
            .onFocusChanged { focusState ->
                val hasFocus = focusState.isFocused || focusState.hasFocus
                val wasFocused = isFocused
                isFocused = hasFocus
                
                // Trigger callback whenever item gains focus, even if already focused before
                if (hasFocus) {
                    if (!wasFocused || !hasTriggeredCallback) {
                        android.util.Log.d("MoviesRow", "Movie focused: ${movie.name}, id: ${movie.id}, index: $index")
                        hasTriggeredCallback = true
                        onMovieFocused(movie)
                    }
                } else {
                    // Reset callback flag when losing focus so it can trigger again
                    if (wasFocused) {
                        hasTriggeredCallback = false
                    }
                }
            }
            .focusProperties {
                left = if (index == 0) {
                    FocusRequester.Cancel
                } else {
                    FocusRequester.Default
                }
            }
            .then(modifier)
    ) {
        MoviesRowItemImage(
            modifier = Modifier.aspectRatio(itemDirection.aspectRatio),
            showIndexOverImage = showIndexOverImage,
            movie = movie,
            index = index
        )
    }
}

@Composable
private fun MoviesRowItemImage(
    movie: Movie,
    showIndexOverImage: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.CenterStart) {
        PosterImage(
            movie = movie,
            modifier = modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    if (showIndexOverImage) {
                        drawRect(
                            color = Color.Black.copy(
                                alpha = 0.1f
                            )
                        )
                    }
                    // Draw progress bar at the bottom if there's progress
                    if (movie.progressPercentage > 0f) {
                        val progressBarHeight = size.height * 0.04f // 4% of image height
                        val progressBarY = size.height - progressBarHeight
                        // Background bar
                        drawRect(
                            color = Color.Black.copy(alpha = 0.3f),
                            topLeft = Offset(0f, progressBarY),
                            size = androidx.compose.ui.geometry.Size(size.width, progressBarHeight)
                        )
                        // Progress bar
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(0f, progressBarY),
                            size = androidx.compose.ui.geometry.Size(size.width * movie.progressPercentage, progressBarHeight)
                        )
                    }
                },
        )
        if (showIndexOverImage) {
            Text(
                modifier = Modifier.padding(11.2.dp),
                text = "#${index.inc()}",
                style = MaterialTheme.typography.displayLarge
                    .copy(
                        shadow = Shadow(
                            offset = Offset(0.5f, 0.5f),
                            blurRadius = 5f
                        ),
                        color = Color.White
                    ),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MoviesRowItemText(
    showItemTitle: Boolean,
    isItemFocused: Boolean,
    movie: Movie,
    modifier: Modifier = Modifier
) {
    if (showItemTitle) {
        val movieNameAlpha by animateFloatAsState(
            targetValue = if (isItemFocused) 1f else 0f,
            label = "",
        )
        Text(
            text = movie.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
                modifier = modifier
                .alpha(movieNameAlpha)
                .fillMaxWidth()
                .padding(horizontal = 2.8.dp, vertical = 2.8.dp), // 30% smaller padding
            maxLines = 2, // Allow 2 lines for better readability
            overflow = TextOverflow.Ellipsis,
        )
    }
}
