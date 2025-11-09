package com.example.starstack.models

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val joinedDate: Timestamp = Timestamp.now(),
    val totalReviews: Int = 0,
    val watchlistCount: Int = 0
)