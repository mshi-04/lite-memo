package com.appvoyager.litememo.data.provider

import com.appvoyager.litememo.domain.model.value.TagId
import com.appvoyager.litememo.domain.provider.TagIdProvider
import java.util.UUID

class UuidTagIdProvider : TagIdProvider {
    override fun newTagId(): TagId = TagId(UUID.randomUUID().toString())
}
