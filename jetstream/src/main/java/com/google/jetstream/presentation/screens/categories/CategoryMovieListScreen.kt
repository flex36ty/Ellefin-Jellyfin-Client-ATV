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

package com.google.jetstream.presentation.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieCategoryDetails
import com.google.jetstream.presentation.common.Error
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MovieCard
import com.google.jetstream.presentation.common.PosterImage
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.presentation.theme.JetStreamBottomListPadding
import com.google.jetstream.presentation.utils.focusOnInitialVisibility
import com.google.jetstream.tvmaterial.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

object CategoryMovieListScreen {
    const val CategoryIdBundleKey = "categoryId"
}

enum class SortOption(val displayName: String) {
    DATE_ADDED_DESC("Date Added (Newest First)"),
    DATE_ADDED_ASC("Date Added (Oldest First)"),
    DATE_RELEASED_DESC("Date Released (Newest First)"),
    DATE_RELEASED_ASC("Date Released (Oldest First)"),
    ALPHABETICAL_ASC("Alphabetical (A-Z)"),
    ALPHABETICAL_DESC("Alphabetical (Z-A)")
}

@Composable
fun CategoryMovieListScreen(
    onBackPressed: () -> Unit,
    onHomePressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    categoryMovieListScreenViewModel: CategoryMovieListScreenViewModel = hiltViewModel()
) {
    val uiState by categoryMovieListScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        CategoryMovieListScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        CategoryMovieListScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is CategoryMovieListScreenUiState.Done -> {
            val categoryDetails = s.movieCategoryDetails
            CategoryDetails(
                categoryDetails = categoryDetails,
                onBackPressed = onBackPressed,
                onHomePressed = onHomePressed,
                onMovieSelected = onMovieSelected
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryDetails(
    categoryDetails: MovieCategoryDetails,
    onBackPressed: () -> Unit,
    onHomePressed: () -> Unit,
    onMovieSelected: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val childPadding = rememberChildPadding()
    val isFirstItemVisible = remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_ADDED_DESC) }
    var showSortDialog by remember { mutableStateOf(false) }
    var sortedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }

    BackHandler(onBack = if (showSortDialog) { { showSortDialog = false } } else { onBackPressed })

    // Pre-parse dates and sort in background to avoid ANR
    LaunchedEffect(categoryDetails.movies, sortOption) {
        sortedMovies = withContext(Dispatchers.Default) {
            // Pre-parse dates once for all movies
            val moviesWithDates = categoryDetails.movies.map { movie ->
                movie to Pair(
                    parseDate(movie.dateAdded),
                    parseDate(movie.dateReleased)
                )
            }

            when (sortOption) {
                SortOption.DATE_ADDED_DESC -> moviesWithDates
                    .sortedByDescending { (_, dates) -> dates.first ?: Long.MIN_VALUE }
                    .map { (movie, _) -> movie }
                SortOption.DATE_ADDED_ASC -> moviesWithDates
                    .sortedBy { (_, dates) -> dates.first ?: Long.MAX_VALUE }
                    .map { (movie, _) -> movie }
                SortOption.DATE_RELEASED_DESC -> moviesWithDates
                    .sortedByDescending { (_, dates) -> dates.second ?: Long.MIN_VALUE }
                    .map { (movie, _) -> movie }
                SortOption.DATE_RELEASED_ASC -> moviesWithDates
                    .sortedBy { (_, dates) -> dates.second ?: Long.MAX_VALUE }
                    .map { (movie, _) -> movie }
                SortOption.ALPHABETICAL_ASC -> categoryDetails.movies.sortedBy { it.name.lowercase() }
                SortOption.ALPHABETICAL_DESC -> categoryDetails.movies.sortedByDescending { it.name.lowercase() }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Navigation buttons and sort button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = childPadding.start,
                    top = childPadding.top,
                    end = childPadding.end,
                    bottom = 8.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onBackPressed,
                    modifier = Modifier.size(48.dp),
                    shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = LocalContentColor.current
                    )
                }

                // Home button
                Surface(
                    onClick = onHomePressed,
                    modifier = Modifier.size(48.dp),
                    shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        tint = LocalContentColor.current
                    )
                }
            }

            // Sort button - opens dropdown dialog
            Button(
                onClick = { showSortDialog = true },
                modifier = Modifier.padding(start = 16.dp),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                scale = ButtonDefaults.scale(),
                glow = ButtonDefaults.glow(),
                colors = ButtonDefaults.colors()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = sortOption.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Sort options dialog
        SortOptionsDialog(
            showDialog = showSortDialog,
            currentOption = sortOption,
            onDismissRequest = { showSortDialog = false },
            onOptionSelected = { option ->
                sortOption = option
                showSortDialog = false
            }
        )

        // Library title
        Text(
            text = categoryDetails.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .padding(
                    start = childPadding.start,
                    top = 8.dp,
                    bottom = 8.dp
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(
                start = childPadding.start,
                top = 0.dp,
                bottom = JetStreamBottomListPadding
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                sortedMovies,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun SortOptionsDialog(
    showDialog: Boolean,
    currentOption: SortOption,
    onDismissRequest: () -> Unit,
    onOptionSelected: (SortOption) -> Unit
) {
    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        properties = DialogProperties()
    ) {
        val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        val dialogFocusRequester = remember { FocusRequester() }

        Box(
            modifier = Modifier
                .widthIn(min = 400.dp, max = 600.dp)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(8.dp)
                }
                .drawBehind { drawRect(color = elevatedContainerColor) }
                .focusRequester(dialogFocusRequester)
                .focusable()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Dialog title
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // List of sort options
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .focusRequester(dialogFocusRequester),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SortOption.entries) { option ->
                        val isSelected = option == currentOption
                        Surface(
                            onClick = { onOptionSelected(option) },
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(showDialog) {
            if (showDialog) {
                // Small delay to ensure the dialog is fully composed before requesting focus
                kotlinx.coroutines.delay(100)
                dialogFocusRequester.requestFocus()
            }
        }
    }
}

private fun parseDate(dateString: String?): Long? {
    if (dateString.isNullOrBlank()) return null
    return try {
        // Try ISO 8601 format first
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(dateString)?.time
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)?.time
    } catch (e: Exception) {
        null
    }
}
