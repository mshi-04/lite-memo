package com.appvoyager.litememo.data.export

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoArchiveLayoutTest {

    @Test
    fun normalImageEntryNameIsZeroPaddedSequentialPath() {
        // Act
        // Normal: entry 名は連番から生成し、domain の ID や元のファイル名を使わない
        val entryName = MemoArchiveLayout.imageEntryName(1)

        // Assert
        assertEquals("images/00000001", entryName)
    }

    @Test
    fun errorImageEntryNameRejectsNonPositiveIndex() {
        // Act & Assert
        // Error: 採番は 1 始まりで、0 以下は生成規則から外れる
        assertThrows(IllegalArgumentException::class.java) {
            MemoArchiveLayout.imageEntryName(0)
        }
    }

    @Test
    fun boundaryIsImageEntryNameRejectsNamesOutsideGeneratedPattern() {
        // Act
        // Boundary: 生成規則に一致しない画像 entry 名は受け付けない
        val results = listOf(
            "images/1",
            "images/00000001.jpg",
            "images/sub/00000001",
            "manifest.json"
        ).map { MemoArchiveLayout.isImageEntryName(it) }

        // Assert
        assertEquals(List(results.size) { false }, results)
    }

    @Test
    fun errorIsSafeEntryNameRejectsNamesThatEscapeTheArchive() {
        // Act & Assert
        // Error: 絶対 path、親ディレクトリ参照、backslash、control character を含む名前を弾く
        assertAll(
            { assertFalse(MemoArchiveLayout.isSafeEntryName("../evil.txt")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("images/../../evil.txt")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("/etc/passwd")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("images\\00000001")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("C:/images/00000001")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("images/" + Char(1))) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName("./manifest.json")) },
            { assertFalse(MemoArchiveLayout.isSafeEntryName(" ")) }
        )
    }

    @Test
    fun normalIsSafeEntryNameAcceptsGeneratedLayout() {
        // Act
        // Normal: 生成した layout の entry 名はそのまま安全と判定する
        val results = listOf(
            MemoArchiveLayout.MANIFEST_ENTRY_NAME,
            MemoArchiveLayout.imageEntryName(12345678)
        ).map { MemoArchiveLayout.isSafeEntryName(it) }

        // Assert
        assertTrue(results.all { it })
    }
}
