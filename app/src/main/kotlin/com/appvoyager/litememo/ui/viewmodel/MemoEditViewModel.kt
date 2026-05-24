package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.usecase.DeleteMemoUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
import com.appvoyager.litememo.ui.state.MemoEditUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMemoUseCase: GetMemoUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase
) : ViewModel() {

    private val memoId: String? = savedStateHandle["memoId"]
    private val createdAtMillis: Long = savedStateHandle["createdAt"] ?: -1L

    private val _uiState = MutableStateFlow(MemoEditUiState())
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<MemoEditNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadMemo()
        observeTagsUseCase()
            .onEach { tags ->
                _uiState.update { state ->
                    val validTagIds = tags.map { it.id.value }.toSet()
                    state.copy(
                        availableTags = tags.map { TagUiModel.fromDomain(it) },
                        selectedTagIds = state.selectedTagIds.intersect(validTagIds)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun reload() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadMemo()
    }

    private fun loadMemo() {
        if (memoId == null) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                getMemoUseCase(MemoId(memoId))
            }.onSuccess { memo ->
                if (memo == null) {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            memoId = memo.id.value,
                            title = memo.title.value,
                            body = memo.body.value,
                            isFavorite = memo.isFavorite,
                            selectedTagIds = memo.tagIds.map { id -> id.value }.toSet()
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, isModified = true) }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, isModified = true) }
    }

    fun toggleTag(tagId: String) {
        _uiState.update { state ->
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
            _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            return
        }
        viewModelScope.launch {
            runCatching {
                saveMemoUseCase(
                    SaveMemoCommand(
                        id = memoId?.let { MemoId(it) },
                        title = MemoTitle(title),
                        body = MemoBody(body),
                        createdAt = if (createdAtMillis >= 0) {
                            TimestampMillis(createdAtMillis)
                        } else {
                            null
                        },
                        tagIds = state.selectedTagIds.map { TagId(it) },
                        isFavorite = state.isFavorite
                    )
                )
            }.onSuccess {
                _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
            }.onFailure {
                _uiState.update { state -> state.copy(hasError = true) }
            }
        }
    }

    fun delete() {
        val id = memoId ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isDeletePending = true, hasError = false) }
            runCatching {
                val memo = requireNotNull(getMemoUseCase(MemoId(id))) {
                    "Memo not found: $id"
                }
                deleteMemoUseCase(MemoId(id))
                memo
            }.onSuccess {
                _navigationEvent.trySend(MemoEditNavigationEvent.MemoDeleted(it))
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isDeletePending = false, hasError = true)
                }
            }
        }
    }

    fun requestBack() {
        if (_uiState.value.isDeletePending) return
        if (_uiState.value.isModified) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
        }
    }

    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun confirmDiscard() {
        _navigationEvent.trySend(MemoEditNavigationEvent.NavigateBack)
    }
}

sealed interface MemoEditNavigationEvent {
    data object NavigateBack : MemoEditNavigationEvent
    data class MemoDeleted(val memo: Memo) : MemoEditNavigationEvent
}
