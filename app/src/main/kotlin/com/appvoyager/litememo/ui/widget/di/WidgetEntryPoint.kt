package com.appvoyager.litememo.ui.widget.di

import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveRecentMemosUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidgetReceiver / Application は Hilt 非管理のため、
 * application context から @Singleton な UseCase を取り出すための EntryPoint。
 * 公開範囲は UseCase のみに絞り、Repository / DAO は露出しない。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun observeMemosUseCase(): ObserveMemosUseCase

    fun observeRecentMemosUseCase(): ObserveRecentMemosUseCase
}
