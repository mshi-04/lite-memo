package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.state.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun normalSettingsRowsAreDisplayed() {
        // Arrange
        val uiState = SettingsUiState(appVersion = "1.0.0")

        // Act
        composeRule.setContent {
            TestScreenContent {
                SettingsScreen(
                    uiState = uiState,
                    onThemeModeSelect = {},
                    onMemoSortOrderSelect = {},
                    onAppLockEnabledChange = {},
                    onExpandThemeDropdown = {},
                    onCollapseThemeDropdown = {},
                    onExpandSortOrder = {},
                    onCollapseSortOrder = {},
                    onTagManageClick = {},
                    onTrashClick = {},
                    onExportClick = {},
                    onImportClick = {},
                    onConfirmImport = {},
                    onDismissImportConfirmDialog = {},
                    onPrivacyPolicyClick = {},
                    onOpenSourceLicenseClick = {}
                )
            }
        }

        // Assert
        listOf(
            R.string.settings_theme,
            R.string.settings_sort_order,
            R.string.settings_app_lock,
            R.string.settings_export,
            R.string.settings_import
        ).forEach { stringId ->
            composeRule
                .onNodeWithText(string(stringId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun interactionImportDialogOpens() {
        // Arrange
        var uiState by mutableStateOf(SettingsUiState(appVersion = "1.0.0"))
        composeRule.setContent {
            TestScreenContent {
                SettingsScreen(
                    uiState = uiState,
                    onThemeModeSelect = {},
                    onMemoSortOrderSelect = {},
                    onAppLockEnabledChange = {},
                    onExpandThemeDropdown = {},
                    onCollapseThemeDropdown = {},
                    onExpandSortOrder = {},
                    onCollapseSortOrder = {},
                    onTagManageClick = {},
                    onTrashClick = {},
                    onExportClick = {},
                    onImportClick = {
                        uiState = uiState.copy(showImportConfirmDialog = true)
                    },
                    onConfirmImport = {},
                    onDismissImportConfirmDialog = {},
                    onPrivacyPolicyClick = {},
                    onOpenSourceLicenseClick = {}
                )
            }
        }

        // Act
        // Interaction: import action opens the confirm dialog.
        composeRule
            .onNodeWithTag("settingsImportAction")
            .performClick()

        // Assert
        composeRule
            .onNodeWithText(string(R.string.settings_import_confirm_title))
            .assertIsDisplayed()
    }

    @Test
    fun stateTransitionImportLoadingShowsProgress() {
        // Arrange
        val uiState = SettingsUiState(
            appVersion = "1.0.0",
            isImporting = true
        )

        // Act
        composeRule.setContent {
            TestScreenContent {
                SettingsScreen(
                    uiState = uiState,
                    onThemeModeSelect = {},
                    onMemoSortOrderSelect = {},
                    onAppLockEnabledChange = {},
                    onExpandThemeDropdown = {},
                    onCollapseThemeDropdown = {},
                    onExpandSortOrder = {},
                    onCollapseSortOrder = {},
                    onTagManageClick = {},
                    onTrashClick = {},
                    onExportClick = {},
                    onImportClick = {},
                    onConfirmImport = {},
                    onDismissImportConfirmDialog = {},
                    onPrivacyPolicyClick = {},
                    onOpenSourceLicenseClick = {}
                )
            }
        }

        // Assert
        composeRule
            .onNodeWithTag("settingsImportLoadingIndicator")
            .assertIsDisplayed()
    }

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
