package com.appvoyager.litememo.domain.provider

import com.appvoyager.litememo.domain.model.value.TagId

interface TagIdProvider {

    fun newTagId(): TagId

}
