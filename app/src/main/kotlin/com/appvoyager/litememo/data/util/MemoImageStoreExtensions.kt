package com.appvoyager.litememo.data.util

import com.appvoyager.litememo.domain.model.value.MemoImageFileName
import com.appvoyager.litememo.domain.repository.MemoImageStore

suspend fun MemoImageStore.deleteImageFiles(fileNames: Collection<String>) {
    if (fileNames.isEmpty()) return
    deleteImages(fileNames.map { MemoImageFileName(it) })
}
