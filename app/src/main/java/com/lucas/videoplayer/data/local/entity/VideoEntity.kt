package com.lucas.videoplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lucas.videoplayer.data.local.converters.Converters
import java.util.Date

@Entity(tableName = "videos",
    indices = [
        androidx.room.Index(value = ["folderPath"]),
        androidx.room.Index(value = ["lastPlayed"]),
        androidx.room.Index(value = ["dateAdded"])
    ])
@TypeConverters(Converters::class)
data class VideoEntity(
    @PrimaryKey(autoGenerate = false)
    val path: String, // ruta absoluta del archivo - única

    val name: String, // nombre del archivo sin extensión
    val displayName: String, // nombre para mostrar (puede ser editado)
    val folderPath: String, // carpeta contenedora
    val duration: Long, // duración en milisegundos
    val size: Long, // tamaño en bytes
    val width: Int, // ancho video
    val height: Int, // alto video
    val mimeType: String, // video/mp4, video/x-matroska, etc.
    val dateAdded: Date, // cuando se descubrió
    val dateModified: Date, // fecha modificación archivo
    val lastPlayed: Date?, // última reproducción
    val playCount: Int, // veces reproducido
    val lastPosition: Long, // posición última reproducción (ms)
    val thumbnailPath: String?, // path miniatura generada
    val hasSubtitles: Boolean, // si hay .srt/.vtt asociado
    val isFavorite: Boolean, // marcado como favorito
    val tags: String // JSON array de tags
)