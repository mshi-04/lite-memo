package com.appvoyager.litememo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.appvoyager.litememo.data.model.DraftKeys
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.repository.MemoEditDraftRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class DataStoreMemoEditDraftRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : MemoEditDraftRepository {

    override suspend fun getDraft(target: MemoEditDraftTarget): MemoEditDraft? {
        val prefs = dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }.first()
        val keys = keys(target)
        val title = prefs[keys.title] ?: return null
        val body = prefs[keys.body] ?: return null
        val tagIds = prefs[keys.tagIds]
            .orEmpty()
            .split(TAG_ID_SEPARATOR, LEGACY_TAG_ID_SEPARATOR)
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { TagId(it) }.getOrNull() }

        return MemoEditDraft(
            target = target,
            title = MemoTitle(title),
            body = MemoBody(body),
            createdAt = prefs[keys.createdAt]?.let {
                runCatching { TimestampMillis(it) }.getOrNull()
            },
            tagIds = tagIds,
            isFavorite = prefs[keys.isFavorite] ?: false
        )
    }

    override suspend fun saveDraft(draft: MemoEditDraft) {
        val keys = keys(draft.target)
        dataStore.edit { prefs ->
            prefs[keys.title] = draft.title.value
            prefs[keys.body] = draft.body.value
            prefs[keys.tagIds] = draft.tagIds.joinToString(TAG_ID_SEPARATOR) { it.value }
            prefs[keys.isFavorite] = draft.isFavorite
            val createdAt = draft.createdAt
            if (createdAt == null) {
                prefs.remove(keys.createdAt)
            } else {
                prefs[keys.createdAt] = createdAt.value
            }
        }
    }

    override suspend fun clearDraft(target: MemoEditDraftTarget) {
        val keys = keys(target)
        dataStore.edit { prefs ->
            prefs.remove(keys.title)
            prefs.remove(keys.body)
            prefs.remove(keys.tagIds)
            prefs.remove(keys.isFavorite)
            prefs.remove(keys.createdAt)
        }
    }

    private fun keys(target: MemoEditDraftTarget): DraftKeys {
        val prefix = "$KEY_PREFIX${target.value}_"
        return DraftKeys(
            title = stringPreferencesKey("${prefix}title"),
            body = stringPreferencesKey("${prefix}body"),
            tagIds = stringPreferencesKey("${prefix}tag_ids"),
            isFavorite = booleanPreferencesKey("${prefix}is_favorite"),
            createdAt = longPreferencesKey("${prefix}created_at")
        )
    }

    private companion object {
        const val KEY_PREFIX = "memo_edit_draft_"
        const val TAG_ID_SEPARATOR = ","
        const val LEGACY_TAG_ID_SEPARATOR = "\n"
    }
}
