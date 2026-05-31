package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
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
    fun restoreMemoEmitsActionErrorWhenRestoreFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1", deletedAt = 2_000L)
        val viewModel = trashViewModel(
            memoRepository = RestoreFailingMemoRepository(listOf(memo))
        )
        advanceUntilIdle()

        // Act
        viewModel.restoreMemo(memo.id)
        advanceUntilIdle()
        val event = viewModel.actionErrorEvent.first()

        // Assert
        assertEquals(Unit, event)
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
            throw IllegalStateException("Failed to restore memo.")

        override suspend fun deleteMemoPermanently(id: MemoId) =
            repository.deleteMemoPermanently(id)

        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) =
            repository.deleteTrashedMemosDeletedAtOrBefore(cutoff)

        override suspend fun getAllActiveMemos(): List<Memo> = repository.getAllActiveMemos()

        override suspend fun saveAllMemos(memos: List<Memo>) = repository.saveAllMemos(memos)

        override suspend fun importAll(tags: List<Tag>, memos: List<Memo>) =
            repository.importAll(tags, memos)
    }
}
