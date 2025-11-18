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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.util.JellyfinPreferences
import com.google.jetstream.presentation.screens.Screens
import com.google.jetstream.presentation.screens.categories.CategoriesScreen
import com.google.jetstream.presentation.screens.favourites.FavouritesScreen
import com.google.jetstream.presentation.screens.home.HomeScreen
import com.google.jetstream.presentation.screens.library.LibraryScreen
import com.google.jetstream.presentation.screens.movies.MoviesScreen
import com.google.jetstream.presentation.screens.profile.ProfileScreen
import com.google.jetstream.presentation.screens.search.SearchScreen
import com.google.jetstream.presentation.screens.shows.ShowsScreen
import com.google.jetstream.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 11.2.dp, horizontal = 40.6.dp)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 5.6.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 5.6.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

@Composable
fun DashboardScreen(
    openCategoryMovieList: (categoryId: String) -> Unit,
    openMovieDetailsScreen: (movieId: String) -> Unit,
    openVideoPlayer: (Movie) -> Unit,
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    onBackPressed: () -> Unit,
    viewModel: DashboardScreenViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val preferences: JellyfinPreferences = viewModel.preferences
    val topBarTabs by viewModel.topBarTabs.collectAsStateWithLifecycle()

    var currentDestination: String? by remember { mutableStateOf(null) }
    var isTopBarVisible by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Use the standard top bar tabs which include Home, Movies, Shows, Categories, Favourites, Search
    val screens = remember { TopBarTabs }

    // Calculate selected tab index based on current destination
    val selectedTabIndex = remember(currentDestination, screens) {
        when (currentDestination) {
            Screens.Profile() -> -1 // Profile screen index
            else -> {
                screens.indexOfFirst { screen ->
                    screen() == currentDestination
                }.takeIf { it >= 0 } ?: 0
            }
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    
    BackPressHandledArea(
        onBackPressed = onBackPressed
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DashboardTopBar(
                modifier = Modifier
                    .alpha(if (isTopBarVisible) 1f else 0f)
                    .padding(horizontal = ParentPadding.calculateStartPadding(layoutDirection)),
                selectedTabIndex = selectedTabIndex,
                screens = screens,
                onScreenSelection = { screen ->
                    navController.navigate(screen()) {
                        popUpTo(Screens.Home()) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                serverName = preferences.serverName
            )
            Body(
                openCategoryMovieList = openCategoryMovieList,
                openMovieDetailsScreen = openMovieDetailsScreen,
                openVideoPlayer = openVideoPlayer,
                updateTopBarVisibility = { isTopBarVisible = it },
                isTopBarVisible = isTopBarVisible,
                navController = navController,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun BackPressHandledArea(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) =
    Box(
        modifier = Modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBackPressed()
                    true
                } else {
                    false
                }
            }
            .then(modifier),
        content = content
    )

@Composable
private fun Body(
    openCategoryMovieList: (categoryId: String) -> Unit,
    openMovieDetailsScreen: (movieId: String) -> Unit,
    openVideoPlayer: (Movie) -> Unit,
    updateTopBarVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    isTopBarVisible: Boolean = true,
) =
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screens.Home(),
    ) {
        composable(
            route = "Library/{${LibraryScreen.LibraryIdBundleKey}}",
            arguments = listOf(
                navArgument(LibraryScreen.LibraryIdBundleKey) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString(LibraryScreen.LibraryIdBundleKey) ?: return@composable
            LibraryScreen(
                libraryId = libraryId,
                onBackPressed = {
                    navController.navigateUp()
                },
                onMovieSelected = { movie ->
                    openMovieDetailsScreen(movie.id)
                }
            )
        }
        composable(Screens.Profile()) {
            ProfileScreen()
        }
        composable(Screens.Home()) {
            HomeScreen(
                onMovieClick = { selectedMovie ->
                    openMovieDetailsScreen(selectedMovie.id)
                },
                goToVideoPlayer = openVideoPlayer,
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Categories()) {
            CategoriesScreen(
                onCategoryClick = openCategoryMovieList,
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Movies()) {
            MoviesScreen(
                onMovieClick = { movie -> openMovieDetailsScreen(movie.id) },
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Shows()) {
            ShowsScreen(
                onTVShowClick = { movie -> openMovieDetailsScreen(movie.id) },
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Favourites()) {
            FavouritesScreen(
                onMovieClick = openMovieDetailsScreen,
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Search()) {
            SearchScreen(
                onMovieClick = { movie -> openMovieDetailsScreen(movie.id) },
                onScroll = updateTopBarVisibility
            )
        }
    }
