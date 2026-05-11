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
    fun createThrowsWhenUpdatedAtIsBeforeCreatedAt() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            Memo(
                id = MemoId("memo-id"),
                title = MemoTitle("title"),
                body = MemoBody("body"),
                createdAt = TimestampMillis(2L),
                updatedAt = TimestampMillis(1L)
            )
        }

        assertEquals(
            "Memo updatedAt must be greater than or equal to createdAt.",
            exception.message
        )
    }
}
