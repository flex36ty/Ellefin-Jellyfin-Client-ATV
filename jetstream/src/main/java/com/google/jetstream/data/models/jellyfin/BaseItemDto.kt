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

/**
 * BaseItemDto - Main model for Jellyfin items (movies, TV shows, etc.)
 * Based on Jellyfin API: https://api.jellyfin.org/
 */
@Serializable
data class BaseItemDto(
    val Id: String,
    val Name: String,
    val ServerId: String? = null,
    val Type: String? = null, // Movie, Series, Episode, etc.
    val ParentId: String? = null,
    val Overview: String? = null,
    val ProductionYear: Int? = null,
    val PremiereDate: String? = null,
    val EndDate: String? = null,
    val DateCreated: String? = null, // ISO date string for when item was added
    val CommunityRating: Float? = null,
    val OfficialRating: String? = null, // MPAA rating
    val RunTimeTicks: Long? = null,
    val ProductionLocations: List<String>? = null,
    val Genres: List<String>? = null,
    val Taglines: List<String>? = null,
    val Studios: List<NameIdPair>? = null,
    val ImageTags: ImageTags? = null,
    val BackdropImageTags: List<String>? = null,
    val ImageBlurHashes: ImageBlurHashes? = null,
    val People: List<BaseItemPerson>? = null,
    val PersonCount: Int? = null,
    val IndexNumber: Int? = null,
    val IndexNumberEnd: Int? = null,
    val ChildCount: Int? = null,
    val SeriesName: String? = null,
    val SeriesId: String? = null,
    val SeasonId: String? = null,
    val SeasonName: String? = null,
    val EpisodeCount: Int? = null,
    val UserData: UserItemDataDto? = null,
    val MediaStreams: List<MediaStream>? = null,
    val MediaSources: List<MediaSourceInfo>? = null,
    val IsFolder: Boolean = false,
    val IsFavorite: Boolean = false,
    val IsPlayed: Boolean = false,
    val LocationType: String? = null,
    val Path: String? = null
)

@Serializable
data class NameIdPair(
    val Name: String,
    val Id: String? = null
)

@Serializable
data class ImageTags(
    val Primary: String? = null,
    val Logo: String? = null,
    val Art: String? = null,
    val Banner: String? = null,
    val Thumb: String? = null,
    val Disc: String? = null,
    val Box: String? = null,
    val BoxRear: String? = null,
    val Profile: String? = null
)

@Serializable
data class ImageBlurHashes(
    val Primary: Map<String, String>? = null,
    val Art: Map<String, String>? = null,
    val Backdrop: Map<String, String>? = null,
    val Banner: Map<String, String>? = null,
    val Logo: Map<String, String>? = null,
    val Thumb: Map<String, String>? = null,
    val Disc: Map<String, String>? = null,
    val Box: Map<String, String>? = null,
    val BoxRear: Map<String, String>? = null,
    val Profile: Map<String, String>? = null
)

@Serializable
data class BaseItemPerson(
    val Name: String,
    val Id: String? = null,
    val Role: String? = null,
    val Type: String? = null, // Actor, Director, Writer, etc.
    val PrimaryImageTag: String? = null
)

@Serializable
data class UserItemDataDto(
    val PlaybackPositionTicks: Long = 0,
    val PlayCount: Int = 0,
    val IsFavorite: Boolean = false,
    val IsPlayed: Boolean = false,
    val LastPlayedDate: String? = null,
    val Played: Boolean = false,
    val Key: String? = null,
    val ItemId: String? = null
)

@Serializable
data class MediaStream(
    val Codec: String? = null,
    val Language: String? = null,
    val Type: String? = null, // Video, Audio, Subtitle
    val Index: Int? = null,
    val IsDefault: Boolean = false,
    val IsExternal: Boolean = false,
    val Path: String? = null,
    val DisplayTitle: String? = null,
    val Width: Int? = null,
    val Height: Int? = null
)

@Serializable
data class MediaSourceInfo(
    val Id: String? = null,
    val Path: String? = null,
    val Protocol: String? = null, // File, Http
    val Container: String? = null,
    val Size: Long? = null,
    val Name: String? = null,
    val MediaStreams: List<MediaStream>? = null,
    val SupportsDirectPlay: Boolean = false,
    val SupportsDirectStream: Boolean = false,
    val SupportsTranscoding: Boolean = false
)
