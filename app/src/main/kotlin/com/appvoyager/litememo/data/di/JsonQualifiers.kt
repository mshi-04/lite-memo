package com.appvoyager.litememo.data.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExportJson

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InternalJson
