package com.appvoyager.litememo.data.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class MemoArchiveWriterTest {

    @Test
    fun normalWritePlacesManifestAsFirstEntry() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 2048, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )

        // Act
        // Normal: Import が画像を展開する前に manifest を確定できる順序で書き出す
        val archive = writeArchive(manifest, mapOf(MemoArchiveLayout.imageEntryName(1) to content))

        // Assert
        assertEquals(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME, MemoArchiveLayout.imageEntryName(1)),
            entryNames(archive)
        )
    }

    @Test
    fun normalWriteKeepsAlreadyCompressedImageBytesIntact() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 4096, seed = 3)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )
        val archive = writeArchive(manifest, mapOf(MemoArchiveLayout.imageEntryName(1) to content))

        // Act
        // Normal: すでに圧縮された画像も byte-for-byte で復元できる
        val restored = readArchive(archive).images.getValue(MemoArchiveLayout.imageEntryName(1))

        // Assert
        assertEquals(content.toList(), restored.toList())
    }

    @Test
    fun errorWriteRejectsImageWhoseBytesDoNotMatchDeclaredChecksum() {
        // Arrange
        val declared = compressedImageBytes(sizeBytes = 512, seed = 1)
        val actual = compressedImageBytes(sizeBytes = 512, seed = 2)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(declared))))
        )

        // Act & Assert
        // Error: metadata 収集後に画像が差し替わっても壊れた archive を残さない
        assertArchiveFailure(MemoArchiveFailureReason.CHECKSUM_MISMATCH) {
            writeArchive(manifest, mapOf(MemoArchiveLayout.imageEntryName(1) to actual))
        }
    }

    @Test
    fun errorWriteRejectsImageLargerThanDeclaredSize() {
        // Arrange
        val declared = compressedImageBytes(sizeBytes = 512, seed = 1)
        val larger = declared + compressedImageBytes(sizeBytes = 16, seed = 2)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(declared))))
        )

        // Act & Assert
        // Error: 宣言した size を超える画像は書き出しを打ち切る
        assertArchiveFailure(MemoArchiveFailureReason.SIZE_MISMATCH) {
            writeArchive(manifest, mapOf(MemoArchiveLayout.imageEntryName(1) to larger))
        }
    }

    @Test
    fun errorWriteRejectsManifestThatFailsStructureValidation() {
        // Arrange
        val manifest = manifestFixture(version = MemoArchiveLayout.VERSION + 1)

        // Act & Assert
        // Error: writer も reader と同じ manifest 検証を通す
        assertArchiveFailure(MemoArchiveFailureReason.UNSUPPORTED_VERSION) {
            writeArchive(manifest)
        }
    }

    @Test
    fun boundaryWriteRejectsManifestOverManifestSizeLimit() {
        // Arrange
        val manifest = manifestFixture(
            memos = List(32) { index -> memoDtoFixture(id = "memo-$index") }
        )
        val limits = MemoArchiveLimits.DEFAULT.copy(maxManifestBytes = 64)

        // Act & Assert
        // Boundary: manifest だけで上限を超える場合は archive を書き出さない
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            MemoArchiveWriter(archiveJson, limits)
                .write(ByteArrayOutputStream(), manifest) { ByteArrayInputStream(ByteArray(0)) }
        }
    }

    private fun entryNames(archive: ByteArray): List<String> = buildList {
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                add(entry.name)
            }
        }
    }
}
