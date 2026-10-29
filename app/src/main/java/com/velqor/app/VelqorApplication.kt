package com.velqor.app

import android.app.Application
import com.velqor.app.di.AppContainer

class VelqorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
