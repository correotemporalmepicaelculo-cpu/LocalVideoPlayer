package com.lucas.videoplayer.data.repository

import android.content.Context
import com.lucas.videoplayer.data.local.AppDatabase
import com.lucas.videoplayer.data.local.dao.HistoryDao
import com.lucas.videoplayer.data.local.entity.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val context: Context,
    private val database: AppDatabase
) {
    private val historyDao: HistoryDao = database.historyDao()

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun savePlayback(
        videoPath: String,
        position: Long,
        duration: Long,
        completed: Boolean
    ) = withContext(Dispatchers.IO) {
        val history = HistoryEntity(
            videoPath = videoPath,
            position = position,
            duration = duration,
            playedAt = java.util.Date(),
            completed = completed
        )
        historyDao.insert(history)
    }

    suspend fun getLastPosition(videoPath: String): Long? = withContext(Dispatchers.IO) {
        historyDao.getLastPlayback(videoPath)?.position
    }

    suspend fun getRecentHistory(limit: Int = 50): List<HistoryDao.HistoryWithVideo> = withContext(Dispatchers.IO) {
        historyDao.getRecentHistory(limit)
    }

    suspend fun getHistoryForVideo(videoPath: String): List<HistoryEntity> = withContext(Dispatchers.IO) {
        historyDao.getHistoryForVideo(videoPath)
    }

    suspend fun clearOldHistory(days: Int = 90) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
        historyDao.deleteOldHistory(cutoff)
    }

    suspend fun clearHistoryForVideo(videoPath: String) = withContext(Dispatchers.IO) {
        historyDao.deleteHistoryForVideo(videoPath)
    }

    suspend fun getCompletionCount(videoPath: String): Int = withContext(Dispatchers.IO) {
        historyDao.getCompletionCount(videoPath)
    }
}