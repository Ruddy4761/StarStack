package com.example.starstack.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.starstack.R
import com.example.starstack.databinding.ItemReviewBinding
import com.example.starstack.models.Review
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private val onLikeClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit,
    private val currentUserId: String?
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                tvUserName.text = review.userName
                tvRating.text = String.format("%.1f", review.rating)
                tvReviewText.text = review.reviewText
                tvTimestamp.text = formatDate(review.timestamp.toDate())
                tvLikes.text = "${review.likes} likes"

                // Show/hide delete button
                btnDelete.visibility = if (review.userId == currentUserId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Update like button state
                val isLiked = currentUserId?.let { review.likedBy.contains(it) } ?: false
                if (isLiked) {
                    btnLike.setIconResource(R.drawable.ic_like_filled)
                    btnLike.setIconTintResource(R.color.like_red) // Use a red color
                } else {
                    btnLike.setIconResource(R.drawable.ic_like)
                    btnLike.iconTint = null
                }

                btnLike.setOnClickListener {
                    onLikeClick(review)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(review)
                }
            }
        }

        private fun formatDate(date: Date): String {
            val now = Date()
            val diff = now.time - date.time

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 604800000 -> "${diff / 86400000}d ago"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Review, newItem: Review) = oldItem == newItem
    }
}
