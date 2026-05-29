package com.appvoyager.litememo.domain.model.value

import java.net.URI

@JvmInline
value class ExportFileReference private constructor(val value: String) {

    companion object {
        operator fun invoke(rawValue: String): ExportFileReference {
            require(rawValue.isNotBlank()) { "ExportFileReference must not be blank." }
            require(rawValue.isAbsoluteUri()) { "ExportFileReference must be a valid URI." }
            return ExportFileReference(rawValue)
        }
    }
}

private fun String.isAbsoluteUri(): Boolean = runCatching { URI(this) }
    .getOrNull()
    ?.let { uri -> uri.isAbsolute && !uri.rawSchemeSpecificPart.isNullOrBlank() }
    ?: false
