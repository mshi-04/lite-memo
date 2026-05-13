package com.appvoyager.litememo.ui.navigation

import androidx.annotation.StringRes
import com.appvoyager.litememo.R

enum class LiteMemoDestination(
    val route: String,
    @param:StringRes val labelResId: Int,
    val iconText: String
) {
    Home("home", R.string.home_tab, "H"),
    Calendar("calendar", R.string.calendar_tab, "C"),
    Settings("settings", R.string.settings_tab, "S")
}
