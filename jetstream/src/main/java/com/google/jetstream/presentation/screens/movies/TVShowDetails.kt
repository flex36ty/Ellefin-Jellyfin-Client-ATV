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

package com.google.jetstream.presentation.screens.movies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.jetstream.R
import com.google.jetstream.data.entities.Episode
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.entities.Season
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.presentation.theme.JetStreamButtonShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TVShowDetails(
    movieDetails: MovieDetails,
    goToMoviePlayer: (Episode) -> Unit,
    onPlay: (Episode) -> Unit,
    onSubtitleSelect: () -> Unit = {},
    onSelectedSeasonChange: (Int) -> Unit = {}
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val seasonsFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    
    // Filter episodes by selected season
    val filteredEpisodes = if (movieDetails.seasons.isNotEmpty()) {
        val selectedSeason = movieDetails.seasons.getOrNull(selectedSeasonIndex)
        if (selectedSeason != null) {
            movieDetails.episodes.filter { it.seasonNumber == selectedSeason.indexNumber }
        } else {
            movieDetails.episodes
        }
    } else {
        movieDetails.episodes
    }

    // Focus the seasons row by default when the screen loads
    LaunchedEffect(Unit) {
        seasonsFocusRequester.requestFocus()
    }

    // No backdrop image here - it's now fixed at the screen level for TV shows
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(432.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Spacer(modifier = Modifier.height(60.dp))
            Column(
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .focusGroup()
            ) {
                MovieLargeTitle(movieTitle = movieDetails.name)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    MovieDescription(description = movieDetails.description)
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 20.dp),
                        texts = listOfNotNull(
                            movieDetails.pgRating.takeIf { it.isNotBlank() },
                            movieDetails.releaseDate.takeIf { it.isNotBlank() },
                            movieDetails.categories.joinToString(", ").takeIf { movieDetails.categories.isNotEmpty() },
                            movieDetails.resolution.takeIf { it.isNotBlank() }
                        )
                    )
                }
                // Seasons row instead of buttons for TV shows
                LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier
                            .focusRequester(seasonsFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            }
                    ) {
                        items(movieDetails.seasons, key = { it.id }) { season ->
                            SeasonButton(
                                season = season,
                                isSelected = movieDetails.seasons.indexOf(season) == selectedSeasonIndex,
                                onClick = {
                                    val newIndex = movieDetails.seasons.indexOf(season)
                                    selectedSeasonIndex = newIndex
                                    onSelectedSeasonChange(newIndex)
                                },
                                modifier = Modifier.onFocusChanged {
                                    if (it.isFocused) {
                                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                    }
                                }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun SeasonButton(
    season: Season,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = JetStreamButtonShape),
        colors = if (isSelected) {
            ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.colors()
        },
        scale = ButtonDefaults.scale(),
        glow = ButtonDefaults.glow()
    ) {
        Text(
            text = season.name.ifBlank { "Season ${season.indexNumber ?: ""}" },
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape = androidx.tv.material3.ShapeDefaults.Medium),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thumbnail on the left
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(112.dp)
            ) {
                if (!episode.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(episode.imageUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = "Episode ${episode.episodeNumber} thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // Synopsis on the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                // Episode number and title
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (episode.episodeNumber != null) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = episode.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (!episode.runtime.isNullOrBlank()) {
                        Text(
                            text = "• ${episode.runtime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Synopsis
                if (!episode.overview.isNullOrBlank()) {
                    Text(
                        text = episode.overview ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


