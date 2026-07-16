package com.appvoyager.litememo.data.provider

import com.appvoyager.litememo.domain.model.value.MemoImageId
import com.appvoyager.litememo.domain.provider.MemoImageIdProvider
import java.util.UUID

class UuidMemoImageIdProvider : MemoImageIdProvider {

    override fun newMemoImageId(): MemoImageId = MemoImageId(UUID.randomUUID().toString())

}
