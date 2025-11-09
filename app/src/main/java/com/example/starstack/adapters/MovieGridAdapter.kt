package com.example.starstack.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.starstack.databinding.ItemMovieGridBinding
import com.example.starstack.models.Movie

// Movie Grid Adapter
class MovieGridAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieGridAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMovieClick(getItem(position))
                }
            }
        }

        fun bind(movie: Movie) {
            binding.apply {
                tvTitle.text = movie.title
                tvYear.text = movie.year.toString()
                tvRating.text = "⭐ ${String.format("%.1f", movie.averageRating)}"
                tvGenre.text = movie.genre.firstOrNull() ?: ""

                // Load poster image using Glide (you'll need to add actual images)
                // For now, using placeholder
                // Glide.with(itemView.context)
                //     .load(movie.posterUrl)
                //     .placeholder(R.drawable.placeholder_movie)
                //     .into(ivPoster)
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}