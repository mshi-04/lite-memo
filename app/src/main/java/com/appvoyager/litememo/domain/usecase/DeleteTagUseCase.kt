package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.repository.TagRepository

class DeleteTagUseCase(private val tagRepository: TagRepository) {

    suspend operator fun invoke(id: TagId) = tagRepository.deleteTagWithMemoReferences(id)

}
