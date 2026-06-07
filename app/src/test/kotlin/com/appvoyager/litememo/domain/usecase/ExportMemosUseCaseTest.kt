package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportMemosUseCaseTest {

    @Test
    fun invokeReturnsExportDataWithAllActiveMemosAndTags() = runTest {
        // Arrange
        val memos = listOf(
            memoFixture(id = "m1", title = "Memo 1"),
            memoFixture(id = "m2", title = "Memo 2")
        )
        val tags = listOf(
            tagFixture(id = "t1", name = "Tag 1"),
            tagFixture(id = "t2", name = "Tag 2")
        )
        val useCase = exportMemosUseCase(
            memoRepository = FakeMemoRepository(memos),
            tagRepository = FakeTagRepository(tags)
        )

        // Act
        val result = useCase()

        // Assert
        assertEquals(2, result.memos.size)
        assertEquals(2, result.tags.size)
    }

    @Test
    fun invokeExcludesTrashedMemosFromExportData() = runTest {
        // Arrange
        val memos = listOf(
            memoFixture(id = "active", title = "Active"),
            memoFixture(id = "trashed", title = "Trashed", deletedAt = 2000L)
        )
        val useCase = exportMemosUseCase(memoRepository = FakeMemoRepository(memos))

        // Act
        val result = useCase()

        // Assert
        assertEquals(1, result.memos.size)
        assertEquals("active", result.memos[0].id.value)
    }

    @Test
    fun invokeReturnsCurrentVersionAndExportedAt() = runTest {
        // Arrange
        val now = TimestampMillis(5000L)
        val useCase = exportMemosUseCase(timeProvider = MutableTimeProvider(now))

        // Act
        val result = useCase()

        // Assert
        assertEquals(ExportMemosUseCase.CURRENT_VERSION, result.version)
        assertEquals(now, result.exportedAt)
    }

    @Test
    fun invokeReturnsMemosAndTagsInStableExportOrder() = runTest {
        // Arrange
        val memos = listOf(
            memoFixture(id = "m3", createdAt = 3000L),
            memoFixture(id = "m2", createdAt = 1000L),
            memoFixture(id = "m1", createdAt = 1000L)
        )
        val tags = listOf(
            tagFixture(id = "t3", createdAt = 3000L),
            tagFixture(id = "t2", createdAt = 1000L),
            tagFixture(id = "t1", createdAt = 1000L)
        )
        val useCase = exportMemosUseCase(
            memoRepository = FakeMemoRepository(memos),
            tagRepository = FakeTagRepository(tags)
        )

        // Act
        val result = useCase()

        // Assert
        val expected = listOf("m1", "m2", "m3") to listOf("t1", "t2", "t3")
        val actual = result.memos.map { it.id.value } to result.tags.map { it.id.value }
        assertEquals(expected, actual)
    }

    @Test
    fun invokeReturnsEmptyExportDataWhenNoMemosOrTags() = runTest {
        // Arrange
        val useCase = exportMemosUseCase()

        // Act
        val result = useCase()

        // Assert
        assertEquals(0, result.memos.size)
        assertEquals(0, result.tags.size)
    }

    private fun exportMemosUseCase(
        memoRepository: FakeMemoRepository = FakeMemoRepository(),
        tagRepository: FakeTagRepository = FakeTagRepository(),
        timeProvider: MutableTimeProvider = MutableTimeProvider()
    ) = ExportMemosUseCase(
        memoRepository = memoRepository,
        tagRepository = tagRepository,
        currentTimeProvider = timeProvider
    )

}
