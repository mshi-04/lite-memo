package com.appvoyager.litememo.domain.model.value

@JvmInline
value class ExportFileReference private constructor(val value: String) {

    companion object {
        operator fun invoke(rawValue: String): ExportFileReference {
            require(rawValue.isNotBlank()) { "ExportFileReference must not be blank." }
            return ExportFileReference(rawValue)
        }
    }
}
