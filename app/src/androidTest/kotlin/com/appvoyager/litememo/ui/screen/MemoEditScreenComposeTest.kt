package com.appvoyager.litememo.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onAttachImageRequest = {},
                    onImageRemove = {},
                    onShareMemo = {}
                )
            }
        }

        // Act
        // Normal: title and body edits are reflected through callbacks.
        composeRule
            .onNodeWithTag(MemoEditTestTags.TITLE_INPUT)
            .performTextInput("Shopping")
        composeRule
            .onNodeWithTag(MemoEditTestTags.BODY_INPUT)
            .performTextInput("Milk")

        // Assert
        assertEquals(
            MemoEditInputSnapshot(title = "Shopping", body = "Milk"),
            MemoEditInputSnapshot(title = uiState.title, body = uiState.body)
        )
    }

    @Test
    fun interactionAttachImageButtonInvokesCallback() {
        // Arrange
        var clicked = false
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = MemoEditUiState(),
                    onTitleChanged = {},
                    onBodyChanged = {},
                    onTagToggled = {},
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onAttachImageRequest = { clicked = true },
                    onImageRemove = {},
                    onShareMemo = {}
                )
            }
        }

        // Act
        // Interaction: tapping the toolbar image button requests Photo Picker launch.
        composeRule
            .onNodeWithTag(MemoEditTestTags.ATTACH_IMAGE_BUTTON)
            .performClick()

        // Assert
        assertEquals(true, clicked)
    }

    @Test
    fun normalImagesShowImageListAndItems() {
        // Arrange
        val image = testMemoImageUiModel()

        // Act
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = MemoEditUiState(images = listOf(image)),
                    onTitleChanged = {},
                    onBodyChanged = {},
                    onTagToggled = {},
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onAttachImageRequest = {},
                    onImageRemove = {},
                    onShareMemo = {}
                )
            }
        }

        // Assert
        composeRule
            .onNodeWithTag(MemoEditTestTags.IMAGE_LIST)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(MemoEditTestTags.imageItem(image.id))
            .assertIsDisplayed()
    }

    @Test
    fun interactionRemoveImageButtonInvokesCallback() {
        // Arrange
        val image = testMemoImageUiModel()
        var removedId: String? = null
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = MemoEditUiState(images = listOf(image)),
                    onTitleChanged = {},
                    onBodyChanged = {},
                    onTagToggled = {},
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onAttachImageRequest = {},
                    onImageRemove = { removedId = it },
                    onShareMemo = {}
                )
            }
        }

        // Act
        // Interaction: tapping the per-image remove button emits that image id.
        composeRule
            .onNodeWithTag(MemoEditTestTags.removeImageButton(image.id))
            .performClick()

        // Assert
        assertEquals(image.id, removedId)
    }

    @Test
    fun normalDeletePendingHidesAttachImageButton() {
        // Act
        composeRule.setContent {
            TestScreenContent {
                MemoEditScreen(
                    uiState = MemoEditUiState(isDeletePending = true),
                    onTitleChanged = {},
                    onBodyChanged = {},
                    onTagToggled = {},
                    onDelete = {},
                    onBackRequest = {},
                    onRetry = {},
                    onAttachImageRequest = {},
                    onImageRemove = {},
                    onShareMemo = {}
                )
            }
        }

        // Assert
        composeRule
            .onAllNodesWithTag(MemoEditTestTags.ATTACH_IMAGE_BUTTON)
            .assertCountEquals(0)
    }

    private data class MemoEditInputSnapshot(val title: String, val body: String)
}
