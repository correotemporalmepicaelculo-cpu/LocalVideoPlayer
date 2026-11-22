package com.lucas.videoplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lucas.videoplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Update
    suspend fun update(video: VideoEntity): Int

    @Query("DELETE FROM videos WHERE path = :path")
    suspend fun deleteByPath(path: String): Int

    @Query("DELETE FROM videos")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM videos WHERE path = :path")
    suspend fun getByPath(path: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY dateAdded DESC")
    suspend fun getByFolder(folderPath: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE folderPath IN (:folderPaths) ORDER BY dateAdded DESC")
    suspend fun getByFolders(folderPaths: List<String>): List<VideoEntity>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    suspend fun getAllSync(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    suspend fun getRecentlyPlayed(limit: Int): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY lastPlayed DESC")
    fun getFavorites(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayed(limit: Int): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE name LIKE :query OR displayName LIKE :query ORDER BY dateAdded DESC")
    suspend fun search(query: String): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM videos WHERE folderPath = :folderPath")
    suspend fun countByFolder(folderPath: String): Int

    @Query("UPDATE videos SET lastPlayed = :lastPlayed, playCount = playCount + 1, lastPosition = :position WHERE path = :path")
    suspend fun updatePlayStats(path: String, lastPlayed: Long, position: Long): Int

    @Query("UPDATE videos SET lastPosition = :position WHERE path = :path")
    suspend fun updatePosition(path: String, position: Long): Int

    @Query("UPDATE videos SET thumbnailPath = :thumbPath WHERE path = :path")
    suspend fun updateThumbnail(path: String, thumbPath: String): Int

    @Query("UPDATE videos SET hasSubtitles = :hasSubs WHERE path = :path")
    suspend fun updateHasSubtitles(path: String, hasSubs: Boolean): Int
}