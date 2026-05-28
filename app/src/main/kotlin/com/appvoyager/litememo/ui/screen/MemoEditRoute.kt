package com.appvoyager.litememo.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.ui.viewmodel.MemoEditNavigationEvent
import com.appvoyager.litememo.ui.viewmodel.MemoEditViewModel

@Composable
fun MemoEditRoute(
    onNavigateBack: () -> Unit,
    onMemoDeleted: (MemoId) -> Unit,
    onDraftError: () -> Unit,
    onShareError: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                MemoEditNavigationEvent.NavigateBack -> onNavigateBack()
                is MemoEditNavigationEvent.MemoDeleted -> onMemoDeleted(event.memoId)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.draftErrorEvent.collect {
            onDraftError()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.flushDraft()
    }

    BackHandler(enabled = !uiState.isDeletePending) {
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
        onRetry = { viewModel.reload() },
        onShareMemo = {
            val text = viewModel.formatMemoText() ?: return@MemoEditScreen
            context.launchShareMemo(
                text = text,
                subject = uiState.title.trim().ifEmpty { null },
                onError = onShareError
            )
        },
        modifier = modifier
    )
}
