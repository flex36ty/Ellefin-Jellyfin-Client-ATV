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

package com.google.jetstream.presentation.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.entities.MovieCategory
import com.google.jetstream.data.repositories.MovieRepository
import com.google.jetstream.data.util.JellyfinPreferences
import com.google.jetstream.presentation.screens.Screens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "DashboardScreenViewModel"

@HiltViewModel
class DashboardScreenViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    val preferences: JellyfinPreferences
) : ViewModel() {

    val topBarTabs: StateFlow<List<TopBarTab>> = movieRepository.getMovieCategories()
        .catch { e ->
            Log.e(TAG, "Error getting libraries - ${e.javaClass.simpleName}: ${e.message}", e)
            emit(emptyList())
        }
        .map { categories ->
            // Filter out Live TV, Favourites, and Playlists
            val filteredLibraries = categories.filter { library ->
                val name = library.name.lowercase()
                name != "live tv" && name != "livetv" &&
                name != "favourites" && name != "favorites" &&
                name != "playlists" && name != "playlist"
            }
            
            // Build tabs: Home, then libraries (Search removed temporarily)
            val tabs = mutableListOf<TopBarTab>()
            tabs.add(TopBarTab.ScreenTab(Screens.Home))
            
            // Add libraries as tabs
            filteredLibraries.forEach { library ->
                tabs.add(TopBarTab.LibraryTab(library.id, library.name))
            }
            
            Log.d(TAG, "Created ${tabs.size} top bar tabs (${filteredLibraries.size} libraries)")
            tabs
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = listOf(
                TopBarTab.ScreenTab(Screens.Home)
            )
        )
}

