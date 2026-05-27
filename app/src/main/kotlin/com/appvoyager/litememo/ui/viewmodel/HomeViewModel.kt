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
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoFavoriteUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSelectionUiState
import com.appvoyager.litememo.ui.state.HomeSummaryUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.state.TagUiModel
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeMemosUseCase: ObserveMemosUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val filterMemosUseCase: FilterMemosUseCase,
    private val getHomeSummaryUseCase: GetHomeSummaryUseCase,
    private val observeMemoSortOrderUseCase: ObserveMemoSortOrderUseCase,
    private val searchMemosUseCase: SearchMemosUseCase,
    private val setMemoFavoriteUseCase: SetMemoFavoriteUseCase,
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase,
    private val applyMemoBulkActionUseCase: ApplyMemoBulkActionUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<HomeFilterUiState>(HomeFilterUiState.All)
    private val isSearchActive = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val hasActionError = MutableStateFlow(false)
    private val selection = MutableStateFlow(HomeSelectionUiState())
    private val bulkTagDialog = MutableStateFlow(HomeBulkTagDialogUiState())
    private val retryTrigger = MutableStateFlow(0)

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

    private val interactionControls = combine(
        hasActionError,
        selection,
        bulkTagDialog
    ) { actionError, selection, tagDialog ->
        InteractionControls(actionError, selection, tagDialog)
    }

    private val uiControls = combine(
        selectedFilter,
        isSearchActive,
        searchQuery,
        interactionControls
    ) { filter, searching, query, interaction ->
        UiControls(
            filter = filter,
            searching = searching,
            query = query,
            actionError = interaction.actionError,
            selection = interaction.selection,
            tagDialog = interaction.tagDialog
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
            val summary = getHomeSummaryUseCase(memos)
            val tagUiModels = tags.map { TagUiModel.fromDomain(it) }
            val effectiveFilter = controls.filter.effectiveFilter(tags)
            val filteredMemos = filterMemosUseCase(memos, effectiveFilter.toDomainFilter())

            HomeUiState(
                isLoading = false,
                hasActionError = controls.actionError,
                selectedFilter = effectiveFilter,
                memoSortOrder = sortOrder,
                isSearchActive = controls.searching,
                searchQuery = controls.query,
                hasSearchError = searchHits == null,
                selection = controls.selection,
                bulkTagDialog = controls.tagDialog,
                tags = tagUiModels,
                summary = HomeSummaryUiState(
                    totalCount = summary.totalCount,
                    todayCount = summary.todayCount,
                    unorganizedCount = summary.unorganizedCount,
                    favoriteCount = summary.favoriteCount
                ),
                memos = MemoUiModel.fromDomain(filteredMemos, tags),
                searchResults = MemoUiModel.fromDomain(searchHits ?: emptyList(), tags)
            )
        }.catch {
            emit(
                HomeUiState(
                    isLoading = false,
                    hasError = true,
                    hasActionError = hasActionError.value,
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
                hasActionError.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                hasActionError.value = true
            }
        }
    }

    fun startSelection(memoId: MemoId) {
        hasActionError.value = false
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

    fun dismissActionError() {
        hasActionError.value = false
    }

    fun retry() {
        hasActionError.value = false
        retryTrigger.update { it + 1 }
    }

    private fun requestBulkTagOperation(operation: HomeBulkTagDialogUiState.Operation) {
        if (!selection.value.isActive) return
        hasActionError.value = false
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
                hasActionError.value = false
                clearSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                bulkTagDialog.value = HomeBulkTagDialogUiState()
                hasActionError.value = true
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

    private data class UiControls(
        val filter: HomeFilterUiState,
        val searching: Boolean,
        val query: String,
        val actionError: Boolean,
        val selection: HomeSelectionUiState,
        val tagDialog: HomeBulkTagDialogUiState
    )

    private data class InteractionControls(
        val actionError: Boolean,
        val selection: HomeSelectionUiState,
        val tagDialog: HomeBulkTagDialogUiState
    )

}
