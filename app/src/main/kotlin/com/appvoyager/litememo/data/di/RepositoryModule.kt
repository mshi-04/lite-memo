package com.appvoyager.litememo.data.di

import com.appvoyager.litememo.data.repository.RoomMemoRepository
import com.appvoyager.litememo.data.repository.RoomTagRepository
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMemoRepository(repository: RoomMemoRepository): MemoRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(repository: RoomTagRepository): TagRepository

}
