package com.appvoyager.litememo.data.model

import androidx.datastore.preferences.core.Preferences

internal data class DraftKeys(
    val title: Preferences.Key<String>,
    val body: Preferences.Key<String>,
    val tagIds: Preferences.Key<String>,
    val isFavorite: Preferences.Key<Boolean>,
    val createdAt: Preferences.Key<Long>
)
