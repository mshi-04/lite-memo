package com.appvoyager.litememo.ui.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.R
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.ui.viewmodel.HomeViewModel

@Composable
fun HomeRoute(
    onMemoClick: (String) -> Unit,
    onCreateMemoClick: () -> Unit,
    onShareError: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val actionErrorMessage = stringResource(R.string.home_bulk_action_error_body)

    DisposableEffect(viewModel) {
        onDispose { viewModel.closeSearch() }
    }

    LaunchedEffect(viewModel, snackbarHostState, actionErrorMessage) {
        viewModel.actionErrorEvent.collect {
            snackbarHostState.showSnackbar(
                message = actionErrorMessage,
                withDismissAction = true
            )
        }
    }

    HomeScreen(
        uiState = uiState,
        onFilterSelected = { filter -> viewModel.selectFilter(filter) },
        onSortOrderSelected = { order -> viewModel.selectSortOrder(order) },
        onSearchToggle = { viewModel.toggleSearch() },
        onSearchQueryChanged = { query -> viewModel.updateSearchQuery(query) },
        onMemoLongClick = { memoId -> viewModel.startSelection(MemoId(memoId)) },
        onMemoSelectionToggle = { memoId -> viewModel.toggleMemoSelection(MemoId(memoId)) },
        onClearSelection = { viewModel.clearSelection() },
        onMoveSelectedMemosToTrash = { viewModel.moveSelectedMemosToTrash() },
        onSetSelectedMemosFavorite = { isFavorite ->
            viewModel.setSelectedMemosFavorite(isFavorite)
        },
        onRequestAddTagToSelectedMemos = { viewModel.requestAddTagToSelectedMemos() },
        onRequestRemoveTagFromSelectedMemos = { viewModel.requestRemoveTagFromSelectedMemos() },
        onApplySelectedTag = { tagId -> viewModel.applySelectedTag(tagId) },
        onDismissBulkTagDialog = { viewModel.dismissBulkTagDialog() },
        onShareSelectedMemo = {
            val memo = viewModel.getSelectedMemoForShare() ?: return@HomeScreen
            val text = viewModel.formatMemoText(memo.title, memo.body) ?: return@HomeScreen
            context.launchShareMemo(
                text = text,
                subject = memo.title.trim().ifEmpty { null },
                onError = onShareError
            )
        },
        onMemoClick = onMemoClick,
        onCreateMemoClick = onCreateMemoClick,
        onRetry = { viewModel.retry() },
        modifier = modifier
    )
}
