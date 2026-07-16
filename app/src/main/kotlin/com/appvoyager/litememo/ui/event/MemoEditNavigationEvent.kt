package com.appvoyager.litememo.ui.event

import com.appvoyager.litememo.domain.model.value.MemoId

sealed interface MemoEditNavigationEvent {
    data object NavigateBack : MemoEditNavigationEvent
    data class MemoDeleted(val memoId: MemoId) : MemoEditNavigationEvent
}
