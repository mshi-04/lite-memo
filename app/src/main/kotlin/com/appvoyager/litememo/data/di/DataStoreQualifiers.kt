package com.appvoyager.litememo.data.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserSettingsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MemoEditDraftDataStore
