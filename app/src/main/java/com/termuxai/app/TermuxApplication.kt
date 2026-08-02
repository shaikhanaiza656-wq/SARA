package com.termuxai.app

import android.app.Application
import com.termuxai.app.core.AppContainer

class TermuxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
