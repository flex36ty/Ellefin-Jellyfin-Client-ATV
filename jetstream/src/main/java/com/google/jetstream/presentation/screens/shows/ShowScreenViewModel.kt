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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.repositories.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ShowScreenViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    // Popular genres to show
    private val popularGenres = listOf(
        "Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Thriller", "Romance", "Adventure"
    )

    val uiState = combine(
        movieRepository.getBingeWatchDramas(),
        movieRepository.getTVShows(),
        movieRepository.getRecentlyAddedEpisodes()
    ) { (bingeWatchDramaList, tvShowList, recentlyAddedEpisodes) ->
        ShowScreenUiState.Ready(
            bingeWatchDramaList = bingeWatchDramaList,
            tvShowList = tvShowList,
            recentlyAddedEpisodes = recentlyAddedEpisodes,
            genreRows = popularGenres.map { genre ->
                GenreRow(genre = genre, movies = emptyList()) // Will be loaded separately
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShowScreenUiState.Loading
    )

    // Load genre data
    suspend fun loadGenreData(genre: String): MovieList {
        return movieRepository.getTVShowsByGenre(genre, limit = 20)
    }
}

sealed interface ShowScreenUiState {
    data object Loading : ShowScreenUiState
    data class Ready(
        val bingeWatchDramaList: MovieList,
        val tvShowList: MovieList,
        val recentlyAddedEpisodes: MovieList,
        val genreRows: List<GenreRow>
    ) : ShowScreenUiState
}

data class GenreRow(
    val genre: String,
    val movies: MovieList
)
