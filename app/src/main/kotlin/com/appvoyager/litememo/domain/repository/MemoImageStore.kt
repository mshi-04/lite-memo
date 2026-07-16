package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.MemoImage
import com.appvoyager.litememo.domain.model.value.ImageSourceReference
import com.appvoyager.litememo.domain.model.value.MemoImageFileName

interface MemoImageStore {

    suspend fun saveImage(source: ImageSourceReference): MemoImage

    suspend fun deleteImages(fileNames: List<MemoImageFileName>)

    fun resolveImagePath(fileName: MemoImageFileName): String
}
