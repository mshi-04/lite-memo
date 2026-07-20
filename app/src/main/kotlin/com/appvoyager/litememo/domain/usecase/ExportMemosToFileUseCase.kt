package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import javax.inject.Inject

class ExportMemosToFileUseCase @Inject constructor(
    private val exportMemosUseCase: ExportMemosUseCase,
    private val exportFileRepository: ExportFileRepository
) {

    suspend operator fun invoke(reference: ExportFileReference) {
        val data = exportMemosUseCase()
        exportFileRepository.write(reference, data)
    }

}
