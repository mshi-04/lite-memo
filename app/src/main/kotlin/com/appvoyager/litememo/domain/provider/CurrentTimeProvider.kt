package com.appvoyager.litememo.domain.provider

import com.appvoyager.litememo.domain.model.value.TimestampMillis

interface CurrentTimeProvider {

    fun now(): TimestampMillis

}
