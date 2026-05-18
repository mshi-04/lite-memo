package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.TagManageViewModel

@Composable
fun TagManageRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TagManageScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onCreateClick = { viewModel.startCreate() },
        onEditClick = { viewModel.startEdit(it) },
        onDeleteRequest = { viewModel.requestDelete(it) },
        onConfirmDelete = { viewModel.confirmDelete() },
        onDismissDelete = { viewModel.dismissDeleteDialog() },
        onDismissDeleteError = { viewModel.dismissDeleteError() },
        onEditNameChanged = { viewModel.updateEditName(it) },
        onEditColorSelected = { viewModel.selectEditColor(it) },
        onSaveEdit = { viewModel.saveEdit() },
        onCancelEdit = { viewModel.cancelEdit() },
        onRetry = { viewModel.retry() },
        modifier = modifier
    )
}
