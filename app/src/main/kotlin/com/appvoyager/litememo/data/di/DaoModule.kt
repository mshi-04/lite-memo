package com.appvoyager.litememo.data.di

import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideMemoDao(database: LiteMemoDatabase): MemoDao = database.memoDao()

    @Provides
    @Singleton
    fun provideTagDao(database: LiteMemoDatabase): TagDao = database.tagDao()

}
