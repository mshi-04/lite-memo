package com.appvoyager.litememo.domain.model.value

@JvmInline
value class TagId private constructor(val value: String) {

    companion object {
        operator fun invoke(rawValue: String): TagId {
            val value = rawValue.trim()
            require(value.isNotEmpty()) { "TagId must not be blank." }
            return TagId(value)
        }
    }

}
