package com.appvoyager.litememo.ui.component

import androidx.compose.ui.graphics.Color
import com.appvoyager.litememo.ui.state.TagUiModel

internal fun tagColor(argb: Long): Color = Color(argb.toInt())

internal fun TagUiModel.toComposeColor(): Color = tagColor(colorArgb)
