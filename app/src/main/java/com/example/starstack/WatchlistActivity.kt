package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.starstack.adapters.MovieGridAdapter
import com.example.starstack.databinding.ActivityWatchlistBinding
import com.example.starstack.firebase.FirebaseManager
import com.example.starstack.models.Movie
import com.example.starstack.repository.OMDbMovieRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private val movieRepository = OMDbMovieRepository()
    private lateinit var adapter: MovieGridAdapter
    private val TAG = "WatchlistActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadWatchlist()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Watchlist"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = MovieGridAdapter { movie ->
            val intent = Intent(this, MovieDetailActivity::class.java)
            intent.putExtra("movie", movie)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
    }

    private fun loadWatchlist() {
        val currentUser = firebaseManager.getCurrentUser()
        if (currentUser == null) {
            showEmpty(true)
            binding.emptyView.findViewById<android.widget.TextView>(
                com.example.starstack.R.id.emptyView
            )?.text = "Please sign in to view your watchlist"
            return
        }

        showLoading(true)
        Log.d(TAG, "Loading watchlist for user: ${currentUser.uid}")

        lifecycleScope.launch {
            try {
                val watchlistMovieIds = firebaseManager.getWatchlist()
                Log.d(TAG, "Found ${watchlistMovieIds.size} movies in watchlist")

                if (watchlistMovieIds.isEmpty()) {
                    showEmpty(true)
                    showLoading(false)
                    return@launch
                }

                val movies = mutableListOf<Movie>()

                // Fetch details for each movie in watchlist
                watchlistMovieIds.forEachIndexed { index, movieId ->
                    Log.d(TAG, "Fetching details for movie ${index + 1}/${watchlistMovieIds.size}: $movieId")
                    try {
                        // Use withContext to switch to IO dispatcher for network call
                        val details = withContext(Dispatchers.IO) {
                            movieRepository.getMovieDetails(movieId)
                        }
                        if (details != null) {
                            val movie = movieRepository.movieDetailsToMovie(details)
                            movies.add(movie)
                            Log.d(TAG, "Successfully loaded: ${movie.title}")
                        } else {
                            Log.w(TAG, "No details found for movieId: $movieId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading movie $movieId: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Loaded ${movies.size} movies total")

                if (movies.isEmpty()) {
                    showEmpty(true)
                    Toast.makeText(
                        this@WatchlistActivity,
                        "Could not load movie details. Please check your connection.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    adapter.submitList(movies)
                    showEmpty(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading watchlist", e)
                Toast.makeText(
                    this@WatchlistActivity,
                    "Error loading watchlist: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmpty(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyView.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadWatchlist()
    }
}