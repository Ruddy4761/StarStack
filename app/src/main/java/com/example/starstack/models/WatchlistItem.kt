package com.example.starstack.models

import com.google.firebase.Timestamp

// Watchlist item
data class WatchlistItem(
    val id: String = "",
    val userId: String = "",
    val movieId: String = "",
    val addedDate: Timestamp = Timestamp.now()
)