package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.starstack.adapters.ReviewAdapter
import com.example.starstack.databinding.ActivityMovieDetailsBinding
import com.example.starstack.firebase.FirebaseManager
import com.example.starstack.models.Movie
import com.example.starstack.models.Review
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailsBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var movie: Movie
    private lateinit var reviewAdapter: ReviewAdapter
    private var userRating: Double? = null
    private var isInWatchlist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movie = intent.getParcelableExtra("movie") ?: run {
            finish()
            return
        }

        setupToolbar()
        setupReviewRecyclerView()
        displayMovieDetails()
        loadReviews()
        checkWatchlistStatus()
        loadUserRating()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = movie.title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupReviewRecyclerView() {
        reviewAdapter = ReviewAdapter(
            onLikeClick = { review -> toggleLike(review) },
            onDeleteClick = { review -> deleteReview(review) },
            currentUserId = firebaseManager.getCurrentUser()?.uid
        )

        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MovieDetailActivity)
            adapter = reviewAdapter
        }
    }

    private fun displayMovieDetails() {
        binding.apply {
            tvTitle.text = movie.title
            tvYear.text = movie.year.toString()
            tvDirector.text = "Directed by ${movie.director}"
            tvDuration.text = "${movie.duration} min"
            tvGenres.text = movie.genre.joinToString(" • ")
            tvDescription.text = movie.description
            tvCast.text = movie.cast.joinToString(", ")
            tvRating.text = String.format("%.1f", movie.averageRating)
            tvReviewCount.text = "(${movie.totalReviews} reviews)"

            // Set rating stars
            ratingBar.rating = movie.averageRating.toFloat()
        }
    }

    private fun setupListeners() {
        binding.fabAddReview.setOnClickListener {
            if (firebaseManager.getCurrentUser() == null) {
                Toast.makeText(this, "Please sign in to write a review", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                showReviewDialog()
            }
        }

        binding.btnWatchlist.setOnClickListener {
            toggleWatchlist()
        }

        binding.btnRate.setOnClickListener {
            if (firebaseManager.getCurrentUser() == null) {
                Toast.makeText(this, "Please sign in to rate", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                showRatingDialog()
            }
        }
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            try {
                val reviews = firebaseManager.getMovieReviews(movie.id)
                reviewAdapter.submitList(reviews)

                binding.tvNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(
                    this@MovieDetailActivity,
                    "Error loading reviews: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadUserRating() {
        if (firebaseManager.getCurrentUser() == null) return

        lifecycleScope.launch {
            try {
                userRating = firebaseManager.getUserRating(movie.id)
                userRating?.let { rating ->
                    binding.btnRate.text = "Your Rating: ⭐ ${String.format("%.1f", rating)}"
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun checkWatchlistStatus() {
        if (firebaseManager.getCurrentUser() == null) return

        lifecycleScope.launch {
            try {
                isInWatchlist = firebaseManager.isInWatchlist(movie.id)
                updateWatchlistButton()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun updateWatchlistButton() {
        binding.btnWatchlist.text = if (isInWatchlist) "✓ In Watchlist" else "+ Add to Watchlist"
    }

    private fun showRatingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rating, null)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.dialogRatingBar)

        userRating?.let { ratingBar.rating = it.toFloat() }

        AlertDialog.Builder(this)
            .setTitle("Rate ${movie.title}")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating.toDouble()
                submitRating(rating)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitRating(rating: Double) {
        lifecycleScope.launch {
            try {
                firebaseManager.addOrUpdateRating(movie.id, rating)
                userRating = rating
                binding.btnRate.text = "Your Rating: ⭐ ${String.format("%.1f", rating)}"
                Toast.makeText(this@MovieDetailActivity, "Rating submitted!", Toast.LENGTH_SHORT).show()

                // Refresh movie details to show updated average
                val updatedMovie = firebaseManager.getMovieById(movie.id)
                updatedMovie?.let {
                    movie = it
                    displayMovieDetails()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MovieDetailActivity,
                    "Error submitting rating: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showReviewDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.dialogRatingBar)
        val reviewText = dialogView.findViewById<android.widget.EditText>(R.id.etReviewText)

        AlertDialog.Builder(this)
            .setTitle("Write Review for ${movie.title}")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating.toDouble()
                val text = reviewText.text.toString().trim()

                if (rating > 0 && text.isNotEmpty()) {
                    submitReview(rating, text)
                } else {
                    Toast.makeText(this, "Please provide both rating and review", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitReview(rating: Double, reviewText: String) {
        val user = firebaseManager.getCurrentUser() ?: return

        lifecycleScope.launch {
            try {
                val userData = firebaseManager.getUserData(user.uid)

                val review = Review(
                    movieId = movie.id,
                    movieTitle = movie.title,
                    userId = user.uid,
                    userName = userData?.displayName ?: user.displayName ?: "Anonymous",
                    userPhotoUrl = userData?.photoUrl ?: user.photoUrl?.toString() ?: "",
                    rating = rating,
                    reviewText = reviewText,
                    timestamp = Timestamp.now()
                )

                firebaseManager.addReview(review)
                Toast.makeText(this@MovieDetailActivity, "Review submitted!", Toast.LENGTH_SHORT).show()
                loadReviews()

                // Refresh movie details
                val updatedMovie = firebaseManager.getMovieById(movie.id)
                updatedMovie?.let {
                    movie = it
                    displayMovieDetails()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MovieDetailActivity,
                    "Error submitting review: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun toggleLike(review: Review) {
        val currentUser = firebaseManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to like reviews", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                if (review.likedBy.contains(currentUser.uid)) {
                    firebaseManager.unlikeReview(review.id, currentUser.uid)
                } else {
                    firebaseManager.likeReview(review.id, currentUser.uid)
                }
                loadReviews()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MovieDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteReview(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        firebaseManager.deleteReview(review.id, movie.id)
                        Toast.makeText(this@MovieDetailActivity, "Review deleted", Toast.LENGTH_SHORT).show()
                        loadReviews()

                        // Refresh movie details
                        val updatedMovie = firebaseManager.getMovieById(movie.id)
                        updatedMovie?.let {
                            movie = it
                            displayMovieDetails()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MovieDetailActivity,
                            "Error deleting review: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleWatchlist() {
        if (firebaseManager.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to use watchlist", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        lifecycleScope.launch {
            try {
                val success = if (isInWatchlist) {
                    firebaseManager.removeFromWatchlist(movie.id)
                } else {
                    firebaseManager.addToWatchlist(movie.id)
                }

                if (success) {
                    isInWatchlist = !isInWatchlist
                    updateWatchlistButton()
                    Toast.makeText(
                        this@MovieDetailActivity,
                        if (isInWatchlist) "Added to watchlist" else "Removed from watchlist",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MovieDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
