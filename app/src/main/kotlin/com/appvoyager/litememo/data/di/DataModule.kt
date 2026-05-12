package com.appvoyager.litememo.data.di

import android.content.Context
import androidx.room.Room
import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.data.provider.SystemCurrentTimeProvider
import com.appvoyager.litememo.data.provider.UuidMemoIdProvider
import com.appvoyager.litememo.data.provider.UuidTagIdProvider
import com.appvoyager.litememo.data.repository.RoomMemoRepository
import com.appvoyager.litememo.data.repository.RoomTagRepository
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideLiteMemoDatabase(@ApplicationContext context: Context): LiteMemoDatabase =
        Room.databaseBuilder(
            context,
            LiteMemoDatabase::class.java,
            LiteMemoDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideMemoDao(database: LiteMemoDatabase): MemoDao = database.memoDao()

    @Provides
    fun provideTagDao(database: LiteMemoDatabase): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun provideMemoRepository(repository: RoomMemoRepository): MemoRepository = repository

    @Provides
    @Singleton
    fun provideTagRepository(repository: RoomTagRepository): TagRepository = repository

    @Provides
    fun provideCurrentTimeProvider(): CurrentTimeProvider = SystemCurrentTimeProvider()

    @Provides
    fun provideMemoIdProvider(): MemoIdProvider = UuidMemoIdProvider()

    @Provides
    fun provideTagIdProvider(): TagIdProvider = UuidTagIdProvider()

}
