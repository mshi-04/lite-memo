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

    private val memoId: String? = savedStateHandle["memoId"]
    private val createdAtMillis: Long = savedStateHandle["createdAt"] ?: -1L
    private val createdAt: TimestampMillis? = if (createdAtMillis >= 0) {
        TimestampMillis(createdAtMillis)
    } else {
        null
    }
    private val draftTarget: MemoEditDraftTarget = memoId
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
        if (memoId != null) {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
        }
        viewModelScope.launch {
            val savedDraft = savedStateDraft()
            if (savedDraft != null) {
                applyDraft(savedDraft)
                return@launch
            }

            val storedDraft = loadStoredDraft()
            if (storedDraft != null) {
                applyDraft(storedDraft)
                persistSavedState(storedDraft.toUiState())
                return@launch
            }

            if (memoId == null) return@launch
            try {
                val memo = getMemoUseCase(MemoId(memoId))
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
        val state = _uiState.value
        val title = state.title
        val body = state.body
        if (title.isBlank() && body.isBlank()) {
            viewModelScope.launch {
                autosaveJob?.cancel()
                try {
                    clearDraft(notifyOnFailure = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    _operationErrorEvent.trySend(MemoEditOperationErrorEvent.SaveFailed)
                    return@launch
                }
                clearSavedState()
                shouldPersistDraft = false
                _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            }
            return
        }
        viewModelScope.launch {
            try {
                saveMemoUseCase(
                    SaveMemoCommand(
                        id = memoId?.let { MemoId(it) },
                        title = MemoTitle(title),
                        body = MemoBody(body),
                        createdAt = createdAt,
                        tagIds = state.selectedTagIds.map { TagId(it) },
                        isFavorite = state.isFavorite
                    )
                )
                autosaveJob?.cancel()
                clearDraft(notifyOnFailure = false)
                clearSavedState()
                shouldPersistDraft = false
                _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _operationErrorEvent.trySend(MemoEditOperationErrorEvent.SaveFailed)
            }
        }
    }

    fun delete() {
        val id = memoId ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isDeletePending = true, hasError = false) }
            try {
                val memoId = moveMemoToTrashUseCase(MemoId(id))
                autosaveJob?.cancel()
                clearDraft(notifyOnFailure = false)
                clearSavedState()
                shouldPersistDraft = false
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

    private suspend fun loadStoredDraft(): MemoEditDraft? = try {
        getMemoEditDraftUseCase(draftTarget)
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
            clearMemoEditDraftUseCase(draftTarget)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (notifyOnFailure) {
                _draftErrorEvent.trySend(Unit)
            }
            throw e
        }
    }

    private fun applyDraft(draft: MemoEditDraft) {
        _uiState.update { current ->
            draft.toUiState().copy(availableTags = current.availableTags)
        }
    }

    private fun savedStateDraft(): MemoEditDraft? {
        val title = savedStateHandle.get<String>(DRAFT_TITLE_KEY) ?: return null
        val body = savedStateHandle.get<String>(DRAFT_BODY_KEY).orEmpty()
        val tagIds = savedStateHandle
            .get<ArrayList<String>>(DRAFT_TAG_IDS_KEY)
            .orEmpty()
            .map { TagId(it) }
        val isFavorite = savedStateHandle.get<Boolean>(DRAFT_IS_FAVORITE_KEY) ?: false
        val savedCreatedAt = savedStateHandle.get<Long>(DRAFT_CREATED_AT_KEY)

        if (title.isBlank() && body.isBlank() && tagIds.isEmpty() && !isFavorite) return null

        return MemoEditDraft(
            target = draftTarget,
            title = MemoTitle(title),
            body = MemoBody(body),
            createdAt = savedCreatedAt?.let { TimestampMillis(it) },
            tagIds = tagIds,
            isFavorite = isFavorite
        )
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

    private fun MemoEditDraft.toUiState() = MemoEditUiState(
        isLoading = false,
        memoId = this@MemoEditViewModel.memoId,
        title = title.value,
        body = body.value,
        isFavorite = isFavorite,
        isModified = true,
        selectedTagIds = tagIds.map { it.value }.toSet()
    )

    private fun MemoEditUiState.toDraft() = MemoEditDraft(
        target = draftTarget,
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
