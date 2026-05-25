package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.TrashViewModel

@Composable
fun TrashRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrashScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRestoreClick = { viewModel.restoreMemo(it) },
        onPermanentDeleteRequest = { viewModel.requestPermanentDelete(it) },
        onConfirmPermanentDelete = { viewModel.confirmPermanentDelete() },
        onDismissPermanentDelete = { viewModel.dismissPermanentDeleteDialog() },
        onDismissActionError = { viewModel.dismissActionError() },
        onRetry = { viewModel.retry() },
        modifier = modifier
    )
}
