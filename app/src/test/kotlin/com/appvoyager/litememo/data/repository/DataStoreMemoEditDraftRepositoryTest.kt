package com.appvoyager.litememo.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreMemoEditDraftRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun getDraftReturnsSavedDraft() = runTest {
        // Arrange
        val repository = repository(this)
        val draft = memoEditDraft(
            title = "Title",
            body = "Body",
            createdAt = TimestampMillis(1000L),
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )

        // Act
        repository.saveDraft(draft)

        // Assert
        assertEquals(draft, repository.getDraft(draft.target))
    }

    @Test
    fun clearDraftRemovesSavedDraft() = runTest {
        // Arrange
        val repository = repository(this)
        val draft = memoEditDraft(title = "Title")
        repository.saveDraft(draft)

        // Act
        repository.clearDraft(draft.target)

        // Assert
        assertEquals(null, repository.getDraft(draft.target))
    }

    @Test
    fun getDraftKeepsTargetsSeparated() = runTest {
        // Arrange
        val repository = repository(this)
        val newDraft = memoEditDraft(
            target = MemoEditDraftTarget.newMemo(null),
            title = "New"
        )
        val existingDraft = memoEditDraft(
            target = MemoEditDraftTarget.existingMemo(MemoId("new_default")),
            title = "Existing"
        )

        // Act
        repository.saveDraft(newDraft)
        repository.saveDraft(existingDraft)

        // Assert
        assertEquals(
            listOf(newDraft, existingDraft),
            listOf(
                repository.getDraft(newDraft.target),
                repository.getDraft(existingDraft.target)
            )
        )
    }

    private fun repository(scope: CoroutineScope): DataStoreMemoEditDraftRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempDir, "draft.preferences_pb")
        }
        return DataStoreMemoEditDraftRepository(dataStore)
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
