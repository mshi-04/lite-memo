package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.repository.MemoRepository
import com.appvoyager.litememo.domain.repository.TagRepository
import kotlinx.coroutines.flow.first

class DeleteTagUseCase(
    private val tagRepository: TagRepository,
    private val memoRepository: MemoRepository
) {

    suspend operator fun invoke(id: TagId) {
        memoRepository.observeMemos()
            .first()
            .filter { id in it.tagIds }
            .forEach { memo ->
                memoRepository.saveMemo(
                    memo.copy(tagIds = memo.tagIds.filterNot { it == id })
                )
            }
        tagRepository.deleteTag(id)
    }

}
