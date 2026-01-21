package com.agentos

import android.app.Application
import com.agentos.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AgentOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin DI
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AgentOSApplication)
            modules(appModule)
        }
    }
}
