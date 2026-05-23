package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.Memo
import com.appvoyager.litememo.domain.usecase.RestoreMemoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LiteMemoAppViewModel @Inject constructor(private val restoreMemoUseCase: RestoreMemoUseCase) :
    ViewModel() {

    fun restoreMemo(memo: Memo) {
        viewModelScope.launch {
            runCatching {
                restoreMemoUseCase(memo)
            }
        }
    }
}
