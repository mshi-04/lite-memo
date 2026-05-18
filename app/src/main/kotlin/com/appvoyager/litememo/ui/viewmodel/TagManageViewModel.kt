package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.SaveTagCommand
import com.appvoyager.litememo.domain.model.value.TagColor
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TagName
import com.appvoyager.litememo.domain.usecase.DeleteTagUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveTagUseCase
import com.appvoyager.litememo.ui.state.DEFAULT_TAG_COLORS
import com.appvoyager.litememo.ui.state.TagEditState
import com.appvoyager.litememo.ui.state.TagManageUiState
import com.appvoyager.litememo.ui.state.TagUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val saveTagUseCase: SaveTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val editingTag = MutableStateFlow<TagEditState?>(null)
    private val deleteDialog = MutableStateFlow<TagUiModel?>(null)
    private val hasDeleteError = MutableStateFlow(false)
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<TagManageUiState> = retryTrigger.flatMapLatest {
        combine(
            observeTagsUseCase(),
            editingTag,
            deleteDialog,
            hasDeleteError
        ) { tags, editing, deleting, deleteError ->
            TagManageUiState(
                isLoading = false,
                hasDeleteError = deleteError,
                tags = tags.map { TagUiModel.fromDomain(it) },
                editingTag = editing,
                showDeleteDialog = deleting
            )
        }.catch {
            emit(TagManageUiState(isLoading = false, hasError = true))
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
        editingTag.update { it?.copy(name = name, nameError = false) }
    }

    fun selectEditColor(colorArgb: Long) {
        editingTag.update { it?.copy(colorArgb = colorArgb) }
    }

    fun saveEdit() {
        val state = editingTag.value ?: return
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            editingTag.update { it?.copy(nameError = true) }
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
                editingTag.update { state -> state?.copy(saveError = true) }
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
                hasDeleteError.value = false
            }.onFailure {
                deleteDialog.value = null
                hasDeleteError.value = true
            }
        }
    }

    fun dismissDeleteError() {
        hasDeleteError.value = false
    }

    fun dismissDeleteDialog() {
        deleteDialog.value = null
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }
}
