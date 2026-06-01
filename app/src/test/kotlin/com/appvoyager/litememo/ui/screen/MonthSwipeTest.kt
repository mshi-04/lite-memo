package com.appvoyager.litememo.ui.screen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MonthSwipeTest {

    @Test
    fun resolvesToNextWhenDraggedLeftBeyondThreshold() {
        // Act
        val result = resolveMonthSwipe(dragAmount = -120f, thresholdPx = 100f)

        // Assert
        assertEquals(MonthSwipeDirection.NEXT, result)
    }

    @Test
    fun resolvesToPreviousWhenDraggedRightBeyondThreshold() {
        // Act
        val result = resolveMonthSwipe(dragAmount = 120f, thresholdPx = 100f)

        // Assert
        assertEquals(MonthSwipeDirection.PREVIOUS, result)
    }

    @Test
    fun resolvesToNullWhenDragIsBelowThreshold() {
        // Act
        val result = resolveMonthSwipe(dragAmount = -80f, thresholdPx = 100f)

        // Assert
        assertNull(result)
    }

    @Test
    fun resolvesToActionWhenDragEqualsThreshold() {
        // Act
        val result = resolveMonthSwipe(dragAmount = -100f, thresholdPx = 100f)

        // Assert
        assertEquals(MonthSwipeDirection.NEXT, result)
    }
}
