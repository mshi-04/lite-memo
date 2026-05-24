package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.tagFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemoUiModelTest {

    @Test
    fun fromDomainReturnsTagsInMemoTagIdOrder() {
        // Arrange
        val firstTag = tagFixture(id = "tag-1", name = "仕事")
        val secondTag = tagFixture(id = "tag-2", name = "生活")
        val memo = memoFixture(tagIds = listOf(TagId("tag-2"), TagId("tag-1")))

        // Act
        val uiModel = MemoUiModel.fromDomain(listOf(memo), listOf(firstTag, secondTag)).single()

        // Assert
        assertEquals(listOf("生活", "仕事"), uiModel.tags.map { it.name })
    }

    @Test
    fun fromDomainSkipsMissingTags() {
        // Arrange
        val tag = tagFixture(id = "tag-1", name = "仕事")
        val memo = memoFixture(tagIds = listOf(TagId("missing"), TagId("tag-1")))

        // Act
        val uiModel = MemoUiModel.fromDomain(listOf(memo), listOf(tag)).single()

        // Assert
        assertEquals(listOf("仕事"), uiModel.tags.map { it.name })
    }

}
