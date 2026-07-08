package com.appvoyager.litememo.ui.state

data class TutorialUiState(val status: TutorialStatus = TutorialStatus.LOADING) {
    fun next(completed: Boolean): TutorialUiState = when {
        completed -> copy(status = TutorialStatus.HIDDEN)
        status == TutorialStatus.LOADING -> copy(status = TutorialStatus.VISIBLE)
        else -> this
    }
}

enum class TutorialStatus {
    LOADING,
    VISIBLE,
    HIDDEN
}
