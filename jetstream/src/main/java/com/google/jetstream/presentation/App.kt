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

package com.google.jetstream.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.jetstream.data.util.JellyfinPreferences
import com.google.jetstream.presentation.screens.Screens
import com.google.jetstream.presentation.screens.categories.CategoryMovieListScreen
import com.google.jetstream.presentation.screens.dashboard.DashboardScreen
import com.google.jetstream.presentation.screens.login.LoginScreen
import com.google.jetstream.presentation.screens.movies.MovieDetailsScreen
import com.google.jetstream.presentation.screens.videoPlayer.VideoPlayerScreen

@Composable
fun App(
    onBackPressed: () -> Unit
) {
    val appViewModel: AppViewModel = hiltViewModel()
    val preferences = appViewModel.preferences
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(preferences.isLoggedIn) }
    
    LaunchedEffect(Unit) {
        isLoggedIn = preferences.isLoggedIn
    }
    
    // Observe logout events
    LaunchedEffect(Unit) {
        appViewModel.logoutEvent.collect {
            // Clear credentials first
            appViewModel.clearCredentials()
            
            // Set login state to false - this triggers NavHost recreation via key(isLoggedIn)
            // The NavHost will be completely recreated with Login as startDestination
            isLoggedIn = false
        }
    }

    // Use key to force NavHost recomposition when login state changes
    // This completely recreates the NavHost with the appropriate startDestination
    key(isLoggedIn) {
        // Create a fresh NavController for this NavHost instance
        val navController = rememberNavController()
        
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screens.Dashboard() else Screens.Login()
        ) {
        composable(route = Screens.Login()) {
                LoginScreen(
                    onLoginSuccess = {
                        // Setting isLoggedIn to true will recreate NavHost with Dashboard as startDestination
                        isLoggedIn = true
                    }
                )
            }
            composable(
                route = Screens.CategoryMovieList(),
                arguments = listOf(
                    navArgument(CategoryMovieListScreen.CategoryIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                CategoryMovieListScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onHomePressed = {
                        navController.navigate(Screens.Dashboard()) {
                            popUpTo(Screens.Dashboard()) {
                                inclusive = false
                            }
                        }
                    },
                    onMovieSelected = { movie ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movie.id)
                        )
                    }
                )
            }
            composable(
                route = Screens.MovieDetails(),
                arguments = listOf(
                    navArgument(MovieDetailsScreen.MovieIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                MovieDetailsScreen(
                    goToMoviePlayer = { movieId ->
                        navController.navigate(Screens.VideoPlayer.withArgs(movieId))
                    },
                    refreshScreenWithNewMovie = { movie ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movie.id)
                        ) {
                            popUpTo(Screens.MovieDetails()) {
                                inclusive = true
                            }
                        }
                    },
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
            composable(route = Screens.Dashboard()) {
                DashboardScreen(
                    openCategoryMovieList = { categoryId ->
                        navController.navigate(
                            Screens.CategoryMovieList.withArgs(categoryId)
                        )
                    },
                    openMovieDetailsScreen = { movieId ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movieId)
                        )
                    },
                    openVideoPlayer = { movie ->
                        navController.navigate(Screens.VideoPlayer.withArgs(movie.id))
                    },
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    }
                )
            }
            composable(
                route = Screens.VideoPlayer(),
                arguments = listOf(
                    navArgument(VideoPlayerScreen.MovieIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                VideoPlayerScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onNextEpisode = { nextEpisodeId ->
                        // Navigate to next episode
                        navController.navigate(Screens.VideoPlayer.withArgs(nextEpisodeId)) {
                            popUpTo(Screens.VideoPlayer()) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
