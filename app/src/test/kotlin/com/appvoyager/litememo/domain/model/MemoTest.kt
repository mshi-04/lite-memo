package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MemoTest {

    @Test
    fun constructorThrowsWhenUpdatedAtIsBeforeCreatedAt() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            Memo(
                id = MemoId("memo-1"),
                title = MemoTitle("Title"),
                body = MemoBody("Body"),
                createdAt = TimestampMillis(2000L),
                updatedAt = TimestampMillis(1000L)
            )
        }
    }

}
