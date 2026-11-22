package com.lucas.videoplayer.domain.model

import java.util.Date

data class Playlist(
    val id: Long,
    val name: String,
    val description: String,
    val videoPaths: List<String>,
    val dateCreated: Date,
    val dateModified: Date,
    val thumbnailPath: String?
) {
    companion object {
        fun fromEntities(
            playlistEntity: com.lucas.videoplayer.data.local.entity.PlaylistEntity,
            videoEntities: List<com.lucas.videoplayer.data.local.entity.VideoEntity>
        ): Playlist {
            val videoPaths = videoEntities.map { it.path }
            val thumbPath = videoEntities.firstOrNull()?.thumbnailPath ?: playlistEntity.thumbnailPath
            return Playlist(
                id = playlistEntity.id,
                name = playlistEntity.name,
                description = playlistEntity.description,
                videoPaths = videoPaths,
                dateCreated = playlistEntity.dateCreated,
                dateModified = playlistEntity.dateModified,
                thumbnailPath = thumbPath
            )
        }
    }

    val videoCount: Int
        get() = videoPaths.size

    val totalDuration: Long
        get() = 0L // Se calcula cuando se cargan los videos completos
}