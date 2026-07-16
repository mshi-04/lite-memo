package com.appvoyager.litememo.ui.event

data class TutorialNavigationActions(
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
    val onCompleteTutorial: () -> Unit
)
