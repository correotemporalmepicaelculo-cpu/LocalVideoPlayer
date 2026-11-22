package com.lucas.videoplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lucas.videoplayer.data.local.converters.Converters
import com.lucas.videoplayer.data.local.dao.HistoryDao
import com.lucas.videoplayer.data.local.dao.PlaylistDao
import com.lucas.videoplayer.data.local.dao.VideoDao
import com.lucas.videoplayer.data.local.entity.HistoryEntity
import com.lucas.videoplayer.data.local.entity.PlaylistEntity
import com.lucas.videoplayer.data.local.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "videoplayer_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}