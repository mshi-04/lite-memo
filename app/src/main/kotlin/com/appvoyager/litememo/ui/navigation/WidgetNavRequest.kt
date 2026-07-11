package com.appvoyager.litememo.ui.navigation

sealed interface WidgetNavRequest {
    data object NewMemo : WidgetNavRequest

    data class OpenMemo(val memoId: String) : WidgetNavRequest
}
