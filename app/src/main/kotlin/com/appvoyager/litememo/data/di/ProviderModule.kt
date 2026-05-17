package com.appvoyager.litememo.data.di

import android.content.Context
import com.appvoyager.litememo.data.provider.SystemCurrentTimeProvider
import com.appvoyager.litememo.data.provider.UuidMemoIdProvider
import com.appvoyager.litememo.data.provider.UuidTagIdProvider
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideCurrentTimeProvider(): CurrentTimeProvider = SystemCurrentTimeProvider()

    @Provides
    @Singleton
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()

    @Provides
    @Singleton
    fun provideMemoIdProvider(): MemoIdProvider = UuidMemoIdProvider()

    @Provides
    @Singleton
    fun provideTagIdProvider(): TagIdProvider = UuidTagIdProvider()

    @Provides
    @Named("appVersion")
    fun provideAppVersion(@ApplicationContext context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""

}
