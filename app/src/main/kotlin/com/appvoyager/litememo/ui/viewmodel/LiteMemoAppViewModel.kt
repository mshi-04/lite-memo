package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.usecase.RestoreMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LiteMemoAppViewModel @Inject constructor(private val restoreMemoUseCase: RestoreMemoUseCase) :
    ViewModel() {

    private val _restoreMemoErrorEvent = Channel<Unit>(Channel.BUFFERED)
    val restoreMemoErrorEvent = _restoreMemoErrorEvent.receiveAsFlow()

    fun restoreMemo(memo: Memo) {
        viewModelScope.launch {
            runCatching {
                restoreMemoUseCase(memo)
            }.onFailure {
                _restoreMemoErrorEvent.trySend(Unit)
            }
        }
    }
}
