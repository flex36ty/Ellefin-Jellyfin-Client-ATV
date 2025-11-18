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

package com.google.jetstream.data.util

import android.util.Log
import com.google.jetstream.data.entities.Episode
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieCast
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.entities.Season
import com.google.jetstream.data.entities.ThumbnailType
import com.google.jetstream.data.models.jellyfin.BaseItemDto
import com.google.jetstream.data.models.jellyfin.BaseItemPerson
import java.util.concurrent.TimeUnit

/**
 * Converts Jellyfin BaseItemDto to app entities
 */
fun BaseItemDto.toMovie(
    serverUrl: String,
    thumbnailType: ThumbnailType = ThumbnailType.Standard,
    accessToken: String? = null
): Movie {
    // Normalize serverUrl to have trailing slash
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    // For TV shows (Series), always use backdrop images instead of preview/poster
    val isTVShow = Type == "Series"
    
    // Build image URLs based on Jellyfin API format
    val (imageTag, imageType, maxWidth) = if (isTVShow) {
        // TV shows always use backdrop images
        val backdropTag = BackdropImageTags?.firstOrNull()
        if (!backdropTag.isNullOrBlank()) {
            Triple(backdropTag, "Backdrop", 3840)
        } else {
            // Fallback to primary if no backdrop available
            Triple(ImageTags?.Primary ?: ImageTags?.Thumb, "Primary", 3840)
        }
    } else {
        // For movies, use thumbnail type
        when (thumbnailType) {
            ThumbnailType.Standard -> Triple(
                ImageTags?.Primary ?: ImageTags?.Thumb,
                "Primary",
                400
            )
            ThumbnailType.Long -> {
                val backdropTag = BackdropImageTags?.firstOrNull()
                if (!backdropTag.isNullOrBlank()) {
                    // Use high quality (4K width) for hero backdrop images
                    Triple(backdropTag, "Backdrop", 3840)
                } else {
                    // Fallback to primary image if no backdrop available, still high quality
                    Triple(ImageTags?.Primary ?: ImageTags?.Thumb, "Primary", 3840)
                }
            }
        }
    }
    
    val posterUri = if (!imageTag.isNullOrBlank() && !Id.isBlank()) {
        // For Long thumbnails (hero/backdrop), request high quality images (4K width)
        // Jellyfin API will return the best available quality up to maxWidth
        val url = "${normalizedServerUrl}Items/$Id/Images/$imageType?maxWidth=$maxWidth&tag=$imageTag"
        Log.d("JellyfinConverter", "toMovie: Generated posterUri for ${Name} (Type=$Type): $url (type=$imageType, maxWidth=$maxWidth)")
        url
    } else {
        Log.w("JellyfinConverter", "toMovie: Missing imageTag or Id for ${Name}. ImageTags=$ImageTags, Id=$Id")
        ""
    }

    // Get video URL from MediaSources - always use Jellyfin streaming API, ignore local file paths
    val videoUri = if (!Id.isBlank()) {
        MediaSources?.firstOrNull()?.Id?.let { mediaSourceId ->
            "${normalizedServerUrl}Videos/$Id/stream?MediaSourceId=$mediaSourceId"
        } ?: "${normalizedServerUrl}Videos/$Id/stream"
    } else {
        ""
    }

    // Get subtitle URL
    val subtitleUri = MediaStreams
        ?.firstOrNull { it.Type == "Subtitle" && it.IsDefault }
        ?.let { "${normalizedServerUrl}Videos/${Id}/Subtitles/${it.Index}/Stream" }

    // Calculate progress percentage from UserData (PlaybackPositionTicks / RunTimeTicks)
    val progressPercentage = if (UserData != null && RunTimeTicks != null && RunTimeTicks > 0) {
        val playbackTicks = UserData.PlaybackPositionTicks ?: 0L
        val runtimeTicks = RunTimeTicks
        // Clamp between 0.0 and 1.0
        (playbackTicks.toFloat() / runtimeTicks.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    return Movie(
        id = Id,
        videoUri = videoUri,
        subtitleUri = subtitleUri,
        posterUri = posterUri,
        name = Name,
        description = Overview ?: "",
        progressPercentage = progressPercentage,
        dateAdded = DateCreated,
        dateReleased = PremiereDate
    )
}

fun BaseItemDto.toMovieDetails(
    serverUrl: String,
    similarMovies: List<Movie> = emptyList(),
    castList: List<MovieCast> = emptyList(),
    accessToken: String? = null
): MovieDetails {
    // Normalize serverUrl to have trailing slash
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    // For TV shows (Series), use backdrop images instead of preview/poster for background
    val isTVShow = Type == "Series"
    val (imageTag, imageType, maxWidth) = if (isTVShow) {
        // TV shows always use backdrop images for background
        val backdropTag = BackdropImageTags?.firstOrNull()
        if (!backdropTag.isNullOrBlank()) {
            Triple(backdropTag, "Backdrop", 3840)
        } else {
            // Fallback to primary if no backdrop available
            Triple(ImageTags?.Primary ?: ImageTags?.Thumb, "Primary", 3840)
        }
    } else {
        // For movies, use primary/preview image
        Triple(ImageTags?.Primary ?: ImageTags?.Thumb, "Primary", 800)
    }
    
    val posterUri = if (!imageTag.isNullOrBlank() && !Id.isBlank()) {
        val url = "${normalizedServerUrl}Items/$Id/Images/$imageType?maxWidth=$maxWidth&tag=$imageTag"
        Log.d("JellyfinConverter", "toMovieDetails: Generated posterUri for ${Name} (Type=$Type): $url (type=$imageType, maxWidth=$maxWidth)")
        url
    } else {
        Log.w("JellyfinConverter", "toMovieDetails: Missing imageTag or Id for ${Name}. ImageTags=$ImageTags, Id=$Id")
        ""
    }

    // Always use Jellyfin streaming API, ignore local file paths
    val videoUri = if (!Id.isBlank()) {
        MediaSources?.firstOrNull()?.Id?.let { mediaSourceId ->
            "${normalizedServerUrl}Videos/$Id/stream?MediaSourceId=$mediaSourceId"
        } ?: "${normalizedServerUrl}Videos/$Id/stream"
    } else {
        ""
    }

    val subtitleUri = MediaStreams
        ?.firstOrNull { it.Type == "Subtitle" && it.IsDefault }
        ?.let { "${normalizedServerUrl}Videos/${Id}/Subtitles/${it.Index}/Stream" }

    val duration = RunTimeTicks?.let {
        val hours = TimeUnit.MICROSECONDS.toHours(it / 10)
        val minutes = TimeUnit.MICROSECONDS.toMinutes(it / 10) % 60
        if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    } ?: ""

    // Get resolution from first video MediaStream in MediaSources
    val resolution = MediaSources
        ?.firstOrNull()
        ?.MediaStreams
        ?.firstOrNull { it.Type == "Video" }
        ?.let { stream ->
            when {
                stream.Width != null && stream.Height != null -> {
                    val height = stream.Height!!
                    when {
                        height >= 2160 -> "4K"
                        height >= 1440 -> "1440p"
                        height >= 1080 -> "1080p"
                        height >= 720 -> "720p"
                        height >= 480 -> "480p"
                        else -> "${stream.Width}x${stream.Height}"
                    }
                }
                else -> ""
            }
        } ?: ""

    val director = People?.firstOrNull { it.Type == "Director" }?.Name ?: ""
    val writer = People?.firstOrNull { it.Type == "Writer" }?.Name ?: ""
    val composer = People?.firstOrNull { it.Type == "Composer" }?.Name ?: ""

    return MovieDetails(
        id = Id,
        videoUri = videoUri,
        subtitleUri = subtitleUri,
        posterUri = posterUri,
        name = Name,
        description = Overview ?: "",
        pgRating = OfficialRating ?: "",
        releaseDate = PremiereDate?.take(10) ?: ProductionYear?.toString() ?: "",
        categories = Genres ?: emptyList(),
        duration = duration,
        resolution = resolution,
        director = director,
        screenplay = writer,
        music = composer,
        castAndCrew = castList,
        status = if (UserData?.Played == true) "Watched" else "Not Watched",
        originalLanguage = ProductionLocations?.firstOrNull() ?: "",
        budget = "", // Not available in Jellyfin API
        revenue = "", // Not available in Jellyfin API
        similarMovies = similarMovies,
        reviewsAndRatings = emptyList() // Can be populated from CommunityRating if needed
    )
}

fun BaseItemPerson.toMovieCast(serverUrl: String, itemId: String, accessToken: String? = null): MovieCast {
    // Normalize serverUrl to have trailing slash
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    // Use the person's ID for the image URL, not the item ID
    val personId = Id ?: ""
    val imageTag = PrimaryImageTag
    val avatarUrl = if (!personId.isBlank() && !imageTag.isNullOrBlank()) {
        "${normalizedServerUrl}Items/$personId/Images/Primary?maxWidth=200&tag=$imageTag"
    } else {
        ""
    }

    return MovieCast(
        id = personId,
        characterName = Role ?: "",
        realName = Name,
        avatarUrl = avatarUrl
    )
}

/**
 * Gets the image URL for a Jellyfin item
 */
fun BaseItemDto.getImageUrl(
    serverUrl: String,
    imageType: String = "Primary",
    maxWidth: Int = 400,
    accessToken: String? = null
): String {
    // Normalize serverUrl to have trailing slash
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    val imageTag = when (imageType) {
        "Primary" -> ImageTags?.Primary
        "Backdrop" -> BackdropImageTags?.firstOrNull()
        "Logo" -> ImageTags?.Logo
        "Thumb" -> ImageTags?.Thumb
        else -> ImageTags?.Primary
    }

    return if (!imageTag.isNullOrBlank() && !Id.isBlank()) {
        "${normalizedServerUrl}Items/$Id/Images/$imageType?maxWidth=$maxWidth&tag=$imageTag"
    } else {
        ""
    }
}

/**
 * Gets the video stream URL for a Jellyfin item
 */
fun BaseItemDto.getVideoUrl(serverUrl: String, accessToken: String? = null): String {
    // Normalize serverUrl to have trailing slash
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    // Always use Jellyfin streaming API, ignore local file paths
    return if (!Id.isBlank()) {
        MediaSources?.firstOrNull()?.Id?.let { mediaSourceId ->
            "${normalizedServerUrl}Videos/$Id/stream?MediaSourceId=$mediaSourceId"
        } ?: "${normalizedServerUrl}Videos/$Id/stream"
    } else {
        ""
    }
}

/**
 * Converts BaseItemDto to Season
 */
fun BaseItemDto.toSeason(serverUrl: String, accessToken: String? = null): Season {
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    val imageTag = ImageTags?.Primary ?: ImageTags?.Thumb
    val imageUrl = if (!imageTag.isNullOrBlank() && !Id.isBlank()) {
        "${normalizedServerUrl}Items/$Id/Images/Primary?maxWidth=400&tag=$imageTag"
    } else {
        ""
    }
    
    return Season(
        id = Id,
        name = Name,
        indexNumber = IndexNumber,
        overview = Overview,
        premiereDate = PremiereDate,
        endDate = EndDate,
        imageUrl = imageUrl
    )
}

/**
 * Converts BaseItemDto (Episode) to Movie for display in rows
 * Uses primary images (posters) and episode title
 */
fun BaseItemDto.toMovieFromEpisode(serverUrl: String, accessToken: String? = null, useBackdrop: Boolean = false): Movie {
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    // Determine which image to use based on useBackdrop flag
    val posterUri = if (useBackdrop) {
        // Use series backdrop for horizontal cards (like in Shows screen)
        // Episodes don't typically have backdrops, so use the series backdrop
        if (!SeriesId.isNullOrBlank()) {
            "${normalizedServerUrl}Items/$SeriesId/Images/Backdrop?maxWidth=3840"
        } else {
            // Fallback: try episode backdrop if it exists, otherwise Primary
            val backdropTag = BackdropImageTags?.firstOrNull()
            if (!backdropTag.isNullOrBlank()) {
                "${normalizedServerUrl}Items/$Id/Images/Backdrop?maxWidth=3840&tag=$backdropTag"
            } else {
                // Last fallback to Primary
                val primaryTag = ImageTags?.Primary ?: ImageTags?.Thumb
                if (!primaryTag.isNullOrBlank() && !Id.isBlank()) {
                    "${normalizedServerUrl}Items/$Id/Images/Primary?maxWidth=400&tag=$primaryTag"
                } else {
                    ""
                }
            }
        }
    } else {
        // Use primary image (poster) for vertical cards (like in home screen)
        // Prioritize Primary, avoid Thumb to ensure we get proper poster images
        val primaryTag = ImageTags?.Primary
        if (!primaryTag.isNullOrBlank() && !Id.isBlank()) {
            "${normalizedServerUrl}Items/$Id/Images/Primary?maxWidth=400&tag=$primaryTag"
        } else {
            // If no Primary, use Thumb as last resort
            val thumbTag = ImageTags?.Thumb
            if (!thumbTag.isNullOrBlank() && !Id.isBlank()) {
                "${normalizedServerUrl}Items/$Id/Images/Primary?maxWidth=400&tag=$thumbTag"
            } else {
                ""
            }
        }
    }
    
    // Always use Jellyfin streaming API, ignore local file paths
    val videoUri = if (!Id.isBlank()) {
        MediaSources?.firstOrNull()?.Id?.let { mediaSourceId ->
            "${normalizedServerUrl}Videos/$Id/stream?MediaSourceId=$mediaSourceId"
        } ?: "${normalizedServerUrl}Videos/$Id/stream"
    } else {
        ""
    }
    
    val subtitleUri = MediaStreams
        ?.firstOrNull { it.Type == "Subtitle" && it.IsDefault }
        ?.let { "${normalizedServerUrl}Videos/${Id}/Subtitles/${it.Index}/Stream" }
    
    val progressPercentage = if (UserData != null && RunTimeTicks != null && RunTimeTicks > 0) {
        val playbackTicks = UserData.PlaybackPositionTicks ?: 0L
        (playbackTicks.toFloat() / RunTimeTicks.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Use episode name as the title
    return Movie(
        id = Id,
        videoUri = videoUri,
        subtitleUri = subtitleUri,
        posterUri = posterUri,
        name = Name,
        description = Overview ?: "",
        progressPercentage = progressPercentage,
        dateAdded = DateCreated,
        dateReleased = PremiereDate
    )
}

/**
 * Converts BaseItemDto to Episode
 */
fun BaseItemDto.toEpisode(serverUrl: String, accessToken: String? = null): Episode {
    val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    val imageTag = ImageTags?.Primary ?: ImageTags?.Thumb
    val imageUrl = if (!imageTag.isNullOrBlank() && !Id.isBlank()) {
        "${normalizedServerUrl}Items/$Id/Images/Primary?maxWidth=300&tag=$imageTag"
    } else {
        ""
    }
    
    // Always use Jellyfin streaming API, ignore local file paths
    val videoUri = if (!Id.isBlank()) {
        MediaSources?.firstOrNull()?.Id?.let { mediaSourceId ->
            "${normalizedServerUrl}Videos/$Id/stream?MediaSourceId=$mediaSourceId"
        } ?: "${normalizedServerUrl}Videos/$Id/stream"
    } else {
        ""
    }
    
    val subtitleUri = MediaStreams
        ?.firstOrNull { it.Type == "Subtitle" && it.IsDefault }
        ?.let { "${normalizedServerUrl}Videos/${Id}/Subtitles/${it.Index}/Stream" }
    
    val runtime = RunTimeTicks?.let {
        val minutes = TimeUnit.MICROSECONDS.toMinutes(it / 10)
        "${minutes}m"
    } ?: ""
    
    val progressPercentage = if (UserData != null && RunTimeTicks != null && RunTimeTicks > 0) {
        val playbackTicks = UserData.PlaybackPositionTicks ?: 0L
        (playbackTicks.toFloat() / RunTimeTicks.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    return Episode(
        id = Id,
        name = Name,
        overview = Overview,
        seasonNumber = null, // Will be populated when grouping episodes by season
        episodeNumber = IndexNumber,
        premiereDate = PremiereDate,
        runtime = runtime,
        imageUrl = imageUrl,
        videoUri = videoUri,
        subtitleUri = subtitleUri,
        progressPercentage = progressPercentage
    )
}
