package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.MutableTimeProvider
import com.appvoyager.litememo.domain.memoFixture
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.model.value.TimestampMillis
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetHomeSummaryUseCaseTest {

    private val zoneId = ZoneId.of("UTC")
    private val today = Instant.parse("2026-05-11T01:00:00Z").toEpochMilli()
    private val yesterday = Instant.parse("2026-05-10T12:00:00Z").toEpochMilli()

    @Test
    fun invokeReturnsTotalCountMatchingNumberOfInputMemos() {
        // Arrange
        val useCase = summaryUseCase()

        // Act
        val summary = useCase(listOf(memoFixture(id = "memo-1"), memoFixture(id = "memo-2")))

        // Assert
        assertEquals(2, summary.totalCount)
    }

    @Test
    fun invokeIncludesMemoInTodayCountWhenCreatedToday() {
        // Arrange
        val useCase = summaryUseCase()

        // Act
        val summary = useCase(listOf(memoFixture(createdAt = today, updatedAt = today)))

        // Assert
        assertEquals(1, summary.todayCount)
    }

    @Test
    fun invokeIncludesMemoInTodayCountWhenUpdatedToday() {
        // Arrange
        val useCase = summaryUseCase()

        // Act
        val summary = useCase(listOf(memoFixture(createdAt = yesterday, updatedAt = today)))

        // Assert
        assertEquals(1, summary.todayCount)
    }

    @Test
    fun invokeCountsOnlyUntaggedMemosAsUnorganized() {
        // Arrange
        val useCase = summaryUseCase()
        val unorganized = memoFixture(id = "unorganized")
        val tagged = memoFixture(id = "tagged", tagIds = listOf(TagId("tag-1")))

        // Act
        val summary = useCase(listOf(unorganized, tagged))

        // Assert
        assertEquals(1, summary.unorganizedCount)
    }

    @Test
    fun invokeCountsOnlyFavoriteMemosAsFavorite() {
        // Arrange
        val useCase = summaryUseCase()

        // Act
        val summary =
            useCase(
                listOf(
                    memoFixture(id = "normal"),
                    memoFixture(id = "Favorite", isFavorite = true)
                )
            )

        // Assert
        assertEquals(1, summary.favoriteCount)
    }

    private fun summaryUseCase() = GetHomeSummaryUseCase(
        currentTimeProvider = MutableTimeProvider(
            TimestampMillis(Instant.parse("2026-05-11T12:00:00Z").toEpochMilli())
        ),
        zoneId = zoneId
    )

}
