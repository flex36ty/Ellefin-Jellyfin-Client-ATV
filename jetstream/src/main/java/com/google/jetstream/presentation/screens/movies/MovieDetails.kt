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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.jetstream.R
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import com.google.jetstream.presentation.theme.JetStreamButtonShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieDetails(
    movieDetails: MovieDetails,
    goToMoviePlayer: () -> Unit,
    onPlay: () -> Unit,
    onSubtitleSelect: () -> Unit = {}
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Focus the Play button by default when the screen loads
    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(432.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        MovieImageWithGradients(
            movieDetails = movieDetails,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Spacer(modifier = Modifier.height(60.dp))
            Column(
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .focusGroup()
            ) {
                MovieLargeTitle(movieTitle = movieDetails.name)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    MovieDescription(description = movieDetails.description)
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 20.dp),
                        texts = listOfNotNull(
                            movieDetails.pgRating.takeIf { it.isNotBlank() },
                            movieDetails.releaseDate.takeIf { it.isNotBlank() },
                            movieDetails.categories.joinToString(", ").takeIf { movieDetails.categories.isNotEmpty() },
                            movieDetails.duration.takeIf { it.isNotBlank() },
                            movieDetails.resolution.takeIf { it.isNotBlank() }
                        )
                    )
                    DirectorScreenplayMusicRow(
                        director = movieDetails.director,
                        screenplay = movieDetails.screenplay,
                        music = movieDetails.music
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlayButton(
                        modifier = Modifier
                            .focusRequester(playButtonFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        onPlay = onPlay
                    )
                    SubtitleSelectorButton(
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        },
                        onSubtitleSelect = onSubtitleSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayButton(
    modifier: Modifier = Modifier,
    onPlay: () -> Unit
) {
    Button(
        onClick = onPlay,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = JetStreamButtonShape),
        scale = ButtonDefaults.scale(),
        glow = ButtonDefaults.glow(),
        colors = ButtonDefaults.colors()
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Play",
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
internal fun SubtitleSelectorButton(
    modifier: Modifier = Modifier,
    onSubtitleSelect: () -> Unit
) {
    Button(
        onClick = onSubtitleSelect,
        modifier = modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        shape = ButtonDefaults.shape(shape = CircleShape),
        scale = ButtonDefaults.scale(),
        glow = ButtonDefaults.glow(),
        colors = ButtonDefaults.colors()
    ) {
        Icon(
            imageVector = Icons.Outlined.Subtitles,
            contentDescription = "Subtitles",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun DirectorScreenplayMusicRow(
    director: String,
    screenplay: String,
    music: String
) {
    Row(modifier = Modifier.padding(top = 32.dp)) {
        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = stringResource(R.string.director),
            value = director
        )

        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = stringResource(R.string.screenplay),
            value = screenplay
        )

        TitleValueText(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.music),
            value = music
        )
    }
}

@Composable
internal fun MovieDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.75f),
                offset = Offset(1f, 1f),
                blurRadius = 6f
            )
        ),
        modifier = Modifier.padding(top = 8.dp),
        maxLines = 2
    )
}

@Composable
internal fun MovieLargeTitle(movieTitle: String) {
    Text(
        text = movieTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.75f),
                offset = Offset(2f, 2f),
                blurRadius = 8f
            )
        ),
        maxLines = 1
    )
}

@Composable
internal fun MovieImageWithGradients(
    movieDetails: MovieDetails,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(movieDetails.posterUri)
            .crossfade(true).build(),
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .moviePoster(movieDetails.name),
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            drawContent()
            // Darker overlay to improve text readability (40% opacity black)
            drawRect(
                color = Color.Black.copy(alpha = 0.4f)
            )
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, gradientColor.copy(alpha = 0.8f)),
                    startY = 600f
                )
            )
            drawRect(
                Brush.horizontalGradient(
                    colors = listOf(gradientColor.copy(alpha = 0.8f), Color.Transparent),
                    endX = 1000f,
                    startX = 300f
                )
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(gradientColor.copy(alpha = 0.8f), Color.Transparent),
                    start = Offset(x = 500f, y = 500f),
                    end = Offset(x = 1000f, y = 0f)
                )
            )
        }
    )
}
