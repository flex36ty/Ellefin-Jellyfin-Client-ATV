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
fun MoviesScreen(
    onMovieClick: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    moviesScreenViewModel: MoviesScreenViewModel = hiltViewModel(),
) {
    val uiState by moviesScreenViewModel.uiState.collectAsStateWithLifecycle()
    when (val s = uiState) {
        is MoviesScreenUiState.Loading -> Loading()
        is MoviesScreenUiState.Ready -> {
            Catalog(
                popularFilmsThisWeek = s.popularFilmsThisWeek,
                genreRows = s.genreRows,
                onMovieClick = onMovieClick,
                onScroll = onScroll,
                viewModel = moviesScreenViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Catalog(
    popularFilmsThisWeek: MovieList,
    genreRows: List<GenreRow>,
    onMovieClick: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    viewModel: MoviesScreenViewModel,
    modifier: Modifier = Modifier,
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
                    val movies = viewModel.loadGenreData(genreRow.genre)
                    if (movies.isNotEmpty()) {
                        loadedGenres = loadedGenres + (genreRow.genre to movies)
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
        item {
            MoviesRow(
                modifier = Modifier.padding(top = childPadding.top),
                title = StringConstants.Composable.PopularFilmsThisWeekTitle,
                movieList = popularFilmsThisWeek,
                onMovieSelected = onMovieClick
            )
        }
        // Add genre rows
        genreRows.forEach { genreRow ->
            val genreMovies = loadedGenres[genreRow.genre] ?: emptyList()
            if (genreMovies.isNotEmpty()) {
                item(key = "genre_${genreRow.genre}") {
                    MoviesRow(
                        modifier = Modifier.padding(top = childPadding.top),
                        title = genreRow.genre,
                        movieList = genreMovies,
                        onMovieSelected = onMovieClick
                    )
                }
            }
        }
    }
}

