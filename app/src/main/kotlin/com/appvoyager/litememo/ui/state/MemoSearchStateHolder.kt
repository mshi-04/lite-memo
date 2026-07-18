package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.usecase.SearchMemosUseCase
import com.appvoyager.litememo.ui.model.MemoUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MemoSearchStateHolder(private val searchMemosUseCase: SearchMemosUseCase) {

    private val mutableControls = MutableStateFlow(SearchUiState())
    val controls: StateFlow<SearchUiState> = mutableControls.asStateFlow()

    val results: Flow<MemoSearchResult> = controls
        .debounce { search ->
            if (!search.isActive || search.query.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS
        }
        .flatMapLatest { search ->
            when {
                !search.isActive -> flowOf(MemoSearchResult.Inactive)

                search.query.isBlank() -> flowOf(
                    MemoSearchResult.Success(query = search.query, memos = emptyList())
                )

                else -> searchMemosUseCase(search.query)
                    .map<List<Memo>, MemoSearchResult> { memos ->
                        MemoSearchResult.Success(query = search.query, memos = memos)
                    }
                    .catch { throwable ->
                        if (throwable is CancellationException) throw throwable
                        emit(MemoSearchResult.Failure(query = search.query))
                    }
            }
        }

    fun open() {
        mutableControls.update { search ->
            if (search.isActive) search else SearchUiState(isActive = true)
        }
    }

    fun toggle() {
        if (controls.value.isActive) {
            close()
        } else {
            open()
        }
    }

    fun updateQuery(query: String) {
        mutableControls.update { search ->
            if (search.isActive) search.copy(query = query) else search
        }
    }

    fun close() {
        mutableControls.update { SearchUiState() }
    }

    fun toUiState(
        controls: SearchUiState,
        result: MemoSearchResult,
        mapMemos: (List<Memo>) -> List<MemoUiModel>
    ): SearchUiState {
        if (!controls.isActive) return SearchUiState()

        return when (result) {
            MemoSearchResult.Inactive -> controls.copy(hasError = false, results = emptyList())

            is MemoSearchResult.Success -> controls.copy(
                hasError = false,
                results = if (result.query == controls.query) {
                    mapMemos(result.memos)
                } else {
                    emptyList()
                }
            )

            is MemoSearchResult.Failure -> controls.copy(
                hasError = result.query == controls.query,
                results = emptyList()
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
