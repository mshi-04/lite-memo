package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.Tag
import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.repository.TagRepository
import javax.inject.Inject

class GetTagUseCase @Inject constructor(private val tagRepository: TagRepository) {

    suspend operator fun invoke(id: TagId): Tag? = tagRepository.getTag(id)

}
