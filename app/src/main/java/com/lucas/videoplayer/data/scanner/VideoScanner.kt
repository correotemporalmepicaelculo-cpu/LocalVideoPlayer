package com.lucas.videoplayer.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.lucas.videoplayer.data.local.entity.VideoEntity
import com.lucas.videoplayer.domain.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

class VideoScanner(private val context: Context) {
    private val TAG = "VideoScanner"
    private val isScanning = AtomicBoolean(false)
    private val supportedMimeTypes = setOf(
        "video/mp4", "video/x-matroska", "video/webm", "video/quicktime",
        "video/x-msvideo", "video/x-ms-wmv", "video/3gpp", "video/3gpp2",
        "video/mpeg", "video/mp2t", "video/ogg"
    )
    private val supportedExtensions = setOf(
        ".mp4", ".mkv", ".webm", ".mov", ".avi", ".wmv", ".flv",
        ".3gp", ".3g2", ".mpeg", ".mpg", ".mts", ".m2ts", ".ts", ".ogv"
    )

    interface ScanCallback {
        fun onVideoFound(video: VideoEntity)
        fun onScanComplete(totalFound: Int)
        fun onError(error: String)
    }

    suspend fun scanFolders(folderPaths: List<String>, callback: ScanCallback) = withContext(Dispatchers.IO) {
        if (isScanning.getAndSet(true)) {
            callback.onError("Escaneo ya en progreso")
            return@withContext
        }

        var totalFound = 0
        try {
            for (folderPath in folderPaths) {
                if (!isScanning.get()) break
                val found = scanFolderRecursive(folderPath, callback)
                totalFound += found
            }
            callback.onScanComplete(totalFound)
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando: ${e.message}", e)
            callback.onError(e.message ?: "Error desconocido")
        } finally {
            isScanning.set(false)
        }
    }

    private fun scanFolderRecursive(folderPath: String, callback: ScanCallback): Int {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return 0

        var count = 0
        val files = folder.listFiles() ?: return 0

        for (file in files) {
            if (!isScanning.get()) break

            if (file.isDirectory) {
                // Evitar carpetas ocultas y de sistema
                if (!file.name.startsWith(".") && file.name != "Android" && file.name != ".thumbnails") {
                    count += scanFolderRecursive(file.absolutePath, callback)
                }
            } else if (isVideoFile(file)) {
                val video = extractVideoMetadata(file)
                video?.let {
                    callback.onVideoFound(it)
                    count++
                }
            }
        }
        return count
    }

    private fun isVideoFile(file: File): Boolean {
        val name = file.name.lowercase()
        return supportedExtensions.any { name.endsWith(it) }
    }

    private fun extractVideoMetadata(file: File): VideoEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.fromFile(file))

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val mimeTypeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val duration = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0
            val mimeType = mimeTypeStr ?: guessMimeType(file.name)
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // Ajustar width/height si hay rotación 90/270
            val (finalWidth, finalHeight) = if (rotation in setOf(90, 270)) {
                height to width
            } else {
                width to height
            }

            val name = file.nameWithoutExtension
            val now = Date()

            VideoEntity(
                path = file.absolutePath,
                name = name,
                displayName = name,
                folderPath = file.parentFile?.absolutePath ?: "",
                duration = duration,
                size = file.length(),
                width = finalWidth,
                height = finalHeight,
                mimeType = mimeType,
                dateAdded = now,
                dateModified = Date(file.lastModified()),
                lastPlayed = null,
                playCount = 0,
                lastPosition = 0L,
                thumbnailPath = null,
                hasSubtitles = checkSubtitles(file),
                isFavorite = false,
                tags = ""
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error extrayendo metadata de ${file.name}: ${e.message}")
            // Fallback básico sin metadata
            VideoEntity(
                path = file.absolutePath,
                name = file.nameWithoutExtension,
                displayName = file.nameWithoutExtension,
                folderPath = file.parentFile?.absolutePath ?: "",
                duration = 0L,
                size = file.length(),
                width = 0,
                height = 0,
                mimeType = guessMimeType(file.name),
                dateAdded = Date(),
                dateModified = Date(file.lastModified()),
                lastPlayed = null,
                playCount = 0,
                lastPosition = 0L,
                thumbnailPath = null,
                hasSubtitles = checkSubtitles(file),
                isFavorite = false,
                tags = ""
            )
        } finally {
            try { retriever.release() } catch (e: Exception) { }
        }
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.lowercase()) {
            name -> when {
                name.endsWith(".mp4") -> "video/mp4"
                name.endsWith(".mkv") -> "video/x-matroska"
                name.endsWith(".webm") -> "video/webm"
                name.endsWith(".mov") -> "video/quicktime"
                name.endsWith(".avi") -> "video/x-msvideo"
                name.endsWith(".wmv") -> "video/x-ms-wmv"
                name.endsWith(".flv") -> "video/x-flv"
                name.endsWith(".3gp") -> "video/3gpp"
                name.endsWith(".3g2") -> "video/3gpp2"
                name.endsWith(".ts"), name.endsWith(".mts"), name.endsWith(".m2ts") -> "video/mp2t"
                name.endsWith(".ogv") -> "video/ogg"
                else -> "video/mp4"
            }
        }
    }

    private fun checkSubtitles(videoFile: File): Boolean {
        val parent = videoFile.parentFile ?: return false
        val nameWithoutExt = videoFile.nameWithoutExtension
        val subtitleExtensions = setOf(".srt", ".vtt", ".ass", ".ssa", ".sub", ".idx")
        val files = parent.listFiles() ?: return false
        return files.any { f ->
            subtitleExtensions.any { f.name.lowercase().endsWith(it) } &&
                    f.nameWithoutExtension.startsWith(nameWithoutExt)
        }
    }

    fun cancel() {
        isScanning.set(false)
    }

    // Método alternativo usando MediaStore (más rápido en Android 10+)
    suspend fun scanViaMediaStore(folderUris: List<Uri>, callback: ScanCallback) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            callback.onError("MediaStore requiere Android 10+")
            return@withContext
        }

        var totalFound = 0
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        for (folderUri in folderUris) {
            if (!isScanning.get()) break
            val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("${folderUri.path}%")

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && isScanning.get()) {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val video = VideoEntity(
                        path = path,
                        name = file.nameWithoutExtension,
                        displayName = file.nameWithoutExtension,
                        folderPath = file.parentFile?.absolutePath ?: "",
                        duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
                        width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)),
                        height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)),
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)) ?: "video/mp4",
                        dateAdded = Date(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000),
                        dateModified = Date(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)) * 1000),
                        lastPlayed = null,
                        playCount = 0,
                        lastPosition = 0L,
                        thumbnailPath = null,
                        hasSubtitles = checkSubtitles(file),
                        isFavorite = false,
                        tags = ""
                    )
                    callback.onVideoFound(video)
                    totalFound++
                }
            }
        }
        callback.onScanComplete(totalFound)
    }
}