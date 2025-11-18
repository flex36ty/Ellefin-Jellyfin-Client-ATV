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

package com.google.jetstream.presentation.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.presentation.common.Error
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MovieCard
import com.google.jetstream.presentation.common.PosterImage
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.presentation.theme.JetStreamBottomListPadding
import com.google.jetstream.presentation.utils.focusOnInitialVisibility

object LibraryScreen {
    const val LibraryIdBundleKey = "libraryId"
}

@Composable
fun LibraryScreen(
    libraryId: String,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    libraryScreenViewModel: LibraryScreenViewModel = hiltViewModel()
) {
    val uiState by libraryScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        LibraryScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        LibraryScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is LibraryScreenUiState.Done -> {
            LibraryContent(
                libraryName = s.libraryName,
                movies = s.movies,
                onBackPressed = onBackPressed,
                onMovieSelected = onMovieSelected
            )
        }
    }
}

@Composable
private fun LibraryContent(
    libraryName: String,
    movies: List<Movie>,
    onBackPressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val childPadding = rememberChildPadding()
    val isFirstItemVisible = remember { mutableStateOf(false) }

    BackHandler(onBack = onBackPressed)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = libraryName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(
                    start = 0.dp,
                    top = childPadding.top.times(2f),
                    bottom = 8.dp
                )
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = JetStreamBottomListPadding
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                movies,
                key = { _, movie ->
                    movie.id
                }
            ) { index, movie ->
                MovieCard(
                    onClick = { onMovieSelected(movie) },
                    title = {
                        Text(
                            text = movie.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    },
                    modifier = Modifier
                        .aspectRatio(1 / 1.5f)
                        .padding(8.dp)
                        .then(
                            if (index == 0)
                                Modifier.focusOnInitialVisibility(isFirstItemVisible)
                            else Modifier
                        ),
                ) {
                    PosterImage(movie = movie, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
