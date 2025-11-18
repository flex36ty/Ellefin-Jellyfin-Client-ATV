/*
 * Copyright 2025 Google LLC
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

package com.google.jetstream.presentation.screens.videoPlayer.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.google.jetstream.data.util.JellyfinPreferences

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(
    context: Context,
    preferences: JellyfinPreferences? = null
) = remember {
    // Create HTTP data source factory with Jellyfin authentication headers
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Android TV")
        .setConnectTimeoutMs(30_000)
        .setReadTimeoutMs(30_000)
        .apply {
            if (preferences != null) {
                val token = preferences.accessToken
                val deviceId = preferences.deviceId
                if (!token.isNullOrBlank() && !deviceId.isNullOrBlank()) {
                    val device = "Android TV"
                    val version = "1.0.0"
                    val authHeader = "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\", Token=\"$token\""
                    setDefaultRequestProperties(mapOf("X-Emby-Authorization" to authHeader))
                }
            }
        }
    
    // Create default data source factory that uses HTTP for network requests
    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    
    ExoPlayer.Builder(context)
        .setSeekForwardIncrementMs(10)
        .setSeekBackIncrementMs(10)
        .setMediaSourceFactory(
            ProgressiveMediaSource.Factory(dataSourceFactory)
        )
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
}
