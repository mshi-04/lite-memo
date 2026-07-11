package com.appvoyager.litememo.ui.widget.data

import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WidgetMemoLoaderTest {

    private val observeMemosUseCase = mockk<ObserveMemosUseCase>()
    private val loader = WidgetMemoLoader(observeMemosUseCase)

    @Test
    fun normalLoadRecentPreservesUseCaseOrder() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(
            listOf(
                memoFixture(id = "a", title = "A"),
                memoFixture(id = "b", title = "B"),
                memoFixture(id = "c", title = "C")
            )
        )

        // Act
        val items = loader.loadRecent(limit = 10)

        // Assert
        assertEquals(listOf("a", "b", "c"), items.map { it.id })
    }

    @Test
    fun boundaryLoadRecentTruncatesToLimit() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(
            (1..5).map { memoFixture(id = "memo-$it", title = "T$it") }
        )

        // Act
        val items = loader.loadRecent(limit = 2)

        // Assert
        assertEquals(listOf("memo-1", "memo-2"), items.map { it.id })
    }

    @Test
    fun normalLoadRecentReturnsEmptyForNoMemos() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(emptyList())

        // Act
        val items = loader.loadRecent(limit = 8)

        // Assert
        assertTrue(items.isEmpty())
    }

    @Test
    fun normalTitledMemoUsesTitleAsPrimaryAndBodyAsSnippet() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(
            listOf(memoFixture(id = "m", title = "見出し", body = "本文1\n本文2"))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("見出し", item.title)
        assertEquals("本文1 本文2", item.snippet)
    }

    @Test
    fun boundaryBodyOnlyMemoUsesFirstBodyLineAsPrimary() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(
            listOf(memoFixture(id = "m", title = "", body = "先頭行\n2行目"))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("先頭行", item.title)
        assertEquals("2行目", item.snippet)
    }

    @Test
    fun normalObserveRecentEmitsMappedTruncatedItems() = runTest {
        // Arrange
        every { observeMemosUseCase.invoke() } returns flowOf(
            listOf(
                memoFixture(id = "a", title = "A"),
                memoFixture(id = "b", title = "B"),
                memoFixture(id = "c", title = "C")
            )
        )

        // Act
        val items = loader.observeRecent(limit = 2).first()

        // Assert
        assertEquals(listOf("a", "b"), items.map { it.id })
    }

    @Test
    fun boundaryLongTextIsTruncated() = runTest {
        // Arrange
        val longTitle = "あ".repeat(100)
        every { observeMemosUseCase.invoke() } returns flowOf(
            listOf(memoFixture(id = "m", title = longTitle, body = ""))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals(50, item.title.length)
    }
}
