package com.lucas.videoplayer.domain.model

import java.util.Date

data class Video(
    val path: String,
    val name: String,
    val displayName: String,
    val folderPath: String,
    val duration: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val dateAdded: Date,
    val dateModified: Date,
    val lastPlayed: Date?,
    val playCount: Int,
    val lastPosition: Long,
    val thumbnailPath: String?,
    val hasSubtitles: Boolean,
    val isFavorite: Boolean,
    val tags: List<String>
) {
    companion object {
        fun fromEntity(entity: com.lucas.videoplayer.data.local.entity.VideoEntity): Video {
            return Video(
                path = entity.path,
                name = entity.name,
                displayName = entity.displayName,
                folderPath = entity.folderPath,
                duration = entity.duration,
                size = entity.size,
                width = entity.width,
                height = entity.height,
                mimeType = entity.mimeType,
                dateAdded = entity.dateAdded,
                dateModified = entity.dateModified,
                lastPlayed = entity.lastPlayed,
                playCount = entity.playCount,
                lastPosition = entity.lastPosition,
                thumbnailPath = entity.thumbnailPath,
                hasSubtitles = entity.hasSubtitles,
                isFavorite = entity.isFavorite,
                tags = entity.tags.split(",").filter { it.isNotBlank() }
            )
        }
    }

    fun toEntity(): com.lucas.videoplayer.data.local.entity.VideoEntity {
        return com.lucas.videoplayer.data.local.entity.VideoEntity(
            path = path,
            name = name,
            displayName = displayName,
            folderPath = folderPath,
            duration = duration,
            size = size,
            width = width,
            height = height,
            mimeType = mimeType,
            dateAdded = dateAdded,
            dateModified = dateModified,
            lastPlayed = lastPlayed,
            playCount = playCount,
            lastPosition = lastPosition,
            thumbnailPath = thumbnailPath,
            hasSubtitles = hasSubtitles,
            isFavorite = isFavorite,
            tags = tags.joinToString(",")
        )
    }

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            return when {
                size >= 1_000_000_000 -> "%.1f GB".format(size / 1_000_000_000.0)
                size >= 1_000_000 -> "%.1f MB".format(size / 1_000_000.0)
                size >= 1_000 -> "%.1f KB".format(size / 1_000.0)
                else -> "$size B"
            }
        }

    val resolution: String
        get() = "${width}x${height}"

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height else 16f / 9f
}