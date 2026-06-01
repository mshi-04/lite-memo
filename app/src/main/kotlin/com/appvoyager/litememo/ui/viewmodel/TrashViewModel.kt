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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val retryTrigger = MutableStateFlow(false)
    private val hasPurgeError = MutableStateFlow(false)
    private val permanentDeleteDialog = MutableStateFlow<TrashedMemoUiModel?>(null)

    // 操作失敗は一回限りの通知で、同一文言の最新イベントだけ届けばよい。
    private val _actionErrorEvent = Channel<Unit>(Channel.CONFLATED)
    val actionErrorEvent = _actionErrorEvent.receiveAsFlow()

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
        hasPurgeError,
        permanentDeleteDialog
    ) { observed, purgeError, dialog ->
        val hasError = observed.memos == null || observed.tags == null || purgeError
        TrashUiState(
            isLoading = false,
            hasError = hasError,
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

    fun restoreMemo(id: MemoId) {
        viewModelScope.launch {
            try {
                restoreMemoFromTrashUseCase(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _actionErrorEvent.trySend(Unit)
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
                deleteMemoPermanentlyUseCase(memo.id)
                permanentDeleteDialog.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                permanentDeleteDialog.value = null
                _actionErrorEvent.trySend(Unit)
            }
        }
    }

    fun retry() {
        hasPurgeError.value = false
        purgeExpiredTrashedMemos()
        retryTrigger.update { !it }
    }

    private fun purgeExpiredTrashedMemos() {
        viewModelScope.launch {
            try {
                purgeExpiredTrashedMemosUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                hasPurgeError.value = true
            }
        }
    }

    private fun toUiModels(memos: List<Memo>, tags: List<Tag>): List<TrashedMemoUiModel> {
        val tagsById = tags.associateBy { it.id }
        return memos.mapNotNull { memo ->
            val deletedAt = memo.deletedAt ?: return@mapNotNull null
            TrashedMemoUiModel(
                id = memo.id,
                title = memo.title.value,
                body = memo.body.value,
                tags = memo.tagIds.mapNotNull { id ->
                    tagsById[id]?.let { TagUiModel.fromDomain(it) }
                },
                deletedAt = deletedAt
            )
        }
    }
}

private data class ObservedTrashData(val memos: List<Memo>?, val tags: List<Tag>?)
