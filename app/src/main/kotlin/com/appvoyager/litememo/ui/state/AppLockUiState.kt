package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.type.AppLockUiMessage
import com.appvoyager.litememo.ui.type.AppLockUiStatus

data class AppLockUiState(
    val status: AppLockUiStatus = AppLockUiStatus.LOADING,
    val message: AppLockUiMessage? = null
) {
    val canShowAppContent: Boolean
        get() = status == AppLockUiStatus.UNLOCKED
}
