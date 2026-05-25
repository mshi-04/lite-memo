package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.DeleteMemoPermanentlyUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.PurgeExpiredTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.RestoreMemoFromTrashUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class TrashViewModelTest {

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
    fun uiStateShowsTrashedMemos() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", title = "Trash", deletedAt = 2_000L)
        val viewModel = trashViewModel(memoRepository = FakeMemoRepository(listOf(memo)))

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(listOf("Trash"), state.memos.map { it.title })
    }

    @Test
    fun restoreMemoDelegatesMemoId() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val repository = FakeMemoRepository(listOf(memo))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()

        // Act
        viewModel.restoreMemo(MemoId("memo-1"))
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(memo.id), repository.restoredIds)
    }

    @Test
    fun confirmPermanentDeleteDelegatesSelectedMemoId() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val repository = FakeMemoRepository(listOf(memo))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        val trashedMemo = viewModel.uiState.first { it.memos.isNotEmpty() }.memos.single()

        // Act
        viewModel.requestPermanentDelete(trashedMemo)
        viewModel.confirmPermanentDelete()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(memo.id), repository.permanentlyDeletedIds)
    }

    @Test
    fun initTriggersExpiredTrashPurge() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMemoRepository(listOf(memoFixture(deletedAt = 2_000L)))

        // Act
        trashViewModel(memoRepository = repository)
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(TimestampMillis(0L)), repository.purgeCutoffs)
    }

    private fun trashViewModel(
        memoRepository: FakeMemoRepository = FakeMemoRepository(),
        tagRepository: FakeTagRepository = FakeTagRepository(listOf(tagFixture()))
    ) = TrashViewModel(
        observeTrashedMemosUseCase = ObserveTrashedMemosUseCase(memoRepository),
        observeTagsUseCase = ObserveTagsUseCase(tagRepository),
        restoreMemoFromTrashUseCase = RestoreMemoFromTrashUseCase(memoRepository),
        deleteMemoPermanentlyUseCase = DeleteMemoPermanentlyUseCase(memoRepository),
        purgeExpiredTrashedMemosUseCase = PurgeExpiredTrashedMemosUseCase(
            memoRepository = memoRepository,
            currentTimeProvider = MutableTimeProvider(TimestampMillis(0L))
        )
    )
}
