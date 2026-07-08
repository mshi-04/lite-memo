package com.appvoyager.litememo.ui.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TutorialUiStateTest {

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
