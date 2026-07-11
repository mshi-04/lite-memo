package com.appvoyager.litememo.ui.navigation

sealed class WidgetNavRequest {
    data object NewMemo : WidgetNavRequest()

    data class OpenMemo(val memoId: String) : WidgetNavRequest()
}
