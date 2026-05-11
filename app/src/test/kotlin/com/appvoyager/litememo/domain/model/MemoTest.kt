package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MemoTest {

    @Test
    fun constructorReturnsMemoWhenTitleIsProvidedAndBodyIsBlank() {
        // Act
        val memo = Memo(
            id = MemoId("memo-1"),
            title = MemoTitle("Title"),
            body = MemoBody(" "),
            createdAt = TimestampMillis(1000L),
            updatedAt = TimestampMillis(1000L)
        )

        // Assert
        assertEquals("Title", memo.title.value)
    }

    @Test
    fun constructorReturnsMemoWhenBodyIsProvidedAndTitleIsBlank() {
        // Act
        val memo = Memo(
            id = MemoId("memo-1"),
            title = MemoTitle(" "),
            body = MemoBody("Body"),
            createdAt = TimestampMillis(1000L),
            updatedAt = TimestampMillis(1000L)
        )

        // Assert
        assertEquals("Body", memo.body.value)
    }

    @Test
    fun constructorThrowsWhenTitleAndBodyAreBlank() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            Memo(
                id = MemoId("memo-1"),
                title = MemoTitle(" "),
                body = MemoBody(" "),
                createdAt = TimestampMillis(1000L),
                updatedAt = TimestampMillis(1000L)
            )
        }
    }

}
