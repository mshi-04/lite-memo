package com.appvoyager.litememo.data.di

import com.appvoyager.litememo.data.repository.DataStoreMemoEditDraftRepository
import com.appvoyager.litememo.data.repository.DataStoreUserSettingsRepository
import com.appvoyager.litememo.data.repository.RoomMemoRepository
import com.appvoyager.litememo.data.repository.RoomTagRepository
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
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

    @Binds
    @Singleton
    abstract fun bindMemoEditDraftRepository(
        repository: DataStoreMemoEditDraftRepository
    ): MemoEditDraftRepository

    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        repository: DataStoreUserSettingsRepository
    ): UserSettingsRepository
}
