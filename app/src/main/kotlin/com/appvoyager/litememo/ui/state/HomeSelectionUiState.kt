package com.appvoyager.litememo.ui.state

data class HomeSelectionUiState(val selectedMemoIds: List<String> = emptyList()) {
    val isActive: Boolean
        get() = selectedMemoIds.isNotEmpty()

    val selectedCount: Int
        get() = selectedMemoIds.size

    fun contains(memoId: String): Boolean = memoId in selectedMemoIds
}
