package com.appvoyager.litememo.domain.usecase

import com.appvoyager.litememo.domain.model.value.ExportFileReference
import com.appvoyager.litememo.domain.repository.ExportFileRepository
import com.appvoyager.litememo.domain.repository.MemoImportArchiveRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportMemosFromFileUseCase @Inject constructor(
    private val exportFileRepository: ExportFileRepository,
    private val memoImportArchiveRepository: MemoImportArchiveRepository,
    private val importMemosUseCase: ImportMemosUseCase
) {

    suspend operator fun invoke(reference: ExportFileReference) {
        if (memoImportArchiveRepository.isArchive(reference)) {
            importArchive(reference)
        } else {
            importMemosUseCase(exportFileRepository.read(reference))
        }
    }

    private suspend fun importArchive(reference: ExportFileReference) {
        val staged = memoImportArchiveRepository.stageImportImages(reference)
        val failure = runCatching { importMemosUseCase(staged.data) }.exceptionOrNull()
        if (failure != null) {
            withContext(NonCancellable) {
                memoImportArchiveRepository.rollbackStagedImport(staged.token)
            }
            throw failure
        }
        withContext(NonCancellable) {
            memoImportArchiveRepository.completeStagedImport(staged.token)
        }
    }

}
