package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.starstack.adapters.MovieGridAdapter
import com.example.starstack.databinding.ActivityMainBinding
import com.example.starstack.firebase.FirebaseManager
import com.example.starstack.models.Movie
import com.example.starstack.repository.OMDbMovieRepository
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private val movieRepository = OMDbMovieRepository()
    private lateinit var adapter: MovieGridAdapter

    private var allMovies = listOf<Movie>()
    private var selectedGenres = mutableSetOf<String>()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "StarStack"

        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()

        loadPopularMovies()
    }

    private fun setupRecyclerView() {
        adapter = MovieGridAdapter { movie ->
            val intent = Intent(this, MovieDetailActivity::class.java)
            intent.putExtra("movie", movie)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun setupSearchBar() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                // Cancel previous search job
                searchJob?.cancel()

                if (query.length >= 3) {
                    // Debounce search - wait 500ms after user stops typing
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        searchMovies(query)
                    }
                } else if (query.isEmpty()) {
                    filterMovies()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPopularMovies()
        }
    }

    private fun loadPopularMovies() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val omdbResults = movieRepository.getPopularMovies()
                allMovies = omdbResults.map { movieRepository.searchResultToMovie(it) }

                Log.d("MainActivity", "Loaded ${allMovies.size} movies from OMDb")

                if (allMovies.isEmpty()) {
                    showEmpty(true)
                    Toast.makeText(
                        this@MainActivity,
                        "No movies loaded. Check your API key and internet connection.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    showEmpty(false)
                    adapter.submitList(allMovies)
                    setupGenreFilter()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading movies", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error loading movies: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupGenreFilter() {
        binding.genreChipGroup.removeAllViews()

        // Extract genres from loaded movies
        val genres = movieRepository.extractGenresFromMovies(allMovies).sorted()

        if (genres.isEmpty()) return

        genres.forEach { genre ->
            val chip = Chip(this).apply {
                text = genre
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGenres.add(genre)
                    } else {
                        selectedGenres.remove(genre)
                    }
                    filterMovies()
                }
            }
            binding.genreChipGroup.addView(chip)
        }
    }

    private fun searchMovies(query: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val omdbResults = movieRepository.searchMovies(query)

                if (omdbResults.isEmpty()) {
                    allMovies = emptyList()
                    showEmpty(true)
                    Toast.makeText(
                        this@MainActivity,
                        "No movies found for \"$query\"",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Get full details for each search result
                    allMovies = omdbResults.map { result ->
                        val details = movieRepository.getMovieDetails(result.imdbID)
                        if (details != null) {
                            movieRepository.movieDetailsToMovie(details)
                        } else {
                            movieRepository.searchResultToMovie(result)
                        }
                    }

                    showEmpty(false)
                    adapter.submitList(allMovies)
                    setupGenreFilter()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error searching movies", e)
                Toast.makeText(
                    this@MainActivity,
                    "Search error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterMovies() {
        val query = binding.searchBar.text.toString().toLowerCase()

        val filtered = allMovies.filter { movie ->
            val matchesSearch = query.isEmpty() ||
                    movie.title.toLowerCase().contains(query) ||
                    movie.director.toLowerCase().contains(query)

            val matchesGenre = selectedGenres.isEmpty() ||
                    movie.genre.any { it in selectedGenres }

            matchesSearch && matchesGenre
        }

        adapter.submitList(filtered)
        showEmpty(filtered.isEmpty() && query.isNotEmpty())
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyView.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                if (firebaseManager.getCurrentUser() != null) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                true
            }
            R.id.action_watchlist -> {
                if (firebaseManager.getCurrentUser() != null) {
                    startActivity(Intent(this, WatchlistActivity::class.java))
                } else {
                    Toast.makeText(this, "Please sign in to view watchlist", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                true
            }
            R.id.action_sort_rating -> {
                val sorted = allMovies.sortedByDescending { it.averageRating }
                adapter.submitList(sorted)
                Toast.makeText(this, "Sorted by rating", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_sort_year -> {
                val sorted = allMovies.sortedByDescending { it.year }
                adapter.submitList(sorted)
                Toast.makeText(this, "Sorted by year", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_sort_title -> {
                val sorted = allMovies.sortedBy { it.title }
                adapter.submitList(sorted)
                Toast.makeText(this, "Sorted by title", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (allMovies.isNotEmpty()) {
            filterMovies()
        }
    }
}