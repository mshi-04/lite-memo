package com.appvoyager.litememo.domain.provider

import com.appvoyager.litememo.domain.model.value.MemoImageId

/**
 * Provides image ids for MemoImageStore implementations in the data layer.
 * Domain keeps the abstraction so image ids remain part of the domain contract
 * without exposing UUID or file-system details.
 */
interface MemoImageIdProvider {

    fun newMemoImageId(): MemoImageId

}
