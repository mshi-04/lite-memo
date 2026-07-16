package com.appvoyager.litememo

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(Application::class)
interface LiteMemoHiltTestApplication
