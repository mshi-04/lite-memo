package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.usecase.RestoreMemoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiteMemoAppViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoreMemoEmitsErrorEventWhenRestoreFails() = runTest(dispatcher) {
        // Arrange
        val viewModel = LiteMemoAppViewModel(
            RestoreMemoUseCase(FailingSaveMemoRepository())
        )

        // Act
        viewModel.restoreMemo(memoFixture())
        advanceUntilIdle()
        val event = viewModel.restoreMemoErrorEvent.first()

        // Assert
        assertEquals(Unit, event)
    }

    private class FailingSaveMemoRepository : MemoRepository {

        override fun observeMemos(): Flow<List<Memo>> = flowOf(emptyList())

        override fun observeMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            flowOf(emptyList())

        override fun observeMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = flowOf(emptyList())

        override suspend fun getMemo(id: MemoId): Memo? = null

        override suspend fun saveMemo(memo: Memo) {
            error("Save failed.")
        }

        override suspend fun deleteMemo(id: MemoId) = Unit
    }
}
