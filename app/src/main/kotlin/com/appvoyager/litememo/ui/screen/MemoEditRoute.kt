package com.appvoyager.litememo.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.viewmodel.MemoEditNavigationEvent
import com.appvoyager.litememo.ui.viewmodel.MemoEditViewModel

@Composable
fun MemoEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val memoDeletedMessage = stringResource(R.string.memo_deleted_message)
    val undoLabel = stringResource(R.string.undo_label)

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                MemoEditNavigationEvent.NavigateBack -> onNavigateBack()

                MemoEditNavigationEvent.MemoDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = memoDeletedMessage,
                        actionLabel = undoLabel,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    } else {
                        onNavigateBack()
                    }
                }
            }
        }
    }

    BackHandler(enabled = !uiState.showDiscardDialog && !uiState.isDeletePending) {
        viewModel.requestBack()
    }

    MemoEditScreen(
        uiState = uiState,
        onTitleChanged = { viewModel.updateTitle(it) },
        onBodyChanged = { viewModel.updateBody(it) },
        onTagToggled = { viewModel.toggleTag(it) },
        onSave = { viewModel.save() },
        onDelete = { viewModel.delete() },
        onBackRequest = { viewModel.requestBack() },
        onDismissDiscard = { viewModel.dismissDiscardDialog() },
        onConfirmDiscard = { viewModel.confirmDiscard() },
        onRetry = { viewModel.reload() },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}
