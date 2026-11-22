package com.lucas.videoplayer.di

import android.content.Context
import com.lucas.videoplayer.data.local.AppDatabase
import com.lucas.videoplayer.data.repository.HistoryRepository
import com.lucas.videoplayer.data.repository.PlaylistRepository
import com.lucas.videoplayer.data.repository.VideoRepository
import com.lucas.videoplayer.data.scanner.VideoScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVideoScanner(@ApplicationContext context: Context): VideoScanner {
        return VideoScanner(context)
    }

    @Provides
    @Singleton
    fun provideVideoRepository(
        @ApplicationContext context: Context,
        database: AppDatabase,
        scanner: VideoScanner
    ): VideoRepository {
        return VideoRepository(context, database, scanner)
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): PlaylistRepository {
        return PlaylistRepository(context, database)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): HistoryRepository {
        return HistoryRepository(context, database)
    }
}