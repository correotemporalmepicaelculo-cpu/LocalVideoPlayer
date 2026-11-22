package com.lucas.videoplayer.data.repository

import android.content.Context
import com.lucas.videoplayer.data.local.AppDatabase
import com.lucas.videoplayer.data.local.dao.VideoDao
import com.lucas.videoplayer.data.local.entity.VideoEntity
import com.lucas.videoplayer.data.scanner.VideoScanner
import com.lucas.videoplayer.domain.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val videoScanner: VideoScanner
) {
    private val videoDao: VideoDao = database.videoDao()

    val allVideos: Flow<List<Video>> = videoDao.getAll()
        .map { entities -> entities.map { Video.fromEntity(it) } }

    val favorites: Flow<List<Video>> = videoDao.getFavorites()
        .map { entities -> entities.map { Video.fromEntity(it) } }

    suspend fun scanFolders(folderPaths: List<String>, callback: VideoScanner.ScanCallback) {
        videoScanner.scanFolders(folderPaths, callback)
    }

    suspend fun insertVideos(videos: List<VideoEntity>) = withContext(Dispatchers.IO) {
        videoDao.insertAll(videos)
    }

    suspend fun getVideoByPath(path: String): Video? = withContext(Dispatchers.IO) {
        videoDao.getByPath(path)?.let { Video.fromEntity(it) }
    }

    suspend fun getVideosByFolder(folderPath: String): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getByFolder(folderPath).map { Video.fromEntity(it) }
    }

    suspend fun getVideosByFolders(folderPaths: List<String>): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getByFolders(folderPaths).map { Video.fromEntity(it) }
    }

    suspend fun getAllVideosSync(): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getAllSync().map { Video.fromEntity(it) }
    }

    suspend fun getRecentlyPlayed(limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getRecentlyPlayed(limit).map { Video.fromEntity(it) }
    }

    suspend fun getMostPlayed(limit: Int = 20): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getMostPlayed(limit).map { Video.fromEntity(it) }
    }

    suspend fun searchVideos(query: String): List<Video> = withContext(Dispatchers.IO) {
        videoDao.search("%$query%").map { Video.fromEntity(it) }
    }

    suspend fun updatePlayStats(path: String, lastPlayed: Long, position: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlayStats(path, lastPlayed, position)
    }

    suspend fun updatePosition(path: String, position: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePosition(path, position)
    }

    suspend fun updateThumbnail(path: String, thumbPath: String) = withContext(Dispatchers.IO) {
        videoDao.updateThumbnail(path, thumbPath)
    }

    suspend fun updateHasSubtitles(path: String, hasSubs: Boolean) = withContext(Dispatchers.IO) {
        videoDao.updateHasSubtitles(path, hasSubs)
    }

    suspend fun toggleFavorite(video: Video) = withContext(Dispatchers.IO) {
        val updated = video.copy(isFavorite = !video.isFavorite)
        videoDao.update(updated.toEntity())
    }

    suspend fun getVideoCount(): Int = withContext(Dispatchers.IO) {
        videoDao.count()
    }

    suspend fun getVideoCountByFolder(folderPath: String): Int = withContext(Dispatchers.IO) {
        videoDao.countByFolder(folderPath)
    }

    suspend fun deleteVideo(path: String) = withContext(Dispatchers.IO) {
        videoDao.deleteByPath(path)
    }
}