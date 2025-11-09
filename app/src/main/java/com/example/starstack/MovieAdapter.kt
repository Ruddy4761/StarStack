package com.example.starstack


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovieAdapter(private var movies: List<Movie>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private var fullList: List<Movie> = ArrayList(movies) // keep copy of full list

    // ViewHolder
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val poster: ImageView = itemView.findViewById(R.id.moviePoster)
        val title: TextView = itemView.findViewById(R.id.movieTitle)
        val rating: TextView = itemView.findViewById(R.id.movieRating)
        val description: TextView = itemView.findViewById(R.id.movieDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.poster.setImageResource(movie.posterResId)
        holder.title.text = movie.title
        holder.rating.text = "⭐ ${movie.rating}/5"
        holder.description.text = movie.description
    }

    override fun getItemCount(): Int = movies.size

    // ✅ Filter function
    fun filter(query: String) {
        movies = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter { it.title.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}