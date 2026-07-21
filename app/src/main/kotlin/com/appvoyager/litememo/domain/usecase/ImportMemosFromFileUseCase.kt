package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import javax.inject.Inject

class ImportMemosFromFileUseCase @Inject constructor(
    private val exportFileRepository: ExportFileRepository,
    private val importMemosUseCase: ImportMemosUseCase
) {

    suspend operator fun invoke(reference: ExportFileReference) {
        val data = exportFileRepository.read(reference)
        importMemosUseCase(data)
    }

}
