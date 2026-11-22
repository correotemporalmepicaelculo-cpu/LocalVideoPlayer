package com.lucas.videoplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lucas.videoplayer.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<PlaylistEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity): Int

    @Delete
    suspend fun delete(playlist: PlaylistEntity): Int

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM playlists WHERE videoPath = :videoPath AND id = :id")
    suspend fun removeVideoFromPlaylist(id: Long, videoPath: String): Int

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE name = :name")
    suspend fun getByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE videoPath = :videoPath ORDER BY orderIndex")
    suspend fun getByVideoPath(videoPath: String): List<PlaylistEntity>

    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY dateModified DESC")
    suspend fun getAllSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE videoPath = :videoPath ORDER BY orderIndex")
    suspend fun getVideosInPlaylist(videoPath: String): List<PlaylistEntity>

    @Query("SELECT v.* FROM videos v INNER JOIN playlists p ON v.path = p.videoPath WHERE p.id = :playlistId ORDER BY p.orderIndex")
    suspend fun getPlaylistVideos(playlistId: Long): List<com.lucas.videoplayer.data.local.entity.VideoEntity>

    @Query("SELECT MAX(orderIndex) FROM playlists WHERE id = :id")
    suspend fun getMaxOrderIndex(id: Long): Int?

    @Query("UPDATE playlists SET orderIndex = orderIndex + 1 WHERE id = :id AND orderIndex >= :newIndex")
    suspend fun shiftOrderIndex(id: Long, newIndex: Int): Int

    @Query("UPDATE playlists SET orderIndex = :newIndex WHERE id = :id AND videoPath = :videoPath")
    suspend fun updateVideoOrder(id: Long, videoPath: String, newIndex: Int): Int
}