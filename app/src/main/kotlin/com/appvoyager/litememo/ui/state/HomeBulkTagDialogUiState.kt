package com.appvoyager.litememo.ui.state

data class HomeBulkTagDialogUiState(val operation: Operation? = null) {
    enum class Operation {
        AddTag,
        RemoveTag
    }
}
