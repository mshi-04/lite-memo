package com.appvoyager.litememo.data.provider

import com.appvoyager.litememo.domain.model.value.MemoId
import com.appvoyager.litememo.domain.provider.MemoIdProvider
import java.util.UUID

class UuidMemoIdProvider : MemoIdProvider {

    override fun newMemoId(): MemoId = MemoId(UUID.randomUUID().toString())

}
