package com.appvoyager.litememo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.appvoyager.litememo.ui.navigation.LiteMemoApp
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiteMemoTheme {
                LiteMemoApp()
            }
        }
    }
}
