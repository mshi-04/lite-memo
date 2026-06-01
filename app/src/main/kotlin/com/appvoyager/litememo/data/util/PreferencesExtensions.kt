package com.appvoyager.litememo.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

internal fun DataStore<Preferences>.dataOrEmptyOnIoError(): Flow<Preferences> = data.catch { e ->
    if (e is IOException) emit(emptyPreferences()) else throw e
}
