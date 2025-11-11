package com.example.starstack

import android.content.Intent
import android.os.Bundle
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

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private val movieRepository = OMDbMovieRepository()
    private lateinit var adapter: MovieGridAdapter

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
        showLoading(true)

        lifecycleScope.launch {
            try {
                val watchlistMovieIds = firebaseManager.getWatchlist()

                if (watchlistMovieIds.isEmpty()) {
                    showEmpty(true)
                    showLoading(false)
                    return@launch
                }

                val movies = mutableListOf<Movie>()

                // Fetch details for each movie in watchlist
                watchlistMovieIds.forEach { movieId ->
                    val details = movieRepository.getMovieDetails(movieId)
                    if (details != null) {
                        movies.add(movieRepository.movieDetailsToMovie(details))
                    }
                }

                adapter.submitList(movies)
                showEmpty(movies.isEmpty())
            } catch (e: Exception) {
                Toast.makeText(
                    this@WatchlistActivity,
                    "Error loading watchlist: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyView.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadWatchlist()
    }
}