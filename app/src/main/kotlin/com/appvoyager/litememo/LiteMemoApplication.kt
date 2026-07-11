package com.appvoyager.litememo

import android.app.Application
import android.util.Log
import com.appvoyager.litememo.ui.widget.data.WidgetRefresher
import com.appvoyager.litememo.ui.widget.di.WidgetEntryPoint
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

private const val WIDGET_REFRESH_DEBOUNCE_MS = 500L
private const val WIDGET_REFRESH_TAG = "WidgetRefresh"

@HiltAndroidApp
class LiteMemoApplication : BaseLiteMemoApplication() {

    override fun onCreate() {
        super.onCreate()
        observeMemosForWidgetRefresh()
    }

    // メモ変更をアプリプロセス全体で 1 箇所監視し、リスト系ウィジェットを更新する。
    // domain / data 層に Glance を持ち込まないよう、更新トリガはこの app 層に置く。
    // Hilt component が確実に生成される @HiltAndroidApp 側にだけ置き、
    // @CustomTestApplication が継承する BaseLiteMemoApplication では動かさない
    // （テスト用 App では onCreate 時点で component 未生成のため）。
    // 更新失敗で監視全体を止めないため Throwable を包括 catch する（Cancellation は再送出）。
    // 上流 Flow（Room emit 等）の例外は collect を突き抜けて監視コルーチンを終了させるため、
    // retryWhen で再購読して監視を継続する（Cancellation はそのまま伝播させる）。
    @Suppress("TooGenericExceptionCaught")
    private fun observeMemosForWidgetRefresh() {
        applicationScope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                this@LiteMemoApplication,
                WidgetEntryPoint::class.java
            )
            entryPoint.observeMemosUseCase()()
                .drop(1)
                .distinctUntilChanged()
                // 連続作成など短時間の大量変更をまとめ、バースト後に 1 度だけ最新状態で更新する。
                .debounce(WIDGET_REFRESH_DEBOUNCE_MS)
                .retryWhen { cause, _ ->
                    if (cause is CancellationException) {
                        false
                    } else {
                        // 上流失敗でも監視を止めず、少し待って再購読する。
                        Log.w(WIDGET_REFRESH_TAG, "Memo observation failed; retrying", cause)
                        delay(WIDGET_REFRESH_DEBOUNCE_MS)
                        true
                    }
                }
                .collect {
                    try {
                        WidgetRefresher.refreshLists(this@LiteMemoApplication)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // 1 回の失敗で監視全体を止めない（次の変更で再度更新される）。
                        Log.w(WIDGET_REFRESH_TAG, "Widget refresh failed", e)
                    }
                }
        }
    }
}

open class BaseLiteMemoApplication : Application() {

    protected val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Mobile Ads SDK の初期化は I/O を伴うためバックグラウンドで行う。
        applicationScope.launch {
            MobileAds.initialize(this@BaseLiteMemoApplication)
        }
    }
}
