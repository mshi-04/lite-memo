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
import com.appvoyager.litememo.domain.usecase.RestoreMemoUseCase
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
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
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
    fun requestBackDoesNotNavigateWhenDeleteIsPending() = runTest(dispatcher) {
        // Arrange
        val viewModel = memoEditViewModel(
            memo = memoFixture(id = "memo-1")
        )
        advanceUntilIdle()
        viewModel.delete()
        advanceUntilIdle()
        viewModel.navigationEvent.first()

        // Act
        viewModel.requestBack()
        advanceUntilIdle()
        val event = withTimeoutOrNull(10) { viewModel.navigationEvent.first() }

        // Assert
        assertNull(event)
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
            restoreMemoUseCase = RestoreMemoUseCase(memoRepository),
            observeTagsUseCase = ObserveTagsUseCase(tagRepository)
        )
    }
}
