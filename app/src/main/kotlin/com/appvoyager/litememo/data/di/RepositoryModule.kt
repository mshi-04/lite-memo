package com.appvoyager.litememo.data.di

import com.appvoyager.litememo.data.repository.RoomMemoRepository
import com.appvoyager.litememo.data.repository.RoomTagRepository
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMemoRepository(repository: RoomMemoRepository): MemoRepository = repository

    @Provides
    @Singleton
    fun provideTagRepository(repository: RoomTagRepository): TagRepository = repository

}
