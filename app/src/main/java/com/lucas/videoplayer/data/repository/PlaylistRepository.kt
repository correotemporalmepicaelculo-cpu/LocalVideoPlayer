package com.lucas.videoplayer.data.repository

import android.content.Context
import com.lucas.videoplayer.data.local.AppDatabase
import com.lucas.videoplayer.data.local.dao.PlaylistDao
import com.lucas.videoplayer.data.local.entity.PlaylistEntity
import com.lucas.videoplayer.data.local.entity.VideoEntity
import com.lucas.videoplayer.domain.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val context: Context,
    private val database: AppDatabase
) {
    private val playlistDao: PlaylistDao = database.playlistDao()

    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAll()
        .map { entities ->
            entities.groupBy { it.id }.map { (id, items) ->
                val videos = items.flatMap { item ->
                    database.videoDao().getByPath(item.videoPath)
                }
                Playlist.fromEntities(items.first(), videos)
            }
        }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(
            name = name,
            description = description,
            videoPath = "",
            orderIndex = 0,
            dateCreated = java.util.Date(),
            dateModified = java.util.Date(),
            thumbnailPath = null
        )
        playlistDao.insert(playlist)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, videoPath: String): Long = withContext(Dispatchers.IO) {
        val maxIndex = playlistDao.getMaxOrderIndex(playlistId) ?: -1
        val playlist = PlaylistEntity(
            name = "",
            description = "",
            videoPath = videoPath,
            orderIndex = maxIndex + 1,
            dateCreated = java.util.Date(),
            dateModified = java.util.Date(),
            thumbnailPath = null
        )
        playlistDao.insert(playlist)
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoPath: String) = withContext(Dispatchers.IO) {
        playlistDao.removeVideoFromPlaylist(playlistId, videoPath)
    }

    suspend fun reorderVideo(playlistId: Long, videoPath: String, newIndex: Int) = withContext(Dispatchers.IO) {
        playlistDao.updateVideoOrder(playlistId, videoPath, newIndex)
    }

    suspend fun getPlaylistVideos(playlistId: Long): List<VideoEntity> = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistVideos(playlistId)
    }

    suspend fun getPlaylistById(id: Long): Playlist? = withContext(Dispatchers.IO) {
        val entity = playlistDao.getById(id)
        if (entity == null) return@withContext null
        val videos = playlistDao.getPlaylistVideos(id)
        Playlist.fromEntities(entity, videos)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.update(playlist.copy(dateModified = java.util.Date()))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deleteById(id)
    }

    suspend fun getPlaylistsForVideo(videoPath: String): List<PlaylistEntity> = withContext(Dispatchers.IO) {
        playlistDao.getByVideoPath(videoPath)
    }
}