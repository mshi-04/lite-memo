package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.HomeViewModel

@Composable
fun HomeRoute(
    onMemoClick: (String) -> Unit,
    onCreateMemoClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.closeSearch() }
    }

    HomeScreen(
        uiState = uiState,
        onFilterSelected = { filter -> viewModel.selectFilter(filter) },
        onSortOrderSelected = { order -> viewModel.selectSortOrder(order) },
        onSearchToggle = { viewModel.toggleSearch() },
        onSearchQueryChanged = { query -> viewModel.updateSearchQuery(query) },
        onImportantToggle = { memoId, isImportant ->
            viewModel.setMemoImportant(memoId, isImportant)
        },
        onMemoClick = onMemoClick,
        onCreateMemoClick = onCreateMemoClick,
        onRetry = { viewModel.retry() },
        modifier = modifier
    )
}
