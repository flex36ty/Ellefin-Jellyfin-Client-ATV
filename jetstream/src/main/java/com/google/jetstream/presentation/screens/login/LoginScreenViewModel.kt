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

package com.google.jetstream.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.repositories.JellyfinDataSource
import com.google.jetstream.data.util.JellyfinPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val jellyfinDataSource: JellyfinDataSource,
    private val preferences: JellyfinPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            serverUrl = preferences.serverUrl ?: "",
            isLoggedIn = preferences.isLoggedIn
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url.trim()) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }
    
    /**
     * Builds the full server URL with protocol and port if needed
     */
    private fun buildServerUrl(input: String): String {
        val trimmed = input.trim()
        
        // If already has protocol, use as-is
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        
        // Check if port is included (has : but not at start)
        val hasPort = trimmed.contains(":") && !trimmed.startsWith(":")
        
        // If no protocol but has port, add http://
        if (hasPort) {
            return "http://$trimmed"
        }
        
        // No protocol and no port, add default port
        return "http://$trimmed:8096"
    }

    /**
     * Login with username and password
     */
    fun login() {
        val currentState = _uiState.value
        
        if (currentState.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Server IP is required") }
            return
        }
        
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(error = "Username is required") }
            return
        }
        
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(error = "Password is required") }
            return
        }

        _uiState.update { 
            it.copy(isLoading = true, error = null) 
        }

        viewModelScope.launch {
            try {
                val fullServerUrl = buildServerUrl(currentState.serverUrl)
                
                android.util.Log.d("LoginScreenViewModel", "Attempting to connect to: $fullServerUrl")
                
                // Save server URL first
                preferences.serverUrl = fullServerUrl
                
                // Authenticate
                val authResult = jellyfinDataSource.authenticate(
                    username = currentState.username,
                    password = currentState.password
                )
                
                android.util.Log.d("LoginScreenViewModel", "Authentication successful")
                
                // Save credentials
                authResult.AccessToken?.let {
                    preferences.accessToken = it
                }
                
                authResult.User?.Id?.let {
                    preferences.userId = it
                }
                
                authResult.User?.Name?.let {
                    preferences.username = it
                }
                
                // Mark as logged in
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        error = null
                    )
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("LoginScreenViewModel", "Unknown host: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Cannot reach server. Check the server IP and network connection."
                    )
                }
            } catch (e: javax.net.ssl.SSLException) {
                android.util.Log.e("LoginScreenViewModel", "SSL error: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "SSL certificate error. If using HTTPS, ensure the certificate is valid."
                    )
                }
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("LoginScreenViewModel", "Connection timeout: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Connection timeout. Check server IP and network."
                    )
                }
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("LoginScreenViewModel", "Connection failed: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Cannot connect to server. Check the server IP and port (default: 8096)."
                    )
                }
            } catch (e: retrofit2.HttpException) {
                android.util.Log.e("LoginScreenViewModel", "HTTP error: ${e.code()} - ${e.message()}", e)
                val errorMessage = when (e.code()) {
                    401 -> "Invalid username or password"
                    403 -> "Access denied"
                    404 -> "Server endpoint not found. Check server IP."
                    500, 502, 503, 504 -> "Server error. Try again later."
                    else -> "Server error (${e.code()}): ${e.message()}"
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginScreenViewModel", "Login failed: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to connect: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
