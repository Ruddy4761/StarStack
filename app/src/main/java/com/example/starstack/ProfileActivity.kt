package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.starstack.adapters.ReviewAdapter
import com.example.starstack.databinding.ActivityProfileBinding
import com.example.starstack.firebase.FirebaseManager
import com.example.starstack.models.User
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var reviewAdapter: ReviewAdapter
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadUserProfile()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(
            onLikeClick = {},
            onDeleteClick = { review -> deleteReview(review) },
            currentUserId = firebaseManager.getCurrentUser()?.uid
        )

        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = reviewAdapter
        }
    }

    private fun loadUserProfile() {
        val currentUser = firebaseManager.getCurrentUser()
        if (currentUser == null) {
            showGuestView()
            return
        }

        Log.d(TAG, "Loading profile for user: ${currentUser.uid}")
        showLoading(true)

        lifecycleScope.launch {
            try {
                val userData = firebaseManager.getUserData(currentUser.uid)
                Log.d(TAG, "User data loaded: ${userData?.displayName}")
                displayUserData(userData)
                loadUserReviews(currentUser.uid)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Error loading profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showGuestView() {
        binding.apply {
            tvUserName.text = "Guest User"
            tvUserEmail.text = "Sign in to unlock features"
            tvReviewCount.text = "0 Reviews"
            tvWatchlistCount.text = "0 Movies"
            btnSignOut.text = "Sign In"
            reviewsRecyclerView.visibility = View.GONE
            tvNoReviews.visibility = View.VISIBLE
            tvNoReviews.text = "Sign in to write reviews"
        }
    }

    private fun displayUserData(user: User?) {
        if (user == null) {
            showGuestView()
            return
        }

        Log.d(TAG, "Displaying user data - Reviews: ${user.totalReviews}, Watchlist: ${user.watchlistCount}")

        binding.apply {
            tvUserName.text = user.displayName.ifEmpty { "User" }
            tvUserEmail.text = user.email
            tvReviewCount.text = "${user.totalReviews} Reviews"
            tvWatchlistCount.text = "${user.watchlistCount} Movies"

            if (user.bio.isNotEmpty()) {
                tvBio.text = user.bio
                tvBio.visibility = View.VISIBLE
            } else {
                tvBio.visibility = View.GONE
            }
        }
    }

    private fun loadUserReviews(userId: String) {
        Log.d(TAG, "Loading reviews for user: $userId")

        lifecycleScope.launch {
            try {
                val reviews = firebaseManager.getUserReviews(userId)
                Log.d(TAG, "Loaded ${reviews.size} reviews")

                reviewAdapter.submitList(reviews)

                if (reviews.isEmpty()) {
                    binding.tvNoReviews.visibility = View.VISIBLE
                    binding.tvNoReviews.text = "No reviews yet"
                    binding.reviewsRecyclerView.visibility = View.GONE
                } else {
                    binding.tvNoReviews.visibility = View.GONE
                    binding.reviewsRecyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Error loading reviews: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvNoReviews.visibility = View.VISIBLE
                binding.tvNoReviews.text = "Error loading reviews"
                binding.reviewsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.btnSignOut.setOnClickListener {
            if (firebaseManager.getCurrentUser() == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                showSignOutDialog()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            if (firebaseManager.getCurrentUser() == null) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            } else {
                showEditProfileDialog()
            }
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                firebaseManager.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etDisplayName = dialogView.findViewById<android.widget.EditText>(R.id.etDisplayName)
        val etBio = dialogView.findViewById<android.widget.EditText>(R.id.etBio)

        lifecycleScope.launch {
            val user = firebaseManager.getCurrentUser()?.uid?.let {
                firebaseManager.getUserData(it)
            }

            etDisplayName.setText(user?.displayName)
            etBio.setText(user?.bio)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etDisplayName.text.toString().trim()
                val newBio = etBio.text.toString().trim()
                updateProfile(newName, newBio)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProfile(displayName: String, bio: String) {
        val userId = firebaseManager.getCurrentUser()?.uid ?: return

        lifecycleScope.launch {
            try {
                val updates = mutableMapOf<String, Any>()
                if (displayName.isNotEmpty()) updates["displayName"] = displayName
                if (bio.isNotEmpty()) updates["bio"] = bio

                firebaseManager.updateUserProfile(userId, updates)
                Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserProfile()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Error updating profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteReview(review: com.example.starstack.models.Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        firebaseManager.deleteReview(review.id, review.movieId)
                        Toast.makeText(this@ProfileActivity, "Review deleted", Toast.LENGTH_SHORT).show()
                        firebaseManager.getCurrentUser()?.uid?.let { loadUserReviews(it) }
                        loadUserProfile()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting review", e)
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }
}