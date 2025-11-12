package com.example.starstack.repository

import android.util.Log
import com.example.starstack.api.OMDbMovieDetails
import com.example.starstack.api.OMDbRetrofitClient
import com.example.starstack.api.OMDbSearchResult
import com.example.starstack.models.Movie

class OMDbMovieRepository {

    private val api = OMDbRetrofitClient.api
    private val apiKey = OMDbRetrofitClient.API_KEY

    // Popular movie titles to show on home screen
    private val popularMovieTitles = listOf(
        "Inception", "Interstellar", "The Dark Knight", "The Matrix",
        "Pulp Fiction", "The Shawshank Redemption", "Forrest Gump",
        "The Godfather", "Titanic", "Avatar", "Avengers", "Iron Man",
        "Spider-Man", "Batman", "Superman", "Star Wars", "Jurassic Park",
        "The Lion King", "Toy Story", "Finding Nemo", "Harry Potter",
        "Lord of the Rings", "Gladiator", "The Prestige", "Fight Club"
    )

    suspend fun getPopularMovies(): List<OMDbSearchResult> {
        val movies = mutableListOf<OMDbSearchResult>()

        // Search for each popular movie
        popularMovieTitles.forEach { title ->
            try {
                val response = api.searchMovies(apiKey, title, page = 1)
                if (response.Response == "True" && !response.Search.isNullOrEmpty()) {
                    movies.add(response.Search[0]) // Add first result
                }
            } catch (e: Exception) {
                Log.e("OMDbRepository", "Error fetching $title", e)
            }
        }

        return movies
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<OMDbSearchResult> {
        return try {
            val response = api.searchMovies(apiKey, query, page = page)
            if (response.Response == "True") {
                response.Search ?: emptyList()
            } else {
                Log.e("OMDbRepository", "Search error: ${response.Error}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("OMDbRepository", "Error searching movies", e)
            emptyList()
        }
    }

    suspend fun getMovieDetails(imdbId: String): OMDbMovieDetails? {
        return try {
            val details = api.getMovieDetails(apiKey, imdbId = imdbId)
            if (details.Response == "True") {
                details
            } else {
                Log.e("OMDbRepository", "Details error: ${details.Error}")
                null
            }
        } catch (e: Exception) {
            Log.e("OMDbRepository", "Error fetching details for $imdbId", e)
            null
        }
    }

    suspend fun getMovieDetailsByTitle(title: String): OMDbMovieDetails? {
        return try {
            val details = api.getMovieDetails(apiKey, title = title)
            if (details.Response == "True") {
                details
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("OMDbRepository", "Error fetching details by title", e)
            null
        }
    }

    // Convert OMDb search result to our Movie model (basic info)
    fun searchResultToMovie(result: OMDbSearchResult): Movie {
        return Movie(
            id = result.imdbID,
            title = result.Title,
            year = extractYear(result.Year),
            director = "",
            cast = emptyList(),
            genre = emptyList(),
            duration = 0,
            description = "",
            posterUrl = OMDbRetrofitClient.getPosterUrl(result.Poster),
            backdropUrl = OMDbRetrofitClient.getPosterUrl(result.Poster),
            averageRating = 0.0,
            totalReviews = 0,
            releaseDate = result.Year,
            language = "English"
        )
    }

    // Convert OMDb details to our Movie model (full info)
    fun movieDetailsToMovie(details: OMDbMovieDetails): Movie {
        val imdbRating = details.imdbRating?.toDoubleOrNull() ?: 0.0
        val normalizedRating = (imdbRating / 10.0) * 5.0 // Convert from 10 to 5 scale

        return Movie(
            id = details.imdbID ?: "",
            title = details.Title ?: "Unknown",
            year = extractYear(details.Year ?: ""),
            director = details.Director ?: "Unknown",
            cast = details.Actors?.split(", ") ?: emptyList(),
            genre = details.Genre?.split(", ") ?: emptyList(),
            duration = extractRuntime(details.Runtime ?: ""),
            description = details.Plot ?: "No description available",
            posterUrl = OMDbRetrofitClient.getPosterUrl(details.Poster),
            backdropUrl = OMDbRetrofitClient.getPosterUrl(details.Poster),
            averageRating = normalizedRating,
            totalReviews = 0,
            releaseDate = details.Released ?: "",
            language = details.Language ?: "English"
        )
    }

    private fun extractYear(yearString: String): Int {
        return try {
            // Handle formats like "2010", "2010-2015", etc.
            yearString.split("-", "–").firstOrNull()?.trim()?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun extractRuntime(runtimeString: String): Int {
        return try {
            // Handle "148 min" format
            runtimeString.replace(" min", "").trim().toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Get genres from movies (for filter)
    fun extractGenresFromMovies(movies: List<Movie>): Set<String> {
        return movies.flatMap { it.genre }.toSet()
    }
}