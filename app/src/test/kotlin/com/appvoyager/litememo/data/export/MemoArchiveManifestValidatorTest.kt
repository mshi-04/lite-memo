package com.appvoyager.litememo.data.export

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class MemoArchiveManifestValidatorTest {

    private val limits = MemoArchiveLimits.DEFAULT

    @Test
    fun normalValidateAcceptsImageOnlyMemo() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    title = "",
                    body = "",
                    images = listOf(imageDtoFixture(content))
                )
            )
        )

        // Act & Assert
        // Normal: 画像だけを持つメモも archive の構造としては正当
        assertDoesNotThrow { MemoArchiveManifestValidator.validate(manifest, limits) }
    }

    @Test
    fun errorValidateRejectsUnsupportedVersion() {
        // Arrange
        val manifest = manifestFixture(version = MemoArchiveLayout.VERSION + 1)

        // Act & Assert
        // Error: 未対応 version は archive 全体を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.UNSUPPORTED_VERSION) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsDuplicateMemoIds() {
        // Arrange
        val manifest = manifestFixture(
            memos = listOf(memoDtoFixture(id = "memo-1"), memoDtoFixture(id = "memo-1"))
        )

        // Act & Assert
        // Error: memo ID の重複を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.DUPLICATE_IDENTIFIER) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsDuplicateTagIds() {
        // Arrange
        val manifest = manifestFixture(
            tags = listOf(tagDtoFixture(id = "tag-1"), tagDtoFixture(id = "tag-1"))
        )

        // Act & Assert
        // Error: tag ID の重複を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.DUPLICATE_IDENTIFIER) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsImageIdSharedByDifferentMemos() {
        // Arrange
        val first = compressedImageBytes(sizeBytes = 64, seed = 1)
        val second = compressedImageBytes(sizeBytes = 64, seed = 2)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(id = "memo-1", images = listOf(imageDtoFixture(first, index = 1))),
                memoDtoFixture(
                    id = "memo-2",
                    images = listOf(imageDtoFixture(second, index = 2, id = "image-1"))
                )
            )
        )

        // Act & Assert
        // Error: memo_images.id は DB 全体の primary key のため archive 全体で一意にする
        assertArchiveFailure(MemoArchiveFailureReason.DUPLICATE_IDENTIFIER) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsDuplicateArchiveEntries() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(
                        imageDtoFixture(content, index = 1),
                        imageDtoFixture(content, index = 2, archiveEntry = "images/00000001")
                    )
                )
            )
        )

        // Act & Assert
        // Error: 同じ archive entry を複数の画像が参照する manifest を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.DUPLICATE_IDENTIFIER) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsArchiveEntryOutsideGeneratedLayout() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(imageDtoFixture(content, archiveEntry = "../evil.txt"))
                )
            )
        )

        // Act & Assert
        // Error: 生成規則から外れる entry 名は manifest の段階で弾く
        assertArchiveFailure(MemoArchiveFailureReason.INVALID_ENTRY_NAME) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsImageFileNameWithPathSeparator() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(imageDtoFixture(content, fileName = "sub/picked.jpg"))
                )
            )
        )

        // Act & Assert
        // Error: 保存ファイル名にできない値を archive の段階で弾く
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun errorValidateRejectsChecksumThatIsNotSha256Hex() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(images = listOf(imageDtoFixture(content, sha256 = "not-a-digest")))
            )
        )

        // Act & Assert
        // Error: SHA-256 hex 以外の checksum を持つ manifest を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.MALFORMED_ARCHIVE) {
            MemoArchiveManifestValidator.validate(manifest, limits)
        }
    }

    @Test
    fun boundaryValidateRejectsSingleImageOverImageLimit() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(images = listOf(imageDtoFixture(content, sizeBytes = 9)))
            )
        )

        // Act & Assert
        // Boundary: 単一画像の上限を超える宣言を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            MemoArchiveManifestValidator.validate(manifest, limits.copy(maxImageBytes = 8))
        }
    }

    @Test
    fun boundaryValidateRejectsTotalImageBytesOverLimit() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(
                        imageDtoFixture(content, index = 1, sizeBytes = 8),
                        imageDtoFixture(content, index = 2, sizeBytes = 8)
                    )
                )
            )
        )

        // Act & Assert
        // Boundary: 展開後合計 byte 数の上限を超える宣言を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            MemoArchiveManifestValidator.validate(
                manifest,
                limits.copy(maxImageBytes = 8, maxTotalImageBytes = 15)
            )
        }
    }

    @Test
    fun boundaryValidateRejectsEntryCountOverLimit() {
        // Arrange
        val content = compressedImageBytes(sizeBytes = 64, seed = 1)
        val manifest = manifestFixture(
            memos = listOf(
                memoDtoFixture(
                    images = listOf(
                        imageDtoFixture(content, index = 1),
                        imageDtoFixture(content, index = 2)
                    )
                )
            )
        )

        // Act & Assert
        // Boundary: manifest を含む entry 総数の上限を超える宣言を拒否する
        assertArchiveFailure(MemoArchiveFailureReason.LIMIT_EXCEEDED) {
            MemoArchiveManifestValidator.validate(manifest, limits.copy(maxEntryCount = 2))
        }
    }
}
