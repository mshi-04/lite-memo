package com.appvoyager.litememo.ui.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.viewmodel.TrashViewModel

@Composable
fun TrashRoute(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionErrorMessage = stringResource(R.string.trash_action_error_body)

    LaunchedEffect(viewModel, snackbarHostState, actionErrorMessage) {
        viewModel.actionErrorEvent.collect {
            snackbarHostState.showSnackbar(
                message = actionErrorMessage,
                withDismissAction = true
            )
        }
    }

    TrashScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRestoreClick = { viewModel.restoreMemo(it) },
        onPermanentDeleteRequest = { viewModel.requestPermanentDelete(it) },
        onConfirmPermanentDelete = { viewModel.confirmPermanentDelete() },
        onDismissPermanentDelete = { viewModel.dismissPermanentDeleteDialog() },
        onRetry = { viewModel.retry() },
        modifier = modifier
    )
}
