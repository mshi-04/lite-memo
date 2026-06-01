package com.appvoyager.litememo.ui.viewmodel

import com.appvoyager.litememo.domain.FakeTagRepository
import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.QueueTagIdProvider
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.repository.TagRepository
import com.appvoyager.litememo.domain.tagFixture
import com.appvoyager.litememo.domain.usecase.DeleteTagUseCase
import com.appvoyager.litememo.domain.usecase.ObserveTagsUseCase
import com.appvoyager.litememo.domain.usecase.SaveTagUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagManageViewModelTest {

    private lateinit var dispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiStateKeepsEditingTagWhenObserveTagsFails() = runTest(dispatcher) {
        // Arrange
        val viewModel = tagManageViewModel(
            tagRepository = ObserveFailingTagRepository(listOf(tagFixture(id = "tag-1")))
        )
        viewModel.uiState.first { it.hasError }

        // Act
        viewModel.startCreate()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.hasError && it.editingTag != null }

        // Assert
        assertEquals(true to "", state.hasError to state.editingTag?.name)
    }

    @Test
    fun saveEditSetsDuplicateNameErrorWhenTagNameAlreadyExists() = runTest(dispatcher) {
        // Arrange
        val viewModel = tagManageViewModel(
            tagRepository = FakeTagRepository(listOf(tagFixture(id = "tag-1", name = "Work")))
        )
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.startCreate()
        viewModel.updateEditName("Work")
        viewModel.saveEdit()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.editingTag?.duplicateNameError == true }

        // Assert
        assertEquals(true, state.editingTag?.duplicateNameError)
    }

    @Test
    fun saveEditMapsDuplicateNameExceptionToDuplicateNameError() = runTest(dispatcher) {
        // Arrange
        val viewModel = tagManageViewModel(
            tagRepository = StaleObserveTagRepository(
                listOf(
                    tagFixture(id = "tag-1", name = "Work")
                )
            )
        )
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.startCreate()
        viewModel.updateEditName("Work")
        viewModel.saveEdit()
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.editingTag?.duplicateNameError == true }

        // Assert
        assertEquals(
            false to true,
            state.editingTag?.saveError to state.editingTag?.duplicateNameError
        )
    }

    @Test
    fun confirmDeleteEmitsDeleteErrorWhenDeleteFails() = runTest(dispatcher) {
        // Arrange
        val tag = tagFixture(id = "tag-1", name = "Work")
        val viewModel = tagManageViewModel(
            tagRepository = DeleteFailingTagRepository(listOf(tag))
        )
        val tagUiModel = viewModel.uiState.first { it.tags.isNotEmpty() }.tags.single()

        // Act
        viewModel.requestDelete(tagUiModel)
        viewModel.confirmDelete()
        advanceUntilIdle()
        val event = viewModel.deleteErrorEvent.first()

        // Assert
        assertEquals(Unit to null, event to viewModel.uiState.value.showDeleteDialog)
    }

    private fun tagManageViewModel(tagRepository: TagRepository = FakeTagRepository()) =
        TagManageViewModel(
            observeTagsUseCase = ObserveTagsUseCase(tagRepository),
            saveTagUseCase = SaveTagUseCase(
                tagRepository = tagRepository,
                tagIdProvider = QueueTagIdProvider(),
                currentTimeProvider = MutableTimeProvider()
            ),
            deleteTagUseCase = DeleteTagUseCase(tagRepository)
        )

    private class ObserveFailingTagRepository(initialTags: List<Tag>) : TagRepository {

        private val repository = FakeTagRepository(initialTags)

        override fun observeTags(): Flow<List<Tag>> = flow {
            emit(repository.currentTags())
            throw IllegalStateException("Failed to observe tags.")
        }

        override suspend fun getTag(id: TagId): Tag? = repository.getTag(id)

        override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> =
            repository.getTagsByIds(ids)

        override suspend fun saveTag(tag: Tag) = repository.saveTag(tag)

        override suspend fun deleteTag(id: TagId) = repository.deleteTag(id)

        override suspend fun getAllTags(): List<Tag> = repository.getAllTags()

        override suspend fun saveAllTags(tags: List<Tag>) = repository.saveAllTags(tags)
    }

    private class DeleteFailingTagRepository(initialTags: List<Tag>) : TagRepository {

        private val repository = FakeTagRepository(initialTags)

        override fun observeTags(): Flow<List<Tag>> = repository.observeTags()

        override suspend fun getTag(id: TagId): Tag? = repository.getTag(id)

        override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> =
            repository.getTagsByIds(ids)

        override suspend fun saveTag(tag: Tag) = repository.saveTag(tag)

        override suspend fun deleteTag(id: TagId): Unit =
            throw IllegalStateException("Failed to delete tag.")

        override suspend fun getAllTags(): List<Tag> = repository.getAllTags()

        override suspend fun saveAllTags(tags: List<Tag>) = repository.saveAllTags(tags)
    }

    private class StaleObserveTagRepository(initialTags: List<Tag>) : TagRepository {

        private val repository = FakeTagRepository(initialTags)

        override fun observeTags(): Flow<List<Tag>> = flowOf(emptyList())

        override suspend fun getTag(id: TagId): Tag? = repository.getTag(id)

        override suspend fun getTagsByIds(ids: List<TagId>): List<Tag> =
            repository.getTagsByIds(ids)

        override suspend fun saveTag(tag: Tag) = repository.saveTag(tag)

        override suspend fun deleteTag(id: TagId) = repository.deleteTag(id)

        override suspend fun getAllTags(): List<Tag> = repository.getAllTags()

        override suspend fun saveAllTags(tags: List<Tag>) = repository.saveAllTags(tags)
    }
}
