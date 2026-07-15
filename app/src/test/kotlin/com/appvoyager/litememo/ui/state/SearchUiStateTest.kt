package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.value.MemoId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SearchUiStateTest {

    @Test
    fun normalConstructorKeepsActiveQuery() {
        // Act
        // Normal: active search keeps its query.
        val search = SearchUiState(
            isActive = true,
            query = "shopping"
        )

        // Assert
        assertEquals("shopping", search.query)
    }

    @Test
    fun normalConstructorKeepsActiveResults() {
        // Arrange
        val results = listOf(memoUiModel())

        // Act
        // Normal: active search keeps its results.
        val search = SearchUiState(
            isActive = true,
            results = results
        )

        // Assert
        assertEquals(results, search.results)
    }

    @Test
    fun errorConstructorThrowsWhenInactiveQueryIsNotEmpty() {
        // Act & Assert
        // Error: inactive search cannot retain a query.
        assertThrows(IllegalArgumentException::class.java) {
            SearchUiState(query = "shopping")
        }
    }

    @Test
    fun errorConstructorThrowsWhenInactiveResultsAreNotEmpty() {
        // Arrange
        val results = listOf(memoUiModel())

        // Act & Assert
        // Error: inactive search cannot retain results.
        assertThrows(IllegalArgumentException::class.java) {
            SearchUiState(results = results)
        }
    }

    private fun memoUiModel() = MemoUiModel(
        id = MemoId("memo-1"),
        title = "Shopping",
        body = "Buy coffee",
        tags = emptyList(),
        updatedAtMillis = 1_000L,
        isFavorite = false
    )

}
