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

package com.google.jetstream.data.repositories

import android.util.Log
import com.google.jetstream.data.api.JellyfinApiService
import com.google.jetstream.data.api.createRetrofitForServer
import com.google.jetstream.data.models.jellyfin.BaseItemDto
import com.google.jetstream.data.models.jellyfin.ItemsResult
import com.google.jetstream.data.util.JellyfinPreferences
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "JellyfinDataSource"

@Singleton
class JellyfinDataSource @Inject constructor(
    private val preferences: JellyfinPreferences,
    private val okHttpClient: OkHttpClient
) {
    private var apiService: JellyfinApiService? = null

    private fun getApiService(): JellyfinApiService {
        val serverUrl = preferences.serverUrl
            ?: throw IllegalStateException("Server URL not configured")

        // Create or get cached API service
        if (apiService == null || !isCurrentServer(serverUrl)) {
            val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
            apiService = retrofit.create(JellyfinApiService::class.java)
        }

        return apiService!!
    }
    
    private fun getApiServiceOrNull(): JellyfinApiService? {
        val serverUrl = preferences.serverUrl ?: return null
        return try {
            if (apiService == null || !isCurrentServer(serverUrl)) {
                val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
                apiService = retrofit.create(JellyfinApiService::class.java)
            }
            apiService!!
        } catch (e: Exception) {
            null
        }
    }

    private fun isCurrentServer(serverUrl: String): Boolean {
        // Simple check - in production, cache the base URL
        return true
    }

    private fun getAuthHeader(): String {
        val token = preferences.accessToken
            ?: throw IllegalStateException("Not authenticated")
        val deviceId = preferences.deviceId
        val device = "Android TV"
        val version = "1.0.0"
        return "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\", Token=\"$token\""
    }
    
    fun isLoggedIn(): Boolean = preferences.isLoggedIn

    suspend fun authenticate(username: String, password: String): com.google.jetstream.data.models.jellyfin.AuthenticationResult {
        val serverUrl = preferences.serverUrl
            ?: throw IllegalStateException("Server URL not configured")

        val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
        val apiService = retrofit.create(JellyfinApiService::class.java)

        val deviceId = preferences.deviceId
        val device = "Android TV"
        val version = "1.0.0"
        val authHeader = "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\""

        val request = com.google.jetstream.data.api.AuthenticateByNameRequest(
            Username = username,
            Pw = password
        )

        val authResult = apiService.authenticateByName(request, authHeader)
        
        // Fetch server name after authentication
        try {
            val systemInfo = apiService.getSystemInfo(authHeader)
            preferences.serverName = systemInfo.ServerName
        } catch (e: Exception) {
            // If fetching system info fails, continue without server name
            android.util.Log.w(TAG, "Failed to fetch server name: ${e.message}")
        }
        
        return authResult
    }

    suspend fun getSystemInfo(): com.google.jetstream.data.models.jellyfin.SystemInfo {
        return try {
            val authHeader = getAuthHeader()
            getApiService().getSystemInfo(authHeader)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get system info: ${e.message}", e)
            // Return empty system info on error
            com.google.jetstream.data.models.jellyfin.SystemInfo()
        }
    }

    /**
     * Initiate Quick Connect - returns code and secret for pairing
     */
    suspend fun initiateQuickConnect(serverUrl: String): com.google.jetstream.data.models.jellyfin.QuickConnectState {
        val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
        val apiService = retrofit.create(JellyfinApiService::class.java)
        
        val deviceId = preferences.deviceId
        val device = "Android TV"
        val version = "1.0.0"
        val authHeader = "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\""
        
        return apiService.initiateQuickConnect(authHeader)
    }

    /**
     * Poll Quick Connect state - checks if the connection has been authenticated
     */
    suspend fun getQuickConnectState(serverUrl: String, secret: String): com.google.jetstream.data.models.jellyfin.QuickConnectState {
        val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
        val apiService = retrofit.create(JellyfinApiService::class.java)
        
        val deviceId = preferences.deviceId
        val device = "Android TV"
        val version = "1.0.0"
        val authHeader = "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\""
        
        return apiService.getQuickConnectState(secret, authHeader)
    }

    /**
     * Authenticate using Quick Connect secret
     */
    suspend fun authenticateWithQuickConnect(serverUrl: String, secret: String): com.google.jetstream.data.models.jellyfin.AuthenticationResult {
        val retrofit = createRetrofitForServer(serverUrl, okHttpClient)
        val apiService = retrofit.create(JellyfinApiService::class.java)
        
        val deviceId = preferences.deviceId
        val device = "Android TV"
        val version = "1.0.0"
        val authHeader = "MediaBrowser Client=\"$device\", Device=\"$device\", DeviceId=\"$deviceId\", Version=\"$version\""
        
        val request = com.google.jetstream.data.api.AuthenticateWithQuickConnectRequest(
            Secret = secret
        )
        
        val authResult = apiService.authenticateWithQuickConnect(request, authHeader)
        
        // Fetch server name after authentication
        try {
            val systemInfo = apiService.getSystemInfo(authHeader)
            preferences.serverName = systemInfo.ServerName
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to fetch server name: ${e.message}")
        }
        
        return authResult
    }

    suspend fun getLatestItems(limit: Int = 20, includeItemTypes: String? = null): List<BaseItemDto> {
        val userId = preferences.userId ?: return emptyList()
        return try {
            getApiService().getLatestItems(
                userId = userId,
                parentId = null,
                includeItemTypes = includeItemTypes,
                limit = limit,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getResumeItems(limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getResumeItems(
                userId = userId,
                includeItemTypes = null,
                limit = limit,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: HttpException) {
            val code = e.code()
            Log.e(TAG, "getResumeItems: HTTP $code - ${e.message}", e)
            if (code == 401) {
                handle401Error()
            }
            ItemsResult()
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getFavoriteItems(limit: Int = 50): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getFavoriteItems(
                userId = userId,
                filters = "IsFavorite",
                recursive = true,
                includeItemTypes = "Movie,Episode",
                limit = limit,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getMovies(parentId: String? = null, limit: Int = 50): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Movie",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Genres,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getTVShows(parentId: String? = null, limit: Int = 50): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Series",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Genres,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getSeasons(seriesId: String, limit: Int = 100): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getItems(
                userId = userId,
                parentId = seriesId,
                recursive = false,
                includeItemTypes = "Season",
                sortBy = "IndexNumber",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Overview",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getEpisodes(seriesId: String, limit: Int = 50): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getItems(
                userId = userId,
                parentId = seriesId,
                recursive = true,
                includeItemTypes = "Episode",
                sortBy = "IndexNumber",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Overview,MediaSources",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getEpisodesBySeason(seasonId: String, limit: Int = 100): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().getItems(
                userId = userId,
                parentId = seasonId,
                recursive = false,
                includeItemTypes = "Episode",
                sortBy = "IndexNumber",
                sortOrder = "Ascending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Overview,MediaSources",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    /**
     * Helper function to check if an HTTP error is temporary and should be retried
     */
    private fun isRetryableHttpError(e: Throwable): Boolean {
        if (e !is HttpException) return false
        val code = e.code()
        // Retry on server errors (5xx) except 501 (Not Implemented)
        return code in 500..599 && code != 501
    }

    /**
     * Helper function to handle 401 Unauthorized errors by clearing credentials
     */
    private fun handle401Error() {
        Log.w(TAG, "Authentication failed (401), clearing credentials")
        preferences.isLoggedIn = false
        preferences.accessToken = null
        preferences.userId = null
        preferences.username = null
    }

    /**
     * Helper function to retry an operation with exponential backoff
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        operation: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Don't retry on cancellation
                throw e
            } catch (e: Throwable) {
                lastException = e
                // Only retry on retryable HTTP errors
                if (!isRetryableHttpError(e)) {
                    throw e
                }
                
                if (attempt < maxRetries - 1) {
                    Log.w(TAG, "Retryable error (attempt ${attempt + 1}/$maxRetries): ${e.message}. Retrying in ${currentDelay}ms...")
                    delay(currentDelay)
                    currentDelay = minOf(currentDelay * 2, maxDelayMs) // Exponential backoff
                } else {
                    Log.e(TAG, "Max retries reached. Giving up.")
                }
            }
        }

        // If we get here, all retries failed
        throw lastException ?: IllegalStateException("Operation failed but no exception was caught")
    }

    suspend fun getLibraries(): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getLibraries: userId is null")
            return ItemsResult()
        }
        Log.d(TAG, "getLibraries: Fetching libraries for userId=$userId")
        return try {
            retryWithBackoff {
                val authHeader = getAuthHeader()
                Log.d(TAG, "getLibraries: Calling API with authHeader present")
                // Get root items which represent libraries/folders
                val result = getApiService().getItems(
                    userId = userId,
                    parentId = null,
                    recursive = false,
                    includeItemTypes = null, // Include all types (Folders, Collections, etc.)
                    sortBy = "SortName",
                    sortOrder = "Ascending",
                    limit = 100,
                    startIndex = null,
                    fields = "PrimaryImageAspectRatio,BasicSyncInfo,ImageTags",
                    authHeader = authHeader
                )
                Log.d(TAG, "getLibraries: Success! Found ${result.Items.size} libraries")
                result.Items.forEachIndexed { index, item ->
                    Log.d(TAG, "getLibraries: Library[$index] - Id=${item.Id}, Name=${item.Name}, Type=${item.Type}")
                }
                result
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Re-throw cancellation exceptions - they should propagate up
            Log.w(TAG, "getLibraries: Cancelled - ${e.message}")
            throw e
        } catch (e: HttpException) {
            val code = e.code()
            Log.e(TAG, "getLibraries: HTTP $code - ${e.message}", e)
            // On 401 Unauthorized, token has expired - clear credentials to trigger logout
            if (code == 401) {
                handle401Error()
            }
            // For non-retryable HTTP errors, return empty result
            ItemsResult()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getLibraries: IllegalStateException - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getLibraries: Exception - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }

    suspend fun getItemById(itemId: String): BaseItemDto? {
        val userId = preferences.userId ?: return null
        return try {
            getApiService().getItem(
                userId = userId,
                itemId = itemId,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Genres,People,Studios,MediaSources",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchItems(query: String, limit: Int = 50): ItemsResult {
        val userId = preferences.userId ?: return ItemsResult()
        return try {
            getApiService().searchItems(
                userId = userId,
                searchTerm = query,
                includeItemTypes = "Movie,Series,Episode",
                limit = limit,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
        } catch (e: IllegalStateException) {
            ItemsResult()
        } catch (e: Exception) {
            ItemsResult()
        }
    }

    suspend fun getRecentlyAddedMovies(parentId: String, limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getRecentlyAddedMovies: userId is null for parentId=$parentId")
            return ItemsResult()
        }
        Log.d(TAG, "getRecentlyAddedMovies: Fetching movies for parentId=$parentId, userId=$userId")
        return try {
            val result = getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Movie",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
            Log.d(TAG, "getRecentlyAddedMovies: Success! Found ${result.Items.size} movies for parentId=$parentId")
            result
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getRecentlyAddedMovies: IllegalStateException for parentId=$parentId - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getRecentlyAddedMovies: Exception for parentId=$parentId - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }

    suspend fun getRecentlyReleasedMovies(parentId: String, limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getRecentlyReleasedMovies: userId is null for parentId=$parentId")
            return ItemsResult()
        }
        Log.d(TAG, "getRecentlyReleasedMovies: Fetching movies for parentId=$parentId, userId=$userId")
        return try {
            val result = getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Movie",
                sortBy = "PremiereDate",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
            Log.d(TAG, "getRecentlyReleasedMovies: Success! Found ${result.Items.size} movies for parentId=$parentId")
            result
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getRecentlyReleasedMovies: IllegalStateException for parentId=$parentId - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getRecentlyReleasedMovies: Exception for parentId=$parentId - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }

    suspend fun getRecentlyAddedShows(parentId: String, limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getRecentlyAddedShows: userId is null for parentId=$parentId")
            return ItemsResult()
        }
        Log.d(TAG, "getRecentlyAddedShows: Fetching shows for parentId=$parentId, userId=$userId")
        return try {
            val result = getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Series",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,DateCreated,PremiereDate",
                authHeader = getAuthHeader()
            )
            Log.d(TAG, "getRecentlyAddedShows: Success! Found ${result.Items.size} shows for parentId=$parentId")
            result
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getRecentlyAddedShows: IllegalStateException for parentId=$parentId - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getRecentlyAddedShows: Exception for parentId=$parentId - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }

    suspend fun getRecentlyAddedEpisodes(limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getRecentlyAddedEpisodes: userId is null")
            return ItemsResult()
        }
        Log.d(TAG, "getRecentlyAddedEpisodes: Fetching recently added episodes, userId=$userId")
        return try {
            val result = getApiService().getLatestItems(
                userId = userId,
                parentId = null,
                includeItemTypes = "Episode",
                limit = limit,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Overview,MediaSources,ImageTags,BackdropImageTags",
                authHeader = getAuthHeader()
            )
            Log.d(TAG, "getRecentlyAddedEpisodes: Success! Found ${result.size} episodes")
            // Convert List<BaseItemDto> to ItemsResult
            ItemsResult(Items = result, TotalRecordCount = result.size)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getRecentlyAddedEpisodes: IllegalStateException - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getRecentlyAddedEpisodes: Exception - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }

    suspend fun getRecentlyAddedEpisodes(parentId: String, limit: Int = 20): ItemsResult {
        val userId = preferences.userId ?: run {
            Log.e(TAG, "getRecentlyAddedEpisodes: userId is null for parentId=$parentId")
            return ItemsResult()
        }
        Log.d(TAG, "getRecentlyAddedEpisodes: Fetching episodes for parentId=$parentId, userId=$userId")
        return try {
            val result = getApiService().getItems(
                userId = userId,
                parentId = parentId,
                recursive = true,
                includeItemTypes = "Episode",
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
                startIndex = null,
                fields = "PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,CanDownload,CanEditItems,MediaSourceCount,SortName,Overview,MediaSources,ImageTags,BackdropImageTags",
                authHeader = getAuthHeader()
            )
            Log.d(TAG, "getRecentlyAddedEpisodes: Success! Found ${result.Items.size} episodes for parentId=$parentId")
            result
        } catch (e: IllegalStateException) {
            Log.e(TAG, "getRecentlyAddedEpisodes: IllegalStateException for parentId=$parentId - ${e.message}", e)
            ItemsResult()
        } catch (e: Exception) {
            Log.e(TAG, "getRecentlyAddedEpisodes: Exception for parentId=$parentId - ${e.javaClass.simpleName}: ${e.message}", e)
            ItemsResult()
        }
    }
}
