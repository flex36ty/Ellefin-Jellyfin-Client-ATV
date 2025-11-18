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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.jetstream.R
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.theme.JetStreamBorderWidth
import com.google.jetstream.presentation.theme.JetStreamButtonShape
import com.google.jetstream.presentation.utils.Padding
import com.google.jetstream.presentation.utils.handleDPadKeyEvents
import androidx.compose.ui.platform.LocalContext
import coil.compose.LocalImageLoader

@OptIn(ExperimentalTvMaterial3Api::class)
val CarouselSaver = Saver<CarouselState, Int>(
    save = { it.activeItemIndex },
    restore = { CarouselState(it) }
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedMoviesCarousel(
    movies: List<Movie>,
    padding: Padding,
    goToVideoPlayer: (movie: Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    // Show placeholder if movies list is empty instead of hiding the hero
    if (movies.isEmpty()) {
        Box(
            modifier = modifier
                .padding(start = padding.start, end = padding.start, top = padding.top)
                .fillMaxSize()
        )
        return
    }
    
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var isCarouselFocused by remember { mutableStateOf(false) }
    val alpha = if (isCarouselFocused) {
        1f
    } else {
        0f
    }

    // Reset index when movies list changes
    LaunchedEffect(movies.size) {
        if (currentIndex >= movies.size && movies.isNotEmpty()) {
            currentIndex = 0
        }
    }

    // Create carousel state with current index - recreates when currentIndex changes
    val carouselState = remember(currentIndex) { CarouselState(currentIndex) }
    
    val context = LocalContext.current
    @Suppress("DEPRECATION") // LocalImageLoader is deprecated but still used in MainActivity
    val imageLoader = LocalImageLoader.current
    
    // Preload next and previous images for faster carousel transitions
    LaunchedEffect(currentIndex, movies.size) {
        if (movies.isNotEmpty()) {
            // Preload next image
            val nextIndex = if (currentIndex < movies.size - 1) currentIndex + 1 else 0
            val nextMovie = movies[nextIndex]
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(nextMovie.posterUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
            
            // Preload previous image
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else movies.size - 1
            val prevMovie = movies[prevIndex]
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(prevMovie.posterUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }

    // Auto-advance carousel every 7 seconds when not focused
    LaunchedEffect(movies.size, currentIndex, isCarouselFocused) {
        if (movies.isNotEmpty() && !isCarouselFocused) {
            delay(7000) // 7 seconds
            currentIndex = if (currentIndex < movies.size - 1) {
                currentIndex + 1
            } else {
                0 // Loop back to start
            }
        }
    }

    Carousel(
        modifier = modifier
            .padding(start = padding.start, end = padding.start, top = padding.top)
            .border(
                width = JetStreamBorderWidth,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                shape = ShapeDefaults.Medium,
            )
            .clip(ShapeDefaults.Medium)
            .onFocusChanged {
                // Because the carousel itself never gets the focus
                isCarouselFocused = it.hasFocus
            }
            .semantics {
                contentDescription =
                    StringConstants.Composable.ContentDescription.MoviesCarousel
            }
            .handleDPadKeyEvents(onEnter = {
                // Guard against empty list and out of bounds
                if (movies.isNotEmpty() && currentIndex in movies.indices) {
                    goToVideoPlayer(movies[currentIndex])
                }
            }),
        itemCount = movies.size,
        carouselState = carouselState,
        carouselIndicator = {
            CarouselIndicator(
                itemCount = movies.size,
                activeItemIndex = currentIndex
            )
        },
        contentTransformStartToEnd = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        contentTransformEndToStart = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        content = { index ->
            // Guard against out of bounds access
            if (index in movies.indices) {
            val movie = movies[index]
            // background
            CarouselItemBackground(movie = movie, modifier = Modifier.fillMaxSize())
            // foreground
            CarouselItemForeground(
                movie = movie,
                isCarouselFocused = isCarouselFocused,
                modifier = Modifier.fillMaxSize()
            )
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BoxScope.CarouselIndicator(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .graphicsLayer {
                clip = true
                shape = ShapeDefaults.ExtraSmall
            }
            .align(Alignment.BottomEnd)
    ) {
        CarouselDefaults.IndicatorRow(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            itemCount = itemCount,
            activeItemIndex = activeItemIndex,
        )
    }
}

@Composable
private fun CarouselItemForeground(
    movie: Movie,
    modifier: Modifier = Modifier,
    isCarouselFocused: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = movie.name,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 2f, y = 4f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 1
            )
            Text(
                text = movie.description,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.85f
                    ),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 2f, y = 4f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 3,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.7f) // Limit width to 70% for better readability
            )
            AnimatedVisibility(
                visible = isCarouselFocused,
                content = {
                    WatchNowButton()
                }
            )
        }
    }
}

@Composable
private fun CarouselItemBackground(movie: Movie, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(movie.posterUri)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .moviePoster(movie.name),
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // Darken the image more for better text readability
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f) // Increased from 0.5f to 0.7f for better readability
                        )
                    )
                )
            },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun WatchNowButton() {
    Button(
        onClick = {},
        modifier = Modifier.padding(top = 8.dp),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = JetStreamButtonShape),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
        scale = ButtonDefaults.scale(),
        glow = ButtonDefaults.glow()
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.watch_now),
            style = MaterialTheme.typography.titleSmall
        )
    }
}
