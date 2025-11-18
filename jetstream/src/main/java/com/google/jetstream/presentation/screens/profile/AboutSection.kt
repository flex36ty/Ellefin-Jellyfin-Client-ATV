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

package com.google.jetstream.presentation.screens.profile

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.theme.JetStreamCardShape

@Composable
fun AboutSection(
    isCarouselEnabled: Boolean,
    onCarouselEnabledChange: (Boolean) -> Unit,
    isImmersiveListEnabled: Boolean,
    onImmersiveListEnabledChange: (Boolean) -> Unit,
    isDynamicBackgroundColorEnabled: Boolean,
    onDynamicBackgroundColorEnabledChange: (Boolean) -> Unit,
    isBlackBackgroundEnabled: Boolean,
    onBlackBackgroundEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val versionNumber = remember(context) {
        context.getVersionNumber()
    }

    with(StringConstants.Composable.Placeholders) {
        Column(modifier = Modifier.padding(horizontal = 72.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // Carousel toggle
            ListItem(
                modifier = Modifier.padding(top = 32.dp),
                selected = false,
                onClick = { onCarouselEnabledChange(!isCarouselEnabled) },
                trailingContent = {
                    Switch(
                        checked = isCarouselEnabled,
                        onCheckedChange = onCarouselEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                headlineContent = {
                    Text(
                        text = "Show Featured Carousel",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                shape = ListItemDefaults.shape(shape = JetStreamCardShape)
            )
            
            // Immersive list toggle
            ListItem(
                modifier = Modifier.padding(top = 16.dp),
                selected = false,
                onClick = { onImmersiveListEnabledChange(!isImmersiveListEnabled) },
                trailingContent = {
                    Switch(
                        checked = isImmersiveListEnabled,
                        onCheckedChange = onImmersiveListEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                headlineContent = {
                    Text(
                        text = "Immersive List Preview",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                shape = ListItemDefaults.shape(shape = JetStreamCardShape)
            )
            
            // Dynamic background color toggle
            ListItem(
                modifier = Modifier.padding(top = 16.dp),
                selected = false,
                onClick = { onDynamicBackgroundColorEnabledChange(!isDynamicBackgroundColorEnabled) },
                trailingContent = {
                    Switch(
                        checked = isDynamicBackgroundColorEnabled,
                        onCheckedChange = onDynamicBackgroundColorEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                headlineContent = {
                    Text(
                        text = "Dynamic Background Color",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                shape = ListItemDefaults.shape(shape = JetStreamCardShape)
            )
            
            // Black background toggle
            ListItem(
                modifier = Modifier.padding(top = 16.dp),
                selected = false,
                onClick = { onBlackBackgroundEnabledChange(!isBlackBackgroundEnabled) },
                trailingContent = {
                    Switch(
                        checked = isBlackBackgroundEnabled,
                        onCheckedChange = onBlackBackgroundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                headlineContent = {
                    Text(
                        text = "Black Background",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                shape = ListItemDefaults.shape(shape = JetStreamCardShape)
            )
            
            Text(
                modifier = Modifier
                    .graphicsLayer { alpha = 0.6f }
                    .padding(top = 24.dp),
                text = AboutSectionAppVersionTitle,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = versionNumber,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun Context.getVersionNumber(): String {
    val packageName = packageName
    val metaData = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    return metaData.versionName!!
}
