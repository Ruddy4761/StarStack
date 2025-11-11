package com.example.starstack.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OMDbRetrofitClient {

    private const val BASE_URL = "https://www.omdbapi.com/"

    // TODO: Add your OMDb API key here
    // Get it from: https://www.omdbapi.com/apikey.aspx
    const val API_KEY = "9e814232"

    // Default poster when not available
    const val DEFAULT_POSTER = "https://via.placeholder.com/300x450/1a1a1a/ffffff?text=No+Poster"

    fun getPosterUrl(posterUrl: String?): String {
        return if (posterUrl.isNullOrEmpty() || posterUrl == "N/A") {
            DEFAULT_POSTER
        } else {
            posterUrl
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: OMDbApiService = retrofit.create(OMDbApiService::class.java)
}