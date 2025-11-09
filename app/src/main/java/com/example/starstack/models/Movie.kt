package com.example.starstack.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: String = "",
    val title: String = "",
    val year: Int = 0,
    val director: String = "",
    val cast: List<String> = emptyList(),
    val genre: List<String> = emptyList(),
    val duration: Int = 0, // in minutes
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val releaseDate: String = "",
    val language: String = "English"
) : Parcelable
