package com.appvoyager.litememo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.litememo.domain.model.MemoSortOrder
import com.appvoyager.litememo.domain.model.ThemeMode
import com.appvoyager.litememo.domain.repository.UserSettingsRepository
import com.appvoyager.litememo.ui.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    @Named("appVersion") private val appVersion: String
) : ViewModel() {

    private val showThemeDialog = MutableStateFlow(false)
    private val sortOrderExpanded = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        userSettingsRepository.observeThemeMode(),
        userSettingsRepository.observeMemoSortOrder(),
        showThemeDialog,
        sortOrderExpanded
    ) { themeMode, sortOrder, showDialog, expanded ->
        SettingsUiState(
            themeMode = themeMode,
            memoSortOrder = sortOrder,
            appVersion = appVersion,
            showThemeDialog = showDialog,
            sortOrderExpanded = expanded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(appVersion = appVersion)
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userSettingsRepository.setThemeMode(mode) }
    }

    fun setMemoSortOrder(order: MemoSortOrder) {
        viewModelScope.launch { userSettingsRepository.setMemoSortOrder(order) }
    }

    fun showThemeDialog() {
        showThemeDialog.value = true
    }

    fun dismissThemeDialog() {
        showThemeDialog.value = false
    }

    fun expandSortOrder() {
        sortOrderExpanded.value = true
    }

    fun collapseSortOrder() {
        sortOrderExpanded.value = false
    }
}
