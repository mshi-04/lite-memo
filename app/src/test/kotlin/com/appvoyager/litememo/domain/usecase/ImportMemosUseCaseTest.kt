package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoRepository
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
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
        val memoRepo = FakeMemoRepository()
        val useCase = importMemosUseCase(memoRepository = memoRepo)

        // Act
        useCase(data)

        // Assert
        assertEquals(1, memoRepo.importedTags.size)
        assertEquals(1, memoRepo.currentMemos().size)
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
        val useCase = importMemosUseCase(memoRepository = memoRepo)

        // Act
        useCase(exportData())

        // Assert
        assertEquals(0, memoRepo.importedTags.size)
        assertEquals(0, memoRepo.currentMemos().size)
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
    fun invokeRemovesDuplicateTagIdsFromImportedMemos() = runTest {
        // Arrange
        val tag1 = tagFixture(id = "t1")
        val tag2 = tagFixture(id = "t2", name = "Tag2")
        val memo = memoFixture(
            id = "m1",
            tagIds = listOf(TagId("t1"), TagId("t2"), TagId("t1"))
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

    private fun importMemosUseCase(memoRepository: FakeMemoRepository = FakeMemoRepository()) =
        ImportMemosUseCase(memoRepository = memoRepository)

    private fun exportData(tags: List<Tag> = emptyList(), memos: List<Memo> = emptyList()) =
        ExportData(
            version = ExportMemosUseCase.CURRENT_VERSION,
            exportedAt = TimestampMillis(1000L),
            tags = tags,
            memos = memos
        )

}
