package com.appvoyager.litememo.data.export

/**
 * Archive codec が入力を拒否した理由。
 * 例外 message には利用者データを含めないため、呼び出し側の分岐はこの型で行う。
 */
internal enum class MemoArchiveFailureReason {
    UNSUPPORTED_VERSION,
    INVALID_SIGNATURE,
    MALFORMED_ARCHIVE,
    INVALID_ENTRY_NAME,
    DUPLICATE_IDENTIFIER,
    LIMIT_EXCEEDED,
    SIZE_MISMATCH,
    CHECKSUM_MISMATCH
}
