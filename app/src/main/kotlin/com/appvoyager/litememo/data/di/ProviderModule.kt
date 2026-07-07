package com.appvoyager.litememo.data.di

import com.appvoyager.litememo.data.provider.SystemCurrentTimeProvider
import com.appvoyager.litememo.data.provider.UuidMemoIdProvider
import com.appvoyager.litememo.data.provider.UuidMemoImageIdProvider
import com.appvoyager.litememo.data.provider.UuidTagIdProvider
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.provider.MemoImageIdProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideCurrentTimeProvider(): CurrentTimeProvider = SystemCurrentTimeProvider()

    @Provides
    @Singleton
    fun provideMemoIdProvider(): MemoIdProvider = UuidMemoIdProvider()

    @Provides
    @Singleton
    fun provideMemoImageIdProvider(): MemoImageIdProvider = UuidMemoImageIdProvider()

    @Provides
    @Singleton
    fun provideTagIdProvider(): TagIdProvider = UuidTagIdProvider()

}
