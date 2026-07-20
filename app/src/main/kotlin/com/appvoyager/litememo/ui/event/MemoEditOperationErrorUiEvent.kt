package com.appvoyager.litememo.ui.event

sealed interface MemoEditOperationErrorUiEvent {
    data object SaveFailed : MemoEditOperationErrorUiEvent
    data object DeleteFailed : MemoEditOperationErrorUiEvent
    data object ImageAttachFailed : MemoEditOperationErrorUiEvent
}
