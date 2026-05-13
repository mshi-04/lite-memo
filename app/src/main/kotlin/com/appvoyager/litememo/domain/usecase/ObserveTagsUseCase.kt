package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTagsUseCase @Inject constructor(private val tagRepository: TagRepository) {

    operator fun invoke(): Flow<List<Tag>> = tagRepository.observeTags()

}
