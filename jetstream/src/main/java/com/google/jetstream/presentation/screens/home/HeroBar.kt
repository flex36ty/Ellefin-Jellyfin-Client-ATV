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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.AppViewModel
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun HeroBar(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    if (movie == null) {
        return
    }

    val appViewModel: AppViewModel = hiltViewModel()
    val serverUrl = appViewModel.preferences.serverUrl ?: ""

    // Get backdrop URL: extract server base from posterUri or videoUri and construct backdrop URL
    val backdropUri = remember(movie.id, movie.posterUri, movie.videoUri, serverUrl) {
        if (serverUrl.isNotBlank()) {
            val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            "${normalizedServerUrl}Items/${movie.id}/Images/Backdrop?maxWidth=3840"
        } else {
            // Fallback: try to extract server URL from existing URIs
            val baseUrl = movie.posterUri.substringBefore("/Items/")
                .ifEmpty { movie.videoUri.substringBefore("/Videos/") }
            if (baseUrl.isNotBlank() && movie.id.isNotBlank()) {
                "$baseUrl/Items/${movie.id}/Images/Backdrop?maxWidth=3840"
            } else {
                movie.posterUri // Fallback to posterUri if we can't construct backdrop URL
            }
        }
    }

    val childPadding = rememberChildPadding()
    val gradientColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false } // Non-focusable, just displays info
    ) {
        // Backdrop image with gradients (full size background)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(backdropUri)
                .crossfade(true)
                .build(),
            contentDescription = StringConstants.Composable.ContentDescription.moviePoster(movie.name),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Darker overlay to improve text readability (40% opacity black)
                    drawRect(
                        color = Color.Black.copy(alpha = 0.4f)
                    )
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, gradientColor.copy(alpha = 0.8f)),
                            startY = 600f
                        )
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            colors = listOf(gradientColor.copy(alpha = 0.8f), Color.Transparent),
                            endX = 1000f,
                            startX = 300f
                        )
                    )
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(gradientColor.copy(alpha = 0.8f), Color.Transparent),
                            start = Offset(x = 500f, y = 500f),
                            end = Offset(x = 1000f, y = 0f)
                        )
                    )
                }
        )

        // Text content overlay: name and synopsis together on top left
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(top = childPadding.top, start = childPadding.start),
                verticalArrangement = Arrangement.Top
            ) {
                // Movie/show name
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = MaterialTheme.typography.displayMedium.fontSize * 0.4f, // 60% smaller (40% of original)
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.75f),
                            offset = Offset(0.8f, 0.8f), // 60% smaller offset (2f * 0.4)
                            blurRadius = 3.2f // 60% smaller blur (8f * 0.4)
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Synopsis/Description below the name
                Column(
                    modifier = Modifier
                        .alpha(0.75f)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 6.sp, // 60% smaller (15.sp * 0.4)
                            fontWeight = FontWeight.Normal,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.75f),
                                offset = Offset(0.4f, 0.4f), // 60% smaller offset (1f * 0.4)
                                blurRadius = 2.4f // 60% smaller blur (6f * 0.4)
                            )
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

