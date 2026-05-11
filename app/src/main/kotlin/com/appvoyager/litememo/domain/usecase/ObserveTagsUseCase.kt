package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveTagsUseCase(private val tagRepository: TagRepository) {

    operator fun invoke(): Flow<List<Tag>> = tagRepository.observeTags()

}
