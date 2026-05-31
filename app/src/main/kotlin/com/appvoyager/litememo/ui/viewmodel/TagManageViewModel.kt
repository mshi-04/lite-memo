package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.SaveTagCommand
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.usecase.DeleteTagUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveTagUseCase
import com.appvoyager.litememo.ui.state.TagEditState
import com.appvoyager.litememo.ui.state.TagManageUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
class TagManageViewModel @Inject constructor(
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val saveTagUseCase: SaveTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val editingTag = MutableStateFlow<TagEditState?>(null)
    private val deleteDialog = MutableStateFlow<TagUiModel?>(null)
    private val retryTrigger = MutableStateFlow(false)

    // 削除失敗は一回限りの通知なので Channel event で扱う(取りこぼし防止に BUFFERED)
    private val _deleteErrorEvent = Channel<Unit>(Channel.BUFFERED)
    val deleteErrorEvent = _deleteErrorEvent.receiveAsFlow()

    val uiState: StateFlow<TagManageUiState> = retryTrigger.flatMapLatest {
        combine(
            observeTagsUseCase()
                .map<List<Tag>, List<Tag>?> { it }
                .catch { emit(null) },
            editingTag,
            deleteDialog
        ) { tags, editing, deleting ->
            TagManageUiState(
                isLoading = false,
                hasError = tags == null,
                tags = tags?.map { TagUiModel.fromDomain(it) } ?: emptyList(),
                editingTag = editing,
                showDeleteDialog = deleting
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagManageUiState()
    )

    fun startCreate() {
        editingTag.value = TagEditState()
    }

    fun startEdit(tagId: String) {
        val tag = uiState.value.tags.find { it.id == tagId } ?: return
        editingTag.value = TagEditState(
            id = tag.id,
            name = tag.name,
            colorArgb = tag.colorArgb
        )
    }

    fun updateEditName(name: String) {
        editingTag.update {
            it?.copy(name = name, nameError = false, duplicateNameError = false, saveError = false)
        }
    }

    fun selectEditColor(colorArgb: Long) {
        editingTag.update { it?.copy(colorArgb = colorArgb, saveError = false) }
    }

    fun saveEdit() {
        val state = editingTag.value ?: return
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            editingTag.update { it?.copy(nameError = true) }
            return
        }
        if (isDuplicateName(trimmedName, state.id)) {
            editingTag.update { it?.copy(duplicateNameError = true) }
            return
        }
        viewModelScope.launch {
            runCatching {
                saveTagUseCase(
                    SaveTagCommand(
                        id = state.id?.let { TagId(it) },
                        name = TagName(trimmedName),
                        color = TagColor(state.colorArgb)
                    )
                )
            }.onSuccess {
                editingTag.value = null
            }.onFailure {
                editingTag.update { current -> current?.copy(saveError = true) }
            }
        }
    }

    fun cancelEdit() {
        editingTag.value = null
    }

    fun requestDelete(tag: TagUiModel) {
        deleteDialog.value = tag
    }

    fun confirmDelete() {
        val tag = deleteDialog.value ?: return
        viewModelScope.launch {
            runCatching {
                deleteTagUseCase(TagId(tag.id))
            }.onSuccess {
                deleteDialog.value = null
            }.onFailure {
                deleteDialog.value = null
                _deleteErrorEvent.trySend(Unit)
            }
        }
    }

    fun dismissDeleteDialog() {
        deleteDialog.value = null
    }

    fun retry() {
        retryTrigger.update { !it }
    }

    // 同名タグ(自分自身を除く)が既に存在するかを判定する
    private fun isDuplicateName(name: String, excludeId: String?): Boolean =
        uiState.value.tags.any { it.name == name && it.id != excludeId }
}
