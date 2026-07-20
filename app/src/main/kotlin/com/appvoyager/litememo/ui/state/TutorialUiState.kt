package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.type.TutorialUiStatus

data class TutorialUiState(val status: TutorialUiStatus = TutorialUiStatus.LOADING) {
    fun next(completed: Boolean): TutorialUiState = when {
        completed -> copy(status = TutorialUiStatus.HIDDEN)
        status == TutorialUiStatus.LOADING -> copy(status = TutorialUiStatus.VISIBLE)
        else -> this
    }
}
