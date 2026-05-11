package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.SaveTagCommand
import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider
import com.appvoyager.litememo.domain.provider.TagIdProvider
import com.appvoyager.litememo.domain.repository.TagRepository

class SaveTagUseCase(
    private val tagRepository: TagRepository,
    private val tagIdProvider: TagIdProvider,
    private val currentTimeProvider: CurrentTimeProvider
) {

    suspend operator fun invoke(command: SaveTagCommand): Tag {
        val existingTag = command.id?.let { id -> tagRepository.getTag(id) }
        val tag = Tag(
            id = existingTag?.id ?: command.id ?: tagIdProvider.newTagId(),
            name = command.name,
            color = command.color,
            createdAt = existingTag?.createdAt ?: currentTimeProvider.now()
        )

        tagRepository.saveTag(tag)
        return tag
    }

}
