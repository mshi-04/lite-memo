package com.appvoyager.litememo.ui.component

import androidx.compose.ui.graphics.Color
import com.appvoyager.litememo.ui.state.TagUiModel

/** タグ色(ARGB Long)を Compose Color に変換する共通ヘルパー。 */
internal fun tagColor(argb: Long): Color = Color(argb.toInt())

/** タグの表示色を Compose Color として取得する。 */
internal fun TagUiModel.toComposeColor(): Color = tagColor(colorArgb)
