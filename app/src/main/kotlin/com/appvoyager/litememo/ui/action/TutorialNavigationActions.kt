package com.appvoyager.litememo.ui.action

data class TutorialNavigationActions(
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onCompleteTutorial: () -> Unit
)
