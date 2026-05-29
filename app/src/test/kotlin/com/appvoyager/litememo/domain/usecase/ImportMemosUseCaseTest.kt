package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.SearchQuery
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import com.appvoyager.litememo.domain.tagFixture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ImportMemosUseCaseTest {

    @Test
    fun invokeSavesTagsAndMemos() = runTest {
        // Arrange
        val tag = tagFixture(id = "t1", name = "Tag")
        val memo = memoFixture(id = "m1", tagIds = listOf(TagId("t1")))
        val data = exportData(tags = listOf(tag), memos = listOf(memo))
        val tagRepo = FakeTagRepository()
        val memoRepo = FakeMemoRepository()
        val useCase = importMemosUseCase(memoRepository = memoRepo, tagRepository = tagRepo)

        // Act
        useCase(data)

        // Assert
        assertEquals(1, tagRepo.currentTags().size)
        assertEquals(1, memoRepo.currentMemos().size)
    }

    @Test
    fun invokeSavesTagsBeforeMemos() = runTest {
        // Arrange
        val callOrder = mutableListOf<String>()
        val tagRepo = OrderTrackingTagRepository(callOrder)
        val memoRepo = OrderTrackingMemoRepository(callOrder)
        val tag = tagFixture(id = "t1")
        val memo = memoFixture(id = "m1", tagIds = listOf(TagId("t1")))
        val useCase = ImportMemosUseCase(
            memoRepository = memoRepo,
            tagRepository = tagRepo
        )

        // Act
        useCase(exportData(tags = listOf(tag), memos = listOf(memo)))

        // Assert
        assertEquals(listOf("tags", "memos"), callOrder)
    }

    @Test
    fun invokeRemovesInvalidTagIdsFromImportedMemos() = runTest {
        // Arrange
        val tag = tagFixture(id = "t1")
        val memo = memoFixture(id = "m1", tagIds = listOf(TagId("t1"), TagId("missing")))
        val memoRepo = FakeMemoRepository()
        val useCase = importMemosUseCase(memoRepository = memoRepo)

        // Act
        useCase(exportData(tags = listOf(tag), memos = listOf(memo)))

        // Assert
        assertEquals(listOf(TagId("t1")), memoRepo.currentMemos()[0].tagIds)
    }

    @Test
    fun invokeHandlesEmptyExportData() = runTest {
        // Arrange
        val memoRepo = FakeMemoRepository()
        val tagRepo = FakeTagRepository()
        val useCase = importMemosUseCase(memoRepository = memoRepo, tagRepository = tagRepo)

        // Act
        useCase(exportData())

        // Assert
        assertEquals(0, memoRepo.currentMemos().size)
        assertEquals(0, tagRepo.currentTags().size)
    }

    @Test
    fun invokeThrowsForUnsupportedVersion() {
        // Arrange
        val useCase = importMemosUseCase()
        val data = ExportData(
            version = 999,
            exportedAt = TimestampMillis(1000L),
            tags = emptyList(),
            memos = emptyList()
        )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(data) }
        }
    }

    @Test
    fun invokePreservesValidTagIdsWhenSomeAreInvalid() = runTest {
        // Arrange
        val tag1 = tagFixture(id = "t1")
        val tag2 = tagFixture(id = "t2", name = "Tag2")
        val memo = memoFixture(
            id = "m1",
            tagIds = listOf(TagId("t1"), TagId("missing"), TagId("t2"))
        )
        val memoRepo = FakeMemoRepository()
        val useCase = importMemosUseCase(memoRepository = memoRepo)

        // Act
        useCase(exportData(tags = listOf(tag1, tag2), memos = listOf(memo)))

        // Assert
        assertEquals(listOf(TagId("t1"), TagId("t2")), memoRepo.currentMemos()[0].tagIds)
    }

    @Test
    fun invokeUpsertsMemoWithSameId() = runTest {
        // Arrange
        val existing = memoFixture(id = "m1", title = "Old")
        val imported = memoFixture(id = "m1", title = "New")
        val memoRepo = FakeMemoRepository(listOf(existing))
        val useCase = importMemosUseCase(memoRepository = memoRepo)

        // Act
        useCase(exportData(memos = listOf(imported)))

        // Assert
        assertEquals("New", memoRepo.currentMemos().first { it.id.value == "m1" }.title.value)
    }

    private fun importMemosUseCase(
        memoRepository: FakeMemoRepository = FakeMemoRepository(),
        tagRepository: FakeTagRepository = FakeTagRepository()
    ) = ImportMemosUseCase(
        memoRepository = memoRepository,
        tagRepository = tagRepository
    )

    private fun exportData(tags: List<Tag> = emptyList(), memos: List<Memo> = emptyList()) =
        ExportData(
            version = ExportMemosUseCase.CURRENT_VERSION,
            exportedAt = TimestampMillis(1000L),
            tags = tags,
            memos = memos
        )

    private class OrderTrackingMemoRepository(private val callOrder: MutableList<String>) :
        MemoRepository {
        override fun observeActiveMemos(): Flow<List<Memo>> = flowOf(emptyList())
        override fun observeActiveMemosBySearchQuery(query: SearchQuery): Flow<List<Memo>> =
            flowOf(emptyList())
        override fun observeActiveMemosCreatedBetween(
            from: TimestampMillis,
            to: TimestampMillis
        ): Flow<List<Memo>> = flowOf(emptyList())
        override fun observeTrashedMemos(): Flow<List<Memo>> = flowOf(emptyList())
        override suspend fun getActiveMemo(id: MemoId): Memo? = null
        override suspend fun saveMemo(memo: Memo) = Unit
        override suspend fun moveMemoToTrash(id: MemoId, deletedAt: TimestampMillis) = Unit
        override suspend fun restoreMemoFromTrash(id: MemoId) = Unit
        override suspend fun deleteMemoPermanently(id: MemoId) = Unit
        override suspend fun deleteTrashedMemosDeletedAtOrBefore(cutoff: TimestampMillis) = Unit
        override suspend fun getAllActiveMemos(): List<Memo> = emptyList()
        override suspend fun saveAllMemos(memos: List<Memo>) {
            callOrder += "memos"
        }
    }

    private class OrderTrackingTagRepository(private val callOrder: MutableList<String>) :
        TagRepository {
        override fun observeTags(): Flow<List<Tag>> = flowOf(emptyList())
        override suspend fun getTag(id: TagId): Tag? = null
        override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> = emptyList()
        override suspend fun saveTag(tag: Tag) = Unit
        override suspend fun deleteTag(id: TagId) = Unit
        override suspend fun getAllTags(): List<Tag> = emptyList()
        override suspend fun saveAllTags(tags: List<Tag>) {
            callOrder += "tags"
        }
    }

}
