package com.appvoyager.litememo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DataStoreMemoEditDraftRepositoryTest {

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreMemoEditDraftRepository

    private val json = Json

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        dataStoreFile = File(
            context.cacheDir,
            "draft_${UUID.randomUUID()}.preferences_pb"
        )

        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile }
        )

        repository = DataStoreMemoEditDraftRepository(dataStore, json)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun getDraftReturnsSavedDraft() = runTest {
        // Arrange
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

    @Test
    fun getDraftRestoresTagIdsContainingSeparatorCharacters() = runTest {
        // Arrange
        val draft = memoEditDraft(
            title = "Title",
            tagIds = listOf(TagId("tag,with,comma"), TagId("tag\nwith\nnewline"))
        )

        // Act
        repository.saveDraft(draft)

        // Assert
        assertEquals(draft, repository.getDraft(draft.target))
    }

    @Test
    fun getDraftDropsTagIdsStoredInLegacyCommaFormat() = runTest {
        // Arrange
        val target = MemoEditDraftTarget.newMemo(null)
        repository.saveDraft(memoEditDraft(target = target, title = "Title", body = "Body"))
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("memo_edit_draft_${target.value}_tag_ids")] = "tag-1,tag-2"
        }

        // Act
        val restored = repository.getDraft(target)

        // Assert
        assertEquals(emptyList<TagId>(), restored?.tagIds)
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
