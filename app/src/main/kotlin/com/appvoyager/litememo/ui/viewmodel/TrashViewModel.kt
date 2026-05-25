package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.usecase.DeleteMemoPermanentlyUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.PurgeExpiredTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.RestoreMemoFromTrashUseCase
import com.appvoyager.litememo.ui.state.TagUiModel
import com.appvoyager.litememo.ui.state.TrashUiState
import com.appvoyager.litememo.ui.state.TrashedMemoUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val observeTrashedMemosUseCase: ObserveTrashedMemosUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val restoreMemoFromTrashUseCase: RestoreMemoFromTrashUseCase,
    private val deleteMemoPermanentlyUseCase: DeleteMemoPermanentlyUseCase,
    private val purgeExpiredTrashedMemosUseCase: PurgeExpiredTrashedMemosUseCase
) : ViewModel() {

    init {
        purgeExpiredTrashedMemos()
    }

    private val retryTrigger = MutableStateFlow(0)
    private val hasActionError = MutableStateFlow(false)
    private val permanentDeleteDialog = MutableStateFlow<TrashedMemoUiModel?>(null)

    private val observedData = retryTrigger.flatMapLatest {
        combine(
            observeTrashedMemosUseCase()
                .map<List<Memo>, List<Memo>?> { it }
                .catch { emit(null) },
            observeTagsUseCase()
                .map<List<Tag>, List<Tag>?> { it }
                .catch { emit(null) }
        ) { memos, tags ->
            ObservedTrashData(memos = memos, tags = tags)
        }
    }

    val uiState: StateFlow<TrashUiState> = combine(
        observedData,
        hasActionError,
        permanentDeleteDialog
    ) { observed, actionError, dialog ->
        val hasError = observed.memos == null || observed.tags == null
        TrashUiState(
            isLoading = false,
            hasError = hasError,
            hasActionError = actionError,
            memos = if (!hasError) {
                toUiModels(
                    memos = requireNotNull(observed.memos),
                    tags = requireNotNull(observed.tags)
                )
            } else {
                emptyList()
            },
            showPermanentDeleteDialog = dialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrashUiState()
    )

    fun restoreMemo(id: String) {
        viewModelScope.launch {
            try {
                restoreMemoFromTrashUseCase(MemoId(id))
                hasActionError.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                hasActionError.value = true
            }
        }
    }

    fun requestPermanentDelete(memo: TrashedMemoUiModel) {
        permanentDeleteDialog.value = memo
    }

    fun dismissPermanentDeleteDialog() {
        permanentDeleteDialog.value = null
    }

    fun confirmPermanentDelete() {
        val memo = permanentDeleteDialog.value ?: return
        viewModelScope.launch {
            try {
                deleteMemoPermanentlyUseCase(MemoId(memo.id))
                permanentDeleteDialog.value = null
                hasActionError.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                permanentDeleteDialog.value = null
                hasActionError.value = true
            }
        }
    }

    fun dismissActionError() {
        hasActionError.value = false
    }

    fun retry() {
        hasActionError.value = false
        retryTrigger.update { it + 1 }
    }

    private fun purgeExpiredTrashedMemos() {
        viewModelScope.launch {
            try {
                purgeExpiredTrashedMemosUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    private fun toUiModels(memos: List<Memo>, tags: List<Tag>): List<TrashedMemoUiModel> {
        val tagsById = tags.associateBy { it.id }
        return memos.mapNotNull { memo ->
            val deletedAt = memo.deletedAt ?: return@mapNotNull null
            TrashedMemoUiModel(
                id = memo.id.value,
                title = memo.title.value,
                body = memo.body.value,
                tags = memo.tagIds.mapNotNull { id ->
                    tagsById[id]?.let { TagUiModel.fromDomain(it) }
                },
                deletedAtMillis = deletedAt.value
            )
        }
    }

    private data class ObservedTrashData(val memos: List<Memo>?, val tags: List<Tag>?)
}
