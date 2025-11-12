package com.example.starstack.firebase

import android.util.Log
import com.example.starstack.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Collections
    private val usersCollection = db.collection("users")
    private val moviesCollection = db.collection("movies")
    private val reviewsCollection = db.collection("reviews")
    private val ratingsCollection = db.collection("ratings")
    private val watchlistCollection = db.collection("watchlist")

    companion object {
        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager().also { instance = it }
            }
        }
    }

    // ===== Authentication =====

    fun getCurrentUser() = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.let { user ->
            val userData = User(
                uid = user.uid,
                email = email,
                displayName = displayName,
                joinedDate = Timestamp.now()
            )
            usersCollection.document(user.uid).set(userData).await()
        }
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()

        result.user?.let { user ->
            // Check if user document exists
            val userDoc = usersCollection.document(user.uid).get().await()
            if (!userDoc.exists()) {
                val userData = User(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    joinedDate = Timestamp.now()
                )
                usersCollection.document(user.uid).set(userData).await()
            }
        }
    }

    fun signOut() = auth.signOut()

    // ===== User Operations =====

    suspend fun getUserData(uid: String): User? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) {
                // Create user document if it doesn't exist
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.uid == uid) {
                    val userData = User(
                        uid = uid,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName ?: "",
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        joinedDate = Timestamp.now()
                    )
                    usersCollection.document(uid).set(userData).await()
                    return userData
                }
            }
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error getting user data", e)
            null
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        try {
            usersCollection.document(uid).update(updates).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating profile", e)
        }
    }

    // ===== Movie Operations =====

    suspend fun getAllMovies(): List<Movie> {
        return try {
            moviesCollection.get().await().toObjects(Movie::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieById(movieId: String): Movie? {
        return try {
            moviesCollection.document(movieId).get().await().toObject(Movie::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        return try {
            val allMovies = getAllMovies()
            allMovies.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.director.contains(query, ignoreCase = true) ||
                        it.genre.any { genre -> genre.contains(query, ignoreCase = true) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== Review Operations =====

    suspend fun addReview(review: Review): String {
        return try {
            val docRef = reviewsCollection.document()
            val reviewWithId = review.copy(id = docRef.id)
            docRef.set(reviewWithId).await()

            Log.d("FirebaseManager", "Review added with ID: ${docRef.id}")

            // Update user's review count - ensure user doc exists first
            getCurrentUser()?.uid?.let { uid ->
                try {
                    // Get current user data
                    val userDoc = usersCollection.document(uid).get().await()
                    if (userDoc.exists()) {
                        usersCollection.document(uid).update(
                            "totalReviews", com.google.firebase.firestore.FieldValue.increment(1)
                        ).await()
                    } else {
                        // Create user document if it doesn't exist
                        getUserData(uid)
                        usersCollection.document(uid).update(
                            "totalReviews", 1
                        ).await()
                    }
                    Log.d("FirebaseManager", "User review count updated")
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error updating user review count", e)
                }
            }

            docRef.id
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error adding review", e)
            throw e
        }
    }

    suspend fun getMovieReviews(movieId: String): List<Review> {
        return try {
            Log.d("FirebaseManager", "Fetching reviews for movie: $movieId")
            val reviews = reviewsCollection
                .whereEqualTo("movieId", movieId)
                .get()
                .await()
                .toObjects(Review::class.java)
                .sortedByDescending { it.timestamp.toDate() }

            Log.d("FirebaseManager", "Found ${reviews.size} reviews")
            reviews
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error loading reviews", e)
            emptyList()
        }
    }

    suspend fun getUserReviews(userId: String): List<Review> {
        return try {
            Log.d("FirebaseManager", "Fetching reviews for user: $userId")
            val reviews = reviewsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Review::class.java)
                .sortedByDescending { it.timestamp.toDate() }

            Log.d("FirebaseManager", "Found ${reviews.size} user reviews")
            reviews
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error loading user reviews", e)
            emptyList()
        }
    }

    suspend fun deleteReview(reviewId: String, movieId: String) {
        try {
            reviewsCollection.document(reviewId).delete().await()

            getCurrentUser()?.uid?.let { uid ->
                try {
                    usersCollection.document(uid).update(
                        "totalReviews", com.google.firebase.firestore.FieldValue.increment(-1)
                    ).await()
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error updating review count", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting review", e)
            throw e
        }
    }

    suspend fun likeReview(reviewId: String, userId: String) {
        try {
            val reviewRef = reviewsCollection.document(reviewId)
            reviewRef.update(
                mapOf(
                    "likes" to com.google.firebase.firestore.FieldValue.increment(1),
                    "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                )
            ).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error liking review", e)
        }
    }

    suspend fun unlikeReview(reviewId: String, userId: String) {
        try {
            val reviewRef = reviewsCollection.document(reviewId)
            reviewRef.update(
                mapOf(
                    "likes" to com.google.firebase.firestore.FieldValue.increment(-1),
                    "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(userId)
                )
            ).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error unliking review", e)
        }
    }

    // ===== Rating Operations =====

    suspend fun addOrUpdateRating(movieId: String, rating: Double) {
        val userId = getCurrentUser()?.uid ?: return
        val ratingId = "${userId}_${movieId}"

        try {
            val userRating = UserRating(
                userId = userId,
                movieId = movieId,
                rating = rating,
                timestamp = Timestamp.now()
            )

            ratingsCollection.document(ratingId).set(userRating).await()
            Log.d("FirebaseManager", "Rating saved: $rating for movie: $movieId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving rating", e)
        }
    }

    suspend fun getUserRating(movieId: String): Double? {
        val userId = getCurrentUser()?.uid ?: return null
        val ratingId = "${userId}_${movieId}"

        return try {
            val doc = ratingsCollection.document(ratingId).get().await()
            doc.toObject(UserRating::class.java)?.rating
        } catch (e: Exception) {
            null
        }
    }

    // ===== Watchlist Operations =====

    suspend fun addToWatchlist(movieId: String): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        val itemId = "${userId}_${movieId}"

        return try {
            val item = WatchlistItem(
                id = itemId,
                userId = userId,
                movieId = movieId,
                addedDate = Timestamp.now()
            )
            watchlistCollection.document(itemId).set(item).await()
            Log.d("FirebaseManager", "Added to watchlist: $movieId")

            // Update user's watchlist count
            try {
                val userDoc = usersCollection.document(userId).get().await()
                if (userDoc.exists()) {
                    usersCollection.document(userId).update(
                        "watchlistCount", com.google.firebase.firestore.FieldValue.increment(1)
                    ).await()
                } else {
                    getUserData(userId)
                    usersCollection.document(userId).update(
                        "watchlistCount", 1
                    ).await()
                }
                Log.d("FirebaseManager", "User watchlist count updated")
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error updating watchlist count", e)
            }

            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error adding to watchlist", e)
            false
        }
    }

    suspend fun removeFromWatchlist(movieId: String): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        val itemId = "${userId}_${movieId}"

        return try {
            watchlistCollection.document(itemId).delete().await()
            Log.d("FirebaseManager", "Removed from watchlist: $movieId")

            try {
                usersCollection.document(userId).update(
                    "watchlistCount", com.google.firebase.firestore.FieldValue.increment(-1)
                ).await()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error updating watchlist count", e)
            }

            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error removing from watchlist", e)
            false
        }
    }

    suspend fun isInWatchlist(movieId: String): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        val itemId = "${userId}_${movieId}"

        return try {
            val exists = watchlistCollection.document(itemId).get().await().exists()
            Log.d("FirebaseManager", "Watchlist check for $movieId: $exists")
            exists
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error checking watchlist", e)
            false
        }
    }

    suspend fun getWatchlist(): List<String> {
        val userId = getCurrentUser()?.uid ?: return emptyList()

        return try {
            Log.d("FirebaseManager", "Fetching watchlist for user: $userId")
            val items = watchlistCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(WatchlistItem::class.java)
                .sortedByDescending { it.addedDate.toDate() }
                .map { it.movieId }

            Log.d("FirebaseManager", "Found ${items.size} items in watchlist")
            items
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error loading watchlist", e)
            emptyList()
        }
    }
}