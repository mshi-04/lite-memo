package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.repository.MemoImageStore
import javax.inject.Inject

class DeleteMemoImagesUseCase @Inject constructor(private val memoImageStore: MemoImageStore) {

    suspend operator fun invoke(fileNames: List<MemoImageFileName>) =
        memoImageStore.deleteImages(fileNames)

}
