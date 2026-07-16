package com.appvoyager.litememo.ui.navigation

import com.appvoyager.litememo.domain.model.value.MemoId

sealed class WidgetNavRequest {
    data object NewMemo : WidgetNavRequest()

    data class OpenMemo(val memoId: MemoId) : WidgetNavRequest()
}
