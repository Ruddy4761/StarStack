package com.example.starstack.models

import com.google.firebase.Timestamp

// User Rating (simplified for quick access)
data class UserRating(
    val userId: String = "",
    val movieId: String = "",
    val rating: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
)