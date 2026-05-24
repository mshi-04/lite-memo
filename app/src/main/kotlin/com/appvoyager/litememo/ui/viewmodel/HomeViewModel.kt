package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemoSortOrderUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoFavoriteUseCase
import com.appvoyager.litememo.domain.usecase.SetMemoSortOrderUseCase
import com.appvoyager.litememo.ui.state.HomeFilterUiState
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
    private val setMemoSortOrderUseCase: SetMemoSortOrderUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<HomeFilterUiState>(HomeFilterUiState.All)
    private val isSearchActive = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val hasFavoriteUpdateError = MutableStateFlow(false)
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
        searchQuery,
        hasFavoriteUpdateError
    ) { filter, searching, query, favoriteUpdateError ->
        UiControls(filter, searching, query, favoriteUpdateError)
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
                hasError = controls.favoriteUpdateError,
                selectedFilter = effectiveFilter,
                memoSortOrder = sortOrder,
                isSearchActive = controls.searching,
                searchQuery = controls.query,
                hasSearchError = searchHits == null,
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
                hasFavoriteUpdateError.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                hasFavoriteUpdateError.value = true
            }
        }
    }

    fun retry() {
        hasFavoriteUpdateError.value = false
        retryTrigger.update { it + 1 }
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
        val favoriteUpdateError: Boolean
    )

}
