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

package com.google.jetstream.presentation.screens.videoPlayer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.google.jetstream.data.entities.Episode
import kotlinx.coroutines.delay

/**
 * Auto-play countdown overlay that appears when an episode finishes
 * Shows next episode info and a 7-second countdown
 */
@Composable
fun AutoPlayCountdown(
    nextEpisode: Episode,
    countdownSeconds: Int,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var currentCountdown by remember(countdownSeconds) { mutableStateOf(countdownSeconds) }
    var countdownProgress by remember { mutableStateOf(1f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = countdownProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "countdown"
    )

    LaunchedEffect(countdownSeconds) {
        focusRequester.requestFocus()
        // Show initial countdown
        currentCountdown = countdownSeconds
        countdownProgress = 1f
        
        // Countdown from countdownSeconds to 1
        for (i in countdownSeconds - 1 downTo 0) {
            delay(1000)
            currentCountdown = i
            countdownProgress = i.toFloat() / countdownSeconds.toFloat()
        }
        // After countdown reaches 0, wait a moment then play next episode
        delay(500)
        onPlayNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        // Next episode preview card
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(48.dp)
                .focusRequester(focusRequester),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (!nextEpisode.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(nextEpisode.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Next episode thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Episode info and controls
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Next up label
                Text(
                    text = "Next Up",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Episode title
                Text(
                    text = nextEpisode.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                // Episode number and synopsis
                if (nextEpisode.episodeNumber != null) {
                    Text(
                        text = "Episode ${nextEpisode.episodeNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                if (!nextEpisode.overview.isNullOrBlank()) {
                    Text(
                        text = nextEpisode.overview ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onPlayNext,
                        modifier = Modifier,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        scale = ButtonDefaults.scale(),
                        glow = ButtonDefaults.glow(),
                        colors = ButtonDefaults.colors()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play now",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Play Now",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        scale = ButtonDefaults.scale(),
                        glow = ButtonDefaults.glow()
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            // Countdown circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                
                // Background circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(surfaceColor.copy(alpha = 0.5f))
                )

                // Progress circle
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Countdown number
                Text(
                    text = currentCountdown.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = onSurfaceColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

