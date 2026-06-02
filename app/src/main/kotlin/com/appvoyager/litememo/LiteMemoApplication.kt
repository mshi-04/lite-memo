package com.appvoyager.litememo

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LiteMemoApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Mobile Ads SDK の初期化は I/O を伴うためバックグラウンドで行う。
        applicationScope.launch {
            MobileAds.initialize(this@LiteMemoApplication)
        }
    }
}
