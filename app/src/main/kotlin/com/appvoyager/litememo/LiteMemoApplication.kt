package com.appvoyager.litememo

import android.app.Application
import android.util.Log
import com.appvoyager.litememo.di.ApplicationScope
import com.appvoyager.litememo.ui.widget.data.WidgetMemoLoader
import com.appvoyager.litememo.ui.widget.data.WidgetRefresher
import com.appvoyager.litememo.ui.widget.di.WidgetEntryPoint
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WIDGET_REFRESH_DEBOUNCE_MS = 500L
private const val WIDGET_REFRESH_TAG = "WidgetRefresh"

@HiltAndroidApp
class LiteMemoApplication : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            MobileAds.initialize(this@LiteMemoApplication)
        }
        observeMemosForWidgetRefresh()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun observeMemosForWidgetRefresh() {
        applicationScope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                this@LiteMemoApplication,
                WidgetEntryPoint::class.java
            )
            WidgetMemoLoader(entryPoint.observeRecentMemosUseCase()).observeRecent()
                .drop(1)
                .distinctUntilChanged()
                .debounce(WIDGET_REFRESH_DEBOUNCE_MS)
                .retryWhen { cause, _ ->
                    if (cause is CancellationException) {
                        false
                    } else {
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
                        Log.w(WIDGET_REFRESH_TAG, "Widget refresh failed", e)
                    }
                }
        }
    }
}
