package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.SaveMemoCommand
import com.appvoyager.litememo.domain.model.value.MemoBody
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.MemoTitle
import com.appvoyager.litememo.domain.usecase.DeleteMemoUseCase
import com.appvoyager.litememo.domain.usecase.GetMemoUseCase
import com.appvoyager.litememo.domain.usecase.SaveMemoUseCase
import com.appvoyager.litememo.ui.state.MemoEditUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MemoEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMemoUseCase: GetMemoUseCase,
    private val saveMemoUseCase: SaveMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase
) : ViewModel() {

    private val memoId: String? = savedStateHandle["memoId"]

    private val _uiState = MutableStateFlow(MemoEditUiState())
    val uiState: StateFlow<MemoEditUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<Unit>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadMemo()
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
                            body = memo.body.value
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

    fun save() {
        val title = _uiState.value.title
        val body = _uiState.value.body
        if (title.isBlank() && body.isBlank()) {
            _navigationEvent.trySend(Unit)
            return
        }
        viewModelScope.launch {
            runCatching {
                saveMemoUseCase(
                    SaveMemoCommand(
                        id = memoId?.let { MemoId(it) },
                        title = MemoTitle(title),
                        body = MemoBody(body)
                    )
                )
            }.onSuccess {
                _navigationEvent.trySend(Unit)
            }.onFailure {
                _uiState.update { state -> state.copy(hasError = true) }
            }
        }
    }

    fun delete() {
        val id = memoId ?: return
        viewModelScope.launch {
            runCatching {
                deleteMemoUseCase(MemoId(id))
            }.onSuccess {
                _navigationEvent.trySend(Unit)
            }.onFailure {
                _uiState.update { state -> state.copy(hasError = true) }
            }
        }
    }

    fun requestBack() {
        if (_uiState.value.isModified) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _navigationEvent.trySend(Unit)
        }
    }

    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun confirmDiscard() {
        _navigationEvent.trySend(Unit)
    }
}
