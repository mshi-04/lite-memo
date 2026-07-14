package com.appvoyager.litememo.domain.provider

import com.appvoyager.litememo.domain.model.value.MemoImageId

interface MemoImageIdProvider {

    fun newMemoImageId(): MemoImageId

}
