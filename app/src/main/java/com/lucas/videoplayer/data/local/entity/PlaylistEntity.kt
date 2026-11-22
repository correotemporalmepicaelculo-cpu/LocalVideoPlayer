package com.lucas.videoplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lucas.videoplayer.data.local.converters.Converters
import java.util.Date

@Entity(tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["path"],
            childColumns = ["videoPath"],
            onDelete = ForeignKey.CASCADE
        )
    ])
@TypeConverters(Converters::class)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val description: String,
    val videoPath: String, // referencia a VideoEntity.path
    val orderIndex: Int, // orden en la playlist
    val dateCreated: Date,
    val dateModified: Date,
    val thumbnailPath: String? // miniatura de la playlist (primer video)
)