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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.jetstream.data.entities.MovieCategoryList
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.common.MovieCard
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.presentation.utils.GradientBg

@Composable
fun CategoriesScreen(
    gridColumns: Int = 4,
    onCategoryClick: (categoryId: String) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    categoriesScreenViewModel: CategoriesScreenViewModel = hiltViewModel()
) {

    val uiState by categoriesScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        CategoriesScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is CategoriesScreenUiState.Ready -> {
            if (s.categoryList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No libraries found. Please check your Jellyfin server connection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
            Catalog(
                gridColumns = gridColumns,
                movieCategories = s.categoryList,
                onCategoryClick = onCategoryClick,
                onScroll = onScroll,
                modifier = Modifier.fillMaxSize()
            )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Catalog(
    movieCategories: MovieCategoryList,
    modifier: Modifier = Modifier,
    gridColumns: Int = 4,
    onCategoryClick: (categoryId: String) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
) {
    val childPadding = rememberChildPadding()
    val lazyGridState = rememberLazyGridState()
    // Always show top bar - make it persistent
    LaunchedEffect(Unit) {
        onScroll(true)
    }

    AnimatedContent(
        targetState = movieCategories,
        modifier = Modifier
            .padding(horizontal = childPadding.start)
            .padding(top = childPadding.top),
        label = "",
    ) { it ->
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = modifier,
            columns = GridCells.Fixed(gridColumns),
        ) {
            itemsIndexed(it) { index, movieCategory ->
                var isFocused by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .onFocusChanged {
                            isFocused = it.isFocused || it.hasFocus
                        }
                        .focusProperties {
                            if (index % gridColumns == 0) {
                                left = FocusRequester.Cancel
                            }
                        }
                ) {
                    MovieCard(
                        onClick = {
                            onCategoryClick(movieCategory.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                ) {
                    val itemAlpha by animateFloatAsState(
                            targetValue = if (isFocused) 1f else 0.6f,
                        label = ""
                    )

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Show library image if available, otherwise show gradient background
                            if (!movieCategory.imageUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(
                                        androidx.compose.ui.platform.LocalContext.current
                                    )
                                        .data(movieCategory.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = movieCategory.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(itemAlpha)
                                )
                            } else {
                        Box(modifier = Modifier.alpha(itemAlpha)) {
                            GradientBg()
                        }
                            }
                        }
                    }
                    // Title below the card
                        Text(
                            text = movieCategory.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                        )
                }
            }
        }
    }
}
