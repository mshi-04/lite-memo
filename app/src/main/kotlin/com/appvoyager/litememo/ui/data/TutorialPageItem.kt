package com.appvoyager.litememo.ui.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class TutorialPageItem(
    val icon: ImageVector,
    @get:StringRes val titleResId: Int,
    @get:StringRes val bodyResId: Int
)
