package com.appvoyager.litememo.data.export

/**
 * Archive codec の上限値。製品仕様ではなく、悪意ある archive と端末負荷から守るための安全上の実装定数。
 * 各既定値の根拠は `MemoArchiveLimitsTest` に記録する。
 */
internal data class MemoArchiveLimits(
    val maxArchiveBytes: Long,
    val maxManifestBytes: Long,
    val maxImageBytes: Long,
    val maxTotalImageBytes: Long,
    val maxEntryCount: Int
) {

    companion object {
        val DEFAULT = MemoArchiveLimits(
            maxArchiveBytes = DEFAULT_MAX_ARCHIVE_BYTES,
            maxManifestBytes = DEFAULT_MAX_MANIFEST_BYTES,
            maxImageBytes = DEFAULT_MAX_IMAGE_BYTES,
            maxTotalImageBytes = DEFAULT_MAX_TOTAL_IMAGE_BYTES,
            maxEntryCount = DEFAULT_MAX_ENTRY_COUNT
        )
    }

}

private const val BYTES_PER_MIB = 1024L * 1024

// manifest + 全画像 + ZIP overhead を収める大きさ。size を返さない DocumentProvider でも、
// stream の実読込量をこの値で打ち切れるようにする。
private const val DEFAULT_MAX_ARCHIVE_BYTES = 320L * BYTES_PER_MIB

// 現行 JSON import の 5 MiB 上限をそのまま引き継ぐ。manifest は同じ構造化データだけを持つ。
private const val DEFAULT_MAX_MANIFEST_BYTES = 5L * BYTES_PER_MIB

// 画像は picker で選んだファイルを再エンコードせず保存するため、高解像度写真や
// PNG スクリーンショットの実サイズを 1 件分としてまかなえる値にする。
private const val DEFAULT_MAX_IMAGE_BYTES = 32L * BYTES_PER_MIB

// Import は staging と最終保存で一時的に約 2 倍の空き容量を要求するため、
// 端末側のピークが 512 MiB 程度に収まる値にする。
private const val DEFAULT_MAX_TOTAL_IMAGE_BYTES = 256L * BYTES_PER_MIB

// manifest 1 件 + 画像 1000 件。軽量メモアプリの想定利用を大きく超える一方で、
// entry ごとの検証コストを有界にする。
private const val DEFAULT_MAX_ENTRY_COUNT = 1_001
