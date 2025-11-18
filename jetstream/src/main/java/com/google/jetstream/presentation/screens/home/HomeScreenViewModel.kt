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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.entities.LibraryContentRow
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.repositories.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

private const val TAG = "HomeScreenViewModel"

@HiltViewModel
class HomeScreeViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val libraryContentFlow = movieRepository.getMovieCategories()
        .catch { e ->
            Log.e(TAG, "libraryContentFlow: Error getting categories - ${e.javaClass.simpleName}: ${e.message}", e)
            emit(emptyList()) // Emit empty list but don't crash the flow
        }
        .distinctUntilChanged() // Prevent re-triggering when the same categories list is emitted
        .flatMapLatest { categories ->
            flow {
                Log.d(TAG, "libraryContentFlow: Received ${categories.size} categories")
                if (categories.isEmpty()) {
                    Log.w(TAG, "libraryContentFlow: No categories, skipping content load")
                    // Emit cached rows if available, otherwise empty
                    if (lastValidLibraryRows.isNotEmpty()) {
                        Log.d(TAG, "libraryContentFlow: No categories but using cached library rows")
                        emit(lastValidLibraryRows)
                    } else {
                        emit(emptyList())
                    }
                    return@flow
                }
                
                // Filter out Favourites, Playlists, and Live TV from home screen
                val filteredCategories = categories.filter { library ->
                    val name = library.name.lowercase()
                    name != "favourites" && name != "favorites" && 
                    name != "playlists" && name != "playlist" &&
                    name != "live tv" && name != "livetv"
                }
                Log.d(TAG, "libraryContentFlow: Filtered to ${filteredCategories.size} libraries (excluded Favourites, Playlists, Live TV)")
                
                try {
                    // Load library content in parallel using coroutineScope inside the flow
                    val allRows = kotlinx.coroutines.coroutineScope {
                        Log.d(TAG, "libraryContentFlow: Starting parallel load for ${filteredCategories.size} libraries")
                        val libraryRowsDeferred = filteredCategories.map { library ->
                            async {
                                val rows = mutableListOf<LibraryContentRow>()
                                
                                try {
                                    Log.d(TAG, "libraryContentFlow: Processing library - Id=${library.id}, Name=${library.name}")
                                    
                                    // Get recently added movies for this library
                                    val recentMovies = movieRepository.getRecentlyAddedMovies(library.id)
                                    Log.d(TAG, "libraryContentFlow: Library ${library.name} - Got ${recentMovies.size} recent movies")
                                    if (recentMovies.isNotEmpty()) {
                                        rows.add(
                                            LibraryContentRow(
                                                libraryId = library.id,
                                                libraryName = library.name,
                                                title = "${library.name} - Recently Added Movies",
                                                movies = recentMovies
                                            )
                                        )
                                        Log.d(TAG, "libraryContentFlow: Added movies row for library ${library.name}")
                                    }
                                    
                                    // Get recently released movies for this library
                                    val releasedMovies = movieRepository.getRecentlyReleasedMovies(library.id)
                                    Log.d(TAG, "libraryContentFlow: Library ${library.name} - Got ${releasedMovies.size} recently released movies")
                                    if (releasedMovies.isNotEmpty()) {
                                        rows.add(
                                            LibraryContentRow(
                                                libraryId = library.id,
                                                libraryName = library.name,
                                                title = "${library.name} - Recently Released Movies",
                                                movies = releasedMovies
                                            )
                                        )
                                        Log.d(TAG, "libraryContentFlow: Added recently released movies row for library ${library.name}")
                                    }
                                    
                                    // Get recently added shows for this library
                                    val recentShows = movieRepository.getRecentlyAddedShows(library.id)
                                    Log.d(TAG, "libraryContentFlow: Library ${library.name} - Got ${recentShows.size} recent shows")
                                    if (recentShows.isNotEmpty()) {
                                        rows.add(
                                            LibraryContentRow(
                                                libraryId = library.id,
                                                libraryName = library.name,
                                                title = "${library.name} - Recently Added Shows",
                                                movies = recentShows
                                            )
                                        )
                                        Log.d(TAG, "libraryContentFlow: Added shows row for library ${library.name}")
                                    }
                                    
                                    // Get recently added episodes for this library
                                    val recentEpisodes = movieRepository.getRecentlyAddedEpisodes(library.id)
                                    Log.d(TAG, "libraryContentFlow: Library ${library.name} - Got ${recentEpisodes.size} recent episodes")
                                    if (recentEpisodes.isNotEmpty()) {
                                        rows.add(
                                            LibraryContentRow(
                                                libraryId = library.id,
                                                libraryName = library.name,
                                                title = "${library.name} - Recently Added Episodes",
                                                movies = recentEpisodes
                                            )
                                        )
                                        Log.d(TAG, "libraryContentFlow: Added episodes row for library ${library.name}")
                                    }
                                } catch (e: Exception) {
                                    // Handle errors for individual libraries - just skip them
                                    Log.e(TAG, "libraryContentFlow: Error processing library ${library.name} - ${e.javaClass.simpleName}: ${e.message}", e)
                                    e.printStackTrace()
                                }
                                
                                Log.d(TAG, "libraryContentFlow: Library ${library.name} produced ${rows.size} rows")
                                rows
                            }
                        }
                        
                        // Wait for all library content to load and flatten the results
                        val results = libraryRowsDeferred.awaitAll()
                        Log.d(TAG, "libraryContentFlow: All libraries processed, flattening ${results.size} results")
                        val flattened = results.flatten()
                        Log.d(TAG, "libraryContentFlow: Flattened to ${flattened.size} total rows")
                        flattened
                    }
                    
                    Log.d(TAG, "libraryContentFlow: Emitting ${allRows.size} total rows")
                    emit(allRows)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Re-throw cancellation - should propagate
                    Log.w(TAG, "libraryContentFlow: Cancelled during content load")
                    throw e
                } catch (e: Exception) {
                    // If there's an error, emit cached data or empty list but don't crash
                    Log.e(TAG, "libraryContentFlow: Fatal error - ${e.javaClass.simpleName}: ${e.message}", e)
                    e.printStackTrace()
                    // Try to use cached data on error
                    if (lastValidLibraryRows.isNotEmpty()) {
                        Log.d(TAG, "libraryContentFlow: Error but using cached library rows")
                        emit(lastValidLibraryRows)
                    } else {
                        emit(emptyList())
                    }
                }
            }
        }
        .catch { e ->
            if (e is kotlinx.coroutines.CancellationException) {
                // Re-throw cancellation
                throw e
            }
            Log.e(TAG, "libraryContentFlow: Error in flatMapLatest - ${e.javaClass.simpleName}: ${e.message}", e)
            // Emit cached data or empty list on error but keep the flow alive
            if (lastValidLibraryRows.isNotEmpty()) {
                Log.d(TAG, "libraryContentFlow: Error in flatMapLatest, using cached library rows")
                emit(lastValidLibraryRows)
            } else {
                emit(emptyList())
            }
        }

    // Track last valid data to preserve on errors
    private var lastValidContinueWatching: MovieList = emptyList()
    private var lastValidLibraryRows: List<LibraryContentRow> = emptyList()
    private var lastValidFeaturedMovies: MovieList = emptyList()
    
    // Load featured movies for the immersive carousel
    private val featuredMoviesFlow = movieRepository.getFeaturedMovies()
        .catch { e ->
            Log.e(TAG, "featuredMoviesFlow: Error getting featured movies - ${e.javaClass.simpleName}: ${e.message}", e)
            // Emit cached data on error if available
            if (lastValidFeaturedMovies.isNotEmpty()) {
                Log.d(TAG, "featuredMoviesFlow: Error but emitting cached featured movies")
                emit(lastValidFeaturedMovies)
            } else {
                emit(emptyList())
            }
        }
        .onEach { movies ->
            // Update cache when we get valid data
            if (movies.isNotEmpty()) {
                lastValidFeaturedMovies = movies
                Log.d(TAG, "featuredMoviesFlow: Cached ${movies.size} featured movies")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Load immediately and cache the result
            initialValue = lastValidFeaturedMovies // Use cached value as initial if available
        )
    
    // Use Continue Watching instead of Featured Movies - faster to load (resume items)
    private val continueWatchingFlow = movieRepository.getContinueWatching()
    
    private val libraryRowsFlow = libraryContentFlow
        .onEach { rows ->
            // Update cache when we get valid data
            if (rows.isNotEmpty()) {
                lastValidLibraryRows = rows
                Log.d(TAG, "uiState: Cached ${rows.size} library rows")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Load immediately, don't wait for subscribers
            initialValue = emptyList()
        )

    val uiState: StateFlow<HomeScreenUiState> = combine(
        featuredMoviesFlow,
        continueWatchingFlow,
        libraryRowsFlow
    ) { featuredMovies, continueWatchingList, libraryRows ->
        Log.d(TAG, "uiState: Combining states - featuredMovies=${featuredMovies.size}, continueWatching=${continueWatchingList.size}, libraryRows=${libraryRows.size}")
        
        // Use cached data if we get empty results but have cached data (likely due to error)
        val finalFeaturedMovies = if (featuredMovies.isEmpty() && lastValidFeaturedMovies.isNotEmpty()) {
            Log.d(TAG, "uiState: Using cached featured movies (${lastValidFeaturedMovies.size})")
            lastValidFeaturedMovies
        } else {
            featuredMovies
        }
        
        val finalContinueWatching = if (continueWatchingList.isEmpty() && lastValidContinueWatching.isNotEmpty()) {
            Log.d(TAG, "uiState: Using cached continue watching (${lastValidContinueWatching.size})")
            lastValidContinueWatching
        } else {
            continueWatchingList
        }
        
        val finalLibraryRows = if (libraryRows.isEmpty() && lastValidLibraryRows.isNotEmpty()) {
            Log.d(TAG, "uiState: Using cached library rows (${lastValidLibraryRows.size})")
            lastValidLibraryRows
        } else {
            libraryRows
        }
        
        // Show Ready as soon as we have library rows or featured movies (continue watching is optional)
        // Continue watching can be empty if user hasn't watched anything yet
        if (finalLibraryRows.isNotEmpty() || finalContinueWatching.isNotEmpty() || finalFeaturedMovies.isNotEmpty()) {
        HomeScreenUiState.Ready(
                featuredMovies = finalFeaturedMovies,
                continueWatchingList = finalContinueWatching,
                libraryContentRows = finalLibraryRows
            )
        } else {
            Log.w(TAG, "uiState: No data available and no cache, showing error state")
            HomeScreenUiState.Error
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // Start loading immediately
        initialValue = if (lastValidLibraryRows.isNotEmpty() || lastValidContinueWatching.isNotEmpty() || lastValidFeaturedMovies.isNotEmpty()) {
            // Show cached data immediately if available
            HomeScreenUiState.Ready(
                featuredMovies = lastValidFeaturedMovies,
                continueWatchingList = lastValidContinueWatching,
                libraryContentRows = lastValidLibraryRows
            )
        } else {
            HomeScreenUiState.Loading
        }
    )
}

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Error : HomeScreenUiState
    data class Ready(
        val featuredMovies: MovieList,
        val continueWatchingList: MovieList,
        val libraryContentRows: List<LibraryContentRow>
    ) : HomeScreenUiState
}
