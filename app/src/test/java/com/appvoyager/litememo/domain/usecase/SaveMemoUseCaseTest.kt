package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import com.appvoyager.litememo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaveMemoUseCaseTest {

    @Test
    fun invokeReturnsFailureWhenMemoDoesNotExist() = runTest {
        val useCase = SaveMemoUseCase(
            memoRepository = FakeMemoRepository(),
            memoIdProvider = FixedMemoIdProvider(),
            currentTimeProvider = FixedCurrentTimeProvider()
        )

        val result = useCase(
            SaveMemoCommand(
                id = MemoId("missing-memo"),
                title = MemoTitle("title"),
                body = MemoBody("")
            )
        )

        assertEquals(SaveMemoError.MemoNotFound(MemoId("missing-memo")), result.exceptionOrNull())
    }

    @Test
    fun invokeDelegatesSaveToTransactionalRepository() = runTest {
        val memoRepository = FakeMemoRepository()
        val useCase = SaveMemoUseCase(
            memoRepository = memoRepository,
            memoIdProvider = FixedMemoIdProvider(),
            currentTimeProvider = FixedCurrentTimeProvider()
        )

        val result = useCase(
            SaveMemoCommand(
                title = MemoTitle("title"),
                body = MemoBody("")
            )
        )

        assertEquals(result.getOrThrow(), memoRepository.savedMemo)
    }

    private class FakeMemoRepository : MemoRepository {

        var savedMemo: Memo? = null

        override fun observeMemos(): Flow<List<Memo>> = emptyFlow()

        override suspend fun getMemo(id: MemoId): Memo? = null

        override suspend fun saveMemo(memo: Memo) {
            savedMemo = memo
        }

        override suspend fun saveMemoWithTagCheck(memo: Memo): Result<Unit> {
            savedMemo = memo
            return Result.success(Unit)
        }

        override suspend fun deleteMemo(id: MemoId) = Unit
    }

    private class FixedMemoIdProvider : MemoIdProvider {

        override fun newMemoId(): MemoId = MemoId("new-memo")
    }

    private class FixedCurrentTimeProvider : CurrentTimeProvider {

        override fun now(): TimestampMillis = TimestampMillis(1L)
    }
}
