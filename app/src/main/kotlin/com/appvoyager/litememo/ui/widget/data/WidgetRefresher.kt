package com.appvoyager.litememo.ui.widget.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.appvoyager.litememo.ui.widget.recent.RecentMemosWidget

object WidgetRefresher {
    suspend fun refreshLists(context: Context) {
        RecentMemosWidget().updateAll(context)
    }
}
