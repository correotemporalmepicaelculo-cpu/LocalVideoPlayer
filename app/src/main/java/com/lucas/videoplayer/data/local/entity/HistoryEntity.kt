package com.lucas.videoplayer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lucas.videoplayer.data.local.converters.Converters
import java.util.Date

@Entity(tableName = "history",
    indices = [
        Index(value = ["videoPath"]),
        Index(value = ["playedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["path"],
            childColumns = ["videoPath"],
            onDelete = ForeignKey.CASCADE
        )
    ])
@TypeConverters(Converters::class)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val videoPath: String,
    val position: Long, // posición en ms donde se quedó
    val duration: Long, // duración total en ese momento
    val playedAt: Date, // cuándo se reprodujo
    val completed: Boolean // si terminó el video (>90%)
)