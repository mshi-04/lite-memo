package com.appvoyager.litememo.ui.event

import com.appvoyager.litememo.domain.model.value.MemoId

object PreviewTrashScreenActions : TrashScreenActions {
    override fun onBackClick() = Unit

    override fun onMemoLongClick(memoId: MemoId) = Unit

    override fun onMemoSelectionToggle(memoId: MemoId) = Unit

    override fun onClearSelection() = Unit

    override fun onRestoreSelectedMemos() = Unit

    override fun onEmptyTrashRequest() = Unit

    override fun onConfirmEmptyTrash() = Unit

    override fun onDismissEmptyTrash() = Unit

    override fun onRetry() = Unit
}
