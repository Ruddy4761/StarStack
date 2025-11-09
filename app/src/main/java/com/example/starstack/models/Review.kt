package com.example.starstack.models

import com.google.firebase.Timestamp

// Review model
data class Review(
    val id: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val moviePosterUrl: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val rating: Double = 0.0,
    val reviewText: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList()
)