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

package com.google.jetstream.presentation.screens.shows

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MoviesRow
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import kotlinx.coroutines.launch

@Composable
fun ShowsScreen(
    onTVShowClick: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    showScreenViewModel: ShowScreenViewModel = hiltViewModel(),
) {
    val uiState = showScreenViewModel.uiState.collectAsStateWithLifecycle()
    when (val currentState = uiState.value) {
        is ShowScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is ShowScreenUiState.Ready -> {
            Catalog(
                bingeWatchDramaList = currentState.bingeWatchDramaList,
                recentlyAddedEpisodes = currentState.recentlyAddedEpisodes,
                genreRows = currentState.genreRows,
                onTVShowClick = onTVShowClick,
                onScroll = onScroll,
                viewModel = showScreenViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Catalog(
    bingeWatchDramaList: MovieList,
    recentlyAddedEpisodes: MovieList,
    genreRows: List<GenreRow>,
    onTVShowClick: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    viewModel: ShowScreenViewModel,
    modifier: Modifier = Modifier
) {
    val childPadding = rememberChildPadding()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var loadedGenres by remember { mutableStateOf<Map<String, MovieList>>(emptyMap()) }
    
    // Always show top bar - make it persistent
    LaunchedEffect(Unit) {
        onScroll(true)
    }

    // Load genre data when screen is ready
    LaunchedEffect(genreRows) {
        genreRows.forEach { genreRow ->
            if (genreRow.movies.isEmpty() && !loadedGenres.containsKey(genreRow.genre)) {
                coroutineScope.launch {
                    val shows = viewModel.loadGenreData(genreRow.genre)
                    if (shows.isNotEmpty()) {
                        loadedGenres = loadedGenres + (genreRow.genre to shows)
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(top = childPadding.top, bottom = 72.8.dp) // 30% smaller
    ) {
        // Recently Added Episodes row with backdrop images (horizontal cards) - Top row
        if (recentlyAddedEpisodes.isNotEmpty()) {
            item(key = "recently_added_episodes") {
                MoviesRow(
                    modifier = Modifier.padding(top = childPadding.top),
                    title = "Recently Added Episodes",
                    movieList = recentlyAddedEpisodes,
                    itemDirection = com.google.jetstream.presentation.common.ItemDirection.Horizontal,
                    onMovieSelected = onTVShowClick
            )
        }
        }
        
        item {
            MoviesRow(
                modifier = Modifier.padding(top = childPadding.top),
                title = StringConstants.Composable.BingeWatchDramasTitle,
                movieList = bingeWatchDramaList,
                onMovieSelected = onTVShowClick
            )
        }
        
        // Add genre rows
        genreRows.forEach { genreRow ->
            val genreShows = loadedGenres[genreRow.genre] ?: emptyList()
            if (genreShows.isNotEmpty()) {
                item(key = "genre_${genreRow.genre}") {
                    MoviesRow(
                        modifier = Modifier.padding(top = childPadding.top),
                        title = genreRow.genre,
                        movieList = genreShows,
                        onMovieSelected = onTVShowClick
                    )
                }
            }
        }
    }
}

