package com.appvoyager.litememo.domain.exception

import com.appvoyager.litememo.domain.model.value.TagName

class ImportTagNameConflictException(val tagNames: List<TagName>) :
    IllegalArgumentException(
        "Import contains conflicting tag names: ${tagNames.size} name(s)."
    )
