package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.model.MemoFilter
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.usecase.FilterMemosUseCase
import com.appvoyager.litememo.domain.usecase.GetHomeSummaryUseCase
import com.appvoyager.litememo.domain.usecase.ObserveMemosUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.ui.state.HomeFilterUiState
import com.appvoyager.litememo.ui.state.HomeMemoUiModel
import com.appvoyager.litememo.ui.state.HomeSummaryUiState
import com.appvoyager.litememo.ui.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMemosUseCase: ObserveMemosUseCase,
    observeTagsUseCase: ObserveTagsUseCase,
    private val filterMemosUseCase: FilterMemosUseCase,
    private val getHomeSummaryUseCase: GetHomeSummaryUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(HomeFilterUiState.All)

    val uiState: StateFlow<HomeUiState> = combine(
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
            memos = filteredMemos.toUiModels(tags)
        )
    }.catch {
        emit(HomeUiState(isLoading = false, hasError = true))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectFilter(filter: HomeFilterUiState) {
        selectedFilter.value = filter
    }

    private fun HomeFilterUiState.toDomainFilter(): MemoFilter = when (this) {
        HomeFilterUiState.All -> MemoFilter.All
        HomeFilterUiState.Unorganized -> MemoFilter.Unorganized
        HomeFilterUiState.Important -> MemoFilter.Important
    }

    private fun List<Memo>.toUiModels(tags: List<Tag>): List<HomeMemoUiModel> {
        val tagsById = tags.associateBy { it.id }
        return map { memo ->
            val tag = memo.tagIds.firstNotNullOfOrNull { id -> tagsById[id] }
            HomeMemoUiModel(
                id = memo.id.value,
                title = memo.title.value,
                body = memo.body.value,
                tagName = tag?.name?.value,
                tagColorArgb = tag?.color?.argb,
                updatedAtMillis = memo.updatedAt.value,
                isImportant = memo.isImportant
            )
        }
    }
}
