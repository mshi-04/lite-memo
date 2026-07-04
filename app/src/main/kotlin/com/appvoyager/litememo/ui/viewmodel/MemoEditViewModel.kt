package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoEditDraft
import com.appvoyager.litememo.domain.model.MemoEditDraftTarget
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.usecase.ClearMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.MoveMemoToTrashUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoEditDraftUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
import com.appvoyager.litememo.ui.state.MemoEditUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getMemoUseCase: GetMemoUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val moveMemoToTrashUseCase: MoveMemoToTrashUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val getMemoEditDraftUseCase: GetMemoEditDraftUseCase,
    private val saveMemoEditDraftUseCase: SaveMemoEditDraftUseCase,
    private val clearMemoEditDraftUseCase: ClearMemoEditDraftUseCase,
    private val formatMemoTextUseCase: FormatMemoTextUseCase
) : ViewModel() {

    private var memoId: String? = savedStateHandle["memoId"]
    private val createdAtMillis: Long = savedStateHandle["createdAt"] ?: -1L
    private val createdAt: TimestampMillis? = if (createdAtMillis >= 0) {
        TimestampMillis(createdAtMillis)
    } else {
        null
    }
    private val initialDraftTarget: MemoEditDraftTarget = memoId
        ?.let { MemoEditDraftTarget.existingMemo(MemoId(it)) }
        ?: MemoEditDraftTarget.newMemo(createdAt)

    private val _uiState = MutableStateFlow(MemoEditUiState(memoId = memoId))
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<MemoEditNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _draftErrorEvent = Channel<Unit>(Channel.CONFLATED)
    val draftErrorEvent = _draftErrorEvent.receiveAsFlow()

    private val _operationErrorEvent = Channel<MemoEditOperationErrorEvent>(Channel.BUFFERED)
    val operationErrorEvent = _operationErrorEvent.receiveAsFlow()

    private var autosaveJob: Job? = null
    private var shouldPersistDraft = false

    init {
        loadInitialState()
        observeTagsUseCase()
            .onEach { tags ->
                _uiState.update { state ->
                    val validTagIds = tags.map { it.id.value }.toSet()
                    val selectedTagIds = state.selectedTagIds.intersect(validTagIds)
                    state.copy(
                        availableTags = tags.map { TagUiModel.fromDomain(it) },
                        selectedTagIds = selectedTagIds
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun reload() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadInitialState()
    }

    private fun loadInitialState() {
        val memoIdAtStart = memoId
        if (memoIdAtStart != null) {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
        }
        viewModelScope.launch {
            val savedDraft = savedStateDraft(memoIdAtStart)
            if (savedDraft != null) {
                applyDraft(savedDraft, memoIdAtStart)
                return@launch
            }

            val storedDraft = loadStoredDraft(memoIdAtStart)
            if (storedDraft != null) {
                // 非同期の下書きロード中にユーザーが入力済みなら上書きしない。
                if (_uiState.value.isModified) return@launch
                applyDraft(storedDraft, memoIdAtStart)
                persistSavedState(storedDraft.toUiState(memoIdAtStart))
                return@launch
            }

            val currentMemoId = memoIdAtStart ?: return@launch
            try {
                val memo = getMemoUseCase(MemoId(currentMemoId))
                if (memo == null) {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                    return@launch
                }
                _uiState.update { current ->
                    memo.toUiState().copy(availableTags = current.availableTags)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun formatMemoText(): String? {
        val state = _uiState.value
        return formatMemoTextUseCase(state.title, state.body)
    }

    fun updateTitle(title: String) {
        updateDraftState { it.copy(title = title, isModified = true) }
    }

    fun updateBody(body: String) {
        updateDraftState { it.copy(body = body, isModified = true) }
    }

    fun toggleTag(tagId: String) {
        updateDraftState { state ->
            val newIds = if (tagId in state.selectedTagIds) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = newIds, isModified = true)
        }
    }

    fun save() {
        if (_uiState.value.isSaving) return
        val state = _uiState.value
        val title = state.title
        val body = state.body
        if (title.isBlank() && body.isBlank()) {
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                autosaveJob?.cancel()
                try {
                    clearDraft()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // 空メモ save は draft 破棄なので、失敗時は画面に留めて再試行させる。
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                clearSavedState()
                shouldPersistDraft = false
                _uiState.update { it.copy(isSaving = false) }
                _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val savedMemo = saveMemoUseCase(
                    SaveMemoCommand(
                        id = memoId?.let { MemoId(it) },
                        title = MemoTitle(title),
                        body = MemoBody(body),
                        createdAt = createdAt,
                        tagIds = state.selectedTagIds.map { TagId(it) },
                        isFavorite = state.isFavorite
                    )
                )
                applySavedMemo(savedMemo)
                autosaveJob?.cancel()
                clearDraftAfterCompletedOperation()
                clearSavedState()
                shouldPersistDraft = false
                _uiState.update { it.copy(isSaving = false) }
                _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSaving = false) }
                _operationErrorEvent.trySend(MemoEditOperationErrorEvent.SaveFailed)
            }
        }
    }

    fun delete() {
        val id = memoId ?: return
        if (_uiState.value.isDeletePending) return
        _uiState.update { state -> state.copy(isDeletePending = true, hasError = false) }
        viewModelScope.launch {
            try {
                val memoId = moveMemoToTrashUseCase(MemoId(id))
                autosaveJob?.cancel()
                clearDraftAfterCompletedOperation()
                clearSavedState()
                shouldPersistDraft = false
                _uiState.update { state -> state.copy(isDeletePending = false) }
                _navigationEvent.trySend(MemoEditNavigationEvent.MemoDeleted(memoId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _uiState.update { state ->
                    state.copy(isDeletePending = false)
                }
                _operationErrorEvent.trySend(MemoEditOperationErrorEvent.DeleteFailed)
            }
        }
    }

    fun requestBack() {
        if (_uiState.value.isDeletePending) return
        viewModelScope.launch {
            autosaveJob?.cancel()
            saveDraft(_uiState.value)
            _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
        }
    }

    fun flushDraft() {
        if (_uiState.value.isDeletePending) return
        viewModelScope.launch {
            autosaveJob?.cancel()
            saveDraft(_uiState.value)
        }
    }

    private fun updateDraftState(transform: (MemoEditUiState) -> MemoEditUiState) {
        val nextState = transform(_uiState.value)
        shouldPersistDraft = true
        _uiState.value = nextState
        persistSavedState(nextState)
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MILLIS)
            saveDraft(_uiState.value)
        }
    }

    private suspend fun loadStoredDraft(targetMemoId: String?): MemoEditDraft? = try {
        getMemoEditDraftUseCase(draftTargetFor(targetMemoId))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        _draftErrorEvent.trySend(Unit)
        null
    }

    private suspend fun saveDraft(state: MemoEditUiState) {
        if (!shouldPersistDraft) return
        try {
            saveMemoEditDraftUseCase(state.toDraft())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            _draftErrorEvent.trySend(Unit)
        }
    }

    private suspend fun clearDraft(notifyOnFailure: Boolean = true) {
        try {
            clearMemoEditDraftUseCase(initialDraftTarget)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (notifyOnFailure) {
                _draftErrorEvent.trySend(Unit)
            }
            throw e
        }
    }

    private suspend fun clearDraftAfterCompletedOperation() {
        try {
            clearDraft()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // clearDraft already emitted draftErrorEvent.
        }
    }

    private fun applyDraft(draft: MemoEditDraft, targetMemoId: String?) {
        _uiState.update { current ->
            draft.toUiState(targetMemoId).copy(availableTags = current.availableTags)
        }
    }

    private fun applySavedMemo(memo: Memo) {
        memoId = memo.id.value
        savedStateHandle["memoId"] = memo.id.value
        _uiState.update { it.copy(memoId = memo.id.value) }
    }

    private fun currentDraftTarget(): MemoEditDraftTarget = draftTargetFor(memoId)

    private fun draftTargetFor(targetMemoId: String?): MemoEditDraftTarget = targetMemoId
        ?.let { MemoEditDraftTarget.existingMemo(MemoId(it)) }
        ?: MemoEditDraftTarget.newMemo(createdAt)

    private fun savedStateDraft(targetMemoId: String?): MemoEditDraft? {
        val title = savedStateHandle.get<String>(DRAFT_TITLE_KEY)
        val body = savedStateHandle.get<String>(DRAFT_BODY_KEY).orEmpty()
        val tagIds = savedStateHandle
            .get<ArrayList<String>>(DRAFT_TAG_IDS_KEY)
            .orEmpty()
            .map { TagId(it) }
        val isFavorite = savedStateHandle.get<Boolean>(DRAFT_IS_FAVORITE_KEY) ?: false
        val savedCreatedAt = savedStateHandle.get<Long>(DRAFT_CREATED_AT_KEY)

        return if (title == null) {
            null
        } else if (title.isBlank() && body.isBlank() && tagIds.isEmpty() && !isFavorite) {
            null
        } else {
            MemoEditDraft(
                target = draftTargetFor(targetMemoId),
                title = MemoTitle(title),
                body = MemoBody(body),
                createdAt = savedCreatedAt?.let { TimestampMillis(it) },
                tagIds = tagIds,
                isFavorite = isFavorite
            )
        }
    }

    private fun persistSavedState(state: MemoEditUiState) {
        val draft = state.toDraft()
        if (draft.title.value.isBlank() && draft.body.value.isBlank() &&
            draft.tagIds.isEmpty() && !draft.isFavorite
        ) {
            clearSavedState()
            return
        }
        savedStateHandle[DRAFT_TITLE_KEY] = draft.title.value
        savedStateHandle[DRAFT_BODY_KEY] = draft.body.value
        savedStateHandle[DRAFT_TAG_IDS_KEY] = ArrayList(draft.tagIds.map { it.value })
        savedStateHandle[DRAFT_IS_FAVORITE_KEY] = draft.isFavorite
        draft.createdAt?.let { savedStateHandle[DRAFT_CREATED_AT_KEY] = it.value }
            ?: savedStateHandle.remove<Long>(DRAFT_CREATED_AT_KEY)
    }

    private fun clearSavedState() {
        savedStateHandle.remove<String>(DRAFT_TITLE_KEY)
        savedStateHandle.remove<String>(DRAFT_BODY_KEY)
        savedStateHandle.remove<ArrayList<String>>(DRAFT_TAG_IDS_KEY)
        savedStateHandle.remove<Boolean>(DRAFT_IS_FAVORITE_KEY)
        savedStateHandle.remove<Long>(DRAFT_CREATED_AT_KEY)
    }

    private fun Memo.toUiState() = MemoEditUiState(
        isLoading = false,
        memoId = id.value,
        title = title.value,
        body = body.value,
        isFavorite = isFavorite,
        selectedTagIds = tagIds.map { it.value }.toSet()
    )

    private fun MemoEditDraft.toUiState(targetMemoId: String?) = MemoEditUiState(
        isLoading = false,
        memoId = targetMemoId,
        title = title.value,
        body = body.value,
        isFavorite = isFavorite,
        isModified = true,
        selectedTagIds = tagIds.map { it.value }.toSet()
    )

    private fun MemoEditUiState.toDraft() = MemoEditDraft(
        target = currentDraftTarget(),
        title = MemoTitle(title),
        body = MemoBody(body),
        createdAt = createdAt,
        tagIds = selectedTagIds.map { TagId(it) },
        isFavorite = isFavorite
    )

    private companion object {
        const val DRAFT_SAVE_DEBOUNCE_MILLIS = 1_000L
        const val DRAFT_TITLE_KEY = "draftTitle"
        const val DRAFT_BODY_KEY = "draftBody"
        const val DRAFT_TAG_IDS_KEY = "draftTagIds"
        const val DRAFT_IS_FAVORITE_KEY = "draftIsFavorite"
        const val DRAFT_CREATED_AT_KEY = "draftCreatedAt"
    }
}

sealed interface MemoEditNavigationEvent {
    data object NavigateBack : MemoEditNavigationEvent
    data class MemoDeleted(val memoId: MemoId) : MemoEditNavigationEvent
}

sealed interface MemoEditOperationErrorEvent {
    data object SaveFailed : MemoEditOperationErrorEvent
    data object DeleteFailed : MemoEditOperationErrorEvent
}
