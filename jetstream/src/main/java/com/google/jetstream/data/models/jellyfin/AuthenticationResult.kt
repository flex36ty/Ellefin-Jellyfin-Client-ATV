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

package com.google.jetstream.data.models.jellyfin

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResult(
    val User: UserDto? = null,
    val SessionInfo: SessionInfoDto? = null,
    val AccessToken: String? = null,
    val ServerId: String? = null
)

@Serializable
data class UserDto(
    val Id: String,
    val Name: String,
    val ServerId: String? = null,
    val HasPassword: Boolean = false,
    val HasConfiguredPassword: Boolean = false,
    val HasConfiguredEasyPassword: Boolean = false,
    val EnableAutoLogin: Boolean = false,
    val LastLoginDate: String? = null,
    val LastActivityDate: String? = null,
    val Configuration: UserConfiguration? = null,
    val Policy: UserPolicy? = null
)

@Serializable
data class UserConfiguration(
    val PlayDefaultAudioTrack: Boolean = true,
    val SubtitleMode: String? = null,
    val EnableLocalPassword: Boolean = false,
    val OrderedViews: List<String>? = null,
    val LatestItemsExcludes: List<String>? = null,
    val MyMediaExcludes: List<String>? = null
)

@Serializable
data class UserPolicy(
    val IsAdministrator: Boolean = false,
    val IsHidden: Boolean = false,
    val IsDisabled: Boolean = false,
    val EnableLiveTvManagement: Boolean = false,
    val EnableLiveTvAccess: Boolean = false,
    val EnableContentDeletion: Boolean = false,
    val EnableContentDownloading: Boolean = false
)

@Serializable
data class SessionInfoDto(
    val Id: String? = null,
    val ServerId: String? = null,
    val UserId: String? = null,
    val UserName: String? = null,
    val Client: String? = null,
    val DeviceId: String? = null,
    val DeviceName: String? = null,
    val NowPlayingItem: BaseItemDto? = null
)
