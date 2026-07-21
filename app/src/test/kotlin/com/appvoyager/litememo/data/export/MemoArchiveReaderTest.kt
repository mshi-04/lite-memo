package com.appvoyager.litememo.data.export

import com.appvoyager.litememo.data.model.export.LiteMemoExportDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MemoArchiveReaderTest {

    @Test
    fun normalReadRestoresManifestAndImageOrder() {
        // Arrange
        val first = compressedImageBytes(sizeBytes = 1024, seed = 1)
        val second = compressedImageBytes(sizeBytes = 2048, seed = 2)
        val manifest = manifestFixture(
            tags = listOf(tagDtoFixture()),
            memos = listOf(
                memoDtoFixture(
                    isFavorite = true,
                    tagIds = listOf("tag-1"),
                    images = listOf(
                        imageDtoFixture(first, index = 1),
                        imageDtoFixture(second, index = 2)
                    )
                )
            )
        )
        val archive = writeArchive(manifest, contentsOf(manifest, listOf(first, second)))

        // Act
        // Normal: manifest の内容と画像の並びを往復で維持する
        val result = readArchive(archive)

        // Assert
        assertEquals(manifest, result.manifest)
    }

    @Test
    fun normalReadRestoresImageBytesForEveryEntry() {
        // Arrange
        val first = compressedImageBytes(sizeBytes = 1024, seed = 1)
        val second = compressedImageBytes(sizeBytes = 2048, seed = 2)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(
                        imageDtoFixture(first, index = 1),
                        imageDtoFixture(second, index = 2)
                    )
                )
            )
        )
        val archive = writeArchive(manifest, contentsOf(manifest, listOf(first, second)))

        // Act
        // Normal: 各画像 entry を宣言どおりの byte 列として復元する
        val restored = readArchive(archive).images.mapValues { it.value.toList() }

        // Assert
        assertEquals(
            mapOf(
                MemoArchiveLayout.imageEntryName(1) to first.toList(),
                MemoArchiveLayout.imageEntryName(2) to second.toList()
            ),
            restored
        )
    }

    @Test
    fun boundaryReadAcceptsImageOnlyMemo() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 512, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(title = "", body = "", images = listOf(imageDtoFixture(content)))
            )
        )
        val archive = writeArchive(manifest, contentsOf(manifest, listOf(content)))

        // Act
        // Boundary: title と body が空で画像だけを持つメモも読み取れる
        val result = readArchive(archive)

        // Assert
        assertEquals(manifest, result.manifest)
    }

    @Test
    fun errorReadRejectsInputWithoutZipSignature() {
        // Arrange
        val notAnArchive = """{"version":1}""".toByteArray(Charsets.UTF_8)

        // Act & Assert
        // Error: standalone JSON など ZIP でない入力を先頭 byte で拒否する
        assertArchiveFailure(MemoArchiveFailureReason.INVALID_SIGNATURE) {
            readArchive(notAnArchive)
        }
    }

    @Test
    fun errorReadRejectsArchiveWhoseFirstEntryIsNotManifest() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 128, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.imageEntryName(1) to content,
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest)
            )
        )

        // Act & Assert
        // Error: manifest より先に画像 entry が現れる archive を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsArchiveWithSecondManifestEntry() {
        // Arrange
        val manifest = manifestFixture(memos = listOf(memoDtoFixture()))
        val single = rawArchive(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest))
        )
        val archive = concatArchives(single, single)

        // Act & Assert
        // Error: manifest.json は archive root に 1 件だけとする
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsEntryTheManifestDoesNotDeclare() {
        // Arrange
        val manifest = manifestFixture(memos = listOf(memoDtoFixture()))
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest),
                "images/00000001" to compressedImageBytes(sizeBytes = 32, seed = 1)
            )
        )

        // Act & Assert
        // Error: manifest にない未知の file entry を許容しない
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsArchiveMissingDeclaredImageEntry() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 128, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )
        val archive = rawArchive(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest))
        )

        // Act & Assert
        // Error: manifest が参照する画像 entry が実在しない archive を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsEntryNameThatEscapesTheArchive() {
        // Arrange
        val manifest = manifestFixture(memos = listOf(memoDtoFixture()))
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest),
                "../evil.txt" to compressedImageBytes(sizeBytes = 32, seed = 1)
            )
        )

        // Act & Assert
        // Error: zip slip となる entry 名を展開しない
        assertArchiveFailure(MemoArchiveFailureReason.INVALID_ENTRY_NAME) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsEntryLargerThanDeclaredSize() {
        // Arrange
        val declared = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(declared))))
        )
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest),
                MemoArchiveLayout.imageEntryName(1) to ByteArray(EXPANDED_ENTRY_BYTES)
            )
        )

        // Act & Assert
        // Error: 宣言より大きく展開される entry を読み切る前に打ち切る
        assertArchiveFailure(MemoArchiveFailureReason.SIZE_MISMATCH) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsEntryThatDoesNotMatchDeclaredChecksum() {
        // Arrange
        val declared = compressedImageBytes(sizeBytes = 256, seed = 1)
        val actual = compressedImageBytes(sizeBytes = 256, seed = 2)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(declared))))
        )
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest),
                MemoArchiveLayout.imageEntryName(1) to actual
            )
        )

        // Act & Assert
        // Error: SHA-256 が manifest と一致しない画像を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.CHECKSUM_MISMATCH) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsUnsupportedVersion() {
        // Arrange
        val manifest = manifestFixture(version = MemoArchiveLayout.VERSION + 1)
        val archive = rawArchive(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest))
        )

        // Act & Assert
        // Error: 未対応 version は画像を展開せず archive 全体を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.UNSUPPORTED_VERSION) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsManifestThatIsNotValidJson() {
        // Arrange
        val manifestBytes = "not a manifest".toByteArray(Charsets.UTF_8)
        val archive = rawArchive(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME to manifestBytes)
        )

        // Act & Assert
        // Error: manifest が JSON として解釈できない archive を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(archive)
        }
    }

    @Test
    fun errorReadRejectsTruncatedArchive() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 4096, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )
        val archive = writeArchive(manifest, contentsOf(manifest, listOf(content)))
        val truncated = archive.copyOf(archive.size - TRUNCATED_TAIL_BYTES)

        // Act & Assert
        // Error: 途中で切れた archive を部分的に受け入れない
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            readArchive(truncated)
        }
    }

    @Test
    fun boundaryReadRejectsArchiveOverArchiveSizeLimitWithoutDeclaredSize() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 4096, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(images = listOf(imageDtoFixture(content))))
        )
        val archive = writeArchive(manifest, contentsOf(manifest, listOf(content)))
        val limits = MemoArchiveLimits.DEFAULT.copy(maxArchiveBytes = 1024)

        // Act & Assert
        // Boundary: 事前に size が分からなくても stream の実読込量で上限を強制する
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            readArchive(archive, limits)
        }
    }

    @Test
    fun boundaryReadRejectsManifestOverManifestSizeLimit() {
        // Arrange
        val manifest = manifestFixture(
            memos = List(32) { index -> memoDtoFixture(id = "memo-$index") }
        )
        val archive = rawArchive(
            listOf(MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest))
        )
        val limits = MemoArchiveLimits.DEFAULT.copy(maxManifestBytes = 64)

        // Act & Assert
        // Boundary: manifest の展開量にも上限を効かせる
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            readArchive(archive, limits)
        }
    }

    @Test
    fun errorReadDoesNotOpenSinkWhenEntryNameIsUnsafe() {
        // Arrange
        val manifest = manifestFixture(memos = listOf(memoDtoFixture()))
        val archive = rawArchive(
            listOf(
                MemoArchiveLayout.MANIFEST_ENTRY_NAME to encodeManifest(manifest),
                "../evil.txt" to compressedImageBytes(sizeBytes = 32, seed = 1)
            )
        )
        val openedEntries = mutableListOf<String>()

        // Act
        // Error/Interaction: 検証に失敗した archive では展開先を一切開かない
        assertThrows(MemoArchiveException::class.java) {
            MemoArchiveReader(archiveJson, MemoArchiveLimits.DEFAULT)
                .read(ByteArrayInputStream(archive)) { metadata ->
                    openedEntries += metadata.archiveEntry
                    ByteArrayOutputStream()
                }
        }

        // Assert
        assertTrue(openedEntries.isEmpty())
    }

    private fun contentsOf(
        manifest: LiteMemoExportDto,
        contents: List<ByteArray>
    ): Map<String, ByteArray> = manifest.memos
        .flatMap { it.images }
        .mapIndexed { index, image -> image.archiveEntry to contents[index] }
        .toMap()
}

// 圧縮された小さな entry が宣言 size を大きく超えて展開される、zip bomb と同じ形。
private const val EXPANDED_ENTRY_BYTES = 1024 * 1024

// central directory と EOCD を超えて画像 entry の途中まで削る長さ。
private const val TRUNCATED_TAIL_BYTES = 512
