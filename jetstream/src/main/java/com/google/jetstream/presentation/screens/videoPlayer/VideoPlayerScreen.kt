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

package com.google.jetstream.presentation.screens.videoPlayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import com.google.jetstream.data.entities.Episode
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.util.JellyfinPreferences
import com.google.jetstream.presentation.common.Error
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.screens.videoPlayer.components.AutoPlayCountdown
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerControls
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerOverlay
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.BACK
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulse.Type.FORWARD
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerPulseState
import com.google.jetstream.presentation.screens.videoPlayer.components.VideoPlayerState
import com.google.jetstream.presentation.screens.videoPlayer.components.rememberPlayer
import com.google.jetstream.presentation.screens.videoPlayer.components.rememberVideoPlayerPulseState
import com.google.jetstream.presentation.screens.videoPlayer.components.rememberVideoPlayerState
import com.google.jetstream.presentation.utils.handleDPadKeyEvents

object VideoPlayerScreen {
    const val MovieIdBundleKey = "movieId"
}

/**
 * [Work in progress] A composable screen for playing a video.
 *
 * @param onBackPressed The callback to invoke when the user presses the back button.
 * @param videoPlayerScreenViewModel The view model for the video player screen.
 */
@Composable
fun VideoPlayerScreen(
    onBackPressed: () -> Unit,
    onNextEpisode: ((String) -> Unit)? = null, // Callback to navigate to next episode
    videoPlayerScreenViewModel: VideoPlayerScreenViewModel = hiltViewModel()
) {
    val uiState by videoPlayerScreenViewModel.uiState.collectAsStateWithLifecycle()

    // TODO: Handle Loading & Error states
    when (val s = uiState) {
        is VideoPlayerScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is VideoPlayerScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is VideoPlayerScreenUiState.Done -> {
            VideoPlayerScreenContent(
                movieDetails = s.movieDetails,
                onBackPressed = onBackPressed,
                onNextEpisode = onNextEpisode,
                preferences = videoPlayerScreenViewModel.preferences
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreenContent(
    movieDetails: MovieDetails,
    onBackPressed: () -> Unit,
    onNextEpisode: ((String) -> Unit)? = null,
    preferences: JellyfinPreferences
) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context, preferences)

    val videoPlayerState = rememberVideoPlayerState(
        hideSeconds = 4,
    )

    var showAutoPlayCountdown by remember { mutableStateOf(false) }
    var nextEpisode by remember { mutableStateOf<Episode?>(null) }
    var countdownSeconds by remember { mutableStateOf(7) }

    // Check if current item is an episode (for TV shows)
    // When playing an episode, movieDetails.type will be "Episode" and it will have a parent series with all episodes
    val isEpisode = movieDetails.type == "Episode" && movieDetails.episodes.isNotEmpty()
    val currentEpisode = if (isEpisode) {
        // Find the current episode in the episodes list
        movieDetails.episodes.find { it.id == movieDetails.id }
    } else {
        null
    }

    LaunchedEffect(exoPlayer, movieDetails) {
        exoPlayer.addMediaItem(movieDetails.intoMediaItem())
        movieDetails.similarMovies.forEach {
            exoPlayer.addMediaItem(it.intoMediaItem())
        }
        exoPlayer.prepare()
    }

    // Listen for playback end to trigger auto-play countdown
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Player.STATE_ENDED = 4
                if (playbackState == Player.STATE_ENDED) {
                    // Check if this is an episode and there's a next episode
                    if (isEpisode && currentEpisode != null && onNextEpisode != null) {
                        // Find next episode in the episodes list
                        // Sort episodes by season and episode number to find the correct next episode
                        val sortedEpisodes = movieDetails.episodes.sortedWith(
                            compareBy<Episode>(
                                { it.seasonNumber ?: Int.MAX_VALUE },
                                { it.episodeNumber ?: Int.MAX_VALUE }
                            )
                        )
                        val currentEpisodeIndex = sortedEpisodes.indexOfFirst { it.id == currentEpisode.id }
                        if (currentEpisodeIndex >= 0 && currentEpisodeIndex < sortedEpisodes.size - 1) {
                            val next = sortedEpisodes[currentEpisodeIndex + 1]
                            nextEpisode = next
                            countdownSeconds = 7
                            showAutoPlayCountdown = true
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Stop and release player when screen is closed
    DisposableEffect(exoPlayer) {
        onDispose {
            // Stop playback
            exoPlayer.pause()
            exoPlayer.stop()
            // Clear media items
            exoPlayer.clearMediaItems()
            // Release player resources
            exoPlayer.release()
        }
    }

    BackHandler(onBack = if (showAutoPlayCountdown) {
        { showAutoPlayCountdown = false }
    } else {
        onBackPressed
    })

    val pulseState = rememberVideoPlayerPulseState()

    Box(
        Modifier
            .dPadEvents(
                exoPlayer,
                videoPlayerState,
                pulseState
            )
            .focusable()
    ) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = null
            )
        )

        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            isPlaying = exoPlayer.isPlaying,
            isControlsVisible = videoPlayerState.isControlsVisible,
            centerButton = { VideoPlayerPulse(pulseState) },
            subtitles = { /* TODO Implement subtitles */ },
            showControls = videoPlayerState::showControls,
            controls = {
                VideoPlayerControls(
                    player = exoPlayer,
                    movieDetails = movieDetails,
                    focusRequester = focusRequester,
                    onShowControls = { videoPlayerState.showControls(exoPlayer.isPlaying) },
                )
            }
        )

        // Auto-play countdown overlay
        if (showAutoPlayCountdown && nextEpisode != null) {
            AutoPlayCountdown(
                nextEpisode = nextEpisode!!,
                countdownSeconds = countdownSeconds,
                onPlayNext = {
                    showAutoPlayCountdown = false
                    onNextEpisode?.invoke(nextEpisode!!.id)
                },
                onCancel = {
                    showAutoPlayCountdown = false
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    videoPlayerState: VideoPlayerState,
    pulseState: VideoPlayerPulseState
): Modifier = this.handleDPadKeyEvents(
    onLeft = {
        if (!videoPlayerState.isControlsVisible) {
            exoPlayer.seekBack()
            pulseState.setType(BACK)
        }
    },
    onRight = {
        if (!videoPlayerState.isControlsVisible) {
            exoPlayer.seekForward()
            pulseState.setType(FORWARD)
        }
    },
    onUp = { videoPlayerState.showControls() },
    onDown = { videoPlayerState.showControls() },
    onEnter = {
        exoPlayer.pause()
        videoPlayerState.showControls()
    }
)

private fun MovieDetails.intoMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(videoUri)
        .setSubtitleConfigurations(
            if (subtitleUri == null) {
                emptyList()
            } else {
                listOf(
                    MediaItem.SubtitleConfiguration
                        .Builder(Uri.parse(subtitleUri))
                        .setMimeType("application/vtt")
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            }
        ).build()
}

private fun Movie.intoMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(videoUri)
        .setSubtitleConfigurations(
            if (subtitleUri == null) {
                emptyList()
            } else {
                listOf(
                    MediaItem.SubtitleConfiguration
                        .Builder(Uri.parse(subtitleUri))
                        .setMimeType("application/vtt")
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            }
        )
        .build()
}
