package com.appvoyager.litememo.domain.repository

import com.appvoyager.litememo.domain.model.MemoImage
import com.appvoyager.litememo.domain.model.value.ImageSourceReference
import com.appvoyager.litememo.domain.model.value.MemoImageFileName

/**
 * Store abstraction for image file contents managed outside Room.
 * The Store suffix distinguishes file storage from MemoRepository-style model persistence.
 */
interface MemoImageStore {

    /**
     * Copies a picked image into app-managed storage and returns its saved metadata.
     * Implementations throw when the source cannot be copied.
     */
    suspend fun saveImage(source: ImageSourceReference): MemoImage

    /** Deletes saved image files. Missing files are ignored as a no-op. */
    suspend fun deleteImages(fileNames: List<MemoImageFileName>)

    /** Returns an absolute display path without checking whether the file exists. */
    fun resolveImagePath(fileName: MemoImageFileName): String
}
