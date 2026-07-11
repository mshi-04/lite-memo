package com.appvoyager.litememo.ui.widget.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.glance.material3.ColorProviders
import com.appvoyager.litememo.ui.theme.BackgroundDark
import com.appvoyager.litememo.ui.theme.BackgroundLight
import com.appvoyager.litememo.ui.theme.ErrorDark
import com.appvoyager.litememo.ui.theme.ErrorLight
import com.appvoyager.litememo.ui.theme.OnBackgroundDark
import com.appvoyager.litememo.ui.theme.OnBackgroundLight
import com.appvoyager.litememo.ui.theme.OnPrimaryContainerDark
import com.appvoyager.litememo.ui.theme.OnPrimaryContainerLight
import com.appvoyager.litememo.ui.theme.OnPrimaryDark
import com.appvoyager.litememo.ui.theme.OnPrimaryLight
import com.appvoyager.litememo.ui.theme.OnSecondaryDark
import com.appvoyager.litememo.ui.theme.OnSecondaryLight
import com.appvoyager.litememo.ui.theme.OnSurfaceDark
import com.appvoyager.litememo.ui.theme.OnSurfaceLight
import com.appvoyager.litememo.ui.theme.OnSurfaceVariantDark
import com.appvoyager.litememo.ui.theme.OnSurfaceVariantLight
import com.appvoyager.litememo.ui.theme.OutlineDark
import com.appvoyager.litememo.ui.theme.OutlineLight
import com.appvoyager.litememo.ui.theme.OutlineVariantDark
import com.appvoyager.litememo.ui.theme.OutlineVariantLight
import com.appvoyager.litememo.ui.theme.PrimaryContainerDark
import com.appvoyager.litememo.ui.theme.PrimaryContainerLight
import com.appvoyager.litememo.ui.theme.PrimaryDark
import com.appvoyager.litememo.ui.theme.PrimaryLight
import com.appvoyager.litememo.ui.theme.SecondaryDark
import com.appvoyager.litememo.ui.theme.SecondaryLight
import com.appvoyager.litememo.ui.theme.SurfaceDark
import com.appvoyager.litememo.ui.theme.SurfaceLight
import com.appvoyager.litememo.ui.theme.SurfaceVariantDark
import com.appvoyager.litememo.ui.theme.SurfaceVariantLight

/**
 * アプリ本体（[com.appvoyager.litememo.ui.theme] のトークン）と揃えた Glance 用カラー。
 * Glance では MaterialTheme を直接使えないため、同じ生トークンから ColorScheme を組み直す。
 * アプリ本体と同様に dynamicColor は使わず固定パレットで統一する。
 */
private val WidgetLightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight
)

private val WidgetDarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark
)

val WidgetColorProviders = ColorProviders(light = WidgetLightColors, dark = WidgetDarkColors)
