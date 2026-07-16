package com.appvoyager.litememo.ui.component

import androidx.compose.ui.graphics.Color
import com.appvoyager.litememo.ui.model.TagUiModel

fun tagColor(argb: Long): Color = Color(argb.toInt())

fun TagUiModel.toComposeColor(): Color = tagColor(colorArgb)
