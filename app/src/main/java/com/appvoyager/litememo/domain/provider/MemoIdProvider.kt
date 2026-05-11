package com.appvoyager.litememo.domain.provider

import com.appvoyager.litememo.domain.model.value.MemoId

interface MemoIdProvider {

    fun newMemoId(): MemoId

}
