package com.appvoyager.litememo.ui.viewmodel

import app.cash.turbine.test
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoSummary
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.DeleteMemoPermanentlyUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.PurgeExpiredTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.RestoreMemoFromTrashUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    fun startSelectionSelectsMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val viewModel = trashViewModel(memoRepository = FakeMemoRepository(listOf(memo)))
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }

        // Act
        viewModel.startSelection(memo.id)
        val state = viewModel.uiState.first { it.selection.isActive }

        // Assert
        assertEquals(setOf(memo.id), state.selection.selectedMemoIds)
    }

    @Test
    fun toggleMemoSelectionRemovesSelectedMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val viewModel = trashViewModel(memoRepository = FakeMemoRepository(listOf(memo)))
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }
        viewModel.startSelection(memo.id)

        // Act
        viewModel.toggleMemoSelection(memo.id)
        val state = viewModel.uiState.first { !it.selection.isActive }

        // Assert
        assertEquals(emptySet<MemoId>(), state.selection.selectedMemoIds)
    }

    @Test
    fun restoreSelectedMemosDelegatesSelectedMemoIds() = runTest(dispatcher) {
        // Arrange
        val memo1 = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val memo2 = memoFixture(id = "memo-2", deletedAt = 3_000L)
        val repository = FakeMemoRepository(listOf(memo1, memo2))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.size == 2 }
        viewModel.startSelection(memo2.id)
        viewModel.toggleMemoSelection(memo1.id)
        viewModel.uiState.first {
            it.selection.selectedMemoIds == setOf(memo2.id, memo1.id)
        }

        // Act
        viewModel.restoreSelectedMemos()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(memo2.id, memo1.id), repository.restoredIds)
    }

    @Test
    fun requestEmptyTrashShowsEmptyTrashDialog() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val repository = FakeMemoRepository(listOf(memo))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }

        // Act
        viewModel.requestEmptyTrash()
        advanceUntilIdle()

        // Assert
        assertEquals(true, viewModel.uiState.value.showEmptyTrashDialog)
    }

    @Test
    fun boundaryRequestEmptyTrashDoesNotShowDialogWhenTrashIsEmpty() = runTest(dispatcher) {
        // Arrange
        val viewModel = trashViewModel(memoRepository = FakeMemoRepository())
        advanceUntilIdle()
        viewModel.uiState.first { !it.isLoading && it.memos.isEmpty() }

        // Act
        viewModel.requestEmptyTrash()
        advanceUntilIdle()

        // Assert
        assertEquals(false, viewModel.uiState.value.showEmptyTrashDialog)
    }

    @Test
    fun confirmEmptyTrashDeletesAllVisibleTrashedMemos() = runTest(dispatcher) {
        // Arrange
        val memo1 = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val memo2 = memoFixture(id = "memo-2", deletedAt = 3_000L)
        val repository = FakeMemoRepository(listOf(memo1, memo2))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }
        viewModel.requestEmptyTrash()

        // Act
        viewModel.confirmEmptyTrash()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(memo2.id, memo1.id), repository.permanentlyDeletedIds)
    }

    @Test
    fun coroutineRapidConfirmEmptyTrashDoesNotEmitErrorFromReentrancy() = runTest(dispatcher) {
        // Arrange
        val memo1 = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val memo2 = memoFixture(id = "memo-2", deletedAt = 3_000L)
        val repository = FakeMemoRepository(listOf(memo1, memo2))
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }
        viewModel.requestEmptyTrash()

        // Act & Assert
        // Coroutine/Boundary: the in-flight guard blocks the second rapid confirm so the
        // already-deleted memos are not re-deleted (which would emit a spurious error).
        viewModel.actionErrorEvent.test {
            viewModel.confirmEmptyTrash()
            viewModel.confirmEmptyTrash()
            advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(listOf(memo2.id, memo1.id), repository.permanentlyDeletedIds)
    }

    @Test
    fun flowConfirmEmptyTrashEmitsActionErrorAndHidesDialogWhenDeleteFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val viewModel = trashViewModel(
            memoRepository = DeleteFailingMemoRepository(listOf(memo))
        )
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }
        viewModel.requestEmptyTrash()
        viewModel.uiState.first { it.showEmptyTrashDialog }

        // Act & Assert
        // Flow/Error/StateTransition: empty-trash failure emits an event and closes the dialog.
        viewModel.actionErrorEvent.test {
            viewModel.confirmEmptyTrash()
            advanceUntilIdle()
            assertEquals(Unit to false, awaitItem() to viewModel.uiState.value.showEmptyTrashDialog)
        }
    }

    @Test
    fun flowRestoreSelectedMemosEmitsActionErrorWhenRestoreFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val viewModel = trashViewModel(
            memoRepository = RestoreFailingMemoRepository(listOf(memo))
        )
        advanceUntilIdle()
        viewModel.uiState.first { it.memos.isNotEmpty() }
        viewModel.startSelection(memo.id)
        viewModel.uiState.first {
            it.selection.selectedMemoIds == setOf(memo.id)
        }

        // Act & Assert
        viewModel.actionErrorEvent.test {
            viewModel.restoreSelectedMemos()
            advanceUntilIdle()
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun flowUiStateHasErrorWhenObserveTrashedMemosFails() = runTest(dispatcher) {
        // Arrange
        val viewModel = trashViewModel(
            memoRepository = ObserveTrashedFailingMemoRepository()
        )

        // Act
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.hasError }

        // Assert
        assertEquals(true to emptyList<MemoId>(), state.hasError to state.memos.map { it.id })
    }

    @Test
    fun stateTransitionRetryClearsPurgeErrorAfterSuccessfulPurge() = runTest(dispatcher) {
        // Arrange
        val repository = PurgeFailingOnceMemoRepository()
        val viewModel = trashViewModel(memoRepository = repository)
        advanceUntilIdle()
        viewModel.uiState.first { it.hasError }

        // Act
        viewModel.retry()
        advanceUntilIdle()
        val state = viewModel.uiState.first { !it.hasError }

        // Assert
        assertEquals(false to 2, state.hasError to repository.purgeAttempts)
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
        memoRepository: MemoRepository = FakeMemoRepository(),
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

    private class RestoreFailingMemoRepository(initialMemos: List<Memo>) : MemoRepository {

        private val repository = FakeMemoRepository(initialMemos)

        override fun observeActiveMemos(): Flow<List<Memo>> = repository.observeActiveMemos()

        override fun observeRecentActiveMemos(limit: Int): Flow<List<MemoSummary>> =
            repository.observeRecentActiveMemos(limit)

        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            repository.observeActiveMemosBySearchQuery(query)

        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = repository.observeActiveMemosCreatedBetween(from, to)

        override fun observeTrashedMemos(): Flow<List<Memo>> = repository.observeTrashedMemos()

        override suspend fun getActiveMemo(id: MemoId): Memo? = repository.getActiveMemo(id)

        override suspend fun saveMemo(memo: Memo) = repository.saveMemo(memo)

        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) =
            repository.moveMemoToTrash(id, deletedAt)

        override suspend fun restoreMemoFromTrash(id: MemoId): Unit =
            error("Failed to restore memo.")

        override suspend fun deleteMemoPermanently(id: MemoId) =
            repository.deleteMemoPermanently(id)

        override suspend fun discardMemo(id: MemoId) = repository.discardMemo(id)

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) =
            repository.deleteTrashedMemosDeletedAtOrBefore(cutoff)

        override suspend fun getAllActiveMemos(): List<Memo> = repository.getAllActiveMemos()

        override suspend fun saveAllMemos(memos: List<Memo>) = repository.saveAllMemos(memos)

        override suspend fun importAll(tags: List<Tag>, memos: List<Memo>) =
            repository.importAll(tags, memos)
    }

    private class DeleteFailingMemoRepository(initialMemos: List<Memo>) :
        MemoRepository by FakeMemoRepository(initialMemos) {
        override suspend fun deleteMemoPermanently(id: MemoId): Unit =
            error("Failed to delete memo.")
    }

    private class ObserveTrashedFailingMemoRepository : MemoRepository by FakeMemoRepository() {
        override fun observeTrashedMemos(): Flow<List<Memo>> = flow {
            throw IllegalStateException("Failed to observe trashed memos.")
        }
    }

    private class PurgeFailingOnceMemoRepository(
        private val delegate: FakeMemoRepository = FakeMemoRepository()
    ) : MemoRepository by delegate {
        var purgeAttempts = 0
            private set

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) {
            purgeAttempts += 1
            if (purgeAttempts == 1) error("Failed to purge trash.")
            delegate.deleteTrashedMemosDeletedAtOrBefore(cutoff)
        }
    }
}
