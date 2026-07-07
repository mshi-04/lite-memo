package com.appvoyager.litememo.domain.model.value

import java.net.URI

internal fun String.isAbsoluteUri(): Boolean = runCatching { URI(this) }
    .getOrNull()
    ?.let { uri -> uri.isAbsolute && !uri.rawSchemeSpecificPart.isNullOrBlank() }
    ?: false
