package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.ApplyMemoBulkActionCommand
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoBulkAction
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.usecase.ApplyMemoBulkActionUseCase
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.FormatMemoTextUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.ResolveMemoImagePathUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoFavoriteUseCase
import com.appvoyager.litememo.ui.state.HomeBulkTagDialogUiState
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSelectionUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import com.appvoyager.litememo.ui.state.TagUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MILLIS = 250L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
@Suppress("LongParameterList")
class HomeViewModel @Inject constructor(
    private val observeMemosUseCase: ObserveMemosUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val filterMemosUseCase: FilterMemosUseCase,
    private val searchMemosUseCase: SearchMemosUseCase,
    private val setMemoFavoriteUseCase: SetMemoFavoriteUseCase,
    private val applyMemoBulkActionUseCase: ApplyMemoBulkActionUseCase,
    private val formatMemoTextUseCase: FormatMemoTextUseCase,
    private val resolveMemoImagePathUseCase: ResolveMemoImagePathUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<HomeFilterUiState>(HomeFilterUiState.All)
    private val isSearchActive = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val selection = MutableStateFlow(HomeSelectionUiState())
    private val bulkTagDialog = MutableStateFlow(HomeBulkTagDialogUiState())
    private val retryTrigger = MutableStateFlow(false)
    private var isBulkActionInFlight = false

    // 操作失敗は一回限りの通知で、同一文言の最新イベントだけ届けばよい。
    private val _actionErrorEvent = Channel<Unit>(Channel.CONFLATED)
    val actionErrorEvent = _actionErrorEvent.receiveAsFlow()

    private val searchResults = searchQuery
        // searchQuery は StateFlow なので連続する同一値は既に除去される。
        // 空クエリは即時、入力中のみデバウンスして 1 文字ごとの LIKE 検索を抑える。
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
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
            uiControls,
            searchResults
        ) { memos, tags, controls, searchHits ->
            val tagUiModels = tags.map { TagUiModel.fromDomain(it) }
            val effectiveFilter = controls.filter.effectiveFilter(tags)
            val filteredMemos = filterMemosUseCase(memos, effectiveFilter.toDomainFilter())
            val memoById = memos.associateBy { it.id }
            val selectedMemos = controls.selection.selectedMemoIds.mapNotNull(memoById::get)
            val allSelectedFavorite = controls.selection.selectedMemoIds.isNotEmpty() &&
                controls.selection.selectedMemoIds.all { memoById[it]?.isFavorite == true }
            val allSelectedTagIds = selectedMemos
                .takeIf {
                    it.isNotEmpty() && it.size == controls.selection.selectedMemoIds.size
                }
                ?.map { memo -> memo.tagIds.toSet() }
                ?.reduce { commonTagIds, tagIds -> commonTagIds intersect tagIds }
                ?: emptySet()

            HomeUiState(
                isLoading = false,
                selectedFilter = effectiveFilter,
                isSearchActive = controls.searching,
                searchQuery = controls.query,
                hasSearchError = searchHits == null,
                selection = controls.selection,
                allSelectedFavorite = allSelectedFavorite,
                allSelectedTagIds = allSelectedTagIds,
                bulkTagDialog = controls.tagDialog,
                tags = tagUiModels,
                memos = MemoUiModel.fromDomain(
                    filteredMemos,
                    tags,
                    resolveMemoImagePathUseCase::invoke
                ),
                searchResults = MemoUiModel.fromDomain(
                    searchHits ?: emptyList(),
                    tags,
                    resolveMemoImagePathUseCase::invoke
                )
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

    fun requestToggleTagForSelectedMemos() {
        if (!selection.value.isActive) return
        bulkTagDialog.value = HomeBulkTagDialogUiState(isVisible = true)
    }

    fun dismissBulkTagDialog() {
        bulkTagDialog.value = HomeBulkTagDialogUiState()
    }

    fun toggleSelectedMemosTag(tagId: TagId) {
        if (!bulkTagDialog.value.isVisible) return
        val action = if (tagId in uiState.value.allSelectedTagIds) {
            MemoBulkAction.removeTag(tagId)
        } else {
            MemoBulkAction.addTag(tagId)
        }
        bulkTagDialog.value = HomeBulkTagDialogUiState()
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

    private fun applySelectedMemoAction(action: MemoBulkAction) {
        val memoIds = selection.value.selectedMemoIds.toList()
        if (memoIds.isEmpty()) return
        if (isBulkActionInFlight) return
        isBulkActionInFlight = true

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
            } finally {
                isBulkActionInFlight = false
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
