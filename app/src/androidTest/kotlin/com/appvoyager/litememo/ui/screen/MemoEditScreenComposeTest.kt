package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.state.MemoEditUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoEditScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun normalTitleAndBodyInputsAcceptText() {
        // Arrange
        var uiState by mutableStateOf(MemoEditUiState())
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = uiState,
                    onTitleChanged = { title -> uiState = uiState.copy(title = title) },
                    onBodyChanged = { body -> uiState = uiState.copy(body = body) },
                    onTagToggled = {},
                    onSave = {},
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onShareMemo = {}
                )
            }
        }

        // Act
        // Normal: title and body edits are reflected through callbacks.
        composeRule
            .onNodeWithTag("memoEditTitleInput")
            .performTextInput("Shopping")
        composeRule
            .onNodeWithTag("memoEditBodyInput")
            .performTextInput("Milk")

        // Assert
        assertEquals(
            MemoEditInputSnapshot(title = "Shopping", body = "Milk"),
            MemoEditInputSnapshot(title = uiState.title, body = uiState.body)
        )
    }

    @Test
    fun interactionSaveInvokesCallback() {
        // Arrange
        var saveCount = 0
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = MemoEditUiState(),
                    onTitleChanged = {},
                    onBodyChanged = {},
                    onTagToggled = {},
                    onSave = { saveCount += 1 },
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onShareMemo = {}
                )
            }
        }

        // Act
        // Interaction: save action delegates to the provided callback.
        composeRule
            .onNodeWithContentDescription(string(R.string.save_memo))
            .performClick()

        // Assert
        assertEquals(1, saveCount)
    }

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private data class MemoEditInputSnapshot(val title: String, val body: String)
}
