package com.appvoyager.litememo.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.ui.viewmodel.MemoEditViewModel

@Composable
fun MemoEditRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            onNavigateBack()
        }
    }

    BackHandler {
        viewModel.requestBack()
    }

    MemoEditScreen(
        uiState = uiState,
        onTitleChanged = { viewModel.updateTitle(it) },
        onBodyChanged = { viewModel.updateBody(it) },
        onSave = { viewModel.save() },
        onDelete = { viewModel.delete() },
        onBackRequest = { viewModel.requestBack() },
        onDismissDiscard = { viewModel.dismissDiscardDialog() },
        onConfirmDiscard = { viewModel.confirmDiscard() },
        modifier = modifier
    )
}
