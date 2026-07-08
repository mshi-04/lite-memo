package com.appvoyager.litememo.ui.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TutorialUiStateTest {

    @Test
    fun stateTransitionShowsTutorialWhenIncompleteObservedFromLoading() {
        // Arrange
        val state = TutorialUiState(status = TutorialStatus.LOADING)

        // Act
        // StateTransition: incomplete flag from loading shows tutorial
        val result = state.next(completed = false)

        // Assert
        assertEquals(TutorialUiState(status = TutorialStatus.VISIBLE), result)
    }

    @Test
    fun stateTransitionHidesTutorialWhenCompletedObservedFromLoading() {
        // Arrange
        val state = TutorialUiState(status = TutorialStatus.LOADING)

        // Act
        // StateTransition: completed flag from loading hides tutorial
        val result = state.next(completed = true)

        // Assert
        assertEquals(TutorialUiState(status = TutorialStatus.HIDDEN), result)
    }

    @Test
    fun stateTransitionHidesTutorialWhenCompletedObservedFromVisible() {
        // Arrange
        val state = TutorialUiState(status = TutorialStatus.VISIBLE)

        // Act
        // StateTransition: completed flag from visible hides tutorial
        val result = state.next(completed = true)

        // Assert
        assertEquals(TutorialUiState(status = TutorialStatus.HIDDEN), result)
    }

    @Test
    fun stateTransitionKeepsHiddenWhenIncompleteIsObservedAfterHidden() {
        // Arrange
        val state = TutorialUiState(status = TutorialStatus.HIDDEN)

        // Act
        // StateTransition: false after hidden does not show tutorial again
        val result = state.next(completed = false)

        // Assert
        assertEquals(TutorialUiState(status = TutorialStatus.HIDDEN), result)
    }
}
