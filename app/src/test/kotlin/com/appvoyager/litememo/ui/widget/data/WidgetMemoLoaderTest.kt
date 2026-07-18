package com.appvoyager.litememo.ui.widget.data

import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.usecase.ObserveRecentMemosUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WidgetMemoLoaderTest {

    private val observeRecentMemosUseCase = mockk<ObserveRecentMemosUseCase>()
    private val loader = WidgetMemoLoader(observeRecentMemosUseCase)

    @Test
    fun normalLoadRecentMapsUseCaseOrder() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(
                memoFixture(id = "a", title = "A"),
                memoFixture(id = "b", title = "B"),
                memoFixture(id = "c", title = "C")
            )
        )

        // Act
        val items = loader.loadRecent(limit = 8)

        // Assert
        assertEquals(listOf(MemoId("a"), MemoId("b"), MemoId("c")), items.map { it.id })
    }

    @Test
    fun interactionLoadRecentRequestsUseCaseWithLimit() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(emptyList())

        // Act
        // Interaction: the limit is forwarded to the use case (truncation lives in SQL)
        loader.loadRecent(limit = 5)

        // Assert
        verify { observeRecentMemosUseCase.invoke(5) }
    }

    @Test
    fun normalLoadRecentReturnsEmptyForNoMemos() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(emptyList())

        // Act
        val items = loader.loadRecent(limit = 8)

        // Assert
        assertTrue(items.isEmpty())
    }

    @Test
    fun normalObserveRecentEmitsMappedItems() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(
                memoFixture(id = "a", title = "A"),
                memoFixture(id = "b", title = "B")
            )
        )

        // Act
        val items = loader.observeRecent(limit = 8).first()

        // Assert
        assertEquals(listOf(MemoId("a"), MemoId("b")), items.map { it.id })
    }

    @Test
    fun normalFavoriteFlagIsMapped() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = "A", isFavorite = true))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertTrue(item.isFavorite)
    }

    @Test
    fun normalTitledMemoUsesTitleAsPrimaryAndBodyAsSnippet() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
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
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = "", body = "先頭行\n2行目"))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("先頭行", item.title)
        assertEquals("2行目", item.snippet)
    }

    @Test
    fun boundaryUntitledSingleLongLineKeepsRemainderInSnippet() = runTest {
        // Arrange
        // Boundary: untitled single line longer than the title limit must not lose the tail
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = "", body = "x".repeat(100)))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("x".repeat(50), item.title)
        assertEquals("x".repeat(50), item.snippet)
    }

    @Test
    fun boundaryLongTitleTruncatedToMax() = runTest {
        // Arrange
        val longTitle = "あ".repeat(100)
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = longTitle, body = ""))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals(50, item.title.length)
    }

    @Test
    fun boundaryLongSnippetTruncatedToMax() = runTest {
        // Arrange
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = "T", body = "c".repeat(200)))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals(80, item.snippet.length)
    }

    @Test
    fun boundarySurrogatePairTitleIsNotSplit() = runTest {
        // Arrange
        // Boundary: an emoji straddling the 50-char limit must not leave a lone surrogate
        val title = "a".repeat(49) + "😀"
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = title, body = ""))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("a".repeat(49), item.title)
        assertFalse(item.title.last().isHighSurrogate())
    }

    @Test
    fun boundarySurrogatePairSnippetIsNotSplit() = runTest {
        // Arrange
        val body = "b".repeat(79) + "😀"
        every { observeRecentMemosUseCase.invoke(any()) } returns flowOf(
            listOf(memoFixture(id = "m", title = "T", body = body))
        )

        // Act
        val item = loader.loadRecent(limit = 1).single()

        // Assert
        assertEquals("b".repeat(79), item.snippet)
        assertFalse(item.snippet.last().isHighSurrogate())
    }
}
