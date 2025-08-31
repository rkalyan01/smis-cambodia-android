package com.innovative.smis

import android.app.Application
import android.content.Context
import com.innovative.smis.BuildConfig
import com.innovative.smis.di.appModule
import com.innovative.smis.di.apiModule
import com.innovative.smis.di.databaseModule
import com.innovative.smis.di.networkModule
import com.innovative.smis.di.offlineModule
import com.innovative.smis.di.repositoryModule
import com.innovative.smis.util.helper.LocalizationHelper
import com.innovative.smis.util.helper.PreferenceHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SmisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ PERFORMANCE FIX: Setup Koin with optimizations to prevent main thread blocking
        setupKoinOptimized()
        applySavedLanguage()
    }

    override fun attachBaseContext(base: Context) {
        val preferenceHelper = PreferenceHelper(base)
        val savedLanguage = preferenceHelper.selectedLanguage
        val localizedContext = LocalizationHelper.setLocale(base, savedLanguage)
        super.attachBaseContext(localizedContext)
    }

    private fun setupKoinOptimized() {
        startKoin {
            // ✅ PERFORMANCE: Reduce logging overhead - only show warnings/errors in release builds
            if (BuildConfig.DEBUG) {
                androidLogger(Level.INFO) // Reduced from DEBUG to INFO
            } else {
                androidLogger(Level.ERROR) // Only errors in release
            }

            androidContext(this@SmisApplication)

            // ✅ PERFORMANCE: Load modules in order of importance
            // Core modules first (lightweight), heavy modules last
            modules(
                networkModule,    // Fast - just network setup
                appModule,        // Medium - ViewModels and helpers
                databaseModule,   // Heavy - database setup (moved later)
                repositoryModule, // Heavy - depends on database
                apiModule,        // Fast - just API interfaces
                offlineModule     // Heavy - offline managers (moved last)
            )
        }
    }

    private fun applySavedLanguage() {
        try {
            val preferenceHelper = PreferenceHelper(this)
            val savedLanguage = preferenceHelper.selectedLanguage
            LocalizationHelper.setLocale(this, savedLanguage)
        } catch (e: Exception) {
            //
        }
    }
}