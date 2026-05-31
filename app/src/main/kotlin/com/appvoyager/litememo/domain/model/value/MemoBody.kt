package com.appvoyager.litememo.domain.model.value

@JvmInline
value class MemoBody private constructor(val value: String) {

    companion object {
        // 本文は入力値をそのまま保持する。空白や改行を含む書式を維持するため trim しない。
        operator fun invoke(rawValue: String): MemoBody = MemoBody(rawValue)
    }
}
