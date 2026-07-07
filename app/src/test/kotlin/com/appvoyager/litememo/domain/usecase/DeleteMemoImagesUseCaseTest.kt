package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.FakeMemoImageStore
import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteMemoImagesUseCaseTest {

    @Test
    fun interactionInvokeDelegatesFileNamesToStore() = runTest {
        // Arrange
        val store = FakeMemoImageStore()
        val fileNames = listOf(MemoImageFileName("image-1.jpg"), MemoImageFileName("image-2.jpg"))
        val useCase = DeleteMemoImagesUseCase(store)

        // Act
        // 観点: Interaction - deletion policy stays behind MemoImageStore.
        useCase(fileNames)

        // Assert
        assertEquals(fileNames, store.deletedFileNames)
    }

}
