package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
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
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeMemosUseCase: ObserveMemosUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val filterMemosUseCase: FilterMemosUseCase,
    private val getHomeSummaryUseCase: GetHomeSummaryUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(HomeFilterUiState.All)
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = retryTrigger.flatMapLatest {
        combine(
            observeMemosUseCase(),
            observeTagsUseCase(),
            selectedFilter
        ) { memos, tags, filter ->
            val summary = getHomeSummaryUseCase(memos)
            val filteredMemos = filterMemosUseCase(memos, filter.toDomainFilter())

            HomeUiState(
                isLoading = false,
                selectedFilter = filter,
                summary = HomeSummaryUiState(
                    totalCount = summary.totalCount,
                    todayCount = summary.todayCount,
                    unorganizedCount = summary.unorganizedCount,
                    importantCount = summary.importantCount
                ),
                memos = MemoUiModel.fromDomain(filteredMemos, tags)
            )
        }.catch {
            emit(
                HomeUiState(
                    isLoading = false,
                    hasError = true,
                    selectedFilter = selectedFilter.value
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

    fun retry() {
        retryTrigger.value++
    }

    private fun HomeFilterUiState.toDomainFilter(): MemoFilter = when (this) {
        HomeFilterUiState.All -> MemoFilter.All
        HomeFilterUiState.Unorganized -> MemoFilter.Unorganized
        HomeFilterUiState.Important -> MemoFilter.Important
    }
}
