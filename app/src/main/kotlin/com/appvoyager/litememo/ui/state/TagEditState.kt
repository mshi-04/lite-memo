package com.appvoyager.litememo.ui.state

import com.appvoyager.litememo.ui.theme.DEFAULT_TAG_COLORS

data class TagEditState(
    val id: String? = null,
    val name: String = "",
    val colorArgb: Long = DEFAULT_TAG_COLORS.first().argb,
    val nameError: Boolean = false,
    val duplicateNameError: Boolean = false,
    val saveError: Boolean = false,
    val isSaving: Boolean = false
)
