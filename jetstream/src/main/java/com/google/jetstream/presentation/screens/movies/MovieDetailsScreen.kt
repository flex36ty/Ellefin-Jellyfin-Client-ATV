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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.google.jetstream.R
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.common.Error
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MoviesRow
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding

object MovieDetailsScreen {
    const val MovieIdBundleKey = "movieId"
}

@Composable
fun MovieDetailsScreen(
    goToMoviePlayer: (String) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    movieDetailsScreenViewModel: MovieDetailsScreenViewModel = hiltViewModel()
) {
    val uiState by movieDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is MovieDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is MovieDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is MovieDetailsScreenUiState.Done -> {
            Details(
                movieDetails = s.movieDetails,
                goToMoviePlayer = goToMoviePlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewMovie = refreshScreenWithNewMovie,
                movieDetailsScreenViewModel = movieDetailsScreenViewModel,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun Details(
    movieDetails: MovieDetails,
    goToMoviePlayer: (String) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    movieDetailsScreenViewModel: MovieDetailsScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()

    BackHandler(onBack = onBackPressed)
    val isTVShow = movieDetails.type == "Series"
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    
    // Filter episodes by selected season for TV shows
    val filteredEpisodes = if (isTVShow && movieDetails.episodes.isNotEmpty()) {
        if (movieDetails.seasons.isNotEmpty()) {
            val selectedSeason = movieDetails.seasons.getOrNull(selectedSeasonIndex) 
                ?: movieDetails.seasons.firstOrNull()
            if (selectedSeason != null) {
                movieDetails.episodes.filter { it.seasonNumber == selectedSeason.indexNumber }
            } else {
                movieDetails.episodes
            }
        } else {
            movieDetails.episodes
        }
    } else {
        emptyList()
    }
    
    // For TV shows, use fixed backdrop that doesn't scroll
    val gradientColor = if (isTVShow) MaterialTheme.colorScheme.surface else Color.Transparent
    Box(modifier = modifier.fillMaxSize()) {
        // Fixed backdrop background for TV shows - doesn't scroll
        if (isTVShow) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movieDetails.posterUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        // Darker overlay to improve text readability
                        drawRect(color = Color.Black.copy(alpha = 0.4f))
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
                    }
            )
        }
        
        // Scrollable content on top
        LazyColumn(
            contentPadding = PaddingValues(bottom = 135.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
        item {
            if (isTVShow) {
                TVShowDetails(
                    movieDetails = movieDetails,
                    goToMoviePlayer = { episode ->
                        // Navigate to player with episode ID
                        goToMoviePlayer(episode.id)
                    },
                    onPlay = { episode ->
                        // Navigate to player with episode ID
                        goToMoviePlayer(episode.id)
                    },
                    onSubtitleSelect = {
                        // Load subtitles when button is clicked
                        movieDetailsScreenViewModel.loadSubtitleTracks(movieDetails.id)
                    },
                    onSelectedSeasonChange = { newIndex ->
                        selectedSeasonIndex = newIndex
                    }
                )
            } else {
            MovieDetails(
                movieDetails = movieDetails,
                    goToMoviePlayer = { goToMoviePlayer(movieDetails.id) },
                    onPlay = { goToMoviePlayer(movieDetails.id) },
                    onSubtitleSelect = {
                        // Load subtitles when button is clicked
                        movieDetailsScreenViewModel.loadSubtitleTracks(movieDetails.id)
                    }
                )
            }
        }
        
        // Episodes list for TV shows - add as individual items to avoid nested LazyColumns
        items(
            count = filteredEpisodes.size,
            key = { index -> filteredEpisodes[index].id },
            contentType = { "Episode" }
        ) { index ->
            val episode = filteredEpisodes[index]
            com.google.jetstream.presentation.screens.movies.EpisodeItem(
                episode = episode,
                onClick = {
                    // Navigate to player with episode ID
                    goToMoviePlayer(episode.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = childPadding.start)
                    .padding(vertical = 8.dp)
            )
        }

        // Show cast and crew only for movies, not TV shows
        if (!isTVShow) {
            item {
                CastAndCrewList(
                    castAndCrew = movieDetails.castAndCrew
                )
            }
        }

        // Similar shows section for TV shows at the bottom with horizontal list
        if (isTVShow && movieDetails.similarMovies.isNotEmpty()) {
            item {
                MoviesRow(
                    title = StringConstants
                        .Composable
                        .movieDetailsScreenSimilarTo(movieDetails.name),
                    titleStyle = MaterialTheme.typography.titleMedium,
                    movieList = movieDetails.similarMovies,
                    itemDirection = com.google.jetstream.presentation.common.ItemDirection.Horizontal,
                    onMovieSelected = refreshScreenWithNewMovie
                )
            }
        }

        // Similar movies section for movies (keep existing behavior)
        if (!isTVShow) {
            item {
                MoviesRow(
                    title = StringConstants
                        .Composable
                        .movieDetailsScreenSimilarTo(movieDetails.name),
                    titleStyle = MaterialTheme.typography.titleMedium,
                    movieList = movieDetails.similarMovies,
                    onMovieSelected = refreshScreenWithNewMovie
                )
            }
        }

        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)
