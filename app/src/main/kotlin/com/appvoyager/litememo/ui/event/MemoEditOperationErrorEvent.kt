package com.appvoyager.litememo.ui.event

sealed interface MemoEditOperationErrorEvent {
    data object SaveFailed : MemoEditOperationErrorEvent
    data object DeleteFailed : MemoEditOperationErrorEvent
    data object ImageAttachFailed : MemoEditOperationErrorEvent
}
