package com.lucas.videoplayer.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.lucas.videoplayer.R
import com.lucas.videoplayer.domain.model.Video

class VideoAdapter(
    private val onClick: (Video) -> Unit
) : ListAdapter<Video, VideoAdapter.VideoViewHolder>(DiffCallback()) {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.video_thumbnail)
        val title: TextView = view.findViewById(R.id.video_title)
        val duration: TextView = view.findViewById(R.id.video_duration)
        val size: TextView = view.findViewById(R.id.video_size)
        val playIcon: ImageView = view.findViewById(R.id.play_icon)
        val favoriteIcon: ImageView = view.findViewById(R.id.favorite_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.title.text = video.displayName
        holder.duration.text = video.formattedDuration
        holder.size.text = video.formattedSize

        // Cargar miniatura con Glide
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_video_placeholder)
            .error(R.drawable.ic_video_error)
            .transform(RoundedCorners(16))

        video.thumbnailPath?.let { thumbPath ->
            Glide.with(holder.itemView.context)
                .load(thumbPath)
                .apply(requestOptions)
                .into(holder.thumbnail)
        } ?: run {
            // Generar miniatura del primer frame si no existe
            Glide.with(holder.itemView.context)
                .load(video.path)
                .apply(requestOptions)
                .into(holder.thumbnail)
        }

        holder.favoriteIcon.isVisible = video.isFavorite

        holder.itemView.setOnClickListener { onClick(video) }
    }

    class DiffCallback : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem == newItem
        }
    }
}