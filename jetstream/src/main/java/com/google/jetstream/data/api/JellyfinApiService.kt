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

package com.google.jetstream.data.api

import com.google.jetstream.data.models.jellyfin.BaseItemDto
import com.google.jetstream.data.models.jellyfin.AuthenticationResult
import com.google.jetstream.data.models.jellyfin.ItemsResult
import com.google.jetstream.data.models.jellyfin.SystemInfo
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Jellyfin API Service
 * Documentation: https://api.jellyfin.org/
 */
@JvmSuppressWildcards
interface JellyfinApiService {

    /**
     * Authenticate user by name
     * POST /Users/authenticatebyname
     */
    @POST("Users/authenticatebyname")
    @Headers("Content-Type: application/json")
    suspend fun authenticateByName(
        @Body request: AuthenticateByNameRequest,
        @Header("X-Emby-Authorization") authHeader: String
    ): AuthenticationResult

    /**
     * Get items from library
     * GET /Users/{userId}/Items
     */
    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String?,
        @Query("recursive") recursive: Boolean,
        @Query("includeItemTypes") includeItemTypes: String?,
        @Query("sortBy") sortBy: String?,
        @Query("sortOrder") sortOrder: String?,
        @Query("limit") limit: Int?,
        @Query("startIndex") startIndex: Int?,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): ItemsResult

    /**
     * Get item by ID
     * GET /Users/{userId}/Items/{itemId}
     */
    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): BaseItemDto

    /**
     * Search items
     * GET /Users/{userId}/Items
     */
    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Query("searchTerm") searchTerm: String,
        @Query("includeItemTypes") includeItemTypes: String?,
        @Query("limit") limit: Int?,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): ItemsResult

    /**
     * Get libraries
     * GET /Library/MediaFolders
     */
    @GET("Library/MediaFolders")
    suspend fun getLibraries(
        @Header("X-Emby-Authorization") authHeader: String
    ): ItemsResult

    /**
     * Get latest items
     * GET /Users/{userId}/Items/Latest
     */
    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String?,
        @Query("includeItemTypes") includeItemTypes: String?,
        @Query("limit") limit: Int?,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): List<BaseItemDto>

    /**
     * Get resumé items
     * GET /Users/{userId}/Items/Resume
     */
    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Query("includeItemTypes") includeItemTypes: String?,
        @Query("limit") limit: Int?,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): ItemsResult

    /**
     * Get favorite items
     * GET /Users/{userId}/Items
     */
    @GET("Users/{userId}/Items")
    suspend fun getFavoriteItems(
        @Path("userId") userId: String,
        @Query("filters") filters: String,
        @Query("recursive") recursive: Boolean,
        @Query("includeItemTypes") includeItemTypes: String?,
        @Query("limit") limit: Int?,
        @Query("fields") fields: String?,
        @Header("X-Emby-Authorization") authHeader: String
    ): ItemsResult

    /**
     * Get system information
     * GET /System/Info
     */
    @GET("System/Info")
    suspend fun getSystemInfo(
        @Header("X-Emby-Authorization") authHeader: String
    ): SystemInfo

    /**
     * Initiate Quick Connect
     * GET /QuickConnect/Initiate
     */
    @GET("QuickConnect/Initiate")
    suspend fun initiateQuickConnect(
        @Header("X-Emby-Authorization") authHeader: String
    ): com.google.jetstream.data.models.jellyfin.QuickConnectState

    /**
     * Get Quick Connect state
     * GET /QuickConnect/Connect
     */
    @GET("QuickConnect/Connect")
    suspend fun getQuickConnectState(
        @Query("secret") secret: String,
        @Header("X-Emby-Authorization") authHeader: String
    ): com.google.jetstream.data.models.jellyfin.QuickConnectState

    /**
     * Authenticate with Quick Connect
     * POST /Users/authenticatewithquickconnect
     */
    @POST("Users/authenticatewithquickconnect")
    @Headers("Content-Type: application/json")
    suspend fun authenticateWithQuickConnect(
        @Body request: AuthenticateWithQuickConnectRequest,
        @Header("X-Emby-Authorization") authHeader: String
    ): AuthenticationResult
}

@Serializable
data class AuthenticateByNameRequest(
    val Username: String,
    val Pw: String? = null
)

@Serializable
data class AuthenticateWithQuickConnectRequest(
    val Secret: String
)
