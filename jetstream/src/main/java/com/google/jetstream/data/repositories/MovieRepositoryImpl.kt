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
import com.google.jetstream.data.entities.Episode
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieCategory
import com.google.jetstream.data.entities.MovieCategoryDetails
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.entities.Season
import com.google.jetstream.data.entities.SubtitleTrack
import com.google.jetstream.data.entities.ThumbnailType
import com.google.jetstream.data.util.JellyfinPreferences
import com.google.jetstream.data.util.toEpisode
import com.google.jetstream.data.util.toMovie
import com.google.jetstream.data.util.toMovieCast
import com.google.jetstream.data.util.toMovieDetails
import com.google.jetstream.data.util.toMovieFromEpisode
import com.google.jetstream.data.util.toSeason
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "MovieRepositoryImpl"

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val jellyfinDataSource: JellyfinDataSource,
    private val preferences: JellyfinPreferences,
) : MovieRepository {

    private val serverUrl: String
        get() = preferences.serverUrl ?: ""
    
    private val accessToken: String?
        get() = preferences.accessToken

    override fun getFeaturedMovies() = flow {
        // Get latest movies and shows (exclude episodes)
        var latestMovies = jellyfinDataSource.getLatestItems(limit = 5, includeItemTypes = "Movie")
        var latestShows = jellyfinDataSource.getLatestItems(limit = 5, includeItemTypes = "Series")
        
        // If latest is empty, get random movies and shows instead
        if (latestMovies.isEmpty() && latestShows.isEmpty()) {
            Log.d(TAG, "getFeaturedMovies: No latest items, fetching random movies and shows")
            val randomMoviesResult = jellyfinDataSource.getMovies(limit = 50)
            val randomShowsResult = jellyfinDataSource.getTVShows(limit = 50)
            latestMovies = randomMoviesResult.Items.shuffled().take(5)
            latestShows = randomShowsResult.Items.shuffled().take(5)
        } else {
            // If one list is empty, get random items for that type
            if (latestMovies.isEmpty()) {
                Log.d(TAG, "getFeaturedMovies: No latest movies, fetching random movies")
                val randomMoviesResult = jellyfinDataSource.getMovies(limit = 50)
                latestMovies = randomMoviesResult.Items.shuffled().take(5)
            }
            if (latestShows.isEmpty()) {
                Log.d(TAG, "getFeaturedMovies: No latest shows, fetching random shows")
                val randomShowsResult = jellyfinDataSource.getTVShows(limit = 50)
                latestShows = randomShowsResult.Items.shuffled().take(5)
            }
        }
        
        // Combine them and shuffle for randomness
        val allItems = (latestMovies + latestShows).shuffled().take(10)
        
        val movies = allItems.map { it.toMovie(serverUrl, ThumbnailType.Long, accessToken) }
        Log.d(TAG, "getFeaturedMovies: Emitting ${movies.size} featured movies/shows")
        emit(movies)
    }

    override fun getTrendingMovies(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getResumeItems(limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getTop10Movies(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getMovies(limit = 10)
        val movies = result.Items.take(10).map { it.toMovie(serverUrl, ThumbnailType.Long, accessToken) }
        emit(movies)
    }

    override fun getNowPlayingMovies(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getResumeItems(limit = 10)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getMovieCategories() = flow {
        Log.d(TAG, "getMovieCategories: Starting to fetch categories")
        val result = jellyfinDataSource.getLibraries()
        Log.d(TAG, "getMovieCategories: Got ${result.Items.size} items from data source")
        val categories = result.Items.map { category ->
            val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            val imageTag = category.ImageTags?.Primary ?: category.ImageTags?.Thumb
            val imageUrl = if (!imageTag.isNullOrBlank() && !category.Id.isBlank()) {
                "${normalizedServerUrl}Items/${category.Id}/Images/Primary?maxWidth=400&tag=$imageTag"
            } else {
                null
            }
            com.google.jetstream.data.entities.MovieCategory(
                id = category.Id,
                name = category.Name,
                imageUrl = imageUrl
            )
        }
        Log.d(TAG, "getMovieCategories: Emitting ${categories.size} categories")
        categories.forEachIndexed { index, cat ->
            Log.d(TAG, "getMovieCategories: Category[$index] - Id=${cat.id}, Name=${cat.name}")
        }
        emit(categories)
    }

    override suspend fun getMovieCategoryDetails(categoryId: String): MovieCategoryDetails {
        // Get all movies and shows from this library (no limit to show all items)
        val moviesResult = jellyfinDataSource.getMovies(parentId = categoryId, limit = 1000)
        val showsResult = jellyfinDataSource.getTVShows(parentId = categoryId, limit = 1000)
        
        // Combine all movies and shows
        val allItems = moviesResult.Items + showsResult.Items
        val movies = allItems.map { it.toMovie(serverUrl, accessToken = accessToken) }

        // Always get library name from categories list, not from first item
        val categories = getMovieCategories().first()
        val library = categories.find { it.id == categoryId }
        val category = com.google.jetstream.data.entities.MovieCategory(
            id = categoryId,
            name = library?.name ?: "Library"
        )

        return MovieCategoryDetails(
            id = category.id,
            name = category.name,
            movies = movies
        )
    }

    override suspend fun getMovieDetails(movieId: String): MovieDetails {
        // Check if still logged in before attempting to fetch
        if (!jellyfinDataSource.isLoggedIn()) {
            throw IllegalStateException("Not logged in")
        }
        
        val item = jellyfinDataSource.getItemById(movieId) ?: throw IllegalStateException("Item not found")
        
        val isTVShow = item.Type == "Series"
        
        // Get similar movies/shows (same parent/library)
        val similarMovies = if (!item.ParentId.isNullOrBlank()) {
            val similarResult = if (isTVShow) {
                jellyfinDataSource.getTVShows(parentId = item.ParentId, limit = 10)
            } else {
                jellyfinDataSource.getMovies(parentId = item.ParentId, limit = 10)
            }
            similarResult.Items
                .filter { it.Id != movieId }
                .take(4)
                .map { it.toMovie(serverUrl, accessToken = accessToken) }
        } else {
            emptyList()
        }

        // Get cast from People
        val castList = item.People?.map { person ->
            person.toMovieCast(serverUrl, movieId, accessToken)
        } ?: emptyList()

        // For TV shows, get seasons and episodes
        val seasons = if (isTVShow) {
            val seasonsResult = jellyfinDataSource.getSeasons(movieId, limit = 100)
            seasonsResult.Items.map { it.toSeason(serverUrl, accessToken) }
        } else {
            emptyList()
        }
        
        val episodes = if (isTVShow) {
            // Get all episodes for the series, grouped by season
            val episodesResult = jellyfinDataSource.getEpisodes(movieId, limit = 1000)
            // Create a map of season IDs to season index numbers
            val seasonIndexMap = seasons.associate { it.id to it.indexNumber }
            // Get episodes for each season to properly set season numbers
            val allEpisodes = mutableListOf<Episode>()
            seasons.forEach { season ->
                val seasonEpisodesResult = jellyfinDataSource.getEpisodesBySeason(season.id, limit = 1000)
                val seasonEpisodes = seasonEpisodesResult.Items.map { episodeDto ->
                    val episode = episodeDto.toEpisode(serverUrl, accessToken)
                    episode.copy(seasonNumber = season.indexNumber)
                }
                allEpisodes.addAll(seasonEpisodes)
            }
            // If no seasons found but episodes exist, try to get them directly
            if (allEpisodes.isEmpty() && episodesResult.Items.isNotEmpty()) {
                episodesResult.Items.map { it.toEpisode(serverUrl, accessToken) }
            } else {
                allEpisodes
            }
        } else {
            emptyList()
        }

        val movieDetails = item.toMovieDetails(serverUrl, similarMovies, castList, accessToken)
        
        return movieDetails.copy(
            type = item.Type ?: "Movie",
            seasons = seasons,
            episodes = episodes
        )
    }

    override suspend fun searchMovies(query: String): MovieList {
        val result = jellyfinDataSource.searchItems(query, limit = 50)
        return result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
    }

    override fun getMoviesWithLongThumbnail() = flow {
        val result = jellyfinDataSource.getMovies(limit = 50)
        val movies = result.Items.map { it.toMovie(serverUrl, ThumbnailType.Long, accessToken) }
        emit(movies)
    }

    override fun getMovies(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getMovies(limit = 50)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getPopularFilmsThisWeek(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getMovies(limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getTVShows(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getTVShows(limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, ThumbnailType.Long, accessToken) }
        emit(movies)
    }

    override fun getBingeWatchDramas(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getTVShows(limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getFavouriteMovies(): Flow<MovieList> = flow {
        val result = jellyfinDataSource.getFavoriteItems(limit = 50)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        emit(movies)
    }

    override fun getContinueWatching(): Flow<MovieList> = flow {
        Log.d(TAG, "getContinueWatching: Fetching resume items")
        val result = jellyfinDataSource.getResumeItems(limit = 20)
        // Use Long thumbnails (backdrops) for horizontal row display
        val movies = result.Items.map { it.toMovie(serverUrl, ThumbnailType.Long, accessToken) }
        Log.d(TAG, "getContinueWatching: Found ${movies.size} resume items")
        emit(movies)
    }

    override suspend fun getRecentlyAddedMovies(libraryId: String): MovieList {
        Log.d(TAG, "getRecentlyAddedMovies: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getRecentlyAddedMovies(libraryId, limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getRecentlyAddedMovies: Converted ${result.Items.size} items to ${movies.size} movies for libraryId=$libraryId")
        if (movies.isNotEmpty()) {
            Log.d(TAG, "getRecentlyAddedMovies: First movie posterUri=${movies.first().posterUri.take(100)}")
        }
        return movies
    }

    override suspend fun getRecentlyReleasedMovies(libraryId: String): MovieList {
        Log.d(TAG, "getRecentlyReleasedMovies: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getRecentlyReleasedMovies(libraryId, limit = 20)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getRecentlyReleasedMovies: Converted ${result.Items.size} items to ${movies.size} movies for libraryId=$libraryId")
        if (movies.isNotEmpty()) {
            Log.d(TAG, "getRecentlyReleasedMovies: First movie posterUri=${movies.first().posterUri.take(100)}")
        }
        return movies
    }

    override suspend fun getRecentlyAddedShows(libraryId: String): MovieList {
        Log.d(TAG, "getRecentlyAddedShows: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getRecentlyAddedShows(libraryId, limit = 20)
        val shows = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getRecentlyAddedShows: Converted ${result.Items.size} items to ${shows.size} shows for libraryId=$libraryId")
        if (shows.isNotEmpty()) {
            Log.d(TAG, "getRecentlyAddedShows: First show posterUri=${shows.first().posterUri.take(100)}")
        }
        return shows
    }

    override suspend fun getRecentlyAddedEpisodes(libraryId: String): MovieList {
        Log.d(TAG, "getRecentlyAddedEpisodes: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getRecentlyAddedEpisodes(libraryId, limit = 20)
        // Convert episodes to Movie objects using backdrop images for horizontal cards
        val episodes = result.Items.map { it.toMovieFromEpisode(serverUrl, accessToken = accessToken, useBackdrop = true) }
        Log.d(TAG, "getRecentlyAddedEpisodes: Converted ${result.Items.size} items to ${episodes.size} episodes for libraryId=$libraryId")
        if (episodes.isNotEmpty()) {
            Log.d(TAG, "getRecentlyAddedEpisodes: First episode posterUri=${episodes.first().posterUri.take(100)}")
        }
        return episodes
    }

    override suspend fun getMoviesByLibrary(libraryId: String, limit: Int): MovieList {
        Log.d(TAG, "getMoviesByLibrary: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getMovies(parentId = libraryId, limit = limit)
        val movies = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getMoviesByLibrary: Found ${movies.size} movies for libraryId=$libraryId")
        return movies
    }

    override suspend fun getTVShowsByLibrary(libraryId: String, limit: Int): MovieList {
        Log.d(TAG, "getTVShowsByLibrary: Fetching for libraryId=$libraryId")
        val result = jellyfinDataSource.getTVShows(parentId = libraryId, limit = limit)
        val shows = result.Items.map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getTVShowsByLibrary: Found ${shows.size} shows for libraryId=$libraryId")
        return shows
    }

    override suspend fun getMoviesByGenre(genre: String, limit: Int): MovieList {
        Log.d(TAG, "getMoviesByGenre: Fetching movies for genre=$genre")
        // Fetch a larger set of movies to filter by genre
        val result = jellyfinDataSource.getMovies(limit = 500)
        val movies = result.Items
            .filter { it.Genres?.any { g -> g.equals(genre, ignoreCase = true) } == true }
            .take(limit)
            .map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getMoviesByGenre: Found ${movies.size} movies for genre=$genre")
        return movies
    }

    override suspend fun getTVShowsByGenre(genre: String, limit: Int): MovieList {
        Log.d(TAG, "getTVShowsByGenre: Fetching shows for genre=$genre")
        // Fetch a larger set of shows to filter by genre
        val result = jellyfinDataSource.getTVShows(limit = 500)
        val shows = result.Items
            .filter { it.Genres?.any { g -> g.equals(genre, ignoreCase = true) } == true }
            .take(limit)
            .map { it.toMovie(serverUrl, accessToken = accessToken) }
        Log.d(TAG, "getTVShowsByGenre: Found ${shows.size} shows for genre=$genre")
        return shows
    }

    override fun getRecentlyAddedEpisodes(): Flow<MovieList> = flow {
        Log.d(TAG, "getRecentlyAddedEpisodes: Fetching recently added episodes")
        val result = jellyfinDataSource.getRecentlyAddedEpisodes(limit = 20)
        // For Shows screen, use backdrop images for horizontal cards
        val movies = result.Items.map { it.toMovieFromEpisode(serverUrl, accessToken = accessToken, useBackdrop = true) }
        Log.d(TAG, "getRecentlyAddedEpisodes: Converted ${result.Items.size} episodes to ${movies.size} movies")
        emit(movies)
    }

    override suspend fun getSubtitleTracks(movieId: String): List<SubtitleTrack> {
        Log.d(TAG, "getSubtitleTracks: Fetching subtitles for movieId=$movieId")
        val item = jellyfinDataSource.getItemById(movieId) ?: return emptyList()
        val normalizedServerUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

        // Collect subtitle streams from both MediaStreams (direct) and MediaSources
        val subtitleStreams = mutableListOf<com.google.jetstream.data.models.jellyfin.MediaStream>()
        
        // First, check direct MediaStreams on BaseItemDto
        item.MediaStreams?.filter { it.Type == "Subtitle" }?.let { subtitleStreams.addAll(it) }
        
        // Then, check MediaStreams within MediaSources
        item.MediaSources?.forEach { mediaSource ->
            mediaSource.MediaStreams?.filter { it.Type == "Subtitle" }?.let { subtitleStreams.addAll(it) }
        }

        return subtitleStreams.map { stream ->
            val index = stream.Index ?: 0
            SubtitleTrack(
                index = index,
                language = stream.Language,
                displayTitle = stream.DisplayTitle,
                codec = stream.Codec,
                isDefault = stream.IsDefault,
                isExternal = stream.IsExternal,
                url = "${normalizedServerUrl}Videos/$movieId/Subtitles/$index/Stream"
            )
        }
    }
}
