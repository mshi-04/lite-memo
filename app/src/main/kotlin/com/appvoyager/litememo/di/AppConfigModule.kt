package com.appvoyager.litememo.di

import com.appvoyager.litememo.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()

    @Provides
    @Singleton
    @AppVersion
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    @ImportMaxFileSizeBytes
    fun provideImportMaxFileSizeBytes(): Long = DEFAULT_IMPORT_MAX_FILE_SIZE_BYTES

    private const val DEFAULT_IMPORT_MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024
}
