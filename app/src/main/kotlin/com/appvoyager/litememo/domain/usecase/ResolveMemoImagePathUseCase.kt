package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.repository.MemoImageStore
import javax.inject.Inject

class ResolveMemoImagePathUseCase @Inject constructor(private val memoImageStore: MemoImageStore) {

    operator fun invoke(fileName: MemoImageFileName): String =
        memoImageStore.resolveImagePath(fileName)

}
