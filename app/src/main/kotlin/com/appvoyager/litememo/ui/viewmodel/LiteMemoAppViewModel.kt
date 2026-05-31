package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.usecase.PurgeExpiredTrashedMemosUseCase
import com.appvoyager.litememo.domain.usecase.RestoreMemoFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiteMemoAppViewModel @Inject constructor(
    private val restoreMemoFromTrashUseCase: RestoreMemoFromTrashUseCase,
    private val purgeExpiredTrashedMemosUseCase: PurgeExpiredTrashedMemosUseCase
) : ViewModel() {

    private val _restoreMemoErrorEvent = Channel<Unit>(Channel.BUFFERED)
    val restoreMemoErrorEvent = _restoreMemoErrorEvent.receiveAsFlow()

    init {
        purgeExpiredTrashedMemos()
    }

    fun restoreMemo(memoId: MemoId) {
        viewModelScope.launch {
            try {
                restoreMemoFromTrashUseCase(memoId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _restoreMemoErrorEvent.trySend(Unit)
            }
        }
    }

    private fun purgeExpiredTrashedMemos() {
        viewModelScope.launch {
            try {
                purgeExpiredTrashedMemosUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }
}
