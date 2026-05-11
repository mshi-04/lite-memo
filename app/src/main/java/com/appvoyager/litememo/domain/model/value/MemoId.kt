package com.appvoyager.litememo.domain.model.value

@JvmInline
value class MemoId private constructor(val value: String) {

    companion object {
        operator fun invoke(rawValue: String): MemoId {
            val value = rawValue.trim()
            require(value.isNotEmpty()) { "MemoId must not be blank." }
            return MemoId(value)
        }
    }

}
