package com.appvoyager.litememo.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.appvoyager.litememo.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob のアンカー型アダプティブバナーを表示する。
 * 広告ユニット ID は文字列リソースで管理し、flavor ごとにテスト/本番 ID を切り替える。
 * AdView はライフサイクルに合わせて pause/resume/destroy し、
 * バックグラウンド中の不要なリフレッシュやリークを防ぐ。
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val adUnitId = stringResource(R.string.admob_banner_unit_id)
    val context = LocalContext.current
    val adWidthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidthDp)
            )
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}
