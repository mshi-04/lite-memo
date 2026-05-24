package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueMemoIdProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.usecase.DeleteMemoUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
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
        assertEquals(MemoEditNavigationEvent.MemoDeleted(memo), event)
    }

    private fun memoEditViewModel(memo: Memo): MemoEditViewModel {
        val memoRepository = FakeMemoRepository(listOf(memo))
        val tagRepository = FakeTagRepository()
        return MemoEditViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "memoId" to memo.id.value,
                    "createdAt" to memo.createdAt.value
                )
            ),
            getMemoUseCase = GetMemoUseCase(memoRepository),
            saveMemoUseCase = SaveMemoUseCase(
                memoRepository = memoRepository,
                tagRepository = tagRepository,
                memoIdProvider = QueueMemoIdProvider(),
                currentTimeProvider = MutableTimeProvider(TimestampMillis(2000L))
            ),
            deleteMemoUseCase = DeleteMemoUseCase(memoRepository),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository)
        )
    }
}
