package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.appvoyager.litememo.domain.FakeMemoEditDraftRepository
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueMemoIdProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.ClearMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.MoveMemoToTrashUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoEditViewModelTest {

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
    fun uiStateRestoresSavedStateDraft() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "draftTitle" to "Saved title",
                    "draftBody" to "Saved body",
                    "draftTagIds" to arrayListOf("tag-1"),
                    "draftIsFavorite" to true
                )
            )
        )
        advanceUntilIdle()

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals("Saved title", state.title)
        assertEquals("Saved body", state.body)
        assertEquals(setOf("tag-1"), state.selectedTagIds)
        assertEquals(true, state.isFavorite)
    }

    @Test
    fun uiStateRestoresStoredDraft() = runTest(dispatcher) {
        // Arrange
        val draft = memoEditDraft(
            title = "Stored title",
            body = "Stored body",
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )
        val viewModel = memoEditViewModel(
            draftRepository = FakeMemoEditDraftRepository(listOf(draft))
        )
        advanceUntilIdle()

        // Act
        val state = viewModel.uiState.value

        // Assert
        assertEquals("Stored title", state.title)
        assertEquals("Stored body", state.body)
        assertEquals(setOf("tag-1"), state.selectedTagIds)
        assertEquals(true, state.isFavorite)
    }

    @Test
    fun updateTitleSavesDraftAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Draft")
        advanceTimeBy(1_000L)
        advanceUntilIdle()

        // Assert
        assertEquals(
            MemoTitle("Draft"),
            draftRepository.savedDrafts.single().title
        )
    }

    @Test
    fun requestBackFlushesDraft() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Draft")
        viewModel.requestBack()
        advanceUntilIdle()

        // Assert
        assertEquals(
            MemoTitle("Draft"),
            draftRepository.savedDrafts.single().title
        )
    }

    @Test
    fun saveClearsDraftWhenMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val target = MemoEditDraftTarget.newMemo(null)
        val seededDraft = memoEditDraft(target = target, title = "Seeded")
        val draftRepository = FakeMemoEditDraftRepository(listOf(seededDraft))
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Title")
        viewModel.save()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(target), draftRepository.clearedTargets)
        assertEquals(emptyList<MemoEditDraft>(), draftRepository.currentDrafts())
    }

    @Test
    fun deleteClearsDraftWhenMemoIsDeleted() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val target = MemoEditDraftTarget.existingMemo(memo.id)
        val seededDraft = memoEditDraft(target = target, title = "Seeded")
        val draftRepository = FakeMemoEditDraftRepository(listOf(seededDraft))
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act
        viewModel.delete()
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(target), draftRepository.clearedTargets)
        assertEquals(emptyList<MemoEditDraft>(), draftRepository.currentDrafts())
    }

    @Test
    fun blankDraftClearsDraftAfterDebounce() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository()
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle(" ")
        advanceTimeBy(1_000L)
        advanceUntilIdle()

        // Assert
        assertEquals(listOf(MemoEditDraftTarget.newMemo(null)), draftRepository.clearedTargets)
    }

    @Test
    fun saveDoesNotNavigateBackWhenBlankDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle(" ")
        viewModel.save()
        advanceUntilIdle()
        val event = withTimeoutOrNull(1) { viewModel.navigationEvent.first() }

        // Assert
        assertNull(event)
    }

    @Test
    fun saveEmitsDraftErrorWhenBlankDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle(" ")
        viewModel.save()
        advanceUntilIdle()
        val event = viewModel.draftErrorEvent.first()

        // Assert
        assertEquals(Unit, event)
    }

    @Test
    fun saveNavigatesBackWhenDraftClearFailsAfterMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Title")
        viewModel.save()
        advanceUntilIdle()
        val event = viewModel.navigationEvent.first()

        // Assert
        assertEquals(MemoEditNavigationEvent.NavigateBack, event)
    }

    @Test
    fun saveEmitsDraftErrorWhenDraftClearFailsAfterMemoIsSaved() = runTest(dispatcher) {
        // Arrange
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(draftRepository = draftRepository)
        advanceUntilIdle()

        // Act
        viewModel.updateTitle("Title")
        viewModel.save()
        advanceUntilIdle()
        val event = viewModel.draftErrorEvent.first()

        // Assert
        assertEquals(Unit, event)
    }

    @Test
    fun deleteEmitsMemoDeletedEventWithDeletedMemo() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val viewModel = memoEditViewModel(
            memo = memo
        )
        advanceUntilIdle()

        // Act
        viewModel.delete()
        advanceUntilIdle()
        val event = viewModel.navigationEvent.first()

        // Assert
        assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), event)
    }

    @Test
    fun deleteEmitsMemoDeletedEventWhenDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act
        viewModel.delete()
        advanceUntilIdle()
        val event = viewModel.navigationEvent.first()

        // Assert
        assertEquals(MemoEditNavigationEvent.MemoDeleted(memo.id), event)
    }

    @Test
    fun deleteEmitsDraftErrorWhenDraftClearFails() = runTest(dispatcher) {
        // Arrange
        val memo = memoFixture(id = "memo-1")
        val draftRepository = FakeMemoEditDraftRepository(clearDraftError = IllegalStateException())
        val viewModel = memoEditViewModel(
            memo = memo,
            draftRepository = draftRepository
        )
        advanceUntilIdle()

        // Act
        viewModel.delete()
        advanceUntilIdle()
        val event = viewModel.draftErrorEvent.first()

        // Assert
        assertEquals(Unit, event)
    }

    private fun memoEditViewModel(
        memo: Memo? = null,
        savedStateHandle: SavedStateHandle = memo?.let {
            SavedStateHandle(
                mapOf(
                    "memoId" to it.id.value,
                    "createdAt" to it.createdAt.value
                )
            )
        } ?: SavedStateHandle(),
        draftRepository: FakeMemoEditDraftRepository = FakeMemoEditDraftRepository()
    ): MemoEditViewModel {
        val memoRepository = FakeMemoRepository(listOfNotNull(memo))
        val tagRepository = FakeTagRepository(
            listOf(tagFixture(id = "tag-1"))
        )
        return MemoEditViewModel(
            savedStateHandle = savedStateHandle,
            getMemoUseCase = GetMemoUseCase(memoRepository),
            saveMemoUseCase = SaveMemoUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                memoIdProvider = QueueMemoIdProvider(listOf(MemoId("generated-id"))),
                currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
            ),
            moveMemoToTrashUseCase = MoveMemoToTrashUseCase(
                memoRepository = memoRepository,
                currentTimeProvider = MutableTimeProvider(TimestampMillis(2_000L))
            ),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            getMemoEditDraftUseCase = GetMemoEditDraftUseCase(draftRepository),
            saveMemoEditDraftUseCase = SaveMemoEditDraftUseCase(draftRepository),
            clearMemoEditDraftUseCase = ClearMemoEditDraftUseCase(draftRepository),
            formatMemoTextUseCase = FormatMemoTextUseCase()
        )
    }

    private fun memoEditDraft(
        target: MemoEditDraftTarget = MemoEditDraftTarget.newMemo(null),
        title: String = "",
        body: String = "",
        createdAt: TimestampMillis? = null,
        tagIds: List<TagId> = emptyList(),
        isFavorite: Boolean = false
    ) = MemoEditDraft(
        target = target,
        title = MemoTitle(title),
        body = MemoBody(body),
        createdAt = createdAt,
        tagIds = tagIds,
        isFavorite = isFavorite
    )
}
