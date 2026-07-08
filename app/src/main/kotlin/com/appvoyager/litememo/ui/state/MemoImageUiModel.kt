package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.domain.model.MemoImage
import com.appvoyager.litememo.domain.model.value.MemoImageFileName

data class MemoImageUiModel(
    val id: String,
    val fileName: String,
    val filePath: String,
    val isPersisted: Boolean
) {
    companion object {
        fun fromDomain(
            image: MemoImage,
            resolveImagePath: (MemoImageFileName) -> String,
            isPersisted: Boolean
        ): MemoImageUiModel = MemoImageUiModel(
            id = image.id.value,
            fileName = image.fileName.value,
            filePath = resolveImagePath(image.fileName),
            isPersisted = isPersisted
        )
    }
}
