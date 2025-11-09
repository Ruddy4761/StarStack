package com.example.starstack.firebase


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
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>) {
        usersCollection.document(uid).update(updates).await()
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
        val docRef = reviewsCollection.document()
        val reviewWithId = review.copy(id = docRef.id)
        docRef.set(reviewWithId).await()

        // Update movie's average rating and review count
        updateMovieRating(review.movieId)

        // Update user's review count
        getCurrentUser()?.uid?.let { uid ->
            usersCollection.document(uid).update(
                "totalReviews", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
        }

        return docRef.id
    }

    suspend fun getMovieReviews(movieId: String): List<Review> {
        return try {
            reviewsCollection
                .whereEqualTo("movieId", movieId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserReviews(userId: String): List<Review> {
        return try {
            reviewsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteReview(reviewId: String, movieId: String) {
        reviewsCollection.document(reviewId).delete().await()
        updateMovieRating(movieId)

        getCurrentUser()?.uid?.let { uid ->
            usersCollection.document(uid).update(
                "totalReviews", com.google.firebase.firestore.FieldValue.increment(-1)
            ).await()
        }
    }

    suspend fun likeReview(reviewId: String, userId: String) {
        val reviewRef = reviewsCollection.document(reviewId)
        reviewRef.update(
            "likes", com.google.firebase.firestore.FieldValue.increment(1),
            "likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId)
        ).await()
    }

    suspend fun unlikeReview(reviewId: String, userId: String) {
        val reviewRef = reviewsCollection.document(reviewId)
        reviewRef.update(
            "likes", com.google.firebase.firestore.FieldValue.increment(-1),
            "likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(userId)
        ).await()
    }

    // ===== Rating Operations =====

    suspend fun addOrUpdateRating(movieId: String, rating: Double) {
        val userId = getCurrentUser()?.uid ?: return
        val ratingId = "${userId}_${movieId}"

        val userRating = UserRating(
            userId = userId,
            movieId = movieId,
            rating = rating,
            timestamp = Timestamp.now()
        )

        ratingsCollection.document(ratingId).set(userRating).await()
        updateMovieRating(movieId)
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

    private suspend fun updateMovieRating(movieId: String) {
        try {
            // Get all ratings for this movie
            val ratings = ratingsCollection
                .whereEqualTo("movieId", movieId)
                .get()
                .await()
                .toObjects(UserRating::class.java)

            // Get all reviews for this movie
            val reviews = reviewsCollection
                .whereEqualTo("movieId", movieId)
                .get()
                .await()
                .toObjects(Review::class.java)

            // Combine ratings from both sources
            val allRatings = ratings.map { it.rating } + reviews.map { it.rating }

            if (allRatings.isNotEmpty()) {
                val avgRating = allRatings.average()
                moviesCollection.document(movieId).update(
                    mapOf(
                        "averageRating" to avgRating,
                        "totalReviews" to reviews.size
                    )
                ).await()
            }
        } catch (e: Exception) {
            // Handle error
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

            usersCollection.document(userId).update(
                "watchlistCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFromWatchlist(movieId: String): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        val itemId = "${userId}_${movieId}"

        return try {
            watchlistCollection.document(itemId).delete().await()

            usersCollection.document(userId).update(
                "watchlistCount", com.google.firebase.firestore.FieldValue.increment(-1)
            ).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isInWatchlist(movieId: String): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        val itemId = "${userId}_${movieId}"

        return try {
            watchlistCollection.document(itemId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWatchlist(): List<String> {
        val userId = getCurrentUser()?.uid ?: return emptyList()

        return try {
            watchlistCollection
                .whereEqualTo("userId", userId)
                .orderBy("addedDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WatchlistItem::class.java)
                .map { it.movieId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== Initialize Sample Data =====

    suspend fun initializeSampleMovies() {
        // Check if movies already exist
        val existingMovies = moviesCollection.limit(1).get().await()
        if (!existingMovies.isEmpty) return

        val sampleMovies = listOf(
            Movie(
                id = "inception",
                title = "Inception",
                year = 2010,
                director = "Christopher Nolan",
                cast = listOf("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Ellen Page"),
                genre = listOf("Sci-Fi", "Thriller", "Action"),
                duration = 148,
                description = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
                releaseDate = "July 16, 2010",
                averageRating = 4.8
            ),
            Movie(
                id = "interstellar",
                title = "Interstellar",
                year = 2014,
                director = "Christopher Nolan",
                cast = listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
                genre = listOf("Sci-Fi", "Drama", "Adventure"),
                duration = 169,
                description = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
                releaseDate = "November 7, 2014",
                averageRating = 4.7
            ),
            Movie(
                id = "dark_knight",
                title = "The Dark Knight",
                year = 2008,
                director = "Christopher Nolan",
                cast = listOf("Christian Bale", "Heath Ledger", "Aaron Eckhart"),
                genre = listOf("Action", "Crime", "Drama"),
                duration = 152,
                description = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests.",
                releaseDate = "July 18, 2008",
                averageRating = 4.9
            ),
            Movie(
                id = "godfather",
                title = "The Godfather",
                year = 1972,
                director = "Francis Ford Coppola",
                cast = listOf("Marlon Brando", "Al Pacino", "James Caan"),
                genre = listOf("Crime", "Drama"),
                duration = 175,
                description = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.",
                releaseDate = "March 24, 1972",
                averageRating = 5.0
            ),
            Movie(
                id = "shawshank",
                title = "The Shawshank Redemption",
                year = 1994,
                director = "Frank Darabont",
                cast = listOf("Tim Robbins", "Morgan Freeman", "Bob Gunton"),
                genre = listOf("Drama"),
                duration = 142,
                description = "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
                releaseDate = "September 23, 1994",
                averageRating = 4.9
            )
        )

        sampleMovies.forEach { movie ->
            moviesCollection.document(movie.id).set(movie).await()
        }
    }
}