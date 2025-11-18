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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.repositories.JellyfinDataSource
import com.google.jetstream.data.util.JellyfinPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    val jellyfinDataSource: JellyfinDataSource,
    val preferences: JellyfinPreferences
) : ViewModel() {
    
    private val _logoutEvent = Channel<Unit>(Channel.BUFFERED)
    val logoutEvent = _logoutEvent.receiveAsFlow()
    
    fun logout() {
        // Send logout event first to trigger navigation
        // Credentials will be cleared after navigation completes
        viewModelScope.launch {
            _logoutEvent.send(Unit)
        }
    }
    
    /**
     * Actually clear credentials - called after navigation is complete
     */
    fun clearCredentials() {
        preferences.isLoggedIn = false
        preferences.accessToken = null
        preferences.userId = null
        preferences.username = null
    }
}

