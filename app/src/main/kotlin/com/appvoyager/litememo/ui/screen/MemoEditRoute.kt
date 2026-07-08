package com.appvoyager.litememo.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.ui.viewmodel.MemoEditNavigationEvent
import com.appvoyager.litememo.ui.viewmodel.MemoEditOperationErrorEvent
import com.appvoyager.litememo.ui.viewmodel.MemoEditViewModel

@Composable
@Suppress("LongParameterList")
fun MemoEditRoute(
    onNavigateBack: () -> Unit,
    onMemoDeleted: (MemoId) -> Unit,
    onSaveError: () -> Unit,
    onDeleteError: () -> Unit,
    onShareError: () -> Unit,
    onImageAttachError: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnImageAttachError by rememberUpdatedState(onImageAttachError)
    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.attachImages(uris.map { it.toString() })
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                MemoEditNavigationEvent.NavigateBack -> onNavigateBack()
                is MemoEditNavigationEvent.MemoDeleted -> onMemoDeleted(event.memoId)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.operationErrorEvent.collect { event ->
            when (event) {
                MemoEditOperationErrorEvent.SaveFailed -> onSaveError()
                MemoEditOperationErrorEvent.DeleteFailed -> onDeleteError()
                MemoEditOperationErrorEvent.ImageAttachFailed -> currentOnImageAttachError()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.flushEdits()
    }

    BackHandler(enabled = !uiState.isDeletePending) {
        viewModel.finishEditing()
    }

    MemoEditScreen(
        uiState = uiState,
        onTitleChanged = { viewModel.updateTitle(it) },
        onBodyChanged = { viewModel.updateBody(it) },
        onTagToggled = { viewModel.toggleTag(it) },
        onDelete = { viewModel.delete() },
        onBackRequest = { viewModel.finishEditing() },
        onRetry = { viewModel.reload() },
        onAttachImageRequest = {
            pickImagesLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onImageRemove = { viewModel.removeImage(it) },
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
