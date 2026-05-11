package com.appvoyager.litememo.data.provider

import com.appvoyager.litememo.domain.model.value.TimestampMillis
import com.appvoyager.litememo.domain.provider.CurrentTimeProvider

class SystemCurrentTimeProvider : CurrentTimeProvider {

    override fun now(): TimestampMillis = TimestampMillis(System.currentTimeMillis())

}
