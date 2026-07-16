package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.type.TutorialStatus

data class TutorialUiState(val status: TutorialStatus = TutorialStatus.LOADING) {
    fun next(completed: Boolean): TutorialUiState = when {
        completed -> copy(status = TutorialStatus.HIDDEN)
        status == TutorialStatus.LOADING -> copy(status = TutorialStatus.VISIBLE)
        else -> this
    }
}
