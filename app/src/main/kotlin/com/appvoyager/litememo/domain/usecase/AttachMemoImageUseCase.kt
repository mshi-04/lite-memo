package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.MemoImage
import com.appvoyager.litememo.domain.model.value.ImageSourceReference
import com.appvoyager.litememo.domain.repository.MemoImageStore
import javax.inject.Inject

class AttachMemoImageUseCase @Inject constructor(private val memoImageStore: MemoImageStore) {

    suspend operator fun invoke(source: ImageSourceReference): MemoImage =
        memoImageStore.saveImage(source)

}
