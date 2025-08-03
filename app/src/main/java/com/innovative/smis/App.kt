package com.innovative.smis

import android.app.Application
import com.innovative.smis.di.appModule
import com.innovative.smis.di.networkModule
import com.innovative.smis.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Custom Application class for the SMIS app.
 * This class is the entry point of the application and is used to initialize
 * application-wide components, such as Koin for dependency injection.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            // Use Koin's Android logger for debugging (prints logs to Logcat)
            androidLogger()
            // Provide the Android application context to Koin
            androidContext(this@App)
            // Load all the defined Koin modules
            modules(appModule, repositoryModule, networkModule)
        }
    }
}
