package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.ApplyMemoBulkActionCommand
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoBulkAction
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.usecase.ApplyMemoBulkActionUseCase
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoFavoriteUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSelectionUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.state.TagUiModel
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeMemosUseCase: ObserveMemosUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val filterMemosUseCase: FilterMemosUseCase,
    private val observeMemoSortOrderUseCase: ObserveMemoSortOrderUseCase,
    private val searchMemosUseCase: SearchMemosUseCase,
    private val setMemoFavoriteUseCase: SetMemoFavoriteUseCase,
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase,
    private val applyMemoBulkActionUseCase: ApplyMemoBulkActionUseCase,
    private val formatMemoTextUseCase: FormatMemoTextUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<HomeFilterUiState>(HomeFilterUiState.All)
    private val isSearchActive = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(HomeSelectionUiState())
    private val bulkTagDialog = MutableStateFlow(HomeBulkTagDialogUiState())
    private val retryTrigger = MutableStateFlow(false)

    // 操作失敗は一回限りの通知で、同一文言の最新イベントだけ届けばよい。
    private val _actionErrorEvent = Channel<Unit>(Channel.CONFLATED)
    val actionErrorEvent = _actionErrorEvent.receiveAsFlow()

    private val searchResults = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                searchMemosUseCase(query)
                    .map<List<Memo>, List<Memo>?> { memos -> memos }
                    .catch { emit(null) }
            }
        }

    private val uiControls = combine(
        selectedFilter,
        isSearchActive,
        searchQuery,
        selection,
        bulkTagDialog
    ) { filter, searching, query, activeSelection, tagDialog ->
        HomeUiControls(
            filter = filter,
            searching = searching,
            query = query,
            selection = activeSelection,
            tagDialog = tagDialog
        )
    }

    val uiState: StateFlow<HomeUiState> = retryTrigger.flatMapLatest {
        combine(
            observeMemosUseCase(),
            observeTagsUseCase(),
            observeMemoSortOrderUseCase(),
            uiControls,
            searchResults
        ) { memos, tags, sortOrder, controls, searchHits ->
            val tagUiModels = tags.map { TagUiModel.fromDomain(it) }
            val effectiveFilter = controls.filter.effectiveFilter(tags)
            val filteredMemos = filterMemosUseCase(memos, effectiveFilter.toDomainFilter())
            val memoById = memos.associateBy { it.id }
            val allSelectedFavorite = controls.selection.selectedMemoIds.isNotEmpty() &&
                controls.selection.selectedMemoIds.all { memoById[it]?.isFavorite == true }

            HomeUiState(
                isLoading = false,
                selectedFilter = effectiveFilter,
                memoSortOrder = sortOrder,
                isSearchActive = controls.searching,
                searchQuery = controls.query,
                hasSearchError = searchHits == null,
                selection = controls.selection,
                allSelectedFavorite = allSelectedFavorite,
                bulkTagDialog = controls.tagDialog,
                tags = tagUiModels,
                memos = MemoUiModel.fromDomain(filteredMemos, tags),
                searchResults = MemoUiModel.fromDomain(searchHits ?: emptyList(), tags)
            )
        }.catch {
            emit(
                HomeUiState(
                    isLoading = false,
                    hasError = true,
                    selectedFilter = selectedFilter.value,
                    isSearchActive = isSearchActive.value,
                    searchQuery = searchQuery.value,
                    selection = selection.value,
                    bulkTagDialog = bulkTagDialog.value
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectFilter(filter: HomeFilterUiState) {
        selectedFilter.value = filter
    }

    fun selectSortOrder(order: MemoSortOrder) {
        viewModelScope.launch {
            try {
                setMemoSortOrderUseCase(order)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    fun toggleSearch() {
        val wasActive = isSearchActive.value
        isSearchActive.value = !wasActive
        if (wasActive) {
            closeSearch()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun closeSearch() {
        isSearchActive.value = false
        searchQuery.value = ""
    }

    fun setMemoFavorite(memoId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                setMemoFavoriteUseCase(MemoId(memoId), isFavorite)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _actionErrorEvent.trySend(Unit)
            }
        }
    }

    fun startSelection(memoId: MemoId) {
        bulkTagDialog.value = HomeBulkTagDialogUiState()
        selection.value = HomeSelectionUiState(selectedMemoIds = setOf(memoId))
    }

    fun toggleMemoSelection(memoId: MemoId) {
        val current = selection.value.selectedMemoIds
        val next = if (memoId in current) {
            current - memoId
        } else {
            current + memoId
        }
        selection.value = HomeSelectionUiState(selectedMemoIds = next)
        if (next.isEmpty()) {
            bulkTagDialog.value = HomeBulkTagDialogUiState()
        }
    }

    fun clearSelection() {
        selection.value = HomeSelectionUiState()
        bulkTagDialog.value = HomeBulkTagDialogUiState()
    }

    fun moveSelectedMemosToTrash() {
        applySelectedMemoAction(MemoBulkAction.moveToTrash())
    }

    fun setSelectedMemosFavorite(isFavorite: Boolean) {
        applySelectedMemoAction(MemoBulkAction.setFavorite(isFavorite))
    }

    fun requestAddTagToSelectedMemos() {
        requestBulkTagOperation(HomeBulkTagDialogUiState.Operation.AddTag)
    }

    fun requestRemoveTagFromSelectedMemos() {
        requestBulkTagOperation(HomeBulkTagDialogUiState.Operation.RemoveTag)
    }

    fun dismissBulkTagDialog() {
        bulkTagDialog.value = HomeBulkTagDialogUiState()
    }

    fun applySelectedTag(tagId: TagId) {
        val operation = bulkTagDialog.value.operation ?: return
        val action = when (operation) {
            HomeBulkTagDialogUiState.Operation.AddTag -> MemoBulkAction.addTag(tagId)
            HomeBulkTagDialogUiState.Operation.RemoveTag -> MemoBulkAction.removeTag(tagId)
        }
        applySelectedMemoAction(action)
    }

    fun formatMemoText(title: String, body: String): String? = formatMemoTextUseCase(title, body)

    fun getSelectedMemoForShare(): MemoUiModel? {
        val state = uiState.value
        val selectedId = state.selection.selectedMemoIds.singleOrNull() ?: return null
        return (state.memos + state.searchResults).find { it.id == selectedId.value }
    }

    fun retry() {
        retryTrigger.update { !it }
    }

    private fun requestBulkTagOperation(operation: HomeBulkTagDialogUiState.Operation) {
        if (!selection.value.isActive) return
        bulkTagDialog.value = HomeBulkTagDialogUiState(operation)
    }

    private fun applySelectedMemoAction(action: MemoBulkAction) {
        val memoIds = selection.value.selectedMemoIds.toList()
        if (memoIds.isEmpty()) return

        viewModelScope.launch {
            try {
                applyMemoBulkActionUseCase(
                    ApplyMemoBulkActionCommand(
                        memoIds = memoIds,
                        action = action
                    )
                )
                clearSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                bulkTagDialog.value = HomeBulkTagDialogUiState()
                _actionErrorEvent.trySend(Unit)
            }
        }
    }

    private fun HomeFilterUiState.toDomainFilter(): MemoFilter = when (type) {
        HomeFilterUiState.Type.All -> MemoFilter.All
        HomeFilterUiState.Type.Unorganized -> MemoFilter.Unorganized
        HomeFilterUiState.Type.Favorite -> MemoFilter.Favorite
        HomeFilterUiState.Type.ByTag -> MemoFilter.ByTag(requireNotNull(tagId))
    }

    private fun HomeFilterUiState.effectiveFilter(tags: List<Tag>): HomeFilterUiState =
        if (type == HomeFilterUiState.Type.ByTag) {
            if (tags.any { tag -> tag.id == tagId }) this else HomeFilterUiState.All
        } else {
            this
        }
}

private data class HomeUiControls(
    val filter: HomeFilterUiState,
    val searching: Boolean,
    val query: String,
    val selection: HomeSelectionUiState,
    val tagDialog: HomeBulkTagDialogUiState
)
