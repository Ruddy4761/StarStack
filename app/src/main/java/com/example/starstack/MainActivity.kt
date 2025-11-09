package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var adapter: MovieGridAdapter
    
    
    private var allMovies = listOf<Movie>()
    private var selectedGenres = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "StarStack"

        setupRecyclerView()
        setupSearchBar()
        setupGenreFilter()
        setupSwipeRefresh()

        loadMovies()
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
                filterMovies()
            }
        })
    }

    private fun setupGenreFilter() {
        val genres = listOf("Action", "Drama", "Sci-Fi", "Thriller", "Crime", "Adventure")

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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadMovies()
        }
    }

    private fun loadMovies() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Initialize sample data if needed
                firebaseManager.initializeSampleMovies()

                allMovies = firebaseManager.getAllMovies()
                adapter.submitList(allMovies)

                if (allMovies.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                }
            } catch (e: Exception) {
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
        showEmpty(filtered.isEmpty() && !query.isEmpty())
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
                true
            }
            R.id.action_sort_year -> {
                val sorted = allMovies.sortedByDescending { it.year }
                adapter.submitList(sorted)
                true
            }
            R.id.action_sort_title -> {
                val sorted = allMovies.sortedBy { it.title }
                adapter.submitList(sorted)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh movies when returning to this activity
        filterMovies()
    }
}
