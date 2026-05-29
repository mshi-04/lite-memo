package com.appvoyager.litememo.ui.state

data class AppLockUiState(
    val status: AppLockStatus = AppLockStatus.LOADING,
    val message: AppLockMessage? = null
) {
    val canShowAppContent: Boolean
        get() = status == AppLockStatus.UNLOCKED
}

enum class AppLockStatus {
    LOADING,
    UNLOCKED,
    LOCKED,
    AUTHENTICATING,
    UNAVAILABLE
}

enum class AppLockMessage {
    AUTHENTICATION_FAILED,
    AUTHENTICATION_CANCELED,
    NO_DEVICE_CREDENTIAL,
    AUTHENTICATION_UNAVAILABLE
}
