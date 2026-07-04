package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import javax.inject.Inject

class GenerateMemoIdUseCase @Inject constructor(private val memoIdProvider: MemoIdProvider) {

    operator fun invoke(): MemoId = memoIdProvider.newMemoId()

}
