package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.model.value.MemoImageId

data class MemoImage(val id: MemoImageId, val fileName: MemoImageFileName)
