package com.appvoyager.litememo.ui.state

data class SearchUiState(
    val isActive: Boolean = false,
    val query: String = "",
    val hasError: Boolean = false,
    val results: List<MemoUiModel> = emptyList()
) {

    init {
        require(isActive || query.isEmpty()) {
            "SearchUiState query must be empty when search is inactive."
        }
        require(isActive || results.isEmpty()) {
            "SearchUiState results must be empty when search is inactive."
        }
    }

}
