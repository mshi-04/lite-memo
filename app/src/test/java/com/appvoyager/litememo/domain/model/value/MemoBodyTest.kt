package com.appvoyager.litememo.domain.model.value

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemoBodyTest {

    @Test
    fun constructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
        // Act
        val body = MemoBody("  body  ")

        // Assert
        assertEquals("body", body.value)
    }

    @Test
    fun constructorReturnsEmptyStringWhenValueIsBlank() {
        // Act
        val body = MemoBody(" ")

        // Assert
        assertEquals("", body.value)
    }

}
