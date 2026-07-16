package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.type.AppLockMessage
import com.appvoyager.litememo.ui.type.AppLockStatus

data class AppLockUiState(
    val status: AppLockStatus = AppLockStatus.LOADING,
    val message: AppLockMessage? = null
) {
    val canShowAppContent: Boolean
        get() = status == AppLockStatus.UNLOCKED
}
