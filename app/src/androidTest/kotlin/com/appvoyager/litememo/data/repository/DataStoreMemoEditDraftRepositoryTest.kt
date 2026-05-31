package com.appvoyager.litememo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreMemoEditDraftRepositoryTest {

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreMemoEditDraftRepository

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

        repository = DataStoreMemoEditDraftRepository(dataStore)
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
        val draft = memoEditDraft(
            title = "Title",
            body = "Body",
            createdAt = TimestampMillis(1000L),
            tagIds = listOf(TagId("tag-1")),
            isFavorite = true
        )

        repository.saveDraft(draft)

        assertEquals(draft, repository.getDraft(draft.target))
    }

    @Test
    fun clearDraftRemovesSavedDraft() = runTest {
        val draft = memoEditDraft(title = "Title")
        repository.saveDraft(draft)

        repository.clearDraft(draft.target)

        assertEquals(null, repository.getDraft(draft.target))
    }

    @Test
    fun getDraftKeepsTargetsSeparated() = runTest {
        val newDraft = memoEditDraft(
            target = MemoEditDraftTarget.newMemo(null),
            title = "New"
        )
        val existingDraft = memoEditDraft(
            target = MemoEditDraftTarget.existingMemo(MemoId("new_default")),
            title = "Existing"
        )

        repository.saveDraft(newDraft)
        repository.saveDraft(existingDraft)

        assertEquals(
            listOf(newDraft, existingDraft),
            listOf(
                repository.getDraft(newDraft.target),
                repository.getDraft(existingDraft.target)
            )
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
