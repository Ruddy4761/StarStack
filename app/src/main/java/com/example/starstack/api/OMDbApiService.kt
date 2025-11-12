package com.example.starstack.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OMDbApiService {

    // Search movies by title
    @GET("/")
    suspend fun searchMovies(
        @Query("apikey") apiKey: String,
        @Query("s") searchQuery: String,
        @Query("type") type: String = "movie",
        @Query("page") page: Int = 1
    ): OMDbSearchResponse

    // Get movie details by ID or title
    @GET("/")
    suspend fun getMovieDetails(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String? = null,
        @Query("t") title: String? = null,
        @Query("plot") plot: String = "full"
    ): OMDbMovieDetails
}

// Search Response
data class OMDbSearchResponse(
    val Search: List<OMDbSearchResult>?,
    val totalResults: String?,
    val Response: String,
    val Error: String?
)

data class OMDbSearchResult(
    val Title: String,
    val Year: String,
    val imdbID: String,
    val Type: String,
    val Poster: String
)

// Detailed Movie Response
data class OMDbMovieDetails(
    val Title: String?,
    val Year: String?,
    val Rated: String?,
    val Released: String?,
    val Runtime: String?,
    val Genre: String?,
    val Director: String?,
    val Writer: String?,
    val Actors: String?,
    val Plot: String?,
    val Language: String?,
    val Country: String?,
    val Awards: String?,
    val Poster: String?,
    val Ratings: List<Rating>?,
    val Metascore: String?,
    val imdbRating: String?,
    val imdbVotes: String?,
    val imdbID: String?,
    val Type: String?,
    val DVD: String?,
    val BoxOffice: String?,
    val Production: String?,
    val Website: String?,
    val Response: String,
    val Error: String?
)

data class Rating(
    val Source: String,
    val Value: String
)