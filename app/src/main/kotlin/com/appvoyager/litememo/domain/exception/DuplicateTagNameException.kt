package com.appvoyager.litememo.domain.exception

import com.appvoyager.litememo.domain.model.value.TagName

class DuplicateTagNameException(tagName: TagName) :
    IllegalArgumentException("Tag name already exists: ${tagName.value}")
