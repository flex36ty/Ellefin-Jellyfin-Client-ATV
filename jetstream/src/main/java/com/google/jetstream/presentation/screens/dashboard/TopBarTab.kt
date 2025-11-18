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

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.jetstream.presentation.screens.Screens

/**
 * Represents a tab in the top navigation bar
 */
sealed class TopBarTab {
    abstract val route: String
    abstract val displayName: String
    abstract val icon: ImageVector?
    
    data class ScreenTab(
        val screen: Screens
    ) : TopBarTab() {
        override val route: String = screen()
        override val displayName: String = screen.name
        override val icon: ImageVector? = screen.tabIcon
    }
    
    data class LibraryTab(
        val libraryId: String,
        val libraryName: String
    ) : TopBarTab() {
        override val route: String = "Library/{${com.google.jetstream.presentation.screens.library.LibraryScreen.LibraryIdBundleKey}}"
        override val displayName: String = libraryName
        override val icon: ImageVector? = null
        
        fun routeWithId(): String = "Library/$libraryId"
    }
}

