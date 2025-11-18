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

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieCategory
import com.google.jetstream.data.repositories.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LibraryScreenViewModel"

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val libraryId: String = savedStateHandle.get<String>(LibraryScreen.LibraryIdBundleKey).orEmpty()

    private val _uiState = MutableStateFlow<LibraryScreenUiState>(LibraryScreenUiState.Loading)
    val uiState: StateFlow<LibraryScreenUiState> = _uiState.asStateFlow()

    init {
        loadLibraryContent()
    }

    private fun loadLibraryContent() {
        viewModelScope.launch {
            try {
                if (libraryId.isBlank()) {
                    Log.e(TAG, "Library ID is empty")
                    _uiState.value = LibraryScreenUiState.Error
                    return@launch
                }
                
                Log.d(TAG, "Loading library content for libraryId=$libraryId")
                
                // Get library name from categories
                val categories = movieRepository.getMovieCategories().first()
                val library = categories.find { it.id == libraryId }
                val libraryName = library?.name ?: "Library"
                
                // Get all movies and shows from this library
                val movies = movieRepository.getMoviesByLibrary(libraryId, limit = 100)
                val shows = movieRepository.getTVShowsByLibrary(libraryId, limit = 100)
                
                val allItems = movies + shows
                
                Log.d(TAG, "Loaded ${allItems.size} items for library $libraryName (${movies.size} movies, ${shows.size} shows)")
                
                _uiState.value = LibraryScreenUiState.Done(
                    libraryName = libraryName,
                    movies = allItems
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading library content", e)
                _uiState.value = LibraryScreenUiState.Error
            }
        }
    }
}

sealed interface LibraryScreenUiState {
    data object Loading : LibraryScreenUiState
    data object Error : LibraryScreenUiState
    data class Done(
        val libraryName: String,
        val movies: List<Movie>
    ) : LibraryScreenUiState
}

