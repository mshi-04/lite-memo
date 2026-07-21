package com.appvoyager.litememo.data.repository

import com.appvoyager.litememo.data.local.LiteMemoDatabase
import com.appvoyager.litememo.data.local.dao.MemoDao
import com.appvoyager.litememo.data.local.dao.TagDao
import com.appvoyager.litememo.domain.FakeMemoImageStore
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.ExportData
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.ExportMemosUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoomMemoImportRepositoryTest {

    @Test
    fun errorDuplicateMemoIdsAreRejectedBeforeTransaction() {
        // Arrange
        val repository = repository()
        val data = exportData(
            memos = listOf(memoFixture(id = "memo"), memoFixture(id = "memo"))
        )

        // Act & Assert
        // Error: duplicate memo ids remain invalid at the persistence boundary.
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.import(data) }
        }
    }

    @Test
    fun errorDuplicateTagIdsAreRejectedBeforeTransaction() {
        // Arrange
        val repository = repository()
        val data = exportData(
            tags = listOf(tagFixture(id = "tag"), tagFixture(id = "tag"))
        )

        // Act & Assert
        // Error: duplicate tag ids remain invalid at the persistence boundary.
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.import(data) }
        }
    }

    private fun repository() = RoomMemoImportRepository(
        memoDao = mockk<MemoDao>(relaxed = true),
        tagDao = mockk<TagDao>(relaxed = true),
        database = mockk<LiteMemoDatabase>(relaxed = true),
        memoImageStore = FakeMemoImageStore()
    )

    private fun exportData(tags: List<Tag> = emptyList(), memos: List<Memo> = emptyList()) =
        ExportData(
            version = ExportMemosUseCase.CURRENT_VERSION,
            exportedAt = TimestampMillis(1_000L),
            tags = tags,
            memos = memos
        )

}
