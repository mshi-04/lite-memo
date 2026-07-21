package com.appvoyager.litememo.data.export

import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import com.appvoyager.litememo.data.model.export.MemoImageExportDto
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ZIP version 1 archive を stream で書き出す。
 * 画像本体はメモリへ載せず、[write] に渡された source からそのまま流す。
 */
internal class MemoArchiveWriter(private val json: Json, private val limits: MemoArchiveLimits) {

    /**
     * 検証済みの [manifest] を先頭 entry として書き、続けて画像 entry を宣言順に書き出す。
     * 画像 entry ごとに [openImageStream] を呼び、読み終えた stream を閉じる。
     *
     * manifest が宣言した size と SHA-256 は書き出しながら再検証する。
     * これにより、metadata 収集後に画像が差し替わった場合でも壊れた archive を残さない。
     *
     * [target] は書き出し完了時に閉じる。
     */
    fun write(
        target: OutputStream,
        manifest: LiteMemoExportDto,
        openImageStream: (MemoImageExportDto) -> InputStream
    ) {
        MemoArchiveManifestValidator.validate(manifest, limits)
        val manifestBytes = encodeManifest(manifest)

        ZipOutputStream(target).use { zip ->
            zip.putNextEntry(ZipEntry(MemoArchiveLayout.MANIFEST_ENTRY_NAME))
            zip.write(manifestBytes)
            zip.closeEntry()

            // 画像は保存時点ですでに圧縮済みのため、再圧縮の CPU コストだけを避ける。
            zip.setLevel(Deflater.NO_COMPRESSION)
            manifest.memos.flatMap { it.images }.forEach { metadata ->
                zip.putNextEntry(ZipEntry(metadata.archiveEntry))
                openImageStream(metadata).use { source ->
                    MemoArchiveImageCopier.copyVerified(source, zip, metadata)
                }
                zip.closeEntry()
            }
        }
    }

    private fun encodeManifest(manifest: LiteMemoExportDto): ByteArray {
        val bytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
        if (bytes.size > limits.maxManifestBytes) {
            archiveFailure(
                MemoArchiveFailureReason.LIMIT_EXCEEDED,
                "Archive manifest is larger than ${limits.maxManifestBytes} bytes."
            )
        }
        return bytes
    }

}
