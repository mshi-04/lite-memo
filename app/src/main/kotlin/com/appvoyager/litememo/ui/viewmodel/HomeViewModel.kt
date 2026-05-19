package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoImportantUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeSummaryUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import com.appvoyager.litememo.ui.state.MemoUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    private val setMemoImportantUseCase: SetMemoImportantUseCase,
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(HomeFilterUiState.All)
    private val isSearchActive = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
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

    private val uiControls = combine(
        selectedFilter,
        isSearchActive,
        searchQuery
    ) { filter, searching, query ->
        UiControls(filter, searching, query)
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
            val filteredMemos = filterMemosUseCase(memos, controls.filter.toDomainFilter())

            HomeUiState(
                isLoading = false,
                selectedFilter = controls.filter,
                memoSortOrder = sortOrder,
                isSearchActive = controls.searching,
                searchQuery = controls.query,
                hasSearchError = searchHits == null,
                summary = HomeSummaryUiState(
                    totalCount = summary.totalCount,
                    todayCount = summary.todayCount,
                    unorganizedCount = summary.unorganizedCount,
                    importantCount = summary.importantCount
                ),
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
                    searchQuery = searchQuery.value
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
            runCatching { setMemoSortOrderUseCase(order) }
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

    fun setMemoImportant(memoId: String, isImportant: Boolean) {
        viewModelScope.launch {
            runCatching {
                setMemoImportantUseCase(MemoId(memoId), isImportant)
            }
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    private fun HomeFilterUiState.toDomainFilter(): MemoFilter = when (this) {
        HomeFilterUiState.All -> MemoFilter.All
        HomeFilterUiState.Unorganized -> MemoFilter.Unorganized
        HomeFilterUiState.Important -> MemoFilter.Important
    }

    private data class UiControls(
        val filter: HomeFilterUiState,
        val searching: Boolean,
        val query: String
    )

}
