package com.appvoyager.litememo.ui.event

import com.appvoyager.litememo.domain.model.value.MemoId

sealed interface MemoEditNavigationUiEvent {
    data object NavigateBack : MemoEditNavigationUiEvent
    data class MemoDeleted(val memoId: MemoId) : MemoEditNavigationUiEvent
}
