package com.appvoyager.litememo.data.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoArchiveLimitsTest {

    @Test
    fun normalDefaultLimitsRecordArchiveSafetyBudget() {
        // Act
        // Normal: 既定の上限は製品仕様ではなく、安全上の実装定数として固定する
        val limits = MemoArchiveLimits.DEFAULT

        // Assert
        assertEquals(
            MemoArchiveLimits(
                // manifest と全画像に ZIP overhead を足しても収まり、size を返さない
                // DocumentProvider でも stream の実読込量で打ち切れる大きさ。
                maxArchiveBytes = 320L * BYTES_PER_MIB,
                // 現行 JSON import の 5 MiB 上限を引き継ぐ。manifest が持つのは同じ構造化データだけ。
                maxManifestBytes = 5L * BYTES_PER_MIB,
                // 画像は picker で選んだファイルを再エンコードせず保存するため、
                // 高解像度写真や PNG スクリーンショット 1 枚の実サイズをまかなう。
                maxImageBytes = 32L * BYTES_PER_MIB,
                // Import は staging と最終保存で一時的に約 2 倍の空き容量を要求するため、
                // 端末側のピークが 512 MiB 程度に収まる値にする。
                maxTotalImageBytes = 256L * BYTES_PER_MIB,
                // manifest 1 件 + 画像 1000 件。想定利用を大きく超えつつ検証コストを有界にする。
                maxEntryCount = 1_001
            ),
            limits
        )
    }

    @Test
    fun boundaryDefaultArchiveLimitAcceptsLargestExportableArchive() {
        // Arrange
        val limits = MemoArchiveLimits.DEFAULT

        // Act
        // Boundary: 自アプリが出力しうる最大の archive を Import 側の上限が拒否しない
        val largestExportableBytes = limits.maxManifestBytes + limits.maxTotalImageBytes

        // Assert
        assertTrue(limits.maxArchiveBytes > largestExportableBytes)
    }
}

private const val BYTES_PER_MIB = 1024L * 1024
