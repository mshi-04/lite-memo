package com.appvoyager.litememo.domain.model.value

@JvmInline
value class MemoTitle private constructor(val value: String) {

    companion object {
        // 本文のみのメモを保存できるため、空のタイトルを許容する。
        operator fun invoke(rawValue: String): MemoTitle = MemoTitle(rawValue.trim())
    }

}
