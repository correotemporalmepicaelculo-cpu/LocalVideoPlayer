package com.lucas.videoplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucas.videoplayer.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<HistoryEntity>): List<Long>

    @Query("SELECT * FROM history WHERE videoPath = :videoPath ORDER BY playedAt DESC LIMIT 1")
    suspend fun getLastPlayback(videoPath: String): HistoryEntity?

    @Query("SELECT * FROM history WHERE videoPath = :videoPath ORDER BY playedAt DESC")
    suspend fun getHistoryForVideo(videoPath: String): List<HistoryEntity>

    @Query("SELECT h.*, v.name, v.thumbnailPath, v.duration FROM history h INNER JOIN videos v ON h.videoPath = v.path ORDER BY h.playedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<HistoryWithVideo>

    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history WHERE playedAt < :beforeDate")
    suspend fun deleteOldHistory(beforeDate: Long): Int

    @Query("DELETE FROM history WHERE videoPath = :videoPath")
    suspend fun deleteHistoryForVideo(videoPath: String): Int

    @Query("SELECT COUNT(*) FROM history WHERE videoPath = :videoPath AND completed = 1")
    suspend fun getCompletionCount(videoPath: String): Int
}

data class HistoryWithVideo(
    val id: Long,
    val videoPath: String,
    val position: Long,
    val duration: Long,
    val playedAt: java.util.Date,
    val completed: Boolean,
    val name: String,
    val thumbnailPath: String?,
    val videoDuration: Long
)